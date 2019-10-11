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

import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerErrorConsumerType;
import com.io7m.ironpage.metadata.schema.compiler.api.MSchemaCompilerMessagesType;
import com.io7m.ironpage.metadata.schema.compiler.binder.api.MSchemaCompilerBinderType;
import com.io7m.ironpage.metadata.schema.compiler.loader.api.MSchemaCompilerLoaderType;
import com.io7m.ironpage.metadata.schema.compiler.spi.MSchemaCompilerSourceType;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.MSCVCompilers;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.MSCVMessagesProvider;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.binder.MSCVBinders;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.loader.MSCVLoaders;
import com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.MSCVParsers;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

@Tag("schemaCompiler")
public final class MSCVBindersTest extends MSchemaCompilerBinderContract
{
  private static final Logger LOG = LoggerFactory.getLogger(MSCVBindersTest.class);

  @Override
  protected MSchemaCompilerLoaderType loader(
    final MSchemaCompilerSourceType source)
  {
    return new MSCVLoaders()
      .createLoader(
        new MSCVCompilers(),
        error -> LOG.error("error: {}", error),
        new MSCVMessagesProvider().createStrings(Locale.getDefault()),
        source);
  }

  @Override
  protected MSchemaCompilerBinderType createBinder(
    final MSchemaCompilerErrorConsumerType errors,
    final MSchemaCompilerMessagesType messages,
    final MSchemaCompilerLoaderType loader,
    final URI uri,
    final InputStream stream)
    throws Exception
  {
    try (var parser = new MSCVParsers().createParser(errors, messages, uri, stream)) {
      return new MSCVBinders().createBinder(errors, loader, messages, uri, parser.execute().get());
    }
  }
}
