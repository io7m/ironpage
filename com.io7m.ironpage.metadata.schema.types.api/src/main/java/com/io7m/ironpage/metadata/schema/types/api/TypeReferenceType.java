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


package com.io7m.ironpage.metadata.schema.types.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

/**
 * A reference to a type.
 */

public interface TypeReferenceType
{
  /**
   * @return A humanly-readable representation of the type
   */

  String show();

  /**
   * @return The kind of type reference
   */

  Kind kind();

  /**
   * @return The evaluated base primitive type
   */

  TypePrimitive basePrimitiveType();

  /**
   * The kind of type references.
   */

  enum Kind
  {
    /**
     * The reference refers to a named type.
     */

    TYPE_REFERENCE_NAMED,

    /**
     * The reference refers to a primitive type.
     */

    TYPE_REFERENCE_PRIMITIVE
  }

  /**
   * The type of references to named types.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface TypeReferenceNamedType extends TypeReferenceType
  {
    @Override
    default Kind kind()
    {
      return Kind.TYPE_REFERENCE_NAMED;
    }

    /**
     * @return The schema of the referenced type
     */

    @Value.Parameter
    MetaSchemaIdentifier schema();

    /**
     * @return The name of the referenced type
     */

    @Value.Parameter
    TypeName name();

    @Override
    @Value.Parameter
    TypePrimitive basePrimitiveType();

    @Override
    default String show()
    {
      return String.format("%s:%s", this.schema().name().show(), this.name().show());
    }
  }

  /**
   * The type of references to primitive types.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface TypeReferencePrimitiveType extends TypeReferenceType
  {
    @Override
    default Kind kind()
    {
      return Kind.TYPE_REFERENCE_PRIMITIVE;
    }

    @Override
    @Value.Parameter
    TypePrimitive basePrimitiveType();

    @Override
    default String show()
    {
      return this.basePrimitiveType().name();
    }
  }
}
