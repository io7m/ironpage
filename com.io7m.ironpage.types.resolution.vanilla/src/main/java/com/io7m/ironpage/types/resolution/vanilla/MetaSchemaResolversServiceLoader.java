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

package com.io7m.ironpage.types.resolution.vanilla;

import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverProviderType;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverType;
import com.io7m.ironpage.types.resolution.spi.MetaSchemaDirectoryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * A schema resolver that takes directories from {@link ServiceLoader}.
 */

public final class MetaSchemaResolversServiceLoader implements MetaSchemaResolverProviderType
{
  private final List<MetaSchemaDirectoryType> services;

  /**
   * Construct a resolver provider, loading directories from {@link ServiceLoader}.
   */

  public MetaSchemaResolversServiceLoader()
  {
    this(loadServices());
  }

  /**
   * Construct a resolver provider, using the given directories.
   *
   * @param inServices The directories
   */

  public MetaSchemaResolversServiceLoader(
    final List<MetaSchemaDirectoryType> inServices)
  {
    this.services = Objects.requireNonNull(inServices, "services");
  }

  /**
   * @return A new schema resolver
   */

  public static MetaSchemaResolverProviderType provide()
  {
    return new MetaSchemaResolversServiceLoader(loadServices());
  }

  private static List<MetaSchemaDirectoryType> loadServices()
  {
    final var services = new ArrayList<MetaSchemaDirectoryType>();
    final var iterator = ServiceLoader.load(MetaSchemaDirectoryType.class).iterator();

    while (iterator.hasNext()) {
      services.add(iterator.next());
    }
    return List.copyOf(services);
  }

  @Override
  public MetaSchemaResolverType createForLocale(final Locale locale)
  {
    return new MetaSchemaResolver(locale, this.services);
  }
}
