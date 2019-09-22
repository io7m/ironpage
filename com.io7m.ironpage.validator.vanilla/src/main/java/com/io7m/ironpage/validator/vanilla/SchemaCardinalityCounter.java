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

package com.io7m.ironpage.validator.vanilla;

import com.io7m.ironpage.types.api.AttributeNameQualified;

import java.math.BigInteger;
import java.util.HashMap;

final class SchemaCardinalityCounter
{
  private final HashMap<AttributeNameQualified, BigInteger> entries;

  SchemaCardinalityCounter(final int size)
  {
    this.entries = new HashMap<>(size);
  }

  BigInteger add(
    final AttributeNameQualified name)
  {
    return this.entries.compute(name, (ignored, count) -> {
      if (count == null) {
        return BigInteger.ONE;
      }
      return count.add(BigInteger.ONE);
    });
  }

  BigInteger count(
    final AttributeNameQualified name)
  {
    return this.entries.compute(name, (ignored, count) -> {
      if (count == null) {
        return BigInteger.ZERO;
      }
      return count;
    });
  }
}
