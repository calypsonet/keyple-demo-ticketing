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
package org.calypsonet.keyple.demo.reload.remote.server.card;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Properties;
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto;
import org.calypsonet.keyple.demo.common.dto.CardIssuanceInputDto;
import org.calypsonet.keyple.demo.common.dto.RemoteServiceId;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndAnalyzeContractsInputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndIncreaseContractCounterInputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndLoadContractInputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndPersonalizeCardInputDto;
import org.calypsonet.keyple.demo.common.dto.WriteContractInputDto;
import org.eclipse.keyple.distributed.RemoteReaderServer;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.selection.spi.SmartCard;

/**
 * Dispatches each remote service requested by a client to the business service handling it: the
 * client specifies the Service ID ({@link RemoteServiceId}) when executing the remote service, and
 * provides its input data and, depending on the service, the initial card content (selected card or
 * processed selection scenario).
 */
@ApplicationScoped
public class RemoteServiceDispatcher {

  @Inject ContractCounterService contractCounterService;
  @Inject ContractAnalysisService contractAnalysisService;
  @Inject ContractLoadingService contractLoadingService;
  @Inject CardPersonalizationService cardPersonalizationService;
  @Inject CardSelectionApi cardSelectionApi;

  /**
   * Executes the business service matching the Service ID specified by the client, and returns its
   * output data.
   *
   * @param reader The remote reader of the client.
   * @param readerExtension The extension of the remote reader, providing the Service ID, the input
   *     data and the initial card content.
   * @return The output data of the service, to be returned to the client.
   * @throws IllegalArgumentException If the Service ID is not recognized.
   */
  Object execute(CardReader reader, RemoteReaderServer readerExtension) {
    RemoteServiceId serviceId;
    try {
      serviceId = RemoteServiceId.valueOf(readerExtension.getServiceId());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Service ID not recognized");
    }
    switch (serviceId) {
      // Services of the .NET client (selection of the application by the server)
      case SELECT_APP_AND_READ_CONTRACTS:
        return contractCounterService.selectAppAndReadContracts(reader);
      case SELECT_APP_AND_INCREASE_CONTRACT_COUNTER:
        return contractCounterService.selectAppAndIncreaseContractCounter(
            reader,
            readerExtension.getInputData(SelectAppAndIncreaseContractCounterInputDto.class));
      // Services of the Android client (selection of the application by the client)
      case READ_CARD_AND_ANALYZE_CONTRACTS:
        return contractAnalysisService.analyzeContracts(
            reader,
            readerExtension.getInitialCardContent(SmartCard.class),
            readerExtension.getInputData(AnalyzeContractsInputDto.class));
      case READ_CARD_AND_WRITE_CONTRACT:
        return contractLoadingService.writeContract(
            reader,
            readerExtension.getInitialCardContent(SmartCard.class),
            readerExtension.getInputData(WriteContractInputDto.class));
      case PERSONALIZE_CARD:
        return cardPersonalizationService.initCard(
            reader,
            readerExtension.getInitialCardContent(SmartCard.class),
            readerExtension.getInputData(CardIssuanceInputDto.class));
      // Services of the JSON API (selection of the application by the server, possibly from the
      // selection scenario already processed by the client)
      case SELECT_APP_AND_ANALYZE_CONTRACTS:
        return cardSelectionApi.selectAppAndAnalyzeContracts(
            reader,
            readerExtension.getInputData(SelectAppAndAnalyzeContractsInputDto.class),
            readerExtension.getInitialCardContent(Properties.class));
      case SELECT_APP_AND_LOAD_CONTRACT:
        return cardSelectionApi.selectAppAndLoadContract(
            reader,
            readerExtension.getInputData(SelectAppAndLoadContractInputDto.class),
            readerExtension.getInitialCardContent(Properties.class));
      case SELECT_APP_AND_PERSONALIZE_CARD:
        return cardSelectionApi.selectAppAndPersonalizeCard(
            reader,
            readerExtension.getInputData(SelectAppAndPersonalizeCardInputDto.class),
            readerExtension.getInitialCardContent(Properties.class));
      default:
        throw new IllegalArgumentException("Service ID not recognized");
    }
  }
}
