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


package com.io7m.ironpage.metadata.schema.compiler.typed.api;

import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaBoundType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;

import java.net.URI;

/**
 * A provider of schema type checkers ("typers").
 *
 * A typer, in this context, is responsible for producing a final run-time schema will fully
 * evaluated type information.
 *
 * @see com.io7m.ironpage.metadata.schema.types.api.MetaSchema
 */

public interface MSchemaCompilerTyperProviderType
{
  /**
   * Create a new typer.
   *
   * @param errors   An error consumer
   * @param loader   A schema loader
   * @param messages The compiler messages
   * @param uri      The URI of the input schema for diagnostic purposes
   * @param schema   The schema AST
   *
   * @return A new type checker
   */

  MSchemaCompilerTyperType createTyper(
    MSchemaCompilerErrorConsumerType errors,
    MSchemaCompilerLoaderType loader,
    MSchemaCompilerMessagesType messages,
    URI uri,
    MADeclSchema<MSchemaBoundType> schema);
}
