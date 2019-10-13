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

import com.io7m.ironpage.database.api.DatabaseParameters;
import com.io7m.ironpage.database.api.DatabaseProviderType;
import com.io7m.ironpage.database.api.DatabaseType;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.database.spi.DatabasePartitionProviderRegistryType;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Objects;

/**
 * A provider of Derby databases.
 */

public final class DatabaseDerbyProvider implements DatabaseProviderType
{
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseDerbyProvider.class);

  private final DatabasePartitionProviderRegistryType registry;

  /**
   * Construct a Derby database provider.
   *
   * @param inRegistry The partition provider registry
   */

  public DatabaseDerbyProvider(
    final DatabasePartitionProviderRegistryType inRegistry)
  {
    this.registry =
      Objects.requireNonNull(inRegistry, "registry");
  }

  static DatabaseException ofSQLException(
    final String resourceId,
    final SQLException e)
  {
    return new DatabaseException(
      DatabaseMessages.localize(resourceId, e.getLocalizedMessage()),
      e);
  }

  @Override
  public DatabaseType open(final DatabaseParameters parameters)
    throws DatabaseException
  {
    Objects.requireNonNull(parameters, "parameters");

    final var path = parameters.path();
    LOG.info("open: {}", path);

    try {
      final var dataSource = new EmbeddedConnectionPoolDataSource();
      dataSource.setDatabaseName(path);
      dataSource.setCreateDatabase("true");
      dataSource.setConnectionAttributes("create=true");

      final var connection = dataSource.getConnection();
      connection.setAutoCommit(false);

      final var partitionProviders = this.registry.findProvidersForDialect("DERBY");
      LOG.debug("{} partition providers for DERBY", Integer.valueOf(partitionProviders.size()));

      for (final var partitionProvider : partitionProviders) {
        final var receivedDialect = partitionProvider.dialect();
        if (!Objects.equals(receivedDialect, "DERBY")) {
          LOG.warn(
            "[{}]: received incorrect dialect ({} instead of {})",
            partitionProvider.getClass().getCanonicalName(),
            receivedDialect,
            "DERBY");
          continue;
        }
        partitionProvider.upgradePartitionToLatest(connection);
      }

      return new DatabaseDerby(dataSource, this.registry);
    } catch (final Exception e) {
      throw new DatabaseException(
        ErrorSeverity.SEVERITY_ERROR,
        DatabaseMessages.localize("errorOpenDatabase", e.getLocalizedMessage()),
        e);
    }
  }
}
