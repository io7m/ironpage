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
  String show();

  Kind kind();

  TypePrimitive basePrimitiveType();

  enum Kind
  {
    TYPE_REFERENCE_NAMED,
    TYPE_REFERENCE_PRIMITIVE
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface TypeReferenceNamedType extends TypeReferenceType
  {
    @Override
    default Kind kind()
    {
      return Kind.TYPE_REFERENCE_NAMED;
    }

    @Value.Parameter
    MetaSchemaIdentifier schema();

    @Value.Parameter
    TypeName name();

    @Value.Parameter
    TypePrimitive basePrimitiveType();

    @Override
    default String show()
    {
      return String.format("%s:%s", this.schema().name().show(), this.name().show());
    }
  }

  @ImmutablesStyleType
  @Value.Immutable
  interface TypeReferencePrimitiveType extends TypeReferenceType
  {
    @Override
    default Kind kind()
    {
      return Kind.TYPE_REFERENCE_PRIMITIVE;
    }

    @Value.Parameter
    TypePrimitive basePrimitiveType();

    @Override
    default String show()
    {
      return this.basePrimitiveType().name();
    }
  }
}
