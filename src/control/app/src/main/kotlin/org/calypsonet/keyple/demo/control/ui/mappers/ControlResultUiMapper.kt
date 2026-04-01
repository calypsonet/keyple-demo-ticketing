package org.calypsonet.keyple.demo.control.ui.mappers

import org.calypsonet.keyple.demo.control.domain.model.ControlResult
import org.calypsonet.keyple.demo.control.ui.model.UiControlResult

fun ControlResult.toUi(): UiControlResult =
    UiControlResult(
        status = status,
        authenticationMode = authenticationMode,
        lastValidationsList = lastValidationsList?.mapTo(ArrayList()) { it.toUi() },
        titlesList = titlesList.mapTo(ArrayList()) { it.toUi() },
        errorTitle = errorTitle,
        errorMessage = errorMessage
    )

fun UiControlResult.toDomain(): ControlResult = ControlResult(
    status = status,
    authenticationMode = authenticationMode,
    lastValidationsList = lastValidationsList?.mapTo(ArrayList()) { it.toDomain() },
    titlesList = titlesList.mapTo(ArrayList()) { it.toDomain() },
    errorTitle = errorTitle,
    errorMessage = errorMessage,
)