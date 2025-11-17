package org.calypsonet.keyple.demo.validation.domain.model

import org.calypsonet.keyple.demo.common.model.type.VersionNumber
import java.time.LocalDate

/**
 * Domain model representing the card environment structure.
 */
data class CardEnvironment(
    val versionNumber: VersionNumber,
    val applicationNumber: Int,
    val applicationIssuanceDate: LocalDate,
    val endDate: LocalDate
) {
    /**
     * Business rule: Check if environment is valid on the given date.
     */
    fun isValid(currentDate: LocalDate): Boolean {
        return currentDate.isBefore(endDate) || currentDate.isEqual(endDate)
    }

    /**
     * Business rule: Check if environment version is current.
     */
    fun hasValidVersion(): Boolean {
        return versionNumber == VersionNumber.CURRENT_VERSION
    }
}
