package org.calypsonet.keyple.demo.reload.remote.di

import dagger.Module
import dagger.Provides
import org.calypsonet.keyple.demo.reload.remote.data.KeypopApiProviderImpl
import org.calypsonet.keyple.demo.reload.remote.di.scopes.AppScoped
import org.calypsonet.keyple.demo.reload.remote.domain.spi.KeypopApiProvider

@Suppress("unused")
@Module
class KeypopApiModule {

    @Provides
    @AppScoped
    fun provideKeypopApiProvider(): KeypopApiProvider = KeypopApiProviderImpl()
}