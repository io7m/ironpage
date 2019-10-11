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

package com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.v1_0;

import com.io7m.blackthorne.api.BTElementHandlerConstructorType;
import com.io7m.blackthorne.api.BTElementHandlerType;
import com.io7m.blackthorne.api.BTElementParsingContextType;
import com.io7m.blackthorne.api.BTQualifiedName;
import com.io7m.blackthorne.api.Blackthorne;
import com.io7m.ironpage.metadata.schema.ast.MATypeReferenceType;
import com.io7m.ironpage.metadata.schema.compiler.parser.api.Parsed;

import java.util.Map;

import static com.io7m.ironpage.metadata.schema.compiler.vanilla.parser.v1_0.MSX1Constants.META_1_0_NAMESPACE;

/**
 * A handler for "TypeReference" elements.
 */

public final class MSX1TypeReferenceHandler
  implements BTElementHandlerType<MATypeReferenceType<Parsed>, MATypeReferenceType<Parsed>>
{
  private MATypeReferenceType<Parsed> result;

  /**
   * Construct a handler.
   */

  public MSX1TypeReferenceHandler()
  {

  }

  @Override
  public Map<BTQualifiedName,
    BTElementHandlerConstructorType<?, ? extends MATypeReferenceType<Parsed>>>
  onChildHandlersRequested(
    final BTElementParsingContextType context)
  {
    final var namespaceURI = META_1_0_NAMESPACE.toString();

    return Map.ofEntries(
      Map.entry(
        BTQualifiedName.of(namespaceURI, "TypeNamed"),
        Blackthorne.widenConstructor(c -> new MSX1TypeNamedHandler())),
      Map.entry(
        BTQualifiedName.of(namespaceURI, "TypePrimitive"),
        Blackthorne.widenConstructor(c -> new MSX1TypePrimitiveHandler()))
    );
  }

  @Override
  public void onChildValueProduced(
    final BTElementParsingContextType context,
    final MATypeReferenceType<Parsed> inResult)
  {
    this.result = inResult;
  }

  @Override
  public MATypeReferenceType<Parsed> onElementFinished(
    final BTElementParsingContextType context)
  {
    return this.result;
  }
}
