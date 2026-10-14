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

using App.domain.api;
using Newtonsoft.Json;
using Serilog;

namespace App.application
{

    /// <summary>
    /// The Application class coordinates the main operations of the application.
    /// </summary>
    public class Application
    {
        private readonly ILogger _logger;

        // API of the ticketing services of the server
        private readonly TicketingServiceApi _ticketingService;

        /// <summary>
        /// Application constructor.
        /// </summary>
        /// <param name="ticketingService">API of the ticketing services</param>
        public Application(TicketingServiceApi ticketingService)
        {
            // Initialize logger for this context
            _logger = Log.ForContext<Application>();

            // Assign the ticketing service instance
            _ticketingService = ticketingService;
        }

        /// <summary>
        /// Start the main execution of the application.
        /// </summary>
        public void Start()
        {
            WaitForCard();

            // Select the card and read its content
            string output = _ticketingService.SelectAppAndReadContracts();

            // Deserialize the output into a dynamic object
            EndRemoteServiceBody outputDto = JsonConvert.DeserializeObject<EndRemoteServiceBody>(output)!;

            if (outputDto.OutputData.StatusCode != 0)
            {
                Display("Reading failed, status code: " + outputDto.OutputData.StatusCode, ConsoleColor.DarkRed);
                Display(outputDto.OutputData.Message, ConsoleColor.DarkRed);
                _logger.Warning($"Contracts reading failed [statusCode={outputDto.OutputData.StatusCode}, message={outputDto.OutputData.Message}]");
                return;
            }

            Display("Reading successful", ConsoleColor.DarkGreen);

            List<string> contracts = outputDto.OutputData.Items;
            _logger.Information($"Contracts read [contracts={contracts.Count}]");
            int i = 1;
            bool multitripContractPresent = false;
            foreach (string contract in contracts)
            {
                // Print each item to the console
                Display($"Contract #{i++}:\r\n" + contract, ConsoleColor.DarkGreen);
                if (contract.Contains("MULTI_TRIP"))
                {
                    multitripContractPresent = true;
                }
            }

            if (!multitripContractPresent)
            {
                Display("No MULTI_TRIP contract found.", ConsoleColor.DarkRed);
                _logger.Warning("No multi-trip contract found");
            }

            int tripsToLoad;
            do
            {
                Console.Write("Please enter a number of trips to load between 1 and 20: ");
                string input = Console.ReadLine()!;

                if (!int.TryParse(input, out tripsToLoad) || tripsToLoad < 1 || tripsToLoad > 20)
                {
                    Console.WriteLine("Error: please enter a valid number of trips between 1 and 20.");
                }
            } while (tripsToLoad < 1 || tripsToLoad > 20);

            Display($"{tripsToLoad} trip(s) will now be loaded in the card.", ConsoleColor.DarkCyan);
            _logger.Information($"Loading trips [tripsToLoad={tripsToLoad}]");

            WaitForCard();

            // Select the application of the card and increase the contract counter by the number of trips to load
            output = _ticketingService.SelectAppAndIncreaseContractCounter(tripsToLoad);

            outputDto = JsonConvert.DeserializeObject<EndRemoteServiceBody>(output)!;

            if (outputDto.OutputData.StatusCode == 0)
            {
                Display("Loading successful", ConsoleColor.DarkGreen);
                _logger.Information($"Trips loaded [tripsToLoad={tripsToLoad}]");
            }
            else
            {
                Display("Loading failed, status code: " + outputDto.OutputData.StatusCode, ConsoleColor.DarkRed);
                Display(outputDto.OutputData.Message, ConsoleColor.DarkRed);
                _logger.Warning($"Trips loading failed [statusCode={outputDto.OutputData.StatusCode}, message={outputDto.OutputData.Message}]");
            }
        }

        /// <summary>
        /// Waits for a card, the user being invited to present it.
        /// </summary>
        private void WaitForCard()
        {
            Display("Waiting for a card...", ConsoleColor.DarkBlue);
            _logger.Information("Waiting for a card");

            _ticketingService.WaitForCard();

            Display("Card found!\nContacting server...", ConsoleColor.Green);
            _logger.Information("Card found");
        }

        /// <summary>
        /// Displays the text in the specified color on the console. The texts displayed are written for the user, the
        /// log messages following the convention of the project (see "Log Messages" in the README of the project).
        /// </summary>
        /// <param name="text">The text to be displayed.</param>
        /// <param name="color">The color in which the text will be displayed on the console.</param>
        private static void Display(string text, ConsoleColor color)
        {
            Console.ForegroundColor = color;
            Console.WriteLine(text);
            Console.ResetColor();
        }
    }
}
