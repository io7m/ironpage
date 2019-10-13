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


package com.io7m.ironpage.security.api;

import com.io7m.ironpage.parser.api.ParserErrorCode;
import com.io7m.ironpage.parser.api.ParserType;

import java.util.Optional;

/**
 * A security policy parser.
 */

public interface SPolicyParserType extends ParserType
{
  /**
   * The input was not parseable as XML.
   */

  ParserErrorCode MALFORMED_XML =
    ParserErrorCode.builder()
      .setCode(SPolicyParserType.class.getCanonicalName() + ":malformedXML")
      .build();

  /**
   * An I/O error occurred whilst trying to read from the stream.
   */

  ParserErrorCode IO_ERROR =
    ParserErrorCode.builder()
      .setCode(SPolicyParserType.class.getCanonicalName() + ":ioError")
      .build();

  /**
   * The platform's underlying XML parser was not suitable for use.
   */

  ParserErrorCode BROKEN_XML_PARSER =
    ParserErrorCode.builder()
      .setCode(SPolicyParserType.class.getCanonicalName() + ":brokenXMLParser")
      .build();

  /**
   * The input was not valid according to the published schema.
   */

  ParserErrorCode INVALID_DATA =
    ParserErrorCode.builder()
      .setCode(SPolicyParserType.class.getCanonicalName() + ":invalidData")
      .build();

  /**
   * @return A parsed security policy, assuming no errors occurred
   */

  Optional<SPolicy> execute();
}
