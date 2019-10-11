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

package com.io7m.ironpage.metadata.schema.compiler.spi;

import com.io7m.ironpage.metadata.schema.types.api.MetaSchemaIdentifier;

import java.io.IOException;
import java.util.Optional;

/**
 * An interface for fetching schema sources.
 *
 * The purpose of this interface is to abstract over the actual reading of schema sources. The
 * intention is for implementations to read schema files from the filesystem, from jar resources, or
 * relational databases.
 */

public interface MSchemaCompilerSourceType
{
  /**
   * Retrieve the sources for a schema with identifier {@code identifier}.
   *
   * @param identifier The schema identifier
   *
   * @return The schema sources, or nothing if the schema does not exist
   *
   * @throws IOException On I/O errors
   */

  Optional<MSchemaCompilerStream> openSchemaSource(
    MetaSchemaIdentifier identifier)
    throws IOException;
}
