// Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
//
// See the NOTICE file(s) distributed with this work for additional information
// regarding copyright ownership.
//
// This program and the accompanying materials are made available under the
// terms of the BSD 3-Clause License which is available at
// https://opensource.org/licenses/BSD-3-Clause.
//
// SPDX-License-Identifier: BSD-3-Clause

using App.domain.data;
using App.domain.data.command;
using App.domain.data.executeremoteservice;
using App.domain.data.response;
using App.domain.spi;
using Newtonsoft.Json;
using Serilog;
using Serilog.Events;

namespace App.domain.api
{


    class TicketingServiceApiAdapter : TicketingServiceApi
    {
        private readonly ILogger _logger;
        private readonly ReaderSpi _reader;
        private readonly ServerSpi _server;
        private readonly string _clientNodeId;
        private string _localReaderName;

        private const int StatusWord6100 = 0x6100;
        private const int StatusWord6C00 = 0x6C00;
        private const int Sw1Mask = 0xFF00;
        private const int Sw2Mask = 0x00FF;

        private const string ExecuteRemoteServiceAction = "EXECUTE_REMOTE_SERVICE";
        private const string EndRemoteServiceAction = "END_REMOTE_SERVICE";
        private const string ResponseAction = "RESP";

        private const string IsContactlessService = "IS_CONTACTLESS";
        private const string IsCardPresentService = "IS_CARD_PRESENT";
        private const string TransmitCardSelectionRequestsService = "TRANSMIT_CARD_SELECTION_REQUESTS";
        private const string TransmitCardRequestService = "TRANSMIT_CARD_REQUEST";

        private const string SelectAppAndReadContractsService = "SELECT_APP_AND_READ_CONTRACTS";
        private const string SelectAppAndLoadTripsService = "SELECT_APP_AND_LOAD_TRIPS";

        internal TicketingServiceApiAdapter(ReaderSpi readerSpi, string readerName, ServerSpi serverSpi)
        {
            _logger = Log.ForContext<TicketingServiceApiAdapter>();
            _logger.Information("Creating the ticketing service");

            _reader = readerSpi;
            _server = serverSpi;

            _clientNodeId = Guid.NewGuid().ToString();

            List<string> readerNames = _reader.GetReaders();

            if (readerNames.Count == 0)
            {
                throw new ReaderNotFoundException("No reader found");
            }

            if (!readerNames.Contains(readerName))
            {
                throw new ReaderNotFoundException($"Reader not found [reader={readerName}]");
            }

            _localReaderName = readerName;
            _logger.Information($"Reader selected [reader={_localReaderName}]");
            _reader.SelectReader(_localReaderName);
        }

        /// <inheritdoc/>
        public void WaitForCard()
        {
            _reader.WaitForCard();
        }

        /// <inheritdoc/>
        public string SelectAppAndReadContracts()
        {
            _logger.Information("Executing the remote service [service=SELECT_APP_AND_READ_CONTRACTS]");

            return ExecuteRemoteService(SelectAppAndReadContractsService, new ReadContractsInputData { });
        }

        /// <inheritdoc/>
        public string SelectAppAndLoadTrips(int tripsToLoad, string applicationSerialNumber)
        {
            _logger.Information("Executing the remote service [service=SELECT_APP_AND_LOAD_TRIPS]");

            return ExecuteRemoteService(SelectAppAndLoadTripsService, new LoadTripsInputData { ApplicationSerialNumber = applicationSerialNumber, TripsToLoad = tripsToLoad });
        }

        private string ExecuteRemoteService(string serviceId, InputData inputData)
        {
            string sessionId = Guid.NewGuid().ToString();

            // Create and fill the body of the remote service execution
            ExecuteRemoteServiceBody executeRemoteServiceBody = new ExecuteRemoteServiceBody
            {
                ServiceId = serviceId,
                InputData = inputData,
                IsReaderContactless = true
            };

            // Create and fill the message
            MessageDto message = new MessageDto
            {
                ApiLevel = ApiInfo.ApiLevel,
                SessionId = sessionId,
                Action = ExecuteRemoteServiceAction,
                ClientNodeId = _clientNodeId,
                LocalReaderName = _localReaderName,
                Body = JsonConvert.SerializeObject(executeRemoteServiceBody, Formatting.None)
            };

            string jsonResponse = _server.TransmitRequest(JsonConvert.SerializeObject(message));

            message = JsonConvert.DeserializeObject<List<MessageDto>>(jsonResponse)![0];

            message = ProcessServerCommands(message);

            return message.Body;
        }

        private MessageDto ProcessServerCommands(MessageDto message)
        {
            while (message.Action != EndRemoteServiceAction)
            {
                _logger.Debug($"Processing the action [action={message.Action}]");

                CmdBody command = JsonConvert.DeserializeObject<CmdBody>(message.Body)!;

                string service = command.Service;
                _logger.Debug($"Command received [service={service}]");
                string responseBody = "";
                switch (service)
                {
                    case IsContactlessService:
                        responseBody = CreateIsContactlessResponse();
                        break;
                    case IsCardPresentService:
                        responseBody = CreateIsCardPresentResponse();
                        break;
                    case TransmitCardSelectionRequestsService:
                        responseBody = TransmitCardSelectionRequests(message);
                        break;
                    case TransmitCardRequestService:
                        responseBody = TransmitCardRequest(message);
                        break;
                }
                message.ApiLevel = ApiInfo.ApiLevel;
                message.Action = ResponseAction;
                message.Body = responseBody;
                string jsonResponse = _server.TransmitRequest(JsonConvert.SerializeObject(message, Formatting.None));
                message = JsonConvert.DeserializeObject<List<MessageDto>>(jsonResponse)![0];
            }
            return message;
        }

        private static string CreateIsContactlessResponse()
        {
            IsContactlessRespBody isContactlessRespBody = new IsContactlessRespBody();
            isContactlessRespBody.Result = true;
            return JsonConvert.SerializeObject(isContactlessRespBody, Formatting.None);
        }

        private static string CreateIsCardPresentResponse()
        {
            IsCardPresentRespBody isCardPresentRespBody = new IsCardPresentRespBody();
            isCardPresentRespBody.Result = true;
            return JsonConvert.SerializeObject(isCardPresentRespBody, Formatting.None);
        }

        private string TransmitCardSelectionRequests(MessageDto message)
        {
            TransmitCardSelectionRequestsCmdBody transmitCardSelectionRequestsCmdBody = JsonConvert.DeserializeObject<TransmitCardSelectionRequestsCmdBody>(message.Body)!;
            TransmitCardSelectionRequestsRespBody transmitCardSelectionRequestsRespBody = new TransmitCardSelectionRequestsRespBody();
            List<CardSelectionResponse> cardSelectionResponses = new List<CardSelectionResponse>();
            Error? error = null;
            int cardSelectorCount = transmitCardSelectionRequestsCmdBody.Parameters.CardSelectors.Length;
            for (int i = 0; i < cardSelectorCount; i++)
            {
                try
                {
                    CardSelectionResponse cardSelectionResponse =
                        ProcessCardSelectionRequest(transmitCardSelectionRequestsCmdBody.Parameters.CardSelectors[i],
                        transmitCardSelectionRequestsCmdBody.Parameters.CardSelectionRequests[i],
                        transmitCardSelectionRequestsCmdBody.Parameters.ChannelControl);
                    cardSelectionResponses.Add(cardSelectionResponse);
                    if (cardSelectionResponse.HasMatched)
                    {
                        break;
                    }
                }
                catch (CardIOException ex)
                {
                    _logger.Warning($"Card communication error while processing the card selection request [cause={ex.Message}]");
                    error = new Error { Code = ErrorCode.CARD_COMMUNICATION_ERROR, Message = ex.Message };
                    break;
                }
                catch (ReaderNotFoundException ex)
                {
                    _logger.Warning($"Reader communication error while processing the card selection request [cause={ex.Message}]");
                    error = new Error { Code = ErrorCode.READER_COMMUNICATION_ERROR, Message = ex.Message };
                    break;
                }
                catch (UnexpectedStatusWordException ex)
                {
                    _logger.Warning($"Unexpected status word while processing the card selection request [cause={ex.Message}]");
                    error = new Error { Code = ErrorCode.CARD_COMMAND_ERROR, Message = ex.Message };
                    break;
                }
            }
            if (error == null)
            {
                transmitCardSelectionRequestsRespBody.Result = cardSelectionResponses;
            }
            else
            {
                transmitCardSelectionRequestsRespBody.Error = error;
            }
            return JsonConvert.SerializeObject(transmitCardSelectionRequestsRespBody, Formatting.None);
        }

        private CardSelectionResponse ProcessCardSelectionRequest(CardSelector cardSelector, CardSelectionRequest cardSelectionRequest, ChannelControl channelControl)
        {
            _reader.OpenPhysicalChannel();
            ApduResponse selectAppResponse = SelectApplication(cardSelector);
            CardResponse? cardResponse = null;

            if (cardSelectionRequest.CardRequest != null)
            {
                cardResponse = ProcessCardRequest(cardSelectionRequest.CardRequest, channelControl);
            }

            CardSelectionResponse cardSelectionResponse = new CardSelectionResponse
            {
                HasMatched = cardSelectionRequest.SuccessfulSelectionStatusWords?.Contains(selectAppResponse.StatusWord) ?? false,
                PowerOnData = _reader.GetPowerOnData(),
                SelectApplicationResponse = selectAppResponse,
                CardResponse = cardResponse
            };

            return cardSelectionResponse;
        }

        private ApduResponse SelectApplication(CardSelector cardSelector)
        {
            if (cardSelector != null && cardSelector.Aid != null)
            {
                byte[] selectApplicationCommand = new byte[6 + cardSelector.Aid.Length];
                selectApplicationCommand[0] = 0x00; // CLA
                selectApplicationCommand[1] = 0xA4; // INS
                selectApplicationCommand[2] = 0x04; // P1: select by name
                                                    // P2: b0,b1 define the File occurrence, b2,b3 define the File control information
                                                    // the bitmasks defined in the respective enums are used
                selectApplicationCommand[3] =
                    ComputeSelectApplicationP2(
                        cardSelector.FileOccurrence, cardSelector.FileControlInformation);
                selectApplicationCommand[4] = (byte)(cardSelector.Aid.Length); // Lc
                Array.Copy(cardSelector.Aid, 0, selectApplicationCommand, 5, cardSelector.Aid.Length); // data
                selectApplicationCommand[5 + cardSelector.Aid.Length] = 0x00; // Le

                ApduRequest apduRequest = new ApduRequest
                {
                    Apdu = selectApplicationCommand,
                    SuccessfulStatusWords = new HashSet<int>()
                };

                if (_logger.IsEnabled(LogEventLevel.Debug))
                {
                    apduRequest.Info = "Internal Select Application";
                }

                return ProcessApduRequest(apduRequest);
            }

            // Handle the case where cardSelector or cardSelector.Aid is null.
            throw new ArgumentNullException("cardSelector", "Card selector or its AID missing");
        }

        private byte ComputeSelectApplicationP2(FileOccurrence fileOccurrence, FileControlInformation fileControlInformation)
        {
            byte p2;

            switch (fileOccurrence)
            {
                case FileOccurrence.FIRST:
                    p2 = 0x00;
                    break;
                case FileOccurrence.LAST:
                    p2 = 0x01;
                    break;
                case FileOccurrence.NEXT:
                    p2 = 0x02;
                    break;
                case FileOccurrence.PREVIOUS:
                    p2 = 0x03;
                    break;
                default:
                    throw new InvalidOperationException($"Unexpected file occurrence [fileOccurrence={fileOccurrence}]");
            }

            switch (fileControlInformation)
            {
                case FileControlInformation.FCI:
                    p2 |= 0x00;
                    break;
                case FileControlInformation.FCP:
                    p2 |= 0x04;
                    break;
                case FileControlInformation.FMD:
                    p2 |= 0x08;
                    break;
                case FileControlInformation.NO_RESPONSE:
                    p2 |= 0x0C;
                    break;
                default:
                    throw new InvalidOperationException($"Unexpected file control information [fileControlInformation={fileControlInformation}]");
            }

            return p2;
        }

        private string TransmitCardRequest(MessageDto message)
        {
            TransmitCardRequestRespBody transmitCardRequestRespBody = new TransmitCardRequestRespBody();
            TransmitCardRequestCmdBody transmitCardRequestCmdBody = JsonConvert.DeserializeObject<TransmitCardRequestCmdBody>(message.Body)!;
            CardRequest cardRequest = transmitCardRequestCmdBody.Parameters.CardRequest;
            CardResponse? cardResponse = null;
            Error? error = null;
            try
            {
                cardResponse = ProcessCardRequest(cardRequest, transmitCardRequestCmdBody.Parameters.ChannelControl);
            }
            catch (CardIOException ex)
            {
                _logger.Warning($"Card communication error while processing the card request [cause={ex.Message}]");
                error = new Error { Code = ErrorCode.CARD_COMMUNICATION_ERROR, Message = ex.Message };
            }
            catch (ReaderNotFoundException ex)
            {
                _logger.Warning($"Reader communication error while processing the card request [cause={ex.Message}]");
                error = new Error { Code = ErrorCode.READER_COMMUNICATION_ERROR, Message = ex.Message };
            }
            catch (UnexpectedStatusWordException ex)
            {
                _logger.Warning($"Unexpected status word while processing the card request [cause={ex.Message}]");
                error = new Error { Code = ErrorCode.CARD_COMMAND_ERROR, Message = ex.Message };
            }
            if (error == null)
            {
                transmitCardRequestRespBody.Result = cardResponse;
            }
            else
            {
                transmitCardRequestRespBody.Error = error;
            }
            return JsonConvert.SerializeObject(transmitCardRequestRespBody, Formatting.None);
        }

        private CardResponse ProcessCardRequest(CardRequest cardRequest, ChannelControl channelControl)
        {
            bool isLogicalChannelOpen = true;
            List<ApduResponse> apduResponses = new List<ApduResponse>();

            foreach (ApduRequest apduRequest in cardRequest.ApduRequests)
            {
                try
                {
                    ApduResponse apduResponse = ProcessApduRequest(apduRequest);
                    apduResponses.Add(apduResponse);

                    if (!apduRequest.SuccessfulStatusWords.Contains(apduResponse.StatusWord))
                    {
                        throw new UnexpectedStatusWordException($"Unexpected status word [statusWord={apduResponse.StatusWord:X}]");
                    }
                }
                catch (ReaderIOException)
                {
                    _reader.ClosePhysicalChannel();
                    throw;
                }
                catch (CardIOException)
                {
                    _reader.ClosePhysicalChannel();
                    throw;
                }
            }

            if (channelControl == ChannelControl.CLOSE_AFTER)
            {
                _reader.ClosePhysicalChannel();
                isLogicalChannelOpen = false;
            }

            return new CardResponse { IsLogicalChannelOpen = isLogicalChannelOpen, ApduResponses = apduResponses };
        }

        private ApduResponse ProcessApduRequest(ApduRequest apduRequest)
        {
            byte[] apdu = _reader.TransmitApdu(apduRequest.Apdu);
            ApduResponse apduResponse = new ApduResponse
            {
                Apdu = apdu,
                StatusWord = (apdu[apdu.Length - 2] << 8) | apdu[apdu.Length - 1]
            };

            if (apduResponse.Apdu.Length == 2)
            {
                if ((apduResponse.StatusWord & Sw1Mask) == StatusWord6100)
                {
                    byte[] getResponseApdu = {
                        0x00,
                        0xC0,
                        0x00,
                        0x00,
                        (byte)(apduResponse.StatusWord & Sw2Mask)
                        };
                    apduResponse = ProcessApduRequest(new ApduRequest { Apdu = getResponseApdu, SuccessfulStatusWords = new HashSet<int>(), Info = "Internal Get Response" });
                }
                else if ((apduResponse.StatusWord & Sw1Mask) == StatusWord6C00)
                {
                    apduRequest.Apdu[apduRequest.Apdu.Length - 1] =
                        (byte)(apduResponse.StatusWord & Sw2Mask);
                    apduResponse = ProcessApduRequest(apduRequest);
                }
            }

            return apduResponse;
        }
    }
}
