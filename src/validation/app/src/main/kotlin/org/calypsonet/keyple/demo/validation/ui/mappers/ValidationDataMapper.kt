package org.calypsonet.keyple.demo.validation.ui.mappers

import org.calypsonet.keyple.demo.validation.domain.model.ValidationData
import org.calypsonet.keyple.demo.validation.ui.model.UIValidationData

fun ValidationData.toUi(): UIValidationData =
    UIValidationData(
        name = name,
        location = location.toUi(),
        destination = destination,
        dateTime = dateTime,
        provider = provider
    )

fun UIValidationData.toDomain(): ValidationData =
    ValidationData(
        name = name,
        location = location.toDomain(),
        destination = destination,
        dateTime = dateTime,
        provider = provider
    )