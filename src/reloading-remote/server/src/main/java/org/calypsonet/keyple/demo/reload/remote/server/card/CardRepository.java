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

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService;
import org.calypsonet.keyple.demo.common.codecs.CalypsoContractCodec;
import org.calypsonet.keyple.demo.common.codecs.CalypsoEnvironmentHolderCodec;
import org.calypsonet.keyple.demo.common.codecs.CalypsoEventCodec;
import org.calypsonet.keyple.demo.common.codecs.StorageCardContractCodec;
import org.calypsonet.keyple.demo.common.codecs.StorageCardEnvironmentHolderCodec;
import org.calypsonet.keyple.demo.common.codecs.StorageCardEventCodec;
import org.calypsonet.keyple.demo.common.constants.CalypsoAids;
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles;
import org.calypsonet.keyple.demo.common.constants.DefaultKifs;
import org.calypsonet.keyple.demo.common.constants.StorageCardBlocks;
import org.calypsonet.keyple.demo.common.model.ContractStructure;
import org.calypsonet.keyple.demo.common.model.EnvironmentHolderStructure;
import org.calypsonet.keyple.demo.common.model.EventStructure;
import org.calypsonet.keyple.demo.common.model.type.DateCompact;
import org.calypsonet.keyple.demo.common.model.type.VersionNumber;
import org.eclipse.keyple.card.calypso.CalypsoExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.service.resource.CardResource;
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

  private static final String SECURE_SESSION_CLOSED = "Secure session closed";

  /**
   * Number of the next personalized card application (EnvApplicationNumber, unique identifier in
   * the system), initialized with the start time of the server in seconds so that the numbers are
   * not reused after a restart.
   */
  private final AtomicInteger nextApplicationNumber =
      new AtomicInteger((int) Instant.now().getEpochSecond());

  private static CardSelectionManager createCardSelectionManager() {
    ReaderApiFactory readerApiFactory = SmartCardServiceProvider.getService().getReaderApiFactory();
    CardSelectionManager cardSelectionManager = readerApiFactory.createCardSelectionManager();
    CalypsoCardApiFactory calypsoCardApiFactory =
        CalypsoExtensionService.getInstance().getCalypsoCardApiFactory();

    cardSelectionManager.prepareSelection(
        readerApiFactory.createIsoCardSelector().filterByDfName(CalypsoAids.KEYPLE_GENERIC),
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    cardSelectionManager.prepareSelection(
        readerApiFactory.createIsoCardSelector().filterByDfName(CalypsoAids.CALYPSO_LIGHT),
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    cardSelectionManager.prepareSelection(
        readerApiFactory.createIsoCardSelector().filterByDfName(CalypsoAids.CD_LIGHT_GTML),
        calypsoCardApiFactory.createCalypsoCardSelectionExtension().acceptInvalidatedCard());

    cardSelectionManager.prepareSelection(
        readerApiFactory.createIsoCardSelector().filterByDfName(CalypsoAids.NORMALIZED_IDF),
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
      throw new IllegalStateException("Card application not found");
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
      throw new IllegalStateException("Card application not found");
    }

    // Get the SmartCard resulting of the selection.
    return (CalypsoCard) selectionResult.getActiveSmartCard();
  }

  CardData readCard(CardReader cardReader, CalypsoCard calypsoCard, CardResource samResource) {
    int contractRecordCount = getContractRecordCount(calypsoCard);

    SecureRegularModeTransactionManager cardTransactionManager =
        createCardTransactionManager(cardReader, calypsoCard, samResource);

    logger.debug("Opening the secure session [writeAccessLevel=LOAD]");
    cardTransactionManager
        .prepareOpenSecureSession(WriteAccessLevel.LOAD)
        .prepareReadRecords(
            CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER,
            1,
            1,
            CalypsoFiles.ENVIRONMENT_HOLDER_RECORD_SIZE_BYTES)
        .prepareReadRecords(CalypsoFiles.SFI_EVENT_LOG, 1, 1, CalypsoFiles.EVENT_RECORD_SIZE_BYTES)
        .prepareReadRecords(
            CalypsoFiles.SFI_CONTRACTS,
            1,
            contractRecordCount,
            CalypsoFiles.CONTRACT_RECORD_SIZE_BYTES)
        .prepareReadCounter(CalypsoFiles.SFI_COUNTERS, contractRecordCount)
        .prepareCloseSecureSession()
        .processCommands(ChannelControl.KEEP_OPEN);
    logger.debug(SECURE_SESSION_CLOSED);

    return decodeCardData(calypsoCard);
  }

  CardData readCard(CardReader cardReader, StorageCard storageCard, CardResource samResource) {
    logger.debug(
        "Reading the storage card [productType={}, blockSize={}, blockCount={}, capacity={}]",
        storageCard.getProductType(),
        storageCard.getProductType().getBlockSize(),
        storageCard.getProductType().getBlockCount(),
        storageCard.getProductType().getBlockSize() * storageCard.getProductType().getBlockCount());

    StorageCardExtensionService storageCardExtension = StorageCardExtensionService.getInstance();
    StorageCardTransactionManager cardTransactionManager =
        storageCardExtension
            .getStorageCardApiFactory()
            .createStorageCardTransactionManager(cardReader, storageCard);

    // MIFARE Classic requires authentication before reading
    if (storageCard.getProductType().hasAuthentication()) {
      logger.debug(
          "Authenticating the MIFARE Classic sector [sector=1, block=4, keyType=KEY_A, keyNumber=0]");
      cardTransactionManager.prepareMifareClassicAuthenticate(4, MifareClassicKeyType.KEY_A, 0);
    } else {
      logger.debug("No authentication required [productType={}]", storageCard.getProductType());
    }

    // For MIFARE Classic 1K, read only data blocks 4-6 (sector 1, avoiding sector trailer block 7)
    // For other storage cards, read all blocks
    int startBlock = 0;
    int endBlock = storageCard.getProductType().getBlockCount() - 1;
    if (storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K) {
      startBlock = 4;
      endBlock = 6;
      logger.debug(
          "Reading the MIFARE Classic 1K data blocks, sector trailer excluded [firstBlock={}, lastBlock={}]",
          startBlock,
          endBlock);
    } else {
      logger.debug("Reading the blocks [firstBlock={}, lastBlock={}]", startBlock, endBlock);
    }

    cardTransactionManager
        .prepareReadBlocks(startBlock, endBlock)
        .processCommands(ChannelControl.KEEP_OPEN);

    logger.debug("Storage card read");
    return decodeCardData(storageCard);
  }

  int writeCard(
      CardReader cardReader, CalypsoCard calypsoCard, CardResource samResource, CardData card) {

    SecureRegularModeTransactionManager cardTransactionManager =
        createCardTransactionManager(cardReader, calypsoCard, samResource);

    logger.debug("Opening the secure session [writeAccessLevel=LOAD]");
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
              CalypsoFiles.SFI_CONTRACTS,
              contractNumber,
              CalypsoContractCodec.INSTANCE.encode(contract));
          // update counter
          if (contract.getCounterValue() != null) {
            cardTransactionManager.prepareSetCounter(
                CalypsoFiles.SFI_COUNTERS, contractNumber, contract.getCounterValue());
          }
        }
      }
    }
    /* Update event */
    if (Boolean.TRUE.equals(card.isEventUpdated())) {
      cardTransactionManager.prepareUpdateRecord(
          CalypsoFiles.SFI_EVENT_LOG,
          1,
          CalypsoEventCodec.INSTANCE.encode(buildEvent(card.getEvent())));
    }

    cardTransactionManager.prepareCloseSecureSession().processCommands(ChannelControl.KEEP_OPEN);
    logger.debug(SECURE_SESSION_CLOSED);

    return 0;
  }

  int writeCard(
      CardReader cardReader, StorageCard storageCard, CardResource samResource, CardData card) {
    logger.debug(
        "Writing the storage card [productType={}, updatedContracts={}, eventUpdated={}]",
        storageCard.getProductType(),
        card.getUpdatedContracts().size(),
        card.isEventUpdated());

    StorageCardExtensionService storageCardExtension = StorageCardExtensionService.getInstance();
    StorageCardTransactionManager cardTransactionManager =
        storageCardExtension
            .getStorageCardApiFactory()
            .createStorageCardTransactionManager(cardReader, storageCard);

    // MIFARE Classic requires authentication before writing
    if (storageCard.getProductType().hasAuthentication()) {
      logger.debug(
          "Authenticating the MIFARE Classic sector [sector=1, block=4, keyType=KEY_A, keyNumber=0]");
      cardTransactionManager.prepareMifareClassicAuthenticate(4, MifareClassicKeyType.KEY_A, 0);
    }

    /* Update contract records */
    // TODO simplify
    if (!card.getUpdatedContracts().isEmpty()) {
      int contractCount = card.getContracts().size();
      for (int i = 0; i < contractCount; i++) {
        ContractStructure contract = card.getContracts().get(i);
        if (card.getUpdatedContracts().contains(contract)) {
          // For MIFARE Classic 1K, write to block 5 (16 bytes)
          // For other cards, write to blocks 8-11 (4×4 bytes = 16 bytes)
          int contractBlock =
              (storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K)
                  ? 5
                  : StorageCardBlocks.CONTRACT_FIRST_BLOCK;
          logger.debug("Updating the contract [block={}]", contractBlock);
          cardTransactionManager.prepareWriteBlocks(
              contractBlock, StorageCardContractCodec.INSTANCE.encode(contract));
        }
      }
    }
    /* Update event */
    if (Boolean.TRUE.equals(card.isEventUpdated())) {
      // For MIFARE Classic 1K, write to block 6 (16 bytes)
      // For other cards, write to blocks 12-15 (4×4 bytes = 16 bytes)
      int eventBlock =
          (storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K)
              ? 6
              : StorageCardBlocks.EVENT_FIRST_BLOCK;
      logger.debug("Updating the event [block={}]", eventBlock);
      cardTransactionManager.prepareWriteBlocks(
          eventBlock, StorageCardEventCodec.INSTANCE.encode(buildEvent(card.getEvent())));
    }

    cardTransactionManager.processCommands(ChannelControl.KEEP_OPEN);
    logger.debug("Storage card written");
    return 0;
  }

  void personalizeCard(CardReader cardReader, CalypsoCard calypsoCard, CardResource samResource) {

    SecureRegularModeTransactionManager cardTransactionManager =
        createCardTransactionManager(cardReader, calypsoCard, samResource);

    logger.debug("Opening the secure session [writeAccessLevel=PERSONALIZATION]");
    cardTransactionManager.prepareOpenSecureSession(WriteAccessLevel.PERSONALIZATION);

    // Fill the environment structure with predefined values
    cardTransactionManager.prepareUpdateRecord(
        CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER,
        1,
        CalypsoEnvironmentHolderCodec.INSTANCE.encode(buildEnvironmentHolder()));

    // Clear the first event (update with a byte array filled with 0 s).
    cardTransactionManager.prepareUpdateRecord(
        CalypsoFiles.SFI_EVENT_LOG, 1, new byte[CalypsoFiles.EVENT_RECORD_SIZE_BYTES]);

    // Clear all contracts (update with a byte array filled with 0 s).
    int contractRecordCount = getContractRecordCount(calypsoCard);
    for (int i = 1; i <= contractRecordCount; i++) {
      cardTransactionManager.prepareUpdateRecord(
          CalypsoFiles.SFI_CONTRACTS, i, new byte[CalypsoFiles.CONTRACT_RECORD_SIZE_BYTES]);
    }

    // Clear the counter-file (update with a byte array filled with 0 s).
    cardTransactionManager.prepareUpdateRecord(
        CalypsoFiles.SFI_COUNTERS, 1, new byte[contractRecordCount * 3]);

    cardTransactionManager.prepareCloseSecureSession().processCommands(ChannelControl.KEEP_OPEN);
    logger.debug(SECURE_SESSION_CLOSED);
  }

  void personalizeCard(CardReader cardReader, StorageCard storageCard, CardResource samResource) {

    logger.debug(
        "Personalizing the storage card [productType={}, blockSize={}]",
        storageCard.getProductType(),
        storageCard.getProductType().getBlockSize());

    StorageCardExtensionService storageCardExtension = StorageCardExtensionService.getInstance();
    StorageCardTransactionManager cardTransactionManager =
        storageCardExtension
            .getStorageCardApiFactory()
            .createStorageCardTransactionManager(cardReader, storageCard);

    // MIFARE Classic requires authentication before writing
    if (storageCard.getProductType().hasAuthentication()) {
      logger.debug(
          "Authenticating the MIFARE Classic sector [sector=1, block=4, keyType=KEY_A, keyNumber=0]");
      cardTransactionManager.prepareMifareClassicAuthenticate(4, MifareClassicKeyType.KEY_A, 0);
    }

    // For MIFARE Classic 1K, use single-block layout (blocks 4, 5, 6)
    // For other cards, use multi-block layout (blocks 4-7, 8-11, 12-15)
    boolean isMifareClassic = storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K;
    int environmentBlock =
        isMifareClassic ? 4 : StorageCardBlocks.ENVIRONMENT_AND_HOLDER_FIRST_BLOCK;
    int contractBlock = isMifareClassic ? 5 : StorageCardBlocks.CONTRACT_FIRST_BLOCK;
    int eventBlock = isMifareClassic ? 6 : StorageCardBlocks.EVENT_FIRST_BLOCK;

    // Fill the environment structure with predefined values
    logger.debug("Writing the environment [block={}]", environmentBlock);
    cardTransactionManager.prepareWriteBlocks(
        environmentBlock,
        StorageCardEnvironmentHolderCodec.INSTANCE.encode(buildEnvironmentHolder()));

    // Clear the first event (update with a byte array filled with 0 s).
    logger.debug("Clearing the event [block={}]", eventBlock);
    cardTransactionManager.prepareWriteBlocks(
        eventBlock, new byte[StorageCardBlocks.EVENT_RECORD_SIZE_BYTES]);

    // Clear all contracts (update with a byte array filled with 0 s).
    logger.debug("Clearing the contract [block={}]", contractBlock);
    cardTransactionManager.prepareWriteBlocks(
        contractBlock, new byte[StorageCardBlocks.CONTRACT_RECORD_SIZE_BYTES]);

    cardTransactionManager.processCommands(ChannelControl.KEEP_OPEN);
    logger.debug("Storage card personalized");
  }

  @NotNull
  private SecureRegularModeTransactionManager createCardTransactionManager(
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
            .assignDefaultKif(WriteAccessLevel.PERSONALIZATION, DefaultKifs.PERSONALIZATION)
            .assignDefaultKif(WriteAccessLevel.LOAD, DefaultKifs.LOAD)
            .assignDefaultKif(WriteAccessLevel.DEBIT, DefaultKifs.DEBIT);

    return calypsoCardApiFactory.createSecureRegularModeTransactionManager(
        cardReader, calypsoCard, cardSecuritySetting);
  }

  private EnvironmentHolderStructure buildEnvironmentHolder() {
    // calculate issuing date
    Instant now = Instant.now();
    // calculate env end date
    LocalDate envEndDate =
        now.atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1).plusYears(6);
    return new EnvironmentHolderStructure(
        VersionNumber.CURRENT_VERSION,
        nextApplicationNumber.getAndIncrement(),
        new DateCompact(LocalDate.now()),
        new DateCompact(envEndDate),
        null,
        null);
  }

  /**
   * Builds the event to write: the last event with the current version number, and its contract
   * priorities. Only the priority of the loaded contract is updated (by the contract loading): the
   * other priorities are kept (e.g. 31 for an expired or exhausted contract).
   */
  private EventStructure buildEvent(EventStructure event) {
    return new EventStructure(
        VersionNumber.CURRENT_VERSION,
        event.getEventDateStamp(),
        event.getEventTimeStamp(),
        event.getEventLocation(),
        event.getEventContractUsed(),
        event.getContractPriorities());
  }

  private CardData decodeCardData(CalypsoCard calypsoCard) {
    // Parse environment
    EnvironmentHolderStructure environment =
        CalypsoEnvironmentHolderCodec.INSTANCE.decode(
            calypsoCard
                .getFileBySfi(CalypsoFiles.SFI_ENVIRONMENT_AND_HOLDER)
                .getData()
                .getContent());
    // parse contracts
    List<ContractStructure> contracts = new ArrayList<>();
    FileData fileData = calypsoCard.getFileBySfi(CalypsoFiles.SFI_CONTRACTS).getData();
    if (fileData != null) {
      int contractRecordCount = getContractRecordCount(calypsoCard);
      for (int i = 1; i < contractRecordCount + 1; i++) {
        // contract, with the counter tied to it
        int counterValue =
            calypsoCard
                .getFileBySfi(CalypsoFiles.SFI_COUNTERS)
                .getData()
                .getContentAsCounterValue(i);
        contracts.add(
            CalypsoContractCodec.INSTANCE
                .decode(fileData.getContent(i))
                .withCounterValue(counterValue));
      }
    }
    // parse event
    EventStructure event =
        CalypsoEventCodec.INSTANCE.decode(
            calypsoCard.getFileBySfi(CalypsoFiles.SFI_EVENT_LOG).getData().getContent());
    return new CardData(environment, contracts, event);
  }

  /** Returns the number of contract records of the card, depending on its product type. */
  private int getContractRecordCount(CalypsoCard calypsoCard) {
    if (calypsoCard.getProductType() == CalypsoCard.ProductType.BASIC) {
      return 1;
    } else if (calypsoCard.getProductType() == CalypsoCard.ProductType.LIGHT) {
      return 2;
    }
    return 4;
  }

  private CardData decodeCardData(StorageCard storageCard) {
    EnvironmentHolderStructure environment;
    List<ContractStructure> contracts = new ArrayList<>();
    EventStructure event;

    // MIFARE Classic 1K uses a different layout: 1 block of 16 bytes per structure
    // (blocks 4, 5, 6) instead of 4 blocks of 4 bytes (blocks 4-7, 8-11, 12-15)
    if (storageCard.getProductType() == ProductType.MIFARE_CLASSIC_1K) {
      logger.debug(
          "Parsing the MIFARE Classic 1K data, single-block layout [environmentBlock=4, contractBlock=5, eventBlock=6]");

      // Parse environment from block 4 (16 bytes)
      environment = StorageCardEnvironmentHolderCodec.INSTANCE.decode(storageCard.getBlock(4));

      // Parse contract from block 5 (16 bytes)
      contracts.add(StorageCardContractCodec.INSTANCE.decode(storageCard.getBlock(5)));

      // Parse event from block 6 (16 bytes)
      event = StorageCardEventCodec.INSTANCE.decode(storageCard.getBlock(6));

    } else {
      // MIFARE Ultralight and ST25 SRT512 use multi-block layout
      logger.debug(
          "Parsing the storage card data [productType={}, environmentFirstBlock={}, environmentLastBlock={}]",
          storageCard.getProductType(),
          StorageCardBlocks.ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
          StorageCardBlocks.ENVIRONMENT_AND_HOLDER_LAST_BLOCK);

      // Parse environment from blocks 4-7 (4 blocks × 4 bytes = 16 bytes)
      environment =
          StorageCardEnvironmentHolderCodec.INSTANCE.decode(
              storageCard.getBlocks(
                  StorageCardBlocks.ENVIRONMENT_AND_HOLDER_FIRST_BLOCK,
                  StorageCardBlocks.ENVIRONMENT_AND_HOLDER_LAST_BLOCK));

      logger.debug(
          "Parsing the contract [firstBlock={}, lastBlock={}]",
          StorageCardBlocks.CONTRACT_FIRST_BLOCK,
          StorageCardBlocks.COUNTER_LAST_BLOCK);

      // Parse contract from blocks 8-11 (4 blocks × 4 bytes = 16 bytes)
      contracts.add(
          StorageCardContractCodec.INSTANCE.decode(
              storageCard.getBlocks(
                  StorageCardBlocks.CONTRACT_FIRST_BLOCK, StorageCardBlocks.COUNTER_LAST_BLOCK)));

      // Parse event from blocks 12-15 (4 blocks × 4 bytes = 16 bytes)
      event =
          StorageCardEventCodec.INSTANCE.decode(
              storageCard.getBlocks(
                  StorageCardBlocks.EVENT_FIRST_BLOCK, StorageCardBlocks.EVENT_LAST_BLOCK));
    }

    return new CardData(environment, contracts, event);
  }
}
