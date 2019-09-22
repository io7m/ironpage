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

package com.io7m.ironpage.types.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A typed attribute value.
 *
 * @param <T> The type of the attribute value
 */

public interface AttributeValueTypedType<T> extends AttributeValueType<T>
{
  /**
   * @return The base type
   */

  default AttributeTypeBase baseType()
  {
    return this.type().baseType();
  }

  /**
   * @return The fully-qualified attribute type name
   */

  AttributeTypeNameQualified typeName();

  /**
   * @return The type of the attribute
   */

  AttributeTypeAnonymousType type();

  /**
   * An integer value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueIntegerType extends AttributeValueTypedType<BigInteger>
  {
    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    AttributeTypeNameQualified typeName();

    @Override
    @Value.Parameter
    AttributeTypeInteger type();

    @Override
    @Value.Parameter
    BigInteger value();
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
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    AttributeTypeNameQualified typeName();

    @Override
    @Value.Parameter
    AttributeTypeBoolean type();

    @Override
    @Value.Parameter
    Boolean value();
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
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    AttributeTypeNameQualified typeName();

    @Override
    @Value.Parameter
    AttributeTypeString type();

    @Override
    @Value.Parameter
    String value();
  }

  /**
   * A real value.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeValueRealType extends AttributeValueTypedType<BigDecimal>
  {
    @Override
    @Value.Parameter
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    AttributeTypeNameQualified typeName();

    @Override
    @Value.Parameter
    AttributeTypeReal type();

    @Override
    @Value.Parameter
    BigDecimal value();
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
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    AttributeTypeNameQualified typeName();

    @Override
    @Value.Parameter
    AttributeTypeTimestamp type();

    @Override
    @Value.Parameter
    OffsetDateTime value();
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
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    AttributeTypeNameQualified typeName();

    @Override
    @Value.Parameter
    AttributeTypeURI type();

    @Override
    @Value.Parameter
    URI value();
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
    AttributeNameQualified name();

    @Override
    @Value.Parameter
    AttributeTypeNameQualified typeName();

    @Override
    @Value.Parameter
    AttributeTypeUUID type();

    @Override
    @Value.Parameter
    UUID value();
  }
}
