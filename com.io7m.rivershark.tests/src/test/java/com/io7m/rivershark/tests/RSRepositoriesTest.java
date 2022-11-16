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

import com.io7m.rivershark.obrcontrol.RSRepositories;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryConfiguration;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryException;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RSRepositoriesTest
{
  private RSRepositories repositories;
  private Path directory;
  private RSRepositoryType repository;
  private Path reposDirectory;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory =
      RSTestDirectories.createTempDirectory();
    this.reposDirectory =
      this.directory.resolve("repos");

    this.repositories =
      new RSRepositories();
    this.repository =
      this.repositories.open(
        RSRepositoryConfiguration.builder(this.reposDirectory)
          .setDirectory(this.reposDirectory)
          .setName("Rivershark")
          .build()
      );
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.repository.close();
  }

  @Test
  public void testInstallNotOSGi0()
    throws Exception
  {
    final var file =
      RSTestDirectories.resourceOf(
        RSRepositoriesTest.class, this.directory, "sunflower.png");

    final var ex =
      assertThrows(RSRepositoryException.class, () -> {
        this.repository.install(List.of(file));
      });

    assertTrue(ex.getMessage().contains("missing osgi.identity"));
  }

  @Test
  public void testInstallNotOSGi1()
    throws Exception
  {
    final var file =
      RSTestDirectories.resourceOf(
        RSRepositoriesTest.class, this.directory, "empty.jar");

    final var ex =
      assertThrows(RSRepositoryException.class, () -> {
        this.repository.install(List.of(file));
      });

    assertTrue(ex.getMessage().contains("missing osgi.identity"));
  }

  @Test
  public void testInstallOK0()
    throws Exception
  {
    final var file =
      RSTestDirectories.resourceOf(
        RSRepositoriesTest.class,
        this.directory,
        "com.io7m.junreachable.core-4.0.0.jar");

    this.repository.install(List.of(file));

    assertTrue(
      Files.exists(this.reposDirectory.resolve("obr.xml"))
    );
  }
}
