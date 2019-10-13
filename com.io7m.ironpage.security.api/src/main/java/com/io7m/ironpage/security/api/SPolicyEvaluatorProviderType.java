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

/**
 * A provider of policy evaluators.
 *
 * A <i>policy evaluator</i> takes a security policy as input, and a tuple {@code (role, permission,
 * label)}, and determines whether or not the policy permits the subject {@code role} to perform
 * {@code permission} on subject {@code label}.
 */

public interface SPolicyEvaluatorProviderType
{
  /**
   * Create a policy evaluator.
   *
   * @param policy     The policy
   * @param role       The input role
   * @param permission The input permission
   * @param label      The input label
   *
   * @return A new policy evaluator
   */

  SPolicyEvaluatorType create(
    SPolicy policy,
    SRole role,
    SPermission permission,
    SLabel label);
}
