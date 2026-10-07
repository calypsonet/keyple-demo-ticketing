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
package org.calypsonet.keyple.demo.validation.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.calypsonet.keyple.demo.validation.data.ReaderManagerImpl
import org.calypsonet.keyple.demo.validation.domain.spi.Logger
import org.calypsonet.keyple.demo.validation.domain.spi.ReaderManager
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class ReaderModule {

  @Provides
  @Singleton
  fun provideReaderManager(
      cardReaderObservationExceptionHandlerSpi: CardReaderObservationExceptionHandlerSpi
  ): ReaderManager = ReaderManagerImpl(cardReaderObservationExceptionHandlerSpi)

  @Provides
  @Singleton
  fun provideCardReaderObservationExceptionHandlerSpi(
      logger: Logger
  ): CardReaderObservationExceptionHandlerSpi =
      CardReaderObservationExceptionHandlerSpi { pluginName, readerName, e ->
        logger.e("An unexpected reader error occurred: $pluginName:$readerName", e)
      }
}
