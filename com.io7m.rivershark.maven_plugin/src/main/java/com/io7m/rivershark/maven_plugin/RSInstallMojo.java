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

package com.io7m.rivershark.maven_plugin;

import com.io7m.rivershark.obrcontrol.RSRepositories;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryConfiguration;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;

import java.nio.file.Paths;
import java.util.Map;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

/**
 * The "install" mojo.
 */

@Mojo(name = "installOBR", requiresDependencyResolution = RUNTIME)
public final class RSInstallMojo extends AbstractMojo
{
  /**
   * Access to the Maven project.
   */

  @Parameter(
    defaultValue = "${project}",
    required = true,
    readonly = true)
  private MavenProject project;

  /**
   * Access to the Maven session.
   */

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Component
  private DependencyGraphBuilder dependencyGraphBuilder;

  @Parameter(
    name = "repositoryDirectory",
    required = true,
    property = "rivershark.repositoryDirectory")
  private String repositoryDirectory;

  @Parameter(
    name = "repositoryName",
    required = false,
    property = "rivershark.repositoryName")
  private String repositoryName;

  /**
   * The "install" mojo.
   */

  public RSInstallMojo()
  {

  }

  @Override
  public void execute()
    throws MojoExecutionException
  {
    try {
      final var log =
        this.getLog();

      final var collector =
        new RSDependencyCollector(
          log,
          this.dependencyGraphBuilder,
          this.project,
          this.session
        );

      final var artifacts = collector.collect();
      log.info("collected %d dependencies".formatted(artifacts.size()));

      this.installArtifacts(artifacts);
    } catch (final DependencyGraphBuilderException | RSRepositoryException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void installArtifacts(
    final Map<String, Artifact> artifacts)
    throws RSRepositoryException
  {
    final var log =
      this.getLog();

    final var configurationBuilder =
      RSRepositoryConfiguration.builder(Paths.get(this.repositoryDirectory));

    if (this.repositoryName != null) {
      configurationBuilder.setName(this.repositoryName);
    }

    final var configuration =
      configurationBuilder.build();

    final var repositories = new RSRepositories();
    try (var repository = repositories.open(configuration)) {
      final var files =
        artifacts.values()
        .stream()
        .map(a -> a.getFile().toPath())
        .toList();

      for (final var file : files) {
        log.info("install %s".formatted(file));
      }

      repository.install(files);
    }
  }
}
