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

package com.io7m.ironpage.security.vanilla;

import com.io7m.blackthorne.api.BTContentHandler;
import com.io7m.blackthorne.api.BTParseError;
import com.io7m.blackthorne.api.BTParseErrorType;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.parser.api.ParserError;
import com.io7m.ironpage.security.api.SPolicy;
import com.io7m.ironpage.security.api.SPolicyParserErrorReceiverType;
import com.io7m.ironpage.security.api.SPolicyParserType;
import com.io7m.ironpage.security.vanilla.v1.SPP1TopLevelHandler;
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
import static com.io7m.ironpage.security.vanilla.v1.SPP1Constants.POLICY_1_0_NAMESPACE;

final class SPolicyParser implements SPolicyParserType
{
  private static final Logger LOG = LoggerFactory.getLogger(SPolicyParser.class);

  private static final JXESchemaDefinition SCHEMA_1_0 =
    JXESchemaDefinition.builder()
      .setFileIdentifier("policy-1_0.xsd")
      .setLocation(SPolicyParser.class.getResource(
        "/com/io7m/ironpage/security/vanilla/policy-1.0.xsd"))
      .setNamespace(POLICY_1_0_NAMESPACE)
      .build();

  private static final JXESchemaResolutionMappings SCHEMA_MAPPINGS =
    JXESchemaResolutionMappings.builder()
      .putMappings(SCHEMA_1_0.namespace(), SCHEMA_1_0)
      .build();

  private static final JXEHardenedSAXParsers PARSERS =
    new JXEHardenedSAXParsers();

  private final SPolicyParserErrorReceiverType errors;
  private final URI uri;
  private final InputStream stream;

  SPolicyParser(
    final SPolicyParserErrorReceiverType inErrors,
    final URI inUri,
    final InputStream inStream)
  {
    this.errors =
      new SafeErrorPublisher(Objects.requireNonNull(inErrors, "errors"));
    this.uri =
      Objects.requireNonNull(inUri, "uri");
    this.stream =
      Objects.requireNonNull(inStream, "stream");
  }

  private static ParserError errorBrokenXMLParser(
    final URI uri,
    final ParserConfigurationException e)
  {
    return ParserError.builder()
      .setMessage(e.getLocalizedMessage())
      .setLexical(lexicalOf(uri, e))
      .setException(e)
      .setSeverity(SEVERITY_ERROR)
      .setErrorCode(BROKEN_XML_PARSER)
      .build();
  }

  private static ParserError errorMalformedXML(
    final URI uri,
    final SAXException e)
  {
    return ParserError.builder()
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

  private static ParserError errorIO(
    final URI uri,
    final IOException e)
  {
    return ParserError.builder()
      .setLexical(lexicalOf(uri, e))
      .setMessage(e.getLocalizedMessage())
      .setException(e)
      .setSeverity(SEVERITY_ERROR)
      .setErrorCode(IO_ERROR)
      .build();
  }

  private static ParserError blackthorneToIron(
    final BTParseError error)
  {
    return ParserError.builder()
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
  public Optional<SPolicy> execute()
  {
    try {
      LOG.debug("executing parser for {}", this.uri);

      final var parser =
        PARSERS.createXMLReader(Optional.empty(), JXEXInclude.XINCLUDE_DISABLED, SCHEMA_MAPPINGS);

      final var source = new InputSource(this.stream);
      final var urlText = this.uri.toString();
      source.setPublicId(urlText);

      final var contentHandler =
        BTContentHandler.<SPolicy>builder()
          .addHandler(
            POLICY_1_0_NAMESPACE.toString(),
            "SecurityPolicy",
            context -> new SPP1TopLevelHandler())
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
    throws IOException
  {
    this.stream.close();
  }

  private static final class SafeErrorPublisher implements SPolicyParserErrorReceiverType
  {
    private final SPolicyParserErrorReceiverType errors;

    SafeErrorPublisher(
      final SPolicyParserErrorReceiverType inErrors)
    {
      this.errors = Objects.requireNonNull(inErrors, "errors");
    }

    @Override
    public void receive(
      final ParserError error)
    {
      try {
        this.errors.receive(error);
      } catch (final Exception e) {
        LOG.debug("ignored exception raised by consumer: ", e);
      }
    }
  }
}
