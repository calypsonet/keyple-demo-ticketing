/* ******************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.common.dto

/**
 * Status of a remote service executed by the server, transmitted as an integer [code] in the
 * `statusCode` field of the output DTOs of the remote services: [AnalyzeContractsOutputDto],
 * [WriteContractOutputDto] and [CardIssuanceOutputDto] (Keyple distributed clients), and the
 * `SelectAppAnd...OutputDto` of the Server JSON API.
 *
 * The integer is kept in the DTOs so that the JSON format remains unchanged for all the clients.
 */
enum class RemoteServiceStatus(val code: Int) {
  /** The operation succeeded. */
  SUCCESS(0),
  /** An error occurred while communicating with the card. */
  CARD_COMMUNICATION_ERROR(1),
  /** An unexpected error occurred on the server side. */
  SERVER_ERROR(2),
  /** The card is rejected because its file structure is not supported (Calypso cards only). */
  CARD_REJECTED(3),
  /** The card has not been personalized. */
  CARD_NOT_PERSONALIZED(4),
  /** The environment of the card has expired. */
  EXPIRED_ENVIRONMENT(5),
  /** The presented card is not the one read before the contract loading. */
  DIFFERENT_CARD(6);

  companion object {
    /**
     * Returns the status corresponding to the provided code, or [SERVER_ERROR] if the code is
     * unknown.
     */
    @JvmStatic
    fun fromCode(code: Int): RemoteServiceStatus = values().find { it.code == code } ?: SERVER_ERROR
  }
}
