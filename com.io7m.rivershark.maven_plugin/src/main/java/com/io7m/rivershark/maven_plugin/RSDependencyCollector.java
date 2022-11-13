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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A dependency collector.
 */

public final class RSDependencyCollector
{
  private final Log log;
  private final DependencyGraphBuilder dependencyGraphBuilder;
  private final HashMap<String, Artifact> results;
  private final MavenProject project;
  private final MavenSession session;

  /**
   * A dependency collector.
   *
   * @param inSession      The project session
   * @param inGraphBuilder The graph builder
   * @param inLog          The log
   * @param inProject      The project
   */

  public RSDependencyCollector(
    final Log inLog,
    final DependencyGraphBuilder inGraphBuilder,
    final MavenProject inProject,
    final MavenSession inSession)
  {
    this.log =
      Objects.requireNonNull(inLog, "log");
    this.dependencyGraphBuilder =
      Objects.requireNonNull(inGraphBuilder, "dependencyGraphBuilder");
    this.project =
      Objects.requireNonNull(inProject, "project");
    this.session =
      Objects.requireNonNull(inSession, "session");
    this.results =
      new HashMap<>();
  }

  /**
   * Collect all transitive artifacts.
   *
   * @return The artifacts
   *
   * @throws DependencyGraphBuilderException On errors
   */

  public Map<String, Artifact> collect()
    throws DependencyGraphBuilderException
  {
    this.results.clear();

    if (isDesirableArtifact(this.project.getArtifact())) {
      this.results.put(
        coordinatesOf(this.project.getArtifact()),
        this.project.getArtifact()
      );
    }

    for (final var artifact : this.project.getArtifacts()) {
      if (isDesirableArtifact(artifact)) {
        this.results.put(coordinatesOf(artifact), artifact);
      }
    }

    for (final var artifact : this.project.getAttachedArtifacts()) {
      if (isDesirableArtifact(artifact)) {
        this.results.put(coordinatesOf(artifact), artifact);
      }
    }

    final ProjectBuildingRequest request =
      new DefaultProjectBuildingRequest(
        this.session.getProjectBuildingRequest()
      );

    request.setProject(this.project);

    Objects.requireNonNull(request, "request");

    final var node =
      this.dependencyGraphBuilder.buildDependencyGraph(request, artifact -> {
        return Objects.equals(artifact.getScope(), Artifact.SCOPE_RUNTIME);
      });

    this.collectDependencies(node);
    return Map.copyOf(this.results);
  }

  private void collectDependencies(
    final DependencyNode node)
  {
    final var artifact = node.getArtifact();
    if (isDesirableArtifact(artifact)) {
      this.results.put(coordinatesOf(artifact), artifact);

      for (final DependencyNode child : node.getChildren()) {
        this.collectDependencies(child);
      }
    }
  }

  private static boolean isDesirableArtifact(
    final Artifact artifact)
  {
    return Objects.equals(artifact.getType(), "jar")
           || Objects.equals(artifact.getType(), "bundle");
  }

  private static String coordinatesOf(
    final Artifact artifact)
  {
    return String.format(
      "%s:%s:%s",
      artifact.getGroupId(),
      artifact.getArtifactId(),
      artifact.getVersion()
    );
  }
}
