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
import com.io7m.ironpage.types.api.AttributeValueTypedType;
import com.io7m.ironpage.types.api.AttributeValueUntyped;
import com.io7m.ironpage.types.api.SchemaAttribute;
import com.io7m.ironpage.types.api.SchemaDeclaration;
import com.io7m.ironpage.types.api.SchemaIdentifier;
import com.io7m.ironpage.types.api.SchemaName;
import com.io7m.ironpage.validator.api.SchemaValidationError;
import com.io7m.ironpage.validator.api.SchemaValidationErrorReceiverType;
import com.io7m.ironpage.validator.api.SchemaValidationRequest;
import com.io7m.ironpage.validator.api.SchemaValidatorType;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.collection.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

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

  @Override
  public Seq<AttributeValueTypedType<?>> validate(
    final SchemaValidationRequest request,
    final SchemaValidationErrorReceiverType receiver)
  {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(receiver, "receiver");

    final var cardinality =
      new SchemaCardinalityCounter(request.attributes().size());
    final var typedValues =
      this.validateAttributes(request, receiver, cardinality);


    return Vector.of();
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

  private Optional<AttributeValueTypedType<?>> validateAttribute(
    final SchemaValidationRequest request,
    final SchemaValidationErrorReceiverType receiver,
    final AttributeValueUntyped attribute)
  {
    final var importsByName = request.importsByName();

    /*
     * Verify that the schema referred to by the attribute is actually imported.
     */

    final var attributeName = attribute.name();
    final var schemaName = attributeName.schema();
    if (!importsByName.containsKey(schemaName)) {
      this.safeErrorPublish(receiver, this.errorSchemaNotImported(attributeName, importsByName));
      return Optional.empty();
    }

    /*
     * Verify that the schema referred to by the attribute exists.
     */

    final var resolvedModules = request.resolvedModules();
    final var schemasByName = resolvedModules.schemasByName();
    if (!schemasByName.containsKey(schemaName)) {
      this.safeErrorPublish(receiver, this.errorSchemaNotFound(attributeName, schemasByName));
      return Optional.empty();
    }

    /*
     * Verify that the schema actually contains a definition for the attribute.
     */

    final var schemaDeclaration = schemasByName.get(schemaName).get();
    final var schemaAttributesByName = schemaDeclaration.attributesByName();
    final var attributeNameSimple = attributeName.name();
    if (!schemaAttributesByName.containsKey(attributeNameSimple)) {
      this.safeErrorPublish(
        receiver,
        this.errorSchemaAttributeNotFound(
          attributeName,
          schemaDeclaration.identifier(),
          schemaAttributesByName));
      return Optional.empty();
    }

    final var schemaAttribute = schemaAttributesByName.get(attributeNameSimple).get();


    return Optional.empty();
  }

  private void safeErrorPublish(
    final SchemaValidationErrorReceiverType receiver,
    final SchemaValidationError error)
  {
    try {
      receiver.receive(error);
    } catch (final Exception e) {
      LOG.error("ignored exception raised by error receiver: ", e);
    }
  }

  private SchemaValidationError errorSchemaNotImported(
    final AttributeNameQualified attributeName,
    final SortedMap<SchemaName, SchemaIdentifier> importsByName)
  {
    final var errorAttributes = new TreeMap<String, String>();
    errorAttributes.put(this.messages.localize("attribute"), attributeName.name().name());

    importsByName.forEach(
      (key, value) -> {
        final var name = MessageFormat.format(this.messages.localize("schemaImported"), key.name());
        errorAttributes.put(name, value.show());
      });

    return SchemaValidationError.builder()
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SchemaValidatorType.SCHEMA_NOT_IMPORTED)
      .setMessage(this.messages.localize("schemaNotImported"))
      .build();
  }

  private SchemaValidationError errorSchemaNotFound(
    final AttributeNameQualified attributeName,
    final Map<SchemaName, SchemaDeclaration> schemasByName)
  {
    final var errorAttributes = new TreeMap<String, String>();
    errorAttributes.put(this.messages.localize("attribute"), attributeName.name().name());

    schemasByName.forEach(
      (key, value) -> {
        final var name = MessageFormat.format(this.messages.localize("schemaAvailable"), key.name());
        errorAttributes.put(name, value.identifier().show());
      });

    return SchemaValidationError.builder()
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SchemaValidatorType.SCHEMA_NOT_FOUND)
      .setMessage(this.messages.localize("schemaNotFound"))
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
      .setSeverity(ErrorSeverity.SEVERITY_ERROR)
      .setErrorCode(SchemaValidatorType.SCHEMA_ATTRIBUTE_NOT_FOUND)
      .setMessage(this.messages.localize("schemaAttributeNotFound"))
      .build();
  }

}
