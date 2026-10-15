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
  private static final String NON_KEYPLE_PLUGIN = "Non Keyple plugin";

  @Inject CardRepository cardRepository;
  @Inject TransactionRecorder transactionRecorder;
  @Inject ContractAnalysisService contractAnalysisService;
  @Inject ContractLoadingService contractLoadingService;

  SelectAppAndReadContractsOutputDto selectAppAndReadContracts(CardReader cardReader) {

    CalypsoCard calypsoCard = null;
    List<String> output = new ArrayList<>();
    int statusCode = RemoteServiceStatus.SUCCESS.getCode();
    CardResource samResource = null;

    try {
      calypsoCard = cardRepository.selectCard(cardReader);
      logger.info(
          "Reading the contracts [plugin={}, serialNumber={}]",
          NON_KEYPLE_PLUGIN,
          HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));

      samResource =
          CardResourceServiceProvider.getService()
              .getCardResource(CardConfigurator.SAM_RESOURCE_PROFILE_NAME);

      CardData card = cardRepository.readCard(cardReader, calypsoCard, samResource);
      List<ContractStructure> presentContracts = contractAnalysisService.findPresentContracts(card);

      for (ContractStructure contract : presentContracts) {
        output.add(describeContract(contract));
      }
    } catch (CardNotPersonalizedException e) {
      // Refusal logged where it is detected
      statusCode = RemoteServiceStatus.CARD_NOT_PERSONALIZED.getCode();
    } catch (EnvironmentExpiredException e) {
      // Refusal logged where it is detected
      statusCode = RemoteServiceStatus.ENVIRONMENT_EXPIRED.getCode();
    } catch (CardCommunicationException e) {
      statusCode = RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode();
      logger.warn(
          "Card communication error while reading the contracts [cause={}]", e.getMessage());
    } catch (RuntimeException e) {
      statusCode = RemoteServiceStatus.SERVER_ERROR.getCode();
      logger.error("Unexpected error while reading the contracts", e);
    } finally {
      transactionRecorder.record(
          NON_KEYPLE_PLUGIN,
          statusCode == RemoteServiceStatus.SUCCESS.getCode(),
          SECURED_READ,
          calypsoCard == null ? "-" : HexUtil.toHex(calypsoCard.getApplicationSerialNumber()));
      if (samResource != null) {
        CardResourceServiceProvider.getService().releaseCardResource(samResource);
      }
    }
    return new SelectAppAndReadContractsOutputDto(
        output,
        statusCode,
        RemoteServiceMessages.of(RemoteServiceStatus.fromCode(statusCode), calypsoCard));
  }

  SelectAppAndIncreaseContractCounterOutputDto selectAppAndIncreaseContractCounter(
      CardReader cardReader, SelectAppAndIncreaseContractCounterInputDto inputData) {

    CalypsoCard calypsoCard = null;
    CardResource samResource = null;
    int statusCode = RemoteServiceStatus.SUCCESS.getCode();
    try {
      calypsoCard = cardRepository.selectCard(cardReader);
      logger.info(
          "Increasing the contract counter [plugin={}, serialNumber={}, counterIncrement={}]",
          NON_KEYPLE_PLUGIN,
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
      // Refusal logged where it is detected
      statusCode = RemoteServiceStatus.CARD_FULL.getCode();
    } catch (CardNotPersonalizedException e) {
      // Refusal logged where it is detected
      statusCode = RemoteServiceStatus.CARD_NOT_PERSONALIZED.getCode();
    } catch (EnvironmentExpiredException e) {
      // Refusal logged where it is detected
      statusCode = RemoteServiceStatus.ENVIRONMENT_EXPIRED.getCode();
    } catch (CardCommunicationException e) {
      statusCode = RemoteServiceStatus.CARD_COMMUNICATION_ERROR.getCode();
      logger.warn(
          "Card communication error while increasing the contract counter [cause={}]",
          e.getMessage());
    } catch (RuntimeException e) {
      statusCode = RemoteServiceStatus.SERVER_ERROR.getCode();
      logger.error("Unexpected error while increasing the contract counter", e);
    } finally {
      boolean success = statusCode == RemoteServiceStatus.SUCCESS.getCode();
      transactionRecorder.record(
          NON_KEYPLE_PLUGIN,
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
    return new SelectAppAndIncreaseContractCounterOutputDto(
        statusCode,
        RemoteServiceMessages.of(RemoteServiceStatus.fromCode(statusCode), calypsoCard));
  }

  /** Returns the description of a contract, one field per line, sent as text to the client. */
  private static String describeContract(ContractStructure contract) {
    StringBuilder builder = new StringBuilder();

    builder
        .append("Contract Version Number: ")
        .append(contract.getContractVersionNumber())
        .append("\n");

    builder.append("Contract Tariff: ").append(contract.getContractTariff()).append("\n");

    builder
        .append("Contract Sale Date: ")
        .append(contract.getContractSaleDate().getDate())
        .append("\n");

    builder
        .append("Contract Validity End Date: ")
        .append(contract.getContractValidityEndDate().getDate())
        .append("\n");

    if (contract.getContractSaleSam() != null) {
      builder.append("Contract Sale Sam: ").append(contract.getContractSaleSam()).append("\n");
    }

    if (contract.getContractSaleCounter() != null) {
      builder
          .append("Contract Sale Counter: ")
          .append(contract.getContractSaleCounter())
          .append("\n");
    }

    if (contract.getContractAuthKvc() != null) {
      builder.append("Contract Auth Kvc: ").append(contract.getContractAuthKvc()).append("\n");
    }

    if (contract.getContractAuthenticator() != null) {
      builder
          .append("Contract Authenticator: ")
          .append(contract.getContractAuthenticator())
          .append("\n");
    }

    if (contract.getCounterValue() != null) {
      builder.append("Counter Value: ").append(contract.getCounterValue()).append("\n");
    }

    return builder.toString();
  }
}
