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

package com.io7m.rivershark.tests;

import com.io7m.rivershark.launcher.api.RSLauncherConfiguration;
import com.io7m.sunburst.model.SBPackageIdentifier;
import com.io7m.sunburst.model.SBPackageVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class RSLauncherConfigurationTest
{
  private Path directory;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory = RSTestDirectories.createTempDirectory();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    RSTestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testEmpty()
    throws Exception
  {
    final var configuration =
      RSLauncherConfiguration.builder(this.directory)
        .build();

    this.roundTrip(configuration);
  }

  @Test
  public void testComplete()
    throws Exception
  {
    final var configuration =
      RSLauncherConfiguration.builder(this.directory)
        .addJavaModule(this.directory.resolve("module-0.jar"))
        .addJavaModule(this.directory.resolve("module-1.jar"))
        .addJavaModule(this.directory.resolve("module-2.jar"))
        .addOSGIBundle(this.directory.resolve("bundle-0.jar"))
        .addOSGIBundle(this.directory.resolve("bundle-1.jar"))
        .addOSGIBundle(this.directory.resolve("bundle-2.jar"))
        .addParameter("x0", "y0")
        .addParameter("x1", "y1")
        .addParameter("x2", "y2")
        .addSunburstPackage(
          new SBPackageIdentifier(
            "com.io7m.ex0",
            new SBPackageVersion(1,0,0,"")))
        .addSunburstPackage(
          new SBPackageIdentifier(
            "com.io7m.ex1",
            new SBPackageVersion(2,0,0,"")))
        .addSunburstPackage(
          new SBPackageIdentifier(
            "com.io7m.ex2",
            new SBPackageVersion(1,3,1,"")))
        .build();

    this.roundTrip(configuration);
  }

  private void roundTrip(
    final RSLauncherConfiguration configuration)
    throws IOException
  {
    final var properties =
      configuration.toProperties();

    final var file =
      this.directory.resolve("rivershark.conf");

    try (var output = Files.newOutputStream(file)) {
      properties.store(output, "");
    }

    final RSLauncherConfiguration loaded =
      RSLauncherConfiguration.parseFile(file);

    assertEquals(configuration, loaded);
    assertEquals(configuration, configuration);
    assertEquals(configuration.hashCode(), loaded.hashCode());
    assertNotEquals(configuration, Integer.valueOf(23));

    assertEquals(configuration.javaModules(), loaded.javaModules());
    assertEquals(configuration.osgiBundles(), loaded.osgiBundles());
    assertEquals(configuration.runtimeDirectory(), loaded.runtimeDirectory());
    assertEquals(configuration.parameters(), loaded.parameters());
  }
}
