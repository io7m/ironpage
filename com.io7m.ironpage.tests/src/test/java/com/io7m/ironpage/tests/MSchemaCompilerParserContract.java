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


package com.io7m.ironpage.tests;

import com.io7m.ironpage.metadata.schema.ast.MACardinality;
import com.io7m.ironpage.metadata.schema.ast.MADeclAttribute;
import com.io7m.ironpage.metadata.schema.ast.MADeclComment;
import com.io7m.ironpage.metadata.schema.ast.MADeclImport;
import com.io7m.ironpage.metadata.schema.ast.MADeclType;
import com.io7m.ironpage.metadata.schema.ast.MAElementType;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferenceNamed;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferencePrimitive;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerError;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorCode;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.MSchemaCompilerParserType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.MSCVMessagesProvider;
import com.io7m.ironpage.metadata.schema.types.api.TypePrimitive;
import org.apache.commons.io.input.BrokenInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Tag("schemaCompiler")
public abstract class MSchemaCompilerParserContract
{
  private ArrayList<MSchemaCompilerError> errors;
  private MSchemaCompilerMessagesType messages;

  private static InputStream resource(
    final String name)
    throws FileNotFoundException
  {
    final var file = String.format("/com/io7m/ironpage/tests/%s", name);
    final var stream = MSchemaCompilerParserContract.class.getResourceAsStream(file);
    if (stream == null) {
      throw new FileNotFoundException(file);
    }
    return stream;
  }

  private static <T> T declarationAs(
    final List<MAElementType<Parsed>> declarations,
    final int index,
    final Class<T> clazz)
  {
    return clazz.cast(declarations.get(index));
  }

  protected abstract MSchemaCompilerParserType createParser(
    MSchemaCompilerErrorConsumerType errors,
    MSchemaCompilerMessagesType messages,
    URI uri,
    InputStream stream);

  private boolean errorsContain(
    final MSchemaCompilerErrorCode code)
  {
    return this.errors.stream().anyMatch(e -> e.errorCode().equals(code));
  }

  @BeforeEach
  public final void testSetup()
  {
    this.errors = new ArrayList<>();
    this.messages = new MSCVMessagesProvider().createStrings(Locale.getDefault());
  }

  /**
   * Invalid XML is an error.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testInvalid0()
    throws Exception
  {
    try (var parser = this.createParser(
      this.showError(), this.messages, URI.create("urn:test"), resource("invalid-0.xml"))) {
      final var resultOpt = parser.execute();
      Assertions.assertTrue(resultOpt.isEmpty());
      Assertions.assertEquals(2, this.errors.size());
      Assertions.assertTrue(this.errorsContain(MSchemaCompilerParserType.INVALID_DATA));
      Assertions.assertTrue(this.errorsContain(MSchemaCompilerParserType.MALFORMED_XML));
    }
  }

  /**
   * Invalid XML is an error.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testInvalid1()
    throws Exception
  {
    try (var parser = this.createParser(
      this.showError(), this.messages, URI.create("urn:test"), resource("invalid-1.xml"))) {
      final var resultOpt = parser.execute();
      Assertions.assertTrue(resultOpt.isEmpty());
      Assertions.assertEquals(2, this.errors.size());
      Assertions.assertTrue(this.errorsContain(MSchemaCompilerParserType.INVALID_DATA));
    }
  }

  /**
   * IO errors are errors.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testInvalid2()
    throws Exception
  {
    try (var parser = this.createParser(
      this.showError(), this.messages, URI.create("urn:test"), new BrokenInputStream())) {
      final var resultOpt = parser.execute();
      Assertions.assertTrue(resultOpt.isEmpty());
      Assertions.assertEquals(1, this.errors.size());
      Assertions.assertTrue(this.errorsContain(MSchemaCompilerParserType.IO_ERROR));
    }
  }

  /**
   * Empty schemas are empty.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testEmpty()
    throws Exception
  {
    try (var parser = this.createParser(
      this.showError(), this.messages, URI.create("urn:test"), resource("meta-empty.xml"))) {
      final var resultOpt = parser.execute();
      Assertions.assertTrue(resultOpt.isPresent());
      final var result = resultOpt.get();
      Assertions.assertEquals("com.io7m.example", result.schemaName());
      Assertions.assertEquals(BigInteger.ONE, result.versionMajor());
      Assertions.assertEquals(BigInteger.ZERO, result.versionMinor());
      Assertions.assertEquals(0, result.declarations().size());
      Assertions.assertEquals(0, this.errors.size());
    }
  }

  /**
   * Basic schemas are parsed correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testBasic()
    throws Exception
  {
    try (var parser = this.createParser(
      this.showError(), this.messages, URI.create("urn:test"), resource("meta-basic.xml"))) {
      final var resultOpt = parser.execute();
      Assertions.assertTrue(resultOpt.isPresent());
      final var result = resultOpt.get();
      Assertions.assertEquals("com.io7m.example", result.schemaName());
      Assertions.assertEquals(BigInteger.ONE, result.versionMajor());
      Assertions.assertEquals(BigInteger.ZERO, result.versionMinor());

      Assertions.assertEquals(7, result.declarations().size());

      {
        final var d = declarationAs(result.declarations(), 0, MADeclComment.class);
        Assertions.assertEquals("Comment 0.", d.text());
      }

      {
        final var d = declarationAs(result.declarations(), 1, MADeclImport.class);
        Assertions.assertEquals("com.io7m.basic", d.schemaName());
        Assertions.assertEquals(BigInteger.ONE, d.versionMajor());
        Assertions.assertEquals(BigInteger.TWO, d.versionMinor());
      }

      {
        final var d = declarationAs(result.declarations(), 2, MADeclImport.class);
        Assertions.assertEquals("com.io7m.other", d.schemaName());
        Assertions.assertEquals(BigInteger.TWO, d.versionMajor());
        Assertions.assertEquals(BigInteger.valueOf(3L), d.versionMinor());
      }

      {
        final var d = declarationAs(result.declarations(), 3, MADeclType.class);
        Assertions.assertEquals("t", d.name());
        final var r = MATypeReferencePrimitive.class.cast(d.baseType());
        Assertions.assertEquals(TypePrimitive.TYPE_INTEGER, r.primitive());
        final Optional<MADeclComment<?>> comment = d.comment();
        Assertions.assertEquals("A t.", comment.get().text());
      }

      {
        final var d = declarationAs(result.declarations(), 4, MADeclType.class);
        Assertions.assertEquals("u", d.name());
        final var r = MATypeReferenceNamed.class.cast(d.baseType());
        Assertions.assertEquals("com.io7m.example", r.schema());
        Assertions.assertEquals("t", r.name());
        final Optional<MADeclComment<?>> comment = d.comment();
        Assertions.assertEquals("A u.", comment.get().text());
      }

      {
        final var d = declarationAs(result.declarations(), 5, MADeclType.class);
        Assertions.assertEquals("be", d.name());
        final var r = MATypeReferenceNamed.class.cast(d.baseType());
        Assertions.assertEquals("com.io7m.basic", r.schema());
        Assertions.assertEquals("b", r.name());
        final Optional<MADeclComment<?>> comment = d.comment();
        Assertions.assertEquals("A be.", comment.get().text());
      }

      {
        final var d = declarationAs(result.declarations(), 6, MADeclAttribute.class);
        Assertions.assertEquals("a", d.name());
        Assertions.assertEquals(MACardinality.CARDINALITY_1, d.cardinality());
        final var r = MATypeReferenceNamed.class.cast(d.type());
        Assertions.assertEquals("com.io7m.example", r.schema());
        Assertions.assertEquals("u", r.name());
        final Optional<MADeclComment<?>> comment = d.comment();
        Assertions.assertEquals("An a.", comment.get().text());
      }

      Assertions.assertEquals(0, this.errors.size());
    }
  }

  private MSchemaCompilerErrorConsumerType showError()
  {
    return e -> {
      this.errors.add(e);
      throw new IllegalStateException("Bad consumer!");
    };
  }
}
