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

import java.util.Optional;
import java.util.stream.Stream;

/**
 * The roles-related queries supported by core databases.
 */

public interface CDRolesQueriesType extends DatabaseQueriesType
{
  /**
   * A role already exists.
   */

  CDErrorCode ROLE_ALREADY_EXISTS =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDRolesQueriesType.class.getCanonicalName())
        .append(":roleAlreadyExists")
        .toString());

  /**
   * A role does not exist.
   */

  CDErrorCode ROLE_NONEXISTENT =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDRolesQueriesType.class.getCanonicalName())
        .append(":roleNonexistent")
        .toString());

  /**
   * An unexpected database error occurred.
   */

  CDErrorCode DATABASE_ERROR =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDRolesQueriesType.class.getCanonicalName())
        .append(":databaseError")
        .toString());

  /**
   * Create a role with the given name and description.
   *
   * @param name        The name of the role (must be unique)
   * @param description A humanly-readable description of the role
   *
   * @return A new role
   *
   * @throws CDException On errors
   */

  CDSecurityRoleDTO roleCreate(
    String name,
    String description)
    throws CDException;

  /**
   * @param id The ID of the role
   *
   * @return The role in the database with the given ID, if any
   *
   * @throws CDException On errors
   */

  Optional<CDSecurityRoleDTO> roleGet(long id)
    throws CDException;

  /**
   * @param name The name of the role
   *
   * @return The role in the database with the given ID, if any
   *
   * @throws CDException On errors
   */

  Optional<CDSecurityRoleDTO> roleGetForName(String name)
    throws CDException;

  /**
   * Update the name and/or description of the given role.
   *
   * @param role The role to be updated
   *
   * @return The updated role
   *
   * @throws CDException On errors
   */

  CDSecurityRoleDTO roleUpdate(CDSecurityRoleDTO role)
    throws CDException;

  /**
   * @return All available roles in the database.
   *
   * @throws CDException On errors
   */

  Stream<CDSecurityRoleDTO> roleList()
    throws CDException;
}
