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

import com.io7m.ironpage.security.api.SLabel;
import com.io7m.ironpage.security.api.SPermission;
import com.io7m.ironpage.security.api.SPolicy;
import com.io7m.ironpage.security.api.SPolicyEvaluatorProviderType;
import com.io7m.ironpage.security.api.SPolicyEvaluatorType;
import com.io7m.ironpage.security.api.SRole;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The default policy evaluator implementation.
 */

public final class SPolicyEvaluators implements SPolicyEvaluatorProviderType
{
  private final AtomicLong count;

  /**
   * Construct a policy evaluator.
   */

  public SPolicyEvaluators()
  {
    this.count = new AtomicLong(0L);
  }

  @Override
  public SPolicyEvaluatorType create(
    final SPolicy policy,
    final SRole role,
    final SPermission permission,
    final SLabel label)
  {
    Objects.requireNonNull(policy, "policy");
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(permission, "permission");
    Objects.requireNonNull(label, "label");

    return new SPolicyEvaluator(this.count.getAndIncrement(), policy, role, permission, label);
  }
}
