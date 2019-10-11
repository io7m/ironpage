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


package com.io7m.ironpage.presentable.api;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Convenient methods to construct immutable presentable attributes.
 */

public final class PresentableAttributes
{
  private PresentableAttributes()
  {

  }

  /**
   * Create attributes from the given list of entries.
   *
   * @param entries The entries
   *
   * @return A read-only map of attributes
   */

  @SafeVarargs
  public static SortedMap<String, String> of(
    final SortedMap.Entry<String, String>... entries)
  {
    Objects.requireNonNull(entries, "entries");
    final var map = new TreeMap<String, String>();
    for (final var entry : entries) {
      map.put(entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableSortedMap(map);
  }

  /**
   * Construct a map of attributes with a single key/value pair.
   *
   * @param key   The key
   * @param value The value
   *
   * @return A read-only map of attributes
   */

  public static SortedMap<String, String> one(
    final String key,
    final String value)
  {
    return of(entry(key, value));
  }

  /**
   * Construct a map entry.
   *
   * @param key   The key
   * @param value The value
   *
   * @return A map entry
   */

  public static SortedMap.Entry<String, String> entry(
    final String key,
    final String value)
  {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    return new AbstractMap.SimpleEntry<>(key, value);
  }
}
