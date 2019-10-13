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

import com.io7m.ironpage.parser.api.ParserError;
import com.io7m.ironpage.parser.api.ParserErrorCode;
import com.io7m.ironpage.security.api.SLabel;
import com.io7m.ironpage.security.api.SPermission;
import com.io7m.ironpage.security.api.SPolicyParserType;
import com.io7m.ironpage.security.api.SRole;
import org.apache.commons.io.input.BrokenInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;

import static com.io7m.ironpage.security.api.SPolicyRuleConclusion.PERMIT_QUICK;

public abstract class SPolicyParsersContract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(SPolicyParsersContract.class);

  private ArrayList<ParserError> errors;

  private static InputStream resource(
    final String name)
    throws FileNotFoundException
  {
    final var file = String.format("/com/io7m/ironpage/tests/%s", name);
    final var stream = SPolicyParsersContract.class.getResourceAsStream(file);
    if (stream == null) {
      throw new FileNotFoundException(file);
    }
    return stream;
  }

  protected final void showError(
    final ParserError error)
  {
    LOG.debug("error: {}", error);
    this.errors.add(error);
    throw new IllegalStateException("Bad receiver!");
  }

  private boolean errorsContain(
    final ParserErrorCode code)
  {
    return this.errors.stream().anyMatch(e -> e.errorCode().equals(code));
  }

  protected abstract SPolicyParserType parser(InputStream stream);

  @BeforeEach
  public final void testSetup()
  {
    this.errors = new ArrayList<>();
  }

  /**
   * Invalid XML is an error.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testInvalid0()
    throws Exception
  {
    try (var parser = this.parser(resource("invalid-0.xml"))) {
      final var resultOpt = parser.execute();
      Assertions.assertTrue(resultOpt.isEmpty());
      Assertions.assertEquals(2, this.errors.size());
      Assertions.assertTrue(this.errorsContain(SPolicyParserType.INVALID_DATA));
      Assertions.assertTrue(this.errorsContain(SPolicyParserType.MALFORMED_XML));
    }
  }

  /**
   * Invalid XML is an error.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testInvalid1()
    throws Exception
  {
    try (var parser = this.parser(resource("invalid-1.xml"))) {
      final var resultOpt = parser.execute();
      Assertions.assertTrue(resultOpt.isEmpty());
      Assertions.assertEquals(2, this.errors.size());
      Assertions.assertTrue(this.errorsContain(SPolicyParserType.INVALID_DATA));
    }
  }

  /**
   * IO errors are errors.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testInvalid2()
    throws Exception
  {
    final var parser = this.parser(new BrokenInputStream());
    final var resultOpt = parser.execute();
    Assertions.assertTrue(resultOpt.isEmpty());
    Assertions.assertEquals(1, this.errors.size());
    Assertions.assertTrue(this.errorsContain(SPolicyParserType.IO_ERROR));
  }

  /**
   * Empty policies are empty.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testEmpty()
    throws Exception
  {
    try (var parser = this.parser(resource("policy-empty.xml"))) {
      final var result = parser.execute();
      Assertions.assertTrue(result.isPresent());
      final var policy = result.get();
      Assertions.assertEquals(1L, policy.version().longValue());
      Assertions.assertEquals("", policy.comment());
      Assertions.assertEquals(0L, (long) policy.rules().size());
    }
  }

  /**
   * A basic policy is parsed correctly.
   *
   * @throws Exception On errors
   */

  @Test
  public final void testBasic0()
    throws Exception
  {
    try (var parser = this.parser(resource("policy-basic0.xml"))) {
      final var result = parser.execute();
      Assertions.assertTrue(result.isPresent());
      final var policy = result.get();
      Assertions.assertEquals(23L, policy.version().longValue());
      Assertions.assertEquals("A basic security policy.", policy.comment());
      Assertions.assertEquals(4L, (long) policy.rules().size());

      {
        final var rule = policy.rules().get(0);
        Assertions.assertEquals(PERMIT_QUICK, rule.conclude());
        Assertions.assertEquals(Optional.empty(), rule.role());
        Assertions.assertEquals(Optional.of(SPermission.of("write")), rule.permission());
        Assertions.assertEquals(Optional.of(SLabel.of("page")), rule.label());
        Assertions.assertEquals("Anyone can write to ordinary pages.", rule.comment());
      }

      {
        final var rule = policy.rules().get(1);
        Assertions.assertEquals(PERMIT_QUICK, rule.conclude());
        Assertions.assertEquals(Optional.empty(), rule.role());
        Assertions.assertEquals(Optional.of(SPermission.of("read")), rule.permission());
        Assertions.assertEquals(Optional.of(SLabel.of("page")), rule.label());
        Assertions.assertEquals("Anyone can read from ordinary pages.", rule.comment());
      }

      {
        final var rule = policy.rules().get(2);
        Assertions.assertEquals(PERMIT_QUICK, rule.conclude());
        Assertions.assertEquals(Optional.of(SRole.of("superuser")), rule.role());
        Assertions.assertEquals(Optional.of(SPermission.of("write")), rule.permission());
        Assertions.assertEquals(Optional.of(SLabel.of("adminPage")), rule.label());
        Assertions.assertEquals("Superusers can write to administration pages.", rule.comment());
      }

      {
        final var rule = policy.rules().get(3);
        Assertions.assertEquals(PERMIT_QUICK, rule.conclude());
        Assertions.assertEquals(Optional.of(SRole.of("superuser")), rule.role());
        Assertions.assertEquals(Optional.of(SPermission.of("read")), rule.permission());
        Assertions.assertEquals(Optional.of(SLabel.of("adminPage")), rule.label());
        Assertions.assertEquals("Superusers can read from administration pages.", rule.comment());
      }
    }
  }
}
