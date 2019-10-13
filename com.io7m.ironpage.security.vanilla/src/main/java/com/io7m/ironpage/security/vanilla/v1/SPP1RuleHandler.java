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

package com.io7m.ironpage.security.vanilla.v1;

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.blackthorne.api.Blackthorne;
import com.io7m.ironpage.security.api.SLabel;
import com.io7m.ironpage.security.api.SPermission;
import com.io7m.ironpage.security.api.SPolicyRule;
import com.io7m.ironpage.security.api.SPolicyRuleConclusion;
import com.io7m.ironpage.security.api.SRole;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Map;
import java.util.Optional;

/**
 * A handler for "Rule" elements.
 */

public final class SPP1RuleHandler implements BTElementHandlerType<String, SPolicyRule>
{
  private SPolicyRule.Builder result = SPolicyRule.builder();

  /**
   * Construct a handler.
   */

  public SPP1RuleHandler()
  {

  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ? extends String>>
  onChildHandlersRequested(final BTElementParsingContextType context)
  {
    final var namespaceURI = SPP1Constants.POLICY_1_0_NAMESPACE.toString();

    return Map.ofEntries(
      Map.entry(
        BTQualifiedName.of(namespaceURI, "Comment"),
        Blackthorne.widenConstructor(c -> new SPP1CommentHandler()))
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final String comment)
  {
    this.result.setComment(comment);
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.result.setLexical(
        SPolicyLexical.position(context));
      this.result.setConclude(
        SPolicyRuleConclusion.valueOf(attributes.getValue("conclusion")));
      this.result.setLabel(
        Optional.ofNullable(attributes.getValue("label")).map(SLabel::of));
      this.result.setPermission(
        Optional.ofNullable(attributes.getValue("permission")).map(SPermission::of));
      this.result.setRole(
        Optional.ofNullable(attributes.getValue("role")).map(SRole::of));
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public SPolicyRule onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result.build();
  }
}
