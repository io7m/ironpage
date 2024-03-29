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
import org.immutables.value.Value;

import java.math.BigInteger;
import java.net.URI;

/**
 * An import declaration.
 *
 * @param <T> The type of data that can be associated with the AST by various compiler stages
 */

@Value.Immutable
@ImmutablesStyleType
public interface MADeclImportType<T> extends MAElementType<T>
{
  @Override
  @Value.Auxiliary
  LexicalPosition<URI> lexical();

  @Override
  default MAElementType.Kind kind()
  {
    return MAElementType.Kind.IMPORT_DECLARATION;
  }

  @Override
  T data();

  /**
   * @return The name of the schema to import
   */

  String schemaName();

  /**
   * @return The major version of the schema to import
   */

  BigInteger versionMajor();

  /**
   * @return The minor version of the schema to import
   */

  BigInteger versionMinor();
}
