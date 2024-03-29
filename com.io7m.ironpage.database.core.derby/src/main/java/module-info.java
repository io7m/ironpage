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

/**
 * Document database (Core Derby implementation)
 */

module com.io7m.ironpage.database.core.derby
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.service.component.annotations;

  requires com.io7m.jaffirm.core;
  requires io.reactivex.rxjava3;
  requires org.apache.commons.codec;
  requires org.apache.derby.commons;
  requires org.jooq;
  requires org.slf4j;

  requires transitive com.io7m.ironpage.database.audit.api;
  requires transitive com.io7m.ironpage.database.core.api;
  requires transitive com.io7m.ironpage.database.pages.api;
  requires transitive com.io7m.ironpage.database.spi;

  provides com.io7m.ironpage.database.spi.DatabasePartitionProviderType
    with com.io7m.ironpage.database.core.derby.CoreDatabasePartitionProviderDerby;
}
