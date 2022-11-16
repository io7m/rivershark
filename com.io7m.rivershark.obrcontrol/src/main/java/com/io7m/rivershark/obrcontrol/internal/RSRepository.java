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


package com.io7m.rivershark.obrcontrol.internal;

import aQute.bnd.osgi.repository.SimpleIndexer;
import aQute.bnd.osgi.resource.ResourceBuilder;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryConfiguration;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryException;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryType;
import com.io7m.verona.core.Version;
import com.io7m.verona.core.VersionException;
import com.io7m.verona.core.VersionParser;
import org.osgi.resource.Resource;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * The default repository implementation.
 */

public final class RSRepository implements RSRepositoryType
{
  private static final StandardOpenOption[] OPTIONS = {
    CREATE, TRUNCATE_EXISTING, WRITE,
  };

  private final RSRepositoryConfiguration configuration;
  private final Path fileIndex;
  private final Path fileIndexTmp;
  private final Path fileLock;
  private final Path fileBundles;
  private final FileChannel channel;

  private RSRepository(
    final RSRepositoryConfiguration inConfiguration,
    final Path inFileIndex,
    final Path inFileIndexTmp,
    final Path inFileLock,
    final Path inFileBundles,
    final FileChannel inChannel)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.fileIndex =
      Objects.requireNonNull(inFileIndex, "fileIndex");
    this.fileIndexTmp =
      Objects.requireNonNull(inFileIndexTmp, "inFileIndexTmp");
    this.fileLock =
      Objects.requireNonNull(inFileLock, "fileLock");
    this.fileBundles =
      Objects.requireNonNull(inFileBundles, "fileBundles");
    this.channel =
      Objects.requireNonNull(inChannel, "channel");
  }

  /**
   * Create a new repository.
   *
   * @param configuration The configuration
   *
   * @return A repository
   *
   * @throws RSRepositoryException On errors
   */

  public static RSRepositoryType create(
    final RSRepositoryConfiguration configuration)
    throws RSRepositoryException
  {
    try {
      final var directory =
        configuration.directory();
      final var fileLock =
        directory.resolve("index.lock");
      final var fileIndex =
        directory.resolve("obr.xml");
      final var fileIndexTmp =
        directory.resolve("obr.xml.tmp");
      final var fileBundles =
        directory.resolve("bundles");

      Files.createDirectories(directory);

      final var channel =
        FileChannel.open(fileLock, OPTIONS);

      return new RSRepository(
        configuration,
        fileIndex,
        fileIndexTmp,
        fileLock,
        fileBundles,
        channel
      );
    } catch (final IOException e) {
      throw new RSRepositoryException(e.getMessage(), e);
    }
  }

  private static boolean isJarFile(
    final Path file)
  {
    return file.getFileName()
      .toString()
      .endsWith(".jar");
  }

  private static Map<Path, Identifier> checkBundles(
    final Collection<Path> files)
    throws RSRepositoryException
  {
    final var mapBundles =
      new HashMap<Path, Identifier>();
    final var exceptions =
      new ExceptionTracker<RSRepositoryException>();

    for (final var file : files) {
      try {
        final var builder = new ResourceBuilder();
        builder.addFile(file.toFile(), file.toUri());
        final var resource =
          builder.build();
        final var identifier =
          findIdentity(file, resource);

        mapBundles.put(file, identifier);
      } catch (final Exception e) {
        exceptions.addException(new RSRepositoryException(e.getMessage(), e));
      }
    }

    exceptions.throwIfNecessary();
    return mapBundles;
  }

  private static Identifier findIdentity(
    final Path file,
    final Resource resource)
    throws RSRepositoryException
  {
    final var caps =
      resource.getCapabilities("osgi.identity");

    String identifier = null;
    Version version = null;

    for (final var cap : caps) {
      for (final var entry : cap.getAttributes().entrySet()) {
        final var name =
          entry.getKey();
        final var valText =
          entry.getValue().toString();

        switch (name) {
          case "osgi.identity" -> {
            identifier = valText;
          }
          case "version" -> {
            try {
              version = VersionParser.parse(valText);
            } catch (final VersionException e) {
              throw new RSRepositoryException(
                String.format(
                  "File '%s' has unparseable version '%s': %s",
                  file,
                  valText,
                  e.getMessage()
                ),
                e
              );
            }
          }
          default -> {

          }
        }
      }
    }

    if (identifier != null && version != null) {
      return new Identifier(identifier, version);
    }

    throw new RSRepositoryException(
      String.format(
        "File '%s' is not an OSGi bundle (missing osgi.identity and/or version)",
        file)
    );
  }

  @Override
  public void install(
    final Collection<Path> files)
    throws RSRepositoryException
  {
    Objects.requireNonNull(files, "files");

    final var checked = checkBundles(files);

    try (var ignored = this.channel.lock()) {
      for (final var entry : checked.entrySet()) {
        this.installBundleFile(entry.getKey(), entry.getValue());
      }

      this.generateIndex();
    } catch (final IOException e) {
      throw new RSRepositoryException(e.getMessage(), e);
    }
  }

  private void generateIndex()
    throws RSRepositoryException
  {
    try (var stream = Files.list(this.fileBundles)) {
      final var jars =
        stream.filter(RSRepository::isJarFile)
          .map(Path::toFile)
          .toList();

      new SimpleIndexer()
        .name(this.configuration.name())
        .base(this.configuration.directory().toUri())
        .files(jars)
        .index(this.fileIndexTmp.toFile());

      Files.move(
        this.fileIndexTmp,
        this.fileIndex,
        ATOMIC_MOVE,
        REPLACE_EXISTING
      );
    } catch (final IOException e) {
      throw new RSRepositoryException(e.getMessage(), e);
    }
  }

  private void installBundleFile(
    final Path file,
    final Identifier value)
    throws IOException
  {
    final var name =
      String.format("%s-%s.jar", value.name, value.version.toString());
    final var nameTmp =
      String.format("%s-%s.jar.tmp", value.name, value.version);

    final var fileOutTmp =
      this.fileBundles.resolve(nameTmp);
    final var fileOut =
      this.fileBundles.resolve(name);

    Files.createDirectories(this.fileBundles);

    try (var output = Files.newOutputStream(fileOutTmp, OPTIONS)) {
      try (var input = Files.newInputStream(file)) {
        input.transferTo(output);
      }
      output.flush();
    }

    Files.move(fileOutTmp, fileOut, ATOMIC_MOVE, REPLACE_EXISTING);
  }

  @Override
  public void close()
  {

  }

  private record Identifier(
    String name,
    Version version)
  {

  }
}
