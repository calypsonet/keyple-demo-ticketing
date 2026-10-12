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
package org.calypsonet.keyple.demo.control.data

import org.calypsonet.keyple.demo.control.domain.spi.Logger
import timber.log.Timber

/**
 * [Logger] adapter based on Timber.
 *
 * Each message is tagged with the simple name of the class calling the logger. Otherwise, Timber
 * would tag all the messages with the name of this adapter, the class calling Timber.
 */
class TimberLogger : Logger {

  override fun d(message: String) {
    Timber.tag(callerTag()).d(message)
  }

  override fun i(message: String) {
    Timber.tag(callerTag()).i(message)
  }

  override fun e(message: String, throwable: Throwable?) {
    Timber.tag(callerTag()).e(throwable, message)
  }

  override fun w(message: String, throwable: Throwable?) {
    Timber.tag(callerTag()).w(throwable, message)
  }

  /**
   * Returns the simple name of the first class of the call stack outside of the logger (this
   * adapter and the port, which holds the default arguments), without the suffix of the nested or
   * anonymous classes (e.g. lambdas, coroutines).
   */
  private fun callerTag(): String {
    val caller =
        Throwable().stackTrace.firstOrNull {
          it.className != TimberLogger::class.java.name &&
              it.className != Logger::class.java.name &&
              !it.className.startsWith(Logger::class.java.name + "$")
        } ?: return TAG_UNKNOWN
    return caller.className.substringAfterLast('.').substringBefore('$')
  }

  private companion object {
    const val TAG_UNKNOWN = "Unknown"
  }
}
