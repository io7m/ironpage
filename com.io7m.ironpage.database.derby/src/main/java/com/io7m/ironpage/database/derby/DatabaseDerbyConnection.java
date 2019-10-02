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

package com.io7m.ironpage.database.derby;

import com.io7m.ironpage.database.api.DatabaseConnectionType;
import com.io7m.ironpage.database.api.DatabaseTransactionType;
import com.io7m.ironpage.database.spi.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

final class DatabaseDerbyConnection implements DatabaseConnectionType
{
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseDerbyConnection.class);

  private final Connection connection;
  private final DatabaseDerby database;

  DatabaseDerbyConnection(
    final DatabaseDerby inDatabase,
    final Connection inConnection)
  {
    this.database =
      Objects.requireNonNull(inDatabase, "inDatabase");
    this.connection =
      Objects.requireNonNull(inConnection, "inConnection");
  }

  @Override
  public void close()
    throws DatabaseException
  {
    try {
      LOG.trace("close");
      this.connection.close();
    } catch (final SQLException e) {
      throw DatabaseDerbyProvider.ofSQLException("errorCloseConnection", e);
    }
  }

  @Override
  public DatabaseTransactionType beginTransaction()
  {
    return new DatabaseDerbyTransaction(this);
  }

  DatabaseDerby database()
  {
    return this.database;
  }

  Connection sqlConnection()
  {
    return this.connection;
  }
}
