package org.calypsonet.keyple.demo.control.di

import dagger.Module
import dagger.Provides
import org.calypsonet.keyple.demo.control.data.KeypopApiProviderImpl
import org.calypsonet.keyple.demo.control.di.scope.AppScoped
import org.calypsonet.keyple.demo.control.domain.spi.KeypopApiProvider

@Suppress("unused")
@Module
class KeypopApiModule {

    @Provides @AppScoped
    fun provideKeypopApiProvider(): KeypopApiProvider = KeypopApiProviderImpl()
}