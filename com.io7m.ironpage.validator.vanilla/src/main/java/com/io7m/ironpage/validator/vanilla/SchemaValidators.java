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

package com.io7m.ironpage.validator.vanilla;

import com.io7m.ironpage.validator.api.SchemaValidatorProviderType;
import com.io7m.ironpage.validator.api.SchemaValidatorType;
import org.osgi.service.component.annotations.Component;

import java.util.Locale;

/**
 * The default provider of schema validators.
 */

@Component(service = SchemaValidatorProviderType.class)
public final class SchemaValidators implements SchemaValidatorProviderType
{
  /**
   * Construct a schema validator provider.
   */

  public SchemaValidators()
  {

  }

  @Override
  public SchemaValidatorType createForLocale(final Locale locale)
  {
    return new SchemaValidator(locale);
  }
}
