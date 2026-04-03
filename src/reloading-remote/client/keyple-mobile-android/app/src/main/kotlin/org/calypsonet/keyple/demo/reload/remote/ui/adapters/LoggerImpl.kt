package org.calypsonet.keyple.demo.reload.remote.ui.adapters

import org.calypsonet.keyple.demo.reload.remote.domain.spi.Logger
import timber.log.Timber

class LoggerImpl : Logger {

    override fun i(message: String) {
        Timber.i(message)
    }

    override fun e(message: String, throwable: Throwable?) {
        Timber.e(throwable, message)
    }
}