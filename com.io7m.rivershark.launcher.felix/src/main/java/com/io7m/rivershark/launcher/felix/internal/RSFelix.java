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


package com.io7m.rivershark.launcher.felix.internal;

import com.io7m.rivershark.launcher.api.RSLauncherConfiguration;
import com.io7m.rivershark.launcher.api.RSLauncherType;
import org.apache.felix.atomos.Atomos;
import org.apache.felix.atomos.AtomosContent;
import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.felix.atomos.Atomos.ATOMOS_CONTENT_INSTALL;
import static org.apache.felix.atomos.Atomos.ATOMOS_CONTENT_START;
import static org.apache.felix.atomos.AtomosLayer.LoaderType.SINGLE;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES;

/**
 * A launcher using Felix.
 */

public final class RSFelix implements RSLauncherType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RSFelix.class);

  private final RSLauncherConfiguration configuration;
  private final AtomicBoolean started;
  private Framework framework;

  private RSFelix(
    final RSLauncherConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");

    this.started = new AtomicBoolean(false);
  }

  /**
   * Create a launcher.
   *
   * @param configuration The configuration
   *
   * @return A launcher
   */

  public static RSLauncherType create(
    final RSLauncherConfiguration configuration)
  {
    return new RSFelix(configuration);
  }

  @Override
  public void run()
    throws Exception
  {
    if (this.started.compareAndSet(false, true)) {
      LOG.debug("starting launcher");

      final var configMap = new HashMap<String, String>();
      configMap.put(
        FRAMEWORK_STORAGE,
        this.configuration.runtimeDirectory()
          .toAbsolutePath()
          .toString()
      );
      configMap.put(FRAMEWORK_STORAGE_CLEAN, "true");
      configMap.put(FRAMEWORK_SYSTEMPACKAGES, "");
      configMap.put(ATOMOS_CONTENT_INSTALL, "false");
      configMap.put(ATOMOS_CONTENT_START, "false");

      final var atomos =
        Atomos.newAtomos(configMap);

      final var bootLayer =
        atomos.getBootLayer();
      final var javaModules =
        this.configuration.javaModules();

      this.framework = atomos.newFramework(configMap);
      this.framework.init();

      /*
       * Filter modules that were discovered by Atomos. Limit the observable
       * modules to only those present in the JDK/JRE.
       */

      LOG.debug("[boot-jpms] installing modules");

      final var bootModules =
        bootLayer.getAtomosContents()
          .stream()
          .filter(RSFelix::isExposedBootModule)
          .sorted(Comparator.comparing(AtomosContent::getSymbolicName))
          .toList();

      for (final var content : bootModules) {
        LOG.debug("[boot-jpms] install {}", content.getSymbolicName());
        content.install();
      }

      /*
       * Install all the JPMS modules that aren't exposed by the JDK/JRE. This
       * involves creating module layers for each module. The created layers
       * are all children of the boot layer.
       */

      LOG.debug("[application-jpms] installing");

      if (javaModules.isEmpty()) {
        LOG.debug("[application-jpms] no application modules required");
      }

      for (final var module : javaModules) {
        final var name = module.getFileName().toString();
        LOG.debug("[application-jpms] install {}", name);

        final var directory =
          module.getParent();
        final var layer =
          atomos.addLayer(List.of(bootLayer), name, SINGLE, directory);

        for (final var content : layer.getAtomosContents()) {
          content.install();
        }
      }

      /*
       * Install OSGi bundles.
       */

      LOG.debug("[application-osgi] installing");

      final var osgiBundles =
        this.configuration.osgiBundles();

      if (osgiBundles.isEmpty()) {
        LOG.debug("[application-osgi] no application OSGi bundles required");
      }

      final var context =
        this.framework.getBundleContext();

      final var toStart = new ArrayList<Bundle>();
      for (final var bundleFile : osgiBundles) {
        final var name = bundleFile.getFileName();
        LOG.debug("[application-osgi] install {}", name);

        final var location = new StringBuilder("reference:file:");
        location.append(bundleFile.toAbsolutePath());
        toStart.add(context.installBundle(location.toString()));
      }

      LOG.debug("[application-osgi] starting");

      for (final var bundle : toStart) {
        LOG.debug("[application-osgi] start {} {}", bundle.getSymbolicName(), bundle.getVersion());
        bundle.start();
      }

      LOG.debug("[osgi] starting framework");
      this.framework.start();
    }
  }

  private static boolean isExposedBootModule(
    final AtomosContent c)
  {
    final var name = c.getSymbolicName();
    return name.startsWith("java") || name.startsWith("jdk");
  }

  @Override
  public void waitForStop(
    final long timeout)
    throws Exception
  {
    this.framework.waitForStop(timeout);
  }

  @Override
  public RSLauncherConfiguration configuration()
  {
    return this.configuration;
  }

  @Override
  public void close()
    throws Exception
  {
    if (this.started.compareAndSet(true, false)) {
      LOG.debug("stopping launcher");
      this.framework.stop();
    }
  }
}
