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


package com.io7m.rivershark.product.xml;

import com.io7m.anethum.common.SerializeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rivershark.product.RSArtifactDependencyJPMS;
import com.io7m.rivershark.product.RSArtifactDependencyOSGi;
import com.io7m.rivershark.product.RSArtifactDependencySunburst;
import com.io7m.rivershark.product.RSArtifactDependencyType;
import com.io7m.rivershark.product.RSHash;
import com.io7m.rivershark.product.RSProduct;
import com.io7m.rivershark.product.xml.jaxb.Dependencies;
import com.io7m.rivershark.product.xml.jaxb.Hash;
import com.io7m.rivershark.product.xml.jaxb.HashAlgorithmT;
import com.io7m.rivershark.product.xml.jaxb.JPMSModule;
import com.io7m.rivershark.product.xml.jaxb.Metadata;
import com.io7m.rivershark.product.xml.jaxb.OSGIBundle;
import com.io7m.rivershark.product.xml.jaxb.ObjectFactory;
import com.io7m.rivershark.product.xml.jaxb.SunburstPackage;
import com.io7m.rivershark.product.xml.jaxb.Version;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

import static java.lang.Boolean.TRUE;
import static java.lang.Integer.toUnsignedLong;

/**
 * The default factory of product serializers.
 */

public final class RSProductSerializers
  implements RSProductSerializerFactoryType
{
  /**
   * The default factory of product serializers.
   */

  public RSProductSerializers()
  {

  }

  @Override
  public RSProductSerializerType createSerializerWithContext(
    final Void context,
    final URI target,
    final OutputStream stream)
  {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(stream, "stream");
    return new Serializer(target, stream);
  }

  private static final class Serializer implements RSProductSerializerType
  {
    private static final HexFormat HEX_FORMAT =
      HexFormat.of()
        .withUpperCase();

    private final URI target;
    private final OutputStream stream;
    private final ObjectFactory objects;

    private Serializer(
      final URI inTarget,
      final OutputStream inStream)
    {
      this.target =
        Objects.requireNonNull(inTarget, "target");
      this.stream =
        Objects.requireNonNull(inStream, "stream");
      this.objects =
        new ObjectFactory();
    }

    @Override
    public void execute(
      final RSProduct value)
      throws SerializeException
    {
      try {
        final var p =
          this.objects.createProduct();

        p.setID(value.id());
        p.setVersion(
          this.processVersion(value.version()));
        p.setMetadata(
          this.processMetadata(value.metadata()));
        p.setDependencies(
          this.processDependencies(value.artifactDependencies()));

        final var context =
          JAXBContext.newInstance("com.io7m.rivershark.product.xml.jaxb");
        final var marshaller =
          context.createMarshaller();

        marshaller.setProperty("jaxb.formatted.output", TRUE);
        marshaller.marshal(p, this.stream);
      } catch (final JAXBException e) {
        throw new SerializeException(e.getMessage(), e);
      }
    }

    private Dependencies processDependencies(
      final Iterable<RSArtifactDependencyType> dependencies)
    {
      final var d =
        this.objects.createDependencies();
      final var output =
        d.getOSGIBundleOrJPMSModuleOrSunburstPackage();

      for (final var dep : dependencies) {
        output.add(this.processDependency(dep));
      }
      return d;
    }

    private Object processDependency(
      final RSArtifactDependencyType dep)
    {
      if (dep instanceof RSArtifactDependencyJPMS jpms) {
        return this.processDependencyJPMS(jpms);
      }
      if (dep instanceof RSArtifactDependencyOSGi osgi) {
        return this.processDependencyOSGi(osgi);
      }
      if (dep instanceof RSArtifactDependencySunburst sunburst) {
        return this.processDependencySunburst(sunburst);
      }

      throw new UnreachableCodeException();
    }

    private SunburstPackage processDependencySunburst(
      final RSArtifactDependencySunburst sunburst)
    {
      final var r = this.objects.createSunburstPackage();
      r.setName(sunburst.name());
      r.setVersion(this.processVersion(sunburst.version()));
      sunburst.hash().ifPresent(h -> r.setHash(this.processHash(h)));
      return r;
    }

    private OSGIBundle processDependencyOSGi(
      final RSArtifactDependencyOSGi osgi)
    {
      final var r = this.objects.createOSGIBundle();
      r.setGroup(osgi.group());
      r.setName(osgi.artifact());
      r.setVersion(this.processVersion(osgi.version()));
      osgi.hash().ifPresent(h -> r.setHash(this.processHash(h)));
      return r;
    }

    private JPMSModule processDependencyJPMS(
      final RSArtifactDependencyJPMS jpms)
    {
      final var r = this.objects.createJPMSModule();
      r.setGroup(jpms.group());
      r.setName(jpms.artifact());
      r.setVersion(this.processVersion(jpms.version()));
      jpms.hash().ifPresent(h -> r.setHash(this.processHash(h)));
      return r;
    }

    private Hash processHash(
      final RSHash h)
    {
      final var r = this.objects.createHash();
      r.setAlgorithm(
        switch (h.algorithm()) {
          case SHA2_256 -> HashAlgorithmT.SHA_2_256;
        }
      );
      r.setValue(HEX_FORMAT.formatHex(h.value()));
      return r;
    }

    private Metadata processMetadata(
      final Map<String, String> metadata)
    {
      final var m = this.objects.createMetadata();
      final var ms = m.getMeta();

      final var entries =
        metadata.entrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey());

      for (final var entry : entries.toList()) {
        final var v = this.objects.createMeta();
        v.setKey(entry.getKey());
        v.setValue(entry.getValue());
        ms.add(v);
      }

      return m;
    }

    private Version processVersion(
      final com.io7m.verona.core.Version version)
    {
      final var r = this.objects.createVersion();
      r.setMajor(toUnsignedLong(version.major()));
      r.setMinor(toUnsignedLong(version.minor()));
      r.setPatch(toUnsignedLong(version.patch()));

      final var q = version.qualifier();
      q.ifPresent(qualifier -> r.setQualifier(qualifier.text()));
      return r;
    }

    @Override
    public void close()
      throws IOException
    {
      this.stream.close();
    }
  }
}
