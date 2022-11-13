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


package com.io7m.rivershark.obrcontrol.api;

import java.nio.file.Path;
import java.util.Objects;

/**
 * An immutable repository configuration.
 */

public final class RSRepositoryConfiguration
{
  private final Path directory;
  private final String name;

  private RSRepositoryConfiguration(
    final Path inDirectory,
    final String inName)
  {
    this.directory =
      Objects.requireNonNull(inDirectory, "directory");
    this.name =
      Objects.requireNonNull(inName, "name");
  }

  /**
   * Create a new repository configuration builder.
   *
   * @param directory The directory that will hold the repository
   *
   * @return A new builder
   */

  public static Builder builder(
    final Path directory)
  {
    return new Builder(
      directory.toAbsolutePath()
    );
  }

  /**
   * @return The base repository directory
   */

  public Path directory()
  {
    return this.directory;
  }

  /**
   * @return The name of the repository that will be placed into the index file
   */

  public String name()
  {
    return this.name;
  }

  /**
   * A mutable repository configuration builder.
   */

  public static final class Builder
  {
    private Path directory;
    private String name;

    private Builder(
      final Path inDirectory)
    {
      this.directory =
        Objects.requireNonNull(inDirectory, "directory");
      this.name =
        "Rivershark";
    }

    /**
     * Set the base repository directory.
     *
     * @param inDirectory The directory
     *
     * @return this
     */

    public Builder setDirectory(
      final Path inDirectory)
    {
      this.directory = Objects.requireNonNull(inDirectory, "directory");
      return this;
    }

    /**
     * Set the repository name.
     *
     * @param inName The name
     *
     * @return this
     */

    public Builder setName(
      final String inName)
    {
      this.name = Objects.requireNonNull(inName, "name");
      return this;
    }

    /**
     * @return The repository configuration
     */

    public RSRepositoryConfiguration build()
    {
      return new RSRepositoryConfiguration(
        this.directory,
        this.name
      );
    }
  }
}
