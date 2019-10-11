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

package com.io7m.ironpage.metadata.schema.compiler.binder.api;

import com.io7m.ironpage.metadata.schema.types.api.NameType;

/**
 * Data indicating that a schema element has gone through binding analysis.
 */

public interface MSchemaBoundType
{
  /**
   * Retrieve the specific name associated with this element.
   *
   * @param nameType The type of name
   * @param <T>      The precise type of name
   *
   * @return The name
   *
   * @throws IllegalArgumentException If no name is associated with this element, or the name is not
   *                                  of the specified type
   */

  <T extends NameType> T name(Class<T> nameType)
    throws IllegalArgumentException;
}
