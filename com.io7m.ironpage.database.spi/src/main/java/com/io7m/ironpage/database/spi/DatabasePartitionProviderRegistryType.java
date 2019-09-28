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

import java.util.List;
import java.util.Optional;

/**
 * A registry of partition providers.
 */

public interface DatabasePartitionProviderRegistryType
{
  /**
   * @param name The SQL dialect with which providers must be compatible
   *
   * @return The current list of partition providers that use the given SQL dialect
   */

  List<DatabasePartitionProviderType> findProvidersForDialect(
    String name);

  /**
   * @param name         The SQL dialect with which the provider must be compatible
   * @param queriesClass The type of queries the provider must support
   * @param <P>          The precise type of queries
   *
   * @return The partition providers that uses the given SQL dialect and query types, if any
   */

  <P extends DatabaseQueriesType>
  Optional<DatabasePartitionProviderType> findProviderForDialectAndQueries(
    String name,
    Class<P> queriesClass);
}
