/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.control.ui.adapters

import org.calypsonet.keyple.demo.control.domain.spi.Logger
import timber.log.Timber

class LoggerImpl : Logger {

  override fun d(message: String) {
    Timber.d(message)
  }

  override fun i(message: String) {
    Timber.i(message)
  }

  override fun e(message: String, throwable: Throwable?) {
    Timber.e(throwable, message)
  }

  override fun w(message: String, throwable: Throwable?) {
    Timber.w(throwable, message)
  }
}
