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


package com.io7m.ironpage.security.vanilla;

import com.io7m.ironpage.security.api.SPolicyParserErrorReceiverType;
import com.io7m.ironpage.security.api.SPolicyParserProviderType;
import com.io7m.ironpage.security.api.SPolicyParserType;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/**
 * A provider of security policy parsers.
 */

public final class SPolicyParsers implements SPolicyParserProviderType
{
  /**
   * Construct a provider.
   */

  public SPolicyParsers()
  {

  }

  @Override
  public SPolicyParserType create(
    final SPolicyParserErrorReceiverType errors,
    final URI uri,
    final InputStream stream)
  {
    Objects.requireNonNull(errors, "errors");
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    return new SPolicyParser(errors, uri, stream);
  }
}
