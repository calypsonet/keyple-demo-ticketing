package org.calypsonet.keyple.demo.validation.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Business rules for card validation.
 * This object contains pure domain logic without any infrastructure dependencies.
 */
object ValidationRules {
    /**
     * Anti-passback minimum delay in minutes.
     */
    private const val ANTI_PASSBACK_DELAY_MINUTES = 1L

    /**
     * Business rule: Check if anti-passback rule is violated.
     * Validation must be at least 1 minute after the previous validation.
     *
     * @param lastValidation The datetime of the last validation
     * @param currentValidation The datetime of the current validation attempt
     * @return true if the anti-passback rule is violated (too soon)
     */
    fun isAntiPassbackViolated(
        lastValidation: LocalDateTime,
        currentValidation: LocalDateTime
    ): Boolean {
        val duration = Duration.between(lastValidation, currentValidation)
        return duration.toMinutes() < ANTI_PASSBACK_DELAY_MINUTES
    }

    /**
     * Business rule: Select the best valid contract from a list.
     * Selection is based on priority order (lower priority number = higher priority).
     *
     * @param contracts List of contracts to evaluate
     * @param currentDate The date to check validity against
     * @return The best valid contract or null if none found
     */
    fun selectBestContract(
        contracts: List<Contract>,
        currentDate: LocalDate = LocalDate.now()
    ): Contract? {
        return contracts
            .filter { it.isValid(currentDate) && it.hasTripsRemaining() }
            .minByOrNull { it.priorityCode.key }
    }

    /**
     * Business rule: Check if a contract list has any valid contracts.
     *
     * @param contracts List of contracts to check
     * @param currentDate The date to check validity against
     * @return true if at least one valid contract exists
     */
    fun hasValidContract(
        contracts: List<Contract>,
        currentDate: LocalDate = LocalDate.now()
    ): Boolean {
        return contracts.any { it.isValid(currentDate) && it.hasTripsRemaining() }
    }
}
