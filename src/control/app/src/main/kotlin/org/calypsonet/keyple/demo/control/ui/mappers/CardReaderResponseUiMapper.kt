package org.calypsonet.keyple.demo.control.ui.mappers

import org.calypsonet.keyple.demo.control.domain.model.CardReaderResponse
import org.calypsonet.keyple.demo.control.ui.model.UiCardReaderResponse

fun CardReaderResponse.toUi(): UiCardReaderResponse =
    UiCardReaderResponse(
        status = status,
        authenticationMode = authenticationMode,
        lastValidationsList = lastValidationsList?.mapTo(ArrayList()) { it.toUi() },
        titlesList = titlesList.mapTo(ArrayList()) { it.toUi() },
        errorTitle = errorTitle,
        errorMessage = errorMessage
    )

fun UiCardReaderResponse.toDomain(): CardReaderResponse = CardReaderResponse(
    status = status,
    authenticationMode = authenticationMode,
    lastValidationsList = lastValidationsList?.mapTo(ArrayList()) { it.toDomain() },
    titlesList = titlesList.mapTo(ArrayList()) { it.toDomain() },
    errorTitle = errorTitle,
    errorMessage = errorMessage,
)