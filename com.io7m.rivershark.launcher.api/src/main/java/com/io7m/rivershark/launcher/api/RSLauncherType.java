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

package com.io7m.rivershark.launcher.api;

/**
 * A launcher.
 */

public interface RSLauncherType extends AutoCloseable
{
  /**
   * @return The launcher configuration
   */

  RSLauncherConfiguration configuration();

  /**
   * Start the launcher.
   *
   * @throws Exception On errors
   */

  void run()
    throws Exception;

  /**
   * Wait for the launcher to stop. The method will return after {@code timeout}
   * milliseconds if the launcher does not stop. The method will wait
   * indefinitely if {@code timeout} is 0.
   *
   * @param timeout The number of milliseconds to wait
   *
   * @throws Exception On errors
   */

  void waitForStop(long timeout)
    throws Exception;
}
