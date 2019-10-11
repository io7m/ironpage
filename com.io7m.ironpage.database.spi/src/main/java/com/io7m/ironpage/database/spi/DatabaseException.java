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

package com.io7m.ironpage.database.spi;

import com.io7m.ironpage.errors.api.ErrorSeverity;
import com.io7m.ironpage.errors.api.ErrorType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The type of exceptions raised by databases.
 */

public class DatabaseException extends Exception implements ErrorType
{
  private final ErrorSeverity severity;
  private final SortedMap<String, String> attributes;
  private final List<String> messageExtras;

  /**
   * Construct an exception.
   *
   * @param inSeverity      The error severity
   * @param message         The error message
   * @param cause           The cause
   * @param inAttributes    The attributes associated with the error
   * @param inMessageExtras The extra message lines
   */

  public DatabaseException(
    final ErrorSeverity inSeverity,
    final String message,
    final Throwable cause,
    final SortedMap<String, String> inAttributes,
    final List<String> inMessageExtras)
  {
    super(message, cause);
    this.severity = Objects.requireNonNull(inSeverity, "inSeverity");
    this.attributes = Objects.requireNonNull(inAttributes, "inAttributes");
    this.messageExtras = Objects.requireNonNull(inMessageExtras, "inMessageExtras");
  }

  /**
   * Construct an exception.
   *
   * @param message The error message
   */

  public DatabaseException(
    final String message)
  {
    this(ErrorSeverity.SEVERITY_ERROR, message, null, new TreeMap<>(), List.of());
  }


  /**
   * Construct an exception.
   *
   * @param message The error message
   * @param cause   The cause
   */
  public DatabaseException(
    final String message,
    final Throwable cause)
  {
    this(ErrorSeverity.SEVERITY_ERROR, message, cause, new TreeMap<>(), List.of());
  }

  /**
   * Construct an exception.
   *
   * @param inSeverity   The error severity
   * @param message      The error message
   * @param cause        The cause
   * @param inAttributes The attributes associated with the error
   */

  public DatabaseException(
    final ErrorSeverity inSeverity,
    final String message,
    final Throwable cause,
    final SortedMap<String, String> inAttributes)
  {
    this(inSeverity, message, cause, inAttributes, List.of());
  }

  /**
   * Construct an exception.
   *
   * @param inSeverity      The error severity
   * @param message         The error message
   * @param cause           The cause
   * @param inMessageExtras The extra message lines
   */

  public DatabaseException(
    final ErrorSeverity inSeverity,
    final String message,
    final Throwable cause,
    final List<String> inMessageExtras)
  {
    this(inSeverity, message, cause, new TreeMap<>(), inMessageExtras);
  }

  /**
   * Construct an exception.
   *
   * @param inSeverity The error severity
   * @param message    The error message
   * @param cause      The cause
   */

  public DatabaseException(
    final ErrorSeverity inSeverity,
    final String message,
    final Throwable cause)
  {
    this(inSeverity, message, cause, new TreeMap<>(), List.of());
  }

  @Override
  public final ErrorSeverity severity()
  {
    return this.severity;
  }

  @Override
  public final SortedMap<String, String> attributes()
  {
    return this.attributes;
  }

  @Override
  public final List<String> messageExtras()
  {
    return this.messageExtras;
  }

  @Override
  public final String message()
  {
    return this.getMessage();
  }

  @Override
  public final Optional<Exception> exception()
  {
    return Optional.of(this);
  }
}
