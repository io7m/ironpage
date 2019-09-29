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

import com.io7m.ironpage.database.api.DatabaseConnectionType;
import com.io7m.ironpage.database.api.DatabaseParameters;
import com.io7m.ironpage.database.api.DatabaseTransactionType;
import com.io7m.ironpage.database.api.DatabaseType;
import com.io7m.ironpage.database.core.derby.CoreDatabasePartitionProviderDerby;
import com.io7m.ironpage.database.derby.DatabaseDerbyProvider;
import com.io7m.ironpage.database.spi.DatabaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class AccountsDatabaseQueriesDerbyTest extends AccountsDatabaseQueriesContract
{
  private static final Instant NOW = Instant.parse("2000-01-01T00:00:00Z");

  private Path databasePath;
  private DatabaseType database;
  private DatabaseConnectionType connection;
  private SettableClock clock =
    new SettableClock(ZoneId.of("UTC"), NOW, (c, instant) -> {
      c.setTime(instant.plus(1L, ChronoUnit.SECONDS));
    });

  @BeforeEach
  public void testSetupDatabase()
    throws IOException, DatabaseException
  {
    this.databasePath = Files.createTempDirectory("ironpage-");
    Files.deleteIfExists(this.databasePath);

    final var registry = new MutablePartitionProviderRegistry();
    registry.add(new CoreDatabasePartitionProviderDerby(this.clock));

    final var databases = new DatabaseDerbyProvider(registry);
    final var parameters =
      DatabaseParameters.builder()
        .setCreate(true)
        .setPath(this.databasePath.toString())
        .build();

    this.database = databases.open(parameters);
    this.connection = this.database.openConnection();
  }

  @AfterEach
  public void testTearDownDatabase()
    throws DatabaseException, IOException
  {
    this.connection.close();
    this.database.close();
  }

  @Override
  protected SettableClock clock()
  {
    return this.clock;
  }

  @Override
  protected Instant now()
  {
    return NOW;
  }

  @Override
  protected DatabaseTransactionType transaction()
    throws DatabaseException
  {
    return this.connection.beginTransaction();
  }
}
