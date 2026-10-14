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

using System.Runtime.Versioning;
using System.ServiceProcess;
using App.domain.spi;
using App.domain.utils;
using PCSC;
using PCSC.Exceptions;
using Serilog;
using Serilog.Events;

namespace App.infrastructure.pcscreader
{
    /// <summary>
    /// Adapter class that implements the ReaderSpi interface for PC/SC smart card readers.
    /// </summary>
    internal class PcscReaderSpiAdapter : ReaderSpi
    {
        private readonly ILogger _logger;
        private readonly IContextFactory _contextFactory;
        private ISCardContext _context;
        private string? _readerName;
        private byte[]? _cardAtr;
        private ICardReader? _reader;
        private const string SmartCardServiceName = "SCardSvr";
        private const int ReceiveBufferSize = 256;

        /// <summary>
        /// Creates a new instance of the <see cref="PcscReaderSpiAdapter"/> class.
        /// </summary>
        public PcscReaderSpiAdapter()
        {
            _logger = Log.ForContext<PcscReaderSpiAdapter>();
            _contextFactory = ContextFactory.Instance;
            _context = _contextFactory.Establish(SCardScope.System);
        }

        /// <inheritdoc/>
        [SupportedOSPlatform("windows")]
        public List<string> GetReaders()
        {
            List<string> readerNames = new List<string>();

            // Check if the service is running, if not, start it.
            ServiceController sc = new ServiceController(SmartCardServiceName);

            if (sc.Status == ServiceControllerStatus.Stopped)
            {
                try
                {
                    // Start the service if the current status is stopped.
                    sc.Start();
                }
                catch (InvalidOperationException ex)
                {
                    _logger.Error(ex, "Unable to start the smart card service (SCardSvr)");
                    return readerNames;
                }
                sc.WaitForStatus(desiredStatus: ServiceControllerStatus.Running);
                // Establish a new context.
                _context = _contextFactory.Establish(SCardScope.System);
            }

            // Get the list of readers.
            readerNames.AddRange(_context.GetReaders());
            _logger.Information($"Readers found [count={readerNames.Count}]");

            return readerNames;
        }

        /// <inheritdoc/>
        public void SelectReader(string readerName)
        {
            _readerName = readerName;
        }

        /// <inheritdoc/>
        public bool WaitForCard()
        {
            try
            {
                SCardReaderState[] readerStates = new[]
                {
                    new SCardReaderState { ReaderName = _readerName, CurrentState = SCRState.Unknown }
                };

                while (true)
                {
                    SCardError sc = _context.GetStatusChange(timeout: 1000, readerStates);
                    if (sc == SCardError.Success)
                    {
                        if ((readerStates[0].EventState & SCRState.Present) == SCRState.Present)
                        {
                            return true;
                        }
                    }
                    else if (sc == SCardError.Timeout)
                    {
                        // Timeout occurred, you can break the loop if you want to stop waiting
                        // or continue to wait for the card
                    }
                    else
                    {
                        // Error occurred, handle it accordingly
                        break;
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.Error(ex, "Error while waiting for a card");
            }
            return false;
        }

        /// <inheritdoc/>
        public void OpenPhysicalChannel()
        {
            try
            {
                _cardAtr = null;
                _reader = _context.ConnectReader(_readerName, SCardShareMode.Shared, SCardProtocol.T0 | SCardProtocol.T1);
                _logger.Debug($"Physical channel open [reader={_readerName}]");

                _cardAtr = _reader.GetAttrib(SCardAttribute.AtrString);
                if (_logger.IsEnabled(LogEventLevel.Debug))
                {
                    _logger.Debug($"Card ATR read [atr={GetPowerOnData()}]");
                }
            }
            catch (PCSCException ex) when (ex.SCardError == SCardError.CommunicationError)
            {
                throw new CardIOException($"Reader disconnected or card removed [reader={_readerName}]", ex);
            }
            catch (Exception ex)
            {
                throw new CardIOException($"Error while connecting to the reader [reader={_readerName}]", ex);
            }
        }

        /// <inheritdoc/>
        public void ClosePhysicalChannel()
        {
            if (_reader == null)
            {
                throw new InvalidOperationException("Reader not available");
            }
            try
            {
                // Disconnect the reader 
                _reader.Disconnect(SCardReaderDisposition.Leave);
            }
            catch (PCSCException ex) when (ex.SCardError == SCardError.CommunicationError)
            {
                throw new CardIOException($"Reader disconnected or card removed [reader={_readerName}]", ex);
            }
            catch (Exception ex)
            {
                throw new CardIOException($"Error while disconnecting the reader [reader={_readerName}]", ex);
            }
        }

        /// <inheritdoc/>
        public string GetPowerOnData()
        {
            if (_cardAtr == null)
            {
                throw new InvalidOperationException("No ATR available");
            }
            return HexUtil.ToHex(_cardAtr);
        }

        /// <inheritdoc/>
        public byte[] TransmitApdu(byte[] commandApdu)
        {
            if (_reader == null)
            {
                throw new InvalidOperationException("Reader not available");
            }
            try
            {
                if (_logger.IsEnabled(LogEventLevel.Debug))
                {
                    _logger.Debug($"APDU sent [command={HexUtil.ToHex(commandApdu)}]");
                }
                byte[] receiveBuffer = new byte[ReceiveBufferSize];
                int responseLength = _reader.Transmit(commandApdu, receiveBuffer);
                byte[] responseApdu = new byte[responseLength];
                Array.Copy(receiveBuffer, 0, responseApdu, 0, responseLength);
                if (_logger.IsEnabled(LogEventLevel.Debug))
                {
                    _logger.Debug($"APDU received [response={HexUtil.ToHex(responseApdu)}]");
                }
                return responseApdu;
            }
            catch (PCSCException ex) when (ex.SCardError == SCardError.CommunicationError)
            {
                throw new CardIOException("Error while communicating with the card", ex);
            }
            catch (Exception ex)
            {
                throw new ReaderIOException("Error while communicating with the reader", ex);
            }
        }
    }
}
