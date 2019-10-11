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

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.blackthorne.api.Blackthorne;
import com.io7m.ironpage.metadata.schema.ast.MACardinality;
import com.io7m.ironpage.metadata.schema.ast.MADeclAttribute;
import com.io7m.ironpage.metadata.schema.ast.MADeclComment;
import com.io7m.ironpage.metadata.schema.ast.MAElementType;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferenceType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Map;

import static com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.v1_0.MSX1Constants.META_1_0_NAMESPACE;

/**
 * A handler for "DeclareAttribute" elements.
 */

public final class MSX1AttributeHandler
  implements BTElementHandlerType<MAElementType<Parsed>, MADeclAttribute<Parsed>>
{
  private final MADeclAttribute.Builder<Parsed> builder;

  /**
   * Construct a handler.
   */

  public MSX1AttributeHandler()
  {
    this.builder = MADeclAttribute.builder();
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends MAElementType<Parsed>>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    final var namespaceURI = META_1_0_NAMESPACE.toString();

    return Map.ofEntries(
      Map.entry(
        BTQualifiedName.of(namespaceURI, "TypeReference"),
        Blackthorne.widenConstructor(c -> new MSX1TypeReferenceHandler())),
      Map.entry(
        BTQualifiedName.of(namespaceURI, "Comment"),
        Blackthorne.widenConstructor(c -> new MSX1CommentHandler()))
    );
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.builder.setData(Parsed.get());
      this.builder.setLexical(MSXLexical.position(context));
      this.builder.setName(attributes.getValue("name"));
      this.builder.setCardinality(MACardinality.valueOf(attributes.getValue("cardinality")));
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public MADeclAttribute<Parsed> onElementFinished(
    final BTElementParsingContextType context)
    throws SAXException
  {
    try {
      return this.builder.build();
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final MAElementType<Parsed> result)
    throws SAXException
  {
    try {
      switch (result.kind()) {
        case IMPORT_DECLARATION:
        case SCHEMA_DECLARATION:
        case TYPE_DECLARATION:
        case ATTRIBUTE_DECLARATION: {
          break;
        }
        case TYPE_REFERENCE: {
          assert result instanceof MATypeReferenceType;
          this.builder.setType((MATypeReferenceType<Parsed>) result);
          break;
        }
        case COMMENT: {
          assert result instanceof MADeclComment;
          this.builder.setComment((MADeclComment<Parsed>) result);
          break;
        }
      }
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }
}
