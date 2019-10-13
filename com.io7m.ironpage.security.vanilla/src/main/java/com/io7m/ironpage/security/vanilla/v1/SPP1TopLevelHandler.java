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
import com.io7m.ironpage.security.api.SPolicy;
import com.io7m.ironpage.security.api.SPolicyRule;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.math.BigInteger;
import java.util.Map;

/**
 * A handler for top-level "SecurityPolicy" elements.
 */

public final class SPP1TopLevelHandler
  implements BTElementHandlerType<Object, SPolicy>
{
  private final SPolicy.Builder policyBuilder;

  /**
   * Construct a handler.
   */

  public SPP1TopLevelHandler()
  {
    this.policyBuilder = SPolicy.builder();
  }

  @Override
  public void onElementStart(
    final BTElementParsingContextType context,
    final Attributes attributes)
    throws SAXException
  {
    try {
      this.policyBuilder.setLexical(SPolicyLexical.position(context));
      this.policyBuilder.setVersion(new BigInteger(attributes.getValue("version")));
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public Map<BTQualifiedName, BTElementHandlerConstructorType<?, ?>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    final var namespaceURI = SPP1Constants.POLICY_1_0_NAMESPACE.toString();

    return Map.ofEntries(
      Map.entry(
        BTQualifiedName.of(namespaceURI, "Comment"),
        Blackthorne.widenConstructor(c -> new SPP1CommentHandler())),
      Map.entry(
        BTQualifiedName.of(namespaceURI, "Rule"),
        Blackthorne.widenConstructor(c -> new SPP1RuleHandler()))
    );
  }

  @Override
  public SPolicy onElementFinished(
    final BTElementParsingContextType context)
    throws SAXException
  {
    try {
      return this.policyBuilder.build();
    } catch (final Exception e) {
      throw context.parseException(e);
    }
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final Object incoming)
  {
    if (incoming instanceof String) {
      this.policyBuilder.setComment((String) incoming);
      return;
    }
    if (incoming instanceof SPolicyRule) {
      this.policyBuilder.addRules((SPolicyRule) incoming);
      return;
    }
  }
}
