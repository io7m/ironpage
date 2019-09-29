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
import com.io7m.ironpage.errors.api.ErrorSeverity;
import io.vavr.collection.TreeMap;
import org.apache.commons.codec.binary.Hex;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record4;
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
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.ResourceBundle;

final class CorePagesDatabaseQueries implements PagesDatabaseQueriesType
{
  private static final ResourceBundle RESOURCES =
    ResourceBundle.getBundle("com.io7m.ironpage.database.core.derby.Messages");

  private final Connection connection;
  private final DSLContext dslContext;

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

  CorePagesDatabaseQueries(
    final Connection inConnection)
  {
    this.connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(this.connection, SQLDialect.DERBY, settings);
  }

  @Override
  public String pageBlobPut(
    final String mediaType,
    final byte[] data)
    throws PagesDatabaseException
  {
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(data, "data");

    final var hash = hashOf(data);

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

    try (var query =
           this.dslContext.insertInto(TABLE_BLOBS)
             .set(FIELD_BLOB_ID, hash)
             .set(FIELD_BLOB_DATA, data)
             .set(FIELD_BLOB_MEDIA_TYPE, mediaType)
             .set(FIELD_BLOB_REDACTION, (Long) null)) {
      query.execute();
    } catch (final DataAccessException e) {
      final var cause = e.getCause();
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

    return hash;
  }

  @Override
  public Optional<PagesDatabaseBlobDTO> pageBlobGet(final String id)
    throws PagesDatabaseException
  {
    Objects.requireNonNull(id, "id");

    try (var query =
           this.dslContext.select(
             FIELD_BLOB_ID,
             FIELD_BLOB_MEDIA_TYPE,
             FIELD_BLOB_DATA,
             FIELD_BLOB_REDACTION)
             .from(TABLE_BLOBS)
             .where(FIELD_BLOB_ID.eq(id))
             .limit(1)) {
      final var results = query.fetch();
      if (results.isNotEmpty()) {
        return dataFromRecord(results.get(0));
      }
      return Optional.empty();
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  private static Optional<PagesDatabaseBlobDTO> dataFromRecord(
    final Record4<String, String, byte[], Long> record)
  {
    final OptionalLong redactionOpt;
    final var redaction = record.get(FIELD_BLOB_REDACTION);
    if (redaction == null) {
      redactionOpt = OptionalLong.empty();
    } else {
      redactionOpt = OptionalLong.of(redaction.longValue());
    }
    return Optional.of(
      PagesDatabaseBlobDTO.builder()
        .setId(record.get(FIELD_BLOB_ID))
        .setMediaType(record.get(FIELD_BLOB_MEDIA_TYPE))
        .setRedaction(redactionOpt)
        .setData(record.get(FIELD_BLOB_DATA))
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
}
