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

import java.math.BigInteger;

/**
 * The type of schema identifiers.
 */

@Value.Immutable
@ImmutablesStyleType
public interface SchemaIdentifierType
{
  /**
   * @return The schema name
   */

  @Value.Parameter
  SchemaName name();

  /**
   * @return The schema major version
   */

  @Value.Parameter
  BigInteger versionMajor();

  /**
   * @return The schema minor version
   */

  @Value.Parameter
  BigInteger versionMinor();

  /**
   * @return The schema identifier as a humanly readable string
   */

  default String show()
  {
    return new StringBuilder(64)
      .append(this.name().name())
      .append(":")
      .append(this.versionMajor())
      .append(":")
      .append(this.versionMinor())
      .toString();
  }
}
