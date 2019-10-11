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

import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerProviderType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerSourceType;

/**
 * A provider of schema loaders.
 *
 * A schema loader is responsible for loading a schema when one is requested by identifier. In
 * practice, implementations will load already-compiled schemas from caches, and will invoke the
 * compiler pipeline recursively to compile schemas that have not already been compiled.
 * Implementations talk to implementations of the {@link MSchemaCompilerSourceType} interface to
 * load schema source text from stores such as the filesystem, relational databases, etc.
 *
 * @see com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier
 * @see MSchemaCompilerSourceType
 */

public interface MSchemaCompilerLoaderProviderType
{
  /**
   * Create a new schema loader.
   *
   * @param compilers A provider of compilers for schemas that have not been compiled
   * @param errors    An error consumer
   * @param messages  The compiler messages
   * @param sources   A source of schemas
   *
   * @return A new schema loader
   */

  MSchemaCompilerLoaderType createLoader(
    MSchemaCompilerProviderType compilers,
    MSchemaCompilerErrorConsumerType errors,
    MSchemaCompilerMessagesType messages,
    MSchemaCompilerSourceType sources);
}
