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
import io.vavr.collection.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
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
      final var source = new InputSource(stream);
      final var urlText = uri.toString();
      source.setSystemId(urlText);
      source.setPublicId(urlText);

      final var errors = new ArrayList<String>();
      final var schema = loadSchema();
      final var documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
      documentBuilderFactory.setSchema(schema);
      documentBuilderFactory.setNamespaceAware(true);
      documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

      final var documentBuilder = documentBuilderFactory.newDocumentBuilder();
      documentBuilder.setErrorHandler(new LoggingErrorHandler(errors));

      final var document = documentBuilder.parse(source);
      if (errors.size() > 0) {
        throw new DatabaseException(
          SEVERITY_ERROR, errors.get(0), null, TreeMap.empty(), List.copyOf(errors));
      }

      final var statementElements = document.getElementsByTagName("statement");

      final var statements = new ArrayList<String>(statementElements.getLength());
      for (var index = 0; index < statementElements.getLength(); ++index) {
        final var statementElement = statementElements.item(index);
        statements.add(statementElement.getTextContent());
      }

      return new DatabaseSchemaRevisionXML(schemaPrevious, schemaCurrent, statements);
    } catch (final ParserConfigurationException | SAXException | IOException e) {
      throw new DatabaseException(SEVERITY_ERROR, e.getLocalizedMessage(), e);
    }
  }

  private static Schema loadSchema()
    throws SAXException
  {
    final var schemaURL =
      DatabaseSchemaRevisionXML.class.getResource(
        "/com/io7m/ironpage/database/spi/statements.xsd");
    final var schemas = SchemaFactory.newDefaultInstance();
    return schemas.newSchema(schemaURL);
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

  private static final class LoggingErrorHandler implements ErrorHandler
  {
    private final List<String> errors;

    LoggingErrorHandler(
      final List<String> inErrors)
    {
      this.errors = Objects.requireNonNull(inErrors, "errors");
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
  }
}
