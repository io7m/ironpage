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

import com.io7m.ironpage.database.core.api.CDException;
import com.io7m.ironpage.database.core.api.CDLabelsQueriesType;
import com.io7m.ironpage.database.core.api.CDSecurityLabelDTO;
import com.io7m.ironpage.database.pages.api.PagesDatabaseBlobDTO;
import com.io7m.ironpage.database.pages.api.PagesDatabaseQueriesType;
import com.io7m.ironpage.database.pages.api.PagesDatabaseRedactionDTO;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import io.vavr.collection.TreeMap;
import org.apache.commons.codec.binary.Hex;
import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.Record8;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLDataException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.io7m.ironpage.database.audit.api.AuditEventKind.BLOB_CREATED;
import static com.io7m.ironpage.database.audit.api.AuditEventKind.BLOB_REDACTED;

final class CorePagesQueries implements PagesDatabaseQueriesType
{
  private final Connection connection;
  private final DSLContext dslContext;
  private final CoreAuditQueries audit;
  private final Clock clock;

  CorePagesQueries(
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
      .setId(redactionRecord.getValue(CoreTables.FIELD_REDACTION_ID).longValue())
      .setOwner(redactionRecord.getValue(CoreTables.FIELD_REDACTION_USER_ID))
      .setTime(redactionRecord.getValue(CoreTables.FIELD_REDACTION_TIME).toInstant())
      .setReason(redactionRecord.getValue(CoreTables.FIELD_REDACTION_REASON))
      .build();
  }

  private static Optional<PagesDatabaseBlobDTO> dataFromRecord(
    final Record8<String, String, byte[], UUID, Long, Long, String, String> record,
    final Optional<PagesDatabaseRedactionDTO> redaction)
  {
    final var label =
      CDSecurityLabelDTO.builder()
        .setId(record.<Long>getValue(CoreTables.FIELD_LABEL_ID).longValue())
        .setName(record.getValue(CoreTables.FIELD_LABEL_NAME))
        .setDescription(record.getValue(CoreTables.FIELD_LABEL_DESCRIPTION))
        .build();

    return Optional.of(
      PagesDatabaseBlobDTO.builder()
        .setId(record.get(CoreTables.FIELD_BLOB_ID))
        .setMediaType(record.get(CoreTables.FIELD_BLOB_MEDIA_TYPE))
        .setRedaction(redaction)
        .setData(record.get(CoreTables.FIELD_BLOB_DATA))
        .setOwner(record.get(CoreTables.FIELD_BLOB_OWNER))
        .setSecurityLabel(label)
        .build());
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

  private static CDException genericDatabaseExceptionFormatted(
    final String resource,
    final Object... args)
  {
    return new CDException(
      ErrorSeverity.SEVERITY_ERROR,
      DATABASE_ERROR,
      CoreMessages.localize(resource, args),
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
    final byte[] data,
    final CDSecurityLabelDTO securityLabel)
    throws CDException
  {
    Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(data, "data");
    Objects.requireNonNull(securityLabel, "securityLabel");

    final var hash = hashOf(data);
    this.checkBlobDoesNotExist(hash);

    try (var query =
           this.dslContext.insertInto(CoreTables.TABLE_BLOBS)
             .set(CoreTables.FIELD_BLOB_ID, hash)
             .set(CoreTables.FIELD_BLOB_DATA, data)
             .set(CoreTables.FIELD_BLOB_MEDIA_TYPE, mediaType)
             .set(CoreTables.FIELD_BLOB_OWNER, owner)
             .set(CoreTables.FIELD_BLOB_SECURITY_LABEL, Long.valueOf(securityLabel.id()))
             .set(CoreTables.FIELD_BLOB_REDACTION, (Long) null)) {
      query.execute();
    } catch (final DataAccessException e) {

      /*
       * An integrity violation exception will be raised if the blob refers to a user that
       * does not exist.
       */

      final var cause = e.getCause();
      if (cause instanceof DerbySQLIntegrityConstraintViolationException) {
        final var integrity = (DerbySQLIntegrityConstraintViolationException) cause;
        switch (integrity.getConstraintName()) {
          case "BLOB_OWNER_REFERENCE": {
            throw new CDException(
              ErrorSeverity.SEVERITY_ERROR,
              PagesDatabaseQueriesType.DATA_OWNER_NONEXISTENT,
              CoreMessages.localize("errorPageDataOwnerNonexistent"),
              e,
              TreeMap.of(CoreMessages.localize("userID"), owner.toString()));
          }
          case "BLOB_LABEL_REFERENCE": {
            throw new CDException(
              ErrorSeverity.SEVERITY_ERROR,
              CDLabelsQueriesType.LABEL_NONEXISTENT,
              CoreMessages.localize("errorLabelNonexistent"),
              e,
              TreeMap.of(CoreMessages.localize("labelID"), Long.toString(securityLabel.id())));
          }
          default: {
            break;
          }
        }
      }

      /*
       * A truncation error will occur if the blob is too long.
       */

      if (cause instanceof SQLDataException) {
        final var dataCause = (SQLDataException) cause;
        if ("22001".equals(dataCause.getSQLState())) {
          throw new CDException(
            PagesDatabaseQueriesType.DATA_INVALID,
            CoreMessages.localize("errorPageDataInvalid"),
            e);
        }
      }
      throw genericDatabaseException(e);
    }

    try {
      this.audit.auditEventLog(BLOB_CREATED, owner, hash, securityLabel.name(), "");
    } catch (final Exception e) {
      throw genericDatabaseException(e);
    }

    return hash;
  }

  private void checkBlobDoesNotExist(
    final String hash)
    throws CDException
  {
    try (var query =
           this.dslContext.select(CoreTables.FIELD_BLOB_ID)
             .from(CoreTables.TABLE_BLOBS)
             .where(CoreTables.FIELD_BLOB_ID.eq(hash))
             .limit(1)) {
      final var count = query.execute();
      if (count > 0) {
        throw new CDException(
          ErrorSeverity.SEVERITY_ERROR,
          PagesDatabaseQueriesType.DATA_ALREADY_EXISTS,
          CoreMessages.localize("errorPageDataAlreadyExists"),
          null,
          TreeMap.of(CoreMessages.localize("dataHash"), hash));
      }
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public Optional<PagesDatabaseBlobDTO> pageBlobGet(
    final String id)
    throws CDException
  {
    Objects.requireNonNull(id, "id");

    try (var blobQuery =
           this.dslContext.select(
             CoreTables.FIELD_BLOB_ID,
             CoreTables.FIELD_BLOB_MEDIA_TYPE,
             CoreTables.FIELD_BLOB_DATA,
             CoreTables.FIELD_BLOB_OWNER,
             CoreTables.FIELD_BLOB_REDACTION,
             CoreTables.FIELD_LABEL_ID,
             CoreTables.FIELD_LABEL_NAME,
             CoreTables.FIELD_LABEL_DESCRIPTION)
             .from(CoreTables.TABLE_BLOBS)
             .join(CoreTables.TABLE_LABELS)
             .on(CoreTables.FIELD_LABEL_ID.eq(CoreTables.FIELD_BLOB_SECURITY_LABEL))
             .where(CoreTables.FIELD_BLOB_ID.eq(id))
             .limit(1)) {
      final var blobResults = blobQuery.fetch();
      if (blobResults.isEmpty()) {
        return Optional.empty();
      }

      final var blobRecord = blobResults.get(0);
      final var blobRedaction =
        Optional.ofNullable(blobRecord.getValue(CoreTables.FIELD_BLOB_REDACTION));

      return dataFromRecord(blobRecord, this.fetchRedactionOptionally(blobRedaction));
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  private Optional<PagesDatabaseRedactionDTO> fetchRedactionOptionally(
    final Optional<Long> id)
  {
    if (id.isEmpty()) {
      return Optional.empty();
    }
    return this.fetchRedaction(id.get());
  }

  private Optional<PagesDatabaseRedactionDTO> fetchRedaction(
    final Long id)
  {
    try (var redactionQuery =
           this.dslContext.select(
             CoreTables.FIELD_REDACTION_ID,
             CoreTables.FIELD_REDACTION_REASON,
             CoreTables.FIELD_REDACTION_TIME,
             CoreTables.FIELD_REDACTION_USER_ID)
             .from(CoreTables.TABLE_REDACTIONS)
             .where(CoreTables.FIELD_REDACTION_ID.eq(id))) {

      final var redactionRecord = redactionQuery.fetchOne();
      return Optional.of(redactionFromRecord(redactionRecord));
    }
  }

  @Override
  public void pageBlobRedact(
    final UUID caller,
    final String id,
    final String reason)
    throws CDException
  {
    Objects.requireNonNull(caller, "caller");
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(reason, "reason");

    /*
     * Add a redaction record.
     */

    final var timestamp = Timestamp.from(this.clock.instant());
    try (var query = this.dslContext.insertInto(CoreTables.TABLE_REDACTIONS)
      .set(CoreTables.FIELD_REDACTION_REASON, reason)
      .set(CoreTables.FIELD_REDACTION_TIME, timestamp)
      .set(CoreTables.FIELD_REDACTION_USER_ID, caller)) {
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
    try (var query = this.dslContext.select(CoreTables.FIELD_REDACTION_ID)
      .from(CoreTables.TABLE_REDACTIONS)
      .where(
        CoreTables.FIELD_REDACTION_REASON.eq(reason),
        CoreTables.FIELD_REDACTION_TIME.eq(timestamp),
        CoreTables.FIELD_REDACTION_USER_ID.eq(caller))
      .orderBy(CoreTables.FIELD_REDACTION_ID.asc())
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

    try (var query = this.dslContext.update(CoreTables.TABLE_BLOBS)
      .set(CoreTables.FIELD_BLOB_DATA, new byte[0])
      .set(CoreTables.FIELD_BLOB_REDACTION, redaction)
      .where(CoreTables.FIELD_BLOB_ID.eq(id))) {
      final var results = query.execute();
      if (results != 1) {
        throw new CDException(
          ErrorSeverity.SEVERITY_ERROR,
          PagesDatabaseQueriesType.DATA_NONEXISTENT,
          CoreMessages.localize("errorPageDataNonexistent"),
          null,
          TreeMap.of(CoreMessages.localize("dataHash"), id));
      }
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }
}
