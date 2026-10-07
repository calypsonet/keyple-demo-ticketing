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
package org.calypsonet.keyple.demo.reload.remote.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.calypsonet.keyple.demo.reload.remote.data.ReaderManagerImpl
import org.calypsonet.keyple.demo.reload.remote.data.RemoteServiceManagerImpl
import org.calypsonet.keyple.demo.reload.remote.data.network.KeypleSyncEndPointClient
import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import org.calypsonet.keyple.demo.reload.remote.domain.spi.ReaderManager
import org.calypsonet.keyple.demo.reload.remote.domain.spi.RemoteServiceManager
import org.eclipse.keyple.core.service.SmartCardServiceProvider
import org.eclipse.keyple.distributed.LocalServiceClient
import org.eclipse.keyple.distributed.LocalServiceClientFactoryBuilder

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
class ReaderModule {

  @Provides
  @Singleton
  fun provideLocalServiceClient(
      keypleSyncEndPointClient: KeypleSyncEndPointClient
  ): LocalServiceClient {
    val smartCardService = SmartCardServiceProvider.getService()
    val localService =
        smartCardService.getDistributedLocalService("localService")
            ?: smartCardService.registerDistributedLocalService(
                LocalServiceClientFactoryBuilder.builder("localService")
                    .withSyncNode(keypleSyncEndPointClient)
                    .build())
    return localService.getExtension(LocalServiceClient::class.java)
  }

  @Provides
  @Singleton
  fun provideReaderManager(logger: Logger): ReaderManager {
    return ReaderManagerImpl(logger)
  }

  @Provides
  @Singleton
  fun provideRemoteServiceManager(localServiceClient: LocalServiceClient): RemoteServiceManager {
    return RemoteServiceManagerImpl(localServiceClient)
  }
}
