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

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The declaration of a metadata schema.
 */

@Value.Immutable
@ImmutablesStyleType
public interface MetaSchemaType
{
  /**
   * @return The identifier of the schema
   */

  @Value.Parameter
  MetaSchemaIdentifier identifier();

  /**
   * @return The schemas imported by this schema
   */

  @Value.Parameter
  List<MetaSchemaIdentifier> imports();

  /**
   * @return The named attribute types exported by the schema
   */

  @Value.Parameter
  List<TypeNamed> types();

  /**
   * @return The attributes declared by the schema
   */

  @Value.Parameter
  List<MetaSchemaAttribute> attributes();

  /**
   * @return The imports organized by name
   */

  @Value.Derived
  @Value.Auxiliary
  default SortedMap<MetaSchemaName, MetaSchemaIdentifier> importsByName()
  {
    final var imports = this.imports();
    final var importMap = new TreeMap<MetaSchemaName, MetaSchemaIdentifier>();

    for (final var importValue : imports) {
      final var name = importValue.name();
      if (importMap.containsKey(name)) {
        final var separator = System.lineSeparator();
        throw new IllegalArgumentException(
          new StringBuilder(64)
            .append("Multiple imports with the same name.")
            .append(separator)
            .append("  Name: ")
            .append(name.name())
            .append(separator)
            .toString());
      }
      importMap.put(name, importValue);
    }

    return Collections.unmodifiableSortedMap(importMap);
  }

  /**
   * @return The types organized by name
   */

  @Value.Derived
  @Value.Auxiliary
  default SortedMap<TypeName, TypeNamed> typesByName()
  {
    final var types = this.types();
    final var typeMap = new TreeMap<TypeName, TypeNamed>();

    for (final var type : types) {
      final var name = type.name();
      if (typeMap.containsKey(name)) {
        final var separator = System.lineSeparator();
        throw new IllegalArgumentException(
          new StringBuilder(64)
            .append("Multiple types with the same name.")
            .append(separator)
            .append("  Name: ")
            .append(name.name())
            .append(separator)
            .toString());
      }
      typeMap.put(name, type);
    }

    return Collections.unmodifiableSortedMap(typeMap);
  }

  /**
   * @return The attributes organized by name
   */

  @Value.Derived
  @Value.Auxiliary
  default SortedMap<AttributeName, MetaSchemaAttribute> attributesByName()
  {
    final var attrs = this.attributes();
    final var attributeMap = new TreeMap<AttributeName, MetaSchemaAttribute>();

    for (final var attr : attrs) {
      final var name = attr.name();
      if (attributeMap.containsKey(name)) {
        final var separator = System.lineSeparator();
        throw new IllegalArgumentException(
          new StringBuilder(64)
            .append("Multiple attributes with the same name.")
            .append(separator)
            .append("  Name: ")
            .append(name.name())
            .append(separator)
            .toString());
      }
      attributeMap.put(name, attr);
    }

    return Collections.unmodifiableSortedMap(attributeMap);
  }
}
