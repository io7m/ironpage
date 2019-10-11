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
import com.io7m.ironpage.database.core.api.CDException;
import com.io7m.ironpage.database.core.api.CDLabelsQueriesType;
import com.io7m.ironpage.database.core.api.CDSecurityLabelDTO;
import com.io7m.ironpage.database.spi.DatabaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.ironpage.database.core.api.CDLabelsQueriesType.LABEL_ALREADY_EXISTS;
import static com.io7m.ironpage.database.core.api.CDLabelsQueriesType.LABEL_NONEXISTENT;

@Tag("database")
public abstract class LabelsQueriesContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LabelsQueriesContract.class);

  protected abstract SettableClock clock();

  protected abstract Instant now();

  protected abstract DatabaseTransactionType transaction()
    throws DatabaseException;

  /**
   * Creating labels works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testLabelCreate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDLabelsQueriesType.class);

    final var label = queries.labelCreate("high_security_t", "High security data");
    Assertions.assertEquals(1L, label.id());
    Assertions.assertEquals("high_security_t", label.name());
    Assertions.assertEquals("High security data", label.description());

    final var labelGet = queries.labelGet(label.id());
    Assertions.assertEquals(Optional.of(label), labelGet);
  }

  /**
   * Creating duplicate labels fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testLabelCreateDuplicate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDLabelsQueriesType.class);

    final var label = queries.labelCreate("high_security_t", "High security data");

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.labelCreate("high_security_t", "Whatever");
    });
    Assertions.assertEquals(LABEL_ALREADY_EXISTS, ex.errorCode());
  }

  /**
   * Getting a nonexistent label fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testLabelGetNonexistent()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDLabelsQueriesType.class);

    final var labelGet = queries.labelGet(32767L);
    Assertions.assertTrue(labelGet.isEmpty());
  }

  /**
   * Updating a nonexistent label fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testLabelUpdateNonexistent()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDLabelsQueriesType.class);

    final var ex =
      Assertions.assertThrows(CDException.class, () -> {
        queries.labelUpdate(CDSecurityLabelDTO.builder()
                              .setId(32767L)
                              .setName("high_security_t")
                              .setDescription("High security data")
                              .build());
      });
    Assertions.assertEquals(LABEL_NONEXISTENT, ex.errorCode());
  }

  /**
   * Renaming a label to a name that conflicts, fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testLabelUpdateDuplicate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDLabelsQueriesType.class);

    final var label0 = queries.labelCreate("high_security_t0", "High security data");
    final var label1 = queries.labelCreate("high_security_t1", "High security data");

    final var ex =
      Assertions.assertThrows(CDException.class, () -> {
        queries.labelUpdate(label1.withName(label0.name()));
      });
    Assertions.assertEquals(LABEL_ALREADY_EXISTS, ex.errorCode());
  }

  /**
   * Updating labels works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testLabelUpdate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDLabelsQueriesType.class);

    final var label0 =
      queries.labelCreate("high_security_t0", "High security data");
    final var label1 =
      label0.withName("high_security_t1")
        .withDescription("High security data updated");

    final var labelGet = queries.labelUpdate(label1);
    Assertions.assertEquals(label1, labelGet);
  }

  /**
   * Listing labels works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testLabelList()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDLabelsQueriesType.class);

    final var label0 =
      queries.labelCreate("high_security_t0", "High security data 0");
    final var label1 =
      queries.labelCreate("high_security_t1", "High security data 1");
    final var label2 =
      queries.labelCreate("high_security_t2", "High security data 2");

    final var labels =
      queries.labelList()
        .collect(Collectors.toList());

    Assertions.assertEquals(List.of(label0, label1, label2), labels);
  }
}
