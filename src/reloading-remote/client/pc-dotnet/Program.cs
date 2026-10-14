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

using System.Configuration;
using App.application;
using App.domain.api;
using App.domain.spi;
using App.infrastructure.pcscreader;
using App.infrastructure.server;
using Microsoft.Extensions.Configuration;
using Serilog;


// Create and configure the logger
Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Debug()
    .Enrich.FromLogContext()
    .WriteTo.File("logs/demo-keyple-less-.log", rollingInterval: RollingInterval.Day, retainedFileCountLimit: 7)
    .CreateLogger();

Log.Information("Starting the application");
try
{
    Log.Information("Loading the parameters");

    IConfigurationBuilder builder = new ConfigurationBuilder()
        .AddJsonFile(Path.Combine(Directory.GetCurrentDirectory(), "appsettings.json"), optional: false, reloadOnChange: false);

    // Load configuration data
    IConfigurationRoot configuration = builder.Build();

    string? serverHost = configuration.GetSection("server")?["host"];
    int serverPort = int.TryParse(configuration.GetSection("server")?["port"], out int parsedPort) ? parsedPort : -1;
    string? serverEndpoint = configuration.GetSection("server")?["endpoint"];
    string? readerName = configuration.GetSection("reader")?["name"];
    string? logLevel = configuration.GetSection("logging")?["loglevel"];

    // Handle null or missing values as needed
    if (serverHost == null)
    {
        throw new ConfigurationErrorsException("Server host missing in the configuration");
    }

    if (serverPort == -1)
    {
        throw new ConfigurationErrorsException("Server port missing or invalid in the configuration");
    }

    if (serverEndpoint == null)
    {
        throw new ConfigurationErrorsException("Server endpoint missing in the configuration");
    }

    if (readerName == null)
    {
        throw new ConfigurationErrorsException("Reader name missing in the configuration");
    }


    Log.Information("Retrieving the reader and server connectors");

    // Get an instance of the ReaderSpi implementation
    ReaderSpi cardService = PcscReaderSpiProvider.GetInstance();

    // Get an instance of the ServerSpi implementation
    ServerSpi server = ServerSpiProvider.GetInstance(serverHost, serverPort, serverEndpoint);

    // Create an instance of the TicketingServiceApi using the reader service and server
    TicketingServiceApi ticketingService = TicketingServiceApiProvider.GetInstance(cardService, readerName, server);

    Log.Information("Creating and starting the application");

    // Create an instance of the application and start it
    Application app = new Application(ticketingService);
    app.Start();
}
catch (Exception ex)
{
    Console.Error.WriteLine("Error while running the application (see log file for details).");
    Log.Fatal(ex, "Application failed");
}
finally
{
    Console.WriteLine("Press any key to exit.");
    Console.ReadKey();
    Log.Information("Closing the application");
    Log.CloseAndFlush();
}
