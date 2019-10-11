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
import com.io7m.ironpage.metadata.schema.types.api.TypePrimitive;
import com.io7m.jlexing.core.LexicalPosition;
import org.immutables.value.Value;

import java.net.URI;

/**
 * A reference to a type.
 *
 * @param <T> The type of data that can be associated with the AST by various compiler stages
 */

public interface MATypeReferenceType<T> extends MAElementType<T>
{
  @Override
  default MAElementType.Kind kind()
  {
    return MAElementType.Kind.TYPE_REFERENCE;
  }

  /**
   * @return The kind of reference
   */

  ReferenceKind referenceKind();

  /**
   * The kind of type references.
   */

  enum ReferenceKind
  {
    /**
     * The reference refers directly to a primitive type.
     */

    REFERENCE_PRIMITIVE,

    /**
     * The reference refers to another named type.
     */

    REFERENCE_NAMED
  }

  /**
   * The type of references to primitive types.
   *
   * @param <T> The type of data that can be associated with the AST by various compiler stages
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface MATypeReferencePrimitiveType<T> extends MATypeReferenceType<T>
  {
    @Override
    @Value.Auxiliary
    default ReferenceKind referenceKind()
    {
      return ReferenceKind.REFERENCE_PRIMITIVE;
    }

    @Override
    LexicalPosition<URI> lexical();

    /**
     * @return The primitive type
     */

    TypePrimitive primitive();
  }

  /**
   * The type of references to named types.
   *
   * @param <T> The type of data that can be associated with the AST by various compiler stages
   */

  @ImmutablesStyleType
  @Value.Immutable
  interface MATypeReferenceNamedType<T> extends MATypeReferenceType<T>
  {
    @Override
    @Value.Auxiliary
    default ReferenceKind referenceKind()
    {
      return ReferenceKind.REFERENCE_NAMED;
    }

    @Override
    LexicalPosition<URI> lexical();

    /**
     * @return The schema name within which the target type exists
     */

    String schema();

    /**
     * @return The name of the target type
     */

    String name();
  }
}
