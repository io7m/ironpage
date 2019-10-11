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
import com.io7m.ironpage.presentable.api.PresentableAttributes;
import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.io7m.ironpage.errors.api.ErrorSeverity.SEVERITY_ERROR;

final class CoreLabelsQueries implements CDLabelsQueriesType
{
  private final DSLContext dslContext;

  CoreLabelsQueries(
    final Clock inClock,
    final Connection inConnection)
  {
    final var connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(connection, SQLDialect.DERBY, settings);
  }

  private static CDException handleUpdateException(
    final String name,
    final Exception e)
  {
    final var cause = e.getCause();
    if (cause instanceof DerbySQLIntegrityConstraintViolationException) {
      switch (((DerbySQLIntegrityConstraintViolationException) cause).getConstraintName()) {
        case "LABEL_NAME_UNIQUE": {
          return new CDException(
            SEVERITY_ERROR,
            LABEL_ALREADY_EXISTS,
            CoreMessages.localize("errorLabelAlreadyExists", name),
            e,
            PresentableAttributes.one(CoreMessages.localize("label"), name));
        }
        default: {
          break;
        }
      }
    }

    return genericDatabaseException(e);
  }

  private static CDSecurityLabelDTO labelFromRecord(
    final Record3<Long, String, String> record)
  {
    return CDSecurityLabelDTO.builder()
      .setId(record.getValue(CoreTables.FIELD_LABEL_ID).longValue())
      .setDescription(record.getValue(CoreTables.FIELD_LABEL_DESCRIPTION))
      .setName(record.getValue(CoreTables.FIELD_LABEL_NAME))
      .build();
  }

  private static CDException genericDatabaseException(
    final Exception e)
  {
    return new CDException(
      SEVERITY_ERROR,
      DATABASE_ERROR,
      CoreMessages.localize("errorDatabase", e.getLocalizedMessage()),
      e);
  }

  @Override
  public CDSecurityLabelDTO labelCreate(
    final String name,
    final String description)
    throws CDException
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(description, "description");

    try (var query =
           this.dslContext.insertInto(CoreTables.TABLE_LABELS)
             .set(CoreTables.FIELD_LABEL_NAME, name)
             .set(CoreTables.FIELD_LABEL_DESCRIPTION, description)) {
      query.execute();
    } catch (final DataAccessException e) {
      throw handleUpdateException(name, e);
    }

    return this.labelGetForName(name).get();
  }

  @Override
  public Optional<CDSecurityLabelDTO> labelGet(
    final long id)
    throws CDException
  {
    try (var query =
           this.dslContext.select(
             CoreTables.FIELD_LABEL_ID,
             CoreTables.FIELD_LABEL_NAME,
             CoreTables.FIELD_LABEL_DESCRIPTION)
             .from(CoreTables.TABLE_LABELS)
             .where(CoreTables.FIELD_LABEL_ID.eq(Long.valueOf(id)))) {
      final var rows = query.fetch();
      if (rows.size() != 1) {
        return Optional.empty();
      }
      return Optional.of(labelFromRecord(rows.get(0)));
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public Optional<CDSecurityLabelDTO> labelGetForName(
    final String name)
    throws CDException
  {
    Objects.requireNonNull(name, "name");

    try (var query =
           this.dslContext.select(
             CoreTables.FIELD_LABEL_ID,
             CoreTables.FIELD_LABEL_NAME,
             CoreTables.FIELD_LABEL_DESCRIPTION)
             .from(CoreTables.TABLE_LABELS)
             .where(CoreTables.FIELD_LABEL_NAME.eq(name))) {
      return Optional.of(labelFromRecord(query.fetchOne()));
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }

  @Override
  public CDSecurityLabelDTO labelUpdate(
    final CDSecurityLabelDTO label)
    throws CDException
  {
    Objects.requireNonNull(label, "label");

    final var idBox = Long.valueOf(label.id());
    try (var query =
           this.dslContext.update(CoreTables.TABLE_LABELS)
             .set(CoreTables.FIELD_LABEL_NAME, label.name())
             .set(CoreTables.FIELD_LABEL_DESCRIPTION, label.description())
             .where(CoreTables.FIELD_LABEL_ID.eq(idBox))) {
      final var results = query.execute();
      if (results != 1) {
        throw new CDException(
          SEVERITY_ERROR,
          LABEL_NONEXISTENT,
          CoreMessages.localize("errorLabelNonexistent", idBox),
          null,
          PresentableAttributes.one(CoreMessages.localize("labelID"), idBox.toString())
        );
      }
      return label;
    } catch (final DataAccessException e) {
      throw handleUpdateException(label.name(), e);
    }
  }

  @Override
  public Stream<CDSecurityLabelDTO> labelList()
    throws CDException
  {
    try (var query =
           this.dslContext.select(
             CoreTables.FIELD_LABEL_ID,
             CoreTables.FIELD_LABEL_NAME,
             CoreTables.FIELD_LABEL_DESCRIPTION)
             .from(CoreTables.TABLE_LABELS)
             .orderBy(CoreTables.FIELD_LABEL_ID.asc())) {
      return query.fetchStream()
        .map(CoreLabelsQueries::labelFromRecord);
    } catch (final DataAccessException e) {
      throw genericDatabaseException(e);
    }
  }
}
