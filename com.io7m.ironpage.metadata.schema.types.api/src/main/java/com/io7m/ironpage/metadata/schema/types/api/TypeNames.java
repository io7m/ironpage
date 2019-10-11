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

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Functions that define valid names.
 */

public final class TypeNames
{
  /**
   * The pattern that defines a valid type name.
   */

  public static final Pattern VALID_TYPE_NAME_PATTERN =
    Pattern.compile("[a-z][a-z0-9_]{0,127}");

  private TypeNames()
  {

  }

  /**
   * @param name The name
   *
   * @return {@code true} if {@code name} is a valid type name
   */

  public static boolean isValidName(final String name)
  {
    Objects.requireNonNull(name, "name");
    return VALID_TYPE_NAME_PATTERN.matcher(name).matches();
  }

  /**
   * @param name The name
   *
   * @return {@code name} if {@code name} is a valid type name
   */

  public static String checkValidName(final String name)
  {
    if (!isValidName(name)) {
      throw new IllegalArgumentException(
        new StringBuilder(64)
          .append("Not a valid type name: ")
          .append(name)
          .append(" (must match ")
          .append(VALID_TYPE_NAME_PATTERN.pattern())
          .append(")")
          .toString());
    }
    return name;
  }
}