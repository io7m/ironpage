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


package com.io7m.ironpage.tests;

import com.io7m.ironpage.database.spi.DatabasePartitionProviderRegistryType;
import com.io7m.ironpage.database.spi.DatabasePartitionProviderType;
import com.io7m.ironpage.database.spi.DatabaseQueriesType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MutablePartitionProviderRegistry implements DatabasePartitionProviderRegistryType
{
  private final ArrayList<DatabasePartitionProviderType> providers;

  public MutablePartitionProviderRegistry()
  {
    this.providers = new ArrayList<>();
  }

  public MutablePartitionProviderRegistry add(
    final DatabasePartitionProviderType provider)
  {
    Objects.requireNonNull(provider, "provider");
    this.providers.add(provider);
    return this;
  }

  @Override
  public List<DatabasePartitionProviderType> findProvidersForDialect(
    final String name)
  {
    Objects.requireNonNull(name, "name");
    return this.providers.stream()
      .filter(provider -> Objects.equals(provider.dialect(), name))
      .collect(Collectors.toList());
  }

  @Override
  public <P extends DatabaseQueriesType> Optional<DatabasePartitionProviderType> findProviderForDialectAndQueries(
    final String name,
    final Class<P> queriesClass)
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(queriesClass, "queriesClass");

    return this.providers.stream()
      .filter(provider -> Objects.equals(provider.dialect(), name))
      .filter(provider -> provider.queriesSupported(queriesClass))
      .findFirst();
  }
}
