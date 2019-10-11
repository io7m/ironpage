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

package com.io7m.ironpage.metadata.attribute.validator.vanilla;

import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorError;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorErrorReceiverType;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorMessagesType;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorRequest;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorType;
import com.io7m.ironpage.metadata.schema.types.api.AttributeNameQualified;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueBoolean;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueInteger;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueReal;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueString;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueTimestamp;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueTypedType;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueURI;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueUUID;
import com.io7m.ironpage.metadata.schema.types.api.AttributeValueUntyped;
import com.io7m.ironpage.metadata.schema.types.api.MetaDocumentTyped;
import com.io7m.ironpage.metadata.schema.types.api.MetaDocumentUntyped;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaAttribute;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.TypePrimitive;
import com.io7m.ironpage.metadata.schema.types.api.TypeReferenceType;
import com.io7m.ironpage.presentable.api.PresentableAttributes;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

final class MetaValidator implements MetaValidatorType
{
  private static final Logger LOG = LoggerFactory.getLogger(MetaValidator.class);

  private final SafeErrorPublisher errors;
  private final MetaValidatorMessagesType messages;
  private final MetaValidatorRequest request;
  private final HashMap<AttributeNameQualified, CardinalityValidator> cardinalities;

  MetaValidator(
    final MetaValidatorErrorReceiverType inErrors,
    final MetaValidatorMessagesType inMessages,
    final MetaValidatorRequest inRequest)
  {
    this.errors =
      new SafeErrorPublisher(Objects.requireNonNull(inErrors, "errors"));
    this.messages =
      Objects.requireNonNull(inMessages, "messages");
    this.request =
      Objects.requireNonNull(inRequest, "inRequest");

    this.cardinalities = new HashMap<>();
  }

  @Override
  public Optional<MetaDocumentTyped> execute()
  {
    final var document = this.request.document();
    this.checkInitializeCardinalities(document);

    final var attributes = document.attributes();
    final var results = new ArrayList<AttributeValueTypedType<?>>(attributes.size());
    for (final var value : attributes) {
      final var result = this.checkAttribute(value);
      result.ifPresent(results::add);
    }

    this.checkCardinalitiesResults();
    if (this.errors.isFailed()) {
      return Optional.empty();
    }

    return Optional.of(
      MetaDocumentTyped.builder()
        .addAllImports(document.imports())
        .addAllAttributes(results)
        .setUri(document.uri())
        .build());
  }

  private void checkCardinalitiesResults()
  {
    for (final var cardinalityEntry : this.cardinalities.entrySet()) {
      LOG.debug("checking cardinality {}", cardinalityEntry.getKey().show());

      final var cardinalityRecord = cardinalityEntry.getValue();
      switch (cardinalityRecord.attribute.cardinality()) {
        case CARDINALITY_1: {
          if (cardinalityRecord.count != 1) {
            this.errors.receive(this.errorCardinality(
              cardinalityRecord.attribute, cardinalityRecord.count));
          }
          break;
        }
        case CARDINALITY_0_TO_1: {
          if (cardinalityRecord.count > 1) {
            this.errors.receive(this.errorCardinality(
              cardinalityRecord.attribute, cardinalityRecord.count));
          }
          break;
        }
        case CARDINALITY_0_TO_N: {
          break;
        }
        case CARDINALITY_1_TO_N: {
          if (cardinalityRecord.count < 1) {
            this.errors.receive(this.errorCardinality(
              cardinalityRecord.attribute, cardinalityRecord.count));
          }
          break;
        }
      }
    }
  }

  private void checkInitializeCardinalities(
    final MetaDocumentUntyped document)
  {
    LOG.trace("initializing cardinalities");

    final var imports = document.imports();
    for (final var importId : imports) {
      LOG.trace("initializing schema {}", importId.show());

      final var schemaOpt = this.request.findSchema(importId);
      if (schemaOpt.isEmpty()) {
        this.errors.receive(this.errorSchemaNotFound(importId));
        continue;
      }

      final var schema = schemaOpt.get();
      for (final var attribute : schema.attributes()) {
        final var nameQ = AttributeNameQualified.of(schema.identifier().name(), attribute.name());
        LOG.trace("initializing attribute {}", nameQ.show());

        Preconditions.checkPreconditionV(
          !this.cardinalities.containsKey(nameQ),
          "Only one qualified attribute name '%s' can be in scope",
          nameQ.show());

        this.cardinalities.put(nameQ, new CardinalityValidator(attribute));
      }
    }
  }

  private Optional<AttributeValueTypedType<?>> checkAttribute(
    final AttributeValueUntyped value)
  {
    return this.findSchemaFor(value).flatMap(schema -> {
      return this.checkAttributeType(schema, value).map(typed -> {
        this.cardinalities.get(typed.name()).addOccurence();
        return typed;
      });
    });
  }

  private Optional<AttributeValueTypedType<?>> checkAttributeType(
    final MetaSchema schema,
    final AttributeValueUntyped value)
  {
    return this.findAttributeFor(schema, value).flatMap(attributeDef -> {
      final var type = attributeDef.type();
      switch (type.basePrimitiveType()) {
        case TYPE_BOOLEAN:
          return this.checkAttributeTypeBoolean(type, value);
        case TYPE_INTEGER:
          return this.checkAttributeTypeInteger(type, value);
        case TYPE_REAL:
          return this.checkAttributeTypeReal(type, value);
        case TYPE_STRING:
          return this.checkAttributeTypeString(type, value);
        case TYPE_TIMESTAMP:
          return this.checkAttributeTypeTimestamp(type, value);
        case TYPE_URI:
          return this.checkAttributeTypeURI(type, value);
        case TYPE_UUID:
          return this.checkAttributeTypeUUID(type, value);
      }
      throw new UnreachableCodeException();
    });
  }

  private Optional<AttributeValueUUID> checkAttributeTypeUUID(
    final TypeReferenceType type,
    final AttributeValueUntyped attribute)
  {
    Preconditions.checkPrecondition(
      type.basePrimitiveType(),
      type.basePrimitiveType() == TypePrimitive.TYPE_UUID,
      t -> "Type must be TYPE_UUID");

    try {
      final var value = UUID.fromString(attribute.value());
      return Optional.of(
        AttributeValueUUID.of(attribute.lexical(), attribute.name(), type, value));
    } catch (final Exception e) {
      this.errors.receive(this.errorValueTypeError(type, attribute, e));
      return Optional.empty();
    }
  }

  private Optional<AttributeValueURI> checkAttributeTypeURI(
    final TypeReferenceType type,
    final AttributeValueUntyped attribute)
  {
    Preconditions.checkPrecondition(
      type.basePrimitiveType(),
      type.basePrimitiveType() == TypePrimitive.TYPE_URI,
      t -> "Type must be TYPE_URI");

    try {
      final var value = new URI(attribute.value());
      return Optional.of(
        AttributeValueURI.of(attribute.lexical(), attribute.name(), type, value));
    } catch (final Exception e) {
      this.errors.receive(this.errorValueTypeError(type, attribute, e));
      return Optional.empty();
    }
  }

  private Optional<AttributeValueTimestamp> checkAttributeTypeTimestamp(
    final TypeReferenceType type,
    final AttributeValueUntyped attribute)
  {
    Preconditions.checkPrecondition(
      type.basePrimitiveType(),
      type.basePrimitiveType() == TypePrimitive.TYPE_TIMESTAMP,
      t -> "Type must be TYPE_TIMESTAMP");

    try {
      final var value = OffsetDateTime.parse(attribute.value());
      return Optional.of(
        AttributeValueTimestamp.of(attribute.lexical(), attribute.name(), type, value));
    } catch (final Exception e) {
      this.errors.receive(this.errorValueTypeError(type, attribute, e));
      return Optional.empty();
    }
  }

  private Optional<AttributeValueString> checkAttributeTypeString(
    final TypeReferenceType type,
    final AttributeValueUntyped attribute)
  {
    Preconditions.checkPrecondition(
      type.basePrimitiveType(),
      type.basePrimitiveType() == TypePrimitive.TYPE_STRING,
      t -> "Type must be TYPE_STRING");

    return Optional.of(
      AttributeValueString.of(attribute.lexical(), attribute.name(), type, attribute.value()));
  }

  private Optional<AttributeValueInteger> checkAttributeTypeInteger(
    final TypeReferenceType type,
    final AttributeValueUntyped attribute)
  {
    Preconditions.checkPrecondition(
      type.basePrimitiveType(),
      type.basePrimitiveType() == TypePrimitive.TYPE_INTEGER,
      t -> "Type must be TYPE_INTEGER");

    try {
      final var value = new BigInteger(attribute.value());
      return Optional.of(
        AttributeValueInteger.of(attribute.lexical(), attribute.name(), type, value));
    } catch (final Exception e) {
      this.errors.receive(this.errorValueTypeError(type, attribute, e));
      return Optional.empty();
    }
  }

  private Optional<AttributeValueReal> checkAttributeTypeReal(
    final TypeReferenceType type,
    final AttributeValueUntyped attribute)
  {
    Preconditions.checkPrecondition(
      type.basePrimitiveType(),
      type.basePrimitiveType() == TypePrimitive.TYPE_REAL,
      t -> "Type must be TYPE_REAL");

    try {
      final var value = Double.parseDouble(attribute.value());
      return Optional.of(
        AttributeValueReal.of(attribute.lexical(), attribute.name(), type, Double.valueOf(value)));
    } catch (final Exception e) {
      this.errors.receive(this.errorValueTypeError(type, attribute, e));
      return Optional.empty();
    }
  }

  private Optional<AttributeValueBoolean> checkAttributeTypeBoolean(
    final TypeReferenceType type,
    final AttributeValueUntyped attribute)
  {
    Preconditions.checkPrecondition(
      type.basePrimitiveType(),
      type.basePrimitiveType() == TypePrimitive.TYPE_BOOLEAN,
      t -> "Type must be TYPE_BOOLEAN");

    final var text = attribute.value();
    final var name = attribute.name();
    switch (text) {
      case "true":
        return Optional.of(
          AttributeValueBoolean.of(attribute.lexical(), name, type, Boolean.TRUE));
      case "false":
        return Optional.of(
          AttributeValueBoolean.of(attribute.lexical(), name, type, Boolean.FALSE));
      default: {
        this.errors.receive(this.errorValueTypeError(type, attribute, new Exception()));
        return Optional.empty();
      }
    }
  }

  private Optional<MetaSchemaAttribute> findAttributeFor(
    final MetaSchema schema,
    final AttributeValueUntyped value)
  {
    final var attrDefinitionOpt =
      Optional.ofNullable(schema.attributesByName().get(value.name().name()));

    if (attrDefinitionOpt.isEmpty()) {
      this.errors.receive(this.errorSchemaAttributeNotFound(value));
      return Optional.empty();
    }
    return attrDefinitionOpt;
  }

  private Optional<MetaSchema> findSchemaFor(
    final AttributeValueUntyped value)
  {
    final var schemas = this.request.schemas();

    final var schemaOpt =
      schemas.stream()
        .filter(schema -> Objects.equals(schema.identifier().name(), value.name().schema()))
        .findFirst();

    if (schemaOpt.isEmpty()) {
      this.errors.receive(this.errorSchemaNotFound(value));
      return Optional.empty();
    }

    return schemaOpt;
  }

  private MetaValidatorError errorCardinality(
    final MetaSchemaAttribute attribute,
    final int count)
  {
    final var errorAttributes = PresentableAttributes.of(
      PresentableAttributes.entry(
        this.messages.format("attribute"), attribute.name().show()),
      PresentableAttributes.entry(
        this.messages.format("cardinality"), attribute.cardinality().show()),
      PresentableAttributes.entry(
        this.messages.format("occurrences"), String.valueOf(count))
    );

    return MetaValidatorError.builder()
      .setMessage(this.messages.format("errorAttributeCardinalityError"))
      .setAttributes(errorAttributes)
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(ATTRIBUTE_CARDINALITY_ERROR)
      .build();
  }


  private MetaValidatorError errorValueTypeError(
    final TypeReferenceType type,
    final AttributeValueUntyped attribute,
    final Exception e)
  {
    final var errorAttributes = PresentableAttributes.of(
      PresentableAttributes.entry(this.messages.format("type"), type.show()),
      PresentableAttributes.entry(this.messages.format("attribute"), attribute.name().show()),
      PresentableAttributes.entry(this.messages.format("value"), attribute.value())
    );

    return MetaValidatorError.builder()
      .setMessage(this.messages.format("errorTypeError"))
      .setAttributes(errorAttributes)
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setException(e)
      .setErrorCode(TYPE_ERROR)
      .build();
  }

  private MetaValidatorError errorSchemaAttributeNotFound(
    final AttributeValueUntyped value)
  {
    final var attrName = value.name();
    final var errorAttributes = PresentableAttributes.of(
      PresentableAttributes.entry(this.messages.format("attribute"), attrName.show()),
      PresentableAttributes.entry(this.messages.format("schema"), attrName.schema().show())
    );

    return MetaValidatorError.builder()
      .setMessage(this.messages.format("errorSchemaAttributeNotFound"))
      .setAttributes(errorAttributes)
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(ATTRIBUTE_NOT_FOUND)
      .build();
  }

  private MetaValidatorError errorSchemaNotFound(
    final MetaSchemaIdentifier value)
  {
    final var errorAttributes = PresentableAttributes.of(
      PresentableAttributes.entry(this.messages.format("schema"), value.show())
    );

    return MetaValidatorError.builder()
      .setMessage(this.messages.format("errorSchemaNotFound"))
      .setAttributes(errorAttributes)
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SCHEMA_NOT_FOUND)
      .build();
  }

  private MetaValidatorError errorSchemaNotFound(
    final AttributeValueUntyped value)
  {
    final var attrName = value.name();
    final var errorAttributes = PresentableAttributes.of(
      PresentableAttributes.entry(this.messages.format("attribute"), attrName.show()),
      PresentableAttributes.entry(this.messages.format("schema"), attrName.schema().show())
    );

    return MetaValidatorError.builder()
      .setMessage(this.messages.format("errorSchemaNotFound"))
      .setAttributes(errorAttributes)
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SCHEMA_NOT_FOUND)
      .build();
  }

  private static final class CardinalityValidator
  {
    private final MetaSchemaAttribute attribute;
    private int count;

    CardinalityValidator(
      final MetaSchemaAttribute inAttribute)
    {
      this.attribute = Objects.requireNonNull(inAttribute, "attribute");
    }

    void addOccurence()
    {
      LOG.trace("added occurrence of {}", this.attribute.name().show());
      ++this.count;
    }
  }

  private static final class SafeErrorPublisher implements MetaValidatorErrorReceiverType
  {
    private final MetaValidatorErrorReceiverType errors;
    private boolean failed;

    SafeErrorPublisher(
      final MetaValidatorErrorReceiverType inErrors)
    {
      this.errors = Objects.requireNonNull(inErrors, "errors");
      this.failed = false;
    }

    boolean isFailed()
    {
      return this.failed;
    }

    @Override
    public void receive(
      final MetaValidatorError error)
    {
      try {
        if (error.severity() == ErrorSeverity.SEVERITY_ERROR) {
          this.failed = true;
        }
        this.errors.receive(error);
      } catch (final Exception e) {
        LOG.debug("ignored exception raised by consumer: ", e);
      }
    }
  }
}
