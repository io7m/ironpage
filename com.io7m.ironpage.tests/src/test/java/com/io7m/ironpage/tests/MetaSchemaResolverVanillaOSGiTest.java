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

import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverProviderType;
import com.io7m.ironpage.types.resolution.spi.MetaSchemaDirectoryType;
import com.io7m.ironpage.types.resolution.vanilla.MetaSchemaResolversOSGi;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Tag("schemaCompiler")
public final class MetaSchemaResolverVanillaOSGiTest extends MetaSchemaResolverContract
{
  @Override
  protected Logger logger()
  {
    return LoggerFactory.getLogger(MetaSchemaResolverVanillaOSGiTest.class);
  }

  @Override
  protected MetaSchemaResolverProviderType resolvers(
    final List<MetaSchemaDirectoryType> directories)
  {
    final var resolvers = new MetaSchemaResolversOSGi();
    for (final var directory : directories) {
      resolvers.onDirectoryUnavailable(directory);
      resolvers.onDirectoryAvailable(directory);
      resolvers.onDirectoryUnavailable(directory);
      resolvers.onDirectoryAvailable(directory);
    }
    return resolvers;
  }
}
