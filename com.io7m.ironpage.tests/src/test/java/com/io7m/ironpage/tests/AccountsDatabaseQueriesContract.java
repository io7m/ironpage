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

package com.io7m.ironpage.tests;

import com.io7m.ironpage.database.accounts.api.AccountsDatabaseException;
import com.io7m.ironpage.database.accounts.api.AccountsDatabasePasswordHashDTO;
import com.io7m.ironpage.database.accounts.api.AccountsDatabaseQueriesType;
import com.io7m.ironpage.database.accounts.api.AccountsDatabaseUserDTO;
import com.io7m.ironpage.database.spi.DatabaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.ironpage.database.accounts.api.AccountsDatabaseQueriesType.DISPLAY_NAME_ALREADY_USED;
import static com.io7m.ironpage.database.accounts.api.AccountsDatabaseQueriesType.ID_ALREADY_USED;
import static com.io7m.ironpage.database.accounts.api.AccountsDatabaseQueriesType.INVALID_DATA;
import static com.io7m.ironpage.database.accounts.api.AccountsDatabaseQueriesType.NONEXISTENT;

public abstract class AccountsDatabaseQueriesContract
{
  private static final Logger LOG = LoggerFactory.getLogger(AccountsDatabaseQueriesContract.class);

  protected abstract AccountsDatabaseQueriesType queries()
    throws DatabaseException;

  /**
   * Two users cannot be created with the same display name.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountCreateDisplayNameUsed()
    throws Exception
  {
    final var queries = this.queries();

    queries.accountCreate(
      UUID.randomUUID(),
      "User",
      AccountsDatabasePasswordHashDTO.builder()
        .setHash(new byte[16])
        .setParameters("params")
        .build(),
      "someone@example.com",
      Optional.empty());

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(new byte[16])
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.empty());
    });

    LOG.error("", ex);
    Assertions.assertEquals(DISPLAY_NAME_ALREADY_USED, ex.errorCode());
  }

  /**
   * Two users cannot be created with the same ID.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountCreateIDUsed()
    throws Exception
  {
    final var queries = this.queries();

    final var id = UUID.randomUUID();
    queries.accountCreate(
      id,
      "User 0",
      AccountsDatabasePasswordHashDTO.builder()
        .setHash(new byte[16])
        .setParameters("params")
        .build(),
      "someone@example.com",
      Optional.empty());

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      queries.accountCreate(
        id,
        "User 1",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(new byte[16])
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.empty());
    });

    LOG.error("", ex);
    Assertions.assertEquals(ID_ALREADY_USED, ex.errorCode());
  }

  /**
   * An ID was the zero UUID.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountCreateIdZero()
    throws Exception
  {
    final var queries = this.queries();

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      queries.accountCreate(
        new UUID(0L, 0L),
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(new byte[16])
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.empty());
    });

    LOG.error("", ex);
    Assertions.assertEquals(INVALID_DATA, ex.errorCode());
  }

  /**
   * A display name was too long.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountCreateDisplayNameLong()
    throws Exception
  {
    final var queries = this.queries();

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(new byte[16])
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.empty());
    });

    LOG.error("", ex);
    Assertions.assertEquals(INVALID_DATA, ex.errorCode());
  }

  /**
   * An email was too long.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountCreateEmailLong()
    throws Exception
  {
    final var queries = this.queries();

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(new byte[16])
          .setParameters("params")
          .build(),
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        Optional.empty());
    });

    LOG.error("", ex);
    Assertions.assertEquals(INVALID_DATA, ex.errorCode());
  }

  /**
   * A password algorithm was too long.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountCreatePasswordAlgoLong()
    throws Exception
  {
    final var queries = this.queries();

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        AccountsDatabasePasswordHashDTO.builder()
          .setAlgorithm(
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
          .setHash(new byte[16])
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.empty());
    });

    LOG.error("", ex);
    Assertions.assertEquals(INVALID_DATA, ex.errorCode());
  }

  /**
   * A password hash was too long.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountCreatePasswordHashLong()
    throws Exception
  {
    final var queries = this.queries();

    final var hash = new byte[128];
    new SecureRandom().nextBytes(hash);

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(new byte[128])
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.empty());
    });

    LOG.error("", ex);
    Assertions.assertEquals(INVALID_DATA, ex.errorCode());
  }

  /**
   * Creating a user works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountCreate()
    throws Exception
  {
    final var queries = this.queries();

    final var id = UUID.randomUUID();
    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var account =
      queries.accountCreate(
        id,
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    Assertions.assertEquals(id, account.id());
    Assertions.assertEquals("User", account.displayName());
    Assertions.assertEquals("someone@example.com", account.email());
    Assertions.assertEquals("PBKDF2WithHmacSHA256", account.passwordHash().algorithm());
    Assertions.assertArrayEquals(passwordHash, account.passwordHash().hash());
    Assertions.assertEquals("params", account.passwordHash().parameters());
  }

  /**
   * Updating a user works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountUpdate()
    throws Exception
  {
    final var queries = this.queries();

    final var id = UUID.randomUUID();

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var account =
      queries.accountCreate(
        id,
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    Assertions.assertEquals(id, account.id());
    Assertions.assertEquals("User", account.displayName());
    Assertions.assertEquals("someone@example.com", account.email());
    Assertions.assertEquals("PBKDF2WithHmacSHA256", account.passwordHash().algorithm());
    Assertions.assertArrayEquals(passwordHash, account.passwordHash().hash());
    Assertions.assertEquals("params", account.passwordHash().parameters());

    final var passwordHash2 = new byte[16];
    new SecureRandom().nextBytes(passwordHash2);

    final var updatedAccount =
      AccountsDatabaseUserDTO.builder()
        .from(account)
        .setDisplayName("User 1")
        .setEmail("someone2@example.com")
        .setLocked(Optional.empty())
        .setPasswordHash(AccountsDatabasePasswordHashDTO.builder()
                           .setHash(passwordHash2)
                           .setParameters("params2")
                           .build())
        .build();

    final var resulting = queries.accountUpdate(account.id(), updatedAccount);
    Assertions.assertEquals(updatedAccount, resulting);
  }

  /**
   * Updating a user fails if the user does not exist.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountUpdateNonexistent()
    throws Exception
  {
    final var queries = this.queries();

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var account =
      AccountsDatabaseUserDTO.builder()
        .setId(UUID.randomUUID())
        .setDisplayName("User 1")
        .setEmail("someone2@example.com")
        .setLocked(Optional.empty())
        .setPasswordHash(AccountsDatabasePasswordHashDTO.builder()
                           .setHash(passwordHash)
                           .setParameters("params")
                           .build())
        .build();

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      queries.accountUpdate(account.id(), account);
    });

    LOG.error("", ex);
    Assertions.assertEquals(NONEXISTENT, ex.errorCode());
  }

  /**
   * Updating a user with invalid data fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountUpdateInvalid()
    throws Exception
  {
    final var queries = this.queries();

    final var id = UUID.randomUUID();

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var account =
      queries.accountCreate(
        id,
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var ex = Assertions.assertThrows(AccountsDatabaseException.class, () -> {
      final var account1 =
        AccountsDatabaseUserDTO.builder()
          .from(account)
          .setDisplayName(
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
          .build();

      queries.accountUpdate(account.id(), account1);
    });

    LOG.error("", ex);
    Assertions.assertEquals(INVALID_DATA, ex.errorCode());
  }

  /**
   * Creating and finding users works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountFind()
    throws Exception
  {
    final var queries = this.queries();

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);

    final var account0 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var account1 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 1",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var account2 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 2",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    try (final var accounts = queries.accountFind(
      Optional.empty(),
      Optional.empty(),
      Optional.empty())) {

      final var accountList = accounts.collect(Collectors.toList());
      Assertions.assertEquals(3, accountList.size());
      Assertions.assertEquals(account0, accountList.get(0));
      Assertions.assertEquals(account1, accountList.get(1));
      Assertions.assertEquals(account2, accountList.get(2));
    }
  }

  /**
   * Creating and finding users works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountFindUser1()
    throws Exception
  {
    final var queries = this.queries();

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);

    final var account0 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var account1 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 1",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var account2 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 2",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    try (final var accounts = queries.accountFind(
      Optional.empty(),
      Optional.of("User 1"),
      Optional.empty())) {

      final var accountList = accounts.collect(Collectors.toList());
      Assertions.assertEquals(1, accountList.size());
      Assertions.assertEquals(account1, accountList.get(0));
    }
  }

  /**
   * Creating and finding users works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountFindUser0_1()
    throws Exception
  {
    final var queries = this.queries();

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);

    final var account0 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var account1 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 1",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone1@example.com",
        Optional.of("Lock reason"));

    final var account2 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 2",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    try (final var accounts = queries.accountFind(
      Optional.empty(),
      Optional.empty(),
      Optional.of("someone@example.com"))) {

      final var accountList = accounts.collect(Collectors.toList());
      Assertions.assertEquals(2, accountList.size());
      Assertions.assertEquals(account0, accountList.get(0));
      Assertions.assertEquals(account2, accountList.get(1));
    }
  }

  /**
   * Creating and finding users works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountFindUserId2()
    throws Exception
  {
    final var queries = this.queries();

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);

    final var account0 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var account1 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 1",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone1@example.com",
        Optional.of("Lock reason"));

    final var id2 = UUID.randomUUID();
    final var account2 =
      queries.accountCreate(
        id2,
        "User 2",
        AccountsDatabasePasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    try (final var accounts = queries.accountFind(
      Optional.of(id2),
      Optional.empty(),
      Optional.empty())) {

      final var accountList = accounts.collect(Collectors.toList());
      Assertions.assertEquals(1, accountList.size());
      Assertions.assertEquals(account2, accountList.get(0));
    }
  }
}
