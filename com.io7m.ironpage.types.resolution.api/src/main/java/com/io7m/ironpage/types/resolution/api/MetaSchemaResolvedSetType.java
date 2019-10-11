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

package com.io7m.ironpage.types.resolution.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.ironpage.metadata.schema.types.api.TypeNameQualified;
import com.io7m.ironpage.metadata.schema.types.api.TypeNamedType;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaName;
import org.immutables.value.Value;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A resolved set of modules.
 */

@ImmutablesStyleType
@Value.Immutable
public interface MetaSchemaResolvedSetType
{
  /**
   * @return The graph of resolved modules
   */

  @Value.Parameter
  DirectedAcyclicGraph<MetaSchemaIdentifier, MetaSchemaResolvedSetEdge> graph();

  /**
   * @return The available schema declarations
   */

  @Value.Parameter
  List<MetaSchema> schemas();

  /**
   * @return The available types by name
   */

  @Value.Derived
  default Map<TypeNameQualified, TypeNamedType> types()
  {
    final var schemas =
      this.schemas();
    final var typeMap =
      new HashMap<TypeNameQualified, TypeNamedType>(
        schemas.size());

    for (final var schemaDeclaration : schemas) {
      final var schemaName = schemaDeclaration.identifier().name();
      for (final var type : schemaDeclaration.types()) {
        final var attrName = TypeNameQualified.of(schemaName, type.name());
        if (typeMap.containsKey(attrName)) {
          final var separator = System.lineSeparator();
          throw new IllegalArgumentException(
            new StringBuilder(64)
              .append("Multiple types with the same name.")
              .append(separator)
              .append("  Name: ")
              .append(schemaName.name())
              .append(separator)
              .toString());
        }
        typeMap.put(attrName, type);
      }
    }
    return Map.copyOf(typeMap);
  }

  /**
   * @return The available schemas by name
   */

  @Value.Derived
  default Map<MetaSchemaName, MetaSchema> schemasByName()
  {
    final var schemas = this.schemas();
    final var schemaMap = new HashMap<MetaSchemaName, MetaSchema>(schemas.size());

    for (final var schemaDeclaration : schemas) {
      final var name = schemaDeclaration.identifier().name();
      if (schemaMap.containsKey(name)) {
        final var separator = System.lineSeparator();
        throw new IllegalArgumentException(
          new StringBuilder(64)
            .append("Multiple schemas with the same name.")
            .append(separator)
            .append("  Name: ")
            .append(name.name())
            .append(separator)
            .toString());
      }
      schemaMap.put(name, schemaDeclaration);
    }

    return Map.copyOf(schemaMap);
  }

}
