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

import static org.calypsonet.keyple.demo.sales.server.card.TransactionRecorder.READ;
import static org.calypsonet.keyple.demo.sales.server.card.TransactionRecorder.SECURED_READ;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
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
  private static final String CARD_COMMUNICATION_ERROR_WHILE_ANALYZING =
      "Card communication error while analyzing the contracts [cause={}]";
  private static final String UNEXPECTED_ERROR_WHILE_ANALYZING =
      "Unexpected error while analyzing the contracts";

  @Inject CardRepository cardRepository;
  @Inject Clock clock;
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
    String applicationSerialNumber = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());

    logger.info(
        "Analyzing the contracts of a Calypso card [plugin={}, serialNumber={}, productType={}, applicationSubtype={}]",
        pluginType,
        applicationSerialNumber,
        calypsoCard.getProductType(),
        HexUtil.toHex(calypsoCard.getApplicationSubtype()));

    if (!CalypsoFiles.ALLOWED_FILE_STRUCTURES.contains(calypsoCard.getApplicationSubtype())) {
      logger.warn(
          "Card rejected: file structure not supported [applicationSubtype={}]",
          HexUtil.toHex(calypsoCard.getApplicationSubtype()));
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_REJECTED.getCode());
    }

    CardResource samResource =
        CardResourceServiceProvider.getService()
            .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    try {
      CardData card = cardRepository.readCard(cardReader, calypsoCard, samResource);
      List<ContractStructure> presentContracts = findPresentContracts(card);
      transactionRecorder.record(pluginType, true, SECURED_READ, applicationSerialNumber);
      return new AnalyzeContractsOutputDto(presentContracts, RemoteServiceStatus.SUCCESS.getCode());
    } catch (CardNotPersonalizedException e) {
      // Rejection logged where it is detected
      transactionRecorder.record(pluginType, false, SECURED_READ, applicationSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_NOT_PERSONALIZED.getCode());
    } catch (EnvironmentExpiredException e) {
      // Rejection logged where it is detected
      transactionRecorder.record(pluginType, false, SECURED_READ, applicationSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.ENVIRONMENT_EXPIRED.getCode());
    } catch (EventWrongVersionException e) {
      // Rejection logged where it is detected
      transactionRecorder.record(pluginType, false, SECURED_READ, applicationSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.EVENT_WRONG_VERSION.getCode());
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_ANALYZING, e.getMessage());
      transactionRecorder.record(pluginType, false, SECURED_READ, applicationSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_ANALYZING, e);
      transactionRecorder.record(pluginType, false, SECURED_READ, applicationSerialNumber);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      CardResourceServiceProvider.getService().releaseCardResource(samResource);
    }
  }

  private AnalyzeContractsOutputDto analyzeStorageCardContracts(
      CardReader cardReader, StorageCard storageCard, AnalyzeContractsInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String uid = HexUtil.toHex(storageCard.getUID());

    logger.info(
        "Analyzing the contracts of a storage card [plugin={}, uid={}, productType={}, blockSize={}, blockCount={}]",
        pluginType,
        uid,
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
      List<ContractStructure> presentContracts = findPresentContracts(card);
      transactionRecorder.record(pluginType, true, READ, uid);
      return new AnalyzeContractsOutputDto(presentContracts, RemoteServiceStatus.SUCCESS.getCode());
    } catch (CardNotPersonalizedException e) {
      // Rejection logged where it is detected
      transactionRecorder.record(pluginType, false, READ, uid);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_NOT_PERSONALIZED.getCode());
    } catch (EnvironmentExpiredException e) {
      // Rejection logged where it is detected
      transactionRecorder.record(pluginType, false, READ, uid);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.ENVIRONMENT_EXPIRED.getCode());
    } catch (EventWrongVersionException e) {
      // Rejection logged where it is detected
      transactionRecorder.record(pluginType, false, READ, uid);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.EVENT_WRONG_VERSION.getCode());
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_ANALYZING, e.getMessage());
      transactionRecorder.record(pluginType, false, READ, uid);
      return new AnalyzeContractsOutputDto(
          Collections.emptyList(), RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_ANALYZING, e);
      transactionRecorder.record(pluginType, false, READ, uid);
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
   * in the card: their expiration is evaluated from their dates by the users of the result, the
   * ContractTariff field keeping the type of the contract.
   *
   * @throws CardNotPersonalizedException If the environment has not the current version.
   * @throws EnvironmentExpiredException If the environment has expired.
   * @throws EventWrongVersionException If the last event has neither the current version nor the
   *     undefined one (clean card).
   */
  List<ContractStructure> findPresentContracts(CardData card) {
    // Check environment
    EnvironmentHolderStructure environment = card.getEnvironment();
    if (environment.getEnvVersionNumber() != VersionNumber.CURRENT_VERSION) {
      logger.warn(
          "Card rejected: unexpected EnvVersionNumber [envVersionNumber={}]",
          environment.getEnvVersionNumber());
      throw new CardNotPersonalizedException();
    }
    if (environment.getEnvEndDate().getDate().isBefore(LocalDate.now(clock))) {
      logger.warn(
          "Card rejected: environment expired [envEndDate={}]",
          environment.getEnvEndDate().getDate());
      throw new EnvironmentExpiredException();
    }
    // Check last event
    EventStructure lastEvent = card.getEvent();
    if (lastEvent.getEventVersionNumber() != VersionNumber.CURRENT_VERSION
        && lastEvent.getEventVersionNumber() != VersionNumber.UNDEFINED) {
      logger.warn(
          "Card rejected: unexpected EventVersionNumber [eventVersionNumber={}]",
          lastEvent.getEventVersionNumber());
      throw new EventWrongVersionException();
    }
    // Iterate through the contracts in the card session
    List<ContractStructure> contracts = card.getContracts();
    List<ContractStructure> presentContracts = new ArrayList<>();
    int recordNumber = 1;
    for (ContractStructure contract : contracts) {
      logger.debug(
          "Contract read [recordNumber={}, contractTariff={}, contractSaleDate={}]",
          recordNumber,
          contract.getContractTariff(),
          contract.getContractSaleDate().getDate());
      if (contract.getContractVersionNumber() == VersionNumber.UNDEFINED) {
        // If ContractVersionNumber is 0, ensure that the associated ContractPriority field value is
        // 0 and move on to the next contract.
        if (contract.getContractTariff() != PriorityCode.FORBIDDEN) {
          logger.warn(
              "Blank contract of an unexpected ContractTariff [recordNumber={}, contractTariff={}]",
              recordNumber,
              contract.getContractTariff());
        }
      } else {
        presentContracts.add(contract);
      }
      recordNumber++;
    }
    logger.debug("Contracts present in the card [contracts={}]", presentContracts);
    return presentContracts;
  }
}
