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

import com.io7m.ironpage.security.api.SPolicy;
import com.io7m.ironpage.security.api.SPolicyParserType;
import com.io7m.ironpage.security.api.SPolicySerializerType;
import com.io7m.ironpage.security.vanilla.SPolicyParsers;
import com.io7m.ironpage.security.vanilla.SPolicySerializers;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public final class SPolicyParsersTest extends SPolicyParsersContract
{
  @Override
  protected SPolicyParserType parser(
    final InputStream stream)
  {
    return new SPolicyParsers().create(super::showError, URI.create("urn:test"), stream);
  }

  @Override
  protected SPolicySerializerType serializer(
    final SPolicy policy,
    final OutputStream outputStream)
  {
    return new SPolicySerializers().create(policy, outputStream);
  }
}
