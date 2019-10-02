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

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Valid schema names.
 */

public final class SchemaNames
{
  /**
   * The pattern that defines a valid schema name part.
   */

  private static final Pattern VALID_SCHEMA_NAME_PART_PATTERN =
    Pattern.compile("[a-z][a-z0-9_]*");

  private SchemaNames()
  {

  }

  /**
   * @param name The name
   *
   * @return {@code true} if {@code name} is a valid schema name
   */

  public static boolean isValidName(final String name)
  {
    Objects.requireNonNull(name, "name");

    if (name.length() > 128) {
      return false;
    }

    final var parts = List.of(name.split("\\."));
    if (parts.isEmpty()) {
      return false;
    }

    return parts.stream().allMatch(part -> VALID_SCHEMA_NAME_PART_PATTERN.matcher(part).matches());
  }

  /**
   * @param name The name
   *
   * @return {@code name} if {@code name} is a valid schema name
   */

  public static String checkValidName(final String name)
  {
    if (!isValidName(name)) {
      throw new IllegalArgumentException(
        new StringBuilder(64)
          .append("Not a valid schema name: ")
          .append(name)
          .append(" (Must be a dot-separated sequence of ")
          .append(VALID_SCHEMA_NAME_PART_PATTERN.pattern())
          .append(")")
          .toString());
    }
    return name;
  }
}
