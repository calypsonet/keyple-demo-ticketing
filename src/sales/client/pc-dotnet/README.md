# Keyple Sales Demo — .NET desktop client

[![.NET](https://img.shields.io/badge/.NET-10.0-purple.svg)](https://dotnet.microsoft.com/)
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

**Key benefits**:
- **Lightweight**: No Keyple middleware dependencies
- **Cross-platform potential**: Adaptable to any language/OS supporting JSON APIs
- **Educational**: Clear demonstration of the underlying communication protocols
- **Flexible**: Easy to integrate into existing .NET applications

## Prerequisites

### Development environment
- **Microsoft Visual Studio 2022** (recommended)
- **.NET 10.0 SDK** or later
- **Windows 10/11** (primary target platform)

### Hardware requirements
- **PC/SC compatible card reader** connected via USB
- **Calypso cards** with a supported AID, personalized by another client (this client does not personalize the
  cards)
- **Network connectivity** to Keyple demo server

### Server requirements
- Running [Keyple Sales Demo server](../../server/README.md) with SAM integration
- Accessible network endpoint (default: `http://localhost:8080`)

## Installation

### Download
1. Download latest `kdt-sales-pc-dotnet-app-YY.MM.DD.zip` from [releases](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Extract to desired directory
3. Run `App.exe`

### Build from source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/sales/client/pc-dotnet
dotnet build --configuration Release
dotnet run
```

## Configuration

### Application settings (`appsettings.json`)

```json
{
  "server": {
    "host": "http://localhost",
    "port": 8080,
    "endpoint": "card/remote-plugin"
  },
  "reader": {
    "name": "SpringCard Puck Contactless 0"
  }
}
```

Configure the server connection and specify your PC/SC reader name.

## Usage

### Workflow

1. **Start application**: Launch `App.exe`
2. **Read the contracts**: present a card; the server selects its application and reads its contracts, which are
   displayed
3. **Load trips**: enter a number of trips between 1 and 20, then present the card again; the server adds the trips to
   the multi-trip ticket (or loads a new one when the card has none)
4. **Result**: success, or the status code and the message of the server

## Technical architecture

### Hexagonal architecture

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

### Key classes

#### Application layer (`/application`)
- **Application.cs**: Main application logic and card processing workflow
- **Program.cs** (project root): Entry point, configuration, and logging

#### Domain layer (`/domain`)

**API interfaces** (`/domain/api`):
- `TicketingServiceApi`: Interface of the ticketing services of the server (reading of the contracts, loading of trips)
- `TicketingServiceApiAdapter`: Service adapter implementation
- `TicketingServiceApiProvider`: Service provider

**SPI interfaces** (`/domain/spi`):
- `ReaderSpi`: Card reader abstraction
- `ServerSpi`: Server communication interface

**Data models** (`/domain/data`):
- `MessageDto`: Message data transfer object
- `CardRequest/CardResponse`: Card operation DTOs
- `CardSelectionRequest/CardSelectionResponse`: Card selection DTOs
- `ApduRequest/ApduResponse`: APDU command/response DTOs

#### Infrastructure layer (`/infrastructure`)

**Reader implementation**:
- **PcscReaderSpiAdapter**: PC/SC reader adapter
- **PcscReaderSpiProvider**: PC/SC reader provider

**Server implementation**:
- **ServerSpiAdapter**: Server communication adapter
- **ServerSpiProvider**: Server communication provider

### Keyple Server JSON API integration

The client implements the Keyple Server JSON API without the full SDK. It starts a remote service with an
`EXECUTE_REMOTE_SERVICE` message, answers the commands of the server (`IS_CONTACTLESS`, `IS_CARD_PRESENT`,
`TRANSMIT_CARD_SELECTION_REQUESTS`, `TRANSMIT_CARD_REQUEST`), until the `END_REMOTE_SERVICE` message giving the result.

**Body of the `EXECUTE_REMOTE_SERVICE` message** (loading of trips):
```json
{
  "serviceId": "SELECT_APP_AND_LOAD_TRIPS",
  "inputData": { "tripsToLoad": 5 },
  "isReaderContactless": true
}
```

**Body of the `END_REMOTE_SERVICE` message**:
```json
{
  "outputData": {
    "statusCode": 0,
    "message": "..."
  }
}
```

The remote services used are `SELECT_APP_AND_READ_CONTRACTS` (the `items` of its output being the descriptions of the
contracts) and `SELECT_APP_AND_LOAD_TRIPS`.

### Error handling

The application handles various exceptions:

- `ReaderIOException`: Reader communication errors
- `CardIOException`: Card communication errors
- `ServerIOException`: Server communication errors
- `ReaderNotFoundException`: Reader not found
- `UnexpectedStatusWordException`: Unexpected card response

## Extending to other platforms

This implementation can serve as a template for other platforms:

- **macOS/Linux**: Replace PC/SC implementation with platform-specific APIs
- **Web applications**: Adapt HTTP client for browser-based JavaScript
- **Mobile platforms**: Use platform NFC APIs instead of PC/SC
- **Other languages**: Translate architecture patterns to Python, Go, etc.

## Development

### Project structure

```
pc-dotnet/
├── App.csproj                        # Project file
├── appsettings.json                  # Configuration
├── Program.cs                        # Entry point
├── application/
│   └── Application.cs                # Main application logic
├── domain/
│   ├── api/                          # API interfaces and implementations
│   ├── spi/                          # SPI interfaces
│   ├── data/                         # Data models and DTOs
│   └── utils/                        # Utilities (HexUtil, JsonConverters)
└── infrastructure/
    ├── pcscreader/                   # PC/SC reader implementation
    └── server/                       # Server communication implementation
```

### Building and testing

```bash
# Restore dependencies
dotnet restore

# Build project
dotnet build

# Create release package
dotnet publish -c Release -r win-x64 --self-contained
```

## Troubleshooting

### Common issues

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
- Check the APDU exchanges in the log file (`logs/demo-keyple-less-<date>.log`, `DEBUG` level)

## Contributing

See the [Contributing](../../../../README.md#contributing) section of the main README. Specific to this client: keep the
hexagonal architecture, without dependency on the Keyple middleware.

## Related documentation

- [Keyple Server JSON API](https://keyple.org/user-guides/non-keyple-client/server-json-api/)
- [Main Project Overview](../../../../README.md)
- [Server Documentation](../../server/README.md)
- [Common Library](../../../common/README.md)

## License

This .NET client is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.
