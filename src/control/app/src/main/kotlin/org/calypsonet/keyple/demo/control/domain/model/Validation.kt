package org.calypsonet.keyple.demo.control.domain.model

import java.time.LocalDateTime

data class Validation(
    val name: String,
    val location: Location,
    val destination: String?,
    val dateTime: LocalDateTime,
    val provider: Int? = null
)
