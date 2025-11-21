package org.calypsonet.keyple.demo.validation.ui.mappers

import org.calypsonet.keyple.demo.validation.domain.model.ValidationResult
import org.calypsonet.keyple.demo.validation.ui.model.UIValidationResult

fun ValidationResult.toUi(): UIValidationResult =
    UIValidationResult(
        status = status,
        cardType = cardType,
        nbTicketsLeft = nbTicketsLeft,
        contract = contract,
        validationData = validationData?.toUi(),
        eventDateTime = eventDateTime,
        passValidityEndDate = passValidityEndDate,
        errorMessage = errorMessage
    )

fun UIValidationResult.toDomain(): ValidationResult =
    ValidationResult(
        status = status,
        cardType = cardType,
        nbTicketsLeft = nbTicketsLeft,
        contract = contract,
        validationData = validationData?.toDomain(),
        eventDateTime = eventDateTime,
        passValidityEndDate = passValidityEndDate,
        errorMessage = errorMessage
    )