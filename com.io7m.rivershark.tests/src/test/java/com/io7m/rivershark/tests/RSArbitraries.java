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

import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.lanark.core.RDottedName;
import com.io7m.rivershark.product.RSArtifactDependencyJPMS;
import com.io7m.rivershark.product.RSArtifactDependencyOSGi;
import com.io7m.rivershark.product.RSArtifactDependencySunburst;
import com.io7m.rivershark.product.RSArtifactDependencyType;
import com.io7m.rivershark.product.RSHash;
import com.io7m.rivershark.product.RSHashAlgorithm;
import com.io7m.rivershark.product.RSProduct;
import com.io7m.verona.core.Version;
import com.io7m.verona.core.VersionQualifier;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class RSArbitraries implements ArbitraryProvider
{
  private static final Set<Class<?>> CLASSES = Set.of(
    RSHash.class,
    RSArtifactDependencyType.class,
    RSProduct.class
  );

  public RSArbitraries()
  {

  }

  @Override
  public boolean canProvideFor(
    final TypeUsage targetType)
  {
    return CLASSES.contains(targetType.getRawType());
  }

  @Override
  public Set<Arbitrary<?>> provideFor(
    final TypeUsage targetType,
    final SubtypeProvider subtypeProvider)
  {
    return Set.of(provide(targetType));
  }

  private static Arbitrary<?> provide(
    final TypeUsage targetType)
  {
    final Class<?> raw = targetType.getRawType();
    if (raw.equals(RSHash.class)) {
      return hashes();
    }
    if (raw.equals(RSArtifactDependencyType.class)) {
      return artifactDependencies();
    }
    if (raw.equals(RSProduct.class)) {
      return products();
    }

    throw new UnreachableCodeException();
  }

  private static Arbitrary<RSProduct> products()
  {
    return Combinators.combine(
      names(),
      versions(),
      metas(),
      artifactDependencies().list()
    ).as(RSProduct::new);
  }

  private static Arbitrary<Map<String, String>> metas()
  {
    return Combinators.combine(names(), names())
      .as(Map::entry)
      .list()
      .uniqueElements()
      .map(entries -> {
        final var m = new HashMap<String, String>();
        for (final var e : entries) {
          m.put(e.getKey(), e.getValue());
        }
        return Map.copyOf(m);
      });
  }

  private static Arbitrary<RSArtifactDependencyType> artifactDependencies()
  {
    return Arbitraries.integers()
      .between(0, 2)
      .flatMap(integer -> {
        return switch (integer.intValue()) {
          case 0 -> provideArtifactDependencyJPMS();
          case 1 -> provideArtifactDependencyOSGi();
          case 2 -> provideArtifactDependencySunburst();
          default -> throw new UnreachableCodeException();
        };
      });
  }

  private static Arbitrary<String> names()
  {
    return Arbitraries.defaultFor(RDottedName.class)
      .map(RDottedName::value);
  }

  private static Arbitrary<Version> versions()
  {
    return Combinators.combine(
      Arbitraries.integers()
        .between(0, 100),
      Arbitraries.integers()
        .between(0, 100),
      Arbitraries.integers()
        .between(0, 100),
      Arbitraries.strings()
        .ofMinLength(1)
        .ofMaxLength(8)
        .alpha()
        .optional()
    ).as((major, minor, patch, qual) -> {
      return new Version(
        major.intValue(),
        minor.intValue(),
        patch.intValue(),
        qual.map(VersionQualifier::new)
      );
    });
  }

  private static Arbitrary<RSArtifactDependencyType>
  provideArtifactDependencySunburst()
  {
    return Combinators.combine(
      names(),
      versions(),
      hashes().optional()
    ).as(RSArtifactDependencySunburst::new);
  }

  private static Arbitrary<RSArtifactDependencyType>
  provideArtifactDependencyOSGi()
  {
    return Combinators.combine(
      names(),
      names(),
      versions(),
      hashes().optional()
    ).as(RSArtifactDependencyOSGi::new);
  }

  private static Arbitrary<RSArtifactDependencyType>
  provideArtifactDependencyJPMS()
  {
    return Combinators.combine(
      names(),
      names(),
      versions(),
      hashes().optional()
    ).as(RSArtifactDependencyJPMS::new);
  }

  private static Arbitrary<RSHash> hashes()
  {
    return Combinators.combine(
      Arbitraries.bytes()
        .array(byte[].class)
        .ofMinSize(8)
        .ofMaxSize(32),
      Arbitraries.defaultFor(RSHashAlgorithm.class)
    ).as((bytes, algo) -> {
      return new RSHash(algo, bytes);
    });
  }
}
