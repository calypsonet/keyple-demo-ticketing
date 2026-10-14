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

import static org.calypsonet.keyple.demo.reload.remote.server.card.TransactionRecorder.READ;
import static org.calypsonet.keyple.demo.reload.remote.server.card.TransactionRecorder.SECURED_READ;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles;
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsInputDto;
import org.calypsonet.keyple.demo.common.dto.AnalyzeContractsOutputDto;
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus;
import org.calypsonet.keyple.demo.common.model.ContractStructure;
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure;
import org.calypsonet.keyple.demo.common.model.EventStructure;
import org.calypsonet.keyple.demo.common.model.type.PriorityCode;
import org.calypsonet.keyple.demo.common.model.type.VersionNumber;
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.reader.CardCommunicationException;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.eclipse.keypop.storagecard.card.StorageCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analysis of the contracts of a card (remote service {@code READ_CARD_AND_ANALYZE_CONTRACTS}):
 * check of the environment and of the last event, then list of the contracts present in the card.
 */
@ApplicationScoped
public class ContractAnalysisService {

  private static final Logger logger = LoggerFactory.getLogger(ContractAnalysisService.class);
  private static final String AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS =
      "An error occurred while analyzing the contracts: {}";
  private static final String VERSION_NUMBER_OF_CARD_IS_INVALID_REJECT_CARD =
      "Version Number of card is invalid, reject card";
  private static final String ENV_END_DATE_OF_CARD_IS_INVALID_REJECT_CARD =
      "EnvEndDate of card is invalid, reject card";
  private static final String EVENT_VERSION_NUMBER_OF_CARD_IS_INVALID_REJECT_CARD =
      "EventVersionNumber of card is invalid, reject card";
  private static final String CONTRACT_TARIFF_IS_NOT_VALID_FOR_THIS_CONTRACT =
      "Contract tariff is not valid for this contract";
  private static final String CONTRACT_AT_INDEX = "Contract at index {}: {} {}";
  private static final String CONTRACTS = "Contracts {}";

  @Inject CardRepository cardRepository;
  @Inject TransactionRecorder transactionRecorder;

  AnalyzeContractsOutputDto analyzeContracts(
      CardReader cardReader, SmartCard smartCard, AnalyzeContractsInputDto inputData) {
    if (smartCard instanceof CalypsoCard) {
      return analyzeCalypsoCardContracts(cardReader, (CalypsoCard) smartCard, inputData);
    } else {
      return analyzeStorageCardContracts(cardReader, (StorageCard) smartCard, inputData);
    }
  }

  private AnalyzeContractsOutputDto analyzeCalypsoCardContracts(
      CardReader cardReader, CalypsoCard calypsoCard, AnalyzeContractsInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String appSerialNumber = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());

    logger.info(
        "=== ANALYZE CALYPSO CARD === Plugin: {}, CSN: {}, ProductType: {}, ApplicationSubtype: {}",
        pluginType,
        appSerialNumber,
        calypsoCard.getProductType(),
        HexUtil.toHex(calypsoCard.getApplicationSubtype()));

    if (!CalypsoFiles.ALLOWED_FILE_STRUCTURES.contains(calypsoCard.getApplicationSubtype())) {
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_REJECTED.getCode());
    }

    CardResource samResource =
        CardResourceServiceProvider.getService()
            .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    try {
      CardData card = cardRepository.readCard(cardReader, calypsoCard, samResource);
      // logger.info("{}", card); deactivate until LocalDate is properly processed by KeypleUtil
      List<ContractStructure> presentContracts = findPresentContracts(card);
      transactionRecorder.record(pluginType, true, SECURED_READ, appSerialNumber);
      return new AnalyzeContractsOutputDto(presentContracts, RemoteServiceStatus.SUCCESS.getCode());
    } catch (CardNotPersonalizedException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage());
      transactionRecorder.record(pluginType, false, SECURED_READ, appSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_NOT_PERSONALIZED.getCode());
    } catch (EnvironmentExpiredException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage());
      transactionRecorder.record(pluginType, false, SECURED_READ, appSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.ENVIRONMENT_EXPIRED.getCode());
    } catch (CardCommunicationException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage(), e);
      transactionRecorder.record(pluginType, false, SECURED_READ, appSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage(), e);
      transactionRecorder.record(pluginType, false, SECURED_READ, appSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      CardResourceServiceProvider.getService().releaseCardResource(samResource);
    }
  }

  private AnalyzeContractsOutputDto analyzeStorageCardContracts(
      CardReader cardReader, StorageCard storageCard, AnalyzeContractsInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String cardUID = HexUtil.toHex(storageCard.getUID());

    logger.info(
        "=== ANALYZE STORAGE CARD === Plugin: {}, UID: {}, ProductType: {}, BlockSize: {} bytes, BlockCount: {}",
        pluginType,
        cardUID,
        storageCard.getProductType(),
        storageCard.getProductType().getBlockSize(),
        storageCard.getProductType().getBlockCount());

    // SAM is not currently used for Storage Cards but may be needed in the future
    // for crypto operations (contract verification, authentication, etc.)
    // To enable SAM allocation when needed:
    // CardResource samResource = CardResourceServiceProvider.getService()
    //     .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    CardResource samResource = null;
    try {
      CardData card = cardRepository.readCard(cardReader, storageCard, samResource);
      // logger.info("{}", card); deactivate until LocalDate is properly processed by KeypleUtil
      List<ContractStructure> presentContracts = findPresentContracts(card);
      transactionRecorder.record(pluginType, true, READ, cardUID);
      return new AnalyzeContractsOutputDto(presentContracts, RemoteServiceStatus.SUCCESS.getCode());
    } catch (CardNotPersonalizedException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage());
      transactionRecorder.record(pluginType, false, READ, cardUID);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_NOT_PERSONALIZED.getCode());
    } catch (EnvironmentExpiredException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage());
      transactionRecorder.record(pluginType, false, READ, cardUID);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.ENVIRONMENT_EXPIRED.getCode());
    } catch (CardCommunicationException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage(), e);
      transactionRecorder.record(pluginType, false, READ, cardUID);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_ANALYZING_THE_CONTRACTS, e.getMessage(), e);
      transactionRecorder.record(pluginType, false, READ, cardUID);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      if (samResource != null) {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      }
    }
  }

  /**
   * Returns the contracts present in the card (ContractVersionNumber different from 0), as recorded
   * in the card: their expiry is evaluated from their dates by the users of the result, the
   * ContractTariff field keeping the type of the contract.
   *
   * @throws CardNotPersonalizedException If the environment has not the current version.
   * @throws EnvironmentExpiredException If the environment has expired.
   */
  List<ContractStructure> findPresentContracts(CardData card) {
    // Check environment
    EnvironmentHolderStructure environment = card.getEnvironment();
    if (environment.getEnvVersionNumber() != VersionNumber.CURRENT_VERSION) {
      logger.warn(VERSION_NUMBER_OF_CARD_IS_INVALID_REJECT_CARD);
      throw new CardNotPersonalizedException();
    }
    if (environment.getEnvEndDate().getDate().isBefore(LocalDate.now())) {
      logger.warn(ENV_END_DATE_OF_CARD_IS_INVALID_REJECT_CARD);
      throw new EnvironmentExpiredException();
    }
    // Check last event
    EventStructure lastEvent = card.getEvent();
    if (lastEvent.getEventVersionNumber() != VersionNumber.CURRENT_VERSION
        && lastEvent.getEventVersionNumber() != VersionNumber.UNDEFINED) {
      logger.warn(EVENT_VERSION_NUMBER_OF_CARD_IS_INVALID_REJECT_CARD);
      return Collections.emptyList();
    }
    // Iterate through the contracts in the card session
    List<ContractStructure> contracts = card.getContracts();
    List<ContractStructure> presentContracts = new ArrayList<>();
    int contractIndex = 1;
    for (ContractStructure contract : contracts) {
      logger.info(
          CONTRACT_AT_INDEX,
          contractIndex,
          contract.getContractTariff(),
          contract.getContractSaleDate().getValue());
      if (contract.getContractVersionNumber() == VersionNumber.UNDEFINED) {
        // If ContractVersionNumber is 0, ensure that the associated ContractPriority field value is
        // 0 and move on to the next contract.
        if (contract.getContractTariff() != PriorityCode.FORBIDDEN) {
          logger.warn(CONTRACT_TARIFF_IS_NOT_VALID_FOR_THIS_CONTRACT);
        }
      } else {
        presentContracts.add(contract);
      }
      contractIndex++;
    }
    logger.info(CONTRACTS, Arrays.deepToString(presentContracts.toArray()));
    return presentContracts;
  }
}
