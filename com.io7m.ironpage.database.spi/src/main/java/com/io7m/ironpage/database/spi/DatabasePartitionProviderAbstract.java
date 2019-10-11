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

import com.io7m.ironpage.presentable.api.PresentableAttributes;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.io7m.ironpage.errors.api.ErrorSeverity.SEVERITY_ERROR;

/**
 * An abstract partition provider. This implements common services required by provider
 * implementations.
 */

public abstract class DatabasePartitionProviderAbstract implements DatabasePartitionProviderType
{
  private final ResourceBundle resources;
  private final DatabaseQueriesContructorCollection queryConstructors;
  private final Clock clock;
  private boolean installedSchemaVersionRetrieved;
  private Optional<BigInteger> installedSchemaVersion;
  private Optional<NavigableMap<BigInteger, DatabaseSchemaRevisionType>> schemaRevisions;

  protected DatabasePartitionProviderAbstract(
    final Clock inClock,
    final DatabaseQueriesContructorCollection inQueryConstructors)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.queryConstructors =
      Objects.requireNonNull(inQueryConstructors, "queryConstructors");

    this.installedSchemaVersion = Optional.empty();
    this.installedSchemaVersionRetrieved = false;
    this.schemaRevisions = Optional.empty();

    this.resources =
      ResourceBundle.getBundle("com.io7m.ironpage.database.spi.Messages");
  }

  protected abstract Logger logger();

  protected abstract String partitionName();

  protected abstract Optional<BigInteger> findSchemaVersionActual(
    Connection connection)
    throws DatabaseException;

  protected abstract NavigableMap<BigInteger, DatabaseSchemaRevisionType> schemaRevisionsActual()
    throws DatabaseException;

  @Override
  public final Optional<BigInteger> findSchemaVersion(
    final Connection connection)
    throws DatabaseException
  {
    Objects.requireNonNull(connection, "connection");

    if (!this.installedSchemaVersionRetrieved) {
      final var installedVersion = this.findSchemaVersionActual(connection);
      this.installedSchemaVersion = Objects.requireNonNull(installedVersion, "installedVersion");
      this.installedSchemaVersionRetrieved = true;
    }

    return this.installedSchemaVersion;
  }

  @Override
  public final NavigableMap<BigInteger, DatabaseSchemaRevisionType> schemaRevisions()
    throws DatabaseException
  {
    if (this.schemaRevisions.isPresent()) {
      return this.schemaRevisions.get();
    }

    final var retrieved = Collections.unmodifiableNavigableMap(this.schemaRevisionsActual());
    this.schemaRevisions = Optional.of(retrieved);
    return retrieved;
  }

  @Override
  public final boolean isUpgradeRequired(
    final Connection connection)
    throws DatabaseException
  {
    final var installedVersionOpt = this.findSchemaVersion(connection);
    if (installedVersionOpt.isEmpty()) {
      return true;
    }

    final var highestAvailableOpt = this.highestAvailableSchemaVersion();
    if (highestAvailableOpt.isEmpty()) {
      return true;
    }

    final var installedVersion = installedVersionOpt.get();
    final var highestAvailable = highestAvailableOpt.get();
    return installedVersion.compareTo(highestAvailable) < 0;
  }

  private Optional<BigInteger> highestAvailableSchemaVersion()
    throws DatabaseException
  {
    final var revisions = this.schemaRevisions();
    return Optional.ofNullable(revisions.lastEntry()).map(Map.Entry::getKey);
  }

  private List<BigInteger> checkUpgradesToRun(
    final Connection connection)
    throws DatabaseException
  {
    final var logger = this.logger();

    final var installedVersionOpt =
      this.findSchemaVersion(connection);
    final var availableVersions =
      this.schemaRevisions();

    if (installedVersionOpt.isPresent()) {
      final var installedVersion = installedVersionOpt.get();
      if (!availableVersions.containsKey(installedVersion)) {
        throw new DatabaseException(
          SEVERITY_ERROR,
          this.localize("errorUnsupportedInstalledVersion"),
          null,
          PresentableAttributes.of(
            PresentableAttributes.entry(this.localize("installedVersion"), installedVersion.toString()),
            PresentableAttributes.entry(this.localize("supportedVersions"), availableVersions.keySet().toString())));
      }

      return List.copyOf(availableVersions.tailMap(installedVersion, true).keySet());
    }

    logger.debug("[{}]: database schema uninitialized, running full install", this.partitionName());
    return List.copyOf(availableVersions.keySet());
  }

  private String localize(
    final String code)
  {
    return this.resources.getString(code);
  }

  @Override
  public final void upgradePartitionToLatest(
    final Connection connection)
    throws DatabaseException
  {
    Objects.requireNonNull(connection, "connection");

    if (!this.isUpgradeRequired(connection)) {
      return;
    }

    final var logger = this.logger();
    final var availableVersions = this.schemaRevisions();
    final var upgradesToRun = this.checkUpgradesToRun(connection);
    logger.debug(
      "[{}]: {} upgrades required",
      this.partitionName(),
      Integer.valueOf(upgradesToRun.size()));

    try {
      if (!upgradesToRun.isEmpty()) {
        for (final var upgradeVersion : upgradesToRun) {
          logger.debug("[{}]: running schema migration {}", this.partitionName(), upgradeVersion);
          final var revision = availableVersions.get(upgradeVersion);
          revision.schemaMigrate(connection);
          this.installedSchemaVersion = Optional.of(upgradeVersion);
          this.installedSchemaVersionRetrieved = true;
        }
        logger.debug("[{}]: committing upgrades", this.partitionName());
        connection.commit();
      } else {
        connection.rollback();
      }
    } catch (final SQLException e) {
      throw new DatabaseException(SEVERITY_ERROR, e.getLocalizedMessage(), e);
    }
  }

  @Override
  public final <P extends DatabaseQueriesType> P queriesCreate(
    final Connection connection,
    final Class<P> queriesClass)
    throws DatabaseException
  {
    if (this.queryConstructors.has(queriesClass)) {
      return this.queryConstructors.get(queriesClass).create(this.clock, connection);
    }

    throw new DatabaseException(
      SEVERITY_ERROR,
      this.localize("errorUnavailableQueries"),
      null,
      PresentableAttributes.of(
        PresentableAttributes.entry(this.localize("queryClass"), queriesClass.getCanonicalName())));
  }

  @Override
  public final <P extends DatabaseQueriesType> boolean queriesSupported(
    final Class<P> queriesClass)
  {
    return this.queryConstructors.has(queriesClass);
  }
}
