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

package com.io7m.ironpage.database.pages.api;

import com.io7m.ironpage.database.core.api.CDErrorCode;
import com.io7m.ironpage.database.core.api.CDException;
import com.io7m.ironpage.database.core.api.CDSecurityLabelDTO;
import com.io7m.ironpage.database.spi.DatabaseQueriesType;
import com.io7m.ironpage.events.api.EventPublishedType;

import java.util.Optional;
import java.util.UUID;

/**
 * The queries supported by pages databases.
 */

public interface PagesDatabaseQueriesType extends DatabaseQueriesType
{
  /**
   * A piece of inserted data already exists.
   */

  CDErrorCode DATA_ALREADY_EXISTS =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(PagesDatabaseQueriesType.class.getCanonicalName())
        .append(":dataAlreadyExists")
        .toString());

  /**
   * A referenced piece of data doesn't exist.
   */

  CDErrorCode DATA_NONEXISTENT =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(PagesDatabaseQueriesType.class.getCanonicalName())
        .append(":dataNonexistent")
        .toString());

  /**
   * An unexpected database error occurred.
   */

  CDErrorCode DATABASE_ERROR =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(PagesDatabaseQueriesType.class.getCanonicalName())
        .append(":databaseError")
        .toString());

  /**
   * One or more page data fields were invalid.
   */

  CDErrorCode DATA_INVALID =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(PagesDatabaseQueriesType.class.getCanonicalName())
        .append(":dataInvalid")
        .toString());

  /**
   * The owner of the page data does not exist.
   */

  CDErrorCode DATA_OWNER_NONEXISTENT =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(PagesDatabaseQueriesType.class.getCanonicalName())
        .append(":dataOwnerNonexistent")
        .toString());

  /**
   * Save the given page blob.
   *
   * @param owner     The ID of the owner
   * @param mediaType The IANA media type
   * @param data      The data
   * @param label     The security label of the blob
   *
   * @return The hash of the blob
   *
   * @throws CDException On database errors
   * @see "https://www.iana.org/assignments/media-types/media-types.xhtml"
   */

  @EventPublishedType(PagesDatabaseBlobCreated.class)
  String pageBlobPut(
    UUID owner,
    String mediaType,
    byte[] data,
    CDSecurityLabelDTO label)
    throws CDException;

  /**
   * Retrieve the given page blob.
   *
   * @param id The blob ID
   *
   * @return A blob
   *
   * @throws CDException On database errors
   */

  Optional<PagesDatabaseBlobDTO> pageBlobGet(
    String id)
    throws CDException;

  /**
   * Redact the given page blob.
   *
   * @param owner  The owner of the redaction
   * @param id     The blob ID
   * @param reason The redaction reason
   *
   * @throws CDException On database errors
   */

  @EventPublishedType(PagesDatabaseBlobRedacted.class)
  void pageBlobRedact(
    UUID owner,
    String id,
    String reason)
    throws CDException;
}
