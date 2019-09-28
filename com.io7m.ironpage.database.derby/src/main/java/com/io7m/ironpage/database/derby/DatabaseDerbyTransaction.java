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
import io.vavr.collection.TreeMap;

import java.sql.SQLException;
import java.util.Objects;

final class DatabaseDerbyTransaction implements DatabaseTransactionType
{
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
    try {
      this.connection.pooledConnection().getConnection().close();
    } catch (final SQLException e) {
      throw this.connection.database().provider().ofSQLException("errorConnectionClose", e);
    }
  }

  @Override
  public void commit()
    throws DatabaseException
  {
    try {
      this.connection.pooledConnection().getConnection().commit();
    } catch (final SQLException e) {
      throw this.connection.database().provider().ofSQLException("errorConnectionCommit", e);
    }
  }

  @Override
  public void rollback()
    throws DatabaseException
  {
    try {
      this.connection.pooledConnection().getConnection().rollback();
    } catch (final SQLException e) {
      throw this.connection.database().provider().ofSQLException("errorConnectionRollback", e);
    }
  }

  @Override
  public <P extends DatabaseQueriesType> P queries(final Class<P> queriesClass)
    throws DatabaseException
  {
    final var database = this.connection.database();

    try {
      final var providerOpt =
        database.partitionProviders()
          .findProviderForDialectAndQueries("DERBY", queriesClass);

      if (providerOpt.isPresent()) {
        final var provider = providerOpt.get();
        return provider.queriesCreate(
          this.connection.pooledConnection().getConnection(), queriesClass);
      }

      throw new DatabaseException(
        ErrorSeverity.SEVERITY_ERROR,
        this.localize("errorCreateQueriesUnavailable"),
        null,
        TreeMap.of(this.localize("queriesClass"), queriesClass.getCanonicalName())
      );
    } catch (final SQLException e) {
      throw database.provider().ofSQLException("errorCreateQueries", e);
    }
  }

  private String localize(final String resource)
  {
    return this.connection.database().localize(resource);
  }
}
