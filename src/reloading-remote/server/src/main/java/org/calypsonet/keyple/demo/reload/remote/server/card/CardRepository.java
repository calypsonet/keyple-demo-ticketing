/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService;
import org.calypsonet.keyple.demo.common.constants.CardConstants;
import org.calypsonet.keyple.demo.common.model.ContractStructure;
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure;
import org.calypsonet.keyple.demo.common.model.EventStructure;
import org.calypsonet.keyple.demo.common.model.type.DateCompact;
import org.calypsonet.keyple.demo.common.model.type.PriorityCode;
import org.calypsonet.keyple.demo.common.model.type.VersionNumber;
import org.calypsonet.keyple.demo.common.parsers.*;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.util.json.JsonUtil;
import org.eclipse.keypop.calypso.card.CalypsoCardApiFactory;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.calypso.card.card.FileData;
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager;
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ChannelControl;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.CardSelectionManager;
import org.eclipse.keypop.reader.selection.CardSelectionResult;
import org.eclipse.keypop.storagecard.MifareClassicKeyType;
import org.eclipse.keypop.storagecard.card.ProductType;
import org.eclipse.keypop.storagecard.card.StorageCard;
import org.eclipse.keypop.storagecard.transaction.StorageCardTransactionManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CardRepository {

  private static final Logger logger = LoggerFactory.getLogger(CardRepository.class);

  private static final String CALYPSO_SESSION_CLOSED = "Calypso Session Closed.";

  private static CardSelectionManager createCardSelectionManager() {
    ReaderApiFactory readerApiFactory = SmartCardServiceProvider.getService().getReaderApiFactory();
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();
    CalypsoCardApiFactory calypsoCardApiFactory =
        CalypsoExtensionService.getInstance().getCalypsoCardApiFactory();

    cardSelectionManager.prepareSelection(
        readerApiFactory
            .createIsoCardSelector()
            .filterByDfName(CardConstants.Companion.getAID_KEYPLE_GENERIC()),
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    cardSelectionManager.prepareSelection(
        readerApiFactory
            .createIsoCardSelector()
            .filterByDfName(CardConstants.Companion.getAID_CALYPSO_LIGHT()),
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    cardSelectionManager.prepareSelection(
        readerApiFactory
            .createIsoCardSelector()
            .filterByDfName(CardConstants.Companion.getAID_CD_LIGHT_GTML()),
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    cardSelectionManager.prepareSelection(
        readerApiFactory
            .createIsoCardSelector()
            .filterByDfName(CardConstants.Companion.getAID_NORMALIZED_IDF()),
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());
    return cardSelectionManager;
  }

  String exportCardSelectionScenario() {
    return createCardSelectionManager().exportCardSelectionScenario();
  }

  CalypsoCard importProcessedCardSelectionScenario(
      String processedCardSelectionScenarioJsonString) {

    // Prepare the card selection scenario
    CardSelectionManager cardSelectionManager = createCardSelectionManager();

    // Import the processed card selection scenario
    CardSelectionResult cardSelectionResult =
        cardSelectionManager.importProcessedCardSelectionScenario(
            processedCardSelectionScenarioJsonString);

    // Check the selection result.
    if (cardSelectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("Selection error: AID not found");
    }

    // Get the SmartCard resulting of the selection.
    return (CalypsoCard) cardSelectionResult.getActiveSmartCard();
  }

  CalypsoCard selectCard(CardReader cardReader) {

    CardSelectionManager cardSelectionManager = createCardSelectionManager();

    // Actual card communication: run the selection scenario.
    CardSelectionResult selectionResult =
        cardSelectionManager.processCardSelectionScenario(cardReader);

    // Check the selection result.
    if (selectionResult.getActiveSmartCard() == null) {
      throw new IllegalStateException("Selection error: AID not found");
    }

    // Get the SmartCard resulting of the selection.
    return (CalypsoCard) selectionResult.getActiveSmartCard();
  }

  Card readCard(CardReader cardReader, CalypsoCard calypsoCard, CardResource samResource) {
    int contractCount = getContractCount(calypsoCard);

    SecureRegularModeTransactionManager cardTransactionManager =
        initCardTransactionManager(cardReader, calypsoCard, samResource);

    logger.info("Open Calypso Session (LOAD)...");
    cardTransactionManager
        .prepareOpenSecureSession(WriteAccessLevel.LOAD)
        .prepareReadRecords(
            CardConstants.SFI_ENVIRONMENT_AND_HOLDER,
            1,
            1,
            CardConstants.ENVIRONMENT_HOLDER_RECORD_SIZE_BYTES)
        .prepareReadRecords(
            CardConstants.SFI_EVENTS_LOG, 1, 1, CardConstants.EVENT_RECORD_SIZE_BYTES)
        .prepareReadRecords(
            CardConstants.SFI_CONTRACTS, 1, contractCount, CardConstants.CONTRACT_RECORD_SIZE_BYTES)
        .prepareReadCounter(CardConstants.SFI_COUNTERS, contractCount)
        .prepareCloseSecureSession()
        .processCommands(ChannelControl.KEEP_OPEN);
    logger.info(CALYPSO_SESSION_CLOSED);

    return parse(calypsoCard);
  }

  Card readCard(CardReader cardReader, StorageCard storageCard, CardResource samResource) {
    logger.info(
        "Reading StorageCard: ProductType={}, BlockSize={} bytes, BlockCount={}, TotalCapacity={} bytes",
        storageCard.getProductType(),
        storageCard.getProductType().getBlockSize(),
        storageCard.getProductType().getBlockCount(),
        storageCard.getProductType().getBlockSize() * storageCard.getProductType().getBlockCount());

    StorageCardExtensionService storageCardExtension = StorageCardExtensionService.getInstance();
    StorageCardTransactionManager cardTransactionManager =
        storageCardExtension
            .getStorageCardApiFactory()
            .createStorageCardTransactionManager(cardReader, storageCard);

    // Mifare Classic requires authentication before reading
    if (storageCard.getProductType().hasAuthentication()) {
      logger.info(
          "Mifare Classic detected - authenticating sector 1 (block 4) with KEY_A, keyNumber=0");
      cardTransactionManager.prepareMifareClassicAuthenticate(4, MifareClassicKeyType.KEY_A, 0);
    } else {
      logger.info("No authentication required for StorageCard: {}", JsonUtil.toJson(storageCard));
    }

    // For Mifare Classic 1K, read only data blocks 4-6 (sector 1, avoiding sector trailer block 7)
    // For other storage cards, read all blocks
    int startBlock = 0;
    int endBlock = storageCard.getProductType().getBlockCount() - 1;
    if (storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K) {
      startBlock = 4;
      endBlock = 6;
      logger.info(
          "Mifare Classic 1K - reading data blocks {} to {} (avoiding sector trailer block 7)",
          startBlock,
          endBlock);
    } else {
      logger.info("Preparing to read blocks {} to {} from StorageCard", startBlock, endBlock);
    }

    cardTransactionManager
        .prepareReadBlocks(startBlock, endBlock)
        .processCommands(ChannelControl.KEEP_OPEN);

    logger.info("StorageCard read completed successfully");
    return parse(storageCard);
  }

  int writeCard(
      CardReader cardReader, CalypsoCard calypsoCard, CardResource samResource, Card card) {

    SecureRegularModeTransactionManager cardTransactionManager =
        initCardTransactionManager(cardReader, calypsoCard, samResource);

    logger.info("Open Calypso Session (LOAD)...");
    cardTransactionManager.prepareOpenSecureSession(WriteAccessLevel.LOAD);

    /* Update contract records */
    if (!card.getUpdatedContracts().isEmpty()) {
      int contractCount = card.getContracts().size();
      for (int i = 0; i < contractCount; i++) {
        int contractNumber = i + 1;
        ContractStructure contract = card.getContracts().get(i);
        if (card.getUpdatedContracts().contains(contract)) {
          // update contract
          cardTransactionManager.prepareUpdateRecord(
              CardConstants.SFI_CONTRACTS,
              contractNumber,
              new ContractStructureParser().generate(contract));
          // update counter
          if (contract.getCounterValue() != null) {
            cardTransactionManager.prepareSetCounter(
                CardConstants.SFI_COUNTERS, contractNumber, contract.getCounterValue());
          }
        }
      }
    }
    /* Update event */
    if (Boolean.TRUE.equals(card.isEventUpdated())) {
      cardTransactionManager.prepareUpdateRecord(
          CardConstants.SFI_EVENTS_LOG,
          1,
          new EventStructureParser().generate(buildEvent(card.getEvent(), card.getContracts())));
    }

    cardTransactionManager.prepareCloseSecureSession().processCommands(ChannelControl.KEEP_OPEN);
    logger.info(CALYPSO_SESSION_CLOSED);

    return 0;
  }

  int writeCard(
      CardReader cardReader, StorageCard storageCard, CardResource samResource, Card card) {
    logger.info(
        "Writing StorageCard: ProductType={}, UpdatedContracts={}, EventUpdated={}",
        storageCard.getProductType(),
        card.getUpdatedContracts().size(),
        card.isEventUpdated());

    StorageCardExtensionService storageCardExtension = StorageCardExtensionService.getInstance();
    StorageCardTransactionManager cardTransactionManager =
        storageCardExtension
            .getStorageCardApiFactory()
            .createStorageCardTransactionManager(cardReader, storageCard);

    // Mifare Classic requires authentication before writing
    if (storageCard.getProductType().hasAuthentication()) {
      logger.info(
          "Mifare Classic detected - authenticating sector 1 (block 4) with KEY_A, keyNumber=0 before write");
      cardTransactionManager.prepareMifareClassicAuthenticate(4, MifareClassicKeyType.KEY_A, 0);
    }

    /* Update contract records */
    // TODO simplify
    if (!card.getUpdatedContracts().isEmpty()) {
      int contractCount = card.getContracts().size();
      for (int i = 0; i < contractCount; i++) {
        ContractStructure contract = card.getContracts().get(i);
        if (card.getUpdatedContracts().contains(contract)) {
          // For Mifare Classic 1K, write to block 5 (16 bytes)
          // For other cards, write to blocks 8-11 (4×4 bytes = 16 bytes)
          int contractBlock =
              (storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K)
                  ? 5
                  : CardConstants.SC_CONTRACT_FIRST_BLOCK;
          logger.info("Updating contract on StorageCard at block {}", contractBlock);
          cardTransactionManager.prepareWriteBlocks(
              contractBlock, new ScContractStructureParser().generate(contract));
        }
      }
    }
    /* Update event */
    if (Boolean.TRUE.equals(card.isEventUpdated())) {
      // For Mifare Classic 1K, write to block 6 (16 bytes)
      // For other cards, write to blocks 12-15 (4×4 bytes = 16 bytes)
      int eventBlock =
          (storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K)
              ? 6
              : CardConstants.SC_EVENT_FIRST_BLOCK;
      logger.info("Updating event on StorageCard at block {}", eventBlock);
      cardTransactionManager.prepareWriteBlocks(
          eventBlock,
          new ScEventStructureParser().generate(buildEvent(card.getEvent(), card.getContracts())));
    }

    cardTransactionManager.processCommands(ChannelControl.KEEP_OPEN);
    logger.info("StorageCard write completed successfully");
    return 0;
  }

  void initCard(CardReader cardReader, CalypsoCard calypsoCard, CardResource samResource) {

    SecureRegularModeTransactionManager cardTransactionManager =
        initCardTransactionManager(cardReader, calypsoCard, samResource);

    logger.info("Open Calypso Session (PERSONALIZATION)...");
    cardTransactionManager.prepareOpenSecureSession(WriteAccessLevel.PERSONALIZATION);

    // Fill the environment structure with predefined values
    cardTransactionManager.prepareUpdateRecord(
        CardConstants.SFI_ENVIRONMENT_AND_HOLDER,
        1,
        new EnvironmentHolderStructureParser().generate(buildEnvironmentHolderStructure()));

    // Clear the first event (update with a byte array filled with 0 s).
    cardTransactionManager.prepareUpdateRecord(
        CardConstants.SFI_EVENTS_LOG, 1, new byte[CardConstants.EVENT_RECORD_SIZE_BYTES]);

    // Clear all contracts (update with a byte array filled with 0 s).
    int contractCount = getContractCount(calypsoCard);
    for (int i = 1; i <= contractCount; i++) {
      cardTransactionManager.prepareUpdateRecord(
          CardConstants.SFI_CONTRACTS, i, new byte[CardConstants.CONTRACT_RECORD_SIZE_BYTES]);
    }

    // Clear the counter-file (update with a byte array filled with 0 s).
    cardTransactionManager.prepareUpdateRecord(
        CardConstants.SFI_COUNTERS, 1, new byte[contractCount * 3]);

    cardTransactionManager.prepareCloseSecureSession().processCommands(ChannelControl.KEEP_OPEN);
    logger.info(CALYPSO_SESSION_CLOSED);
  }

  void initCard(CardReader cardReader, StorageCard storageCard, CardResource samResource) {

    logger.info(
        "Initializing StorageCard: ProductType={}, BlockSize={} bytes",
        storageCard.getProductType(),
        storageCard.getProductType().getBlockSize());

    StorageCardExtensionService storageCardExtension = StorageCardExtensionService.getInstance();
    StorageCardTransactionManager cardTransactionManager =
        storageCardExtension
            .getStorageCardApiFactory()
            .createStorageCardTransactionManager(cardReader, storageCard);

    // Mifare Classic requires authentication before writing
    if (storageCard.getProductType().hasAuthentication()) {
      logger.info(
          "Mifare Classic detected - authenticating sector 1 (block 4) with KEY_A, keyNumber=0 before initialization");
      cardTransactionManager.prepareMifareClassicAuthenticate(4, MifareClassicKeyType.KEY_A, 0);
    }

    // For Mifare Classic 1K, use single-block layout (blocks 4, 5, 6)
    // For other cards, use multi-block layout (blocks 4-7, 8-11, 12-15)
    boolean isMifareClassic = storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K;
    int environmentBlock =
        isMifareClassic ? 4 : CardConstants.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK;
    int contractBlock = isMifareClassic ? 5 : CardConstants.SC_CONTRACT_FIRST_BLOCK;
    int eventBlock = isMifareClassic ? 6 : CardConstants.SC_EVENT_FIRST_BLOCK;

    // Fill the environment structure with predefined values
    logger.info("Writing environment structure at block {}", environmentBlock);
    cardTransactionManager.prepareWriteBlocks(
        environmentBlock,
        new ScEnvironmentHolderStructureParser().generate(buildEnvironmentHolderStructure()));

    // Clear the first event (update with a byte array filled with 0 s).
    logger.info("Clearing event at block {}", eventBlock);
    cardTransactionManager.prepareWriteBlocks(
        eventBlock, new byte[CardConstants.SC_EVENT_RECORD_SIZE_BYTES]);

    // Clear all contracts (update with a byte array filled with 0 s).
    logger.info("Clearing contract at block {}", contractBlock);
    cardTransactionManager.prepareWriteBlocks(
        contractBlock, new byte[CardConstants.SC_CONTRACT_RECORD_SIZE_BYTES]);

    cardTransactionManager.processCommands(ChannelControl.KEEP_OPEN);
    logger.info("StorageCard initialization completed successfully");
  }

  @NotNull
  private SecureRegularModeTransactionManager initCardTransactionManager(
      CardReader cardReader, CalypsoCard calypsoCard, CardResource samResource) {
    CalypsoCardApiFactory calypsoCardApiFactory =
        CalypsoExtensionService.getInstance().getCalypsoCardApiFactory();
    SymmetricCryptoSecuritySetting cardSecuritySetting =
        calypsoCardApiFactory
            .createSymmetricCryptoSecuritySetting(
                LegacySamExtensionService.getInstance()
                    .getLegacySamApiFactory()
                    .createSymmetricCryptoCardTransactionManagerFactory(
                        samResource.getReader(), (LegacySam) samResource.getSmartCard()))
            .enableMultipleSession()
            .assignDefaultKif(
                WriteAccessLevel.PERSONALIZATION, CardConstants.DEFAULT_KIF_PERSONALIZATION)
            .assignDefaultKif(WriteAccessLevel.LOAD, CardConstants.DEFAULT_KIF_LOAD)
            .assignDefaultKif(WriteAccessLevel.DEBIT, CardConstants.DEFAULT_KIF_DEBIT);

    return calypsoCardApiFactory.createSecureRegularModeTransactionManager(
        cardReader, calypsoCard, cardSecuritySetting);
  }

  private EnvironmentHolderStructure buildEnvironmentHolderStructure() {
    // calculate issuing date
    Instant now = Instant.now();
    // calculate env end date
    LocalDate envEndDate =
        now.atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1).plusYears(6);
    return new EnvironmentHolderStructure(
        VersionNumber.CURRENT_VERSION,
        1,
        new DateCompact(LocalDate.now()),
        new DateCompact(envEndDate),
        null,
        null);
  }

  private EventStructure buildEvent(EventStructure oldEvent, List<ContractStructure> contracts) {
    int contractCount = contracts.size();
    return new EventStructure(
        VersionNumber.CURRENT_VERSION,
        oldEvent.getEventDateStamp(),
        oldEvent.getEventTimeStamp(),
        oldEvent.getEventLocation(),
        oldEvent.getEventContractUsed(),
        contracts.get(0).getContractTariff(),
        contractCount >= 2 ? contracts.get(1).getContractTariff() : PriorityCode.FORBIDDEN,
        contractCount >= 3 ? contracts.get(2).getContractTariff() : PriorityCode.FORBIDDEN,
        contractCount >= 4 ? contracts.get(3).getContractTariff() : PriorityCode.FORBIDDEN);
  }

  private Card parse(CalypsoCard calypsoCard) {
    // Parse environment
    EnvironmentHolderStructure environment =
        new EnvironmentHolderStructureParser()
            .parse(
                calypsoCard
                    .getFileBySfi(CardConstants.SFI_ENVIRONMENT_AND_HOLDER)
                    .getData()
                    .getContent());
    // parse contracts
    List<ContractStructure> contracts = new ArrayList<>();
    FileData fileData = calypsoCard.getFileBySfi(CardConstants.SFI_CONTRACTS).getData();
    if (fileData != null) {
      int contractCount = getContractCount(calypsoCard);
      for (int i = 1; i < contractCount + 1; i++) {
        ContractStructure contract = new ContractStructureParser().parse(fileData.getContent(i));
        contracts.add(contract);
        // update counter tied to contract
        int counterValue =
            calypsoCard
                .getFileBySfi(CardConstants.SFI_COUNTERS)
                .getData()
                .getContentAsCounterValue(i);
        contract.setCounterValue(counterValue);
      }
    }
    // parse event
    EventStructure event =
        new EventStructureParser()
            .parse(calypsoCard.getFileBySfi(CardConstants.SFI_EVENTS_LOG).getData().getContent());
    return new Card(environment, contracts, event);
  }

  private int getContractCount(CalypsoCard calypsoCard) {
    if (calypsoCard.getProductType() == CalypsoCard.ProductType.BASIC) {
      return 1;
    } else if (calypsoCard.getProductType() == CalypsoCard.ProductType.LIGHT) {
      return 2;
    }
    return 4;
  }

  private Card parse(StorageCard storageCard) {
    EnvironmentHolderStructure environment;
    List<ContractStructure> contracts = new ArrayList<>();
    EventStructure event;

    // Mifare Classic 1K uses a different layout: 1 block of 16 bytes per structure
    // (blocks 4, 5, 6) instead of 4 blocks of 4 bytes (blocks 4-7, 8-11, 12-15)
    if (storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K) {
      logger.info(
          "Parsing Mifare Classic 1K data: Using single-block layout (block 4=Environment, 5=Contract, 6=Event)");

      // Parse environment from block 4 (16 bytes)
      environment = new ScEnvironmentHolderStructureParser().parse(storageCard.getBlock(4));

      // Parse contract from block 5 (16 bytes)
      contracts.add(new ScContractStructureParser().parse(storageCard.getBlock(5)));

      // Parse event from block 6 (16 bytes)
      event = new ScEventStructureParser().parse(storageCard.getBlock(6));

    } else {
      // MIFARE Ultralight and ST25 SRT512 use multi-block layout
      logger.info(
          "Parsing StorageCard data: ProductType={}, Reading blocks {}-{} for environment",
          storageCard.getProductType(),
          CardConstants.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
          CardConstants.SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK);

      // Parse environment from blocks 4-7 (4 blocks × 4 bytes = 16 bytes)
      environment =
          new ScEnvironmentHolderStructureParser()
              .parse(
                  storageCard.getBlocks(
                      CardConstants.SC_ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                      CardConstants.SC_ENVIRONMENT_AND_HOLDER_LAST_BLOCK));

      logger.info(
          "Parsing contract from blocks {}-{}",
          CardConstants.SC_CONTRACT_FIRST_BLOCK,
          CardConstants.SC_COUNTER_LAST_BLOCK);

      // Parse contract from blocks 8-11 (4 blocks × 4 bytes = 16 bytes)
      contracts.add(
          new ScContractStructureParser()
              .parse(
                  storageCard.getBlocks(
                      CardConstants.SC_CONTRACT_FIRST_BLOCK, CardConstants.SC_COUNTER_LAST_BLOCK)));

      // Parse event from blocks 12-15 (4 blocks × 4 bytes = 16 bytes)
      event =
          new ScEventStructureParser()
              .parse(
                  storageCard.getBlocks(
                      CardConstants.SC_EVENT_FIRST_BLOCK, CardConstants.SC_EVENT_LAST_BLOCK));
    }

    return new Card(environment, contracts, event);
  }
}
