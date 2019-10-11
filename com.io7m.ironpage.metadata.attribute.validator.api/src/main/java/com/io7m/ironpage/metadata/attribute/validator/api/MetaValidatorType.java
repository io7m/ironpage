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

package com.io7m.ironpage.metadata.attribute.validator.api;

import com.io7m.ironpage.metadata.schema.types.api.MetaDocumentTyped;

import java.util.Optional;

/**
 * A metadata validator.
 */

public interface MetaValidatorType
{
  /**
   * An attribute refers to a schema that doesn't exist.
   */

  MetaValidatorErrorCode SCHEMA_NOT_FOUND =
    MetaValidatorErrorCode.of(
      new StringBuilder(64)
        .append(MetaValidatorType.class.getCanonicalName())
        .append(":schemaNotFound")
        .toString());

  /**
   * An attribute does not exist within a schema.
   */

  MetaValidatorErrorCode ATTRIBUTE_NOT_FOUND =
    MetaValidatorErrorCode.of(
      new StringBuilder(64)
        .append(MetaValidatorType.class.getCanonicalName())
        .append(":attributeNotFound")
        .toString());

  /**
   * The number of occurrences of a value does not match the cardinality declared in the schema.
   */

  MetaValidatorErrorCode ATTRIBUTE_CARDINALITY_ERROR =
    MetaValidatorErrorCode.of(
      new StringBuilder(64)
        .append(MetaValidatorType.class.getCanonicalName())
        .append(":attributeCardinalityError")
        .toString());

  /**
   * A type does not exist within a schema.
   */

  MetaValidatorErrorCode TYPE_NOT_FOUND =
    MetaValidatorErrorCode.of(
      new StringBuilder(64)
        .append(MetaValidatorType.class.getCanonicalName())
        .append(":typeNotFound")
        .toString());

  /**
   * A value is not a member of the target type.
   */

  MetaValidatorErrorCode TYPE_ERROR =
    MetaValidatorErrorCode.of(
      new StringBuilder(64)
        .append(MetaValidatorType.class.getCanonicalName())
        .append(":typeError")
        .toString());

  /**
   * Execute the validator, producing a typed document if validation succeeds.
   *
   * @return The validated document, if no errors occurred
   */

  Optional<MetaDocumentTyped> execute();
}
