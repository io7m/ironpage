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

package com.io7m.ironpage.database.core.derby;

import com.io7m.ironpage.database.accounts.api.AccountsDatabaseQueriesType;
import com.io7m.ironpage.database.pages.api.PagesDatabaseQueriesType;
import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.database.spi.DatabasePartitionProviderAbstract;
import com.io7m.ironpage.database.spi.DatabasePartitionProviderType;
import com.io7m.ironpage.database.spi.DatabaseQueriesContructorCollection;
import com.io7m.ironpage.database.spi.DatabaseSchemaRevisionType;
import com.io7m.ironpage.database.spi.DatabaseSchemaRevisionXML;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import static com.io7m.ironpage.errors.api.ErrorSeverity.SEVERITY_ERROR;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;

@Component(service = DatabasePartitionProviderType.class)
public final class CoreDatabasePartitionProviderDerby extends DatabasePartitionProviderAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoreDatabasePartitionProviderDerby.class);

  // XXX: See org.apache.derby.shared.common.reference.SQLState.LANG_SCHEMA_DOES_NOT_EXIST
  // These packages are not exported but should be in the future.
  private static final String LANG_SCHEMA_DOES_NOT_EXIST = "42Y07";
  private static final String LANG_TABLE_NOT_FOUND = "42X05";

  /**
   * Construct a provider.
   */

  public CoreDatabasePartitionProviderDerby()
  {
    this(Clock.systemUTC());
  }

  /**
   * Construct a provider.
   *
   * @param clock The clock used for time-based queries
   */

  public CoreDatabasePartitionProviderDerby(
    final Clock clock)
  {
    super(
      clock,
      new DatabaseQueriesContructorCollection()
        .put(AccountsDatabaseQueriesType.class, CoreAccountsDatabaseQueries::new)
        .put(PagesDatabaseQueriesType.class, CorePagesDatabaseQueries::new));
  }

  private static DatabaseSchemaRevisionType loadRevision(
    final Optional<BigInteger> previous,
    final BigInteger current)
    throws DatabaseException
  {
    final var path =
      String.format("/com/io7m/ironpage/database/core/derby/schema-%s.xml", current);
    final var clazz = CoreDatabasePartitionProviderDerby.class;
    try (var stream = clazz.getResourceAsStream(path)) {
      final var url = clazz.getResource(path);
      return DatabaseSchemaRevisionXML.fromStream(previous, current, url.toURI(), stream);
    } catch (final IOException | URISyntaxException e) {
      throw new DatabaseException(SEVERITY_ERROR, e.getLocalizedMessage(), e);
    }
  }

  @Override
  protected Logger logger()
  {
    return LOG;
  }

  @Override
  protected String partitionName()
  {
    return CoreDatabasePartitionProviderDerby.class.getCanonicalName();
  }

  @Override
  protected Optional<BigInteger> findSchemaVersionActual(
    final Connection connection)
    throws DatabaseException
  {
    Objects.requireNonNull(connection, "connection");

    try {
      try (var statement = connection.prepareStatement(
        "SELECT version_number FROM accounts.schema_version")) {
        try (var result = statement.executeQuery()) {
          if (!result.next()) {
            throw new SQLException(
              "Schema version table (accounts.schema_version) must contain one row");
          }

          return Optional.of(valueOf(result.getLong(1)));
        }
      }
    } catch (final SQLException e) {
      final var state = e.getSQLState();
      switch (state) {
        case LANG_SCHEMA_DOES_NOT_EXIST:
        case LANG_TABLE_NOT_FOUND: {
          return Optional.empty();
        }
        default: {
          throw new DatabaseException(SEVERITY_ERROR, e.getLocalizedMessage(), e);
        }
      }
    }
  }

  @Override
  protected NavigableMap<BigInteger, DatabaseSchemaRevisionType> schemaRevisionsActual()
    throws DatabaseException
  {
    final var revisions = new TreeMap<BigInteger, DatabaseSchemaRevisionType>();
    revisions.put(ZERO, loadRevision(Optional.empty(), ZERO));
    revisions.put(ONE, loadRevision(Optional.of(ZERO), ONE));
    return revisions;
  }

  @Override
  public String dialect()
  {
    return "DERBY";
  }
}
