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
package org.calypsonet.keyple.demo.reload.remote.ui.load

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.calypsonet.keyple.demo.reload.remote.data.dto.PriorityCode
import org.calypsonet.keyple.demo.reload.remote.data.service.KeypleInteropService
import org.calypsonet.keyple.demo.reload.remote.ui.model.ProductType
import org.calypsonet.keyple.demo.reload.remote.ui.nav.LoadContract
import org.eclipse.keyple.interop.jsonapi.client.api.KeypleResult

sealed class LoadContractScreenState {
  data object WaitForCard : LoadContractScreenState()

  data object WritingToCard : LoadContractScreenState()

  data object ShowTransactionSuccess : LoadContractScreenState()

  data class DisplayError(val message: String) : LoadContractScreenState()
}

class LoadContractScreenViewModel(
    private val keypleInteropService: KeypleInteropService,
    private val route: LoadContract,
) : ViewModel() {
  private var _state =
      MutableStateFlow<LoadContractScreenState>(LoadContractScreenState.WaitForCard)
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
      keypleInteropService.updateReaderMessage("Place your card on the top of the iPhone")
      try {
        val cardFound = keypleInteropService.waitCard()
        if (cardFound) {
          keypleInteropService.updateReaderMessage("Stay still...")
          loadContract(route)
        } else {
          _state.value = LoadContractScreenState.DisplayError("No card found")
        }
      } catch (e: Exception) {
        _state.value = LoadContractScreenState.DisplayError("Error: ${e.message}")
      }
    }
  }

  private suspend fun loadContract(route: LoadContract) {
    _state.value = LoadContractScreenState.WritingToCard

    try {
      val result =
          if (route.type == ProductType.SEASON_PASS.ordinal) loadSeasonPass()
          else loadMultiTrip(route.quantity)
      when (result) {
        is KeypleResult.Failure -> {
          _state.value = LoadContractScreenState.DisplayError(result.message)
        }
        is KeypleResult.Success -> {
          _state.value = LoadContractScreenState.ShowTransactionSuccess
        }
      }
    } catch (e: Exception) {
      Napier.e("Error writing to card", e)
      _state.value = LoadContractScreenState.DisplayError(e.message ?: "Unknown error")
    }
  }

  private suspend fun loadSeasonPass(): KeypleResult<String> {
    return keypleInteropService.selectCardAndLoadContract(
        tripsToLoad = 1, code = PriorityCode.SEASON_PASS)
  }

  private suspend fun loadMultiTrip(trips: Int): KeypleResult<String> {
    return keypleInteropService.selectCardAndLoadContract(
        tripsToLoad = trips, code = PriorityCode.MULTI_TRIP)
  }
}
