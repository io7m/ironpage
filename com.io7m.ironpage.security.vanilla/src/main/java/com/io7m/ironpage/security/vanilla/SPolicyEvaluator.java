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
import com.io7m.ironpage.security.api.SPolicyEvaluatorType;
import com.io7m.ironpage.security.api.SRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * The default policy evaluator implementation.
 */

final class SPolicyEvaluator implements SPolicyEvaluatorType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(SPolicyEvaluator.class);

  private final long id;
  private final SPolicy policy;
  private final SRole role;
  private final SPermission permission;
  private final SLabel label;

  SPolicyEvaluator(
    final long inId,
    final SPolicy inPolicy,
    final SRole inRole,
    final SPermission inPermission,
    final SLabel inLabel)
  {
    this.id = inId;

    this.policy =
      Objects.requireNonNull(inPolicy, "policy");
    this.role =
      Objects.requireNonNull(inRole, "role");
    this.permission =
      Objects.requireNonNull(inPermission, "permission");
    this.label =
      Objects.requireNonNull(inLabel, "label");
  }

  private void trace(
    final String format,
    final Object... args)
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace("[{}]: {}", Long.toUnsignedString(this.id, 16), String.format(format, args));
    }
  }

  @Override
  public boolean permits()
  {
    Objects.requireNonNull(this.policy, "policy");
    Objects.requireNonNull(this.role, "role");
    Objects.requireNonNull(this.permission, "permission");
    Objects.requireNonNull(this.label, "label");

    final var rules = this.policy.rules();
    var permit = false;

    this.trace(
      "checking: role %s permission %s label %s",
      this.role.role(),
      this.permission.permission(),
      this.label.label());

    var ruleMatchLast = 0;
    RULE_LOOP:
    for (var ruleIndex = 0; ruleIndex < rules.size(); ++ruleIndex) {
      final var rule = rules.get(ruleIndex);

      final var matches = rule.matches(this.role, this.permission, this.label);
      this.trace(
        "[%d] role %s permission %s label %s -> %s",
        Integer.valueOf(ruleIndex),
        rule.role().map(SRole::role).orElse("<any>"),
        rule.permission().map(SPermission::permission).orElse("<any>"),
        rule.label().map(SLabel::label).orElse("<any>"),
        matches ? "matches" : "does not match"
      );

      if (matches) {
        ruleMatchLast = ruleIndex;
        switch (rule.conclude()) {
          case PERMIT: {
            permit = true;
            break;
          }
          case PERMIT_QUICK: {
            permit = true;
            break RULE_LOOP;
          }
          case DENY: {
            permit = false;
            break;
          }
          case DENY_QUICK: {
            permit = false;
            break RULE_LOOP;
          }
        }
      }
    }

    this.trace("[%d] %s", Integer.valueOf(ruleMatchLast), permit ? "permit" : "deny");
    return permit;
  }
}
