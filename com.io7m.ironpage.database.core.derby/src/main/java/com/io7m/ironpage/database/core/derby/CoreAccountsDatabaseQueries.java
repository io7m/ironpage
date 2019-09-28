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
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Stream;

final class CoreAccountsDatabaseQueries implements AccountsDatabaseQueriesType
{
  private static final ResourceBundle RESOURCES =
    ResourceBundle.getBundle("com.io7m.ironpage.database.core.derby.Messages");

  private final Connection connection;
  private final DSLContext dslContext;

  private static final Table<Record> TABLE_AUDIT =
    DSL.table(DSL.name("core", "audit"));
  private static final Field<Timestamp> FIELD_AUDIT_TIME =
    DSL.field(DSL.name("audit_time"), SQLDataType.TIMESTAMP);
  private static final Field<String> FIELD_AUDIT_TYPE =
    DSL.field(DSL.name("audit_type"), SQLDataType.VARCHAR(64));
  private static final Field<String> FIELD_AUDIT_ARG0 =
    DSL.field(DSL.name("audit_arg0"), SQLDataType.VARCHAR(256));
  private static final Field<String> FIELD_AUDIT_ARG1 =
    DSL.field(DSL.name("audit_arg1"), SQLDataType.VARCHAR(256));
  private static final Field<String> FIELD_AUDIT_ARG2 =
    DSL.field(DSL.name("audit_arg2"), SQLDataType.VARCHAR(256));
  private static final Field<String> FIELD_AUDIT_ARG3 =
    DSL.field(DSL.name("audit_arg3"), SQLDataType.VARCHAR(256));

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
    DSL.field(DSL.name("user_password_hash"), SQLDataType.VARCHAR(64));
  private static final Field<String> FIELD_USER_PASSWORD_SALT =
    DSL.field(DSL.name("user_password_salt"), SQLDataType.VARCHAR(64));
  private static final Field<String> FIELD_USER_PASSWORD_ALGO =
    DSL.field(DSL.name("user_password_algo"), SQLDataType.VARCHAR(64));

  CoreAccountsDatabaseQueries(
    final Connection inConnection)
  {
    this.connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(this.connection, SQLDialect.DERBY, settings);
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
      .set(FIELD_USER_PASSWORD_SALT, toHex(password.salt()))
      .set(FIELD_USER_PASSWORD_HASH, toHex(password.hash()))) {
      query.execute();
    } catch (final DataAccessException e) {
      throw handleDataAccessException(id, displayName, e);
    }

    this.logAuditEvent("USER_CREATE", id, displayName, "");

    return AccountsDatabaseUserDTO.builder()
      .setId(id)
      .setDisplayName(displayName)
      .setPasswordHash(password)
      .setEmail(email)
      .setLocked(lockedReason)
      .build();
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

  @Override
  public AccountsDatabaseUserDTO accountUpdate(
    final AccountsDatabaseUserDTO account)
    throws AccountsDatabaseException
  {
    Objects.requireNonNull(account, "account");

    final var existing = this.accountGet(account.id());

    try (var query =
           this.dslContext.update(TABLE_USERS)
             .set(FIELD_USER_DISPLAY_NAME, account.displayName())
             .set(FIELD_USER_EMAIL, account.email())
             .set(FIELD_USER_LOCKED_REASON, account.locked().orElse(null))
             .set(FIELD_USER_PASSWORD_ALGO, account.passwordHash().algorithm())
             .set(FIELD_USER_PASSWORD_HASH, toHex(account.passwordHash().hash()))
             .set(FIELD_USER_PASSWORD_SALT, toHex(account.passwordHash().salt()))
             .where(FIELD_USER_ID.eq(account.id()))) {
      query.execute();
    } catch (final DataAccessException e) {
      throw handleDataAccessException(account.id(), account.displayName(), e);
    }

    if (!Objects.equals(existing.displayName(), account.displayName())) {
      this.logAuditEvent(
        "USER_MODIFY_DISPLAY_NAME",
        account.id(),
        existing.displayName(),
        account.displayName());
    }

    if (!Objects.equals(existing.email(), account.email())) {
      this.logAuditEvent(
        "USER_MODIFY_EMAIL",
        account.id(),
        existing.email(),
        account.email());
    }

    if (!Objects.equals(existing.passwordHash(), account.passwordHash())) {
      this.logAuditEvent(
        "USER_MODIFY_PASSWORD",
        account.id(),
        "",
        "");
    }

    if (!Objects.equals(existing.locked(), account.locked())) {
      this.logAuditEvent(
        "USER_MODIFY_LOCKED",
        account.id(),
        existing.locked().orElse(""),
        account.locked().orElse(""));
    }

    return account;
  }

  private static String toHex(final byte[] data)
  {
    return Hex.encodeHexString(data, true);
  }

  private void logAuditEvent(
    final String eventType,
    final UUID userId,
    final String arg1,
    final String arg2)
    throws AccountsDatabaseException
  {
    try (var query = this.dslContext.insertInto(TABLE_AUDIT)
      .set(FIELD_AUDIT_TIME, Timestamp.from(Instant.now()))
      .set(FIELD_AUDIT_TYPE, eventType)
      .set(FIELD_AUDIT_ARG0, userId.toString())
      .set(FIELD_AUDIT_ARG1, arg1)
      .set(FIELD_AUDIT_ARG2, arg2)
      .set(FIELD_AUDIT_ARG3, "")) {
      query.execute();
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public AccountsDatabaseUserDTO accountGet(final UUID userId)
    throws AccountsDatabaseException
  {
    Objects.requireNonNull(userId, "userId");

    try (var query = this.dslContext.select(
      FIELD_USER_DISPLAY_NAME,
      FIELD_USER_EMAIL,
      FIELD_USER_ID,
      FIELD_USER_LOCKED_REASON,
      FIELD_USER_PASSWORD_HASH,
      FIELD_USER_PASSWORD_SALT,
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
        FIELD_USER_PASSWORD_SALT,
        FIELD_USER_PASSWORD_ALGO)
        .from(TABLE_USERS)
        .where(conditions)
        .fetchStream()
        .map(CoreAccountsDatabaseQueries::accountFromRecord);
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  private static AccountsDatabaseUserDTO accountFromRecord(
    final Record record)
  {
    try {
      final var hash =
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(Hex.decodeHex(record.get(FIELD_USER_PASSWORD_HASH)))
          .setSalt(Hex.decodeHex(record.get(FIELD_USER_PASSWORD_SALT)))
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

}
