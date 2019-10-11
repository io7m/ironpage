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

import com.io7m.junreachable.UnreachableCodeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

public final class UnreachableTest
{
  private static final Logger LOG = LoggerFactory.getLogger(UnreachableTest.class);

  @TestFactory
  public Stream<DynamicTest> testConstructors()
  {
    return Stream.of(
      "com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.v1_0.MSX1Constants")
      .map(UnreachableTest::loadClass)
      .map(UnreachableTest::runClass);
  }

  private static DynamicTest runClass(
    final Class<?> clazz)
  {
    return DynamicTest.dynamicTest(
      "testUnreachable: " + clazz.getCanonicalName(),
      () -> {
        try {
          final var cons = clazz.getDeclaredConstructor();
          cons.setAccessible(true);
          cons.newInstance();
        } catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InstantiationException | SecurityException e) {
          Assertions.fail(e);
        } catch (final InvocationTargetException e) {
          if (e.getCause() instanceof UnreachableCodeException) {
            LOG.debug("correctly unreachable: {}", clazz.getCanonicalName());
          } else {
            Assertions.fail(e);
          }
        }
      });
  }

  private static Class<?> loadClass(final String name)
  {
    try {
      return Class.forName(name);
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
