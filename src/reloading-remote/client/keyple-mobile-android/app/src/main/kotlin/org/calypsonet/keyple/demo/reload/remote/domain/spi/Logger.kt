package org.calypsonet.keyple.demo.reload.remote.domain.spi

/**
 * Simple logging abstraction for the domain.
 *
 * This interface allows the domain layer to log messages without depending on a concrete logging
 * framework or platform API. Implementations can delegate to Android Logcat, SLF4J, or any other
 * logging facility.
 */
interface Logger {

    /**
     * Logs an informational message.
     *
     * @param message Human-readable message to log.
     */
    fun i(message: String)

    /**
     * Logs an error message.
     *
     * @param message Human-readable message describing the error.
     * @param throwable Optional associated exception.
     */
    fun e(message: String, throwable: Throwable? = null)
}