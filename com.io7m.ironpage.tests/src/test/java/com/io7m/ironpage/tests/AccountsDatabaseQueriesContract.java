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

import com.io7m.ironpage.database.api.DatabaseTransactionType;
import com.io7m.ironpage.database.audit.api.AuditDatabaseQueriesType;
import com.io7m.ironpage.database.core.api.CDAccountsQueriesType;
import com.io7m.ironpage.database.core.api.CDException;
import com.io7m.ironpage.database.core.api.CDPasswordHashDTO;
import com.io7m.ironpage.database.core.api.CDRolesQueriesType;
import com.io7m.ironpage.database.core.api.CDSecurityRoleDTO;
import com.io7m.ironpage.database.core.api.CDUserDTO;
import com.io7m.ironpage.database.spi.DatabaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.io7m.ironpage.database.core.api.CDAccountsQueriesType.DISPLAY_NAME_ALREADY_USED;
import static com.io7m.ironpage.database.core.api.CDAccountsQueriesType.ID_ALREADY_USED;
import static com.io7m.ironpage.database.core.api.CDAccountsQueriesType.INVALID_DATA;
import static com.io7m.ironpage.database.core.api.CDAccountsQueriesType.NONEXISTENT;
import static com.io7m.ironpage.database.core.api.CDRolesQueriesType.ROLE_NONEXISTENT;
import static java.time.temporal.ChronoUnit.SECONDS;

@Tag("database")
public abstract class AccountsDatabaseQueriesContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AccountsDatabaseQueriesContract.class);

  protected abstract SettableClock clock();

  protected abstract Instant now();

  protected abstract DatabaseTransactionType transaction()
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    queries.accountCreate(
      UUID.randomUUID(),
      "User",
      CDPasswordHashDTO.builder()
        .setHash(new byte[16])
        .setParameters("params")
        .build(),
      "someone@example.com",
      Optional.empty());

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "User",
        CDPasswordHashDTO.builder()
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var id = UUID.randomUUID();
    queries.accountCreate(
      id,
      "User 0",
      CDPasswordHashDTO.builder()
        .setHash(new byte[16])
        .setParameters("params")
        .build(),
      "someone@example.com",
      Optional.empty());

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountCreate(
        id,
        "User 1",
        CDPasswordHashDTO.builder()
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountCreate(
        new UUID(0L, 0L),
        "User",
        CDPasswordHashDTO.builder()
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        CDPasswordHashDTO.builder()
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        CDPasswordHashDTO.builder()
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        CDPasswordHashDTO.builder()
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var hash = new byte[128];
    new SecureRandom().nextBytes(hash);

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        CDPasswordHashDTO.builder()
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
    final var transaction = this.transaction();

    final var queries =
      transaction.queries(CDAccountsQueriesType.class);

    final var id = UUID.randomUUID();
    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var account =
      queries.accountCreate(
        id,
        "User",
        CDPasswordHashDTO.builder()
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
    final var transaction = this.transaction();

    final var roleQueries =
      transaction.queries(CDRolesQueriesType.class);
    final var queries =
      transaction.queries(CDAccountsQueriesType.class);

    final var role0 = roleQueries.roleCreate("role0", "Role 0");
    final var role1 = roleQueries.roleCreate("role1", "Role 1");
    final var role2 = roleQueries.roleCreate("role2", "Role 2");

    final var id = UUID.randomUUID();
    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var account =
      queries.accountCreate(
        id,
        "User",
        CDPasswordHashDTO.builder()
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
      CDUserDTO.builder()
        .from(account)
        .setDisplayName("User 1")
        .setEmail("someone2@example.com")
        .setLocked(Optional.empty())
        .setRoles(new TreeSet<>(List.of(role0, role1, role2)))
        .setPasswordHash(CDPasswordHashDTO.builder()
                           .setHash(passwordHash2)
                           .setParameters("params2")
                           .build())
        .build();

    final var resulting = queries.accountUpdate(account.id(), updatedAccount);
    Assertions.assertEquals(updatedAccount, resulting);

    final var updatedAccountAfter = queries.accountGet(account.id());
    Assertions.assertEquals(updatedAccount, updatedAccountAfter);

    final var auditQueries = transaction.queries(AuditDatabaseQueriesType.class);
    try (var stream = auditQueries.auditEventsDuring(this.now(), this.clock().instant())) {
      final var events = stream.collect(Collectors.toList());
      Assertions.assertEquals(6, events.size());

      {
        final var event = events.remove(0);
        Assertions.assertEquals("USER_CREATED", event.eventType());
      }

      {
        final var event = events.remove(0);
        Assertions.assertEquals("USER_MODIFIED_DISPLAY_NAME", event.eventType());
      }

      {
        final var event = events.remove(0);
        Assertions.assertEquals("USER_MODIFIED_EMAIL", event.eventType());
      }

      {
        final var event = events.remove(0);
        Assertions.assertEquals("USER_MODIFIED_PASSWORD", event.eventType());
      }

      {
        final var event = events.remove(0);
        Assertions.assertEquals("USER_MODIFIED_LOCKED", event.eventType());
      }

      {
        final var event = events.remove(0);
        Assertions.assertEquals("USER_MODIFIED_ROLES", event.eventType());
      }
    }
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var account =
      CDUserDTO.builder()
        .setId(UUID.randomUUID())
        .setDisplayName("User 1")
        .setEmail("someone2@example.com")
        .setLocked(Optional.empty())
        .setPasswordHash(CDPasswordHashDTO.builder()
                           .setHash(passwordHash)
                           .setParameters("params")
                           .build())
        .build();

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountUpdate(account.id(), account);
    });

    LOG.error("", ex);
    Assertions.assertEquals(NONEXISTENT, ex.errorCode());
  }

  /**
   * Updating a user fails if a given role does not exist.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountUpdateNonexistentRole()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var role =
      CDSecurityRoleDTO.builder()
        .setDescription("Role")
        .setName("role")
        .setId(32767L)
        .build();

    final var account =
      CDUserDTO.builder()
        .setId(UUID.randomUUID())
        .setDisplayName("User 1")
        .setEmail("someone2@example.com")
        .setLocked(Optional.empty())
        .setPasswordHash(
          CDPasswordHashDTO.builder()
            .setHash(passwordHash)
            .setParameters("params")
            .build())
        .build();

    queries.accountCreate(
      account.id(),
      account.displayName(),
      account.passwordHash(),
      account.email(),
      account.locked());

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.accountUpdate(
        account.id(),
        account.withRoles(new TreeSet<>(List.of(role))));
    });

    LOG.error("", ex);
    Assertions.assertEquals(ROLE_NONEXISTENT, ex.errorCode());
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
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var id = UUID.randomUUID();

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);
    final var passwordSalt = new byte[16];
    new SecureRandom().nextBytes(passwordSalt);

    final var account =
      queries.accountCreate(
        id,
        "User",
        CDPasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      final var account1 =
        CDUserDTO.builder()
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
   * Creating, updating, and deleting sessions works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountSessionUse()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);

    final var account0 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        CDPasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var session0 =
      queries.accountSessionCreate(account0.id(), "a");
    final var session1 =
      queries.accountSessionCreate(account0.id(), "b");
    final var session2 =
      queries.accountSessionCreate(account0.id(), "c");

    Assertions.assertEquals(account0.id(), session0.userID());
    Assertions.assertEquals("a", session0.id());
    Assertions.assertEquals(this.now().plus(2L, SECONDS), session0.updated());

    Assertions.assertEquals(account0.id(), session1.userID());
    Assertions.assertEquals("b", session1.id());
    Assertions.assertEquals(this.now().plus(4L, SECONDS), session1.updated());

    Assertions.assertEquals(account0.id(), session2.userID());
    Assertions.assertEquals("c", session2.id());
    Assertions.assertEquals(this.now().plus(6L, SECONDS), session2.updated());

    queries.accountSessionUpdate(session0.id());
    queries.accountSessionUpdate(session1.id());
    queries.accountSessionUpdate(session2.id());

    queries.accountSessionDelete(session0.id());
    queries.accountSessionDelete(session1.id());
    queries.accountSessionDelete(session2.id());

    final var ex0 = Assertions.assertThrows(
      CDException.class, () -> queries.accountSessionUpdate(session0.id()));
    Assertions.assertEquals(NONEXISTENT, ex0.errorCode());
    final var ex1 = Assertions.assertThrows(
      CDException.class, () -> queries.accountSessionUpdate(session1.id()));
    Assertions.assertEquals(NONEXISTENT, ex1.errorCode());
    final var ex2 = Assertions.assertThrows(
      CDException.class, () -> queries.accountSessionUpdate(session2.id()));
    Assertions.assertEquals(NONEXISTENT, ex2.errorCode());
  }

  /**
   * Creating a session for a nonexistent user fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountSessionNonexistentUser()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var ex0 = Assertions.assertThrows(
      CDException.class,
      () -> queries.accountSessionCreate(UUID.randomUUID(), "a"));
    Assertions.assertEquals(NONEXISTENT, ex0.errorCode());
  }

  /**
   * Deleting all sessions for a user works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountSessionDeleteForUser()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);

    final var account0 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        CDPasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var session0 =
      queries.accountSessionCreate(account0.id(), "a");
    final var session1 =
      queries.accountSessionCreate(account0.id(), "b");
    final var session2 =
      queries.accountSessionCreate(account0.id(), "c");

    queries.accountSessionDeleteForUser(account0.id());

    final var ex0 = Assertions.assertThrows(
      CDException.class, () -> queries.accountSessionUpdate(session0.id()));
    Assertions.assertEquals(NONEXISTENT, ex0.errorCode());
    final var ex1 = Assertions.assertThrows(
      CDException.class, () -> queries.accountSessionUpdate(session1.id()));
    Assertions.assertEquals(NONEXISTENT, ex1.errorCode());
    final var ex2 = Assertions.assertThrows(
      CDException.class, () -> queries.accountSessionUpdate(session2.id()));
    Assertions.assertEquals(NONEXISTENT, ex2.errorCode());
  }

  /**
   * Creating a duplicate session fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testAccountSessionCreateDuplicate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDAccountsQueriesType.class);

    final var passwordHash = new byte[16];
    new SecureRandom().nextBytes(passwordHash);

    final var account0 =
      queries.accountCreate(
        UUID.randomUUID(),
        "User 0",
        CDPasswordHashDTO.builder()
          .setHash(passwordHash)
          .setParameters("params")
          .build(),
        "someone@example.com",
        Optional.of("Lock reason"));

    final var session0 =
      queries.accountSessionCreate(account0.id(), "a");

    final var ex0 = Assertions.assertThrows(
      CDException.class,
      () -> queries.accountSessionCreate(session0.userID(), "a"));
    Assertions.assertEquals(ID_ALREADY_USED, ex0.errorCode());
  }
}
