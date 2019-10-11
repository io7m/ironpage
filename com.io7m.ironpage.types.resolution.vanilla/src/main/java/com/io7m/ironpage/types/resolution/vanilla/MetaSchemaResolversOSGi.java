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


package com.io7m.ironpage.types.resolution.vanilla;

import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverProviderType;
import com.io7m.ironpage.types.resolution.api.MetaSchemaResolverType;
import com.io7m.ironpage.types.resolution.spi.MetaSchemaDirectoryType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A schema resolver that takes directories the OSGi service directory.
 */

@Component(service = MetaSchemaResolverProviderType.class)
public final class MetaSchemaResolversOSGi implements MetaSchemaResolverProviderType
{
  private final Object serviceLock = new Object();
  private List<MetaSchemaDirectoryType> services = new ArrayList<>();

  /**
   * Construct a resolver provider.
   */

  public MetaSchemaResolversOSGi()
  {

  }

  /**
   * A directory became available.
   *
   * @param directory The directory
   */

  @Reference(
    cardinality = ReferenceCardinality.AT_LEAST_ONE,
    policy = ReferencePolicy.DYNAMIC,
    policyOption = ReferencePolicyOption.GREEDY,
    unbind = "onDirectoryUnavailable")
  public void onDirectoryAvailable(
    final MetaSchemaDirectoryType directory)
  {
    Objects.requireNonNull(directory, "directory");

    synchronized (this.serviceLock) {
      this.services.add(directory);
    }
  }

  /**
   * A directory became unavailable.
   *
   * @param directory The directory
   */

  public void onDirectoryUnavailable(
    final MetaSchemaDirectoryType directory)
  {
    Objects.requireNonNull(directory, "directory");

    synchronized (this.serviceLock) {
      this.services.remove(directory);
    }
  }

  @Override
  public MetaSchemaResolverType createForLocale(final Locale locale)
  {
    synchronized (this.serviceLock) {
      return new MetaSchemaResolver(locale, List.copyOf(this.services));
    }
  }
}
