/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.validation.data

import android.content.Context

/** Mock UI manager feedback for Arrive terminals */
internal class ArriveUiManagerImpl(private val context: Context) : UiManager {
  override fun init(onReady: () -> Unit) {}

  override fun displayResultSuccess() {}

  override fun displayResultFailed() {}

  override fun displayWaiting() {}

  override fun release() {}
}
