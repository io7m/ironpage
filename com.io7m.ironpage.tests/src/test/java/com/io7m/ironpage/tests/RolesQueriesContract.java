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
import com.io7m.ironpage.database.core.api.CDRolesQueriesType;
import com.io7m.ironpage.database.core.api.CDSecurityRoleDTO;
import com.io7m.ironpage.database.spi.DatabaseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.ironpage.database.core.api.CDRolesQueriesType.ROLE_ALREADY_EXISTS;
import static com.io7m.ironpage.database.core.api.CDRolesQueriesType.ROLE_NONEXISTENT;

public abstract class RolesQueriesContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RolesQueriesContract.class);

  protected abstract SettableClock clock();

  protected abstract Instant now();

  protected abstract DatabaseTransactionType transaction()
    throws DatabaseException;

  /**
   * Creating roles works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testRoleCreate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDRolesQueriesType.class);

    final var role = queries.roleCreate("admin", "Administrators");
    Assertions.assertEquals(1L, role.id());
    Assertions.assertEquals("admin", role.name());
    Assertions.assertEquals("Administrators", role.description());

    final var roleGet = queries.roleGet(role.id());
    Assertions.assertEquals(Optional.of(role), roleGet);
  }

  /**
   * Creating duplicate roles fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testRoleCreateDuplicate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDRolesQueriesType.class);

    final var role = queries.roleCreate("admin", "Administrators");

    final var ex = Assertions.assertThrows(CDException.class, () -> {
      queries.roleCreate("admin", "Whatever");
    });
    Assertions.assertEquals(ROLE_ALREADY_EXISTS, ex.errorCode());
  }

  /**
   * Getting a nonexistent role fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testRoleGetNonexistent()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDRolesQueriesType.class);

    final var roleGet = queries.roleGet(32767L);
    Assertions.assertTrue(roleGet.isEmpty());
  }

  /**
   * Updating a nonexistent role fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testRoleUpdateNonexistent()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDRolesQueriesType.class);

    final var ex =
      Assertions.assertThrows(CDException.class, () -> {
        queries.roleUpdate(CDSecurityRoleDTO.builder()
                             .setId(32767L)
                             .setName("admin")
                             .setDescription("Administrators")
                             .build());
      });
    Assertions.assertEquals(ROLE_NONEXISTENT, ex.errorCode());
  }

  /**
   * Renaming a role to a name that conflicts, fails.
   *
   * @throws Exception If required
   */

  @Test
  public final void testRoleUpdateDuplicate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDRolesQueriesType.class);

    final var role0 = queries.roleCreate("admin0", "Administrators");
    final var role1 = queries.roleCreate("admin1", "Administrators");

    final var ex =
      Assertions.assertThrows(CDException.class, () -> {
        queries.roleUpdate(role1.withName(role0.name()));
      });
    Assertions.assertEquals(ROLE_ALREADY_EXISTS, ex.errorCode());
  }

  /**
   * Updating roles works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testRoleUpdate()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDRolesQueriesType.class);

    final var role0 =
      queries.roleCreate("admin0", "Administrators");
    final var role1 =
      role0.withName("admin1")
        .withDescription( "Administrators updated");

    final var roleGet = queries.roleUpdate(role1);
    Assertions.assertEquals(role1, roleGet);
  }

  /**
   * Listing roles works.
   *
   * @throws Exception If required
   */

  @Test
  public final void testRoleList()
    throws Exception
  {
    final var transaction = this.transaction();
    final var queries = transaction.queries(CDRolesQueriesType.class);

    final var role0 =
      queries.roleCreate("admin0", "Administrators 0");
    final var role1 =
      queries.roleCreate("admin1", "Administrators 1");
    final var role2 =
      queries.roleCreate("admin2", "Administrators 2");

    final var roles =
      queries.roleList()
        .collect(Collectors.toList());

    Assertions.assertEquals(List.of(role0, role1, role2), roles);
  }
}
