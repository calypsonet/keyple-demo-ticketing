package org.calypsonet.keyple.demo.reload.remote.domain.model

import java.util.Locale

enum class Status(private val status: String) {
  LOADING("loading"),
  ERROR("error"),
  TICKETS_FOUND("tickets_found"),
  INVALID_CARD("invalid_card"),
  EMPTY_CARD("empty_card"),
  SUCCESS("success");

  override fun toString(): String {
    return status
  }

  companion object {
    fun getStatus(name: String?): Status {
      return try {
        valueOf(name!!.uppercase(Locale.ROOT))
      } catch (_: Exception) {
        // If the given state does not exist, return the default value.
        ERROR
      }
    }
  }
}