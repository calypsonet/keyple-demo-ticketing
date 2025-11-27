/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.validation.domain.spi

/**
 * Abstraction of a UI-specific context that can be adapted to platform types.
 *
 * Implementations wrap a platform/application context (for example, an Android Activity or
 * Application) and provide a generic way to obtain typed adapters without leaking concrete UI
 * dependencies into the domain layer.
 */
interface UiContext {

  /**
   * Returns an adapter view of this UI context for the requested type.
   * - On Android, typical adapters may include Activity, Context or FragmentManager.
   * - On desktop or server, this may return other toolkit-specific objects.
   *
   * Implementations should throw an exception if the requested adapter type is not supported.
   *
   * @param T The adapter type to retrieve.
   * @param adapter The Class object representing the adapter type.
   * @return An instance compatible with the requested adapter type.
   * @throws IllegalArgumentException if the requested adapter type is not supported.
   */
  fun <T> adaptTo(adapter: Class<T>): T
}
