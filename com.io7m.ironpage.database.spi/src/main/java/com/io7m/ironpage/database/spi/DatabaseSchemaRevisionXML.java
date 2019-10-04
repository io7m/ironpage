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

package com.io7m.ironpage.database.spi;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jxe.core.JXEHardenedSAXParsers;
import com.io7m.jxe.core.JXESchemaDefinition;
import com.io7m.jxe.core.JXESchemaResolutionMappings;
import com.io7m.jxe.core.JXEXInclude;
import io.vavr.collection.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.ironpage.errors.api.ErrorSeverity.SEVERITY_ERROR;

/**
 * A class for loading schema revisions from XML files.
 */

public final class DatabaseSchemaRevisionXML implements DatabaseSchemaRevisionType
{
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseSchemaRevisionXML.class);

  private static final JXESchemaDefinition SCHEMA_1_0 =
    JXESchemaDefinition.builder()
      .setFileIdentifier("statements.xsd")
      .setLocation(DatabaseSchemaRevisionXML.class.getResource(
        "/com/io7m/ironpage/database/spi/statements.xsd"))
      .setNamespace(URI.create("urn:com.io7m.ironpage.database.spi.statements:1:0"))
      .build();

  private static final JXESchemaResolutionMappings SCHEMA_MAPPINGS =
    JXESchemaResolutionMappings.builder()
      .putMappings(SCHEMA_1_0.namespace(), SCHEMA_1_0)
      .build();

  private static final JXEHardenedSAXParsers PARSERS =
    new JXEHardenedSAXParsers();

  private final Optional<BigInteger> schemaPrevious;
  private final BigInteger schemaCurrent;
  private final List<String> statements;

  private DatabaseSchemaRevisionXML(
    final Optional<BigInteger> inSchemaPrevious,
    final BigInteger inSchemaCurrent,
    final List<String> inStatements)
  {
    this.schemaPrevious =
      Objects.requireNonNull(inSchemaPrevious, "schemaPrevious");
    this.schemaCurrent =
      Objects.requireNonNull(inSchemaCurrent, "schemaCurrent");
    this.statements =
      Objects.requireNonNull(inStatements, "statements");
  }

  /**
   * Create a schema revision from the given input stream. The input stream must refer to a source
   * that delivers an XML document containing SQL statements.
   *
   * @param schemaPrevious The previous version of which this revision is an upgrade
   * @param schemaCurrent  The current schema version
   * @param uri            The URI of the source
   * @param stream         The source stream
   *
   * @return A schema revision
   *
   * @throws DatabaseException On errors
   */

  public static DatabaseSchemaRevisionType fromStream(
    final Optional<BigInteger> schemaPrevious,
    final BigInteger schemaCurrent,
    final URI uri,
    final InputStream stream)
    throws DatabaseException
  {
    Objects.requireNonNull(schemaPrevious, "schemaPrevious");
    Objects.requireNonNull(schemaCurrent, "schemaCurrent");
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    try {
      final var parser =
        PARSERS.createXMLReader(Optional.empty(), JXEXInclude.XINCLUDE_DISABLED, SCHEMA_MAPPINGS);

      final var source = new InputSource(stream);
      final var urlText = uri.toString();
      source.setPublicId(urlText);

      final var errors = new ArrayList<String>();
      final var statements = new ArrayList<String>();
      final var handler = new XMLContentHandler(errors, statements);
      parser.setErrorHandler(handler);
      parser.setContentHandler(handler);
      parser.parse(source);

      if (errors.size() > 0) {
        throw new DatabaseException(
          SEVERITY_ERROR, errors.get(0), null, TreeMap.empty(), List.copyOf(errors));
      }

      return new DatabaseSchemaRevisionXML(schemaPrevious, schemaCurrent, statements);
    } catch (final ParserConfigurationException | SAXException | IOException e) {
      throw new DatabaseException(SEVERITY_ERROR, e.getLocalizedMessage(), e);
    }
  }

  @Override
  public Optional<BigInteger> schemaPrevious()
  {
    return this.schemaPrevious;
  }

  @Override
  public BigInteger schemaCurrent()
  {
    return this.schemaCurrent;
  }

  @Override
  public void schemaMigrate(
    final Connection connection)
    throws DatabaseException
  {
    Objects.requireNonNull(connection, "connection");

    try {
      Preconditions.checkPrecondition(
        !connection.getAutoCommit(),
        "Auto-commit must be disabled");

      for (final var statement : this.statements) {
        final var trimmed = statement.trim();
        LOG.trace("statement: {}", trimmed);

        try (var prepped = connection.prepareStatement(trimmed)) {
          prepped.execute();
        }
      }
    } catch (final SQLException e) {
      throw new DatabaseException(SEVERITY_ERROR, e.getLocalizedMessage(), e);
    }
  }

  private static final class XMLContentHandler extends DefaultHandler2
  {
    private final List<String> errors;
    private final List<String> statements;
    private boolean inStatement;

    XMLContentHandler(
      final List<String> inErrors,
      final List<String> inStatements)
    {
      this.errors =
        Objects.requireNonNull(inErrors, "errors");
      this.statements =
        Objects.requireNonNull(inStatements, "inStatements");
    }

    @Override
    public void warning(
      final SAXParseException exception)
    {
      LOG.warn(
        "{}:{}:{}: {}",
        Integer.valueOf(exception.getLineNumber()),
        Integer.valueOf(exception.getColumnNumber()),
        exception.getPublicId(),
        exception.getLocalizedMessage());
    }

    @Override
    public void error(
      final SAXParseException exception)
    {
      final var message =
        String.format(
          "%s:%d:%d: %s",
          exception.getPublicId(),
          Integer.valueOf(exception.getLineNumber()),
          Integer.valueOf(exception.getColumnNumber()),
          exception.getLocalizedMessage());

      LOG.error("{}", message);
      this.errors.add(message);
    }

    @Override
    public void fatalError(
      final SAXParseException exception)
      throws SAXException
    {
      final var message =
        String.format(
          "%s:%d:%d: %s",
          exception.getPublicId(),
          Integer.valueOf(exception.getLineNumber()),
          Integer.valueOf(exception.getColumnNumber()),
          exception.getLocalizedMessage());

      LOG.error("{}", message);
      this.errors.add(message);
      throw exception;
    }

    @Override
    public void characters(
      final char[] ch,
      final int start,
      final int length)
    {
      if (!this.errors.isEmpty()) {
        return;
      }
      if (!this.inStatement) {
        return;
      }

      final var text = String.valueOf(ch, start, length).trim();
      if (!text.isEmpty()) {
        LOG.trace("statement: {}", text);
        this.statements.add(text);
      }
    }

    @Override
    public void endElement(
      final String uri,
      final String localName,
      final String qName)
    {
      if (!this.errors.isEmpty()) {
        return;
      }

      LOG.trace("endElement: {}", localName);
      this.inStatement = !Objects.equals(localName, "statement");
    }

    @Override
    public void startElement(
      final String uri,
      final String localName,
      final String qName,
      final Attributes attributes)
    {
      if (!this.errors.isEmpty()) {
        return;
      }

      LOG.trace("startElement: {}", localName);
      this.inStatement = Objects.equals(localName, "statement");
    }
  }
}
