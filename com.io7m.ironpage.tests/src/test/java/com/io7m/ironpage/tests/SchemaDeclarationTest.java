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
import com.io7m.ironpage.types.api.AttributeTypeBoolean;
import com.io7m.ironpage.types.api.AttributeTypeName;
import com.io7m.ironpage.types.api.AttributeTypeNameQualified;
import com.io7m.ironpage.types.api.AttributeTypeNamed;
import com.io7m.ironpage.types.api.SchemaAttribute;
import com.io7m.ironpage.types.api.SchemaDeclaration;
import com.io7m.ironpage.types.api.SchemaIdentifier;
import com.io7m.ironpage.types.api.SchemaName;
import io.vavr.collection.Vector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

public final class SchemaDeclarationTest
{
  /**
   * Cannot construct a schema with duplicate types.
   */

  @Test
  public void testDuplicateTypes()
  {
    final var type =
      AttributeTypeNamed.of(
        AttributeTypeName.of("x"),
        AttributeTypeBoolean.builder().build());

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      SchemaDeclaration.builder()
        .setIdentifier(SchemaIdentifier.of(SchemaName.of("example"), ONE, ZERO))
        .setTypes(Vector.of(type, type))
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
      SchemaIdentifier.of(SchemaName.of("z"), ONE, ZERO);

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      SchemaDeclaration.builder()
        .setIdentifier(SchemaIdentifier.of(SchemaName.of("example"), ONE, ZERO))
        .setImports(Vector.of(importV, importV))
        .build();
    });
  }

  /**
   * Cannot construct a schema with duplicate attributes.
   */

  @Test
  public void testDuplicateAttributes()
  {
    final var attr =
      SchemaAttribute.of(
        AttributeName.of("x"),
        AttributeTypeNameQualified.builder()
          .setSchema(SchemaName.of("z"))
          .setType(AttributeTypeName.of("y"))
          .build(),
        AttributeCardinality.CARDINALITY_1);

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      SchemaDeclaration.builder()
        .setIdentifier(SchemaIdentifier.of(SchemaName.of("example"), ONE, ZERO))
        .setAttributes(Vector.of(attr, attr))
        .build();
    });
  }
}
