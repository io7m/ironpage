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
import com.io7m.ironpage.database.api.DatabaseType;
import com.io7m.ironpage.database.spi.DatabaseEventType;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.database.spi.DatabasePartitionProviderRegistryType;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Objects;

final class DatabaseDerby implements DatabaseType
{
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseDerby.class);

  private final EmbeddedConnectionPoolDataSource dataSource;
  private final DatabasePartitionProviderRegistryType partitionProviders;
  private final PublishSubject<DatabaseEventType> eventSubject;

  DatabaseDerby(
    final EmbeddedConnectionPoolDataSource inDataSource,
    final DatabasePartitionProviderRegistryType inPartitionProviders)
  {
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
    this.partitionProviders =
      Objects.requireNonNull(inPartitionProviders, "inPartitionProviders");

    this.eventSubject =
      PublishSubject.create();
  }

  @Override
  public Observable<DatabaseEventType> events()
  {
    return this.eventSubject;
  }

  @Override
  public String dialect()
  {
    return "DERBY";
  }

  @Override
  public DatabaseConnectionType openConnection()
    throws DatabaseException
  {
    try {
      final var connection = this.dataSource.getPooledConnection();
      return new DatabaseDerbyConnection(
        this,
        connection.getConnection());
    } catch (final SQLException e) {
      throw DatabaseDerbyProvider.ofSQLException("errorOpenConnection", e);
    }
  }

  @Override
  public void close()
  {
    LOG.debug("close");
    this.eventSubject.onComplete();
  }

  DatabasePartitionProviderRegistryType partitionProviders()
  {
    return this.partitionProviders;
  }

  PublishSubject<DatabaseEventType> eventSubject()
  {
    return this.eventSubject;
  }
}
