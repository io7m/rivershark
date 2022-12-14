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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.rivershark.obrcontrol.RSRepositories;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "install-bundles" command.
 */

@Parameters(commandDescription = "Install OSGi bundles into an OBR.")
public final class RSCommandInstallBundles extends CLPAbstractCommand
{
  @Parameter(
    names = "--repository",
    required = true,
    description = "The repository directory.")
  private Path repository;

  @Parameter(
    names = "--file",
    required = false,
    description = "The bundle to install.")
  private List<Path> files = new ArrayList<Path>();

  @Parameter(
    names = "--name",
    required = false,
    description = "The repository title.")
  private String name;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public RSCommandInstallBundles(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    final var configurationBuilder =
      RSRepositoryConfiguration.builder(this.repository);

    if (this.name != null) {
      configurationBuilder.setName(this.name);
    }

    final var configuration =
      configurationBuilder.build();

    final var repositories = new RSRepositories();
    try (var repos = repositories.open(configuration)) {
      repos.install(
        this.files.stream()
          .map(Path::toAbsolutePath)
          .toList()
      );
    }
    return SUCCESS;
  }

  @Override
  public String name()
  {
    return "install-bundles";
  }
}
