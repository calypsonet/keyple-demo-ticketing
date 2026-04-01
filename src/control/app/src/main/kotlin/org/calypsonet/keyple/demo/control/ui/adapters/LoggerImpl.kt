package org.calypsonet.keyple.demo.control.ui.adapters

import org.calypsonet.keyple.demo.control.domain.spi.Logger
import timber.log.Timber

class LoggerImpl : Logger {

    override fun i(message: String) {
        Timber.i(message)
    }

    override fun e(message: String, throwable: Throwable?) {
        Timber.e(throwable, message)
    }

    override fun w(message: String, throwable: Throwable?) {
        Timber.w(throwable, message)
    }
}