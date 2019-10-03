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

import com.io7m.ironpage.types.api.AttributeCardinality;
import com.io7m.ironpage.types.api.AttributeName;
import com.io7m.ironpage.types.api.AttributeNameQualified;
import com.io7m.ironpage.types.api.AttributeTypeAnonymousType;
import com.io7m.ironpage.types.api.AttributeTypeBoolean;
import com.io7m.ironpage.types.api.AttributeTypeInteger;
import com.io7m.ironpage.types.api.AttributeTypeName;
import com.io7m.ironpage.types.api.AttributeTypeNameQualified;
import com.io7m.ironpage.types.api.AttributeTypeNamed;
import com.io7m.ironpage.types.api.AttributeTypeReal;
import com.io7m.ironpage.types.api.AttributeTypeString;
import com.io7m.ironpage.types.api.AttributeTypeTimestamp;
import com.io7m.ironpage.types.api.AttributeTypeURI;
import com.io7m.ironpage.types.api.AttributeTypeUUID;
import com.io7m.ironpage.types.api.AttributeValueUntyped;
import com.io7m.ironpage.types.api.SchemaAttribute;
import com.io7m.ironpage.types.api.SchemaDeclaration;
import com.io7m.ironpage.types.api.SchemaIdentifier;
import com.io7m.ironpage.types.api.SchemaName;
import com.io7m.ironpage.types.resolution.api.SchemaResolvedSet;
import com.io7m.ironpage.types.resolution.api.SchemaResolvedSetEdge;
import com.io7m.ironpage.validator.api.SchemaValidationError;
import com.io7m.ironpage.validator.api.SchemaValidationErrorReceiverType;
import com.io7m.ironpage.validator.api.SchemaValidationRequest;
import com.io7m.ironpage.validator.api.SchemaValidatorProviderType;
import com.io7m.ironpage.validator.api.SchemaValidatorType;
import io.vavr.collection.Vector;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

public abstract class SchemaValidatorContract
{
  protected abstract Logger logger();

  protected abstract SchemaValidatorProviderType validators();

  /**
   * Validating an empty set of attributes that doesn't import any schemas succeeds.
   */

  @Test
  public final void testEmpty()
  {
    final var resolved =
      SchemaResolvedSet.builder()
        .setGraph(new DirectedAcyclicGraph<>(SchemaResolvedSetEdge.class))
        .setSchemas(Vector.empty())
        .build();

    final var validators = this.validators();
    final var validator = validators.create();

    final var request =
      SchemaValidationRequest.builder()
        .setResolvedModules(resolved)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(0, result.size());
  }

  private static SchemaValidationErrorReceiverType appendError(
    final ArrayList<SchemaValidationError> errors)
  {
    return error -> {
      errors.add(error);
      throw new IllegalStateException("Ignored!");
    };
  }

  /**
   * Failing to import a schema is a validation error.
   */

  @Test
  public final void testSchemaNotImported()
  {
    final var resolved =
      SchemaResolvedSet.builder()
        .setGraph(new DirectedAcyclicGraph<>(SchemaResolvedSetEdge.class))
        .setSchemas(Vector.empty())
        .build();

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("23")
          .build()
      );

    final var request =
      SchemaValidationRequest.builder()
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_NOT_IMPORTED, error.errorCode());
    }
  }

  /**
   * Importing a nonexistent schema is a validation error.
   */

  @Test
  public final void testSchemaNonexistent()
  {
    final var resolved =
      SchemaResolvedSet.builder()
        .setGraph(new DirectedAcyclicGraph<>(SchemaResolvedSetEdge.class))
        .setSchemas(Vector.empty())
        .build();

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("23")
          .build()
      );

    final var imports =
      Vector.of(
        SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO)
      );

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_NOT_FOUND, error.errorCode());
    }
  }

  /**
   * If an attribute doesn't exist within the intended schema, validation fails.
   */

  @Test
  public final void testSchemaAttributeNonexistent()
  {
    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var schema =
      SchemaDeclaration.builder()
        .setIdentifier(schemaId)
        .build();

    final var resolved =
      SchemaResolvedSet.builder()
        .setGraph(new DirectedAcyclicGraph<>(SchemaResolvedSetEdge.class))
        .setSchemas(Vector.of(schema))
        .build();

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("23")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_ATTRIBUTE_NOT_FOUND, error.errorCode());
    }
  }

  /**
   * If a schema refers to a type that doesn't exist, validation fails.
   */

  @Test
  public final void testSchemaAttributeTypeNonexistent()
  {
    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var attribute0 =
      SchemaAttribute.of(
        AttributeName.of("y"),
        AttributeTypeNameQualified.of(SchemaName.of("z"), AttributeTypeName.of("int")),
        AttributeCardinality.CARDINALITY_1);

    final var schemaAttributes =
      Vector.of(
        attribute0
      );

    final var schema =
      SchemaDeclaration.builder()
        .setIdentifier(schemaId)
        .setAttributes(schemaAttributes)
        .build();

    final var resolved =
      SchemaResolvedSet.builder()
        .setGraph(new DirectedAcyclicGraph<>(SchemaResolvedSetEdge.class))
        .setSchemas(Vector.of(schema))
        .build();

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("23")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_TYPE_NOT_FOUND, error.errorCode());
    }
  }

  /**
   * If an integer-typed attribute doesn't have an integer value, validation fails.
   */

  @Test
  public final void testSchemaAttributeTypeIntegerInvalid()
  {
    final var attributeType =
      AttributeTypeInteger.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("not an integer")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_ATTRIBUTE_TYPE_ERROR, error.errorCode());
    }
  }

  /**
   * If an integer-typed attribute has an integer value, validation succeeds.
   */

  @Test
  public final void testSchemaAttributeTypeIntegerValid()
  {
    final var attributeType =
      AttributeTypeInteger.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("23")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(1, result.size());

    {
      final var value = result.get(0);
      Assertions.assertEquals(BigInteger.valueOf(23L), value.value());
    }
  }

  /**
   * If a uuid-typed attribute doesn't have a uuid value, validation fails.
   */

  @Test
  public final void testSchemaAttributeTypeUUIDInvalid()
  {
    final var attributeType =
      AttributeTypeUUID.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("not a uuid")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_ATTRIBUTE_TYPE_ERROR, error.errorCode());
    }
  }

  /**
   * If a uuid-typed attribute has a uuid value, validation succeeds.
   */

  @Test
  public final void testSchemaAttributeTypeUUIDValid()
  {
    final var attributeType =
      AttributeTypeUUID.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("1af3ba26-20e4-4517-aad1-31c8c719a051")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(1, result.size());

    {
      final var value = result.get(0);
      Assertions.assertEquals(UUID.fromString("1af3ba26-20e4-4517-aad1-31c8c719a051"), value.value());
    }
  }

  /**
   * If a uri-typed attribute doesn't have a uri value, validation fails.
   */

  @Test
  public final void testSchemaAttributeTypeURIInvalid()
  {
    final var attributeType =
      AttributeTypeURI.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("% not a uri")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_ATTRIBUTE_TYPE_ERROR, error.errorCode());
    }
  }

  /**
   * If a uri-typed attribute has a uri value, validation succeeds.
   */

  @Test
  public final void testSchemaAttributeTypeURIValid()
  {
    final var attributeType =
      AttributeTypeURI.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("http://www.example.com")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(1, result.size());

    {
      final var value = result.get(0);
      Assertions.assertEquals(URI.create("http://www.example.com"), value.value());
    }
  }

  /**
   * If an boolean-typed attribute doesn't have an boolean value, validation fails.
   */

  @Test
  public final void testSchemaAttributeTypeBooleanInvalid()
  {
    final var attributeType =
      AttributeTypeBoolean.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("23")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_ATTRIBUTE_TYPE_ERROR, error.errorCode());
    }
  }

  /**
   * If an boolean-typed attribute has an boolean value, validation succeeds.
   */

  @Test
  public final void testSchemaAttributeTypeBooleanValid0()
  {
    final var attributeType =
      AttributeTypeBoolean.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("false")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(1, result.size());

    {
      final var value = result.get(0);
      Assertions.assertEquals(Boolean.FALSE, value.value());
    }
  }

  /**
   * If an boolean-typed attribute has an boolean value, validation succeeds.
   */

  @Test
  public final void testSchemaAttributeTypeBooleanValid1()
  {
    final var attributeType =
      AttributeTypeBoolean.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("true")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(1, result.size());

    {
      final var value = result.get(0);
      Assertions.assertEquals(Boolean.TRUE, value.value());
    }
  }

  /**
   * If an string-typed attribute has an string value, validation succeeds. It's impossible
   * for any attribute not to have a string-compatible value.
   */

  @Test
  public final void testSchemaAttributeTypeStringValid()
  {
    final var attributeType =
      AttributeTypeString.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("1af3ba26-20e4-4517-aad1-31c8c719a051")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(1, result.size());

    {
      final var value = result.get(0);
      Assertions.assertEquals("1af3ba26-20e4-4517-aad1-31c8c719a051", value.value());
    }
  }

  /**
   * If a time-typed attribute doesn't have a timestamp value, validation fails.
   */

  @Test
  public final void testSchemaAttributeTypeTimestampInvalid()
  {
    final var attributeType =
      AttributeTypeTimestamp.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("% not a timestamp")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_ATTRIBUTE_TYPE_ERROR, error.errorCode());
    }
  }

  /**
   * If a timestamp-typed attribute has a timestamp value, validation succeeds.
   */

  @Test
  public final void testSchemaAttributeTypeTimestampValid()
  {
    final var attributeType =
      AttributeTypeTimestamp.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("2019-01-01T00:00:00+00:00")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(1, result.size());

    {
      final var value = result.get(0);
      Assertions.assertEquals(OffsetDateTime.parse("2019-01-01T00:00:00+00:00"), value.value());
    }
  }

  /**
   * If a real-typed attribute doesn't have a real value, validation fails.
   */

  @Test
  public final void testSchemaAttributeTypeRealInvalid()
  {
    final var attributeType =
      AttributeTypeReal.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("% not a real")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(1, errors.size());
    Assertions.assertEquals(0, result.size());

    {
      final var error = errors.remove(0);
      Assertions.assertEquals(SchemaValidatorType.SCHEMA_ATTRIBUTE_TYPE_ERROR, error.errorCode());
    }
  }

  /**
   * If a real-typed attribute has a real value, validation succeeds.
   */

  @Test
  public final void testSchemaAttributeTypeRealValid()
  {
    final var attributeType =
      AttributeTypeReal.builder().build();

    final var schemaId =
      SchemaIdentifier.of(SchemaName.of("x"), BigInteger.ONE, BigInteger.ZERO);

    final var resolved =
      singleAttributeSchema(schemaId, attributeType);

    final var validators = this.validators();
    final var validator = validators.create();

    final var attributes =
      Vector.of(
        AttributeValueUntyped.builder()
          .setName(AttributeNameQualified.of(SchemaName.of("x"), AttributeName.of("y")))
          .setValue("23.35")
          .build()
      );

    final var imports =
      Vector.of(schemaId);

    final var request =
      SchemaValidationRequest.builder()
        .setImports(imports)
        .setResolvedModules(resolved)
        .setAttributes(attributes)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, appendError(errors));
    errors.forEach(this::logError);

    Assertions.assertEquals(0, errors.size());
    Assertions.assertEquals(1, result.size());

    {
      final var value = result.get(0);
      Assertions.assertEquals(new BigDecimal("23.35"), value.value());
    }
  }
  
  
  private static SchemaResolvedSet singleAttributeSchema(
    final SchemaIdentifier schemaId,
    final AttributeTypeAnonymousType attributeType)
  {
    final var attribute0 =
      SchemaAttribute.of(
        AttributeName.of("y"),
        AttributeTypeNameQualified.of(SchemaName.of("x"), AttributeTypeName.of("int")),
        AttributeCardinality.CARDINALITY_1);

    final var schemaAttributes =
      Vector.of(
        attribute0
      );

    final var schemaTypes =
      Vector.of(
        AttributeTypeNamed.of(AttributeTypeName.of("int"), attributeType)
      );

    final var schema =
      SchemaDeclaration.builder()
        .setIdentifier(schemaId)
        .setAttributes(schemaAttributes)
        .setTypes(schemaTypes)
        .build();

    return SchemaResolvedSet.builder()
      .setGraph(new DirectedAcyclicGraph<>(SchemaResolvedSetEdge.class))
      .setSchemas(Vector.of(schema))
      .build();
  }

  private void logError(
    final SchemaValidationError error)
  {
    this.logger().error(
      "{}: {}: {}",
      error.errorCode().code(),
      error.message(),
      error.attributes());
  }
}
