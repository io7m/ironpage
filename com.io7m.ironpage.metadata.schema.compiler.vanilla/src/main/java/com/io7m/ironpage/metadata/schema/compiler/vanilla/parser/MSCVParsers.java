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


package com.io7m.ironpage.metadata.schema.compiler.vanilla.parser;

import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.MSchemaCompilerParserProviderType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.MSchemaCompilerParserType;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/**
 * A provider of parsers.
 */

public final class MSCVParsers implements MSchemaCompilerParserProviderType
{
  /**
   * Construct a provider.
   */

  public MSCVParsers()
  {

  }

  @Override
  public MSchemaCompilerParserType createParser(
    final MSchemaCompilerErrorConsumerType errors,
    final MSchemaCompilerMessagesType messages,
    final URI uri,
    final InputStream stream)
  {
    Objects.requireNonNull(errors, "errors");
    Objects.requireNonNull(messages, "messages");
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");
    return new MSCVParser(errors, uri, stream);
  }
}
