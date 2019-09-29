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

package com.io7m.ironpage.database.accounts.api;

import com.io7m.ironpage.database.spi.DatabaseQueriesType;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The queries supported by accounts databases.
 */

public interface AccountsDatabaseQueriesType extends DatabaseQueriesType
{
  /**
   * A display name is already used.
   */

  AccountsDatabaseErrorCode DISPLAY_NAME_ALREADY_USED =
    AccountsDatabaseErrorCode.of(
      new StringBuilder(64)
        .append(AccountsDatabaseQueriesType.class.getCanonicalName())
        .append(":displayNameAlreadyUsed")
        .toString());

  /**
   * An ID is already used.
   */

  AccountsDatabaseErrorCode ID_ALREADY_USED =
    AccountsDatabaseErrorCode.of(
      new StringBuilder(64)
        .append(AccountsDatabaseQueriesType.class.getCanonicalName())
        .append(":idAlreadyUsed")
        .toString());

  /**
   * A user does not exist.
   */

  AccountsDatabaseErrorCode NONEXISTENT =
    AccountsDatabaseErrorCode.of(
      new StringBuilder(64)
        .append(AccountsDatabaseQueriesType.class.getCanonicalName())
        .append(":nonexistent")
        .toString());

  /**
   * One or more fields were invalid.
   */

  AccountsDatabaseErrorCode INVALID_DATA =
    AccountsDatabaseErrorCode.of(
      new StringBuilder(64)
        .append(AccountsDatabaseQueriesType.class.getCanonicalName())
        .append(":invalidData")
        .toString());

  /**
   * An unexpected database error occurred.
   */

  AccountsDatabaseErrorCode DATABASE_ERROR =
    AccountsDatabaseErrorCode.of(
      new StringBuilder(64)
        .append(AccountsDatabaseQueriesType.class.getCanonicalName())
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
   * @throws AccountsDatabaseException On errors
   */

  AccountsDatabaseUserDTO accountCreate(
    UUID id,
    String displayName,
    AccountsDatabasePasswordHashDTO password,
    String email,
    Optional<String> lockedReason)
    throws AccountsDatabaseException;

  /**
   * Update an account.
   *
   * @param caller  The account performing the change (may not be the same as the target account)
   * @param account The account
   *
   * @return An updated account
   *
   * @throws AccountsDatabaseException On errors
   */

  AccountsDatabaseUserDTO accountUpdate(
    UUID caller,
    AccountsDatabaseUserDTO account)
    throws AccountsDatabaseException;

  /**
   * Retrieve an account.
   *
   * @param userId The account ID
   *
   * @return The account
   *
   * @throws AccountsDatabaseException On errors
   */

  AccountsDatabaseUserDTO accountGet(
    UUID userId)
    throws AccountsDatabaseException;

  /**
   * Find an account.
   *
   * @param userId      The account ID (if any)
   * @param displayName The display name (if any)
   * @param email       The email (if any)
   *
   * @return A stream of matching accounts
   *
   * @throws AccountsDatabaseException On errors
   */

  Stream<AccountsDatabaseUserDTO> accountFind(
    Optional<UUID> userId,
    Optional<String> displayName,
    Optional<String> email)
    throws AccountsDatabaseException;

  /**
   * Create a new session for the given account.
   *
   * @param owner   The account ID
   * @param session The session ID
   *
   * @return A new session
   *
   * @throws AccountsDatabaseException On errors
   */

  AccountsDatabaseSessionDTO accountSessionCreate(
    UUID owner,
    String session)
    throws AccountsDatabaseException;

  /**
   * Update a session.
   *
   * @param session The session ID
   *
   * @return The updated session
   *
   * @throws AccountsDatabaseException On errors
   */

  AccountsDatabaseSessionDTO accountSessionUpdate(
    String session)
    throws AccountsDatabaseException;

  /**
   * Delete a session.
   *
   * @param session The session ID
   *
   * @throws AccountsDatabaseException On errors
   */

  void accountSessionDelete(
    String session)
    throws AccountsDatabaseException;

  /**
   * Delete all sessions for a given user.
   *
   * @param owner The user ID
   *
   * @return The number of deleted sessions
   *
   * @throws AccountsDatabaseException On errors
   */

  int accountSessionDeleteForUser(
    UUID owner)
    throws AccountsDatabaseException;
}
