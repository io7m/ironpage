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

package com.io7m.ironpage.tests.arbitraries;

import com.io7m.ironpage.database.api.DatabaseParameters;
import com.io7m.ironpage.database.audit.api.AuditDatabaseEventDTO;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Providers of various values.
 */

public final class IronArbitraries implements ArbitraryProvider
{
  /**
   * Construct a provider.
   */

  public IronArbitraries()
  {

  }

  private static final Map<Class<?>, Supplier<Arbitrary<?>>> ARBITRARIES =
    Map.of(
      AuditDatabaseEventDTO.class, IronArbitraries::auditDatabaseEvents,
      DatabaseParameters.class, IronArbitraries::databaseParameters,
      Path.class, IronArbitraries::paths,
      Instant.class, IronArbitraries::instants
    );

  /**
   * @return A generator of {@link Instant} values
   */

  public static Arbitrary<Instant> instants()
  {
    return Arbitraries.longs().between(0L, 0x7f000000L).map(Instant::ofEpochSecond);
  }

  /**
   * @return A generator of {@link Path} values
   */

  public static Arbitrary<Path> paths()
  {
    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16);

    return strings.list()
      .ofMinSize(1)
      .ofMaxSize(8)
      .map(components -> {
        final var array = new String[components.size()];
        components.toArray(array);
        return Paths.get("", array);
      });
  }

  /**
   * @return A generator of {@link DatabaseParameters} values
   */

  public static Arbitrary<DatabaseParameters> databaseParameters()
  {
    return Arbitraries.defaultFor(Boolean.class)
      .flatMap(create0 -> Arbitraries.defaultFor(Boolean.class)
        .flatMap(create1 -> paths()
          .flatMap(path0 -> paths()
            .flatMap(path1 -> {

              final var instance0 =
                DatabaseParameters.builder()
                  .setPath(path0.toString())
                  .setCreate(create0.booleanValue())
                  .build();

              final var instance1 =
                DatabaseParameters.builder()
                  .from(instance0)
                  .build();

              final var instance2 =
                instance1
                  .withCreate(create1.booleanValue())
                  .withPath(path1.toString());

              return Arbitraries.constant(DatabaseParameters.copyOf(instance2));
            }))));
  }

  /**
   * @return A generator of {@link AuditDatabaseEventDTO} values
   */

  public static Arbitrary<AuditDatabaseEventDTO> auditDatabaseEvents()
  {
    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    final var times =
      Arbitraries.defaultFor(Instant.class)
        .list()
        .ofSize(4);

    return times.flatMap(timeValues -> {
      return strings.map(texts -> {
        final var instance0 =
          AuditDatabaseEventDTO.builder()
            .setTime(timeValues.get(0))
            .setEventType(texts.get(0))
            .setArgument0(texts.get(1))
            .setArgument1(texts.get(2))
            .setArgument2(texts.get(3))
            .setArgument3(texts.get(4))
            .build();

        final var instance1 =
          instance0.withTime(timeValues.get(1))
            .withArgument0(texts.get(5))
            .withArgument1(texts.get(6))
            .withArgument2(texts.get(7))
            .withArgument3(texts.get(8))
            .withEventType(texts.get(9));

        final var instance2 =
          AuditDatabaseEventDTO.builder().from(instance1).build();

        return AuditDatabaseEventDTO.copyOf(instance2);
      });
    });
  }

  @Override
  public boolean canProvideFor(final TypeUsage targetType)
  {
    return ARBITRARIES.keySet()
      .stream()
      .anyMatch(targetType::isOfType);
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Collections.singleton(ARBITRARIES.get(targetType.getRawType()).get());
  }
}
