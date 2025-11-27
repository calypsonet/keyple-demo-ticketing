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
 * Simple logging abstraction for the domain.
 *
 * This interface allows the domain layer to log messages without depending on a concrete logging
 * framework or platform API. Implementations can delegate to Android Logcat, SLF4J, or any other
 * logging facility.
 */
interface Logger {

  /**
   * Logs an informational message.
   *
   * @param message Human-readable message to log.
   */
  fun i(message: String)

  /**
   * Logs an error message.
   *
   * @param message Human-readable message describing the error.
   * @param throwable Optional associated exception.
   */
  fun e(message: String, throwable: Throwable? = null)
}
