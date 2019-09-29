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

import com.io7m.ironpage.database.accounts.api.AccountsDatabasePasswordHashDTO;
import com.io7m.ironpage.database.accounts.api.AccountsDatabaseQueriesType;
import com.io7m.ironpage.database.api.DatabaseTransactionType;
import com.io7m.ironpage.database.pages.api.PagesDatabaseException;
import com.io7m.ironpage.database.pages.api.PagesDatabaseQueriesType;
import com.io7m.ironpage.database.pages.api.PagesDatabaseRedactionDTO;
import com.io7m.ironpage.database.spi.DatabaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public abstract class PagesDatabaseQueriesContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(PagesDatabaseQueriesContract.class);

  protected abstract Instant now();

  protected abstract DatabaseTransactionType transaction()
    throws DatabaseException;

  /**
   * Putting a blob works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testPagesBlobPut()
    throws Exception
  {
    final var transaction = this.transaction();

    final var accountsQueries =
      transaction.queries(AccountsDatabaseQueriesType.class);

    final var account =
      accountsQueries.accountCreate(
        UUID.randomUUID(),
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setSalt((byte) 0x0)
          .setHash((byte) 0x0)
          .build(),
        "someone@example.com",
        Optional.empty());

    final var queries =
      transaction.queries(PagesDatabaseQueriesType.class);

    final var data = "hello".getBytes(StandardCharsets.UTF_8);
    final var hash = queries.pageBlobPut(account.id(), "text/plain", data);

    Assertions.assertEquals(
      "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
      hash);

    final var blobOpt = queries.pageBlobGet(hash);
    Assertions.assertTrue(blobOpt.isPresent());
    final var blob = blobOpt.get();
    Assertions.assertEquals(account.id(), blob.owner());
    Assertions.assertArrayEquals(data, blob.data());
    Assertions.assertEquals("text/plain", blob.mediaType());
    Assertions.assertEquals(hash, blob.id());
    Assertions.assertEquals(Optional.empty(), blob.redaction());
  }

  /**
   * Getting a nonexistent blob returns nothing.
   *
   * @throws Exception If required
   */

  @Test
  public final void testPagesBlobGetNonexistent()
    throws Exception
  {
    final var transaction = this.transaction();

    final var queries =
      transaction.queries(PagesDatabaseQueriesType.class);

    final var blobOpt = queries.pageBlobGet("nonexistent");
    Assertions.assertTrue(blobOpt.isEmpty());
  }

  /**
   * Putting a blob that already exists, fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testPagesBlobPutAlreadyExists()
    throws Exception
  {
    final var transaction = this.transaction();

    final var accountsQueries =
      transaction.queries(AccountsDatabaseQueriesType.class);

    final var account =
      accountsQueries.accountCreate(
        UUID.randomUUID(),
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setSalt((byte) 0x0)
          .setHash((byte) 0x0)
          .build(),
        "someone@example.com",
        Optional.empty());

    final var queries =
      transaction.queries(PagesDatabaseQueriesType.class);

    final var hash =
      queries.pageBlobPut(account.id(), "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

    Assertions.assertEquals(
      "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
      hash);

    final var ex = Assertions.assertThrows(PagesDatabaseException.class, () -> {
      queries.pageBlobPut(account.id(), "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
    });

    Assertions.assertEquals(PagesDatabaseQueriesType.DATA_ALREADY_EXISTS, ex.errorCode());
  }

  /**
   * Putting a blob with a nonexistent owner fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testPagesBlobPutNoOwner()
    throws Exception
  {
    final var transaction = this.transaction();

    final var queries =
      transaction.queries(PagesDatabaseQueriesType.class);

    final var ex = Assertions.assertThrows(PagesDatabaseException.class, () -> {
      queries.pageBlobPut(
        UUID.randomUUID(),
        "text/plain",
        "hello".getBytes(StandardCharsets.UTF_8));
    });

    Assertions.assertEquals(PagesDatabaseQueriesType.DATA_OWNER_NONEXISTENT, ex.errorCode());
  }

  /**
   * Putting a blob that is too large fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testPagesBlobPutTooLarge()
    throws Exception
  {
    final var transaction = this.transaction();

    final var accountsQueries =
      transaction.queries(AccountsDatabaseQueriesType.class);

    final var account =
      accountsQueries.accountCreate(
        UUID.randomUUID(),
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setSalt((byte) 0x0)
          .setHash((byte) 0x0)
          .build(),
        "someone@example.com",
        Optional.empty());

    final var queries =
      transaction.queries(PagesDatabaseQueriesType.class);

    final var data = new byte[1_000];
    new SecureRandom().nextBytes(data);

    try (var output = new ByteArrayOutputStream()) {
      for (var index = 0; index < 10_000; ++index) {
        output.write(data);
      }
      final var ex = Assertions.assertThrows(PagesDatabaseException.class, () -> {
        queries.pageBlobPut(account.id(), "text/plain", output.toByteArray());
      });

      Assertions.assertEquals(PagesDatabaseQueriesType.DATA_INVALID, ex.errorCode());
    }
  }

  /**
   * Redacting a blob works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testPagesBlobRedaction()
    throws Exception
  {
    final var transaction = this.transaction();

    final var accountsQueries =
      transaction.queries(AccountsDatabaseQueriesType.class);

    final var account =
      accountsQueries.accountCreate(
        UUID.randomUUID(),
        "User",
        AccountsDatabasePasswordHashDTO.builder()
          .setSalt((byte) 0x0)
          .setHash((byte) 0x0)
          .build(),
        "someone@example.com",
        Optional.empty());

    final var queries = transaction.queries(PagesDatabaseQueriesType.class);
    final var data = "hello".getBytes(StandardCharsets.UTF_8);
    final var hash = queries.pageBlobPut(account.id(), "text/plain", data);

    Assertions.assertEquals(
      "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
      hash);

    queries.pageBlobRedact(
      account.id(),
      "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
      "Redacted for testing");

    final var blobRedaction =
      PagesDatabaseRedactionDTO.builder()
        .setId(1L)
        .setOwner(account.id())
        .setReason("Redacted for testing")
        .setTime(this.now())
        .build();

    final var blobOpt = queries.pageBlobGet(hash);
    Assertions.assertTrue(blobOpt.isPresent());
    final var blob = blobOpt.get();
    Assertions.assertEquals(account.id(), blob.owner());
    Assertions.assertArrayEquals(new byte[0], blob.data());
    Assertions.assertEquals("text/plain", blob.mediaType());
    Assertions.assertEquals(hash, blob.id());
    Assertions.assertEquals(Optional.of(blobRedaction), blob.redaction());
  }
}
