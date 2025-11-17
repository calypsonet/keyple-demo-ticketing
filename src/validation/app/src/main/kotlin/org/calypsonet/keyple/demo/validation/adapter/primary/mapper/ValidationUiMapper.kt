package org.calypsonet.keyple.demo.validation.adapter.primary.mapper

import org.calypsonet.keyple.demo.validation.data.model.CardReaderResponse
import org.calypsonet.keyple.demo.validation.data.model.Location as UiLocation
import org.calypsonet.keyple.demo.validation.data.model.Status as UiStatus
import org.calypsonet.keyple.demo.validation.data.model.Validation as UiValidation
import org.calypsonet.keyple.demo.validation.domain.model.Location as DomainLocation
import org.calypsonet.keyple.demo.validation.domain.model.ValidationEvent as DomainValidationEvent
import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.domain.model.ValidationStatus

/**
 * Mapper for converting between domain models and UI models.
 * UI models include Android-specific features like Parcelable.
 */
object ValidationUiMapper {

    /**
     * Convert domain ValidationResult to UI CardReaderResponse.
     */
    fun toCardReaderResponse(validationResult: ValidationResult): CardReaderResponse {
        return CardReaderResponse(
            status = validationResult.status.toUiStatus(),
            cardType = validationResult.cardType.displayName,
            nbTicketsLeft = validationResult.nbTicketsLeft,
            contract = validationResult.contractName,
            validation = validationResult.validation?.toUiValidation(),
            eventDateTime = validationResult.eventDateTime,
            passValidityEndDate = validationResult.passValidityEndDate,
            errorMessage = validationResult.errorMessage
        )
    }

    /**
     * Convert domain ValidationStatus to UI Status.
     */
    private fun ValidationStatus.toUiStatus(): UiStatus {
        return when (this) {
            ValidationStatus.LOADING -> UiStatus.LOADING
            ValidationStatus.SUCCESS -> UiStatus.SUCCESS
            ValidationStatus.INVALID_CARD -> UiStatus.INVALID_CARD
            ValidationStatus.EMPTY_CARD -> UiStatus.EMPTY_CARD
            ValidationStatus.ERROR -> UiStatus.ERROR
        }
    }

    /**
     * Convert domain ValidationEvent to UI Validation.
     */
    private fun DomainValidationEvent.toUiValidation(): UiValidation {
        return UiValidation(
            name = this.name,
            location = this.location.toUiLocation(),
            destination = this.destination,
            dateTime = this.dateTime,
            provider = this.provider
        )
    }

    /**
     * Convert domain Location to UI Location (Parcelable).
     */
    fun DomainLocation.toUiLocation(): UiLocation {
        return UiLocation(id = this.id, name = this.name)
    }

    /**
     * Convert UI Location to domain Location.
     */
    fun UiLocation.toDomainLocation(): DomainLocation {
        return DomainLocation(id = this.id, name = this.name)
    }
}
