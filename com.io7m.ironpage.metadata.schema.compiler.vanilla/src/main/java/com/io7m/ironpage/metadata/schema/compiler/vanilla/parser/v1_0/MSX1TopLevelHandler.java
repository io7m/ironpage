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
import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.ast.MAElementType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.math.BigInteger;
import java.util.Map;

import static com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.v1_0.MSX1Constants.META_1_0_NAMESPACE;

/**
 * A handler for top-level "MetaSchema" elements.
 */

public final class MSX1TopLevelHandler
  implements BTElementHandlerType<MAElementType<Parsed>, MADeclSchema<Parsed>>
{
  private final MADeclSchema.Builder<Parsed> schemaBuilder;

  /**
   * Construct a handler.
   */

  public MSX1TopLevelHandler()
  {
    this.schemaBuilder = MADeclSchema.builder();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.schemaBuilder.setData(Parsed.get());
      this.schemaBuilder.setLexical(MSXLexical.position(context));
      this.schemaBuilder.setSchemaName(attributes.getValue("id"));
      this.schemaBuilder.setVersionMajor(
        new BigInteger(attributes.getValue("versionMajor")));
      this.schemaBuilder.setVersionMinor(
        new BigInteger(attributes.getValue("versionMinor")));
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends MAElementType<Parsed>>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    final var namespaceURI = META_1_0_NAMESPACE.toString();

    return Map.ofEntries(
      Map.entry(
        BTQualifiedName.of(namespaceURI, "Comment"),
        Blackthorne.widenConstructor(c -> new MSX1CommentHandler())),
      Map.entry(
        BTQualifiedName.of(namespaceURI, "DeclareAttribute"),
        Blackthorne.widenConstructor(c -> new MSX1AttributeHandler())),
      Map.entry(
        BTQualifiedName.of(namespaceURI, "Import"),
        Blackthorne.widenConstructor(c -> new MSX1ImportHandler())),
      Map.entry(
        BTQualifiedName.of(namespaceURI, "DeclareType"),
        Blackthorne.widenConstructor(c -> new MSX1TypeDeclarationHandler()))
    );
  }

  @Override
  public MADeclSchema<Parsed> onElementFinished(
    final BTElementParsingContextType context)
    throws SAXException
  {
    try {
      return this.schemaBuilder.build();
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final MAElementType<Parsed> result)
  {
    switch (result.kind()) {
      case COMMENT:
      case ATTRIBUTE_DECLARATION:
      case TYPE_DECLARATION:
      case IMPORT_DECLARATION: {
        this.schemaBuilder.addDeclarations(result);
        break;
      }
      case SCHEMA_DECLARATION:
      case TYPE_REFERENCE: {
        break;
      }
    }
  }
}
