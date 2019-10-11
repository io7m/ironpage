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

package com.io7m.ironpage.metadata.schema.compiler.binder.api;

import com.io7m.ironpage.metadata.schema.ast.MADeclSchema;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorCode;

import java.util.Optional;

/**
 * A binding analyzer.
 *
 * @see MSchemaCompilerBinderProviderType
 */

public interface MSchemaCompilerBinderType extends AutoCloseable
{
  /**
   * The name of a schema is invalid.
   */

  MSchemaCompilerErrorCode SCHEMA_NAME_INVALID =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerBinderType.class.getCanonicalName() + ":schemaNameInvalid")
      .build();

  /**
   * A required schema does not exist.
   */

  MSchemaCompilerErrorCode SCHEMA_NONEXISTENT =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerBinderType.class.getCanonicalName() + ":schemaNonexistent")
      .build();

  /**
   * The name of a type is invalid.
   */

  MSchemaCompilerErrorCode TYPE_NAME_INVALID =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerBinderType.class.getCanonicalName() + ":typeNameInvalid")
      .build();

  /**
   * A required type does not exist.
   */

  MSchemaCompilerErrorCode TYPE_NONEXISTENT =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerBinderType.class.getCanonicalName() + ":typeNonexistent")
      .build();

  /**
   * The name of an attribute is invalid.
   */

  MSchemaCompilerErrorCode ATTRIBUTE_NAME_INVALID =
    MSchemaCompilerErrorCode.builder()
      .setCode(MSchemaCompilerBinderType.class.getCanonicalName() + ":attributeNameInvalid")
      .build();

  /**
   * Execute the analyzer.
   *
   * @return An analyzed schema, if no errors occurred
   */

  Optional<MADeclSchema<MSchemaBoundType>> execute();
}
