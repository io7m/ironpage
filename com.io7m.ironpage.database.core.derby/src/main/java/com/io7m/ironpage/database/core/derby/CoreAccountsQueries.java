/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.ironpage.database.core.derby;

import com.io7m.ironpage.database.core.api.CDAccountsQueriesType;
import com.io7m.ironpage.database.core.api.CDException;
import com.io7m.ironpage.database.core.api.CDPasswordHashDTO;
import com.io7m.ironpage.database.core.api.CDSecurityRoleDTO;
import com.io7m.ironpage.database.core.api.CDSessionDTO;
import com.io7m.ironpage.database.core.api.CDUserDTO;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.jaffirm.core.Invariants;
import io.vavr.collection.TreeMap;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.ironpage.database.audit.api.AuditEventKind.USER_CREATED;
import static com.io7m.ironpage.database.audit.api.AuditEventKind.USER_MODIFIED_DISPLAY_NAME;
import static com.io7m.ironpage.database.audit.api.AuditEventKind.USER_MODIFIED_EMAIL;
import static com.io7m.ironpage.database.audit.api.AuditEventKind.USER_MODIFIED_LOCKED;
import static com.io7m.ironpage.database.audit.api.AuditEventKind.USER_MODIFIED_PASSWORD;
import static com.io7m.ironpage.database.audit.api.AuditEventKind.USER_MODIFIED_ROLES;
import static com.io7m.ironpage.database.audit.api.AuditEventKind.USER_SESSION_CREATED;
import static com.io7m.ironpage.database.core.api.CDRolesQueriesType.ROLE_NONEXISTENT;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_ROLE_DESCRIPTION;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_ROLE_ID;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_ROLE_NAME;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_ROLE_ROLE_ID;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_ROLE_USER_ID;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_SESSION_ID;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_SESSION_UPDATED;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_SESSION_USER_ID;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_USER_DISPLAY_NAME;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_USER_EMAIL;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_USER_ID;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_USER_LOCKED_REASON;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_USER_PASSWORD_ALGO;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_USER_PASSWORD_HASH;
import static com.io7m.ironpage.database.core.derby.CoreTables.FIELD_USER_PASSWORD_PARAMS;
import static com.io7m.ironpage.database.core.derby.CoreTables.TABLE_ROLES;
import static com.io7m.ironpage.database.core.derby.CoreTables.TABLE_ROLE_USERS;
import static com.io7m.ironpage.database.core.derby.CoreTables.TABLE_SESSIONS;
import static com.io7m.ironpage.database.core.derby.CoreTables.TABLE_USERS;

final class CoreAccountsQueries implements CDAccountsQueriesType
{
  private final DSLContext dslContext;
  private final CoreAuditQueries audit;
  private final Clock clock;

  CoreAccountsQueries(
    final Clock inClock,
    final Connection inConnection)
  {
    this.clock = Objects.requireNonNull(inClock, "clock");
    final var connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(connection, SQLDialect.DERBY, settings);
    this.audit = new CoreAuditQueries(this.clock, connection);
  }

  private static CDException handleDataAccessException(
    final UUID id,
    final String displayName,
    final DataAccessException e)
  {
    final var cause = e.getCause();
    if (cause instanceof BatchUpdateException) {
      final var batchCause = cause.getCause();
      if (batchCause instanceof DerbySQLIntegrityConstraintViolationException) {
        return integrityException(
          (DerbySQLIntegrityConstraintViolationException) batchCause,
          id,
          displayName);
      }
    } else if (cause instanceof DerbySQLIntegrityConstraintViolationException) {
      return integrityException(
        (DerbySQLIntegrityConstraintViolationException) cause,
        id,
        displayName);
    } else if (cause instanceof SQLDataException) {
      return invalidDataException((SQLDataException) cause);
    } else if (cause instanceof SQLException) {
      return genericDatabaseException((SQLException) cause);
    }
    return genericDatabaseException(new SQLException(cause));
  }

  private static String toHex(final byte[] data)
  {
    return Hex.encodeHexString(data, true);
  }

  private static CDUserDTO accountFromRecord(
    final Record userRecord,
    final SortedSet<CDSecurityRoleDTO> userRoles)
  {
    try {
      final var hash =
        CDPasswordHashDTO.builder()
          .setHash(Hex.decodeHex(userRecord.get(FIELD_USER_PASSWORD_HASH)))
          .setParameters(userRecord.get(FIELD_USER_PASSWORD_PARAMS))
          .setAlgorithm(userRecord.get(FIELD_USER_PASSWORD_ALGO))
          .build();

      return CDUserDTO.builder()
        .setDisplayName(userRecord.get(FIELD_USER_DISPLAY_NAME))
        .setEmail(userRecord.get(FIELD_USER_EMAIL))
        .setId(userRecord.get(FIELD_USER_ID))
        .setLocked(Optional.ofNullable(userRecord.get(FIELD_USER_LOCKED_REASON)))
        .setPasswordHash(hash)
        .setRoles(userRoles)
        .build();
    } catch (final DecoderException e) {
      throw new IllegalStateException(e);
    }
  }

  private static CDException integrityException(
    final DerbySQLIntegrityConstraintViolationException e,
    final UUID id,
    final String displayName)
  {
    switch (e.getConstraintName()) {
      case "ROLE_ROLE_ID_REFERENCE": {
        return new CDException(
          ErrorSeverity.SEVERITY_ERROR,
          ROLE_NONEXISTENT,
          CoreMessages.localize("errorRoleNonexistent"),
          e,
          TreeMap.of(CoreMessages.localize("displayName"), displayName));
      }
      case "USER_DISPLAY_NAME_UNIQUE": {
        return new CDException(
          ErrorSeverity.SEVERITY_ERROR,
          DISPLAY_NAME_ALREADY_USED,
          CoreMessages.localize("errorUserDisplayNameConflict", displayName),
          e,
          TreeMap.of(CoreMessages.localize("displayName"), displayName));
      }
      case "USER_ID_KEY": {
        return new CDException(
          ErrorSeverity.SEVERITY_ERROR,
          ID_ALREADY_USED,
          CoreMessages.localize("errorUserIDConflict"),
          e,
          TreeMap.of(CoreMessages.localize("userID"), id.toString()));
      }
      default: {
        return invalidDataException(e);
      }
    }
  }

  private static CDException invalidDataException(
    final Exception e)
  {
    return new CDException(
      ErrorSeverity.SEVERITY_ERROR,
      INVALID_DATA,
      CoreMessages.localize("errorUserDataInvalid"),
      e);
  }

  private static CDException genericDatabaseException(
    final Exception e)
  {
    return new CDException(
      ErrorSeverity.SEVERITY_ERROR,
      DATABASE_ERROR,
      CoreMessages.localize("errorDatabase", e.getLocalizedMessage()),
      e);
  }

  private static CDSecurityRoleDTO roleFromRecord(
    final Record4<UUID, Long, String, String> record)
  {
    return CDSecurityRoleDTO.builder()
      .setId(record.<Long>getValue(FIELD_ROLE_ID).longValue())
      .setName(record.getValue(FIELD_ROLE_NAME))
      .setDescription(record.getValue(FIELD_ROLE_DESCRIPTION))
      .build();
  }

  @Override
  public CDUserDTO accountCreate(
    final UUID id,
    final String displayName,
    final CDPasswordHashDTO password,
    final String email,
    final Optional<String> lockedReason)
    throws CDException
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(password, "password");
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(lockedReason, "lockedReason");

    try (var query = this.dslContext.insertInto(TABLE_USERS)
      .set(FIELD_USER_ID, id)
      .set(FIELD_USER_DISPLAY_NAME, displayName)
      .set(FIELD_USER_EMAIL, email)
      .set(FIELD_USER_LOCKED_REASON, lockedReason.orElse(null))
      .set(FIELD_USER_PASSWORD_ALGO, password.algorithm())
      .set(FIELD_USER_PASSWORD_PARAMS, password.parameters())
      .set(FIELD_USER_PASSWORD_HASH, toHex(password.hash()))) {
      query.execute();
    } catch (final DataAccessException e) {
      throw handleDataAccessException(id, displayName, e);
    }

    try {
      this.audit.auditEventLog(USER_CREATED, id, displayName, "", "");
    } catch (final Exception e) {
      throw genericDatabaseException(e);
    }

    return CDUserDTO.builder()
      .setId(id)
      .setDisplayName(displayName)
      .setPasswordHash(password)
      .setEmail(email)
      .setLocked(lockedReason)
      .setRoles(new TreeSet<>())
      .build();
  }

  @Override
  public CDUserDTO accountUpdate(
    final UUID caller,
    final CDUserDTO account)
    throws CDException
  {
    Objects.requireNonNull(caller, "caller");
    Objects.requireNonNull(account, "account");

    final var existing = this.accountGet(account.id());

    this.accountUpdateRoles(account, existing);
    this.accountUpdateFields(account);
    this.accountUpdateLogDisplayName(caller, account, existing);
    this.accountUpdateLogEmail(caller, account, existing);
    this.accountUpdateLogPasswordHash(caller, account, existing);
    this.accountUpdateLogLocked(caller, account, existing);
    this.accountUpdateLogRoles(caller, account, existing);

    return account;
  }

  private void accountUpdateLogRoles(
    final UUID caller,
    final CDUserDTO account,
    final CDUserDTO existing)
    throws CDException
  {
    final var existingRoles = existing.roles();
    final var newRoles = account.roles();
    if (!Objects.equals(existingRoles, newRoles)) {
      try {
        this.audit.auditEventLog(
          USER_MODIFIED_ROLES,
          caller,
          account.id().toString(),
          existingRoles
            .stream()
            .map(CDSecurityRoleDTO::name)
            .collect(Collectors.joining(",")),
          newRoles
            .stream()
            .map(CDSecurityRoleDTO::name)
            .collect(Collectors.joining(",")));
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }
  }

  private void accountUpdateLogLocked(
    final UUID caller,
    final CDUserDTO account,
    final CDUserDTO existing)
    throws CDException
  {
    final var existingLocked = existing.locked();
    final var newLocked = account.locked();
    if (!Objects.equals(existingLocked, newLocked)) {
      try {
        this.audit.auditEventLog(
          USER_MODIFIED_LOCKED,
          caller,
          account.id().toString(),
          existingLocked.orElse(""),
          newLocked.orElse(""));
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }
  }

  private void accountUpdateLogPasswordHash(
    final UUID caller,
    final CDUserDTO account,
    final CDUserDTO existing)
    throws CDException
  {
    if (!Objects.equals(existing.passwordHash(), account.passwordHash())) {
      try {
        this.audit.auditEventLog(
          USER_MODIFIED_PASSWORD,
          caller,
          account.id().toString(),
          "",
          "");
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }
  }

  private void accountUpdateLogEmail(
    final UUID caller,
    final CDUserDTO account,
    final CDUserDTO existing)
    throws CDException
  {
    final var existingEmail = existing.email();
    final var newEmail = account.email();
    if (!Objects.equals(existingEmail, newEmail)) {
      try {
        this.audit.auditEventLog(
          USER_MODIFIED_EMAIL,
          caller,
          account.id().toString(),
          existingEmail,
          newEmail);
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }
  }

  private void accountUpdateLogDisplayName(
    final UUID caller,
    final CDUserDTO account,
    final CDUserDTO existing)
    throws CDException
  {
    final var existingDisplay = existing.displayName();
    final var newDisplay = account.displayName();
    if (!Objects.equals(existingDisplay, newDisplay)) {
      try {
        this.audit.auditEventLog(
          USER_MODIFIED_DISPLAY_NAME,
          caller,
          account.id().toString(),
          existingDisplay,
          newDisplay);
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }
  }

  private void accountUpdateFields(final CDUserDTO account)
    throws CDException
  {
    final var passwordHash = account.passwordHash();
    try (var query =
           this.dslContext.update(TABLE_USERS)
             .set(FIELD_USER_DISPLAY_NAME, account.displayName())
             .set(FIELD_USER_EMAIL, account.email())
             .set(FIELD_USER_LOCKED_REASON, account.locked().orElse(null))
             .set(FIELD_USER_PASSWORD_ALGO, passwordHash.algorithm())
             .set(FIELD_USER_PASSWORD_HASH, toHex(passwordHash.hash()))
             .set(FIELD_USER_PASSWORD_PARAMS, passwordHash.parameters())
             .where(FIELD_USER_ID.eq(account.id()))) {
      query.execute();
    } catch (final DataAccessException e) {
      throw handleDataAccessException(account.id(), account.displayName(), e);
    }
  }

  private void accountUpdateRoles(
    final CDUserDTO account,
    final CDUserDTO existing)
    throws CDException
  {
    if (!account.roles().equals(existing.roles())) {
      final var accountId = account.id();
      try (var rolesDeleteQuery =
             this.dslContext.delete(TABLE_ROLE_USERS)
               .where(FIELD_ROLE_USER_ID.eq(accountId))) {
        rolesDeleteQuery.execute();

        final var inserts =
          account.roles()
            .stream()
            .map(role -> this.dslContext.insertInto(TABLE_ROLE_USERS)
              .set(FIELD_ROLE_USER_ID, accountId)
              .set(FIELD_ROLE_ROLE_ID, Long.valueOf(role.id())))
            .collect(Collectors.toList());

        this.dslContext.batch(inserts).execute();
      } catch (final DataAccessException e) {
        throw handleDataAccessException(accountId, account.displayName(), e);
      }
    }
  }

  @Override
  public CDUserDTO accountGet(
    final UUID userId)
    throws CDException
  {
    Objects.requireNonNull(userId, "userId");

    try (var accountQuery = this.dslContext.select(
      FIELD_USER_DISPLAY_NAME,
      FIELD_USER_EMAIL,
      FIELD_USER_ID,
      FIELD_USER_LOCKED_REASON,
      FIELD_USER_PASSWORD_HASH,
      FIELD_USER_PASSWORD_PARAMS,
      FIELD_USER_PASSWORD_ALGO)
      .from(TABLE_USERS)
      .where(FIELD_USER_ID.eq(userId))
      .limit(1)) {

      final var userResults = accountQuery.fetch();
      if (userResults.isEmpty()) {
        throw new CDException(
          ErrorSeverity.SEVERITY_ERROR,
          NONEXISTENT,
          CoreMessages.localize("errorUserNonexistent"),
          null,
          TreeMap.of(CoreMessages.localize("userID"), userId.toString()));
      }

      try (var roleQuery = this.dslContext.select(
        FIELD_ROLE_USER_ID,
        FIELD_ROLE_ID,
        FIELD_ROLE_NAME,
        FIELD_ROLE_DESCRIPTION)
        .from(TABLE_ROLE_USERS)
        .join(TABLE_ROLES)
        .on(FIELD_ROLE_ROLE_ID.eq(FIELD_ROLE_ID))
        .where(FIELD_ROLE_USER_ID.eq(userId))) {

        final var roleResults = roleQuery.fetch();
        final SortedSet<CDSecurityRoleDTO> roles =
          new TreeSet<>(roleResults.map(CoreAccountsQueries::roleFromRecord));
        return accountFromRecord(userResults.get(0), roles);
      }
    }
  }

  @Override
  public CDSessionDTO accountSessionCreate(
    final UUID owner,
    final String session)
    throws CDException
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(session, "session");

    final var timestamp = Timestamp.from(this.clock.instant());
    try (var query =
           this.dslContext.insertInto(TABLE_SESSIONS)
             .set(FIELD_SESSION_ID, session)
             .set(FIELD_SESSION_UPDATED, timestamp)
             .set(FIELD_SESSION_USER_ID, owner)) {
      query.execute();

      try {
        this.audit.auditEventLog(USER_SESSION_CREATED, owner, session, "", "");
      } catch (final DatabaseException e) {
        throw genericDatabaseException(e);
      }

      return CDSessionDTO.builder()
        .setId(session)
        .setUserID(owner)
        .setUpdated(timestamp.toInstant())
        .build();
    } catch (final DataAccessException e) {
      final var cause = e.getCause();
      if (cause instanceof DerbySQLIntegrityConstraintViolationException) {
        final var constraintViolation = (DerbySQLIntegrityConstraintViolationException) cause;
        switch (constraintViolation.getConstraintName()) {
          case "SESSION_USER_REFERENCE": {
            throw new CDException(
              ErrorSeverity.SEVERITY_ERROR,
              NONEXISTENT,
              CoreMessages.localize("errorUserNonexistent"),
              cause);
          }
          case "SESSION_ID_KEY": {
            throw new CDException(
              ErrorSeverity.SEVERITY_ERROR,
              ID_ALREADY_USED,
              CoreMessages.localize("errorSessionIDAlreadyUsed"),
              cause);
          }
          default: {
            break;
          }
        }
      }
      throw genericDatabaseException(e);
    }
  }

  @Override
  public CDSessionDTO accountSessionUpdate(
    final String session)
    throws CDException
  {
    Objects.requireNonNull(session, "session");

    final var userId = this.sessionGet(session);
    final var timestamp = Timestamp.from(this.clock.instant());
    try (var query = this.dslContext.update(TABLE_SESSIONS)
      .set(FIELD_SESSION_UPDATED, timestamp)
      .where(FIELD_SESSION_ID.eq(session))) {
      final var updated = query.execute();

      Invariants.checkInvariantV(
        Integer.valueOf(updated),
        updated == 1,
        "Must have updated exactly one row (got %d)",
        Integer.valueOf(updated));

      return CDSessionDTO.builder()
        .setId(session)
        .setUserID(userId)
        .setUpdated(timestamp.toInstant())
        .build();
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  private UUID sessionGet(final String session)
    throws CDException
  {
    try (var query =
           this.dslContext.select(FIELD_SESSION_ID, FIELD_SESSION_USER_ID)
             .from(TABLE_SESSIONS)
             .where(FIELD_SESSION_ID.eq(session))) {

      final var rows = query.fetch();
      if (rows.size() != 1) {
        throw new CDException(
          ErrorSeverity.SEVERITY_ERROR,
          NONEXISTENT,
          CoreMessages.localize("errorSessionNonexistent"),
          null,
          TreeMap.of(CoreMessages.localize("sessionID"), session));
      }

      final var record = rows.get(0);
      final var userId =
        record.getValue(FIELD_SESSION_USER_ID);
      final var sessionReceived =
        record.getValue(FIELD_SESSION_ID).trim();

      Invariants.checkInvariantV(
        session,
        Objects.equals(session, sessionReceived),
        "Expected session '%s' must match received session '%s'",
        session,
        sessionReceived);

      return userId;
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public void accountSessionDelete(final String session)
    throws CDException
  {
    Objects.requireNonNull(session, "session");

    try (var query =
           this.dslContext.deleteFrom(TABLE_SESSIONS)
             .where(FIELD_SESSION_ID.eq(session))) {
      query.execute();
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public int accountSessionDeleteForUser(final UUID owner)
    throws CDException
  {
    Objects.requireNonNull(owner, "owner");

    try (var query =
           this.dslContext.deleteFrom(TABLE_SESSIONS)
             .where(FIELD_SESSION_USER_ID.eq(owner))) {
      return query.execute();
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }
}
