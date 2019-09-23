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

package com.io7m.ironpage.validator.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.ironpage.types.api.AttributeValueUntyped;
import com.io7m.ironpage.types.api.SchemaIdentifier;
import com.io7m.ironpage.types.api.SchemaName;
import com.io7m.ironpage.types.resolution.api.SchemaResolvedSet;
import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.collection.TreeMap;
import io.vavr.collection.Vector;
import org.immutables.value.Value;

import java.util.HashMap;

/**
 * The type of schema validation requests.
 */

@Value.Immutable
@ImmutablesStyleType
public interface SchemaValidationRequestType
{
  /**
   * @return The set of resolved modules use to satisfy imports
   */

  @Value.Parameter
  SchemaResolvedSet resolvedModules();

  /**
   * @return The imported schemas
   */

  @Value.Parameter
  @Value.Default
  default Seq<SchemaIdentifier> imports()
  {
    return Vector.empty();
  }

  /**
   * @return The attribute values
   */

  @Value.Parameter
  @Value.Default
  default Seq<AttributeValueUntyped> attributes()
  {
    return Vector.empty();
  }

  /**
   * @return The imports organized by name
   */

  @Value.Derived
  default SortedMap<SchemaName, SchemaIdentifier> importsByName()
  {
    final var imports = this.imports();
    final var importMap = new HashMap<SchemaName, SchemaIdentifier>(imports.size());

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

    return TreeMap.ofAll(importMap);
  }

}
