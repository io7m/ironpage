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


package com.io7m.ironpage.security.api;

import com.io7m.immutables.styles.ImmutablesStyleType;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jlexing.core.LexicalPositions;
import com.io7m.jlexing.core.LexicalType;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.net.URI;
import java.util.List;

/**
 * A security policy.
 */

@ImmutablesStyleType
@Value.Immutable
public interface SPolicyType extends LexicalType<URI>
{
  @Override
  @Value.Default
  @Value.Auxiliary
  default LexicalPosition<URI> lexical()
  {
    return LexicalPositions.zero();
  }

  /**
   * @return The policy comment
   */

  @Value.Default
  default String comment()
  {
    return "";
  }

  /**
   * @return The security policy version
   */

  BigInteger version();

  /**
   * @return The policy rules, in declaration order
   */

  List<SPolicyRule> rules();
}
