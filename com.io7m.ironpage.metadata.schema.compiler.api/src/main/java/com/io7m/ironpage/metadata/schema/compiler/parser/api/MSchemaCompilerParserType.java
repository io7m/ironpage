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

import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorCode;

import java.util.Optional;

/**
 * A schema parser.
 *
 * @see MSchemaCompilerParserProviderType
 */

public interface MSchemaCompilerParserType extends AutoCloseable
{
  /**
   * The input was not parseable as XML.
   */

  MSchemaCompilerErrorCode MALFORMED_XML =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerParserType.class.getCanonicalName() + ":malformedXML")
      .build();

  /**
   * An I/O error occurred whilst trying to read from the stream.
   */

  MSchemaCompilerErrorCode IO_ERROR =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerParserType.class.getCanonicalName() + ":ioError")
      .build();

  /**
   * The platform's underlying XML parser was not suitable for use.
   */

  MSchemaCompilerErrorCode BROKEN_XML_PARSER =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerParserType.class.getCanonicalName() + ":brokenXMLParser")
      .build();

  /**
   * The input was not valid according to the published schema.
   */

  MSchemaCompilerErrorCode INVALID_DATA =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerParserType.class.getCanonicalName() + ":invalidData")
      .build();

  /**
   * Execute the parser.
   *
   * @return The parsed schema AST
   */

  Optional<MADeclSchema<Parsed>> execute();
}
