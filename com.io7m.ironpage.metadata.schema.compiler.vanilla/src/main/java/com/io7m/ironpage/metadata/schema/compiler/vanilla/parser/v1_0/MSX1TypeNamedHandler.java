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

package com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.v1_0;

import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferenceNamed;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A handler for "TypeNamed" elements.
 */

public final class MSX1TypeNamedHandler implements BTElementHandlerType<Object, MATypeReferenceNamed<Parsed>>
{
  private MATypeReferenceNamed<Parsed> result;

  /**
   * Construct a handler.
   */

  public MSX1TypeNamedHandler()
  {

  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      final var schemaName =
        attributes.getValue("schema");
      final var typeName =
        attributes.getValue("type");

      this.result =
        MATypeReferenceNamed.<Parsed>builder()
          .setData(Parsed.get())
          .setLexical(MSXLexical.position(context))
          .setSchema(schemaName)
          .setName(typeName)
          .build();
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public MATypeReferenceNamed<Parsed> onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
