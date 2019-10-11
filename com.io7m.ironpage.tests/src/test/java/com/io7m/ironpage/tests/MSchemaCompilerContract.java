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

import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerError;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorCode;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerSourceType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerStream;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.MSCVMessagesProvider;
import com.io7m.ironpage.metadata.schema.types.api.AttributeCardinality;
import com.io7m.ironpage.metadata.schema.types.api.AttributeName;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaName;
import com.io7m.ironpage.metadata.schema.types.api.TypePrimitive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import static com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaCompilerBinderType.SCHEMA_NONEXISTENT;
import static com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType.CYCLIC_IMPORT;
import static com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType.SOURCE_ERROR;
import static com.io7m.ironpage.metadata.schema.types.api.AttributeCardinality.CARDINALITY_1;
import static com.io7m.ironpage.metadata.schema.types.api.TypePrimitive.TYPE_INTEGER;

@Tag("schemaCompiler")
public abstract class MSchemaCompilerContract
{
  private static final Logger LOG = LoggerFactory.getLogger(MSchemaCompilerContract.class);

  private ArrayList<MSchemaCompilerError> errors;
  private MSchemaCompilerMessagesType messages;

  private static InputStream resource(
    final String name)
    throws FileNotFoundException
  {
    final var file = String.format("/com/io7m/ironpage/tests/%s", name);
    final var stream = MSchemaCompilerContract.class.getResourceAsStream(file);
    if (stream == null) {
      throw new FileNotFoundException(file);
    }
    return stream;
  }

  protected abstract MSchemaCompilerLoaderType loader(
    MSchemaCompilerSourceType source,
    MSchemaCompilerErrorConsumerType errors);

  protected abstract MSchemaCompilerType createCompiler(
    MSchemaCompilerErrorConsumerType errors,
    MSchemaCompilerMessagesType messages,
    MSchemaCompilerLoaderType loader,
    URI uri,
    InputStream stream)
    throws Exception;

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

  private MSchemaCompilerErrorConsumerType showError()
  {
    return e -> {
      LOG.debug("error: {}", e);
      this.errors.add(e);
      throw new IllegalStateException("Bad consumer!");
    };
  }

  private static final class CyclicSources implements MSchemaCompilerSourceType
  {
    CyclicSources()
    {

    }

    @Override
    public Optional<MSchemaCompilerStream> openSchemaSource(
      final MetaSchemaIdentifier identifier)
      throws IOException
    {
      switch (identifier.show()) {
        case "com.io7m.cycle0:1:0":
          return Optional.of(MSchemaCompilerStream.of(
            identifier,
            URI.create("urn:ok"),
            resource("schema-cyclic2_0.xml")));
        case "com.io7m.cycle1:1:0":
          return Optional.of(MSchemaCompilerStream.of(
            identifier,
            URI.create("urn:ok"),
            resource("schema-cyclic2_1.xml")));
        case "com.io7m.cycle2:1:0":
          return Optional.of(MSchemaCompilerStream.of(
            identifier,
            URI.create("urn:ok"),
            resource("schema-cyclic2_2.xml")));
        case "com.io7m.cycle3:1:0":
          return Optional.of(MSchemaCompilerStream.of(
            identifier,
            URI.create("urn:ok"),
            resource("schema-cyclic2_3.xml")));
        default:
          return Optional.empty();
      }
    }
  }

  /**
   * Cyclic imports are disallowed.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testCyclic0()
    throws Exception
  {
    final var sources = new CyclicSources();
    final var loader = this.loader(sources, this.showError());

    try (var compiler = this.createCompiler(
      this.showError(),
      this.messages,
      loader,
      URI.create("urn:test"),
      resource("schema-cyclic2_0.xml"))) {
      final var result = compiler.execute();
      Assertions.assertTrue(result.isEmpty());
      Assertions.assertTrue(this.errorsContain(CYCLIC_IMPORT));
    }
  }

  private static final class BrokenSource implements MSchemaCompilerSourceType
  {
    BrokenSource()
    {

    }

    @Override
    public Optional<MSchemaCompilerStream> openSchemaSource(
      final MetaSchemaIdentifier identifier)
      throws IOException
    {
      throw new IOException("Ouch");
    }
  }

  private static final class EmptySource implements MSchemaCompilerSourceType
  {
    EmptySource()
    {

    }

    @Override
    public Optional<MSchemaCompilerStream> openSchemaSource(
      final MetaSchemaIdentifier identifier)
      throws IOException
    {
      return Optional.empty();
    }
  }

  /**
   * Sources that raise I/O errors propagate errors.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testSourceIOError()
    throws Exception
  {
    final var sources = new BrokenSource();
    final var loader = this.loader(sources, this.showError());

    try (var compiler = this.createCompiler(
      this.showError(),
      this.messages,
      loader,
      URI.create("urn:test"),
      resource("meta-basic.xml"))) {
      final var result = compiler.execute();
      Assertions.assertTrue(result.isEmpty());
      Assertions.assertTrue(this.errorsContain(SOURCE_ERROR));
    }
  }

  /**
   * Sources that don't return modules propagate errors.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testSourceModuleMissing()
    throws Exception
  {
    final var sources = new EmptySource();
    final var loader = this.loader(sources, this.showError());

    try (var compiler = this.createCompiler(
      this.showError(),
      this.messages,
      loader,
      URI.create("urn:test"),
      resource("meta-basic.xml"))) {
      final var result = compiler.execute();
      Assertions.assertTrue(result.isEmpty());
      Assertions.assertTrue(this.errorsContain(SCHEMA_NONEXISTENT));
    }
  }

  /**
   * The basic schema is compiled correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testBasic0()
    throws Exception
  {
    final var sources = new BasicSource();
    final var loader = this.loader(sources, this.showError());

    try (var compiler = this.createCompiler(
      this.showError(),
      this.messages,
      loader,
      URI.create("urn:test"),
      resource("meta-basic.xml"))) {
      final var result = compiler.execute();
      Assertions.assertTrue(result.isPresent());

      final var schema = result.get();
      Assertions.assertEquals(
        MetaSchemaIdentifier.of(
          MetaSchemaName.of("com.io7m.example"),
          BigInteger.ONE,
          BigInteger.ZERO),
        schema.identifier());
    }
  }

  private static final class BasicSource implements MSchemaCompilerSourceType
  {
    BasicSource()
    {

    }

    @Override
    public Optional<MSchemaCompilerStream> openSchemaSource(
      final MetaSchemaIdentifier identifier)
      throws IOException
    {
      switch (identifier.show()) {
        case "com.io7m.basic:1:2": {
          return Optional.of(MSchemaCompilerStream.of(
            identifier,
            URI.create("urn:ok"),
            resource("meta-basic-12.xml")));
        }
        case "com.io7m.other:2:3": {
          return Optional.of(MSchemaCompilerStream.of(
            identifier,
            URI.create("urn:ok"),
            resource("meta-other-23.xml")));
        }
        default:
          return Optional.empty();
      }
    }
  }

  /**
   * The complete attributes schema is compiled correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testAttributeComplete0()
    throws Exception
  {
    final var sources = new BasicSource();
    final var loader = this.loader(sources, this.showError());

    try (var compiler = this.createCompiler(
      this.showError(),
      this.messages,
      loader,
      URI.create("urn:test"),
      resource("meta-attributes-complete.xml"))) {
      final var result = compiler.execute();
      Assertions.assertTrue(result.isPresent());

      final var schema = result.get();
      Assertions.assertEquals(
        MetaSchemaIdentifier.of(
          MetaSchemaName.of("com.io7m.complete"),
          BigInteger.ONE,
          BigInteger.ZERO),
        schema.identifier());

      final var byName = schema.attributesByName();
      for (final var tp : TypePrimitive.values()) {
        for (final var cardinality : AttributeCardinality.values()) {
          final var attrName =
            AttributeName.of(String.format(
              "%s_c%s",
              primitiveName(tp),
              cardinalityName(cardinality)));

          LOG.debug("checking {}", attrName.show());
          Assertions.assertTrue(byName.containsKey(attrName));
          final var attr = byName.get(attrName);
          Assertions.assertEquals(attrName, attr.name());
          Assertions.assertEquals(tp, attr.type().basePrimitiveType());
          Assertions.assertEquals(cardinality, attr.cardinality());
        }
      }

      final var attr = byName.get(AttributeName.of("named"));
      Assertions.assertEquals(CARDINALITY_1, attr.cardinality());
      Assertions.assertEquals(TYPE_INTEGER, attr.type().basePrimitiveType());
    }
  }

  private static String cardinalityName(
    final AttributeCardinality cardinality)
  {
    return cardinality.toString()
      .toLowerCase()
      .replace("_to_", "")
      .replace("cardinality_", "");
  }

  private static String primitiveName(final TypePrimitive tp)
  {
    return tp.toString()
      .toLowerCase()
      .replace("type_", "");
  }
}
