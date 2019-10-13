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

import com.io7m.ironpage.database.audit.api.AuditDatabaseEventDTO;
import com.io7m.ironpage.database.audit.api.AuditDatabaseQueriesType;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.events.api.EventType;
import io.reactivex.rxjava3.subjects.Subject;
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
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

final class CoreAuditQueries implements AuditDatabaseQueriesType
{
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

  private final DSLContext dslContext;
  private final Clock clock;

  CoreAuditQueries(
    final Clock inClock,
    final Subject<? extends EventType> events,
    final Connection inConnection)
  {
    this.clock = Objects.requireNonNull(inClock, "clock");
    final var connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(connection, SQLDialect.DERBY, settings);
  }

  private static AuditDatabaseEventDTO eventFromRecord(
    final Record record)
  {
    return AuditDatabaseEventDTO.builder()
      .setEventType(record.getValue(FIELD_AUDIT_TYPE))
      .setTime(record.getValue(FIELD_AUDIT_TIME).toInstant())
      .setArgument0(record.getValue(FIELD_AUDIT_ARG0))
      .setArgument1(record.getValue(FIELD_AUDIT_ARG1))
      .setArgument2(record.getValue(FIELD_AUDIT_ARG2))
      .setArgument3(record.getValue(FIELD_AUDIT_ARG3))
      .build();
  }

  @Override
  public Stream<AuditDatabaseEventDTO> auditEventsDuring(
    final Instant timeFrom,
    final Instant timeTo)
    throws DatabaseException
  {
    Objects.requireNonNull(timeFrom, "from");
    Objects.requireNonNull(timeTo, "to");

    final var tsFrom = Timestamp.from(timeFrom);
    final var tsTo = Timestamp.from(timeTo);

    try {
      return this.dslContext.select(
        FIELD_AUDIT_TYPE,
        FIELD_AUDIT_TIME,
        FIELD_AUDIT_ARG0,
        FIELD_AUDIT_ARG1,
        FIELD_AUDIT_ARG2,
        FIELD_AUDIT_ARG3)
        .from(TABLE_AUDIT)
        .where(FIELD_AUDIT_TIME.ge(tsFrom), FIELD_AUDIT_TIME.le(tsTo))
        .orderBy(FIELD_AUDIT_TIME.asc())
        .fetchStream()
        .map(CoreAuditQueries::eventFromRecord);
    } catch (final DataAccessException e) {
      throw new DatabaseException(ErrorSeverity.SEVERITY_ERROR, e.getLocalizedMessage(), e);
    }
  }

  @Override
  public void auditEventLog(
    final String eventType,
    final String arg0,
    final String arg1,
    final String arg2,
    final String arg3)
    throws DatabaseException
  {
    Objects.requireNonNull(eventType, "eventType");
    Objects.requireNonNull(arg0, "arg0");
    Objects.requireNonNull(arg1, "arg1");
    Objects.requireNonNull(arg2, "arg2");
    Objects.requireNonNull(arg3, "arg3");

    try (var query = this.dslContext.insertInto(TABLE_AUDIT)
      .set(FIELD_AUDIT_TIME, Timestamp.from(this.clock.instant()))
      .set(FIELD_AUDIT_TYPE, eventType)
      .set(FIELD_AUDIT_ARG0, arg0)
      .set(FIELD_AUDIT_ARG1, arg1)
      .set(FIELD_AUDIT_ARG2, arg2)
      .set(FIELD_AUDIT_ARG3, arg3)) {
      query.execute();
    } catch (final DataAccessException e) {
      throw new DatabaseException(ErrorSeverity.SEVERITY_ERROR, e.getLocalizedMessage(), e);
    }
  }
}
