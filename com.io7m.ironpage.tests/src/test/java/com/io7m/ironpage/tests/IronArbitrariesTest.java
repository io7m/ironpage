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

import com.io7m.ironpage.database.api.DatabaseParameters;
import com.io7m.ironpage.database.audit.api.AuditDatabaseEventDTO;
import com.io7m.ironpage.database.core.api.CDErrorCode;
import com.io7m.ironpage.database.core.api.CDPasswordHashDTO;
import com.io7m.ironpage.database.core.api.CDSecurityLabelDTO;
import com.io7m.ironpage.database.core.api.CDSecurityRoleDTO;
import com.io7m.ironpage.database.core.api.CDSessionDTO;
import com.io7m.ironpage.database.core.api.CDUserDTO;
import com.io7m.ironpage.database.pages.api.PagesDatabaseBlobDTO;
import com.io7m.ironpage.database.pages.api.PagesDatabaseRedactionDTO;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class IronArbitrariesTest
{
  private static final Logger LOG = LoggerFactory.getLogger(IronArbitrariesTest.class);

  @Property
  public void testPaths(@ForAll final Path x)
  {
    LOG.debug("{}", x);
  }

  @Property
  public void testDatabaseParameters(@ForAll final DatabaseParameters x)
  {
    LOG.debug("{}", x);
  }

  @Property
  public void testAuditDatabaseEventDTO(@ForAll final AuditDatabaseEventDTO x)
  {
    LOG.debug("{}", x);
  }

  @Property
  public void testCDSecurityLabelDTO(@ForAll final CDSecurityLabelDTO x)
  {
    LOG.debug("{}", x);
    Assertions.assertEquals(0, x.compareTo(x));
  }

  @Property
  public void testCDSecurityRoleDTO(@ForAll final CDSecurityRoleDTO x)
  {
    LOG.debug("{}", x);
    Assertions.assertEquals(0, x.compareTo(x));
  }

  @Property
  public void testCDSessionDTO(@ForAll final CDSessionDTO x)
  {
    LOG.debug("{}", x);
  }

  @Property
  public void testCDErrorCode(@ForAll final CDErrorCode x)
  {
    LOG.debug("{}", x);
  }

  @Property
  public void testCDPasswordHash(@ForAll final CDPasswordHashDTO x)
  {
    LOG.debug("{}", x);
  }

  @Property
  public void testCDUser(@ForAll final CDUserDTO x)
  {
    LOG.debug("{}", x);
  }

  @Property
  public void testPagesDatabaseRedactionDTO(@ForAll final PagesDatabaseRedactionDTO x)
  {
    LOG.debug("{}", x);
  }

  @Property
  public void testPagesDatabaseBlobDTO(@ForAll final PagesDatabaseBlobDTO x)
  {
    LOG.debug("{}", x);
  }
}
