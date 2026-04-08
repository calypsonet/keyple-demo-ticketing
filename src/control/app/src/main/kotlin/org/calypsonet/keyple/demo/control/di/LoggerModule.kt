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
package org.calypsonet.keyple.demo.control.di

import dagger.Module
import dagger.Provides
import org.calypsonet.keyple.demo.control.di.scope.AppScoped
import org.calypsonet.keyple.demo.control.domain.spi.Logger
import org.calypsonet.keyple.demo.control.ui.adapters.LoggerImpl

@Suppress("unused")
@Module
class LoggerModule {

  @Provides @AppScoped fun provideLogger(): Logger = LoggerImpl()
}
