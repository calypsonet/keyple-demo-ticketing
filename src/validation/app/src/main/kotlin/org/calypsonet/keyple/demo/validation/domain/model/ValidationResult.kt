package org.calypsonet.keyple.demo.validation.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain model representing the result of a card validation (pure domain, no Android deps).
 */
data class ValidationResult(
    val status: ValidationStatus,
    val cardType: CardType,
    val nbTicketsLeft: Int? = null,
    val contractName: String? = null,
    val validation: ValidationEvent? = null,
    val eventDateTime: LocalDateTime? = null,
    val passValidityEndDate: LocalDate? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun loading(cardType: CardType = CardType.UNKNOWN) = ValidationResult(
            status = ValidationStatus.LOADING,
            cardType = cardType
        )

        fun success(
            cardType: CardType,
            nbTicketsLeft: Int? = null,
            contractName: String? = null,
            validation: ValidationEvent,
            passValidityEndDate: LocalDate? = null
        ) = ValidationResult(
            status = ValidationStatus.SUCCESS,
            cardType = cardType,
            nbTicketsLeft = nbTicketsLeft,
            contractName = contractName,
            validation = validation,
            eventDateTime = validation.dateTime,
            passValidityEndDate = passValidityEndDate
        )

        fun invalidCard(cardType: CardType, errorMessage: String) = ValidationResult(
            status = ValidationStatus.INVALID_CARD,
            cardType = cardType,
            errorMessage = errorMessage
        )

        fun emptyCard(cardType: CardType) = ValidationResult(
            status = ValidationStatus.EMPTY_CARD,
            cardType = cardType
        )

        fun error(cardType: CardType, errorMessage: String) = ValidationResult(
            status = ValidationStatus.ERROR,
            cardType = cardType,
            errorMessage = errorMessage
        )
    }
}
