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

package com.io7m.ironpage.metadata.schema.compiler.loader.api;

import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorCode;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;

import java.util.Optional;

/**
 * A schema loader.
 *
 * @see MSchemaCompilerLoaderProviderType
 */

public interface MSchemaCompilerLoaderType extends AutoCloseable
{
  /**
   * A cyclic import has been detected.
   */

  MSchemaCompilerErrorCode CYCLIC_IMPORT =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerLoaderType.class.getCanonicalName() + ":cyclicImport")
      .build();

  /**
   * A source raised an exception.
   */

  MSchemaCompilerErrorCode SOURCE_ERROR =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerLoaderType.class.getCanonicalName() + ":sourceError")
      .build();

  /**
   * Try to load {@code schema} assuming that {@code requester} is the requesting schema.
   *
   * @param requester The requesting schema
   * @param schema    The target schema
   *
   * @return The schema, if one is available
   */

  Optional<MetaSchema> load(
    MetaSchemaIdentifier requester,
    MetaSchemaIdentifier schema);

  @Override
  default void close()
    throws Exception
  {

  }
}
