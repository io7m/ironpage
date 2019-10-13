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

import com.io7m.ironpage.security.api.SLabel;
import com.io7m.ironpage.security.api.SPermission;
import com.io7m.ironpage.security.api.SPolicy;
import com.io7m.ironpage.security.api.SPolicyEvaluatorProviderType;
import com.io7m.ironpage.security.api.SPolicyRule;
import com.io7m.ironpage.security.api.SPolicyRuleConclusion;
import com.io7m.ironpage.security.api.SRole;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Assertions;

import java.math.BigInteger;

public abstract class SPolicyEvaluatorContract
{
  protected abstract SPolicyEvaluatorProviderType evaluators();

  /**
   * An empty policy denies everything.
   */

  @Property
  public final void testEmpty(
    final @ForAll SRole role,
    final @ForAll SPermission permission,
    final @ForAll SLabel label)
  {
    final var policy =
      SPolicy.builder()
        .setVersion(BigInteger.ONE)
        .build();

    final var evaluator =
      this.evaluators().create(policy, role, permission, label);

    Assertions.assertFalse(evaluator.permits());
  }

  /**
   * A policy with a universal permit rule permits everything.
   */

  @Property
  public final void testPermitAll(
    final @ForAll SRole role,
    final @ForAll SPermission permission,
    final @ForAll SLabel label)
  {
    final var policy =
      SPolicy.builder()
        .setVersion(BigInteger.ONE)
        .addRules(
          SPolicyRule.builder()
            .setConclude(SPolicyRuleConclusion.PERMIT)
            .build())
        .build();

    final var evaluator =
      this.evaluators().create(policy, role, permission, label);

    Assertions.assertTrue(evaluator.permits());
  }

  /**
   * Various properties of a basic policy are evaluated correctly.
   */

  @Property
  public final void testBasic()
  {
    final var policy =
      SPolicy.builder()
        .setVersion(BigInteger.ONE)
        .addRules(
          SPolicyRule.builder()
            .setConclude(SPolicyRuleConclusion.DENY)
            .build())
        .addRules(
          SPolicyRule.builder()
            .setConclude(SPolicyRuleConclusion.DENY_QUICK)
            .setRole(SRole.of("banned"))
            .build())
        .addRules(
          SPolicyRule.builder()
            .setConclude(SPolicyRuleConclusion.PERMIT)
            .setRole(SRole.of("stats"))
            .setPermission(SPermission.of("read"))
            .setLabel(SLabel.of("page"))
            .build())
        .addRules(
          SPolicyRule.builder()
            .setConclude(SPolicyRuleConclusion.PERMIT_QUICK)
            .setRole(SRole.of("superuser"))
            .setPermission(SPermission.of("write"))
            .setLabel(SLabel.of("adminPage"))
            .build())
        .addRules(
          SPolicyRule.builder()
            .setConclude(SPolicyRuleConclusion.PERMIT_QUICK)
            .setRole(SRole.of("superuser"))
            .setPermission(SPermission.of("read"))
            .setLabel(SLabel.of("adminPage"))
            .build())
        .addRules(
          SPolicyRule.builder()
            .setConclude(SPolicyRuleConclusion.PERMIT_QUICK)
            .setPermission(SPermission.of("write"))
            .setLabel(SLabel.of("page"))
            .build())
        .addRules(
          SPolicyRule.builder()
            .setConclude(SPolicyRuleConclusion.PERMIT_QUICK)
            .setPermission(SPermission.of("read"))
            .setLabel(SLabel.of("page"))
            .build())
        .build();

    {
      final var evaluator =
        this.evaluators().create(
          policy,
          SRole.of("user"),
          SPermission.of("write"),
          SLabel.of("page"));
      Assertions.assertTrue(evaluator.permits());
    }

    {
      final var evaluator =
        this.evaluators().create(
          policy,
          SRole.of("user"),
          SPermission.of("write"),
          SLabel.of("adminPage"));
      Assertions.assertFalse(evaluator.permits());
    }

    {
      final var evaluator =
        this.evaluators().create(
          policy,
          SRole.of("stats"),
          SPermission.of("read"),
          SLabel.of("page"));
      Assertions.assertTrue(evaluator.permits());
    }

    {
      final var evaluator =
        this.evaluators().create(
          policy,
          SRole.of("banned"),
          SPermission.of("write"),
          SLabel.of("adminPage"));
      Assertions.assertFalse(evaluator.permits());
    }

    {
      final var evaluator =
        this.evaluators().create(
          policy,
          SRole.of("anonymous"),
          SPermission.of("write"),
          SLabel.of("page"));
      Assertions.assertTrue(evaluator.permits());
    }
  }
}
