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

package com.io7m.rivershark.obrcontrol;

import com.io7m.oatfield.vanilla.OFBundleIndexers;
import com.io7m.oatfield.vanilla.OFBundleReaders;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryConfiguration;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryException;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryFactoryType;
import com.io7m.rivershark.obrcontrol.api.RSRepositoryType;
import com.io7m.rivershark.obrcontrol.internal.RSRepository;

import java.util.Objects;

/**
 * The default repository implementation.
 */

public final class RSRepositories implements RSRepositoryFactoryType
{
  private final OFBundleIndexers indexers;
  private final OFBundleReaders readers;

  /**
   * The default repository implementation.
   */

  public RSRepositories()
  {
    this.indexers = new OFBundleIndexers();
    this.readers = new OFBundleReaders();
  }

  @Override
  public RSRepositoryType open(
    final RSRepositoryConfiguration configuration)
    throws RSRepositoryException
  {
    Objects.requireNonNull(configuration, "configuration");
    return RSRepository.create(configuration, this.indexers, this.readers);
  }
}
