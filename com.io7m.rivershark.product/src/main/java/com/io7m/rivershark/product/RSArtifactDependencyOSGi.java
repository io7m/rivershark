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

package com.io7m.rivershark.product;

import com.io7m.verona.core.Version;

import java.util.Objects;
import java.util.Optional;

/**
 * A reference to an OSGi bundle.
 *
 * @param group    The group ID
 * @param artifact The artifact ID
 * @param version  The version
 * @param hash     The optional hash value
 */

public record RSArtifactDependencyOSGi(
  String group,
  String artifact,
  Version version,
  Optional<RSHash> hash)
  implements RSArtifactDependencyType
{
  /**
   * A reference to an OSGi bundle.
   *
   * @param group    The group ID
   * @param artifact The artifact ID
   * @param version  The version
   * @param hash     The optional hash value
   */

  public RSArtifactDependencyOSGi
  {
    Objects.requireNonNull(group, "group");
    Objects.requireNonNull(artifact, "artifact");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(hash, "hash");

    RSNames.check(group);
    RSNames.check(artifact);
  }
}
