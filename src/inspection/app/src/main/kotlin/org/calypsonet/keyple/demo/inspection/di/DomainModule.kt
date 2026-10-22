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
package org.calypsonet.keyple.demo.inspection.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.calypsonet.keyple.demo.inspection.domain.CardReaderSession
import org.calypsonet.keyple.demo.inspection.domain.procedures.CalypsoCardInspectionProcedure
import org.calypsonet.keyple.demo.inspection.domain.procedures.StorageCardInspectionProcedure
import org.calypsonet.keyple.demo.inspection.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.inspection.domain.spi.Clock
import org.calypsonet.keyple.demo.inspection.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.inspection.domain.spi.Logger
import org.calypsonet.keyple.demo.inspection.domain.spi.TerminalReaders
import org.calypsonet.keyple.demo.inspection.domain.spi.UserFeedback
import org.calypsonet.keyple.demo.inspection.domain.usecases.InspectCard

/** Provides the domain services, which carry no dependency injection annotation. */
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class DomainModule {

  @Provides
  @Singleton
  fun provideCardReaderSession(
      keypopApiProvider: KeypopApiProvider,
      terminalReaders: TerminalReaders,
      userFeedback: UserFeedback,
      logger: Logger
  ): CardReaderSession = CardReaderSession(keypopApiProvider, terminalReaders, userFeedback, logger)

  @Provides
  @Singleton
  fun provideInspectCard(
      cardReaderSession: CardReaderSession,
      keypopApiProvider: KeypopApiProvider,
      appSettings: AppSettingsRepository,
      clock: Clock,
      userFeedback: UserFeedback,
      logger: Logger
  ): InspectCard =
      InspectCard(
          cardReaderSession,
          appSettings,
          clock,
          userFeedback,
          logger,
          // Inspection procedures of the supported card technologies
          listOf(
              CalypsoCardInspectionProcedure(keypopApiProvider, logger),
              StorageCardInspectionProcedure(keypopApiProvider, logger)))
}
