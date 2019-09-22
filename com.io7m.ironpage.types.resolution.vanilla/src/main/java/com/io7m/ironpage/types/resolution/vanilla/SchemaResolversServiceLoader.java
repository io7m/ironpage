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

import com.io7m.ironpage.types.resolution.api.SchemaResolverProviderType;
import com.io7m.ironpage.types.resolution.api.SchemaResolverType;
import com.io7m.ironpage.types.resolution.spi.SchemaDirectoryType;
import io.vavr.collection.Vector;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;

public final class SchemaResolversServiceLoader implements SchemaResolverProviderType
{
  private final Vector<SchemaDirectoryType> services;

  public SchemaResolversServiceLoader()
  {
    this(loadServices());
  }

  public SchemaResolversServiceLoader(
    final Vector<SchemaDirectoryType> inServices)
  {
    this.services = Objects.requireNonNull(inServices, "services");
  }

  public static SchemaResolverProviderType provide()
  {
    return new SchemaResolversServiceLoader(loadServices());
  }

  private static Vector<SchemaDirectoryType> loadServices()
  {
    final var services = new ArrayList<SchemaDirectoryType>();
    final var iterator = ServiceLoader.load(SchemaDirectoryType.class).iterator();

    while (iterator.hasNext()) {
      services.add(iterator.next());
    }
    return Vector.ofAll(services);
  }

  @Override
  public SchemaResolverType createForLocale(final Locale locale)
  {
    return new SchemaResolver(locale, this.services);
  }

  @Override
  public SchemaResolverType create()
  {
    return new SchemaResolver(Locale.getDefault(), this.services);
  }
}
