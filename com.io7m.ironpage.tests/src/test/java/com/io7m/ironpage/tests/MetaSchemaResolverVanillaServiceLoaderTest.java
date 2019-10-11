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

import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverError;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverProviderType;
import com.io7m.ironpage.types.resolution.spi.MetaSchemaDirectoryType;
import com.io7m.ironpage.types.resolution.vanilla.MetaSchemaResolversServiceLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Tag("schemaCompiler")
public final class MetaSchemaResolverVanillaServiceLoaderTest extends MetaSchemaResolverContract
{
  @Override
  protected Logger logger()
  {
    return LoggerFactory.getLogger(MetaSchemaResolverVanillaServiceLoaderTest.class);
  }

  @Override
  protected MetaSchemaResolverProviderType resolvers(
    final List<MetaSchemaDirectoryType> directories)
  {
    return new MetaSchemaResolversServiceLoader(directories);
  }

  @Test
  public void testEmptyFromServices0()
  {
    final var resolvers = new MetaSchemaResolversServiceLoader();
    final var resolver = resolvers.create();

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(new TreeMap<>(), errors::add);
    Assertions.assertEquals(0, errors.size());

    Assertions.assertTrue(result.isPresent());
    final var resolution = result.get();
    Assertions.assertEquals(0, resolution.schemas().size());
    Assertions.assertEquals(0, resolution.types().size());
    Assertions.assertEquals(0, resolution.graph().vertexSet().size());
  }

  @Test
  public void testEmptyFromServices1()
  {
    final var resolvers = MetaSchemaResolversServiceLoader.provide();
    final var resolver = resolvers.create();

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(new TreeMap<>(), errors::add);
    Assertions.assertEquals(0, errors.size());

    Assertions.assertTrue(result.isPresent());
    final var resolution = result.get();
    Assertions.assertEquals(0, resolution.schemas().size());
    Assertions.assertEquals(0, resolution.types().size());
    Assertions.assertEquals(0, resolution.graph().vertexSet().size());
  }
}
