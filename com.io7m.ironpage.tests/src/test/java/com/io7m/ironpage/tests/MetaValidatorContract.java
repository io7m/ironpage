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

import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorError;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorErrorCode;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorErrorReceiverType;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorRequest;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerError;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerSourceType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerStream;
import com.io7m.ironpage.metadata.schema.types.api.MetaDocumentUntyped;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifiers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorType.ATTRIBUTE_CARDINALITY_ERROR;
import static com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorType.ATTRIBUTE_NOT_FOUND;
import static com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorType.SCHEMA_NOT_FOUND;
import static com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorType.TYPE_ERROR;
import static com.io7m.ironpage.metadata.schema.types.api.AttributeValueUntypeds.untypedOf;

public abstract class MetaValidatorContract
{
  private static final Logger LOG = LoggerFactory.getLogger(MetaValidatorContract.class);

  protected ArrayList<MSchemaCompilerError> errors;
  protected ArrayList<MetaValidatorError> validationErrors;

  protected static InputStream resource(
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

  private boolean errorsContain(
    final MetaValidatorErrorCode code)
  {
    return this.validationErrors.stream().anyMatch(e -> e.errorCode().equals(code));
  }

  final MSchemaCompilerErrorConsumerType showCompilerError()
  {
    return e -> {
      LOG.debug("error: {}", e);
      this.errors.add(e);
      throw new IllegalStateException("Bad consumer!");
    };
  }

  final MetaValidatorErrorReceiverType showValidatorError()
  {
    return e -> {
      LOG.debug("error: {}", e);
      this.validationErrors.add(e);
      throw new IllegalStateException("Bad consumer!");
    };
  }

  @BeforeEach
  public final void testSetup()
  {
    this.errors = new ArrayList<>();
    this.validationErrors = new ArrayList<>();
  }

  protected abstract MetaSchema schema(
    MSchemaCompilerSourceType source,
    String file)
    throws Exception;

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
   * A complete document is validated correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testAttributesComplete()
    throws Exception
  {
    final var schema = this.schema(new BasicSource(), "meta-attributes-complete.xml");

    final var uuid = "d9fbf7de-639d-47ee-83a3-257719ab0308";
    final var integer = "23";
    final var real = "23.0";
    final var uri = "urn:uuid:d9fbf7de-639d-47ee-83a3-257719ab0308";
    final var time = "2019-10-11T20:55:47+01:00";

    final var document =
      MetaDocumentUntyped.builder()
        .setUri(URI.create("urn:document"))
        .addImports(MetaSchemaIdentifiers.create("com.io7m.complete", 1, 0))
        .addAttributes(untypedOf("com.io7m.complete", "integer_c1", integer))
        .addAttributes(untypedOf("com.io7m.complete", "integer_c0n", integer))
        .addAttributes(untypedOf("com.io7m.complete", "integer_c0n", integer))
        .addAttributes(untypedOf("com.io7m.complete", "integer_c1n", integer))
        .addAttributes(untypedOf("com.io7m.complete", "integer_c1n", integer))
        .addAttributes(untypedOf("com.io7m.complete", "boolean_c1", "true"))
        .addAttributes(untypedOf("com.io7m.complete", "boolean_c0n", "false"))
        .addAttributes(untypedOf("com.io7m.complete", "boolean_c0n", "true"))
        .addAttributes(untypedOf("com.io7m.complete", "boolean_c1n", "false"))
        .addAttributes(untypedOf("com.io7m.complete", "boolean_c1n", "true"))
        .addAttributes(untypedOf("com.io7m.complete", "string_c1", integer))
        .addAttributes(untypedOf("com.io7m.complete", "string_c0n", integer))
        .addAttributes(untypedOf("com.io7m.complete", "string_c0n", integer))
        .addAttributes(untypedOf("com.io7m.complete", "string_c1n", integer))
        .addAttributes(untypedOf("com.io7m.complete", "string_c1n", integer))
        .addAttributes(untypedOf("com.io7m.complete", "real_c1", real))
        .addAttributes(untypedOf("com.io7m.complete", "real_c0n", real))
        .addAttributes(untypedOf("com.io7m.complete", "real_c0n", real))
        .addAttributes(untypedOf("com.io7m.complete", "real_c1n", real))
        .addAttributes(untypedOf("com.io7m.complete", "real_c1n", real))
        .addAttributes(untypedOf("com.io7m.complete", "uuid_c1", uuid))
        .addAttributes(untypedOf("com.io7m.complete", "uuid_c0n", uuid))
        .addAttributes(untypedOf("com.io7m.complete", "uuid_c0n", uuid))
        .addAttributes(untypedOf("com.io7m.complete", "uuid_c1n", uuid))
        .addAttributes(untypedOf("com.io7m.complete", "uuid_c1n", uuid))
        .addAttributes(untypedOf("com.io7m.complete", "uri_c1", uri))
        .addAttributes(untypedOf("com.io7m.complete", "uri_c0n", uri))
        .addAttributes(untypedOf("com.io7m.complete", "uri_c0n", uri))
        .addAttributes(untypedOf("com.io7m.complete", "uri_c1n", uri))
        .addAttributes(untypedOf("com.io7m.complete", "uri_c1n", uri))
        .addAttributes(untypedOf("com.io7m.complete", "timestamp_c1", time))
        .addAttributes(untypedOf("com.io7m.complete", "timestamp_c0n", time))
        .addAttributes(untypedOf("com.io7m.complete", "timestamp_c0n", time))
        .addAttributes(untypedOf("com.io7m.complete", "timestamp_c1n", time))
        .addAttributes(untypedOf("com.io7m.complete", "timestamp_c1n", time))
        .addAttributes(untypedOf("com.io7m.complete", "named", integer))
        .build();

    final var request =
      MetaValidatorRequest.builder()
        .addSchemas(schema)
        .setDocument(document)
        .build();

    final var validator =
      this.validator(this.showValidatorError(), request);

    final var result = validator.execute();
    Assertions.assertTrue(result.isPresent());

    final var typed = result.get();
    Assertions.assertEquals(document.imports(), typed.imports());
    Assertions.assertEquals(document.attributes().size(), typed.attributes().size());

    for (var index = 0; index < typed.attributes().size(); ++index) {
      final var aTyped = typed.attributes().get(index);
      final var aUntyped = document.attributes().get(index);
      final var typedName = aTyped.name();
      final var typedValue = aTyped.value();
      final var untypedValue = aUntyped.value();
      LOG.debug("{} : {} = {}", typedName.show(), typedValue, untypedValue);
      Assertions.assertEquals(typedValue.toString(), untypedValue);
    }
  }

  /**
   * An attribute with cardinality 1 requires one value.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testAttributesCardinalityMissing0()
    throws Exception
  {
    final var schema = this.schema(new BasicSource(), "meta-cardinality0.xml");

    final var document =
      MetaDocumentUntyped.builder()
        .setUri(URI.create("urn:document"))
        .addImports(MetaSchemaIdentifiers.create("com.io7m.cardinality", 1, 0))
        .build();

    final var request =
      MetaValidatorRequest.builder()
        .addSchemas(schema)
        .setDocument(document)
        .build();

    final var validator =
      this.validator(this.showValidatorError(), request);

    final var result = validator.execute();
    Assertions.assertTrue(result.isEmpty());
    Assertions.assertTrue(this.errorsContain(ATTRIBUTE_CARDINALITY_ERROR));
  }

  /**
   * An attribute with cardinality 1..n requires one value.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testAttributesCardinalityMissing1()
    throws Exception
  {
    final var schema = this.schema(new BasicSource(), "meta-cardinality2.xml");

    final var document =
      MetaDocumentUntyped.builder()
        .setUri(URI.create("urn:document"))
        .addImports(MetaSchemaIdentifiers.create("com.io7m.cardinality", 1, 0))
        .build();

    final var request =
      MetaValidatorRequest.builder()
        .addSchemas(schema)
        .setDocument(document)
        .build();

    final var validator =
      this.validator(this.showValidatorError(), request);

    final var result = validator.execute();
    Assertions.assertTrue(result.isEmpty());
    Assertions.assertTrue(this.errorsContain(ATTRIBUTE_CARDINALITY_ERROR));
  }

  /**
   * An attribute with cardinality 0..1 requires at most value.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testAttributesCardinalityTooMany()
    throws Exception
  {
    final var schema = this.schema(new BasicSource(), "meta-cardinality1.xml");

    final var document =
      MetaDocumentUntyped.builder()
        .setUri(URI.create("urn:document"))
        .addImports(MetaSchemaIdentifiers.create("com.io7m.cardinality", 1, 0))
        .addAttributes(untypedOf("com.io7m.cardinality", "a", "23"))
        .addAttributes(untypedOf("com.io7m.cardinality", "a", "24"))
        .build();

    final var request =
      MetaValidatorRequest.builder()
        .addSchemas(schema)
        .setDocument(document)
        .build();

    final var validator =
      this.validator(this.showValidatorError(), request);

    final var result = validator.execute();
    Assertions.assertTrue(result.isEmpty());
    Assertions.assertTrue(this.errorsContain(ATTRIBUTE_CARDINALITY_ERROR));
  }

  /**
   * Every possible value type error is caught correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testAttributesTypes()
    throws Exception
  {
    final var schema = this.schema(new BasicSource(), "meta-types0.xml");

    final var document =
      MetaDocumentUntyped.builder()
        .setUri(URI.create("urn:document"))
        .addImports(MetaSchemaIdentifiers.create("com.io7m.types", 1, 0))
        .addAttributes(untypedOf("com.io7m.types", "boolean", "not boolean"))
        .addAttributes(untypedOf("com.io7m.types", "integer", "x"))
        .addAttributes(untypedOf("com.io7m.types", "real", "q"))
        .addAttributes(untypedOf("com.io7m.types", "string", "s"))
        .addAttributes(untypedOf("com.io7m.types", "timestamp", "not time"))
        .addAttributes(untypedOf("com.io7m.types", "uri", "& # ~ a"))
        .addAttributes(untypedOf("com.io7m.types", "uuid", "& # ~ a"))
        .build();

    final var request =
      MetaValidatorRequest.builder()
        .addSchemas(schema)
        .setDocument(document)
        .build();

    final var validator =
      this.validator(this.showValidatorError(), request);

    final var result = validator.execute();
    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(6, this.validationErrors.size());
    Assertions.assertEquals(
      6,
      this.validationErrors.stream()
        .filter(e -> Objects.equals(e.errorCode(), TYPE_ERROR))
        .count());
  }

  /**
   * Undeclared attributes are disallowed.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testAttributesExtra()
    throws Exception
  {
    final var schema = this.schema(new BasicSource(), "meta-types0.xml");

    final var document =
      MetaDocumentUntyped.builder()
        .setUri(URI.create("urn:document"))
        .addImports(MetaSchemaIdentifiers.create("com.io7m.types", 1, 0))
        .addAttributes(untypedOf("com.io7m.types", "undeclared", "x"))
        .build();

    final var request =
      MetaValidatorRequest.builder()
        .addSchemas(schema)
        .setDocument(document)
        .build();

    final var validator =
      this.validator(this.showValidatorError(), request);

    final var result = validator.execute();
    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(1, this.validationErrors.size());
    Assertions.assertTrue(this.errorsContain(ATTRIBUTE_NOT_FOUND));
  }

  /**
   * Undeclared schemas are disallowed.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testAttributesUndeclared()
    throws Exception
  {
    final var schema = this.schema(new BasicSource(), "meta-types0.xml");

    final var document =
      MetaDocumentUntyped.builder()
        .setUri(URI.create("urn:document"))
        .addImports(MetaSchemaIdentifiers.create("com.io7m.types", 1, 0))
        .addAttributes(untypedOf("com.io7m.undeclared", "undeclared", "x"))
        .build();

    final var request =
      MetaValidatorRequest.builder()
        .addSchemas(schema)
        .setDocument(document)
        .build();

    final var validator =
      this.validator(this.showValidatorError(), request);

    final var result = validator.execute();
    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(1, this.validationErrors.size());
    Assertions.assertTrue(this.errorsContain(SCHEMA_NOT_FOUND));
  }

  /**
   * Importing schemas that aren't available is an error.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testImportSchemaNonexistent()
    throws Exception
  {
    final var schema = this.schema(new BasicSource(), "meta-types0.xml");

    final var document =
      MetaDocumentUntyped.builder()
        .setUri(URI.create("urn:document"))
        .addImports(MetaSchemaIdentifiers.create("com.io7m.nonexistent", 1, 0))
        .build();

    final var request =
      MetaValidatorRequest.builder()
        .addSchemas(schema)
        .setDocument(document)
        .build();

    final var validator =
      this.validator(this.showValidatorError(), request);

    final var result = validator.execute();
    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(1, this.validationErrors.size());
    Assertions.assertTrue(this.errorsContain(SCHEMA_NOT_FOUND));
  }

  protected abstract MetaValidatorType validator(
    MetaValidatorErrorReceiverType validationErrors,
    MetaValidatorRequest request);


}
