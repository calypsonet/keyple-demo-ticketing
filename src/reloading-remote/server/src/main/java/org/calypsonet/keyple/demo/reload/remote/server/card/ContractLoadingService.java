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

import static org.calypsonet.keyple.demo.reload.remote.server.card.ActivityRecorder.RELOAD;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles;
import org.calypsonet.keyple.demo.common.dto.LoadContractInputDto;
import org.calypsonet.keyple.demo.common.dto.LoadContractOutputDto;
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus;
import org.calypsonet.keyple.demo.common.model.ContractStructure;
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure;
import org.calypsonet.keyple.demo.common.model.EventStructure;
import org.calypsonet.keyple.demo.common.model.type.DateCompact;
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
 * Loading of a contract in a card (remote service {@code READ_CARD_AND_LOAD_CONTRACT}): reload of a
 * contract of the same type, or new contract written in an empty record or in place of an expired
 * or exhausted contract.
 */
@ApplicationScoped
public class ContractLoadingService {

  /** Message of the result of a loading refused because the card is full. */
  static final String CARD_FULL_MESSAGE = "No space left on the card for a new ticket.";

  static final String THE_CARD_IS_FULL =
      "No empty contract record nor expired or exhausted contract to replace, reject card";

  private static final Logger logger = LoggerFactory.getLogger(ContractLoadingService.class);
  private static final String AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT =
      "An error occurred while loading the contract: {}";
  private static final String ONLY_SEASON_PASS_OR_MULTI_TRIP_TICKET_CAN_BE_LOADED =
      "Only Season Pass or Multi Trip ticket can be loaded";

  @Inject CardRepository cardRepository;
  @Inject ActivityRecorder activityRecorder;

  LoadContractOutputDto loadContract(
      CardReader cardReader, SmartCard smartCard, LoadContractInputDto inputData) {
    if (smartCard instanceof CalypsoCard) {
      return loadCalypsoCardContract(cardReader, (CalypsoCard) smartCard, inputData);
    } else {
      return loadStorageCardContract(cardReader, (StorageCard) smartCard, inputData);
    }
  }

  private LoadContractOutputDto loadCalypsoCardContract(
      CardReader cardReader, CalypsoCard calypsoCard, LoadContractInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String appSerialNumber = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());

    if (!CalypsoFiles.ALLOWED_FILE_STRUCTURES.contains(calypsoCard.getApplicationSubtype())) {
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_REJECTED.getCode());
    }

    logger.info(
        "=== LOAD CALYPSO CARD CONTRACT === Plugin: {}, CSN: {}, ProductType: {}, Contract: {}, Tickets: {}",
        pluginType,
        appSerialNumber,
        calypsoCard.getProductType(),
        inputData.getContractTariff(),
        inputData.getTripsToLoad());

    CardResource samResource =
        CardResourceServiceProvider.getService()
            .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    try {
      Card card = cardRepository.readCard(cardReader, calypsoCard, samResource);
      // logger.info("{}", card); deactivate until LocalDate is properly processed by KeypleUtil
      insertNewContract(inputData.getContractTariff(), inputData.getTripsToLoad(), card);
      int statusCode = cardRepository.writeCard(cardReader, calypsoCard, samResource, card);
      activityRecorder.record(
          pluginType, true, RELOAD, appSerialNumber, describeLoadedContract(inputData));
      return new LoadContractOutputDto(statusCode);
    } catch (CardFullException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT, e.getMessage());
      activityRecorder.record(pluginType, false, RELOAD, appSerialNumber, "");
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_FULL.getCode());
    } catch (CardCommunicationException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT, e.getMessage(), e);
      activityRecorder.record(pluginType, false, RELOAD, appSerialNumber, "");
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT, e.getMessage(), e);
      activityRecorder.record(pluginType, false, RELOAD, appSerialNumber, "");
      return new LoadContractOutputDto(RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      CardResourceServiceProvider.getService().releaseCardResource(samResource);
    }
  }

  private LoadContractOutputDto loadStorageCardContract(
      CardReader cardReader, StorageCard storageCard, LoadContractInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String cardUID = HexUtil.toHex(storageCard.getUID());

    logger.info(
        "=== LOAD STORAGE CARD CONTRACT === Plugin: {}, UID: {}, ProductType: {}, Contract: {}, Tickets: {}",
        pluginType,
        cardUID,
        storageCard.getProductType(),
        inputData.getContractTariff(),
        inputData.getTripsToLoad());

    // SAM is not currently used for Storage Cards but may be needed in the future
    // for crypto operations (contract verification, authentication, etc.)
    // To enable SAM allocation when needed:
    // CardResource samResource = CardResourceServiceProvider.getService()
    //     .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    CardResource samResource = null;
    try {
      Card card = cardRepository.readCard(cardReader, storageCard, samResource);
      // logger.info("{}", card); deactivate until LocalDate is properly processed by KeypleUtil
      insertNewContract(inputData.getContractTariff(), inputData.getTripsToLoad(), card);
      int statusCode = cardRepository.writeCard(cardReader, storageCard, samResource, card);
      activityRecorder.record(pluginType, true, RELOAD, cardUID, describeLoadedContract(inputData));
      return new LoadContractOutputDto(statusCode);
    } catch (CardFullException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT, e.getMessage());
      activityRecorder.record(pluginType, false, RELOAD, cardUID, "");
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_FULL.getCode());
    } catch (CardCommunicationException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT, e.getMessage(), e);
      activityRecorder.record(pluginType, false, RELOAD, cardUID, "");
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_LOADING_THE_CONTRACT, e.getMessage(), e);
      activityRecorder.record(pluginType, false, RELOAD, cardUID, "");
      return new LoadContractOutputDto(RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      if (samResource != null) {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      }
    }
  }

  /** Returns the description of the loaded contract shown by the dashboard. */
  private static String describeLoadedContract(LoadContractInputDto inputData) {
    return inputData.getContractTariff().toString().replace("_", " ")
        + ((inputData.getTripsToLoad() != 0) ? ": " + inputData.getTripsToLoad() : "");
  }

  /**
   * Inserts the new contract in the card data (contract and event), to be written in the card.
   *
   * @throws IllegalArgumentException If the contract is neither a season pass nor a multi-trip
   *     ticket.
   * @throws CardFullException If no record is available for a new contract.
   */
  void insertNewContract(PriorityCode contractTariff, Integer tripsToLoad, Card card) {

    if (contractTariff != PriorityCode.SEASON_PASS && contractTariff != PriorityCode.MULTI_TRIP) {
      throw new IllegalArgumentException(ONLY_SEASON_PASS_OR_MULTI_TRIP_TICKET_CAN_BE_LOADED);
    }

    EnvironmentHolderStructure environment = card.getEnvironment();
    List<ContractStructure> contracts = card.getContracts();
    EventStructure currentEvent = card.getEvent();
    ContractStructure newContract;
    int newContractNumber;

    // Contract analysis: the priority of the expired contracts is set to 31 in the event to write
    currentEvent = withExpiredContractPriorities(contracts, currentEvent);

    // Single contract cards support only one contract (contracts.size() == 1)
    boolean isSingleContractCard = contracts.size() == 1;

    int existingContractNumber = getContractNumber(contractTariff, contracts);
    if (existingContractNumber > 0) {
      // Reloading same contract type
      newContractNumber = existingContractNumber;
      ContractStructure currentContract = contracts.get(existingContractNumber - 1);
      // build new contract
      if (PriorityCode.MULTI_TRIP == contractTariff) {
        newContract =
            buildMultiTripContract(
                environment.getEnvEndDate(), currentContract.getCounterValue() + tripsToLoad);
      } else {
        newContract = buildSeasonContract(currentContract);
      }
    } else if (isSingleContractCard) {
      // Single contract card: new contract type replaces existing one at position 1
      newContractNumber = 1;
      logger.info(
          "Single contract card: replacing existing contract {} with new {} contract",
          contracts.get(0).getContractTariff(),
          contractTariff);
      // build new contract
      if (PriorityCode.MULTI_TRIP == contractTariff) {
        newContract = buildMultiTripContract(environment.getEnvEndDate(), tripsToLoad);
      } else {
        newContract = buildSeasonContract(null);
      }
    } else {
      // Calypso: Issuing new contract, find available position
      newContractNumber = findAvailablePosition(contracts, currentEvent);
      if (newContractNumber == 0) {
        logger.warn(THE_CARD_IS_FULL);
        throw new CardFullException();
      }
      // build new contract
      if (PriorityCode.MULTI_TRIP == contractTariff) {
        newContract = buildMultiTripContract(environment.getEnvEndDate(), tripsToLoad);
      } else {
        newContract = buildSeasonContract(null);
      }
    }
    currentEvent =
        currentEvent.withContractPriority(newContractNumber, newContract.getContractTariff());
    // Update contract & Event
    card.setContract(newContractNumber - 1, newContract);
    card.setEvent(currentEvent);
  }

  /**
   * Returns the provided event with the priority set to 31 (EXPIRED) for the contracts present in
   * the card whose validity end date is in the past.
   */
  private EventStructure withExpiredContractPriorities(
      List<ContractStructure> contracts, EventStructure event) {
    LocalDate today = LocalDate.now();
    EventStructure updatedEvent = event;
    for (int i = 0; i < contracts.size(); i++) {
      ContractStructure contract = contracts.get(i);
      if (contract.getContractVersionNumber() != VersionNumber.UNDEFINED
          && contract.getContractValidityEndDate().getDate().isBefore(today)) {
        updatedEvent = updatedEvent.withContractPriority(i + 1, PriorityCode.EXPIRED);
      }
    }
    return updatedEvent;
  }

  private int getContractNumber(PriorityCode contractTariff, List<ContractStructure> contracts) {
    int contractCount = contracts.size();
    for (int i = 0; i < contractCount; i++) {
      if (contractTariff.equals(contracts.get(i).getContractTariff())) {
        return i + 1;
      }
    }
    return 0;
  }

  private ContractStructure buildMultiTripContract(DateCompact envEndDate, Integer counterValue) {
    DateCompact contractSaleDate = new DateCompact(LocalDate.now());
    return new ContractStructure(
        VersionNumber.CURRENT_VERSION,
        PriorityCode.MULTI_TRIP,
        contractSaleDate,
        envEndDate,
        null,
        null,
        null,
        null,
        counterValue);
  }

  /**
   * Builds a season pass valid for 30 days: from today for a new season pass, or from the validity
   * end date of the reloaded season pass when it has not expired (the reload extends its validity).
   *
   * @param reloadedContract The season pass reloaded, null for a new season pass.
   */
  private ContractStructure buildSeasonContract(ContractStructure reloadedContract) {
    DateCompact contractSaleDate = new DateCompact(LocalDate.now());
    DateCompact validityStartDate = contractSaleDate;
    if (reloadedContract != null
        && !reloadedContract.getContractValidityEndDate().getDate().isBefore(LocalDate.now())) {
      validityStartDate = reloadedContract.getContractValidityEndDate();
    }
    DateCompact contractValidityEndDate = new DateCompact(validityStartDate.getValue() + 30);
    return new ContractStructure(
        VersionNumber.CURRENT_VERSION,
        PriorityCode.SEASON_PASS,
        contractSaleDate,
        contractValidityEndDate,
        null,
        null,
        null,
        null);
  }

  /**
   * Returns the number of the contract record where a new contract can be written: the first empty
   * record (ContractPriority 0), otherwise the first record whose contract is expired or exhausted
   * (ContractPriority 31), or 0 if there is none.
   */
  private int findAvailablePosition(List<ContractStructure> contracts, EventStructure event) {
    int contractCount = contracts.size();
    List<PriorityCode> contractPriorities = event.getContractPriorities();
    for (int i = 0; i < contractCount; i++) {
      if (PriorityCode.FORBIDDEN == contractPriorities.get(i)) {
        return i + 1;
      }
    }
    for (int i = 0; i < contractCount; i++) {
      if (PriorityCode.EXPIRED == contractPriorities.get(i)) {
        return i + 1;
      }
    }
    return 0;
  }
}
