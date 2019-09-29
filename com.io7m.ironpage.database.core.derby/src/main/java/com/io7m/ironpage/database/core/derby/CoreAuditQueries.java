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

import com.io7m.ironpage.database.audit.api.AuditDatabaseQueriesType;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.errors.api.ErrorSeverity;
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
import java.util.Objects;

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

  private final Connection connection;
  private final DSLContext dslContext;
  private final Clock clock;

  CoreAuditQueries(
    final Clock inClock,
    final Connection inConnection)
  {
    this.clock = Objects.requireNonNull(inClock, "clock");
    this.connection = Objects.requireNonNull(inConnection, "connection");
    final var settings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
    this.dslContext = DSL.using(this.connection, SQLDialect.DERBY, settings);
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
