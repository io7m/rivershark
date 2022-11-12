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

package com.io7m.rivershark.launcher.api;

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyNonexistent;
import com.io7m.sunburst.model.SBPackageIdentifier;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The configuration parameters for a launcher.
 */

public final class RSLauncherConfiguration
{
  private static final Pattern PARAMETER_PREFIX =
    Pattern.compile("^rivershark\\.parameters\\.");

  private final List<Path> javaModules;
  private final List<Path> osgiBundles;
  private final Map<String, String> parameters;
  private final Path runtimeDirectory;
  private final Set<SBPackageIdentifier> sunburstPackages;

  private RSLauncherConfiguration(
    final Path inRuntimeDirectory,
    final List<Path> inJavaModules,
    final List<Path> inOsgiBundles,
    final Set<SBPackageIdentifier> inSunburstPackages,
    final Map<String, String> inParameters)
  {
    this.runtimeDirectory =
      Objects.requireNonNull(inRuntimeDirectory, "runtimeDirectory");
    this.javaModules =
      Objects.requireNonNull(inJavaModules, "javaModules");
    this.osgiBundles =
      Objects.requireNonNull(inOsgiBundles, "osgiBundles");
    this.sunburstPackages =
      Objects.requireNonNull(inSunburstPackages, "sunburstPackages");
    this.parameters =
      Objects.requireNonNull(inParameters, "inParameters");
  }

  /**
   * Export the current configuration to a set of properties.
   *
   * @return The properties
   */

  public Properties toProperties()
  {
    final var props = new Properties();
    for (final var entry : this.parameters.entrySet()) {
      final var key = "rivershark.parameters.%s".formatted(entry.getKey());
      final var val = entry.getValue();
      props.setProperty(key, val);
    }

    props.setProperty(
      "rivershark.runtimeDirectory",
      this.runtimeDirectory.toString()
    );

    for (int index = 0; index < this.javaModules.size(); ++index) {
      final var key =
        String.format("rivershark.modules.%d", Integer.valueOf(index));
      props.setProperty(key, this.javaModules.get(index).toString());
    }

    for (int index = 0; index < this.osgiBundles.size(); ++index) {
      final var key =
        String.format("rivershark.bundles.%d", Integer.valueOf(index));
      props.setProperty(key, this.osgiBundles.get(index).toString());
    }

    final var sunbursts = new ArrayList<>(this.sunburstPackages);
    Collections.sort(sunbursts);

    for (int index = 0; index < sunbursts.size(); ++index) {
      final var key =
        String.format("rivershark.sunburst.%d", Integer.valueOf(index));
      props.setProperty(key, sunbursts.get(index).toString());
    }

    return props;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !this.getClass().equals(o.getClass())) {
      return false;
    }
    final RSLauncherConfiguration that = (RSLauncherConfiguration) o;
    return this.javaModules.equals(that.javaModules)
           && this.osgiBundles.equals(that.osgiBundles)
           && this.parameters.equals(that.parameters)
           && this.runtimeDirectory.equals(that.runtimeDirectory)
           && this.sunburstPackages.equals(that.sunburstPackages);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(
      this.javaModules,
      this.osgiBundles,
      this.parameters,
      this.runtimeDirectory,
      this.sunburstPackages
    );
  }

  /**
   * @return The run-time directory used for the OSGi container
   */

  public Path runtimeDirectory()
  {
    return this.runtimeDirectory;
  }

  /**
   * @return The list of Java modules that will be installed
   */

  public List<Path> javaModules()
  {
    return this.javaModules;
  }

  /**
   * @return The list of OSGi bundles that will be installed
   */

  public List<Path> osgiBundles()
  {
    return this.osgiBundles;
  }

  /**
   * @return The extra configuration parameters
   */

  public Map<String, String> parameters()
  {
    return this.parameters;
  }

  /**
   * Parse the given file.
   *
   * @param file The file
   *
   * @return A parsed configuration
   *
   * @throws IOException On errors
   */

  public static RSLauncherConfiguration parseFile(
    final Path file)
    throws IOException
  {
    final var filesystem =
      file.getFileSystem();

    final var properties = new Properties();
    try (var stream = Files.newInputStream(file)) {
      properties.load(stream);
    }

    final var exceptions =
      new ExceptionTracker<IOException>();

    final var builder =
      new Builder(filesystem.getPath(""));

    loadRuntimeDirectory(exceptions, filesystem, properties, builder);
    loadModules(filesystem, properties, builder);
    loadBundles(filesystem, properties, builder);
    loadSunbursts(properties, builder);
    loadParameters(properties, builder);

    exceptions.throwIfNecessary();
    return builder.build();
  }

  private static void loadParameters(
    final Properties properties,
    final Builder builder)
  {
    final var params = new HashMap<String, String>();
    for (final var entry : toMap(properties).entrySet()) {
      final var key = entry.getKey();
      final var val = entry.getValue();
      if (key.startsWith("rivershark.parameters.")) {
        params.put(PARAMETER_PREFIX.matcher(key).replaceFirst(""), val);
      }
    }
    builder.addParameters(params);
  }

  private static void loadRuntimeDirectory(
    final ExceptionTracker<IOException> exceptions,
    final FileSystem filesystem,
    final Properties properties,
    final Builder builder)
  {
    try {
      builder.setRuntimeDirectory(
        filesystem.getPath(
          JProperties.getString(properties, "rivershark.runtimeDirectory")
        )
      );
    } catch (final JPropertyNonexistent e) {
      exceptions.addException(new IOException(e));
    }
  }

  private static Map<String, String> toMap(
    final Properties properties)
  {
    final var map = new HashMap<String, String>();
    for (final var e : properties.entrySet()) {
      final var key = (String) e.getKey();
      final var val = (String) e.getValue();
      map.put(key, val);
    }
    return map;
  }

  private static void loadModules(
    final FileSystem filesystem,
    final Properties properties,
    final Builder builder)
  {
    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var key =
        String.format("rivershark.modules.%d", Integer.valueOf(index));
      final var value =
        properties.getProperty(key);

      if (value == null) {
        break;
      }

      final var file =
        filesystem.getPath(value)
          .toAbsolutePath()
          .normalize();

      builder.addJavaModule(file);
    }
  }

  private static void loadSunbursts(
    final Properties properties,
    final Builder builder)
  {
    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var key =
        String.format("rivershark.sunburst.%d", Integer.valueOf(index));
      final var value =
        properties.getProperty(key);

      if (value == null) {
        break;
      }

      builder.addSunburstPackage(SBPackageIdentifier.parse(value));
    }
  }

  private static void loadBundles(
    final FileSystem filesystem,
    final Properties properties,
    final Builder builder)
  {
    for (int index = 0; index < Integer.MAX_VALUE; ++index) {
      final var key =
        String.format("rivershark.bundles.%d", Integer.valueOf(index));
      final var value =
        properties.getProperty(key);

      if (value == null) {
        break;
      }

      final var file =
        filesystem.getPath(value)
          .toAbsolutePath()
          .normalize();

      builder.addOSGIBundle(file);
    }
  }

  /**
   * Create a new builder.
   *
   * @param runtimeDirectory The runtime directory
   *
   * @return A new builder
   */

  public static Builder builder(
    final Path runtimeDirectory)
  {
    return new Builder(runtimeDirectory);
  }

  /**
   * A mutable configuration builder.
   */

  public static final class Builder
  {
    private final ArrayList<Path> javaModules;
    private final ArrayList<Path> osgiBundles;
    private final HashMap<String, String> parameters;
    private final HashSet<SBPackageIdentifier> sunburstPackages;
    private Path runtimeDirectory;

    /**
     * Create a new builder.
     *
     * @param inRuntimeDirectory The runtime directory
     */

    public Builder(
      final Path inRuntimeDirectory)
    {
      this.runtimeDirectory =
        Objects.requireNonNull(inRuntimeDirectory, "runtimeDirectory")
          .toAbsolutePath();

      this.javaModules = new ArrayList<>();
      this.osgiBundles = new ArrayList<>();
      this.sunburstPackages = new HashSet<>();
      this.parameters = new HashMap<>();
    }

    /**
     * Add extra configuration parameters.
     *
     * @param inParameters The parameters
     *
     * @return this
     */

    public Builder addParameters(
      final Map<String, String> inParameters)
    {
      this.parameters.putAll(
        Objects.requireNonNull(inParameters, "parameters"));
      return this;
    }

    /**
     * Add extra configuration parameters.
     *
     * @param key The key
     * @param value The value
     *
     * @return this
     */

    public Builder addParameter(
      final String key,
      final String value)
    {
      this.parameters.put(
        Objects.requireNonNull(key, "key"),
        Objects.requireNonNull(value, "value")
      );
      return this;
    }

    /**
     * Add a Java module.
     *
     * @param path The path
     *
     * @return this
     */

    public Builder addJavaModule(
      final Path path)
    {
      this.javaModules.add(
        Objects.requireNonNull(path, "path")
          .toAbsolutePath()
      );
      return this;
    }

    /**
     * Add an OSGi bundle.
     *
     * @param path The path
     *
     * @return this
     */

    public Builder addOSGIBundle(
      final Path path)
    {
      this.osgiBundles.add(
        Objects.requireNonNull(path, "path")
          .toAbsolutePath()
      );
      return this;
    }


    /**
     * Add a sunburst package.
     *
     * @param pack The path
     *
     * @return this
     */

    public Builder addSunburstPackage(
      final SBPackageIdentifier pack)
    {
      this.sunburstPackages.add(
        Objects.requireNonNull(pack, "pack")
      );
      return this;
    }

    /**
     * @return The configuration
     */

    public RSLauncherConfiguration build()
    {
      return new RSLauncherConfiguration(
        this.runtimeDirectory,
        List.copyOf(this.javaModules),
        List.copyOf(this.osgiBundles),
        Set.copyOf(this.sunburstPackages),
        Map.copyOf(this.parameters)
      );
    }

    /**
     * Set the run-time directory.
     *
     * @param path The path
     *
     * @return this
     */

    public Builder setRuntimeDirectory(
      final Path path)
    {
      this.runtimeDirectory =
        Objects.requireNonNull(path, "path")
          .toAbsolutePath();
      return this;
    }
  }
}
