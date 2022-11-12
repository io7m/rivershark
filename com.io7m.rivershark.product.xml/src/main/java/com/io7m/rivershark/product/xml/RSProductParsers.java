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

import com.io7m.anethum.common.ParseException;
import com.io7m.anethum.common.ParseSeverity;
import com.io7m.anethum.common.ParseStatus;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.rivershark.product.RSArtifactDependencyJPMS;
import com.io7m.rivershark.product.RSArtifactDependencyOSGi;
import com.io7m.rivershark.product.RSArtifactDependencySunburst;
import com.io7m.rivershark.product.RSArtifactDependencyType;
import com.io7m.rivershark.product.RSHash;
import com.io7m.rivershark.product.RSHashAlgorithm;
import com.io7m.rivershark.product.RSProduct;
import com.io7m.rivershark.product.xml.jaxb.Dependencies;
import com.io7m.rivershark.product.xml.jaxb.Hash;
import com.io7m.rivershark.product.xml.jaxb.HashAlgorithmT;
import com.io7m.rivershark.product.xml.jaxb.JPMSModule;
import com.io7m.rivershark.product.xml.jaxb.Metadata;
import com.io7m.rivershark.product.xml.jaxb.OSGIBundle;
import com.io7m.rivershark.product.xml.jaxb.Product;
import com.io7m.rivershark.product.xml.jaxb.SunburstPackage;
import com.io7m.verona.core.Version;
import com.io7m.verona.core.VersionQualifier;
import jakarta.xml.bind.JAXBContext;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static jakarta.xml.bind.ValidationEvent.ERROR;
import static jakarta.xml.bind.ValidationEvent.FATAL_ERROR;
import static jakarta.xml.bind.ValidationEvent.WARNING;

/**
 * The default factory of product parsers.
 */

public final class RSProductParsers
  implements RSProductParserFactoryType
{
  /**
   * The default factory of product parsers.
   */

  public RSProductParsers()
  {

  }

  @Override
  public RSProductParserType createParserWithContext(
    final Void context,
    final URI source,
    final InputStream stream,
    final Consumer<ParseStatus> statusConsumer)
  {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(statusConsumer, "statusConsumer");

    return new Parser(source, stream, statusConsumer);
  }

  private static final class Parser implements RSProductParserType
  {
    private static final HexFormat HEX_FORMAT =
      HexFormat.of()
        .withUpperCase();

    private final URI source;
    private final InputStream stream;
    private final Consumer<ParseStatus> statusConsumer;

    Parser(
      final URI inSource,
      final InputStream inStream,
      final Consumer<ParseStatus> inStatusConsumer)
    {
      this.source =
        Objects.requireNonNull(inSource, "source");
      this.stream =
        Objects.requireNonNull(inStream, "stream");
      this.statusConsumer =
        Objects.requireNonNull(inStatusConsumer, "statusConsumer");
    }

    private static RSProduct processProduct(
      final Product product)
    {
      return new RSProduct(
        product.getID(),
        processVersion(product.getVersion()),
        processMetadata(product.getMetadata()),
        processDependencies(product.getDependencies())
      );
    }

    private static Map<String, String> processMetadata(
      final Metadata metadata)
    {
      final var results = new HashMap<String, String>();
      for (final var data : metadata.getMeta()) {
        results.put(data.getKey(), data.getValue());
      }
      return Map.copyOf(results);
    }

    private static List<RSArtifactDependencyType> processDependencies(
      final Dependencies dependencies)
    {
      final var results = new ArrayList<RSArtifactDependencyType>();
      for (final var dep : dependencies.getOSGIBundleOrJPMSModuleOrSunburstPackage()) {
        if (dep instanceof OSGIBundle osgi) {
          results.add(processOSGIBundle(osgi));
        } else if (dep instanceof JPMSModule jpms) {
          results.add(processJPMSModule(jpms));
        } else if (dep instanceof SunburstPackage sunburst) {
          results.add(processSunburstPackage(sunburst));
        } else {
          throw new UnreachableCodeException();
        }
      }
      return List.copyOf(results);
    }

    private static RSArtifactDependencyType processSunburstPackage(
      final SunburstPackage sunburst)
    {
      return new RSArtifactDependencySunburst(
        sunburst.getName(),
        processVersion(sunburst.getVersion()),
        processHashOptional(sunburst.getHash())
      );
    }

    private static Optional<RSHash> processHashOptional(
      final Hash hash)
    {
      if (hash == null) {
        return Optional.empty();
      }

      return Optional.of(
        new RSHash(
          processHashAlgorithm(hash.getAlgorithm()),
          processHashValue(hash.getValue())
        )
      );
    }

    private static byte[] processHashValue(
      final String value)
    {
      return HEX_FORMAT.parseHex(value);
    }

    private static RSHashAlgorithm processHashAlgorithm(
      final HashAlgorithmT algorithm)
    {
      return switch (algorithm) {
        case SHA_2_256 -> RSHashAlgorithm.SHA2_256;
      };
    }

    private static RSArtifactDependencyType processJPMSModule(
      final JPMSModule jpms)
    {
      return new RSArtifactDependencyJPMS(
        jpms.getGroup(),
        jpms.getName(),
        processVersion(jpms.getVersion()),
        processHashOptional(jpms.getHash())
      );
    }

    private static RSArtifactDependencyType processOSGIBundle(
      final OSGIBundle osgi)
    {
      return new RSArtifactDependencyOSGi(
        osgi.getGroup(),
        osgi.getName(),
        processVersion(osgi.getVersion()),
        processHashOptional(osgi.getHash())
      );
    }

    private static Version processVersion(
      final com.io7m.rivershark.product.xml.jaxb.Version version)
    {
      final var v = version.getQualifier();
      final Optional<VersionQualifier> q;
      if (v != null) {
        q = Optional.of(new VersionQualifier(v));
      } else {
        q = Optional.empty();
      }
      return new Version(
        (int) version.getMajor(),
        (int) version.getMinor(),
        (int) version.getPatch(),
        q
      );
    }

    @Override
    public RSProduct execute()
      throws ParseException
    {
      final var errors = new ArrayList<ParseStatus>();

      try {
        final var schemas =
          SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final var schema =
          schemas.newSchema(
            RSProductParsers.class.getResource(
              "/com/io7m/rivershark/product/xml/product-1.xsd")
          );

        final var context =
          JAXBContext.newInstance(
            "com.io7m.rivershark.product.xml.jaxb");
        final var unmarshaller =
          context.createUnmarshaller();

        unmarshaller.setEventHandler(event -> {
          try {
            final var status =
              ParseStatus.builder()
                .setErrorCode("xml")
                .setMessage(event.getMessage())
                .setSeverity(
                  switch (event.getSeverity()) {
                    case WARNING -> ParseSeverity.PARSE_WARNING;
                    case ERROR -> ParseSeverity.PARSE_ERROR;
                    case FATAL_ERROR -> ParseSeverity.PARSE_ERROR;
                    default -> ParseSeverity.PARSE_ERROR;
                  })
                .setLexical(LexicalPosition.of(
                  event.getLocator().getLineNumber(),
                  event.getLocator().getColumnNumber(),
                  Optional.of(event.getLocator().getURL().toURI())
                )).build();

            errors.add(status);
            this.statusConsumer.accept(status);
          } catch (final URISyntaxException e) {
            // Nothing we can do about it
          }
          return true;
        });
        unmarshaller.setSchema(schema);

        final var streamSource =
          new StreamSource(this.stream, this.source.toString());

        return processProduct((Product) unmarshaller.unmarshal(streamSource));
      } catch (final Exception e) {
        throw new ParseException("Parsing failed.", List.copyOf(errors));
      }
    }

    @Override
    public void close()
      throws IOException
    {
      this.stream.close();
    }
  }
}
