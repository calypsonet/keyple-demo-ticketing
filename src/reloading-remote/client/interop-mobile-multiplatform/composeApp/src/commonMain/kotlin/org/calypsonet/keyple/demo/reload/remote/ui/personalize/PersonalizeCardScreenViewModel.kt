/* ******************************************************************************
 * Copyright (c) 2024 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.reload.remote.ui.personalize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.reload.remote.data.platform.Buzzer
import org.calypsonet.keyple.demo.reload.remote.data.service.KeypleInteropService
import org.eclipse.keyple.interop.jsonapi.client.api.KeypleResult

sealed class PersonalizeCardScreenState {
  data object WaitForCard : PersonalizeCardScreenState()

  data object WritingToCard : PersonalizeCardScreenState()

  data object DisplaySuccess : PersonalizeCardScreenState()

  data class DisplayError(val message: String) : PersonalizeCardScreenState()
}

class PersonalizeCardScreenViewModel(
    private val keypleInteropService: KeypleInteropService,
    private val buzzer: Buzzer,
) : ViewModel() {
  private var _state =
      MutableStateFlow<PersonalizeCardScreenState>(PersonalizeCardScreenState.WaitForCard)
  val state = _state.asStateFlow()

  init {
    scan()
  }

  override fun onCleared() {
    super.onCleared()
    keypleInteropService.releaseReader()
  }

  private fun scan() {
    viewModelScope.launch {
      keypleInteropService.updateReaderMessage("Place your card on the reader")
      try {
        val cardFound = keypleInteropService.waitForCard()
        buzzer.beepAndVibrate()
        if (cardFound) {
          keypleInteropService.updateReaderMessage("Stay still...")
          personalizeCard()
        } else {
          _state.value = PersonalizeCardScreenState.DisplayError("No card found")
        }
      } catch (e: Exception) {
        Napier.e("Error while waiting for the card", e)
        _state.value = PersonalizeCardScreenState.DisplayError("Card reader error")
      }
    }
  }

  private suspend fun personalizeCard() {
    _state.value = PersonalizeCardScreenState.WritingToCard
    try {
      when (val result = keypleInteropService.selectAppAndPersonalizeCard()) {
        is KeypleResult.Failure -> {
          _state.value = PersonalizeCardScreenState.DisplayError(result.message)
        }
        is KeypleResult.Success -> {
          _state.value = PersonalizeCardScreenState.DisplaySuccess
        }
      }
    } catch (e: Exception) {
      Napier.e("Error while personalizing the card", e)
      _state.value = PersonalizeCardScreenState.DisplayError("Unexpected error")
    }
  }
}
