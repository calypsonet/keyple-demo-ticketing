package org.calypsonet.keyple.demo.validation.domain.model

import java.time.LocalDateTime

/**
 * Domain model representing a validation event (pure domain, no Android deps).
 */
data class ValidationEvent(
    val name: String,
    val location: Location,
    val destination: String?,
    val dateTime: LocalDateTime,
    val provider: Int? = null
)
