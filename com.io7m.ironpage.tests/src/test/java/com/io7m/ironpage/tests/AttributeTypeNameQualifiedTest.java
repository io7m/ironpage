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

package com.io7m.ironpage.tests;

import com.io7m.ironpage.types.api.AttributeTypeName;
import com.io7m.ironpage.types.api.AttributeTypeNameQualified;
import com.io7m.ironpage.types.api.SchemaName;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Assertions;

public final class AttributeTypeNameQualifiedTest
{
  @Property
  public void testShow(
    @ForAll("probablyValid") final String text0,
    @ForAll("probablyValid") final String text1)
  {
    final var show =
      AttributeTypeNameQualified.of(
        SchemaName.of(text0),
        AttributeTypeName.of(text1)).show();

    Assertions.assertTrue(show.contains(text0));
    Assertions.assertTrue(show.contains(text1));
  }

  @Provide
  Arbitrary<String> probablyValid()
  {
    return Arbitraries.strings()
      .withCharRange('a', 'z')
      .ofMinLength(1)
      .ofMaxLength(127);
  }
}
