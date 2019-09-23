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

import com.io7m.ironpage.types.resolution.api.SchemaResolverError;
import com.io7m.ironpage.types.resolution.api.SchemaResolverProviderType;
import com.io7m.ironpage.types.resolution.spi.SchemaDirectoryType;
import com.io7m.ironpage.types.resolution.vanilla.SchemaResolversServiceLoader;
import io.vavr.collection.TreeMap;
import io.vavr.collection.Vector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public final class SchemaResolverVanillaServiceLoaderTest extends SchemaResolverContract
{
  @Override
  protected Logger logger()
  {
    return LoggerFactory.getLogger(SchemaResolverVanillaServiceLoaderTest.class);
  }

  @Override
  protected SchemaResolverProviderType resolvers(
    final Vector<SchemaDirectoryType> directories)
  {
    return new SchemaResolversServiceLoader(directories);
  }

  @Test
  public void testEmptyFromServices0()
  {
    final var resolvers = new SchemaResolversServiceLoader();
    final var resolver = resolvers.create();

    final var errors = new ArrayList<SchemaResolverError>();
    final var result = resolver.resolve(TreeMap.empty(), errors::add);
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
    final var resolvers = SchemaResolversServiceLoader.provide();
    final var resolver = resolvers.create();

    final var errors = new ArrayList<SchemaResolverError>();
    final var result = resolver.resolve(TreeMap.empty(), errors::add);
    Assertions.assertEquals(0, errors.size());

    Assertions.assertTrue(result.isPresent());
    final var resolution = result.get();
    Assertions.assertEquals(0, resolution.schemas().size());
    Assertions.assertEquals(0, resolution.types().size());
    Assertions.assertEquals(0, resolution.graph().vertexSet().size());
  }
}
