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


package com.io7m.ironpage.metadata.schema.compiler.binder.api;

import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;

import java.net.URI;

/**
 * A provider of binding analyzers.
 *
 * A binding analyzer analyzes all names given in a schema, checks that all named elements can be
 * resolved, and that all names are valid according to the rules specified in the various name
 * types. The binder returns a copy of the input AST with name annotations added.
 *
 * @see MSchemaBoundType
 * @see com.io7m.ironpage.metadata.schema.types.api.AttributeNames
 * @see com.io7m.ironpage.metadata.schema.types.api.TypeNames
 * @see com.io7m.ironpage.metadata.schema.types.api.MetaSchemaNames
 */

public interface MSchemaCompilerBinderProviderType
{
  /**
   * Create a binding analyzer.
   *
   * @param errors   An error consumer
   * @param loader   A schema loader
   * @param messages The compiler messages
   * @param uri      The URI of the input schema for diagnostic purposes
   * @param schema   The parsed schema
   *
   * @return A new binding analyzer
   */

  MSchemaCompilerBinderType createBinder(
    MSchemaCompilerErrorConsumerType errors,
    MSchemaCompilerLoaderType loader,
    MSchemaCompilerMessagesType messages,
    URI uri,
    MADeclSchema<Parsed> schema);
}
