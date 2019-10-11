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
import com.io7m.ironpage.database.core.derby.CoreDatabasePartitionProviderDerby;
import com.io7m.ironpage.database.derby.DatabaseDerbyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Tag("database")
public final class DatabaseDerbySpecificTest
{
  private static final Logger LOG = LoggerFactory.getLogger(DatabaseDerbySpecificTest.class);

  private Path databasePath;

  @BeforeEach
  public void testSetup()
    throws IOException
  {
    this.databasePath = Files.createTempDirectory("ironpage-");
    Files.deleteIfExists(this.databasePath);
  }

  @AfterEach
  public void testTearDown()
    throws IOException
  {
    Files.walk(this.databasePath)
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .peek(file -> LOG.debug("delete {}", file))
      .forEach(File::delete);
  }

  @Test
  public void testOpenCore()
    throws Exception
  {
    final var registry = new MutablePartitionProviderRegistry();
    registry.add(new CoreDatabasePartitionProviderDerby());

    final var databases = new DatabaseDerbyProvider(registry);
    final var parameters =
      DatabaseParameters.builder()
        .setCreate(true)
        .setPath(this.databasePath.toString())
        .build();

    try (var database = databases.open(parameters)) {
      try (var connection = database.openConnection()) {

      }
    }
  }
}
