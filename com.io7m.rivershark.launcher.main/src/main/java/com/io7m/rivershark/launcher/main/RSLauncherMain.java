/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.rivershark.launcher.main;

import com.io7m.rivershark.launcher.api.RSLauncherFactoryType;

import java.nio.file.Paths;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * The main launcher entry point.
 */

public final class RSLauncherMain
{
  private RSLauncherMain()
  {

  }

  /**
   * The main entry point.
   *
   * @param args Command-line arguments
   */

  public static void main(
    final String[] args)
  {
    try {
      mainExitless(args);
      System.exit(0);
    } catch (final Exception e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  /**
   * The main entry point, raising exceptions on failure.
   *
   * @param args The command-line arguments
   *
   * @throws Exception On errors
   */

  public static void mainExitless(
    final String[] args)
    throws Exception
  {
    if (args.length != 1) {
      System.err.println("rivershark: usage: config.properties");
      throw new Exception("Missing command-line arguments");
    }

    final var launchersLoader =
      ServiceLoader.load(RSLauncherFactoryType.class);

    final var launchers =
      launchersLoader.findFirst()
        .orElseThrow(() -> {
          throw new ServiceConfigurationError(
            "No available services of type: %s"
              .formatted(RSLauncherFactoryType.class));
        });

    final var file = Paths.get(args[0]);
    try (var launcher = launchers.createFromFile(file)) {
      launcher.run();
      launcher.waitForStop(0L);
    }
  }
}
