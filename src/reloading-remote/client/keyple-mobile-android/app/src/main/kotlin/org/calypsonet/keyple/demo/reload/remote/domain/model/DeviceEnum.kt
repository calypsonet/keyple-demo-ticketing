package org.calypsonet.keyple.demo.reload.remote.domain.model

import org.calypsonet.keyple.demo.reload.remote.R
import java.util.Locale

enum class DeviceEnum(val textId: Int) {
  CONTACTLESS_CARD(R.string.contactless_card),
  SIM(R.string.sim_card),
  WEARABLE(R.string.wearable),
  EMBEDDED(R.string.embedded_secure_elem);

  companion object {
    @JvmStatic
    fun getDeviceEnum(name: String): DeviceEnum {
      return try {
        valueOf(name.uppercase(Locale.ROOT))
      } catch (_: Exception) {
        // If the given state does not exist, return the default value.
        CONTACTLESS_CARD
      }
    }
  }
}