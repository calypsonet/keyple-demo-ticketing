package org.calypsonet.keyple.demo.validation.domain.port.output

/**
 * Port for providing user feedback (audio, visual).
 * This abstraction allows the domain to trigger UI feedback without
 * depending on Android-specific implementations.
 */
interface UiFeedbackPort {
    /**
     * Display/play success feedback (sound, LED, etc.).
     */
    suspend fun displaySuccess()

    /**
     * Display/play failure feedback (sound, LED, etc.).
     */
    suspend fun displayFailure()
}
