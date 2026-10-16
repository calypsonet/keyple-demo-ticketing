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
package org.calypsonet.keyple.demo.reload.remote.data.dto

import kotlinx.serialization.Serializable

/**
 * Output of the service analyzing the contracts of the card: serial number of the card and
 * contracts.
 *
 * Mirror of `org.calypsonet.keyple.demo.common.dto.SelectAppAndAnalyzeContractsOutputDto` (JSON API
 * of the server): the `common` library being JVM only, the DTOs are declared again for the
 * multiplatform client.
 */
@Serializable
data class SelectAppAndAnalyzeContractsOutputDto(
    val applicationSerialNumber: String,
    val contracts: List<ContractInfo>,
    override val message: String,
    override val statusCode: Int,
) : RemoteServiceOutputDto {

  /** Contract of the card: name, description and validity. */
  @Serializable
  data class ContractInfo(
      val name: String,
      val description: String,
      val isValid: Boolean,
  )
}
