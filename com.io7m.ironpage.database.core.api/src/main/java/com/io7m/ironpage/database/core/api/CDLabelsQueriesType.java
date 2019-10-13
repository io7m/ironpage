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

package com.io7m.ironpage.database.core.api;

import com.io7m.ironpage.database.spi.DatabaseQueriesType;
import com.io7m.ironpage.events.api.EventPublishedType;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * The labels-related queries supported by core databases.
 */

public interface CDLabelsQueriesType extends DatabaseQueriesType
{
  /**
   * A label already exists.
   */

  CDErrorCode LABEL_ALREADY_EXISTS =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDLabelsQueriesType.class.getCanonicalName())
        .append(":labelAlreadyExists")
        .toString());

  /**
   * A label does not exist.
   */

  CDErrorCode LABEL_NONEXISTENT =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDLabelsQueriesType.class.getCanonicalName())
        .append(":labelNonexistent")
        .toString());

  /**
   * An unexpected database error occurred.
   */

  CDErrorCode DATABASE_ERROR =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDLabelsQueriesType.class.getCanonicalName())
        .append(":databaseError")
        .toString());

  /**
   * Create a label with the given name and description.
   *
   * @param name        The name of the label (must be unique)
   * @param description A humanly-readable description of the label
   *
   * @return A new label
   *
   * @throws CDException On errors
   */

  @EventPublishedType(CDSecurityLabelCreated.class)
  CDSecurityLabelDTO labelCreate(
    String name,
    String description)
    throws CDException;

  /**
   * @param id The ID of the label
   *
   * @return The label in the database with the given ID, if any
   *
   * @throws CDException On errors
   */

  Optional<CDSecurityLabelDTO> labelGet(long id)
    throws CDException;

  /**
   * @param name The name of the label
   *
   * @return The label in the database with the given ID, if any
   *
   * @throws CDException On errors
   */

  Optional<CDSecurityLabelDTO> labelGetForName(String name)
    throws CDException;

  /**
   * Update the name and/or description of the given label.
   *
   * @param label The label to be updated
   *
   * @return The updated label
   *
   * @throws CDException On errors
   */

  @EventPublishedType(CDSecurityLabelUpdated.class)
  CDSecurityLabelDTO labelUpdate(CDSecurityLabelDTO label)
    throws CDException;

  /**
   * @return All available labels in the database.
   *
   * @throws CDException On errors
   */

  Stream<CDSecurityLabelDTO> labelList()
    throws CDException;
}
