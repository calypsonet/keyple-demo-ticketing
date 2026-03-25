package org.calypsonet.keyple.demo.control.ui.mappers

import org.calypsonet.keyple.demo.control.domain.model.Validation
import org.calypsonet.keyple.demo.control.ui.model.UiValidation

fun Validation.toUi(): UiValidation = UiValidation(
    name = name,
    location = location.toUi(),
    destination = destination,
    dateTime = dateTime,
    provider = provider
)

fun UiValidation.toDomain(): Validation =
    Validation(
        name = name,
        location = location.toDomain(),
        destination = destination,
        dateTime = dateTime,
        provider = provider
)