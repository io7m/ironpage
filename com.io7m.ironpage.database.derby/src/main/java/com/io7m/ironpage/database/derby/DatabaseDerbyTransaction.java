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

import com.io7m.ironpage.database.api.DatabaseTransactionType;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.database.spi.DatabaseQueriesType;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.presentable.api.PresentableAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Objects;

final class DatabaseDerbyTransaction implements DatabaseTransactionType
{
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseDerbyTransaction.class);

  private final DatabaseDerbyConnection connection;

  DatabaseDerbyTransaction(
    final DatabaseDerbyConnection inConnection)
  {
    this.connection = Objects.requireNonNull(inConnection, "connection");
  }

  @Override
  public void close()
    throws DatabaseException
  {
    this.rollback();
  }

  @Override
  public void commit()
    throws DatabaseException
  {
    try {
      LOG.trace("commit");
      this.connection.sqlConnection().commit();
    } catch (final SQLException e) {
      throw DatabaseDerbyProvider.ofSQLException("errorConnectionCommit", e);
    }
  }

  @Override
  public void rollback()
    throws DatabaseException
  {
    try {
      LOG.trace("rollback");
      this.connection.sqlConnection().rollback();
    } catch (final SQLException e) {
      throw DatabaseDerbyProvider.ofSQLException("errorConnectionRollback", e);
    }
  }

  @Override
  public <P extends DatabaseQueriesType> P queries(final Class<P> queriesClass)
    throws DatabaseException
  {
    final var database = this.connection.database();

    final var partitionProviderOpt =
      database.partitionProviders()
        .findProviderForDialectAndQueries("DERBY", queriesClass);

    if (partitionProviderOpt.isPresent()) {
      final var partitionProvider = partitionProviderOpt.get();
      return partitionProvider.queriesCreate(this.connection.sqlConnection(), queriesClass);
    }

    throw new DatabaseException(
      ErrorSeverity.SEVERITY_ERROR,
      DatabaseMessages.localize("errorCreateQueriesUnavailable"),
      null,
      PresentableAttributes.one(DatabaseMessages.localize("queriesClass"), queriesClass.getCanonicalName())
    );
  }
}
