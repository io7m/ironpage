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

import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;
import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaName;

import java.util.Optional;
import java.util.SortedMap;

/**
 * The type of schema resolvers.
 */

public interface MetaSchemaResolverType
{
  /**
   * A version conflict occurred; the import graph cannot contain multiple versions of the same
   * schema.
   */

  MetaSchemaResolverErrorCode VERSION_CONFLICT =
    MetaSchemaResolverErrorCode.of(
      new StringBuilder(64)
        .append(MetaSchemaResolverType.class.getCanonicalName())
        .append(":importVersionConflict")
        .toString());

  /**
   * A cycle appeared in the import graph.
   */

  MetaSchemaResolverErrorCode CIRCULAR_IMPORT =
    MetaSchemaResolverErrorCode.of(
      new StringBuilder(64)
        .append(MetaSchemaResolverType.class.getCanonicalName())
        .append(":importCircular")
        .toString());

  /**
   * A schema was not found.
   */

  MetaSchemaResolverErrorCode SCHEMA_NOT_FOUND =
    MetaSchemaResolverErrorCode.of(
      new StringBuilder(64)
        .append(MetaSchemaResolverType.class.getCanonicalName())
        .append(":schemaNotFound")
        .toString());

  /**
   * A schema directory failed.
   */

  MetaSchemaResolverErrorCode SCHEMA_DIRECTORY_FAILED =
    MetaSchemaResolverErrorCode.of(
      new StringBuilder(64)
        .append(MetaSchemaResolverType.class.getCanonicalName())
        .append(":schemaDirectoryFailed")
        .toString());

  /**
   * Resolve a set of imports.
   *
   * @param imports  The schema imports
   * @param receiver An error receiver
   *
   * @return The resolved set of imports, if resolution did not fail
   */

  Optional<MetaSchemaResolvedSet> resolve(
    SortedMap<MetaSchemaName, MetaSchemaIdentifier> imports,
    MetaSchemaResolverErrorReceiverType receiver);

}
