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

package com.io7m.ironpage.database.audit.api;

import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.database.spi.DatabaseQueriesType;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The queries supported by audit databases.
 */

public interface AuditDatabaseQueriesType extends DatabaseQueriesType
{
  /**
   * Retrieve audit events that occurred within the given time range from the database.
   *
   * @param timeFrom The lower bound (inclusive) of the time range
   * @param timeTo   The upper bound (inclusive) of the time range
   *
   * @return A stream of events
   *
   * @throws DatabaseException On errors
   */

  Stream<AuditDatabaseEventDTO> auditEventsDuring(
    Instant timeFrom,
    Instant timeTo)
    throws DatabaseException;

  /**
   * Log an audit event.
   *
   * @param eventType The event type
   * @param arg0      Argument 0
   * @param arg1      Argument 1
   * @param arg2      Argument 2
   * @param arg3      Argument 3
   *
   * @throws DatabaseException On errors
   */

  default void auditEventLog(
    final AuditEventType eventType,
    final String arg0,
    final String arg1,
    final String arg2,
    final String arg3)
    throws DatabaseException
  {
    this.auditEventLog(eventType.toString(), arg0, arg1, arg2, arg3);
  }

  /**
   * Log an audit event.
   *
   * @param eventType The event type
   * @param arg0      Argument 0
   * @param arg1      Argument 1
   * @param arg2      Argument 2
   * @param arg3      Argument 3
   *
   * @throws DatabaseException On errors
   */

  void auditEventLog(
    String eventType,
    String arg0,
    String arg1,
    String arg2,
    String arg3)
    throws DatabaseException;

  /**
   * Log an audit event.
   *
   * @param eventType The event type
   * @param owner     The event owner
   * @param arg1      Argument 1
   * @param arg2      Argument 2
   * @param arg3      Argument 3
   *
   * @throws DatabaseException On errors
   */

  default void auditEventLog(
    final String eventType,
    final UUID owner,
    final String arg1,
    final String arg2,
    final String arg3)
    throws DatabaseException
  {
    this.auditEventLog(eventType, owner.toString(), arg1, arg2, arg3);
  }

  /**
   * Log an audit event.
   *
   * @param eventType The event type
   * @param owner     The event owner
   * @param arg1      Argument 1
   * @param arg2      Argument 2
   * @param arg3      Argument 3
   *
   * @throws DatabaseException On errors
   */

  default void auditEventLog(
    final AuditEventType eventType,
    final UUID owner,
    final String arg1,
    final String arg2,
    final String arg3)
    throws DatabaseException
  {
    this.auditEventLog(eventType.name(), owner.toString(), arg1, arg2, arg3);
  }
}
