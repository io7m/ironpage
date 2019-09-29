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

import com.io7m.ironpage.database.pages.api.PagesDatabaseBlobDTO;
import com.io7m.ironpage.database.pages.api.PagesDatabaseException;
import com.io7m.ironpage.database.pages.api.PagesDatabaseQueriesType;
import com.io7m.ironpage.database.pages.api.PagesDatabaseRedactionDTO;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import io.vavr.collection.TreeMap;
import org.apache.commons.codec.binary.Hex;
import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.Record5;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLDataException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import static com.io7m.ironpage.database.audit.api.AuditEventKind.BLOB_CREATED;
import static com.io7m.ironpage.database.audit.api.AuditEventKind.BLOB_REDACTED;

final class CorePagesDatabaseQueries implements PagesDatabaseQueriesType
{
  private static final ResourceBundle RESOURCES =
    ResourceBundle.getBundle("com.io7m.ironpage.database.core.derby.Messages");

  private static final Table<Record> TABLE_BLOBS =
    DSL.table(DSL.name("core", "blobs"));
  private static final Field<String> FIELD_BLOB_ID =
    DSL.field(DSL.name("blob_id"), SQLDataType.CHAR(64));
  private static final Field<byte[]> FIELD_BLOB_DATA =
    DSL.field(DSL.name("blob_data"), SQLDataType.BLOB(8_000_000));
  private static final Field<Long> FIELD_BLOB_REDACTION =
    DSL.field(DSL.name("blob_redaction"), SQLDataType.BIGINT);
  private static final Field<String> FIELD_BLOB_MEDIA_TYPE =
    DSL.field(DSL.name("blob_media_type"), SQLDataType.VARCHAR(128));
  private static final Field<UUID> FIELD_BLOB_OWNER =
    DSL.field(DSL.name("blob_owner"), SQLDataType.UUID);
  private static final Table<Record> TABLE_REDACTIONS =
    DSL.table(DSL.name("core", "redactions"));
  private static final Field<UUID> FIELD_REDACTION_USER_ID =
    DSL.field(DSL.name("redaction_user"), SQLDataType.UUID);
  private static final Field<Timestamp> FIELD_REDACTION_TIME =
    DSL.field(DSL.name("redaction_time"), SQLDataType.TIMESTAMP);
  private static final Field<String> FIELD_REDACTION_REASON =
    DSL.field(DSL.name("redaction_reason"), SQLDataType.VARCHAR(128));
  private static final Field<Long> FIELD_REDACTION_ID =
    DSL.field(DSL.name("redaction_id"), SQLDataType.BIGINT);

  private final Connection connection;
  private final DSLContext dslContext;
  private final CoreAuditQueries audit;
  private final Clock clock;

  CorePagesDatabaseQueries(
    final Clock inClock,
    final Connection inConnection)
  {
    this.clock = Objects.requireNonNull(inClock, "inClock");
    this.connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(this.connection, SQLDialect.DERBY, settings);
    this.audit = new CoreAuditQueries(this.clock, this.connection);
  }

  private static PagesDatabaseRedactionDTO redactionFromRecord(
    final Record4<Long, String, Timestamp, UUID> redactionRecord)
  {
    return PagesDatabaseRedactionDTO.builder()
      .setId(redactionRecord.getValue(FIELD_REDACTION_ID).longValue())
      .setOwner(redactionRecord.getValue(FIELD_REDACTION_USER_ID))
      .setTime(redactionRecord.getValue(FIELD_REDACTION_TIME).toInstant())
      .setReason(redactionRecord.getValue(FIELD_REDACTION_REASON))
      .build();
  }

  private static Optional<PagesDatabaseBlobDTO> dataFromRecord(
    final Record5<String, String, byte[], UUID, Long> record,
    final Optional<PagesDatabaseRedactionDTO> redaction)
  {
    return Optional.of(
      PagesDatabaseBlobDTO.builder()
        .setId(record.get(FIELD_BLOB_ID))
        .setMediaType(record.get(FIELD_BLOB_MEDIA_TYPE))
        .setRedaction(redaction)
        .setData(record.get(FIELD_BLOB_DATA))
        .setOwner(record.get(FIELD_BLOB_OWNER))
        .build());
  }

  private static String localize(
    final String resources)
  {
    return RESOURCES.getString(resources);
  }

  private static PagesDatabaseException genericDatabaseException(
    final Exception e)
  {
    return new PagesDatabaseException(
      ErrorSeverity.SEVERITY_ERROR,
      DATABASE_ERROR,
      MessageFormat.format(localize("errorDatabase"), e.getLocalizedMessage()),
      e);
  }

  private static PagesDatabaseException genericDatabaseExceptionFormatted(
    final String resource,
    final Object... args)
  {
    return new PagesDatabaseException(
      ErrorSeverity.SEVERITY_ERROR,
      DATABASE_ERROR,
      MessageFormat.format(localize(resource), args),
      null);
  }

  private static String hashOf(final byte[] data)
  {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    return Hex.encodeHexString(digest.digest(data), true);
  }

  @Override
  public String pageBlobPut(
    final UUID owner,
    final String mediaType,
    final byte[] data)
    throws PagesDatabaseException
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(data, "data");

    final var hash = hashOf(data);
    this.checkBlobDoesNotExist(hash);

    try (var query =
           this.dslContext.insertInto(TABLE_BLOBS)
             .set(FIELD_BLOB_ID, hash)
             .set(FIELD_BLOB_DATA, data)
             .set(FIELD_BLOB_MEDIA_TYPE, mediaType)
             .set(FIELD_BLOB_OWNER, owner)
             .set(FIELD_BLOB_REDACTION, (Long) null)) {
      query.execute();
    } catch (final DataAccessException e) {

      /*
       * An integrity violation exception will be raised if the blob refers to a user that
       * does not exist.
       */

      final var cause = e.getCause();
      if (cause instanceof DerbySQLIntegrityConstraintViolationException) {
        final var integrity = (DerbySQLIntegrityConstraintViolationException) cause;
        if (Objects.equals(integrity.getConstraintName(), "BLOB_OWNER_REFERENCE")) {
          throw new PagesDatabaseException(
            ErrorSeverity.SEVERITY_ERROR,
            PagesDatabaseQueriesType.DATA_OWNER_NONEXISTENT,
            localize("errorPageDataOwnerNonexistent"),
            e,
            TreeMap.of(localize("userID"), owner.toString()));
        }
      }

      /*
       * A truncation error will occur if the blob is too long.
       */

      if (cause instanceof SQLDataException) {
        final var dataCause = (SQLDataException) cause;
        if ("22001".equals(dataCause.getSQLState())) {
          throw new PagesDatabaseException(
            PagesDatabaseQueriesType.DATA_INVALID,
            localize("errorPageDataInvalid"),
            e);
        }
      }
      throw genericDatabaseException(e);
    }

    try {
      this.audit.auditEventLog(BLOB_CREATED, owner, hash, "", "");
    } catch (final Exception e) {
      throw genericDatabaseException(e);
    }

    return hash;
  }

  private void checkBlobDoesNotExist(
    final String hash)
    throws PagesDatabaseException
  {
    try (var query =
           this.dslContext.select(FIELD_BLOB_ID)
             .from(TABLE_BLOBS)
             .where(FIELD_BLOB_ID.eq(hash))
             .limit(1)) {
      final var count = query.execute();
      if (count > 0) {
        throw new PagesDatabaseException(
          ErrorSeverity.SEVERITY_ERROR,
          PagesDatabaseQueriesType.DATA_ALREADY_EXISTS,
          localize("errorPageDataAlreadyExists"),
          null,
          TreeMap.of(localize("dataHash"), hash));
      }
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public Optional<PagesDatabaseBlobDTO> pageBlobGet(
    final String id)
    throws PagesDatabaseException
  {
    Objects.requireNonNull(id, "id");

    try (var blobQuery =
           this.dslContext.select(
             FIELD_BLOB_ID,
             FIELD_BLOB_MEDIA_TYPE,
             FIELD_BLOB_DATA,
             FIELD_BLOB_OWNER,
             FIELD_BLOB_REDACTION)
             .from(TABLE_BLOBS)
             .where(FIELD_BLOB_ID.eq(id))
             .limit(1)) {
      final var blobResults = blobQuery.fetch();
      Optional<PagesDatabaseRedactionDTO> redaction = Optional.empty();
      if (blobResults.isNotEmpty()) {
        final var blobRecord = blobResults.get(0);
        final var blobRedaction = blobRecord.getValue(FIELD_BLOB_REDACTION);
        if (blobRedaction != null) {
          redaction = this.fetchRedaction(blobRedaction);
        }
        return dataFromRecord(blobRecord, redaction);
      }
      return Optional.empty();
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  private Optional<PagesDatabaseRedactionDTO> fetchRedaction(final Long id)
  {
    try (var redactionQuery =
           this.dslContext.select(
             FIELD_REDACTION_ID,
             FIELD_REDACTION_REASON,
             FIELD_REDACTION_TIME,
             FIELD_REDACTION_USER_ID)
             .from(TABLE_REDACTIONS)
             .where(FIELD_REDACTION_ID.eq(id))) {

      final var redactionRecord = redactionQuery.fetchOne();
      return Optional.of(redactionFromRecord(redactionRecord));
    }
  }

  @Override
  public void pageBlobRedact(
    final UUID caller,
    final String id,
    final String reason)
    throws PagesDatabaseException
  {
    Objects.requireNonNull(caller, "caller");
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(reason, "reason");

    /*
     * Add a redaction record.
     */

    final var timestamp = Timestamp.from(this.clock.instant());
    try (var query = this.dslContext.insertInto(TABLE_REDACTIONS)
      .set(FIELD_REDACTION_REASON, reason)
      .set(FIELD_REDACTION_TIME, timestamp)
      .set(FIELD_REDACTION_USER_ID, caller)) {
      final var updates = query.execute();
      if (updates != 1) {
        throw genericDatabaseExceptionFormatted(
          "errorUpdatesUnexpected",
          Integer.valueOf(1),
          Integer.valueOf(updates));
      }
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }

    /*
     * Query the redaction record to fetch the ID. Unfortunately this needs to be done
     * in a separate query because Derby doesn't support any kind of returning INSERT.
     */

    final Long redaction;
    try (var query = this.dslContext.select(FIELD_REDACTION_ID)
      .from(TABLE_REDACTIONS)
      .where(
        FIELD_REDACTION_REASON.eq(reason),
        FIELD_REDACTION_TIME.eq(timestamp),
        FIELD_REDACTION_USER_ID.eq(caller))
      .orderBy(FIELD_REDACTION_ID.asc())
      .limit(1)) {
      redaction = query.fetchOne().value1();
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }

    try {
      this.audit.auditEventLog(BLOB_REDACTED, caller, id, String.valueOf(redaction), "");
    } catch (final DatabaseException e) {
      throw genericDatabaseException(e);
    }

    /*
     * Zero out the blob and update the blob redaction field.
     */

    try (var query = this.dslContext.update(TABLE_BLOBS)
      .set(FIELD_BLOB_DATA, new byte[0])
      .set(FIELD_BLOB_REDACTION, redaction)
      .where(FIELD_BLOB_ID.eq(id))) {
      final var results = query.execute();
      if (results != 1) {
        throw new PagesDatabaseException(
          ErrorSeverity.SEVERITY_ERROR,
          PagesDatabaseQueriesType.DATA_NONEXISTENT,
          localize("errorPageDataNonexistent"),
          null,
          TreeMap.of(localize("dataHash"), id));
      }
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }
}
