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

import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaName;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverError;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverProviderType;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverType;
import com.io7m.ironpage.types.resolution.spi.MetaSchemaDirectoryType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

@Tag("schemaCompiler")
public abstract class MetaSchemaResolverContract
{
  protected abstract Logger logger();

  protected abstract MetaSchemaResolverProviderType resolvers(
    List<MetaSchemaDirectoryType> directories);

  /**
   * Empty resolutions give empty results.
   */

  @Test
  public final void testEmpty()
  {
    final var resolvers = this.resolvers(List.of());
    final var resolver = resolvers.create();

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(new java.util.TreeMap<>(), errors::add);

    errors.forEach(this::logError);
    this.logger().debug("result: {}", result);

    Assertions.assertTrue(result.isPresent());
    final var resolution = result.get();
    Assertions.assertEquals(0, resolution.schemas().size());
    Assertions.assertEquals(0, resolution.types().size());
    Assertions.assertEquals(0, resolution.graph().vertexSet().size());
  }

  /**
   * Modules with circular imports can't be resolved.
   */

  @Test
  public final void testCircular()
  {
    final var id0 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("x"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var id1 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("y"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var schema0 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(id0)
        .setImports(List.of(id1))
        .build();

    final var schema1 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(id1)
        .setImports(List.of(id0))
        .build();

    final MetaSchemaDirectoryType directory =
      (MetaSchemaIdentifier id) -> {
        if (Objects.equals(id, id0)) {
          return Optional.of(schema0);
        }
        if (Objects.equals(id, id1)) {
          return Optional.of(schema1);
        }
        return Optional.empty();
      };

    final var resolvers = this.resolvers(List.of(directory));
    final var resolver = resolvers.create();

    final SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports =
      new TreeMap<>(Map.of(schema0.identifier().name(), schema0.identifier()));

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(imports, errors::add);

    errors.forEach(this::logError);
    this.logger().debug("result: {}", result);

    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(1, errors.size());
    final var error0 = errors.get(0);
    Assertions.assertEquals(MetaSchemaResolverType.CIRCULAR_IMPORT, error0.errorCode());
    Assertions.assertEquals(ErrorSeverity.SEVERITY_ERROR, error0.severity());
  }

  /**
   * A missing schema can't be resolved.
   */

  @Test
  public final void testMissing()
  {
    final var id0 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("x"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var id1 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("y"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var schema0 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(id0)
        .setImports(List.of(id1))
        .build();

    final MetaSchemaDirectoryType directory =
      (MetaSchemaIdentifier id) -> {
        if (Objects.equals(id, id0)) {
          return Optional.of(schema0);
        }
        return Optional.empty();
      };

    final var resolvers = this.resolvers(List.of(directory));
    final var resolver = resolvers.create();

    final SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports =
      new TreeMap<>(Map.of(schema0.identifier().name(), schema0.identifier()));

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(imports, errors::add);

    errors.forEach(this::logError);
    this.logger().debug("result: {}", result);

    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(1, errors.size());
    final var error0 = errors.get(0);
    Assertions.assertEquals(MetaSchemaResolverType.SCHEMA_NOT_FOUND, error0.errorCode());
    Assertions.assertEquals(ErrorSeverity.SEVERITY_ERROR, error0.severity());
  }

  /**
   * Imports leading to version conflicts can't be resolved.
   */

  @Test
  public final void testVersionConflict()
  {
    final var x10 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("x"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var x20 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("x"))
        .setVersionMajor(BigInteger.TWO)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var import10 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("import10"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var import20 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("import20"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var importAll =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("import_all"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var schemaX10 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(x10)
        .build();

    final var schemaX20 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(x20)
        .build();

    final var schemaImport10 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(import10)
        .setImports(List.of(schemaX10.identifier()))
        .build();

    final var schemaImport20 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(import20)
        .setImports(List.of(schemaX20.identifier()))
        .build();

    final var schemaImportAll =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(importAll)
        .setImports(List.of(schemaImport10.identifier(), schemaImport20.identifier()))
        .build();

    final MetaSchemaDirectoryType directory =
      (MetaSchemaIdentifier id) -> {
        if (Objects.equals(id, x10)) {
          return Optional.of(schemaX10);
        }
        if (Objects.equals(id, x20)) {
          return Optional.of(schemaX20);
        }
        if (Objects.equals(id, import10)) {
          return Optional.of(schemaImport10);
        }
        if (Objects.equals(id, import20)) {
          return Optional.of(schemaImport20);
        }
        if (Objects.equals(id, importAll)) {
          return Optional.of(schemaImportAll);
        }
        return Optional.empty();
      };

    final var resolvers = this.resolvers(List.of(directory));
    final var resolver = resolvers.create();

    final SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports =
      new TreeMap<>(Map.of(schemaImportAll.identifier().name(), schemaImportAll.identifier()));

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(imports, errors::add);

    errors.forEach(this::logError);
    this.logger().debug("result: {}", result);

    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(1, errors.size());
    final var error0 = errors.get(0);
    Assertions.assertEquals(MetaSchemaResolverType.VERSION_CONFLICT, error0.errorCode());
    Assertions.assertEquals(ErrorSeverity.SEVERITY_ERROR, error0.severity());
  }

  /**
   * A failing directory is a warning.
   */

  @Test
  public final void testFailingDirectory()
  {
    final var id0 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("x"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var id1 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("y"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var schema0 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(id0)
        .setImports(List.of(id1))
        .build();

    final MetaSchemaDirectoryType directory =
      (MetaSchemaIdentifier id) -> {
        throw new IOException("I/O error");
      };

    final var resolvers = this.resolvers(List.of(directory));
    final var resolver = resolvers.create();

    final SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports =
      new TreeMap<>(Map.of(schema0.identifier().name(), schema0.identifier()));

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(imports, errors::add);

    errors.forEach(this::logError);
    this.logger().debug("result: {}", result);

    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(2, errors.size());

    final var error0 = errors.get(0);
    Assertions.assertEquals(MetaSchemaResolverType.SCHEMA_DIRECTORY_FAILED, error0.errorCode());
    Assertions.assertEquals(ErrorSeverity.SEVERITY_WARNING, error0.severity());
    Assertions.assertEquals(IOException.class, error0.exception().get().getClass());

    final var error1 = errors.get(1);
    Assertions.assertEquals(MetaSchemaResolverType.SCHEMA_NOT_FOUND, error1.errorCode());
    Assertions.assertEquals(ErrorSeverity.SEVERITY_ERROR, error1.severity());
  }

  /**
   * A directory that returns the wrong schema is a warning.
   */

  @Test
  public final void testWrongDirectory()
  {
    final var id0 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("x"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var id1 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("y"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var schema0 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(id0)
        .setImports(List.of(id1))
        .build();

    final MetaSchemaDirectoryType directory =
      (MetaSchemaIdentifier id) -> Optional.of(schema0);

    final var resolvers = this.resolvers(List.of(directory));
    final var resolver = resolvers.create();

    final SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports =
      new TreeMap<>(Map.of(schema0.identifier().name(), schema0.identifier()));

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(imports, errors::add);

    errors.forEach(this::logError);
    this.logger().debug("result: {}", result);

    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(2, errors.size());

    final var error0 = errors.get(0);
    Assertions.assertEquals(MetaSchemaResolverType.SCHEMA_DIRECTORY_FAILED, error0.errorCode());
    Assertions.assertEquals(ErrorSeverity.SEVERITY_WARNING, error0.severity());

    final var error1 = errors.get(1);
    Assertions.assertEquals(MetaSchemaResolverType.SCHEMA_NOT_FOUND, error1.errorCode());
    Assertions.assertEquals(ErrorSeverity.SEVERITY_ERROR, error1.severity());
  }

  /**
   * The error receiver crashing doesn't cause problems for the resolver.
   */

  @Test
  public final void testReceiverCrashes()
  {
    final var id0 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("x"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var id1 =
      MetaSchemaIdentifier.builder()
        .setName(MetaSchemaName.of("y"))
        .setVersionMajor(BigInteger.ONE)
        .setVersionMinor(BigInteger.ZERO)
        .build();

    final var schema0 =
      MetaSchema.builder()
        .setAttributes(List.of())
        .setIdentifier(id0)
        .setImports(List.of(id1))
        .build();

    final MetaSchemaDirectoryType directory =
      (MetaSchemaIdentifier id) -> {
        if (Objects.equals(id, id0)) {
          return Optional.of(schema0);
        }
        return Optional.empty();
      };

    final var resolvers = this.resolvers(List.of(directory));
    final var resolver = resolvers.create();

    final SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports =
      new TreeMap<>(Map.of(schema0.identifier().name(), schema0.identifier()));

    final var errors = new ArrayList<MetaSchemaResolverError>();
    final var result = resolver.resolve(imports, error -> {
      errors.add(error);
      throw new IllegalStateException();
    });

    errors.forEach(this::logError);
    this.logger().debug("result: {}", result);

    Assertions.assertTrue(result.isEmpty());
    Assertions.assertEquals(1, errors.size());
    final var error0 = errors.get(0);
    Assertions.assertEquals(MetaSchemaResolverType.SCHEMA_NOT_FOUND, error0.errorCode());
    Assertions.assertEquals(ErrorSeverity.SEVERITY_ERROR, error0.severity());
  }

  private void logError(
    final MetaSchemaResolverError error)
  {
    switch (error.severity()) {
      case SEVERITY_WARNING:
        this.logger().warn("{}", error);
        break;
      case SEVERITY_ERROR:
        this.logger().error("{}", error);
        break;
    }
  }
}
