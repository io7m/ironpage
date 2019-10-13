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
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalType;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.io7m.ironpage.metadata.schema.types.api.TypePrimitive.TYPE_BOOLEAN;
import static com.io7m.ironpage.metadata.schema.types.api.TypePrimitive.TYPE_INTEGER;
import static com.io7m.ironpage.metadata.schema.types.api.TypePrimitive.TYPE_REAL;
import static com.io7m.ironpage.metadata.schema.types.api.TypePrimitive.TYPE_STRING;
import static com.io7m.ironpage.metadata.schema.types.api.TypePrimitive.TYPE_TIMESTAMP;
import static com.io7m.ironpage.metadata.schema.types.api.TypePrimitive.TYPE_URI;
import static com.io7m.ironpage.metadata.schema.types.api.TypePrimitive.TYPE_UUID;

/**
 * A typed attribute value.
 *
 * @param <T> The type of the attribute value
 */

public interface AttributeValueTypedType<T> extends AttributeValueType<T>, LexicalType<URI>
{
  private static void checkType(
    final TypePrimitive receivedType,
    final TypePrimitive expectedType)
  {
    if (receivedType != expectedType) {
      throw new IllegalArgumentException(
        String.format("Expected type %s but was given type %s", expectedType, receivedType));
    }
  }

  /**
   * @return The type against this value was checked
   */

  TypeReferenceType type();

  /**
   * An integer value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueIntegerType extends AttributeValueTypedType<BigInteger>
  {
    @Override
    @Value.Parameter
    @Value.Auxiliary
    LexicalPosition<URI> lexical();

    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    TypeReferenceType type();

    @Override
    @Value.Parameter
    BigInteger value();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      checkType(this.type().basePrimitiveType(), TYPE_INTEGER);
    }
  }

  /**
   * A boolean value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueBooleanType extends AttributeValueTypedType<Boolean>
  {
    @Override
    @Value.Parameter
    @Value.Auxiliary
    LexicalPosition<URI> lexical();

    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    TypeReferenceType type();

    @Override
    @Value.Parameter
    Boolean value();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      checkType(this.type().basePrimitiveType(), TYPE_BOOLEAN);
    }
  }

  /**
   * A string value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueStringType extends AttributeValueTypedType<String>
  {
    @Override
    @Value.Parameter
    @Value.Auxiliary
    LexicalPosition<URI> lexical();

    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    TypeReferenceType type();

    @Override
    @Value.Parameter
    String value();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      checkType(this.type().basePrimitiveType(), TYPE_STRING);
    }
  }

  /**
   * A real value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueRealType extends AttributeValueTypedType<Double>
  {
    @Override
    @Value.Parameter
    @Value.Auxiliary
    LexicalPosition<URI> lexical();

    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    TypeReferenceType type();

    @Override
    @Value.Parameter
    Double value();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      checkType(this.type().basePrimitiveType(), TYPE_REAL);
    }
  }

  /**
   * A timestamp value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueTimestampType extends AttributeValueTypedType<OffsetDateTime>
  {
    @Override
    @Value.Parameter
    @Value.Auxiliary
    LexicalPosition<URI> lexical();

    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    TypeReferenceType type();

    @Override
    @Value.Parameter
    OffsetDateTime value();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      checkType(this.type().basePrimitiveType(), TYPE_TIMESTAMP);
    }
  }

  /**
   * A URI value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueURIType extends AttributeValueTypedType<URI>
  {
    @Override
    @Value.Parameter
    @Value.Auxiliary
    LexicalPosition<URI> lexical();

    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    TypeReferenceType type();

    @Override
    @Value.Parameter
    URI value();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      checkType(this.type().basePrimitiveType(), TYPE_URI);
    }
  }

  /**
   * A UUID value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueUUIDType extends AttributeValueTypedType<UUID>
  {
    @Override
    @Value.Parameter
    @Value.Auxiliary
    LexicalPosition<URI> lexical();

    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    TypeReferenceType type();

    @Override
    @Value.Parameter
    UUID value();

    /**
     * Check preconditions for the type.
     */

    @Value.Check
    default void checkPreconditions()
    {
      checkType(this.type().basePrimitiveType(), TYPE_UUID);
    }
  }
}
