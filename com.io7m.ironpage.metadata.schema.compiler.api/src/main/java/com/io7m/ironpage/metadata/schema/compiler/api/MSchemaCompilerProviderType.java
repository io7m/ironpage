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


package com.io7m.ironpage.metadata.schema.compiler.api;

import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;

import java.io.InputStream;
import java.net.URI;

/**
 * A provider of schema compilers.
 *
 * A schema compiler represents an instance of an entire schema compilation pipeline: Parsing,
 * binding analysis, type checking. A compiler instance is expected to be used once and then
 * discarded.
 */

public interface MSchemaCompilerProviderType
{
  /**
   * Create a new compiler instance.
   *
   * @param errors   An error receiver
   * @param messages A provider of localized messages
   * @param loader   A schema loader
   * @param uri      The URI of the input
   * @param stream   A stream representing a schema
   *
   * @return A new compiler instance
   */

  MSchemaCompilerType createCompiler(
    MSchemaCompilerErrorConsumerType errors,
    MSchemaCompilerMessagesType messages,
    MSchemaCompilerLoaderType loader,
    URI uri,
    InputStream stream);
}
