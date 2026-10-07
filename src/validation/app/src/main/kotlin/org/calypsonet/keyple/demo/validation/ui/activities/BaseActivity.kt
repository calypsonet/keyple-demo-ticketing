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
package org.calypsonet.keyple.demo.validation.ui.activities

import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.domain.TicketingService
import org.calypsonet.keyple.demo.validation.domain.spi.AppSettingsRepository

abstract class BaseActivity : AppCompatActivity() {

  @Inject lateinit var ticketingService: TicketingService
  @Inject lateinit var appSettings: AppSettingsRepository
}
