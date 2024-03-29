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


package com.io7m.ironpage.metadata.attribute.validator.vanilla;

import com.io7m.ironpage.l10n.api.L10NStringsAbstract;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorMessagesType;

import java.util.Locale;

/**
 * A provider of validator messages.
 */

public final class MetaValidatorMessages
  extends L10NStringsAbstract implements MetaValidatorMessagesType
{
  /**
   * Construct a message provider.
   *
   * @param locale The locale for messages
   */

  public MetaValidatorMessages(
    final Locale locale)
  {
    super("com.io7m.ironpage.metadata.attribute.validator.vanilla.Messages", locale);
  }
}
