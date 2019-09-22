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

/**
 * The type of anonymous types.
 */

public interface AttributeTypeAnonymousType
{
  /**
   * @return The base type
   */

  AttributeTypeBase baseType();

  /**
   * The type of plain integers.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeTypeIntegerType extends AttributeTypeAnonymousType
  {
    @Override
    default AttributeTypeBase baseType()
    {
      return AttributeTypeBase.ATTRIBUTE_TYPE_INTEGER;
    }
  }

  /**
   * The type of plain booleans.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeTypeBooleanType extends AttributeTypeAnonymousType
  {
    @Override
    default AttributeTypeBase baseType()
    {
      return AttributeTypeBase.ATTRIBUTE_TYPE_BOOLEAN;
    }
  }

  /**
   * The type of plain real values.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeTypeRealType extends AttributeTypeAnonymousType
  {
    @Override
    default AttributeTypeBase baseType()
    {
      return AttributeTypeBase.ATTRIBUTE_TYPE_REAL;
    }
  }

  /**
   * The type of plain string values.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeTypeStringType extends AttributeTypeAnonymousType
  {
    @Override
    default AttributeTypeBase baseType()
    {
      return AttributeTypeBase.ATTRIBUTE_TYPE_STRING;
    }
  }

  /**
   * The type of plain timestamp values.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeTypeTimestampType extends AttributeTypeAnonymousType
  {
    @Override
    default AttributeTypeBase baseType()
    {
      return AttributeTypeBase.ATTRIBUTE_TYPE_TIMESTAMP;
    }
  }

  /**
   * The type of plain URI values.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeTypeURIType extends AttributeTypeAnonymousType
  {
    @Override
    default AttributeTypeBase baseType()
    {
      return AttributeTypeBase.ATTRIBUTE_TYPE_URI;
    }
  }

  /**
   * The type of plain UUID values.
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface AttributeTypeUUIDType extends AttributeTypeAnonymousType
  {
    @Override
    default AttributeTypeBase baseType()
    {
      return AttributeTypeBase.ATTRIBUTE_TYPE_UUID;
    }
  }
}
