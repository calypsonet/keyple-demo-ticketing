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

import static org.calypsonet.keyple.demo.reload.remote.server.card.TransactionRecorder.LOAD;
import static org.calypsonet.keyple.demo.reload.remote.server.card.TransactionRecorder.SECURED_READ;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.calypsonet.keyple.demo.common.dto.RemoteServiceStatus;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndIncreaseContractCounterInputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndIncreaseContractCounterOutputDto;
import org.calypsonet.keyple.demo.common.dto.SelectAppAndReadContractsOutputDto;
import org.calypsonet.keyple.demo.common.model.ContractStructure;
import org.calypsonet.keyple.demo.common.model.type.PriorityCode;
import org.eclipse.keyple.core.service.resource.CardResource;
import org.eclipse.keyple.core.service.resource.CardResourceServiceProvider;
import org.eclipse.keyple.core.util.HexUtil;
import org.eclipse.keypop.calypso.card.card.CalypsoCard;
import org.eclipse.keypop.reader.CardCommunicationException;
import org.eclipse.keypop.reader.CardReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Services of the .NET client, the application of the card being selected by the server: reading of
 * the contracts as text (remote service {@code SELECT_APP_AND_READ_CONTRACTS}) and loading of trips
 * on the multi-trip ticket (remote service {@code SELECT_APP_AND_INCREASE_CONTRACT_COUNTER}).
 */
@ApplicationScoped
public class ContractCounterService {

  private static final Logger logger = LoggerFactory.getLogger(ContractCounterService.class);
  private static final String CUSTOM_PLUGIN = "Non Keyple plugin";
  private static final String CARD_NOT_PERSONALIZED = "Card not personalized.";
  private static final String ENVIRONMENT_EXPIRED = "Environment expired.";
  private static final String RUNTIME_EXCEPTION = "Runtime exception: ";
  private static final String CARD_COMMUNICATION_ERROR = "Card communication error: ";

  @Inject CardRepository cardRepository;
  @Inject TransactionRecorder transactionRecorder;
  @Inject ContractAnalysisService contractAnalysisService;
  @Inject ContractLoadingService contractLoadingService;

  SelectAppAndReadContractsOutputDto selectAppAndReadContracts(CardReader cardReader) {

    CalypsoCard calypsoCard = null;
    List<String> output = new ArrayList<>();
    int statusCode = RemoteServiceStatus.SUCCESS.getCode();
    String message = "Success.";
    CardResource samResource = null;

    try {
      calypsoCard = cardRepository.selectCard(cardReader);
      logger.info(
          "Reading the contracts [plugin={}, serialNumber={}]",
          CUSTOM_PLUGIN,
          HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));

      samResource =
          CardResourceServiceProvider.getService()
              .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);

      CardData card = cardRepository.readCard(cardReader, calypsoCard, samResource);
      List<ContractStructure> presentContracts = contractAnalysisService.findPresentContracts(card);

      for (ContractStructure contractStructure : presentContracts) {
        output.add(formatContractStructure(contractStructure));
      }
    } catch (CardNotPersonalizedException e) {
      statusCode = RemoteServiceStatus.CARD_NOT_PERSONALIZED.getCode();
      message = CARD_NOT_PERSONALIZED;
    } catch (EnvironmentExpiredException e) {
      statusCode = RemoteServiceStatus.ENVIRONMENT_EXPIRED.getCode();
      message = ENVIRONMENT_EXPIRED;
    } catch (CardCommunicationException e) {
      statusCode = RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode();
      logger.warn(
          "Card communication error while reading the contracts [cause={}]", e.getMessage());
      message = CARD_COMMUNICATION_ERROR + e.getMessage();
    } catch (RuntimeException e) {
      statusCode = RemoteServiceStatus.SERVER_ERROR.getCode();
      logger.error("Unexpected error while reading the contracts", e);
      message = RUNTIME_EXCEPTION + e.getMessage();
    } finally {
      transactionRecorder.record(
          CUSTOM_PLUGIN,
          statusCode == RemoteServiceStatus.SUCCESS.getCode(),
          SECURED_READ,
          calypsoCard == null ? "-" : HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));
      if (samResource != null) {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      }
    }
    return new SelectAppAndReadContractsOutputDto(output, statusCode, message);
  }

  SelectAppAndIncreaseContractCounterOutputDto selectAppAndIncreaseContractCounter(
      CardReader cardReader, SelectAppAndIncreaseContractCounterInputDto inputData) {

    CalypsoCard calypsoCard = null;
    CardResource samResource = null;
    int statusCode = RemoteServiceStatus.SUCCESS.getCode();
    String message = "Success.";
    try {
      calypsoCard = cardRepository.selectCard(cardReader);
      logger.info(
          "Increasing the contract counter [plugin={}, serialNumber={}, counterIncrement={}]",
          CUSTOM_PLUGIN,
          HexUtil.toHex(calypsoCard.getApplicationSerialNumber()),
          inputData.getCounterIncrement());

      samResource =
          CardResourceServiceProvider.getService()
              .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);

      CardData card = cardRepository.readCard(cardReader, calypsoCard, samResource);
      contractLoadingService.insertNewContract(
          PriorityCode.MULTI_TRIP, inputData.getCounterIncrement(), card);
      statusCode = cardRepository.writeCard(cardReader, calypsoCard, samResource, card);
    } catch (CardFullException e) {
      statusCode = RemoteServiceStatus.CARD_FULL.getCode();
      message = ContractLoadingService.CARD_FULL_MESSAGE;
    } catch (CardNotPersonalizedException e) {
      statusCode = RemoteServiceStatus.CARD_NOT_PERSONALIZED.getCode();
      message = CARD_NOT_PERSONALIZED;
    } catch (EnvironmentExpiredException e) {
      statusCode = RemoteServiceStatus.ENVIRONMENT_EXPIRED.getCode();
      message = ENVIRONMENT_EXPIRED;
    } catch (CardCommunicationException e) {
      statusCode = RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode();
      message = CARD_COMMUNICATION_ERROR + e.getMessage();
      logger.warn(
          "Card communication error while increasing the contract counter [cause={}]",
          e.getMessage());
    } catch (RuntimeException e) {
      statusCode = RemoteServiceStatus.SERVER_ERROR.getCode();
      message = RUNTIME_EXCEPTION + e.getMessage();
      logger.error("Unexpected error while increasing the contract counter", e);
    } finally {
      boolean success = statusCode == RemoteServiceStatus.SUCCESS.getCode();
      transactionRecorder.record(
          CUSTOM_PLUGIN,
          success,
          LOAD,
          calypsoCard == null ? "-" : HexUtil.toHex(calypsoCard.getApplicationSerialNumber()),
          success
              ? PriorityCode.MULTI_TRIP.getLabel() + ": " + inputData.getCounterIncrement()
              : null);
      if (samResource != null) {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      }
    }
    return new SelectAppAndIncreaseContractCounterOutputDto(statusCode, message);
  }

  /** Returns the description of a contract, one field per line, sent as text to the client. */
  private static String formatContractStructure(ContractStructure contractStructure) {
    StringBuilder builder = new StringBuilder();

    builder
        .append("Contract Version Number: ")
        .append(contractStructure.getContractVersionNumber())
        .append("\n");

    builder.append("Contract Tariff: ").append(contractStructure.getContractTariff()).append("\n");

    builder
        .append("Contract Sale Date: ")
        .append(contractStructure.getContractSaleDate().getDate())
        .append("\n");

    builder
        .append("Contract Validity End Date: ")
        .append(contractStructure.getContractValidityEndDate().getDate())
        .append("\n");

    if (contractStructure.getContractSaleSam() != null) {
      builder
          .append("Contract Sale Sam: ")
          .append(contractStructure.getContractSaleSam())
          .append("\n");
    }

    if (contractStructure.getContractSaleCounter() != null) {
      builder
          .append("Contract Sale Counter: ")
          .append(contractStructure.getContractSaleCounter())
          .append("\n");
    }

    if (contractStructure.getContractAuthKvc() != null) {
      builder
          .append("Contract Auth Kvc: ")
          .append(contractStructure.getContractAuthKvc())
          .append("\n");
    }

    if (contractStructure.getContractAuthenticator() != null) {
      builder
          .append("Contract Authenticator: ")
          .append(contractStructure.getContractAuthenticator())
          .append("\n");
    }

    if (contractStructure.getCounterValue() != null) {
      builder.append("Counter Value: ").append(contractStructure.getCounterValue()).append("\n");
    }

    return builder.toString();
  }
}
