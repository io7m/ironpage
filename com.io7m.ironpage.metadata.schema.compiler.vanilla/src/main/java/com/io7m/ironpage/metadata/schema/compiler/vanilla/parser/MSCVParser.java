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

import com.io7m.blackthorne.api.BTContentHandler;
import com.io7m.blackthorne.api.BTParseError;
import com.io7m.blackthorne.api.BTParseErrorType;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerError;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.MSchemaCompilerParserType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.v1_0.MSX1TopLevelHandler;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.typed.MSCVSafeErrorConsumer;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalPositions;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.jxe.core.JXEHardenedSAXParsers;
import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaResolutionMappings;
import com.io7m.jxe.core.JXEXInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.io7m.ironpage.errors.api.ErrorSeverity.SEVERITY_ERROR;
import static com.io7m.ironpage.errors.api.ErrorSeverity.SEVERITY_WARNING;
import static com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.v1_0.MSX1Constants.META_1_0_NAMESPACE;

/**
 * A parser implementation.
 */

final class MSCVParser implements MSchemaCompilerParserType
{
  private static final Logger LOG = LoggerFactory.getLogger(MSCVParser.class);

  private static final JXESchemaDefinition SCHEMA_1_0 =
    JXESchemaDefinition.builder()
      .setFileIdentifier("metaschema-1_0.xsd")
      .setLocation(MSCVParser.class.getResource(
        "/com/io7m/ironpage/metadata/schema/compiler/vanilla/metaschema-1_0.xsd"))
      .setNamespace(META_1_0_NAMESPACE)
      .build();

  private static final JXESchemaResolutionMappings SCHEMA_MAPPINGS =
    JXESchemaResolutionMappings.builder()
      .putMappings(SCHEMA_1_0.namespace(), SCHEMA_1_0)
      .build();

  private static final JXEHardenedSAXParsers PARSERS =
    new JXEHardenedSAXParsers();

  private final MSchemaCompilerErrorConsumerType errors;
  private final URI uri;
  private final InputStream stream;

  MSCVParser(
    final MSchemaCompilerErrorConsumerType inErrors,
    final URI inUri,
    final InputStream inStream)
  {
    this.errors =
      new MSCVSafeErrorConsumer(Objects.requireNonNull(inErrors, "errors"));
    this.uri =
      Objects.requireNonNull(inUri, "uri");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
  }

  private static MSchemaCompilerError errorBrokenXMLParser(
    final URI uri,
    final ParserConfigurationException e)
  {
    return MSchemaCompilerError.builder()
      .setMessage(e.getLocalizedMessage())
      .setLexical(lexicalOf(uri, e))
      .setException(e)
      .setSeverity(SEVERITY_ERROR)
      .setErrorCode(BROKEN_XML_PARSER)
      .build();
  }

  private static MSchemaCompilerError errorMalformedXML(
    final URI uri,
    final SAXException e)
  {
    return MSchemaCompilerError.builder()
      .setLexical(lexicalOf(uri, e))
      .setMessage(e.getLocalizedMessage())
      .setException(e)
      .setSeverity(SEVERITY_ERROR)
      .setErrorCode(MALFORMED_XML)
      .build();
  }

  private static LexicalPosition<URI> lexicalOf(
    final URI uri,
    final Exception e)
  {
    if (e instanceof SAXParseException) {
      final var parseEx = (SAXParseException) e;
      return LexicalPosition.<URI>builder()
        .setColumn(parseEx.getColumnNumber())
        .setLine(parseEx.getLineNumber())
        .setFile(uri)
        .build();
    }
    return LexicalPositions.zeroWithFile(uri);
  }

  private static MSchemaCompilerError errorIO(
    final URI uri,
    final IOException e)
  {
    return MSchemaCompilerError.builder()
      .setLexical(lexicalOf(uri, e))
      .setMessage(e.getLocalizedMessage())
      .setException(e)
      .setSeverity(SEVERITY_ERROR)
      .setErrorCode(IO_ERROR)
      .build();
  }

  private static MSchemaCompilerError blackthorneToIron(
    final BTParseError error)
  {
    return MSchemaCompilerError.builder()
      .setLexical(error.lexical())
      .setMessage(error.message())
      .setException(error.exception())
      .setSeverity(blackthorneSeverityToIron(error.severity()))
      .setErrorCode(INVALID_DATA)
      .build();
  }

  private static ErrorSeverity blackthorneSeverityToIron(
    final BTParseErrorType.Severity severity)
  {
    switch (severity) {
      case WARNING:
        return SEVERITY_WARNING;
      case ERROR:
        return SEVERITY_ERROR;
    }
    throw new UnreachableCodeException();
  }

  @Override
  public Optional<MADeclSchema<Parsed>> execute()
  {
    try {
      LOG.debug("executing parser for {}", this.uri);

      final var parser =
        PARSERS.createXMLReader(Optional.empty(), JXEXInclude.XINCLUDE_DISABLED, SCHEMA_MAPPINGS);

      final var source = new InputSource(this.stream);
      final var urlText = this.uri.toString();
      source.setPublicId(urlText);

      final var contentHandler =
        BTContentHandler.<MADeclSchema<Parsed>>builder()
          .addHandler(
            META_1_0_NAMESPACE.toString(),
            "MetaSchema",
            context -> new MSX1TopLevelHandler())
          .build(this.uri, error -> this.errors.receive(blackthorneToIron(error)));

      parser.setErrorHandler(contentHandler);
      parser.setContentHandler(contentHandler);
      parser.parse(source);

      return contentHandler.result().map(Function.identity());
    } catch (final ParserConfigurationException e) {
      this.errors.receive(errorBrokenXMLParser(this.uri, e));
      return Optional.empty();
    } catch (final SAXException e) {
      this.errors.receive(errorMalformedXML(this.uri, e));
      return Optional.empty();
    } catch (final IOException e) {
      this.errors.receive(errorIO(this.uri, e));
      return Optional.empty();
    }
  }

  @Override
  public void close()
  {

  }
}
