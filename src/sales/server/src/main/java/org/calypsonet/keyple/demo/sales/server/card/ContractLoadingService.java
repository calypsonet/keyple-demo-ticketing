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

import static org.calypsonet.keyple.demo.sales.server.card.TransactionRecorder.LOAD;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
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
 * Loading of a contract in a card (remote service {@code READ_CARD_AND_LOAD_CONTRACT}): loading of
 * a contract of the same type (trips added or season pass renewed), or new contract written in an
 * empty record or in place of an expired or exhausted contract.
 */
@ApplicationScoped
public class ContractLoadingService {

  private static final Logger logger = LoggerFactory.getLogger(ContractLoadingService.class);

  private static final String CARD_COMMUNICATION_ERROR_WHILE_LOADING =
      "Card communication error while loading the contract [cause={}]";
  private static final String UNEXPECTED_ERROR_WHILE_LOADING =
      "Unexpected error while loading the contract";
  private static final String ONLY_SEASON_PASS_OR_MULTI_TRIP_TICKET_CAN_BE_LOADED =
      "Only a season pass or a multi-trip ticket can be loaded";

  /** Maximum value of the counter of a multi-trip ticket (counter of 24 bits). */
  private static final int MAX_COUNTER_VALUE = 0xFFFFFF;

  @Inject CardRepository cardRepository;
  @Inject Clock clock;
  @Inject TransactionRecorder transactionRecorder;

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
    String applicationSerialNumber = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());

    if (!CalypsoFiles.ALLOWED_FILE_STRUCTURES.contains(calypsoCard.getApplicationSubtype())) {
      logger.warn(
          "Card rejected: file structure not supported [applicationSubtype={}]",
          HexUtil.toHex(calypsoCard.getApplicationSubtype()));
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_REJECTED.getCode());
    }

    logger.info(
        "Loading a contract in a Calypso card [plugin={}, serialNumber={}, productType={}, contractTariff={}, tripsToLoad={}]",
        pluginType,
        applicationSerialNumber,
        calypsoCard.getProductType(),
        inputData.getContractTariff(),
        inputData.getTripsToLoad());

    CardResource samResource =
        CardResourceServiceProvider.getService()
            .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    try {
      CardData card = cardRepository.readCard(cardReader, calypsoCard, samResource);
      insertNewContract(inputData.getContractTariff(), inputData.getTripsToLoad(), card);
      int statusCode = cardRepository.writeCard(cardReader, calypsoCard, samResource, card);
      transactionRecorder.record(
          pluginType, true, LOAD, applicationSerialNumber, describeLoadedContract(inputData));
      return new LoadContractOutputDto(statusCode);
    } catch (CardFullException e) {
      // Rejection logged where it is detected
      transactionRecorder.record(pluginType, false, LOAD, applicationSerialNumber, "");
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_FULL.getCode());
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_LOADING, e.getMessage());
      transactionRecorder.record(pluginType, false, LOAD, applicationSerialNumber, "");
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_LOADING, e);
      transactionRecorder.record(pluginType, false, LOAD, applicationSerialNumber, "");
      return new LoadContractOutputDto(RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      CardResourceServiceProvider.getService().releaseCardResource(samResource);
    }
  }

  private LoadContractOutputDto loadStorageCardContract(
      CardReader cardReader, StorageCard storageCard, LoadContractInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String uid = HexUtil.toHex(storageCard.getUID());

    logger.info(
        "Loading a contract in a storage card [plugin={}, uid={}, productType={}, contractTariff={}, tripsToLoad={}]",
        pluginType,
        uid,
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
      CardData card = cardRepository.readCard(cardReader, storageCard, samResource);
      insertNewContract(inputData.getContractTariff(), inputData.getTripsToLoad(), card);
      int statusCode = cardRepository.writeCard(cardReader, storageCard, samResource, card);
      transactionRecorder.record(pluginType, true, LOAD, uid, describeLoadedContract(inputData));
      return new LoadContractOutputDto(statusCode);
    } catch (CardFullException e) {
      // Rejection logged where it is detected
      transactionRecorder.record(pluginType, false, LOAD, uid, "");
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_FULL.getCode());
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_LOADING, e.getMessage());
      transactionRecorder.record(pluginType, false, LOAD, uid, "");
      return new LoadContractOutputDto(RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_LOADING, e);
      transactionRecorder.record(pluginType, false, LOAD, uid, "");
      return new LoadContractOutputDto(RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      if (samResource != null) {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      }
    }
  }

  /** Returns the description of the loaded contract shown by the dashboard. */
  private static String describeLoadedContract(LoadContractInputDto inputData) {
    return inputData.getContractTariff().getLabel()
        + ((inputData.getTripsToLoad() != 0) ? ": " + inputData.getTripsToLoad() : "");
  }

  /**
   * Inserts the new contract in the card data (contract and event), to be written in the card.
   *
   * @throws IllegalArgumentException If the contract is neither a season pass nor a multi-trip
   *     ticket, or if the number of trips to load is invalid.
   * @throws CardFullException If no record is available for a new contract.
   */
  void insertNewContract(PriorityCode contractTariff, Integer tripsToLoad, CardData card) {

    if (contractTariff != PriorityCode.SEASON_PASS && contractTariff != PriorityCode.MULTI_TRIP) {
      throw new IllegalArgumentException(ONLY_SEASON_PASS_OR_MULTI_TRIP_TICKET_CAN_BE_LOADED);
    }

    EnvironmentHolderStructure environment = card.getEnvironment();
    List<ContractStructure> contracts = card.getContracts();
    EventStructure currentEvent = card.getEvent();
    ContractStructure newContract;
    int newContractNumber;

    // Contract analysis: the priority of the empty records is set to 0, and the one of the expired
    // contracts to 31, in the event to write
    currentEvent = withAnalyzedContractPriorities(contracts, currentEvent);

    // Single contract cards support only one contract (contracts.size() == 1)
    boolean isSingleContractCard = contracts.size() == 1;

    int existingContractNumber = getContractNumber(contractTariff, contracts);
    if (existingContractNumber > 0) {
      // Contract of the same type: trips added or season pass renewed
      newContractNumber = existingContractNumber;
      ContractStructure currentContract = contracts.get(existingContractNumber - 1);
      // build new contract
      if (PriorityCode.MULTI_TRIP == contractTariff) {
        newContract =
            buildMultiTripContract(
                environment.getEnvEndDate(),
                addTrips(currentContract.getCounterValue(), tripsToLoad));
      } else {
        newContract = buildSeasonPassContract(currentContract);
      }
    } else if (isSingleContractCard) {
      // Single contract card: the new contract replaces the existing one only if it is empty,
      // expired or exhausted, as a contract of a multi-contract card
      if (!isReplaceable(contracts.get(0), currentEvent.getContractPriority(1))) {
        logger.warn(
            "Card rejected: the contract of the single contract card is still valid [contractTariff={}, newContractTariff={}]",
            contracts.get(0).getContractTariff(),
            contractTariff);
        throw new CardFullException();
      }
      newContractNumber = 1;
      logger.info(
          "Single contract card: contract replaced [contractTariff={}, newContractTariff={}]",
          contracts.get(0).getContractTariff(),
          contractTariff);
      // build new contract
      if (PriorityCode.MULTI_TRIP == contractTariff) {
        newContract = buildMultiTripContract(environment.getEnvEndDate(), addTrips(0, tripsToLoad));
      } else {
        newContract = buildSeasonPassContract(null);
      }
    } else {
      // Calypso: loading of a new contract, find available position
      newContractNumber = findAvailablePosition(contracts, currentEvent);
      if (newContractNumber == 0) {
        logger.warn("Card rejected: card full, no empty, expired, or exhausted contract record");
        throw new CardFullException();
      }
      // build new contract
      if (PriorityCode.MULTI_TRIP == contractTariff) {
        newContract = buildMultiTripContract(environment.getEnvEndDate(), addTrips(0, tripsToLoad));
      } else {
        newContract = buildSeasonPassContract(null);
      }
    }
    currentEvent =
        currentEvent.withContractPriority(newContractNumber, newContract.getContractTariff());
    // Update the contract and the event
    card.setContract(newContractNumber - 1, newContract);
    card.setEvent(currentEvent);
  }

  /**
   * Returns the provided event with the contract priorities of the contract analysis: 0 (FORBIDDEN)
   * for the empty records (ContractVersionNumber 0), and 31 (EXPIRED) for the contracts whose
   * validity end date is in the past.
   */
  private EventStructure withAnalyzedContractPriorities(
      List<ContractStructure> contracts, EventStructure event) {
    LocalDate today = LocalDate.now(clock);
    EventStructure updatedEvent = event;
    for (int i = 0; i < contracts.size(); i++) {
      ContractStructure contract = contracts.get(i);
      if (contract.getContractVersionNumber() == VersionNumber.UNDEFINED) {
        updatedEvent = updatedEvent.withContractPriority(i + 1, PriorityCode.FORBIDDEN);
      } else if (contract.getContractValidityEndDate().getDate().isBefore(today)) {
        updatedEvent = updatedEvent.withContractPriority(i + 1, PriorityCode.EXPIRED);
      }
    }
    return updatedEvent;
  }

  /**
   * Indicates whether a contract can be replaced by a new contract: empty record (ContractPriority
   * 0), expired or exhausted contract (ContractPriority 31, or multi-trip ticket without trips
   * left).
   */
  private static boolean isReplaceable(ContractStructure contract, PriorityCode contractPriority) {
    return contractPriority == PriorityCode.FORBIDDEN
        || contractPriority == PriorityCode.EXPIRED
        || (contract.getContractTariff() == PriorityCode.MULTI_TRIP
            && (contract.getCounterValue() == null || contract.getCounterValue() <= 0));
  }

  /**
   * Returns the value of the counter of a multi-trip ticket after the loading of the provided
   * trips.
   *
   * @throws IllegalArgumentException If the number of trips to load is not positive, or if the
   *     counter cannot hold the new value.
   */
  private static int addTrips(int counterValue, Integer tripsToLoad) {
    if (tripsToLoad == null || tripsToLoad < 1) {
      throw new IllegalArgumentException(
          "Invalid number of trips to load [tripsToLoad=" + tripsToLoad + "]");
    }
    long newCounterValue = (long) counterValue + tripsToLoad;
    if (newCounterValue > MAX_COUNTER_VALUE) {
      throw new IllegalArgumentException(
          "Counter capacity exceeded [counterValue="
              + counterValue
              + ", tripsToLoad="
              + tripsToLoad
              + ", maxCounterValue="
              + MAX_COUNTER_VALUE
              + "]");
    }
    return (int) newCounterValue;
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
    DateCompact contractSaleDate = new DateCompact(LocalDate.now(clock));
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
   * end date of the renewed season pass when it has not expired (the renewal extends its validity).
   *
   * @param renewedSeasonPass The season pass renewed, null for a new season pass.
   */
  private ContractStructure buildSeasonPassContract(ContractStructure renewedSeasonPass) {
    DateCompact contractSaleDate = new DateCompact(LocalDate.now(clock));
    DateCompact validityStartDate = contractSaleDate;
    if (renewedSeasonPass != null
        && !renewedSeasonPass
            .getContractValidityEndDate()
            .getDate()
            .isBefore(LocalDate.now(clock))) {
      validityStartDate = renewedSeasonPass.getContractValidityEndDate();
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
