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

import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.database.spi.DatabaseSchemaRevisionXML;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public final class DatabaseSchemaRevisionXMLTest
{
  @Captor
  ArgumentCaptor<String> captor0;
  @Captor
  ArgumentCaptor<String> captor1;
  @Captor
  ArgumentCaptor<String> captor2;

  private static InputStream resource(final String name)
  {
    return DatabaseSchemaRevisionXMLTest.class.getResourceAsStream(
      "/com/io7m/ironpage/tests/" + name);
  }

  @Test
  public void testInvalidXML0()
  {
    final var ex =
      Assertions.assertThrows(DatabaseException.class, () -> {
        DatabaseSchemaRevisionXML.fromStream(
          Optional.empty(),
          BigInteger.ZERO,
          URI.create("urn:invalid"),
          resource("invalid-0.xml"));
      });

    Assertions.assertTrue(ex.getMessage().contains("Premature end of file"));
  }

  @Test
  public void testInvalidXML1()
  {
    final var ex =
      Assertions.assertThrows(DatabaseException.class, () -> {
        DatabaseSchemaRevisionXML.fromStream(
          Optional.empty(),
          BigInteger.ZERO,
          URI.create("urn:invalid"),
          resource("invalid-1.xml"));
      });

    Assertions.assertTrue(ex.getMessage().contains("not-statements"));
  }

  @Test
  public void testValidXML0()
    throws Exception
  {
    final var connection = Mockito.mock(Connection.class);
    final var statement = Mockito.mock(PreparedStatement.class);

    final var revision =
      DatabaseSchemaRevisionXML.fromStream(
        Optional.empty(),
        BigInteger.ZERO,
        URI.create("urn:valid"),
        resource("valid-0.xml"));

    Assertions.assertEquals(Optional.empty(), revision.schemaPrevious());
    Assertions.assertEquals(BigInteger.ZERO, revision.schemaCurrent());

    Mockito.when(connection.prepareStatement("STATEMENT 0")).thenReturn(statement);
    Mockito.when(connection.prepareStatement("STATEMENT 1")).thenReturn(statement);
    Mockito.when(connection.prepareStatement("STATEMENT 2")).thenReturn(statement);

    revision.schemaMigrate(connection);
  }

  @Test
  public void testUpgradeCrashes()
    throws Exception
  {
    final var connection = Mockito.mock(Connection.class);
    final var statement = Mockito.mock(PreparedStatement.class);

    final var revision =
      DatabaseSchemaRevisionXML.fromStream(
        Optional.empty(),
        BigInteger.ZERO,
        URI.create("urn:invalid"),
        resource("valid-0.xml"));

    Assertions.assertEquals(Optional.empty(), revision.schemaPrevious());
    Assertions.assertEquals(BigInteger.ZERO, revision.schemaCurrent());

    Mockito.when(connection.prepareStatement("STATEMENT 0")).thenReturn(statement);
    Mockito.when(connection.prepareStatement("STATEMENT 1")).thenReturn(statement);
    Mockito.when(connection.prepareStatement("STATEMENT 2")).thenThrow(new SQLException("CRASH!"));

    final var ex = Assertions.assertThrows(DatabaseException.class, () -> {
      revision.schemaMigrate(connection);
    });

    Assertions.assertEquals("CRASH!", ex.message());
  }
}
