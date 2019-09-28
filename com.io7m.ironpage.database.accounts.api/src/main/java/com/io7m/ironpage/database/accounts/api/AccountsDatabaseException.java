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

package com.io7m.ironpage.database.accounts.api;

import com.io7m.ironpage.database.spi.DatabaseException;
import com.io7m.ironpage.errors.api.ErrorSeverity;
import io.vavr.collection.SortedMap;

import java.util.List;
import java.util.Objects;

/**
 * The type of exceptions related to accounts databases.
 */

public class AccountsDatabaseException extends DatabaseException
{
  private final AccountsDatabaseErrorCode errorCode;

  /**
   * @return The accounts database error code
   */

  public final AccountsDatabaseErrorCode errorCode()
  {
    return this.errorCode;
  }

  /**
   * Construct an exception.
   *
   * @param inSeverity      The error severity
   * @param inErrorCode     The error code
   * @param message         The message
   * @param cause           The cause
   * @param inAttributes    The attributes associated with the error
   * @param inMessageExtras The extra message lines
   */

  public AccountsDatabaseException(
    final ErrorSeverity inSeverity,
    final AccountsDatabaseErrorCode inErrorCode,
    final String message,
    final Throwable cause,
    final SortedMap<String, String> inAttributes,
    final List<String> inMessageExtras)
  {
    super(inSeverity, message, cause, inAttributes, inMessageExtras);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inErrorCode The error code
   * @param message     The message
   */

  public AccountsDatabaseException(
    final AccountsDatabaseErrorCode inErrorCode,
    final String message)
  {
    super(message);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inErrorCode The error code
   * @param message     The message
   * @param cause       The cause
   */

  public AccountsDatabaseException(
    final AccountsDatabaseErrorCode inErrorCode,
    final String message,
    final Throwable cause)
  {
    super(message, cause);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inSeverity   The error severity
   * @param inErrorCode  The error code
   * @param message      The message
   * @param cause        The cause
   * @param inAttributes The attributes associated with the error
   */

  public AccountsDatabaseException(
    final ErrorSeverity inSeverity,
    final AccountsDatabaseErrorCode inErrorCode,
    final String message,
    final Throwable cause,
    final SortedMap<String, String> inAttributes)
  {
    super(inSeverity, message, cause, inAttributes);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inSeverity      The error severity
   * @param inErrorCode     The error code
   * @param message         The message
   * @param cause           The cause
   * @param inMessageExtras The extra message lines
   */

  public AccountsDatabaseException(
    final ErrorSeverity inSeverity,
    final AccountsDatabaseErrorCode inErrorCode,
    final String message,
    final Throwable cause,
    final List<String> inMessageExtras)
  {
    super(inSeverity, message, cause, inMessageExtras);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }

  /**
   * Construct an exception.
   *
   * @param inSeverity  The error severity
   * @param inErrorCode The error code
   * @param message     The message
   * @param cause       The cause
   */

  public AccountsDatabaseException(
    final ErrorSeverity inSeverity,
    final AccountsDatabaseErrorCode inErrorCode,
    final String message,
    final Throwable cause)
  {
    super(inSeverity, message, cause);
    this.errorCode = Objects.requireNonNull(inErrorCode, "errorCode");
  }
}
