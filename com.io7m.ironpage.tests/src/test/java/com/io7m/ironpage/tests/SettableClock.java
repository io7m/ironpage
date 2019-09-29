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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A clock that only changes time when the time is explicitly set.
 */

public final class SettableClock extends Clock
{
  private final BiConsumer<SettableClock, Instant> onGet;
  private final ZoneId zone;
  private Instant now;

  /**
   * Construct a clock.
   *
   * @param inZone  The time zone
   * @param inNow   The current time
   * @param inOnGet A function evaluated each time someone requests the time
   */

  public SettableClock(
    final ZoneId inZone,
    final Instant inNow,
    final BiConsumer<SettableClock, Instant> inOnGet)
  {
    this.zone = Objects.requireNonNull(inZone, "zone");
    this.now = Objects.requireNonNull(inNow, "now");
    this.onGet = Objects.requireNonNull(inOnGet, "onGet");
  }

  /**
   * Set the current time.
   *
   * @param time The new time
   */

  public void setTime(
    final Instant time)
  {
    this.now = Objects.requireNonNull(time, "time");
  }

  @Override
  public ZoneId getZone()
  {
    return this.zone;
  }

  @Override
  public Clock withZone(final ZoneId inZone)
  {
    return new SettableClock(inZone, this.now, this.onGet);
  }

  @Override
  public Instant instant()
  {
    this.onGet.accept(this, this.now);
    return this.now;
  }
}

