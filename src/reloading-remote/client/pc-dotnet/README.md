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

1. **Start Application**: Launch `App.exe`
2. **Wait for Card**: Application waits for a card to be presented
3. **Card Processing**: Card is read and processed through server
4. **Result Display**: Success or error message is displayed

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
- **Application.cs**: Main application logic and card processing workflow
- **Program.cs**: Application bootstrapping and service configuration

#### Domain Layer (`/domain`)

**API Interfaces** (`/domain/api`):
- `MainServiceApi`: Main service interface for card operations
- `MainServiceApiAdapter`: Service adapter implementation
- `MainServiceApiProvider`: Service provider

**SPI Interfaces** (`/domain/spi`):
- `ReaderSpi`: Card reader abstraction
- `ServerSpi`: Server communication interface

**Data Models** (`/domain/data`):
- `MessageDto`: Message data transfer object
- `CardRequest/CardResponse`: Card operation DTOs
- `CardSelectionRequest/CardSelectionResponse`: Card selection DTOs
- `ApduRequest/ApduResponse`: APDU command/response DTOs

#### Infrastructure Layer (`/infrastructure`)

**Reader Implementation**:
- **PcscReaderSpiAdapter**: PC/SC reader adapter
- **PcscReaderSpiProvider**: PC/SC reader provider

**Server Implementation**:
- **ServerSpiAdapter**: Server communication adapter
- **ServerSpiProvider**: Server communication provider

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

The application handles various exceptions:

- `ReaderIOException`: Reader communication errors
- `CardIOException`: Card communication errors
- `ServerIOException`: Server communication errors
- `ReaderNotFoundException`: Reader not found
- `UnexpectedStatusWordException`: Unexpected card response

## Development

### Project Structure

```
pc-dotnet/
├── App.csproj                          # Project file
├── appsettings.json                    # Configuration
├── Program.cs                          # Entry point
├── application/
│   └── Application.cs                 # Main application logic
├── domain/
│   ├── api/                          # API interfaces and implementations
│   ├── spi/                          # SPI interfaces
│   ├── data/                         # Data models and DTOs
│   └── utils/                        # Utilities (HexUtil, JsonConverters)
└── infrastructure/
    ├── pcscreader/                   # PC/SC reader implementation
    └── server/                       # Server communication implementation
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
