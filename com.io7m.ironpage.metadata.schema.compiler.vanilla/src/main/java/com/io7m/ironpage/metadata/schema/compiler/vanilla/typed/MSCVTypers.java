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


package com.io7m.ironpage.metadata.schema.compiler.vanilla.typed;

import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaBoundType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.typed.api.MSchemaCompilerTyperProviderType;
import com.io7m.ironpage.metadata.schema.compiler.typed.api.MSchemaCompilerTyperType;

import java.net.URI;
import java.util.Objects;

/**
 * The default provider of schema typers.
 */

public final class MSCVTypers implements MSchemaCompilerTyperProviderType
{
  /**
   * Construct a provider.
   */

  public MSCVTypers()
  {

  }

  @Override
  public MSchemaCompilerTyperType createTyper(
    final MSchemaCompilerErrorConsumerType errors,
    final MSchemaCompilerLoaderType loader,
    final MSchemaCompilerMessagesType messages,
    final URI uri,
    final MADeclSchema<MSchemaBoundType> schema)
  {
    Objects.requireNonNull(errors, "errors");
    Objects.requireNonNull(loader, "loader");
    Objects.requireNonNull(messages, "messages");
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(schema, "schema");

    return new MSCVTyper(errors, loader, messages, uri, schema);
  }
}
