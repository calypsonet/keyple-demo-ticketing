# Keyple Reload Demo - .NET Desktop Client

[![.NET](https://img.shields.io/badge/.NET-7.0-purple.svg)](https://dotnet.microsoft.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../../../LICENSE)

A .NET desktop client demonstrating how to integrate with the Keyple server ecosystem **without using the Keyple
middleware**, implementing only
the [Keyple Server JSON API](https://keyple.org/user-guides/non-keyple-client/server-json-api/) for server
communication.

[⬅️ Back to Main Project](../../../../README.md)

## Overview

This C# application targets the .NET environment on Windows and serves as a reference implementation for developing
applications that interact with Keyple-based servers using standard JSON APIs instead of the full Keyple middleware. It
demonstrates the minimal integration approach while maintaining full functionality.

**Key Benefits**:
- **Lightweight**: No Keyple middleware dependencies
- **Cross-platform potential**: Adaptable to any language/OS supporting JSON APIs
- **Educational**: Clear demonstration of the underlying communication protocols
- **Flexible**: Easy to integrate into existing .NET applications

## Prerequisites

### Development Environment
- **Microsoft Visual Studio 2022** (recommended)
- **.NET 7.0 SDK** or later
- **Windows 10/11** (primary target platform)

### Hardware Requirements
- **PC/SC compatible card reader** connected via USB
- **Calypso cards** pre-personalized with supported AIDs
- **Network connectivity** to Keyple demo server

### Server Requirements
- Running [Keyple Demo Server](../server/README.md) with SAM integration
- Accessible network endpoint (default: `http://localhost:8080`)

## Installation

### Option 1: Pre-built Release
1. Download latest `kdt-reloading-pc-dotnet-app-X.Y.Z.zip` from [releases](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Extract to desired directory
3. Run `App.exe`

### Option 2: Build from Source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/reloading-remote/client/pc-dotnet
dotnet build --configuration Release
dotnet run
```

## Configuration

### Application Settings (`appsettings.json`)

```json
{
  "Server": {
    "BaseUrl": "http://localhost:8080",
    "Timeout": 30000,
    "RetryAttempts": 3
  },
  "PcscReader": {
    "ReaderNameFilter": ".*",
    "ConnectionTimeout": 5000,
    "ReconnectAttempts": 2
  },
  "CardOperations": {
    "DefaultValidationAmount": 1,
    "MaxContractSlots": 4,
    "SupportedAids": [
      "A000000291FF9101",
      "315449432E49434131",
      "315449432E49434133",
      "A0000004040125090101"
    ]
  },
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "System": "Warning",
      "Microsoft": "Warning"
    }
  }
}
```

### PC/SC Reader Configuration

The application auto-detects available PC/SC readers. To specify a particular reader:

```json
{
  "PcscReader": {
    "ReaderNameFilter": "ACS ACR122U",
    "PreferredReader": "ACS ACR122U PICC Interface"
  }
}
```

## Usage

### Command Line Interface

The application provides a simple command-line interface:

```
Keyple .NET Demo Client
======================

1. List available readers
2. Read card content
3. Load contract
4. Personalize card
5. Exit

Select option:
```

### Typical Workflow

1. **Start Application**: Launch `App.exe`
2. **Reader Detection**: Application automatically detects PC/SC readers
3. **Present Card**: Place card on reader when prompted
4. **View Contracts**: Existing contracts are displayed with status
5. **Add Units**: Enter number of units to add to multi-trip contract
6. **Present Card Again**: Card is updated with new contract data
7. **Confirmation**: Success/failure message is displayed

### Example Session

```
Starting Keyple .NET Client...
✓ Server connection established
✓ Found PC/SC reader: ACS ACR122U PICC Interface

Please present your card...

Card detected: A000000291FF9101
Environment: Valid until 2030-12-31

Existing contracts:
  Contract 1: Multi-trip ticket (15 trips remaining)
  Contract 2: Empty
  Contract 3: Empty
  Contract 4: Empty

Enter number of units to add to multi-trip contract: 10

Please present the card again to update...

✓ Contract updated successfully!
New balance: 25 trips

Operation completed. Press any key to continue...
```

## Technical Architecture

### Hexagonal Architecture

The project follows a clean hexagonal architecture pattern:

```
┌─────────────────────────────────────────┐
│                Application              │
│  ┌─────────────────────────────────────┐│
│  │              Domain                 ││
│  │  ┌─────────────┐  ┌─────────────┐  ││
│  │  │     API     │  │     SPI     │  ││
│  │  │ (Interfaces)│  │(Interfaces) │  ││
│  │  └─────────────┘  └─────────────┘  ││
│  │  ┌─────────────────────────────────┐││
│  │  │            Data Models          │││
│  │  └─────────────────────────────────┘││
│  └─────────────────────────────────────┘│
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│             Infrastructure              │
│  ┌─────────────┐    ┌─────────────────┐ │
│  │   Reader    │    │     Server      │ │
│  │Implementation│    │ Implementation  │ │
│  └─────────────┘    └─────────────────┘ │
└─────────────────────────────────────────┘
```

### Key Components

#### Application Layer (`/application`)
- **App.cs**: Main application entry point and CLI handling
- **Program.cs**: Application bootstrapping and dependency injection
- **CommandHandler.cs**: Command processing and user interaction

#### Domain Layer (`/domain`)

**API Interfaces** (`/domain/api`):
- `ICardService`: Core business operations
- `ITicketingService`: High-level ticketing workflows
- `IContractManager`: Contract manipulation logic

**SPI Interfaces** (`/domain/spi`):
- `ICardReader`: Card reader abstraction
- `IServerConnector`: Server communication interface
- `IConfigurationProvider`: Configuration management

**Data Models** (`/domain/data`):
- `CardData`: Card information and status
- `ContractInfo`: Contract details and metadata
- `ServerRequest/Response`: Communication DTOs

#### Infrastructure Layer (`/infrastructure`)

**Reader Implementation**:
- **PcscCardReader**: Direct PC/SC API integration
- **CardConnectionManager**: Connection lifecycle management
- **ApduCommandBuilder**: Low-level card command construction

**Server Implementation**:
- **HttpServerConnector**: RESTful API client using HttpClient
- **JsonApiClient**: Keyple Distributed JSON API implementation
- **AuthenticationHandler**: Server authentication (if required)

### Keyple Distributed JSON API Integration

The client implements the Keyple Distributed JSON API without the full SDK:

**Card Selection Request**:
```json
{
  "action": "EXECUTE_REMOTE_SERVICE",
  "serviceId": "RELOAD_CONTRACT",
  "inputData": {
    "cardSelectionRequests": [...],
    "contractData": {...}
  }
}
```

**Server Response**:
```json
{
  "outputData": {
    "cardSelectionResponses": [...],
    "executionStatus": "SUCCESS",
    "contractUpdateResult": {...}
  }
}
```

### Error Handling

Comprehensive error handling across all layers:

```csharp
try
{
    var result = await cardService.LoadContractAsync(contractData);
    Console.WriteLine($"Success: {result.Message}");
}
catch (CardReaderException ex)
{
    Console.WriteLine($"Reader error: {ex.Message}");
}
catch (ServerCommunicationException ex)
{
    Console.WriteLine($"Server error: {ex.Message}");
}
catch (InvalidCardDataException ex)
{
    Console.WriteLine($"Card data error: {ex.Message}");
}
```

## Development

### Project Structure

```
pc-dotnet/
├── App.csproj                          # Project file
├── appsettings.json                    # Configuration
├── Program.cs                          # Entry point
├── application/
│   ├── App.cs                         # Main application logic
│   └── CommandHandlers/               # CLI command handlers
├── domain/
│   ├── api/                          # Business interfaces
│   ├── spi/                          # Infrastructure interfaces
│   ├── data/                         # Data models and DTOs
│   └── utils/                        # Domain utilities
└── infrastructure/
    ├── reader/                       # PC/SC reader implementation
    ├── server/                       # HTTP server client
    └── config/                       # Configuration providers
```

### Building and Testing

```bash
# Restore dependencies
dotnet restore

# Build project
dotnet build

# Run tests
dotnet test

# Create release package
dotnet publish -c Release -r win-x64 --self-contained
```

### Adding New Features

To extend the client with additional functionality:

1. **Define Domain Interface**: Add interface in `/domain/api`
2. **Implement Infrastructure**: Create implementation in `/infrastructure`
3. **Update Application**: Modify CLI handlers in `/application`
4. **Configure DI**: Register services in `Program.cs`

### Custom Reader Integration

To support additional card readers:

```csharp
public class CustomCardReader : ICardReader
{
    public async Task<CardData> ReadCardAsync(CancellationToken cancellationToken)
    {
        // Custom reader implementation
        // Return standardized CardData object
    }

    public async Task WriteCardAsync(CardData cardData, CancellationToken cancellationToken)
    {
        // Custom write implementation
    }
}
```

## Troubleshooting

### Common Issues

**"No PC/SC service available"**
- Ensure PC/SC service is running: `services.msc` → "Smart Card"
- Restart the service if stopped
- Check reader drivers are properly installed

**"Card reader not detected"**
- Verify reader is connected via USB
- Check Device Manager for reader status
- Try different USB ports or cables
- Update reader drivers from manufacturer

**"Server connection timeout"**
- Verify server URL in `appsettings.json`
- Check network connectivity: `ping your-server`
- Ensure server is running and accessible
- Check Windows Firewall settings

**"Card not recognized"**
- Verify card contains supported AID
- Check card is properly positioned on reader
- Try different cards to isolate issue
- Enable debug logging to see APDU exchanges

### Debug Configuration

Enable detailed logging:

```json
{
  "Logging": {
    "LogLevel": {
      "Default": "Debug",
      "Infrastructure.Reader": "Trace",
      "Infrastructure.Server": "Debug"
    }
  }
}
```

### Testing Without Hardware

Mock implementations for testing:

```csharp
// Program.cs
#if DEBUG
services.AddSingleton<ICardReader, MockCardReader>();
services.AddSingleton<IServerConnector, MockServerConnector>();
#else
services.AddSingleton<ICardReader, PcscCardReader>();
services.AddSingleton<IServerConnector, HttpServerConnector>();
#endif
```

## Performance Considerations

- **Connection Pooling**: HttpClient reuse for server communication
- **Card Reader Caching**: Maintain reader connections between operations
- **Async Operations**: Non-blocking I/O for better responsiveness
- **Memory Management**: Proper disposal of PC/SC resources

## Security Notes

- **No Local Cryptography**: All security operations performed on server
- **Data Transmission**: Use HTTPS for production server communication
- **Card Data**: Sensitive information never persisted locally
- **Authentication**: Implement server authentication for production use

## Extending to Other Platforms

This implementation can serve as a template for other platforms:

- **macOS/Linux**: Replace PC/SC implementation with platform-specific APIs
- **Web Applications**: Adapt HTTP client for browser-based JavaScript
- **Mobile Platforms**: Use platform NFC APIs instead of PC/SC
- **Other Languages**: Translate architecture patterns to Python, Go, etc.

## Contributing

When contributing to this .NET client:
1. Maintain the hexagonal architecture pattern
2. Follow C# coding conventions and best practices
3. Add unit tests for new functionality
4. Update documentation for API changes
5. Test on different Windows versions and reader types

## Related Documentation

- [Keyple Server JSON API](https://keyple.org/user-guides/non-keyple-client/server-json-api/)
- [Main Project Overview](../../../../README.md)
- [Server Documentation](../../server/README.md)
- [Common Library](../../../common/README.md)

## License

This .NET client is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.
