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
package org.calypsonet.keyple.demo.validation.data

import javax.inject.Inject
import org.calypsonet.keyple.demo.common.model.Location
import org.calypsonet.keyple.demo.validation.domain.model.ReaderType
import org.calypsonet.keyple.demo.validation.domain.spi.AppSettingsRepository

/** In-memory implementation of the application settings (not persisted). */
class AppSettingsRepositoryImpl @Inject constructor() : AppSettingsRepository {
  override lateinit var readerType: ReaderType
  override lateinit var location: Location
  override var batteryPowered: Boolean = true
}
