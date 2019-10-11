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

import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaName;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaNames;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

@Tag("equals")
public final class MetaSchemaNamesTest
{
  @Property
  public void testOrdering(
    @ForAll("probablyValid") final String text0,
    @ForAll("probablyValid") final String text1)
  {
    Assertions.assertEquals(
      text0.compareTo(text1),
      MetaSchemaName.of(text0).compareTo(MetaSchemaName.of(text1)));
  }

  @Provide
  Arbitrary<String> probablyValid()
  {
    return Arbitraries.strings()
      .withCharRange('a', 'z')
      .ofMinLength(1)
      .ofMaxLength(127);
  }

  @Test
  public void testEquals()
  {
    EqualsVerifier.forClass(MetaSchemaName.class)
      .withNonnullFields("name")
      .verify();
  }

  @TestFactory
  public Stream<DynamicTest> testValid()
  {
    return Stream.of("a", "a0", "a_0", "com.example.x")
      .map(text -> {
        return DynamicTest.dynamicTest(
          "testValid: " + text,
          () -> {
            Assertions.assertTrue(MetaSchemaNames.isValidName(text));
            MetaSchemaNames.checkValidName(text);
          });
      });
  }

  @TestFactory
  public Stream<DynamicTest> testInvalid()
  {
    return Stream.of(
      "",
      "0",
      "A",
      "1.2",
      "-",
      "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
      .map(text -> {
        return DynamicTest.dynamicTest(
          "testInvalid: " + text,
          () -> {
            Assertions.assertFalse(MetaSchemaNames.isValidName(text));
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
              MetaSchemaNames.checkValidName(text);
            });
          });
      });
  }
}
