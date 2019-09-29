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

import com.io7m.ironpage.database.accounts.api.AccountsDatabaseException;
import com.io7m.ironpage.database.accounts.api.AccountsDatabasePasswordHashDTO;
import com.io7m.ironpage.database.accounts.api.AccountsDatabaseQueriesType;
import com.io7m.ironpage.database.accounts.api.AccountsDatabaseUserDTO;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import io.vavr.collection.TreeMap;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.sql.Connection;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Stream;

import static com.io7m.ironpage.database.core.derby.CoreAuditEventKind.USER_CREATED;
import static com.io7m.ironpage.database.core.derby.CoreAuditEventKind.USER_MODIFIED_DISPLAY_NAME;
import static com.io7m.ironpage.database.core.derby.CoreAuditEventKind.USER_MODIFIED_EMAIL;
import static com.io7m.ironpage.database.core.derby.CoreAuditEventKind.USER_MODIFIED_LOCKED;
import static com.io7m.ironpage.database.core.derby.CoreAuditEventKind.USER_MODIFIED_PASSWORD;

final class CoreAccountsDatabaseQueries implements AccountsDatabaseQueriesType
{
  private static final ResourceBundle RESOURCES =
    ResourceBundle.getBundle("com.io7m.ironpage.database.core.derby.Messages");

  private static final Table<Record> TABLE_USERS =
    DSL.table(DSL.name("core", "users"));
  private static final Field<String> FIELD_USER_DISPLAY_NAME =
    DSL.field(DSL.name("user_display_name"), SQLDataType.VARCHAR(128));
  private static final Field<String> FIELD_USER_EMAIL =
    DSL.field(DSL.name("user_email"), SQLDataType.VARCHAR(128));
  private static final Field<UUID> FIELD_USER_ID =
    DSL.field(DSL.name("user_id"), SQLDataType.UUID);
  private static final Field<String> FIELD_USER_LOCKED_REASON =
    DSL.field(DSL.name("user_locked_reason"), SQLDataType.VARCHAR(128));
  private static final Field<String> FIELD_USER_PASSWORD_HASH =
    DSL.field(DSL.name("user_password_hash"), SQLDataType.VARCHAR(128));
  private static final Field<String> FIELD_USER_PASSWORD_PARAMS =
    DSL.field(DSL.name("user_password_params"), SQLDataType.VARCHAR(256));
  private static final Field<String> FIELD_USER_PASSWORD_ALGO =
    DSL.field(DSL.name("user_password_algo"), SQLDataType.VARCHAR(64));

  private final Connection connection;
  private final DSLContext dslContext;
  private final CoreAuditQueries audit;
  private final Clock clock;

  CoreAccountsDatabaseQueries(
    final Clock inClock,
    final Connection inConnection)
  {
    this.clock = Objects.requireNonNull(inClock, "clock");
    this.connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(this.connection, SQLDialect.DERBY, settings);
    this.audit = new CoreAuditQueries(this.clock, this.connection);
  }

  private static AccountsDatabaseException handleDataAccessException(
    final UUID id,
    final String displayName,
    final DataAccessException e)
  {
    final var cause = e.getCause();
    if (cause instanceof DerbySQLIntegrityConstraintViolationException) {
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

  private static AccountsDatabaseUserDTO accountFromRecord(
    final Record record)
  {
    try {
      final var hash =
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(Hex.decodeHex(record.get(FIELD_USER_PASSWORD_HASH)))
          .setParameters(record.get(FIELD_USER_PASSWORD_PARAMS))
          .setAlgorithm(record.get(FIELD_USER_PASSWORD_ALGO))
          .build();

      return AccountsDatabaseUserDTO.builder()
        .setDisplayName(record.get(FIELD_USER_DISPLAY_NAME))
        .setEmail(record.get(FIELD_USER_EMAIL))
        .setId(record.get(FIELD_USER_ID))
        .setLocked(Optional.ofNullable(record.get(FIELD_USER_LOCKED_REASON)))
        .setPasswordHash(hash)
        .build();
    } catch (final DecoderException e) {
      throw new IllegalStateException(e);
    }
  }

  private static AccountsDatabaseException integrityException(
    final DerbySQLIntegrityConstraintViolationException e,
    final UUID id,
    final String displayName)
  {
    switch (e.getConstraintName()) {
      case "USER_DISPLAY_NAME_UNIQUE": {
        return new AccountsDatabaseException(
          ErrorSeverity.SEVERITY_ERROR,
          DISPLAY_NAME_ALREADY_USED,
          MessageFormat.format(localize("errorUserDisplayNameConflict"), displayName),
          e,
          TreeMap.of(localize("displayName"), displayName));
      }
      case "USER_ID_KEY": {
        return new AccountsDatabaseException(
          ErrorSeverity.SEVERITY_ERROR,
          ID_ALREADY_USED,
          localize("errorUserIDConflict"),
          e,
          TreeMap.of(localize("userID"), id.toString()));
      }
      default: {
        return invalidDataException(e);
      }
    }
  }

  private static AccountsDatabaseException invalidDataException(
    final Exception e)
  {
    return new AccountsDatabaseException(
      ErrorSeverity.SEVERITY_ERROR,
      INVALID_DATA,
      localize("errorUserDataInvalid"),
      e);
  }

  private static AccountsDatabaseException genericDatabaseException(
    final Exception e)
  {
    return new AccountsDatabaseException(
      ErrorSeverity.SEVERITY_ERROR,
      DATABASE_ERROR,
      MessageFormat.format(localize("errorDatabase"), e.getLocalizedMessage()),
      e);
  }

  private static String localize(final String resource)
  {
    return RESOURCES.getString(resource);
  }

  @Override
  public AccountsDatabaseUserDTO accountCreate(
    final UUID id,
    final String displayName,
    final AccountsDatabasePasswordHashDTO password,
    final String email,
    final Optional<String> lockedReason)
    throws AccountsDatabaseException
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
      this.audit.logAuditEvent(USER_CREATED, id, displayName, "", "");
    } catch (final Exception e) {
      throw genericDatabaseException(e);
    }

    return AccountsDatabaseUserDTO.builder()
      .setId(id)
      .setDisplayName(displayName)
      .setPasswordHash(password)
      .setEmail(email)
      .setLocked(lockedReason)
      .build();
  }

  @Override
  public AccountsDatabaseUserDTO accountUpdate(
    final UUID caller,
    final AccountsDatabaseUserDTO account)
    throws AccountsDatabaseException
  {
    Objects.requireNonNull(caller, "caller");
    Objects.requireNonNull(account, "account");

    final var existing = this.accountGet(account.id());

    try (var query =
           this.dslContext.update(TABLE_USERS)
             .set(FIELD_USER_DISPLAY_NAME, account.displayName())
             .set(FIELD_USER_EMAIL, account.email())
             .set(FIELD_USER_LOCKED_REASON, account.locked().orElse(null))
             .set(FIELD_USER_PASSWORD_ALGO, account.passwordHash().algorithm())
             .set(FIELD_USER_PASSWORD_HASH, toHex(account.passwordHash().hash()))
             .set(FIELD_USER_PASSWORD_PARAMS, account.passwordHash().parameters())
             .where(FIELD_USER_ID.eq(account.id()))) {
      query.execute();
    } catch (final DataAccessException e) {
      throw handleDataAccessException(account.id(), account.displayName(), e);
    }

    if (!Objects.equals(existing.displayName(), account.displayName())) {
      try {
        this.audit.logAuditEvent(
          USER_MODIFIED_DISPLAY_NAME,
          caller,
          account.id().toString(),
          existing.displayName(),
          account.displayName());
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }

    if (!Objects.equals(existing.email(), account.email())) {
      try {
        this.audit.logAuditEvent(
          USER_MODIFIED_EMAIL,
          caller,
          account.id().toString(),
          existing.email(),
          account.email());
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }

    if (!Objects.equals(existing.passwordHash(), account.passwordHash())) {
      try {
        this.audit.logAuditEvent(
          USER_MODIFIED_PASSWORD,
          caller,
          account.id().toString(),
          "",
          "");
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }

    if (!Objects.equals(existing.locked(), account.locked())) {
      try {
        this.audit.logAuditEvent(
          USER_MODIFIED_LOCKED,
          caller,
          account.id().toString(),
          existing.locked().orElse(""),
          account.locked().orElse(""));
      } catch (final Exception e) {
        throw genericDatabaseException(e);
      }
    }

    return account;
  }

  @Override
  public AccountsDatabaseUserDTO accountGet(
    final UUID userId)
    throws AccountsDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    try (var query = this.dslContext.select(
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

      final var result = query.fetch();
      if (result.isEmpty()) {
        throw new AccountsDatabaseException(
          ErrorSeverity.SEVERITY_ERROR,
          NONEXISTENT,
          localize("errorUserNonexistent"),
          null,
          TreeMap.of(localize("userID"), userId.toString()));
      }

      return accountFromRecord(result.get(0));
    }
  }

  @Override
  public Stream<AccountsDatabaseUserDTO> accountFind(
    final Optional<UUID> userId,
    final Optional<String> displayName,
    final Optional<String> email)
    throws AccountsDatabaseException
  {
    Objects.requireNonNull(userId, "userId");
    Objects.requireNonNull(displayName, "displayName");
    Objects.requireNonNull(email, "email");

    final var conditions = new ArrayList<Condition>(3);
    userId.ifPresent(
      uuid -> conditions.add(FIELD_USER_ID.eq(uuid)));
    displayName.ifPresent(
      cmpDisplayName -> conditions.add(FIELD_USER_DISPLAY_NAME.eq(cmpDisplayName)));
    email.ifPresent(
      cmpEmail -> conditions.add(FIELD_USER_EMAIL.eq(cmpEmail)));

    try {
      return this.dslContext.select(
        FIELD_USER_DISPLAY_NAME,
        FIELD_USER_EMAIL,
        FIELD_USER_ID,
        FIELD_USER_LOCKED_REASON,
        FIELD_USER_PASSWORD_HASH,
        FIELD_USER_PASSWORD_PARAMS,
        FIELD_USER_PASSWORD_ALGO)
        .from(TABLE_USERS)
        .where(conditions)
        .fetchStream()
        .map(CoreAccountsDatabaseQueries::accountFromRecord);
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

}
