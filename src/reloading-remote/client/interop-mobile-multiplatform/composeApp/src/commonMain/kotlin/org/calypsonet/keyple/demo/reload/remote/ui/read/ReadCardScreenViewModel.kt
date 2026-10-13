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
package org.calypsonet.keyple.demo.reload.remote.ui.read

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.reload.remote.data.platform.Buzzer
import org.calypsonet.keyple.demo.reload.remote.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.reload.remote.data.service.ReadCardCache
import org.eclipse.keyple.interop.jsonapi.client.api.KeypleResult

sealed class ReadCardScreenState {
  data object WaitForCard : ReadCardScreenState()

  data object ReadingCard : ReadCardScreenState()

  data object ShowCardContent : ReadCardScreenState()

  data class DisplayError(val message: String) : ReadCardScreenState()
}

class ReadCardScreenViewModel(
    private val keypleInteropService: KeypleInteropService,
    private val readCardCache: ReadCardCache,
    private val buzzer: Buzzer,
) : ViewModel() {

  private var _state = MutableStateFlow<ReadCardScreenState>(ReadCardScreenState.WaitForCard)
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
        keypleInteropService.waitForCard {
          keypleInteropService.updateReaderMessage("Stay still...")
          buzzer.beepAndVibrate()
          viewModelScope.launch(Dispatchers.IO) { readContracts() }
        }
        // _state.value = ReadCardScreenState.DisplayError("No card found")
      } catch (e: Exception) {
        _state.value = ReadCardScreenState.DisplayError("Error: ${e.message}")
      }
    }
  }

  private suspend fun readContracts() {
    _state.value = ReadCardScreenState.ReadingCard
    readCardCache.clear()
    try {
      when (val result = keypleInteropService.selectCardAndAnalyzeContracts()) {
        is KeypleResult.Failure -> {
          _state.value = ReadCardScreenState.DisplayError(result.message)
        }
        is KeypleResult.Success -> {
          // Card read, kept for the following screens (card content, loading of a contract)
          readCardCache.saveCardSerial(result.data.applicationSerialNumber)
          readCardCache.saveCardContracts(result.data.contracts)
          _state.value = ReadCardScreenState.ShowCardContent
        }
      }
    } catch (e: Exception) {
      Napier.e("Error reading card", e)
      _state.value = ReadCardScreenState.DisplayError(e.message ?: "Unknown error")
    }
  }
}
