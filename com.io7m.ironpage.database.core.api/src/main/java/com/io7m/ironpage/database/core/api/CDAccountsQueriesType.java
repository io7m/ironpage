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
import java.util.UUID;

/**
 * The accounts-related queries supported by core databases.
 */

public interface CDAccountsQueriesType extends DatabaseQueriesType
{
  /**
   * A display name is already used.
   */

  CDErrorCode DISPLAY_NAME_ALREADY_USED =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDAccountsQueriesType.class.getCanonicalName())
        .append(":displayNameAlreadyUsed")
        .toString());

  /**
   * An ID is already used.
   */

  CDErrorCode ID_ALREADY_USED =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDAccountsQueriesType.class.getCanonicalName())
        .append(":idAlreadyUsed")
        .toString());

  /**
   * A referenced object does not exist.
   */

  CDErrorCode NONEXISTENT =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDAccountsQueriesType.class.getCanonicalName())
        .append(":nonexistent")
        .toString());

  /**
   * One or more fields were invalid.
   */

  CDErrorCode INVALID_DATA =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDAccountsQueriesType.class.getCanonicalName())
        .append(":invalidData")
        .toString());

  /**
   * An unexpected database error occurred.
   */

  CDErrorCode DATABASE_ERROR =
    CDErrorCode.of(
      new StringBuilder(64)
        .append(CDAccountsQueriesType.class.getCanonicalName())
        .append(":databaseError")
        .toString());

  /**
   * Create an account.
   *
   * @param id           The account ID
   * @param displayName  The display name
   * @param password     The password hash
   * @param email        The account email address
   * @param lockedReason The reason the account is locked, or nothing if the account is not locked
   *
   * @return A new account
   *
   * @throws CDException On errors
   */

  @EventPublishedType(CDAccountCreated.class)
  CDUserDTO accountCreate(
    UUID id,
    String displayName,
    CDPasswordHashDTO password,
    String email,
    Optional<String> lockedReason)
    throws CDException;

  /**
   * Update an account.
   *
   * @param caller  The account performing the change (may not be the same as the target account)
   * @param account The account
   *
   * @return An updated account
   *
   * @throws CDException On errors
   */

  @EventPublishedType(CDAccountUpdated.class)
  CDUserDTO accountUpdate(
    UUID caller,
    CDUserDTO account)
    throws CDException;

  /**
   * Retrieve an account.
   *
   * @param userId The account ID
   *
   * @return The account
   *
   * @throws CDException On errors
   */

  CDUserDTO accountGet(
    UUID userId)
    throws CDException;

  /**
   * Create a new session for the given account.
   *
   * @param owner   The account ID
   * @param session The session ID
   *
   * @return A new session
   *
   * @throws CDException On errors
   */

  CDSessionDTO accountSessionCreate(
    UUID owner,
    String session)
    throws CDException;

  /**
   * Update a session.
   *
   * @param session The session ID
   *
   * @return The updated session
   *
   * @throws CDException On errors
   */

  CDSessionDTO accountSessionUpdate(
    String session)
    throws CDException;

  /**
   * Delete a session.
   *
   * @param session The session ID
   *
   * @throws CDException On errors
   */

  void accountSessionDelete(
    String session)
    throws CDException;

  /**
   * Delete all sessions for a given user.
   *
   * @param owner The user ID
   *
   * @return The number of deleted sessions
   *
   * @throws CDException On errors
   */

  int accountSessionDeleteForUser(
    UUID owner)
    throws CDException;
}
