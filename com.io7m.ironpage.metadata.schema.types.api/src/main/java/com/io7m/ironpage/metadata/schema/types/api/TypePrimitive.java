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

/**
 * The set of primitive types.
 */

public enum TypePrimitive
{
  /**
   * A boolean type.
   */

  TYPE_BOOLEAN,

  /**
   * An arbitrary size integer.
   */

  TYPE_INTEGER,

  /**
   * An arbitrary precision real value.
   */

  TYPE_REAL,

  /**
   * A string value.
   */

  TYPE_STRING,

  /**
   * A timestamp value.
   */

  TYPE_TIMESTAMP,

  /**
   * A URI value.
   */

  TYPE_URI,

  /**
   * A UUID value.
   */

  TYPE_UUID,
}
