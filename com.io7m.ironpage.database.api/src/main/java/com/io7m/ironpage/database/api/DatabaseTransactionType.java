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

package com.io7m.ironpage.database.api;

import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.database.spi.DatabaseQueriesType;

/**
 * A database transaction.
 */

public interface DatabaseTransactionType
  extends AutoCloseable
{
  /**
   * Close this transaction. The transaction is rolled back if {@link #commit()} has not been
   * called, or if changes have been made since the last call to {@link #commit()}.
   *
   * @throws DatabaseException On errors
   */

  @Override
  void close()
    throws DatabaseException;

  /**
   * Commit any changes made.
   *
   * @throws DatabaseException On errors
   */

  void commit()
    throws DatabaseException;

  /**
   * Roll back any changes made since the last call to {@link #commit()} (or everything, if {@link
   * #commit()} has never been called).
   *
   * @throws DatabaseException On errors
   */

  void rollback()
    throws DatabaseException;

  /**
   * @param queriesClass The precise type of queries to execute in the transaction
   * @param <P>          The precise type of queries to execute
   *
   * @return The transaction queries
   *
   * @throws DatabaseException On errors
   */

  <P extends DatabaseQueriesType>
  P queries(Class<P> queriesClass)
    throws DatabaseException;
}
