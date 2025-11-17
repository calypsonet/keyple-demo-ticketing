package org.calypsonet.keyple.demo.validation.infrastructure.di

import dagger.Binds
import dagger.Module
import org.calypsonet.keyple.demo.validation.adapter.secondary.UiFeedbackAdapter
import org.calypsonet.keyple.demo.validation.adapter.secondary.repository.CardRepositoryFacade
import org.calypsonet.keyple.demo.validation.adapter.secondary.repository.LocationAdapter
import org.calypsonet.keyple.demo.validation.domain.port.output.CardRepository
import org.calypsonet.keyple.demo.validation.domain.port.output.LocationProvider
import org.calypsonet.keyple.demo.validation.domain.port.output.UiFeedbackPort
import org.calypsonet.keyple.demo.validation.infrastructure.di.scope.AppScoped

/**
 * Dagger module for binding adapter implementations to their ports (output interfaces).
 */
@Module
abstract class AdapterModule {

    @Binds
    @AppScoped
    abstract fun bindCardRepository(
        impl: CardRepositoryFacade
    ): CardRepository

    @Binds
    @AppScoped
    abstract fun bindLocationProvider(
        impl: LocationAdapter
    ): LocationProvider

    @Binds
    @AppScoped
    abstract fun bindUiFeedbackPort(
        impl: UiFeedbackAdapter
    ): UiFeedbackPort
}
