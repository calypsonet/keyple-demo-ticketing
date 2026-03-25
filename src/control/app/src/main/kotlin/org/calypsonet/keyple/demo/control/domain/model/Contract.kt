package org.calypsonet.keyple.demo.control.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Contract(
    val name: String?,
    val valid: Boolean,
    val validationDateTime: LocalDateTime?,
    val record: Int,
    val expired: Boolean,
    val contractValidityStartDate: LocalDate,
    val contractValidityEndDate: LocalDate,
    val nbTicketsLeft: Int? = null
)
