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

package com.io7m.ironpage.metadata.schema.compiler.vanilla.binder;

import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaCompilerBinderProviderType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaCompilerBinderType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;

import java.net.URI;
import java.util.Objects;

/**
 * A provider of binders.
 */

public final class MSCVBinders implements MSchemaCompilerBinderProviderType
{
  /**
   * Construct a provider.
   */

  public MSCVBinders()
  {

  }

  @Override
  public MSchemaCompilerBinderType createBinder(
    final MSchemaCompilerErrorConsumerType errors,
    final MSchemaCompilerLoaderType loader,
    final MSchemaCompilerMessagesType messages,
    final URI uri,
    final MADeclSchema<Parsed> schema)
  {
    Objects.requireNonNull(errors, "errors");
    Objects.requireNonNull(loader, "loader");
    Objects.requireNonNull(messages, "messages");
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(schema, "schema");
    return new MSCVBinder(errors, loader, messages, uri, schema);
  }
}
