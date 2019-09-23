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

import com.io7m.ironpage.types.resolution.api.SchemaResolvedSet;
import com.io7m.ironpage.types.resolution.api.SchemaResolvedSetEdge;
import com.io7m.ironpage.validator.api.SchemaValidationError;
import com.io7m.ironpage.validator.api.SchemaValidationRequest;
import com.io7m.ironpage.validator.api.SchemaValidatorProviderType;
import io.vavr.collection.Vector;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;

public abstract class SchemaValidatorContract
{
  protected abstract Logger logger();

  protected abstract SchemaValidatorProviderType validators();

  @Test
  public final void testEmpty()
  {
    final var resolved =
      SchemaResolvedSet.builder()
        .setGraph(new DirectedAcyclicGraph<>(SchemaResolvedSetEdge.class))
        .setSchemas(Vector.empty())
        .build();

    final var validators = this.validators();
    final var validator = validators.create();

    final var request =
      SchemaValidationRequest.builder()
        .setResolvedModules(resolved)
        .build();

    final var errors = new ArrayList<SchemaValidationError>();
    final var result = validator.validate(request, errors::add);
    Assertions.assertEquals(0, result.size());
  }
}
