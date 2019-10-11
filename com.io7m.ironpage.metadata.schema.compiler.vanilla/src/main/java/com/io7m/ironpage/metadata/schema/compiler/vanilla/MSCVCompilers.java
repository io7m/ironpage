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


package com.io7m.ironpage.metadata.schema.compiler.vanilla;

import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerProviderType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaCompilerBinderProviderType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.MSchemaCompilerParserProviderType;
import com.io7m.ironpage.metadata.schema.compiler.typed.api.MSchemaCompilerTyperProviderType;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.binder.MSCVBinders;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.MSCVParsers;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.typed.MSCVTypers;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/**
 * A provider of compilers.
 */

public final class MSCVCompilers implements MSchemaCompilerProviderType
{
  private final MSCVParsers parsers;
  private final MSCVBinders binders;
  private final MSCVTypers typers;

  /**
   * Construct a provider.
   */

  public MSCVCompilers()
  {
    this.parsers = new MSCVParsers();
    this.binders = new MSCVBinders();
    this.typers = new MSCVTypers();
  }

  /**
   * @return A parser provider
   */

  public MSchemaCompilerParserProviderType parsers()
  {
    return this.parsers;
  }

  /**
   * @return A binder provider
   */

  public MSchemaCompilerBinderProviderType binders()
  {
    return this.binders;
  }

  /**
   * @return A typer provider
   */

  public MSchemaCompilerTyperProviderType typers()
  {
    return this.typers;
  }

  @Override
  public MSchemaCompilerType createCompiler(
    final MSchemaCompilerErrorConsumerType errors,
    final MSchemaCompilerMessagesType messages,
    final MSchemaCompilerLoaderType loader,
    final URI uri,
    final InputStream stream)
  {
    Objects.requireNonNull(errors, "errors");
    Objects.requireNonNull(messages, "messages");
    Objects.requireNonNull(loader, "loader");
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    return new MSCVCompiler(this, errors, messages, loader, uri, stream);
  }
}
