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

import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorErrorReceiverType;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorMessagesType;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorProviderType;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorRequest;
import com.io7m.ironpage.metadata.attribute.validator.api.MetaValidatorType;
import org.osgi.service.component.annotations.Component;

import java.util.Objects;

/**
 * The default provider of metadata validators.
 */

@Component(service = MetaValidatorProviderType.class)
public final class MetaValidators implements MetaValidatorProviderType
{
  /**
   * Construct a provider.
   */

  public MetaValidators()
  {

  }

  @Override
  public MetaValidatorType createValidator(
    final MetaValidatorErrorReceiverType errors,
    final MetaValidatorMessagesType messages,
    final MetaValidatorRequest request)
  {
    return new MetaValidator(
      Objects.requireNonNull(errors, "errors"),
      Objects.requireNonNull(messages, "messages"),
      Objects.requireNonNull(request, "request"));
  }
}
