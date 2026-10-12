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

import static org.calypsonet.keyple.demo.reload.remote.server.card.ActivityRecorder.ISSUANCE;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.calypsonet.keyple.demo.common.constants.CalypsoFiles;
import org.calypsonet.keyple.demo.common.dto.CardIssuanceInputDto;
import org.calypsonet.keyple.demo.common.dto.CardIssuanceOutputDto;
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
  private static final String AN_ERROR_OCCURRED_WHILE_INITIALIZING_THE_CARD =
      "An error occurred while initializing the card: {}";

  @Inject CardRepository cardRepository;
  @Inject ActivityRecorder activityRecorder;

  CardIssuanceOutputDto initCard(
      CardReader cardReader, SmartCard smartCard, CardIssuanceInputDto inputData) {
    if (smartCard instanceof CalypsoCard) {
      return initCalypsoCard(cardReader, (CalypsoCard) smartCard, inputData);
    } else {
      return initStorageCard(cardReader, (StorageCard) smartCard, inputData);
    }
  }

  private CardIssuanceOutputDto initCalypsoCard(
      CardReader cardReader, CalypsoCard calypsoCard, CardIssuanceInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String appSerialNumber = HexUtil.toHex(calypsoCard.getApplicationSerialNumber());

    logger.info(
        "=== INIT CALYPSO CARD === Plugin: {}, CSN: {}, ProductType: {}",
        pluginType,
        appSerialNumber,
        calypsoCard.getProductType());

    if (!CalypsoFiles.ALLOWED_FILE_STRUCTURES.contains(calypsoCard.getApplicationSubtype())) {
      return new CardIssuanceOutputDto(RemoteServiceStatus.CARD_REJECTED.getCode());
    }

    CardResource samResource =
        CardResourceServiceProvider.getService()
            .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);
    try {
      cardRepository.initCard(cardReader, calypsoCard, samResource);
      activityRecorder.record(pluginType, true, ISSUANCE, appSerialNumber);
      return new CardIssuanceOutputDto(RemoteServiceStatus.SUCCESS.getCode());
    } catch (CardCommunicationException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_INITIALIZING_THE_CARD, e.getMessage(), e);
      activityRecorder.record(pluginType, false, ISSUANCE, appSerialNumber);
      return new CardIssuanceOutputDto(RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_INITIALIZING_THE_CARD, e.getMessage(), e);
      activityRecorder.record(pluginType, false, ISSUANCE, appSerialNumber);
      return new CardIssuanceOutputDto(RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      CardResourceServiceProvider.getService().releaseCardResource(samResource);
    }
  }

  private CardIssuanceOutputDto initStorageCard(
      CardReader cardReader, StorageCard storageCard, CardIssuanceInputDto inputData) {

    String pluginType = inputData.getPluginType();
    String cardUID = HexUtil.toHex(storageCard.getUID());

    logger.info(
        "=== INIT STORAGE CARD === Plugin: {}, UID: {}, ProductType: {}",
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
      cardRepository.initCard(cardReader, storageCard, samResource);
      activityRecorder.record(pluginType, true, ISSUANCE, cardUID);
      return new CardIssuanceOutputDto(RemoteServiceStatus.SUCCESS.getCode());
    } catch (CardCommunicationException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_INITIALIZING_THE_CARD, e.getMessage(), e);
      activityRecorder.record(pluginType, false, ISSUANCE, cardUID);
      return new CardIssuanceOutputDto(RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode());
    } catch (RuntimeException e) {
      logger.error(AN_ERROR_OCCURRED_WHILE_INITIALIZING_THE_CARD, e.getMessage(), e);
      activityRecorder.record(pluginType, false, ISSUANCE, cardUID);
      return new CardIssuanceOutputDto(RemoteServiceStatus.SERVER_ERROR.getCode());
    } finally {
      if (samResource != null) {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      }
    }
  }
}
