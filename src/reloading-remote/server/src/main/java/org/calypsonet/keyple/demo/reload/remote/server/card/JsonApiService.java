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
package org.calypsonet.keyple.demo.reload.remote.server.card;

import static org.calypsonet.keyple.demo.reload.remote.server.card.TransactionRecorder.SECURED_READ;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
 * server selects the application of the card, executes the business service and returns a result
 * ready to be displayed (remote services {@code SELECT_APP_AND_ANALYZE_CONTRACTS}, {@code
 * SELECT_APP_AND_LOAD_CONTRACT} and {@code SELECT_APP_AND_PERSONALIZE_CARD}).
 */
@ApplicationScoped
public class JsonApiService {

  private static final Logger logger = LoggerFactory.getLogger(JsonApiService.class);
  private static final String AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS =
      "An error occurred while analyzing the contracts: {}";
  private static final String AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT =
      "An error occurred while loading the contract: {}";
  private static final String AN_ERROR_OCCURRED_WHILE_PERSONALIZING_THE_CARD =
      "An error occurred while personalizing the card: {}";
  private static final String PROCESSED_CARD_SELECTION_SCENARIO_JSON_STRING =
      "processedCardSelectionScenarioJsonString";

  private final DateTimeFormatter dateTimeFormatter =
      DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

  @Inject CardRepository cardRepository;
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
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage(), e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndAnalyzeContractsOutputDto(
          "",
          Collections.emptyList(),
          RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode(),
          e.getMessage());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage(), e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndAnalyzeContractsOutputDto(
          "", Collections.emptyList(), RemoteServiceStatus.SERVER_ERROR.getCode(), e.getMessage());
    }

    // Analyze contracts
    AnalyzeContractsOutputDto analysis =
        contractAnalysisService.analyzeContracts(
            cardReader, calypsoCard, new AnalyzeContractsInputDto(pluginType));

    // Build result
    String appSerialNumber = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    List<SelectAppAndAnalyzeContractsOutputDto.ContractInfo> contracts =
        analysis.getContracts().stream().map(this::toContractInfo).collect(Collectors.toList());
    int statusCode = analysis.getStatusCode();
    return new SelectAppAndAnalyzeContractsOutputDto(
        appSerialNumber, contracts, statusCode, getStatusMessage(statusCode, calypsoCard));
  }

  SelectAppAndLoadContractOutputDto selectAppAndLoadContract(
      CardReader cardReader, SelectAppAndLoadContractInputDto inputData, Properties properties) {

    String pluginType = inputData.getPluginType();

    // Select application
    CalypsoCard calypsoCard;
    try {
      calypsoCard = selectCard(cardReader, properties);
    } catch (CardCommunicationException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT, e.getMessage(), e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndLoadContractOutputDto(
          RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode(), e.getMessage());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT, e.getMessage(), e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndLoadContractOutputDto(
          RemoteServiceStatus.SERVER_ERROR.getCode(), e.getMessage());
    }

    String selectedApplicationSerialNumber =
        HexUtil.toHex(calypsoCard.getApplicationSerialNumber());
    String expectedApplicationSerialNumber = inputData.getApplicationSerialNumber();
    if (!selectedApplicationSerialNumber.equals(expectedApplicationSerialNumber)) {
      // The product has been bought for the card read before: to avoid swapping the cards, the
      // contract is only loaded in the same card
      return new SelectAppAndLoadContractOutputDto(
          RemoteServiceStatus.DIFFERENT_CARD.getCode(), "Not the same card");
    }

    // Write contract
    LoadContractOutputDto loading =
        contractLoadingService.loadContract(
            cardReader,
            calypsoCard,
            new LoadContractInputDto(
                inputData.getContractTariff(), inputData.getTripsToLoad(), pluginType));

    // Build result
    int statusCode = loading.getStatusCode();
    return new SelectAppAndLoadContractOutputDto(
        statusCode, getStatusMessage(statusCode, calypsoCard));
  }

  SelectAppAndPersonalizeCardOutputDto selectAppAndPersonalizeCard(
      CardReader cardReader, SelectAppAndPersonalizeCardInputDto inputData, Properties properties) {

    String pluginType = inputData.getPluginType();

    // Select application
    CalypsoCard calypsoCard;
    try {
      calypsoCard = selectCard(cardReader, properties);
    } catch (CardCommunicationException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_PERSONALIZING_THE_CARD, e.getMessage(), e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndPersonalizeCardOutputDto(
          RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode(), e.getMessage());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_PERSONALIZING_THE_CARD, e.getMessage(), e);
      recordSelectionFailure(pluginType);
      return new SelectAppAndPersonalizeCardOutputDto(
          RemoteServiceStatus.SERVER_ERROR.getCode(), e.getMessage());
    }

    // Personalize the card
    PersonalizeCardOutputDto personalizationOutput =
        cardPersonalizationService.personalizeCard(
            cardReader, calypsoCard, new PersonalizeCardInputDto(pluginType));

    // Build result
    int statusCode = personalizationOutput.getStatusCode();
    return new SelectAppAndPersonalizeCardOutputDto(
        statusCode, getStatusMessage(statusCode, calypsoCard));
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
        LocalDate now = LocalDate.now();
        LocalDate saleDate = contract.getContractSaleDate().getDate();
        LocalDate validityEndDate = contract.getContractValidityEndDate().getDate();
        boolean isExpired = validityEndDate.isBefore(now);
        name =
            isExpired
                ? PriorityCode.SEASON_PASS.getLabel() + " - Expired"
                : PriorityCode.SEASON_PASS.getLabel();
        description =
            "From\n"
                + saleDate.format(dateTimeFormatter)
                + "\nto\n"
                + validityEndDate.format(dateTimeFormatter);
        isValid = !saleDate.isAfter(now) && !isExpired;
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
  private static String getStatusMessage(int statusCode, CalypsoCard calypsoCard) {
    switch (RemoteServiceStatus.fromCode(statusCode)) {
      case SUCCESS:
        return "Success";
      case CARD_COMMUNICATION_ERROR:
        return "Card communication error";
      case SERVER_ERROR:
        return "Server error";
      case CARD_REJECTED:
        return "Invalid card\nFile structure "
            + HexUtil.toHex(calypsoCard.getApplicationSubtype())
            + "h not supported";
      case CARD_NOT_PERSONALIZED:
        return "Environment error: wrong version number";
      case ENVIRONMENT_EXPIRED:
        return "Environment error: end date expired";
      case CARD_FULL:
        return ContractLoadingService.CARD_FULL_MESSAGE;
      default:
        return "";
    }
  }
}
