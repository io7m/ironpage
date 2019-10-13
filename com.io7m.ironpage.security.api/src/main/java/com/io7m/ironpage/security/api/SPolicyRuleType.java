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

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * A rule in security policy.
 */

@ImmutablesStyleType
@Value.Immutable
public interface SPolicyRuleType extends LexicalType<URI>
{
  private static <T> boolean checkMatches(
    final Optional<T> thisX,
    final T inputX)
  {
    return thisX.map(x -> Boolean.valueOf(x.equals(inputX))).orElse(Boolean.TRUE).booleanValue();
  }

  @Override
  @Value.Default
  @Value.Auxiliary
  default LexicalPosition<URI> lexical()
  {
    return LexicalPositions.zero();
  }

  /**
   * @return The rule comment
   */

  @Value.Parameter
  @Value.Default
  default String comment()
  {
    return "";
  }

  /**
   * @return The conclusion of the rule, if the rule matches.
   */

  @Value.Parameter
  SPolicyRuleConclusion conclude();

  /**
   * @return The role against which the rule will match.
   */

  @Value.Parameter
  Optional<SRole> role();

  /**
   * @return The permission against which the rule will match.
   */

  @Value.Parameter
  Optional<SPermission> permission();

  /**
   * @return The label against which the rule will match.
   */

  @Value.Parameter
  Optional<SLabel> label();

  /**
   * Determine whether or not the given input matches this rule.
   *
   * @param inputRole       The input role
   * @param inputPermission The input permission
   * @param inputLabel      The input label
   *
   * @return {@code true} if the rule matches
   */

  default boolean matches(
    final SRole inputRole,
    final SPermission inputPermission,
    final SLabel inputLabel)
  {
    Objects.requireNonNull(inputRole, "inputRole");
    Objects.requireNonNull(inputPermission, "inputPermission");
    Objects.requireNonNull(inputLabel, "inputLabel");

    return checkMatches(this.role(), inputRole)
      && checkMatches(this.permission(), inputPermission)
      && checkMatches(this.label(), inputLabel);
  }
}
