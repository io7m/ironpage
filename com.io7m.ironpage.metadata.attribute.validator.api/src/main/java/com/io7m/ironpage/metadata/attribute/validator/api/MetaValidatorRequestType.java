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


package com.io7m.ironpage.metadata.attribute.validator.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.ironpage.metadata.schema.types.api.MetaDocumentUntyped;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchema;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import org.immutables.value.Value;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A validation request.
 */

@ImmutablesStyleType
@Value.Immutable
public interface MetaValidatorRequestType
{
  /**
   * @return A list of available schemas
   */

  List<MetaSchema> schemas();

  /**
   * @return An untyped document
   */

  MetaDocumentUntyped document();

  /**
   * Find the schema with the given identifier.
   *
   * @param identifier The identifier
   *
   * @return The schema, if any
   */

  default Optional<MetaSchema> findSchema(
    final MetaSchemaIdentifier identifier)
  {
    Objects.requireNonNull(identifier, "identifier");
    return this.schemas()
      .stream()
      .filter(schema -> Objects.equals(schema.identifier(), identifier))
      .findFirst();
  }
}
