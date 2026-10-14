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

import static org.calypsonet.keyple.demo.reload.remote.server.card.TransactionRecorder.PERSONALIZATION;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles;
import org.calypsonet.keyple.demo.common.dto.PersonalizeCardInputDto;
import org.calypsonet.keyple.demo.common.dto.PersonalizeCardOutputDto;
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus;
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
 * Personalization of a card (remote service {@code PERSONALIZE_CARD}): writing of a new
 * environment, and clearing of the event, of the contracts and of the counters.
 */
@ApplicationScoped
public class CardPersonalizationService {

  private static final Logger logger = LoggerFactory.getLogger(CardPersonalizationService.class);
  private static final String CARD_COMMUNICATION_ERROR_WHILE_PERSONALIZING =
      "Card communication error while personalizing the card [cause={}]";
  private static final String UNEXPECTED_ERROR_WHILE_PERSONALIZING =
      "Unexpected error while personalizing the card";

  @Inject CardRepository cardRepository;
  @Inject TransactionRecorder transactionRecorder;

  PersonalizeCardOutputDto personalizeCard(
      CardReader cardReader, SmartCard smartCard, PersonalizeCardInputDto inputData) {
    if (smartCard instanceof CalypsoCard) {
      return personalizeCalypsoCard(cardReader, (CalypsoCard) smartCard, inputData);
    } else {
      return personalizeStorageCard(cardReader, (StorageCard) smartCard, inputData);
    }
  }

  private PersonalizeCardOutputDto personalizeCalypsoCard(
      CardReader cardReader, CalypsoCard calypsoCard, PersonalizeCardInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String applicationSerialNumber = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());

    logger.info(
        "Personalizing a Calypso card [plugin={}, serialNumber={}, productType={}]",
        pluginType,
        applicationSerialNumber,
        calypsoCard.getProductType());

    if (!CalypsoFiles.ALLOWED_FILE_STRUCTURES.contains(calypsoCard.getApplicationSubtype())) {
      logger.warn(
          "Card refused: file structure not supported [applicationSubtype={}]",
          HexUtil.toHex(calypsoCard.getApplicationSubtype()));
      return new PersonalizeCardOutputDto(RemoteServiceStatus.CARD_REJECTED.getCode());
    }

    CardResource samResource =
        CardResourceServiceProvider.getService()
            .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    try {
      cardRepository.personalizeCard(cardReader, calypsoCard, samResource);
      transactionRecorder.record(pluginType, true, PERSONALIZATION, applicationSerialNumber);
      return new PersonalizeCardOutputDto(RemoteServiceStatus.SUCCESS.getCode());
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_PERSONALIZING, e.getMessage());
      transactionRecorder.record(pluginType, false, PERSONALIZATION, applicationSerialNumber);
      return new PersonalizeCardOutputDto(RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_PERSONALIZING, e);
      transactionRecorder.record(pluginType, false, PERSONALIZATION, applicationSerialNumber);
      return new PersonalizeCardOutputDto(RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      CardResourceServiceProvider.getService().releaseCardResource(samResource);
    }
  }

  private PersonalizeCardOutputDto personalizeStorageCard(
      CardReader cardReader, StorageCard storageCard, PersonalizeCardInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String cardUID = HexUtil.toHex(storageCard.getUID());

    logger.info(
        "Personalizing a storage card [plugin={}, uid={}, productType={}]",
        pluginType,
        cardUID,
        storageCard.getProductType());

    // SAM is not currently used for Storage Cards but may be needed in the future
    // for crypto operations (contract verification, authentication, etc.)
    // To enable SAM allocation when needed:
    // CardResource samResource = CardResourceServiceProvider.getService()
    //     .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    CardResource samResource = null;
    try {
      cardRepository.personalizeCard(cardReader, storageCard, samResource);
      transactionRecorder.record(pluginType, true, PERSONALIZATION, cardUID);
      return new PersonalizeCardOutputDto(RemoteServiceStatus.SUCCESS.getCode());
    } catch (CardCommunicationException e) {
      logger.warn(CARD_COMMUNICATION_ERROR_WHILE_PERSONALIZING, e.getMessage());
      transactionRecorder.record(pluginType, false, PERSONALIZATION, cardUID);
      return new PersonalizeCardOutputDto(RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(UNEXPECTED_ERROR_WHILE_PERSONALIZING, e);
      transactionRecorder.record(pluginType, false, PERSONALIZATION, cardUID);
      return new PersonalizeCardOutputDto(RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      if (samResource != null) {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      }
    }
  }
}
