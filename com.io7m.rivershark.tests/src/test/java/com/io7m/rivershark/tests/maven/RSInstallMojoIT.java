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

package com.io7m.rivershark.tests.maven;

import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.extension.SystemProperty;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MavenJupiterExtension
public final class RSInstallMojoIT
{
  private static final Logger LOG =
    LoggerFactory.getLogger(RSInstallMojoIT.class);

  @MavenGoal("package")
  @MavenGoal("${project.groupId}:com.io7m.rivershark.maven_plugin:${project.version}:installOBR")
  @MavenTest
  void testEmpty(
    final MavenExecutionResult result)
    throws Exception
  {
    showOutput(result);
    assertTrue(result.isFailure());
  }

  @MavenGoal("package")
  @MavenGoal("${project.groupId}:com.io7m.rivershark.maven_plugin:${project.version}:installOBR")
  @SystemProperty(value = "rivershark.repositoryDirectory", content = "target/tmp-rivershark-repos")
  @MavenTest
  void testEmptyWithProperty(
    final MavenExecutionResult result)
    throws Exception
  {
    showOutput(result);
    assertTrue(result.isSuccessful());

    final var target =
      result.getMavenProjectResult()
        .getTargetProjectDirectory()
        .toPath()
        .resolve("target");

    assertTrue(
      Files.exists(
        target.resolve("tmp-rivershark-repos")
          .resolve("bundles")
          .resolve("com.io7m.tests-1.0.0.jar")
      )
    );
    assertTrue(
      Files.exists(
        target.resolve("tmp-rivershark-repos")
          .resolve("obr.xml")
      )
    );
  }

  @MavenGoal("package")
  @MavenGoal("${project.groupId}:com.io7m.rivershark.maven_plugin:${project.version}:installOBR")
  @SystemProperty(value = "rivershark.repositoryDirectory", content = "target/tmp-rivershark-repos")
  @MavenTest
  void testBasic(
    final MavenExecutionResult result)
    throws Exception
  {
    showOutput(result);
    assertTrue(result.isSuccessful());

    final var target =
      result.getMavenProjectResult()
        .getTargetProjectDirectory()
        .toPath()
        .resolve("target");

    final var expectedArtifacts =
      List.of(
        "com.io7m.calino.api-0.0.1.jar",
        "com.io7m.calino.parser.api-0.0.1.jar",
        "com.io7m.calino.supercompression.api-0.0.1.jar",
        "com.io7m.calino.supercompression.spi-0.0.1.jar",
        "com.io7m.calino.validation.api-0.0.1.jar",
        "com.io7m.calino.vanilla-0.0.1.jar",
        "com.io7m.calino.writer.api-0.0.1.jar",
        "com.io7m.cedarbridge.errors-0.0.10.jar",
        "com.io7m.cedarbridge.exprsrc.api-0.0.10.jar",
        "com.io7m.cedarbridge.schema.ast-0.0.10.jar",
        "com.io7m.cedarbridge.schema.binder.api-0.0.10.jar",
        "com.io7m.cedarbridge.schema.compiled-0.0.10.jar",
        "com.io7m.cedarbridge.schema.compiler-0.0.10.jar",
        "com.io7m.cedarbridge.schema.compiler.api-0.0.10.jar",
        "com.io7m.cedarbridge.schema.loader.api-0.0.10.jar",
        "com.io7m.cedarbridge.schema.names-0.0.10.jar",
        "com.io7m.cedarbridge.schema.parser.api-0.0.10.jar",
        "com.io7m.cedarbridge.schema.typer.api-0.0.10.jar",
        "com.io7m.cedarbridge.strings.api-0.0.10.jar",
        "com.io7m.jaffirm.core-4.0.0.jar",
        "com.io7m.jbssio.api-1.1.1.jar",
        "com.io7m.jeucreader.core-3.0.0.jar",
        "com.io7m.jlexing.core-3.0.0.jar",
        "com.io7m.jsx.core-3.0.1.jar",
        "com.io7m.jsx.parser.api-3.0.1.jar",
        "com.io7m.junreachable.core-4.0.0.jar",
        "com.io7m.jxtrand.api-1.1.0.jar",
        "com.io7m.jxtrand.vanilla-1.1.0.jar",
        "com.io7m.tests-1.0.0.jar",
        "com.io7m.wendover.core-0.0.3.jar",
        "slf4j.api-2.0.1.jar"
      );

    for (final var artifact : expectedArtifacts) {
      assertTrue(
        Files.exists(
          target.resolve("tmp-rivershark-repos")
            .resolve("bundles")
            .resolve(artifact)
        )
      );
    }

    assertTrue(
      Files.exists(
        target.resolve("tmp-rivershark-repos")
          .resolve("obr.xml")
      )
    );
  }

  private static void showOutput(
    final MavenExecutionResult result)
    throws IOException
  {
    final var mavenLog = result.getMavenLog();
    LOG.trace("stdout: {}", Files.readString(mavenLog.getStdout()));
    LOG.trace("stderr: {}", Files.readString(mavenLog.getStderr()));
  }
}
