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

import java.math.BigInteger;
import java.sql.Connection;
import java.util.NavigableMap;
import java.util.Optional;

/**
 * An implementation of a database partition.
 */

public interface DatabasePartitionProviderType
{
  /**
   * @param connection An SQL connection
   *
   * @return The version of the schema currently in the database, if any
   *
   * @throws DatabaseException On database errors
   */

  Optional<BigInteger> findSchemaVersion(
    Connection connection)
    throws DatabaseException;

  /**
   * @return The available schema revisions
   *
   * @throws DatabaseException On database errors
   */

  NavigableMap<BigInteger, DatabaseSchemaRevisionType> schemaRevisions()
    throws DatabaseException;

  /**
   * @return The SQL dialect with which this partition provider is compatible
   */

  String dialect();

  /**
   * Create queries for a given connection.
   *
   * @param connection   The connection
   * @param events       An event subject used to publish database events
   * @param queriesClass The queries class
   * @param <P>          The precise type of queries
   *
   * @return A query interface
   *
   * @throws DatabaseException On database errors
   */

  <P extends DatabaseQueriesType>
  P queriesCreate(
    Connection connection,
    Subject<DatabaseEventType> events,
    Class<P> queriesClass)
    throws DatabaseException;

  /**
   * @param queriesClass The queries class
   * @param <P>          The precise type of queries
   *
   * @return {@code true} if queries of type {@code queriesClass} are supported
   */

  <P extends DatabaseQueriesType>
  boolean queriesSupported(Class<P> queriesClass);

  /**
   * Upgrade the partition to the latest supported schema.
   *
   * @param connection The database connection
   *
   * @throws DatabaseException On database errors
   */

  void upgradePartitionToLatest(
    Connection connection)
    throws DatabaseException;

  /**
   * @param connection The database connection
   *
   * @return {@code true} if upgrading is required
   *
   * @throws DatabaseException On database errors
   */

  boolean isUpgradeRequired(
    Connection connection)
    throws DatabaseException;

}
