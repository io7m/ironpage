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
import com.io7m.ironpage.database.core.api.CDErrorCode;
import com.io7m.ironpage.database.core.api.CDPasswordHashDTO;
import com.io7m.ironpage.database.core.api.CDSecurityLabelDTO;
import com.io7m.ironpage.database.core.api.CDSecurityRoleDTO;
import com.io7m.ironpage.database.core.api.CDSessionDTO;
import com.io7m.ironpage.database.core.api.CDUserDTO;
import com.io7m.ironpage.database.pages.api.PagesDatabaseBlobDTO;
import com.io7m.ironpage.database.pages.api.PagesDatabaseRedactionDTO;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Providers of various values.
 */

public final class IronArbitraries implements ArbitraryProvider
{
  private static final Map<Class<?>, Supplier<Arbitrary<?>>> ARBITRARIES =
    Map.ofEntries(
      Map.entry(AuditDatabaseEventDTO.class, IronArbitraries::auditDatabaseEvents),
      Map.entry(CDErrorCode.class, IronArbitraries::errorCodes),
      Map.entry(CDPasswordHashDTO.class, IronArbitraries::passwordHashes),
      Map.entry(CDSecurityLabelDTO.class, IronArbitraries::labels),
      Map.entry(CDSecurityRoleDTO.class, IronArbitraries::roles),
      Map.entry(CDSessionDTO.class, IronArbitraries::sessions),
      Map.entry(CDUserDTO.class, IronArbitraries::users),
      Map.entry(DatabaseParameters.class, IronArbitraries::databaseParameters),
      Map.entry(Instant.class, IronArbitraries::instants),
      Map.entry(PagesDatabaseBlobDTO.class, IronArbitraries::pagesDatabaseBlobs),
      Map.entry(PagesDatabaseRedactionDTO.class, IronArbitraries::pagesDatabaseRedactions),
      Map.entry(Path.class, IronArbitraries::paths)
    );

  /**
   * Construct a provider.
   */

  public IronArbitraries()
  {

  }

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
   * @return A generator of {@link CDUserDTO} values
   */

  public static Arbitrary<CDUserDTO> users()
  {
    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    final var uuids =
      Arbitraries.create(UUID::randomUUID)
        .list()
        .ofSize(4);

    final var roles =
      roles()
        .set()
        .ofSize(4)
        .map(rrs -> {
          final var sortedRoles = new TreeSet<CDSecurityRoleDTO>();
          sortedRoles.addAll(rrs);
          return sortedRoles;
        })
        .list()
        .ofSize(2);

    final var hashes =
      passwordHashes()
        .list()
        .ofSize(3);

    return hashes.flatMap(hashesValues -> {
      return roles.flatMap(rolesValues -> {
        return uuids.flatMap(uuidValues -> {
          return strings.map(texts -> {
            final var instance0 =
              CDUserDTO.builder()
                .setDisplayName(texts.get(0))
                .setLocked(texts.get(1))
                .setEmail(texts.get(2))
                .setId(uuidValues.get(0))
                .setRoles(rolesValues.get(0))
                .setPasswordHash(hashesValues.get(0))
                .build();

            final var instance1 =
              instance0
                .withPasswordHash(hashesValues.get(1))
                .withRoles(rolesValues.get(1))
                .withId(uuidValues.get(1))
                .withDisplayName(texts.get(3))
                .withEmail(texts.get(4))
                .withLocked(texts.get(5));

            final var instance2 =
              CDUserDTO.builder().from(instance1).build();

            return CDUserDTO.copyOf(instance2);
          });
        });
      });
    });
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

  /**
   * @return A generator of {@link CDSecurityLabelDTO} values
   */

  public static Arbitrary<CDSecurityLabelDTO> labels()
  {
    final var longs =
      Arbitraries.longs()
        .list()
        .ofSize(4);

    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    return longs.flatMap(ids -> {
      return strings.map(texts -> {
        final var instance0 =
          CDSecurityLabelDTO.builder()
            .setDescription(texts.get(0))
            .setName(texts.get(1))
            .setId(ids.get(0).longValue())
            .build();

        final var instance1 =
          instance0.withName(texts.get(2))
            .withDescription(texts.get(3))
            .withId(ids.get(1).longValue());

        final var instance2 =
          CDSecurityLabelDTO.builder().from(instance1).build();

        return CDSecurityLabelDTO.copyOf(instance2);
      });
    });
  }

  /**
   * @return A generator of {@link CDSecurityRoleDTO} values
   */

  public static Arbitrary<CDSecurityRoleDTO> roles()
  {
    final var longs =
      Arbitraries.longs()
        .list()
        .ofSize(4);

    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    return longs.flatMap(ids -> {
      return strings.map(texts -> {
        final var instance0 =
          CDSecurityRoleDTO.builder()
            .setDescription(texts.get(0))
            .setName(texts.get(1))
            .setId(ids.get(0).longValue())
            .build();

        final var instance1 =
          instance0.withName(texts.get(2))
            .withDescription(texts.get(3))
            .withId(ids.get(1).longValue());

        final var instance2 =
          CDSecurityRoleDTO.builder().from(instance1).build();

        return CDSecurityRoleDTO.copyOf(instance2);
      });
    });
  }

  /**
   * @return A generator of {@link CDSessionDTO} values
   */

  public static Arbitrary<CDSessionDTO> sessions()
  {
    final var times =
      instants()
        .list()
        .ofSize(4);

    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    final var uuids =
      Arbitraries.create(UUID::randomUUID)
        .list()
        .ofSize(4);

    return uuids.flatMap(uuidValues -> {
      return times.flatMap(instants -> {
        return strings.map(texts -> {
          final var instance0 =
            CDSessionDTO.builder()
              .setId(texts.get(0))
              .setUserID(uuidValues.get(0))
              .setUpdated(instants.get(0))
              .build();

          final var instance1 =
            instance0.withId(texts.get(2))
              .withUserID(uuidValues.get(1))
              .withUpdated(instants.get(1));

          final var instance2 =
            CDSessionDTO.builder().from(instance1).build();

          return CDSessionDTO.copyOf(instance2);
        });
      });
    });
  }

  /**
   * @return A generator of {@link CDErrorCode} values
   */

  public static Arbitrary<CDErrorCode> errorCodes()
  {
    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    return strings.map(texts -> {
      final var instance0 =
        CDErrorCode.builder()
          .setCode(texts.get(0))
          .build();

      final var instance1 =
        instance0.withCode(texts.get(1));

      final var instance2 =
        CDErrorCode.builder().from(instance1).build();

      return CDErrorCode.copyOf(instance2);
    });
  }

  /**
   * @return A generator of {@link CDPasswordHashDTO} values
   */

  public static Arbitrary<CDPasswordHashDTO> passwordHashes()
  {
    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    final var hashes =
      Arbitraries.strings()
        .map(s -> s.getBytes(StandardCharsets.UTF_8))
        .list()
        .ofSize(4);

    return hashes.flatMap(hashValues -> {
      return strings.map(texts -> {
        final var instance0 =
          CDPasswordHashDTO.builder()
            .setParameters(texts.get(0))
            .setAlgorithm(texts.get(1))
            .setHash(hashValues.get(0))
            .build();

        final var instance1 =
          instance0.withParameters(texts.get(2))
            .withAlgorithm(texts.get(3))
            .withHash(hashValues.get(1));

        final var instance2 =
          CDPasswordHashDTO.builder().from(instance1).build();

        return CDPasswordHashDTO.copyOf(instance2);
      });
    });
  }

  /**
   * @return A generator of {@link PagesDatabaseRedactionDTO} values
   */

  public static Arbitrary<PagesDatabaseRedactionDTO> pagesDatabaseRedactions()
  {
    final var longs =
      Arbitraries.longs()
        .list()
        .ofSize(4);

    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    final var uuids =
      Arbitraries.create(UUID::randomUUID)
        .list()
        .ofSize(4);

    final var instants =
      instants()
        .list()
        .ofSize(4);

    return uuids.flatMap(uuidValues -> {
      return longs.flatMap(longValues -> {
        return instants.flatMap(instantsValues -> {
          return strings.map(stringsValues -> {
            final var instance0 =
              PagesDatabaseRedactionDTO.builder()
                .setId(longValues.get(0).longValue())
                .setTime(instantsValues.get(0))
                .setOwner(uuidValues.get(0))
                .setReason(stringsValues.get(0))
                .build();

            final var instance1 =
              instance0
                .withId(longValues.get(1).longValue())
                .withOwner(uuidValues.get(1))
                .withReason(stringsValues.get(1))
                .withTime(instantsValues.get(1));

            final var instance2 =
              PagesDatabaseRedactionDTO.builder()
                .from(instance1)
                .build();

            return PagesDatabaseRedactionDTO.copyOf(instance2);
          });
        });
      });
    });
  }

  /**
   * @return A generator of {@link PagesDatabaseBlobDTO} values
   */

  public static Arbitrary<PagesDatabaseBlobDTO> pagesDatabaseBlobs()
  {
    final var strings =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .list()
        .ofSize(16);

    final var bytes =
      Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(16)
        .map(String::getBytes)
        .list()
        .ofSize(16);

    final var labels =
      labels()
        .list()
        .ofSize(4);

    final var redactions =
      pagesDatabaseRedactions()
        .list()
        .ofSize(4);

    final var uuids =
      Arbitraries.create(UUID::randomUUID)
        .list()
        .ofSize(4);

    return uuids.flatMap(uuidValues -> {
      return strings.flatMap(stringsValues -> {
        return bytes.flatMap(bytesValues -> {
          return labels.flatMap(labelsValues -> {
            return redactions.map(redactionsValues -> {

              final var instance0 =
                PagesDatabaseBlobDTO.builder()
                  .setSecurityLabel(labelsValues.get(0))
                  .setMediaType(stringsValues.get(1))
                  .setData(bytesValues.get(0))
                  .setId(stringsValues.get(2))
                  .setRedaction(redactionsValues.get(0))
                  .setOwner(uuidValues.get(0))
                  .build();

              final var instance1 =
                instance0
                  .withSecurityLabel(labelsValues.get(1))
                  .withMediaType(stringsValues.get(3))
                  .withData(bytesValues.get(1))
                  .withId(stringsValues.get(4))
                  .withRedaction(redactionsValues.get(1))
                  .withOwner(uuidValues.get(1))
                ;

              final var instance2 =
                PagesDatabaseBlobDTO.builder()
                  .from(instance1)
                  .build();

              return PagesDatabaseBlobDTO.copyOf(instance2);
            });
          });
        });
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
