/* ******************************************************************************
 * Copyright (c) 2022 Calypso Networks Association https://calypsonet.org/
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
package org.calypsonet.keyple.demo.sales.server.card;

import static org.calypsonet.keyple.demo.sales.server.card.TransactionRecorder.SECURED_READ;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto;
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsOutputDto;
import org.calypsonet.keyple.demo.common.dto.LoadContractInputDto;
import org.calypsonet.keyple.demo.common.dto.LoadContractOutputDto;
import org.calypsonet.keyple.demo.common.dto.PersonalizeCardInputDto;
import org.calypsonet.keyple.demo.common.dto.PersonalizeCardOutputDto;
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndAnalyzeContractsInputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndAnalyzeContractsOutputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndLoadContractInputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndLoadContractOutputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndPersonalizeCardInputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndPersonalizeCardOutputDto;
import org.calypsonet.keyple.demo.common.model.ContractStructure;
import org.calypsonet.keyple.demo.common.model.type.PriorityCode;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.reader.CardCommunicationException;
import org.eclipse.keypop.reader.CardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote services of the JSON API, used by the clients without Keyple (multiplatform client): the
 * server selects the application of the card, executes the business service, and returns a result
 * ready to be displayed (remote services {@code SELECT_APP_AND_ANALYZE_CONTRACTS}, {@code
 * SELECT_APP_AND_LOAD_CONTRACT} and {@code SELECT_APP_AND_PERSONALIZE_CARD}).
 */
@ApplicationScoped
public class JsonApiService {

  private static final Logger logger = LoggerFactory.getLogger(JsonApiService.class);
  private static final String CARD_COMMUNICATION_ERROR_WHILE_SELECTING =
      "Card communication error while selecting the card application [cause={}]";
  private static final String UNEXPECTED_ERROR_WHILE_SELECTING =
      "Unexpected error while selecting the card application";
  private static final String PROCESSED_CARD_SELECTION_SCENARIO_JSON_STRING =
      "processedCardSelectionScenarioJsonString";

  private final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

  @Inject CardRepository cardRepository;
  @Inject Clock clock;
  @Inject TransactionRecorder transactionRecorder;
  @Inject ContractAnalysisService contractAnalysisService;
  @Inject ContractLoadingService contractLoadingService;
  @Inject CardPersonalizationService cardPersonalizationService;

  SelectAppAndAnalyzeContractsOutputDto selectAppAndAnalyzeContracts(
      CardReader cardReader,
      SelectAppAndAnalyzeContractsInputDto inputData,
      Properties properties) {

    String pluginType = inputData.getPluginType();

    // Select application
    CalypsoCard calypsoCard;
    try {
      calypsoCard = selectCard(cardReader, properties);
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_SELECTING, e.getMessage());
      recordSelectionFailure(pluginType);
      return new SelectAppAndAnalyzeContractsOutputDto(
          "",
          Collections.emptyList(),
          RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode(),
          RemoteServiceMessages.of(RemoteServiceStatus.CARD_COMMUNICATION_ERROR, null));
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_SELECTING, e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndAnalyzeContractsOutputDto(
          "",
          Collections.emptyList(),
          RemoteServiceStatus.SERVER_ERROR.getCode(),
          RemoteServiceMessages.of(RemoteServiceStatus.SERVER_ERROR, null));
    }

    // Analyze contracts
    AnalyzeContractsOutputDto analysisOutput =
        contractAnalysisService.analyzeContracts(
            cardReader, calypsoCard, new AnalyzeContractsInputDto(pluginType));

    // Build result
    String applicationSerialNumber = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    List<SelectAppAndAnalyzeContractsOutputDto.ContractInfo> contracts =
        analysisOutput.getContracts().stream()
            .map(this::toContractInfo)
            .collect(Collectors.toList());
    int statusCode = analysisOutput.getStatusCode();
    return new SelectAppAndAnalyzeContractsOutputDto(
        applicationSerialNumber,
        contracts,
        statusCode,
        RemoteServiceMessages.of(RemoteServiceStatus.fromCode(statusCode), calypsoCard));
  }

  SelectAppAndLoadContractOutputDto selectAppAndLoadContract(
      CardReader cardReader, SelectAppAndLoadContractInputDto inputData, Properties properties) {

    String pluginType = inputData.getPluginType();

    // Select application
    CalypsoCard calypsoCard;
    try {
      calypsoCard = selectCard(cardReader, properties);
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_SELECTING, e.getMessage());
      recordSelectionFailure(pluginType);
      return new SelectAppAndLoadContractOutputDto(
          RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode(),
          RemoteServiceMessages.of(RemoteServiceStatus.CARD_COMMUNICATION_ERROR, null));
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_SELECTING, e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndLoadContractOutputDto(
          RemoteServiceStatus.SERVER_ERROR.getCode(),
          RemoteServiceMessages.of(RemoteServiceStatus.SERVER_ERROR, null));
    }

    String selectedApplicationSerialNumber =
        HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    String expectedApplicationSerialNumber = inputData.getApplicationSerialNumber();
    if (!selectedApplicationSerialNumber.equals(expectedApplicationSerialNumber)) {
      // The product has been bought for the card read before: to avoid swapping the cards, the
      // contract is only loaded in the same card
      logger.warn(
          "Card rejected: not the card read before [serialNumber={}, expectedSerialNumber={}]",
          selectedApplicationSerialNumber,
          expectedApplicationSerialNumber);
      return new SelectAppAndLoadContractOutputDto(
          RemoteServiceStatus.DIFFERENT_CARD.getCode(),
          RemoteServiceMessages.of(RemoteServiceStatus.DIFFERENT_CARD, calypsoCard));
    }

    // Write contract
    LoadContractOutputDto loadingOutput =
        contractLoadingService.loadContract(
            cardReader,
            calypsoCard,
            new LoadContractInputDto(
                inputData.getContractTariff(), inputData.getTripsToLoad(), pluginType));

    // Build result
    int statusCode = loadingOutput.getStatusCode();
    return new SelectAppAndLoadContractOutputDto(
        statusCode,
        RemoteServiceMessages.of(RemoteServiceStatus.fromCode(statusCode), calypsoCard));
  }

  SelectAppAndPersonalizeCardOutputDto selectAppAndPersonalizeCard(
      CardReader cardReader, SelectAppAndPersonalizeCardInputDto inputData, Properties properties) {

    String pluginType = inputData.getPluginType();

    // Select application
    CalypsoCard calypsoCard;
    try {
      calypsoCard = selectCard(cardReader, properties);
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_SELECTING, e.getMessage());
      recordSelectionFailure(pluginType);
      return new SelectAppAndPersonalizeCardOutputDto(
          RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode(),
          RemoteServiceMessages.of(RemoteServiceStatus.CARD_COMMUNICATION_ERROR, null));
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_SELECTING, e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndPersonalizeCardOutputDto(
          RemoteServiceStatus.SERVER_ERROR.getCode(),
          RemoteServiceMessages.of(RemoteServiceStatus.SERVER_ERROR, null));
    }

    // Personalize the card
    PersonalizeCardOutputDto personalizationOutput =
        cardPersonalizationService.personalizeCard(
            cardReader, calypsoCard, new PersonalizeCardInputDto(pluginType));

    // Build result
    int statusCode = personalizationOutput.getStatusCode();
    return new SelectAppAndPersonalizeCardOutputDto(
        statusCode,
        RemoteServiceMessages.of(RemoteServiceStatus.fromCode(statusCode), calypsoCard));
  }

  /**
   * Selects the application of the card: from the selection scenario already processed by the
   * client if provided, otherwise by processing the selection scenario on the card.
   *
   * @param properties Initial card content provided by the client, may contain the processed
   *     selection scenario.
   */
  private CalypsoCard selectCard(CardReader cardReader, Properties properties) {
    String processedCardSelectionScenarioJsonString =
        properties != null
            ? properties.getProperty(PROCESSED_CARD_SELECTION_SCENARIO_JSON_STRING)
            : null;
    if (processedCardSelectionScenarioJsonString != null) {
      return cardRepository.importProcessedCardSelectionScenario(
          processedCardSelectionScenarioJsonString);
    }
    return cardRepository.selectCard(cardReader);
  }

  /** Records the failure of the selection of the application of the card. */
  private void recordSelectionFailure(String pluginType) {
    transactionRecorder.record(pluginType, false, SECURED_READ, "");
  }

  /** Returns the contract information displayed by the client. */
  private SelectAppAndAnalyzeContractsOutputDto.ContractInfo toContractInfo(
      ContractStructure contract) {
    String name;
    String description;
    boolean isValid;
    switch (contract.getContractTariff()) {
      case MULTI_TRIP:
        name = PriorityCode.MULTI_TRIP.getLabel();
        description =
            contract.getCounterValue() != null
                ? contract.getCounterValue() + " trip(s) left"
                : "No counter";
        isValid = contract.getCounterValue() != null && contract.getCounterValue() >= 1;
        break;
      case SEASON_PASS:
        LocalDate today = LocalDate.now(clock);
        LocalDate saleDate = contract.getContractSaleDate().getDate();
        LocalDate validityEndDate = contract.getContractValidityEndDate().getDate();
        boolean isExpired = validityEndDate.isBefore(today);
        name =
            isExpired
                ? PriorityCode.SEASON_PASS.getLabel() + " (expired)"
                : PriorityCode.SEASON_PASS.getLabel();
        description =
            "From\n"
                + saleDate.format(dateTimeFormatter)
                + "\nto\n"
                + validityEndDate.format(dateTimeFormatter);
        isValid = !saleDate.isAfter(today) && !isExpired;
        break;
      case FORBIDDEN:
        name = "FORBIDDEN";
        description = "";
        isValid = false;
        break;
      default:
        name = "UNKNOWN";
        description = "";
        isValid = false;
        break;
    }
    return new SelectAppAndAnalyzeContractsOutputDto.ContractInfo(name, description, isValid);
  }

  /** Returns the message displayed by the client for the provided status of a remote service. */
}
