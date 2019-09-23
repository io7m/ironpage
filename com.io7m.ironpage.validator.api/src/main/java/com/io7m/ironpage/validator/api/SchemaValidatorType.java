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

package com.io7m.ironpage.validator.api;

import com.io7m.ironpage.types.api.AttributeValueTypedType;
import io.vavr.collection.Seq;

/**
 * The interface provided by schema validators.
 */

public interface SchemaValidatorType
{
  /**
   * An attribute refers to a schema that is not imported.
   */

  SchemaValidationErrorCode SCHEMA_NOT_IMPORTED =
    SchemaValidationErrorCode.of(
      new StringBuilder(64)
        .append(SchemaValidatorType.class.getCanonicalName())
        .append(":schemaNotImported")
        .toString());

  /**
   * An attribute refers to a schema that doesn't exist.
   */

  SchemaValidationErrorCode SCHEMA_NOT_FOUND =
    SchemaValidationErrorCode.of(
      new StringBuilder(64)
        .append(SchemaValidatorType.class.getCanonicalName())
        .append(":schemaNotFound")
        .toString());

  /**
   * An attribute is referenced that does not exist in a schema.
   */

  SchemaValidationErrorCode SCHEMA_ATTRIBUTE_NOT_FOUND =
    SchemaValidationErrorCode.of(
      new StringBuilder(64)
        .append(SchemaValidatorType.class.getCanonicalName())
        .append(":schemaAttributeNotFound")
        .toString());

  /**
   * Validate the given set of attributes.
   *
   * @param request  The requested set of attributes
   * @param receiver A validation error receiver
   *
   * @return The result of resolving attributes
   */

  Seq<AttributeValueTypedType<?>> validate(
    SchemaValidationRequest request,
    SchemaValidationErrorReceiverType receiver);
}
