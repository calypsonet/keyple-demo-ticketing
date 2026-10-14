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
package org.calypsonet.keyple.demo.validation.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.calypsonet.keyple.demo.validation.domain.CardReaderSession
import org.calypsonet.keyple.demo.validation.domain.procedures.CalypsoCardValidationProcedure
import org.calypsonet.keyple.demo.validation.domain.procedures.StorageCardValidationProcedure
import org.calypsonet.keyple.demo.validation.domain.spi.AppSettingsRepository
import org.calypsonet.keyple.demo.validation.domain.spi.Clock
import org.calypsonet.keyple.demo.validation.domain.spi.KeypopApiProvider
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.calypsonet.keyple.demo.validation.domain.spi.TerminalReaders
import org.calypsonet.keyple.demo.validation.domain.spi.UserFeedback
import org.calypsonet.keyple.demo.validation.domain.usecases.ValidateCard

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
  fun provideValidateCard(
      cardReaderSession: CardReaderSession,
      keypopApiProvider: KeypopApiProvider,
      appSettings: AppSettingsRepository,
      clock: Clock,
      userFeedback: UserFeedback,
      logger: Logger
  ): ValidateCard =
      ValidateCard(
          cardReaderSession,
          appSettings,
          clock,
          userFeedback,
          logger,
          // Validation procedures of the supported card technologies
          listOf(
              CalypsoCardValidationProcedure(keypopApiProvider, logger),
              StorageCardValidationProcedure(keypopApiProvider, logger)))
}
