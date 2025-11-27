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
package org.calypsonet.keyple.demo.validation.ui.adapters

import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import timber.log.Timber

class LoggerImpl : Logger {

  override fun i(message: String) {
    Timber.i(message)
  }

  override fun e(message: String, throwable: Throwable?) {
    Timber.e(throwable, message)
  }
}
