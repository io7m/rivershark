/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.rivershark.cmdline.internal;

import com.io7m.verona.core.Version;
import com.io7m.verona.core.VersionException;
import com.io7m.verona.core.VersionParser;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * Functions over services.
 */

public final class RSServices
{
  private RSServices()
  {

  }

  /**
   * Find a service, or fail.
   *
   * @param service The service class
   * @param <T>     The type of service
   *
   * @return The service instance
   */

  public static <T> T findService(
    final Class<T> service)
  {
    return ServiceLoader.load(service)
      .findFirst()
      .orElseThrow(() -> missingService(service));
  }

  private static IllegalStateException missingService(
    final Class<?> clazz)
  {
    return new IllegalStateException(String.format(
      "No available implementations of service: %s",
      clazz.getCanonicalName()));
  }

  /**
   * @return The application version
   *
   * @throws IOException      On I/O errors
   * @throws VersionException On parse errors
   */

  public static Version findApplicationVersion()
    throws IOException, VersionException
  {
    final URL resource =
      RSServices.class.getResource(
        "/com/io7m/rivershark/cmdline/internal/version.properties"
      );

    try (var stream = resource.openStream()) {
      final var properties = new Properties();
      properties.load(stream);
      return VersionParser.parse(properties.getProperty("version"));
    }
  }
}
