package org.calypsonet.keyple.demo.control.ui.adapters

import android.app.Activity
import org.calypsonet.keyple.demo.control.domain.spi.UiContext

class UiContextImpl(private val activity: Activity) : UiContext {

    override fun <T> adaptTo(adapter: Class<T>): T {
        if (adapter.isAssignableFrom(Activity::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return activity as T
        }
        throw IllegalArgumentException("Unsupported adapter type: $adapter")
    }
}