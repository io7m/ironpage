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


package com.io7m.ironpage.metadata.schema.compiler.parser.api;

import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;

import java.io.InputStream;
import java.net.URI;

/**
 * A provider of schema parsers.
 *
 * A parser is responsible for taking a raw stream of bytes, representing an XML document, and
 * turning it into an abstract syntax tree.
 */

public interface MSchemaCompilerParserProviderType
{
  /**
   * Create a new parser.
   *
   * @param errors   An error consumer
   * @param messages The compiler messages
   * @param uri      The URI of the input stream for diagnostic purposes
   * @param stream   The source stream
   *
   * @return A new parser
   */

  MSchemaCompilerParserType createParser(
    MSchemaCompilerErrorConsumerType errors,
    MSchemaCompilerMessagesType messages,
    URI uri,
    InputStream stream);
}
