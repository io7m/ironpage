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

import com.io7m.ironpage.metadata.schema.types.api.AttributeCardinality;
import com.io7m.ironpage.metadata.schema.types.api.AttributeName;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaAttribute;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaName;
import com.io7m.ironpage.metadata.schema.types.api.TypeName;
import com.io7m.ironpage.metadata.schema.types.api.TypeNamed;
import com.io7m.ironpage.metadata.schema.types.api.TypePrimitive;
import com.io7m.ironpage.metadata.schema.types.api.TypeQualifiedNamed;
import com.io7m.ironpage.metadata.schema.types.api.TypeReferencePrimitive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

@Tag("schemaCompiler")
public final class MetaSchemaTest
{
  /**
   * Cannot construct a schema with duplicate types.
   */

  @Test
  public void testDuplicateTypes()
  {
    final var type =
      TypeNamed.of(
        TypeName.of("x"),
        TypeReferencePrimitive.of(TypePrimitive.TYPE_BOOLEAN));

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      MetaSchema.builder()
        .setIdentifier(MetaSchemaIdentifier.of(MetaSchemaName.of("example"), ONE, ZERO))
        .setTypes(List.of(type, type))
        .build();
    });
  }

  /**
   * Cannot construct a schema with duplicate imports.
   */

  @Test
  public void testDuplicateImports()
  {
    final var importV =
      MetaSchemaIdentifier.of(MetaSchemaName.of("z"), ONE, ZERO);

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      MetaSchema.builder()
        .setIdentifier(MetaSchemaIdentifier.of(MetaSchemaName.of("example"), ONE, ZERO))
        .setImports(List.of(importV, importV))
        .build();
    });
  }

  /**
   * Cannot construct a schema with duplicate attributes.
   */

  @Test
  public void testDuplicateAttributes()
  {
    final var type =
      TypeQualifiedNamed.of(
        MetaSchemaName.of("z"),
        TypeNamed.of(TypeName.of("y"), TypeReferencePrimitive.of(TypePrimitive.TYPE_INTEGER))
      );

    final var attr =
      MetaSchemaAttribute.of(
        AttributeName.of("x"),
        type,
        AttributeCardinality.CARDINALITY_1);

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      MetaSchema.builder()
        .setIdentifier(MetaSchemaIdentifier.of(MetaSchemaName.of("example"), ONE, ZERO))
        .setAttributes(List.of(attr, attr))
        .build();
    });
  }
}
