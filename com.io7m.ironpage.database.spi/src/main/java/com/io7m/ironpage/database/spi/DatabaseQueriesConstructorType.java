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


package com.io7m.ironpage.database.spi;

import io.reactivex.rxjava3.subjects.Subject;

import java.sql.Connection;
import java.time.Clock;

/**
 * The type of functions that construct database queries.
 *
 * @param <T> The precise type of queries
 */

public interface DatabaseQueriesConstructorType<T extends DatabaseQueriesType>
{
  /**
   * Construct queries.
   *
   * @param clock      The clock used for time-based operations
   * @param events     The event subject used to publish events
   * @param connection The SQL database connection
   *
   * @return A new set of queries
   */

  T create(
    Clock clock,
    Subject<DatabaseEventType> events,
    Connection connection);
}
