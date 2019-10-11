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


package com.io7m.ironpage.metadata.schema.ast;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.junreachable.UnreachableCodeException;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A schema declaration.
 *
 * @param <T> The type of data that can be associated with the AST by various compiler stages
 */

@Value.Immutable
@ImmutablesStyleType
public interface MADeclSchemaType<T> extends MAElementType<T>
{
  @Override
  LexicalPosition<URI> lexical();

  @Override
  default MAElementType.Kind kind()
  {
    return MAElementType.Kind.SCHEMA_DECLARATION;
  }

  /**
   * @return The name of the schema
   */

  String schemaName();

  /**
   * @return The major version of the schema
   */

  BigInteger versionMajor();

  /**
   * @return The minor version of the schema
   */

  BigInteger versionMinor();

  /**
   * @return The declarations within the schema
   */

  List<MAElementType<T>> declarations();

  @Override
  T data();

  /**
   * @return The comment declarations
   */

  @Value.Derived
  @Value.Auxiliary
  default List<MADeclComment<T>> comments()
  {
    return this.declarations()
      .stream()
      .filter(d -> d instanceof MADeclComment)
      .map(d -> (MADeclComment<T>) d)
      .collect(Collectors.toList());
  }

  /**
   * @return The import declarations
   */

  @Value.Derived
  @Value.Auxiliary
  default List<MADeclImport<T>> imports()
  {
    return this.declarations()
      .stream()
      .filter(d -> d instanceof MADeclImport)
      .map(d -> (MADeclImport<T>) d)
      .collect(Collectors.toList());
  }

  /**
   * @return The type declarations
   */

  @Value.Derived
  @Value.Auxiliary
  default Map<String, MADeclType<T>> types()
  {
    return this.declarations()
      .stream()
      .filter(d -> d instanceof MADeclType)
      .map(d -> (MADeclType<T>) d)
      .collect(Collectors.toMap(
        MADeclType::name,
        Function.identity()
      ));
  }

  /**
   * @return The attribute declarations
   */

  @Value.Derived
  @Value.Auxiliary
  default Map<String, MADeclAttribute<T>> attributes()
  {
    return this.declarations()
      .stream()
      .filter(d -> d instanceof MADeclAttribute)
      .map(d -> (MADeclAttribute<T>) d)
      .collect(Collectors.toMap(
        MADeclAttribute::name,
        Function.identity()
      ));
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    if (this.declarations().stream().anyMatch(p -> {
      switch (p.kind()) {
        case TYPE_REFERENCE:
        case SCHEMA_DECLARATION:
          return true;
        case TYPE_DECLARATION:
        case IMPORT_DECLARATION:
        case ATTRIBUTE_DECLARATION:
        case COMMENT:
          return false;
      }
      throw new UnreachableCodeException();
    })) {
      throw new IllegalArgumentException(
        "Schemas can only contain imports, types, attributes, and comments.");
    }
  }
}
