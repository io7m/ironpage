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

package com.io7m.ironpage.validator.vanilla;

import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.types.api.AttributeName;
import com.io7m.ironpage.types.api.AttributeNameQualified;
import com.io7m.ironpage.types.api.AttributeTypeBoolean;
import com.io7m.ironpage.types.api.AttributeTypeInteger;
import com.io7m.ironpage.types.api.AttributeTypeNameQualified;
import com.io7m.ironpage.types.api.AttributeTypeNamedType;
import com.io7m.ironpage.types.api.AttributeTypeReal;
import com.io7m.ironpage.types.api.AttributeTypeString;
import com.io7m.ironpage.types.api.AttributeTypeTimestamp;
import com.io7m.ironpage.types.api.AttributeTypeURI;
import com.io7m.ironpage.types.api.AttributeTypeUUID;
import com.io7m.ironpage.types.api.AttributeValueBoolean;
import com.io7m.ironpage.types.api.AttributeValueInteger;
import com.io7m.ironpage.types.api.AttributeValueReal;
import com.io7m.ironpage.types.api.AttributeValueString;
import com.io7m.ironpage.types.api.AttributeValueTimestamp;
import com.io7m.ironpage.types.api.AttributeValueTypedType;
import com.io7m.ironpage.types.api.AttributeValueURI;
import com.io7m.ironpage.types.api.AttributeValueUUID;
import com.io7m.ironpage.types.api.AttributeValueUntyped;
import com.io7m.ironpage.types.api.SchemaAttribute;
import com.io7m.ironpage.types.api.SchemaDeclaration;
import com.io7m.ironpage.types.api.SchemaIdentifier;
import com.io7m.ironpage.types.api.SchemaName;
import com.io7m.ironpage.validator.api.SchemaValidationError;
import com.io7m.ironpage.validator.api.SchemaValidationErrorReceiverType;
import com.io7m.ironpage.validator.api.SchemaValidationRequest;
import com.io7m.ironpage.validator.api.SchemaValidatorType;
import com.io7m.junreachable.UnreachableCodeException;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.collection.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The default schema validator implementation.
 */

public final class SchemaValidator implements SchemaValidatorType
{
  private static final Logger LOG = LoggerFactory.getLogger(SchemaValidator.class);

  private final SchemaValidatorMessages messages;

  SchemaValidator(
    final Locale inLocale)
  {
    this.messages =
      new SchemaValidatorMessages(Objects.requireNonNull(inLocale, "locale"));
  }

  private static void safeErrorPublish(
    final SchemaValidationErrorReceiverType receiver,
    final SchemaValidationError error)
  {
    try {
      receiver.receive(error);
    } catch (final Exception e) {
      LOG.error("ignored exception raised by error receiver: ", e);
    }
  }

  @Override
  public Seq<AttributeValueTypedType<?>> validate(
    final SchemaValidationRequest request,
    final SchemaValidationErrorReceiverType receiver)
  {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(receiver, "receiver");

    final var cardinality = new SchemaCardinalityCounter(request.attributes().size());
    return this.validateAttributes(request, receiver, cardinality);
  }

  private Seq<AttributeValueTypedType<?>> validateAttributes(
    final SchemaValidationRequest request,
    final SchemaValidationErrorReceiverType receiver,
    final SchemaCardinalityCounter cardinality)
  {
    var attributes = Vector.<AttributeValueTypedType<?>>empty();
    for (final var attribute : request.attributes()) {
      final var validation_result = this.validateAttribute(request, receiver, attribute);
      if (validation_result.isPresent()) {
        final var value = validation_result.get();
        cardinality.add(value.name());
        attributes = attributes.append(value);
      }
    }
    return attributes;
  }

  /**
   * Verify that the schema referred to by the attribute is actually imported.
   */

  private Optional<SchemaName> validateAttributeSchemaImported(
    final SchemaValidationRequest request,
    final SchemaValidationErrorReceiverType receiver,
    final AttributeValueUntyped attribute)
  {
    final var importsByName = request.importsByName();
    final var attributeName = attribute.name();
    final var schemaName = attributeName.schema();
    if (!importsByName.containsKey(schemaName)) {
      safeErrorPublish(receiver, this.errorSchemaNotImported(attributeName, importsByName));
      return Optional.empty();
    }
    return Optional.of(schemaName);
  }

  /**
   * Verify that the schema referred to by the attribute exists.
   */

  private Optional<SchemaDeclaration> validateAttributeSchemaExists(
    final SchemaValidationRequest request,
    final SchemaValidationErrorReceiverType receiver,
    final AttributeValueUntyped attribute,
    final SchemaName schemaName)
  {
    final var resolvedModules = request.resolvedModules();
    final var schemasByName = resolvedModules.schemasByName();
    if (!schemasByName.containsKey(schemaName)) {
      safeErrorPublish(receiver, this.errorSchemaNotFound(attribute.name(), schemasByName));
      return Optional.empty();
    }

    return schemasByName.get(schemaName)
      .toJavaOptional();
  }

  /**
   * Verify that the schema actually contains a definition for the attribute.
   */

  private Optional<Tuple2<SchemaDeclaration, SchemaAttribute>> validateAttributeSchemaContainsDefinition(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeValueUntyped attribute,
    final SchemaDeclaration schemaDeclaration)
  {
    final var schemaAttributesByName = schemaDeclaration.attributesByName();
    final var attributeName = attribute.name();
    final var attributeNameSimple = attributeName.name();
    if (!schemaAttributesByName.containsKey(attributeNameSimple)) {
      safeErrorPublish(
        receiver,
        this.errorSchemaAttributeNotFound(
          attributeName,
          schemaDeclaration.identifier(),
          schemaAttributesByName));
      return Optional.empty();
    }

    return schemaAttributesByName
      .get(attributeNameSimple)
      .toJavaOptional()
      .map(schemaAttribute -> Tuple.of(schemaDeclaration, schemaAttribute));
  }

  /**
   * Verify that the attribute type exists.
   */

  private Optional<Tuple2<AttributeTypeNameQualified, AttributeTypeNamedType>> validateAttributeTypeExists(
    final SchemaValidationRequest request,
    final SchemaValidationErrorReceiverType receiver,
    final AttributeValueUntyped attribute,
    final SchemaDeclaration schemaDeclaration)
  {
    final var resolvedModules = request.resolvedModules();
    final var schemaAttributesByName = schemaDeclaration.attributesByName();
    final var attributeName = attribute.name();
    final var attributeNameSimple = attributeName.name();
    final var schemaAttribute = schemaAttributesByName.get(attributeNameSimple).get();
    final var schemaAttributeTypeName = schemaAttribute.type();
    if (!resolvedModules.types().containsKey(schemaAttributeTypeName)) {
      safeErrorPublish(
        receiver,
        this.errorSchemaAttributeTypeNotFound(
          attributeName,
          schemaAttributeTypeName));
      return Optional.empty();
    }

    return resolvedModules.types()
      .get(schemaAttributeTypeName)
      .toJavaOptional()
      .map(type -> Tuple.of(schemaAttributeTypeName, type));
  }

  private Optional<? extends AttributeValueTypedType<?>> validateAttributeTypeValue(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeValueUntyped attribute,
    final AttributeTypeNameQualified typeName,
    final AttributeTypeNamedType type)
  {
    final var typeAnonymous = type.type();
    final var baseType = typeAnonymous.baseType();

    switch (baseType) {
      case ATTRIBUTE_TYPE_BOOLEAN:
        return this.validateAttributeBoolean(
          receiver,
          typeName,
          (AttributeTypeBoolean) typeAnonymous,
          attribute);
      case ATTRIBUTE_TYPE_INTEGER:
        return this.validateAttributeInteger(
          receiver,
          typeName,
          (AttributeTypeInteger) typeAnonymous,
          attribute);
      case ATTRIBUTE_TYPE_REAL:
        return this.validateAttributeReal(
          receiver,
          typeName,
          (AttributeTypeReal) typeAnonymous,
          attribute);
      case ATTRIBUTE_TYPE_STRING:
        return this.validateAttributeString(
          receiver,
          typeName,
          (AttributeTypeString) typeAnonymous,
          attribute);
      case ATTRIBUTE_TYPE_TIMESTAMP:
        return this.validateAttributeTimestamp(
          receiver,
          typeName,
          (AttributeTypeTimestamp) typeAnonymous,
          attribute);
      case ATTRIBUTE_TYPE_URI:
        return this.validateAttributeURI(
          receiver,
          typeName,
          (AttributeTypeURI) typeAnonymous,
          attribute);
      case ATTRIBUTE_TYPE_UUID:
        return this.validateAttributeUUID(
          receiver,
          typeName,
          (AttributeTypeUUID) typeAnonymous,
          attribute);
    }

    throw new UnreachableCodeException();
  }

  /**
   * Perform type checking for a single attribute.
   */

  private Optional<? extends AttributeValueTypedType<?>> validateAttribute(
    final SchemaValidationRequest request,
    final SchemaValidationErrorReceiverType receiver,
    final AttributeValueUntyped attribute)
  {
    return this.validateAttributeSchemaImported(request, receiver, attribute)
      .flatMap(name -> this.validateAttributeSchemaExists(request, receiver, attribute, name))
      .flatMap(schem -> this.validateAttributeSchemaContainsDefinition(receiver, attribute, schem))
      .flatMap(pair -> this.validateAttributeTypeExists(request, receiver, attribute, pair._1()))
      .flatMap(pair -> this.validateAttributeTypeValue(receiver, attribute, pair._1(), pair._2()));
  }

  /**
   * Construct a set of descriptive attributes for type errors.
   */

  private io.vavr.collection.TreeMap<String, String> typeErrorAttributes(
    final AttributeTypeNameQualified typeName,
    final AttributeValueUntyped attribute,
    final String baseType)
  {
    final var errorAttributes = new TreeMap<String, String>();
    errorAttributes.put(this.messages.localize("attribute"), attribute.name().show());
    errorAttributes.put(this.messages.localize("attributeBaseType"), baseType);
    errorAttributes.put(this.messages.localize("attributeType"), typeName.show());
    errorAttributes.put(this.messages.localize("attributeValue"), attribute.value());
    return io.vavr.collection.TreeMap.ofAll(errorAttributes);
  }

  /**
   * Check that an attribute value is a UUID.
   */

  private Optional<? extends AttributeValueTypedType<?>> validateAttributeUUID(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeTypeNameQualified typeName,
    final AttributeTypeUUID type,
    final AttributeValueUntyped attribute)
  {
    try {
      final var value = UUID.fromString(attribute.value());
      return Optional.of(AttributeValueUUID.of(attribute.name(), typeName, type, value));
    } catch (final Exception e) {
      safeErrorPublish(receiver, this.errorValueTypeError(typeName, attribute, e, "UUID"));
      return Optional.empty();
    }
  }

  /**
   * Check that an attribute value is a URI.
   */

  private Optional<? extends AttributeValueTypedType<?>> validateAttributeURI(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeTypeNameQualified typeName,
    final AttributeTypeURI type,
    final AttributeValueUntyped attribute)
  {
    try {
      final var value = new URI(attribute.value());
      return Optional.of(AttributeValueURI.of(attribute.name(), typeName, type, value));
    } catch (final Exception e) {
      safeErrorPublish(receiver, this.errorValueTypeError(typeName, attribute, e, "URI"));
      return Optional.empty();
    }
  }

  /**
   * Check that an attribute value is a timestamp.
   */

  private Optional<? extends AttributeValueTypedType<?>> validateAttributeTimestamp(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeTypeNameQualified typeName,
    final AttributeTypeTimestamp type,
    final AttributeValueUntyped attribute)
  {
    try {
      final var value = OffsetDateTime.parse(attribute.value());
      return Optional.of(AttributeValueTimestamp.of(attribute.name(), typeName, type, value));
    } catch (final Exception e) {
      safeErrorPublish(receiver, this.errorValueTypeError(typeName, attribute, e, "Timestamp"));
      return Optional.empty();
    }
  }

  /**
   * Check that an attribute value is a string.
   */

  private Optional<? extends AttributeValueTypedType<?>> validateAttributeString(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeTypeNameQualified typeName,
    final AttributeTypeString type,
    final AttributeValueUntyped attribute)
  {
    try {
      return Optional.of(AttributeValueString.of(
        attribute.name(),
        typeName,
        type,
        attribute.value()));
    } catch (final Exception e) {
      safeErrorPublish(receiver, this.errorValueTypeError(typeName, attribute, e, "String"));
      return Optional.empty();
    }
  }

  /**
   * Check that an attribute value is a real number.
   */

  private Optional<? extends AttributeValueTypedType<?>> validateAttributeReal(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeTypeNameQualified typeName,
    final AttributeTypeReal type,
    final AttributeValueUntyped attribute)
  {
    try {
      final var value = new BigDecimal(attribute.value());
      return Optional.of(AttributeValueReal.of(attribute.name(), typeName, type, value));
    } catch (final Exception e) {
      safeErrorPublish(receiver, this.errorValueTypeError(typeName, attribute, e, "Real"));
      return Optional.empty();
    }
  }

  /**
   * Check that an attribute value is an integer.
   */

  private Optional<? extends AttributeValueTypedType<?>> validateAttributeInteger(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeTypeNameQualified typeName,
    final AttributeTypeInteger type,
    final AttributeValueUntyped attribute)
  {
    try {
      final var value = new BigInteger(attribute.value());
      return Optional.of(AttributeValueInteger.of(attribute.name(), typeName, type, value));
    } catch (final Exception e) {
      safeErrorPublish(receiver, this.errorValueTypeError(typeName, attribute, e, "Integer"));
      return Optional.empty();
    }
  }

  /**
   * Check that an attribute value is a boolean.
   */

  private Optional<AttributeValueTypedType<Boolean>> validateAttributeBoolean(
    final SchemaValidationErrorReceiverType receiver,
    final AttributeTypeNameQualified typeName,
    final AttributeTypeBoolean type,
    final AttributeValueUntyped attribute)
  {
    final var text = attribute.value();
    final var name = attribute.name();
    switch (text) {
      case "true": {
        return Optional.of(AttributeValueBoolean.of(name, typeName, type, Boolean.TRUE));
      }
      case "false": {
        return Optional.of(AttributeValueBoolean.of(name, typeName, type, Boolean.FALSE));
      }
      default: {
        safeErrorPublish(
          receiver,
          this.errorValueTypeError(typeName, attribute, new Exception(), "Boolean"));
        return Optional.empty();
      }
    }
  }

  private SchemaValidationError errorValueTypeError(
    final AttributeTypeNameQualified typeName,
    final AttributeValueUntyped attribute,
    final Exception e,
    final String value)
  {
    return SchemaValidationError.builder()
      .setAttributes(this.typeErrorAttributes(typeName, attribute, value))
      .setErrorCode(SchemaValidatorType.SCHEMA_ATTRIBUTE_TYPE_ERROR)
      .setException(e)
      .setMessage(this.messages.localize("schemaAttributeTypeInvalid"))
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

  private SchemaValidationError errorSchemaAttributeTypeNotFound(
    final AttributeNameQualified attributeName,
    final AttributeTypeNameQualified schemaAttributeTypeName)
  {
    final var errorAttributes = new TreeMap<String, String>();
    errorAttributes.put(this.messages.localize("attribute"), attributeName.name().name());
    errorAttributes.put(this.messages.localize("attributeType"), schemaAttributeTypeName.show());

    return SchemaValidationError.builder()
      .setAttributes(io.vavr.collection.TreeMap.ofAll(errorAttributes))
      .setErrorCode(SchemaValidatorType.SCHEMA_TYPE_NOT_FOUND)
      .setMessage(this.messages.localize("schemaTypeNotFound"))
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

  private SchemaValidationError errorSchemaNotImported(
    final AttributeNameQualified attributeName,
    final SortedMap<SchemaName, SchemaIdentifier> importsByName)
  {
    final var errorAttributes = new TreeMap<String, String>();
    errorAttributes.put(this.messages.localize("attribute"), attributeName.name().name());
    errorAttributes.put(this.messages.localize("schema"), attributeName.schema().name());

    importsByName.forEach(
      (key, value) -> {
        final var name = MessageFormat.format(this.messages.localize("schemaImported"), key.name());
        errorAttributes.put(name, value.show());
      });

    return SchemaValidationError.builder()
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SchemaValidatorType.SCHEMA_NOT_IMPORTED)
      .setMessage(this.messages.localize("schemaNotImported"))
      .setAttributes(io.vavr.collection.TreeMap.ofAll(errorAttributes))
      .build();
  }

  private SchemaValidationError errorSchemaNotFound(
    final AttributeNameQualified attributeName,
    final Map<SchemaName, SchemaDeclaration> schemasByName)
  {
    final var errorAttributes = new TreeMap<String, String>();
    errorAttributes.put(this.messages.localize("attribute"), attributeName.name().name());
    errorAttributes.put(this.messages.localize("schema"), attributeName.schema().name());

    schemasByName.forEach(
      (key, value) -> {
        final var name = MessageFormat.format(
          this.messages.localize("schemaAvailable"),
          key.name());
        errorAttributes.put(name, value.identifier().show());
      });

    return SchemaValidationError.builder()
      .setAttributes(io.vavr.collection.TreeMap.ofAll(errorAttributes))
      .setErrorCode(SchemaValidatorType.SCHEMA_NOT_FOUND)
      .setMessage(this.messages.localize("schemaNotFound"))
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

  private SchemaValidationError errorSchemaAttributeNotFound(
    final AttributeNameQualified attributeName,
    final SchemaIdentifier schemaIdentifier,
    final SortedMap<AttributeName, SchemaAttribute> schemaAttributesByName)
  {
    final var errorAttributes = new TreeMap<String, String>();
    errorAttributes.put(this.messages.localize("attribute"), attributeName.name().name());
    errorAttributes.put(this.messages.localize("schema"), schemaIdentifier.show());

    final var builder = SchemaValidationError.builder();
    schemaAttributesByName.forEach(
      (key, value) -> {
        builder.addMessageExtras(MessageFormat.format(
          this.messages.localize("schemaAttributeTyped"),
          key.name(),
          value.type().show(),
          value.cardinality().show()));
      });

    return builder
      .setAttributes(io.vavr.collection.TreeMap.ofAll(errorAttributes))
      .setErrorCode(SchemaValidatorType.SCHEMA_ATTRIBUTE_NOT_FOUND)
      .setMessage(this.messages.localize("schemaAttributeNotFound"))
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .build();
  }

}
