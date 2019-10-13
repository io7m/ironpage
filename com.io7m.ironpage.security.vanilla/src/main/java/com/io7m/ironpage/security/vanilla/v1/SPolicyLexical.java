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


package com.io7m.ironpage.security.vanilla.v1;

import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.jlexing.core.LexicalPosition;
import org.xml.sax.ext.Locator2;

import java.net.URI;

final class SPolicyLexical
{
  private SPolicyLexical()
  {

  }

  static LexicalPosition<URI> position(
    final BTElementParsingContextType context)
  {
    final var locator = context.documentLocator();
    final var uri = findURI(locator);
    return LexicalPosition.<URI>builder()
      .setFile(uri)
      .setColumn(locator.getColumnNumber())
      .setLine(locator.getLineNumber())
      .build();
  }

  private static URI findURI(
    final Locator2 locator)
  {
    final URI uri;
    final var systemId = locator.getSystemId();
    if (systemId == null) {
      final var publicId = locator.getPublicId();
      if (publicId == null) {
        uri = URI.create("urn:unavailable");
      } else {
        uri = URI.create(publicId);
      }
    } else {
      uri = URI.create(systemId);
    }
    return uri;
  }
}