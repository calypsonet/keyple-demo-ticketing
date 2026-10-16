# Keyple Reload Demo - Java Server

[![Java](https://img.shields.io/badge/java-17%2B-orange.svg)](https://openjdk.java.net/)
[![Quarkus](https://img.shields.io/badge/quarkus-3.40-blue.svg)](https://quarkus.io/)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../../LICENSE)

The server component of the Keyple Reload Demo, providing distributed ticketing services with web-based monitoring and
SAM integration for secure Calypso card operations.

[⬅️ Back to Main Project](../../../README.md)

## Overview

This Java server implements the business logic for the Keyple Demo ecosystem, managing:
- Secure card personalization and contract loading (Calypso and storage cards)
- SAM (Secure Access Module) integration
- Remote services for the clients: Keyple Distributed for the Android client, Keyple Server JSON API for the
  multiplatform and .NET clients
- Web dashboard monitoring the transactions

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     Clients     │     │   Java Server   │     │   PC/SC Reader  │
│                 │────>│                 │────>│                 │
│ Android/iOS/PC  │     │ Quarkus + REST  │     │ SAM Integration │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                  │
                        ┌─────────▼─────────┐
                        │   Web Dashboard   │
                        │                   │
                        │ React + Monitoring│
                        └───────────────────┘
```

### Code Organization

The card processing (`card` package) is split by business operation, `RemoteServiceDispatcher` dispatching each
remote service (`RemoteServiceId`) to the service handling it:

| Class                        | Remote services                                                                                       | Clients                  |
|------------------------------|-------------------------------------------------------------------------------------------------------|--------------------------|
| `ContractAnalysisService`    | `READ_CARD_AND_ANALYZE_CONTRACTS`                                                                     | Android (Keyple)         |
| `ContractLoadingService`     | `READ_CARD_AND_LOAD_CONTRACT`                                                                         | Android (Keyple)         |
| `CardPersonalizationService` | `PERSONALIZE_CARD`                                                                                    | Android (Keyple)         |
| `JsonApiService`             | `SELECT_APP_AND_ANALYZE_CONTRACTS`, `SELECT_APP_AND_LOAD_CONTRACT`, `SELECT_APP_AND_PERSONALIZE_CARD` | Multiplatform (JSON API) |
| `ContractCounterService`     | `SELECT_APP_AND_READ_CONTRACTS`, `SELECT_APP_AND_INCREASE_CONTRACT_COUNTER`                           | .NET                     |

- With the Keyple clients, the client selects the application of the card. With the other clients, the server
  selects it: `JsonApiService` selects the application, then delegates to the analysis, loading or
  personalization service and converts the result into a message ready to be displayed.
- `CardRepository` reads and writes the card files (`CardData`), `TransactionRecorder` records the transactions
  displayed by the
  dashboard, and `CardConfigurator` sets up the Keyple infrastructure (SAM and remote plugins).

## Prerequisites

### Hardware Requirements
- PC/SC compatible card reader
- Calypso SAM (Secure Access Module) for secure operations
- USB connection for reader

### Software Requirements
- **JDK 17+** (OpenJDK recommended)
- **Node.js 20.19+ or 22.12+** (for dashboard development, as required by Vite)
- **Compatible PC/SC reader drivers**
- **Storage card extension** of [CNA](https://calypsonet.org/contact-us/) (`keyple-card-cna-storagecard-java-lib`, on request) to process the storage
  cards (replaced by a mock by default, the storage cards being then not supported; installed as described in the
  [main README](../../../README.md#proprietary-plugins-available-on-request))

### Tested Readers
- Cherry TC series
- SCM Microsystems
- Identive CLOUD series
- HID Global readers
- Generic PC/SC compatible devices

## Quick Start

### Using Pre-built JAR

1. **Download** `kdt-reload-server-X.Y.Z-full.jar` from the [releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases).

2. **Start** the server:
```bash
java -jar kdt-reload-server-X.Y.Z-full.jar
```

3. **Access** the web dashboard at `http://localhost:8080`

### Custom Reader Configuration

If your PC/SC reader name doesn't match the default filter, specify a custom pattern:

**Windows Command Prompt:**
```cmd
java "-Dsam.pcsc.reader.filter=Identive CLOUD 2700 R Smart Card Reader.*" -jar kdt-reload-server-X.Y.Z-full.jar
```

**Windows PowerShell:**
```powershell
java '-Dsam.pcsc.reader.filter=Identive CLOUD 2700 R Smart Card Reader.*' -jar .\kdt-reload-server-X.Y.Z-full.jar
```

**Linux/macOS:**
```bash
java -Dsam.pcsc.reader.filter=".*ACS.*" -jar kdt-reload-server-X.Y.Z-full.jar
```

## Configuration

### Application Properties

The server uses the following default configuration in `application.properties`:

```properties
# CORS Configuration (for web clients)
quarkus.http.cors.enabled=true
quarkus.http.cors.origins=/.*/

# Package Configuration
quarkus.package.jar.type=uber-jar
quarkus.package.runner-suffix=-full

# Logging Configuration
quarkus.log.level=INFO
quarkus.log.category."org.calypsonet.keyple".level=INFO
quarkus.log.category."org.eclipse.keyple".level=INFO

# PC/SC Reader Filter (regex format)
sam.pcsc.reader.filter=.*(Cherry TC|SCM Microsystems|Identive|HID|Generic).*
```

### Environment Variables

Override configuration using environment variables:

```bash
export SAM_PCSC_READER_FILTER=".*Your Reader Name.*"
export QUARKUS_LOG_LEVEL=DEBUG
```

## Development Setup

### Building from Source

1. **Clone** the repository:
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/reload-remote/server
```

2. **Install** dashboard dependencies:
```bash
cd dashboard-app
npm install
cd ..
```

3. **Build** the project (compiles Java + React dashboard, runs the unit tests of the server and of the common
   library, produces the runnable JAR):
```bash
./gradlew build
```

4. **Choose a run mode:**

**Development mode** (hot reload, no prior build required):
```bash
./gradlew quarkusDev
```
> The React dashboard is **not rebuilt** in this mode. Use this for Java development only.

**Dashboard development** (Vite development server with hot reload, the server must be running on port 8080):
```bash
cd dashboard-app
npm start
```
> Opens the dashboard at `http://localhost:3000`. The API calls (`/transactions`, `/card`) are forwarded to the server.

**Run the packaged JAR** (requires step 3):
```bash
./gradlew startServer
```
> Launches the self-contained JAR from `build/`. The React dashboard is embedded.

### Development Mode

Run in development mode with hot reload:

```bash
./gradlew quarkusDev
```

This enables:
- Automatic code recompilation
- Development UI at `http://localhost:8080/q/dev/`

## Web Dashboard

### Features

The React dashboard displays:

- **Server and SAM status**: server reachable and SAM available, checked periodically
- **Transactions**: transactions of the clients (start time, plugin, type, card serial number, status, loaded
  contract), updated in real time (Server-Sent Events)

### Dashboard URLs

- **Main Dashboard**: `http://localhost:8080`

## API Endpoints

### Card Operations

```http
GET  /card/export-card-selection-scenario
POST /card/remote-plugin
GET  /card/sam-availability
```

### Transaction Monitoring

```http
GET  /transactions           # all the transactions (JSON)
GET  /transactions/stream    # new transactions (Server-Sent Events, one JSON transaction per event)
```

## Troubleshooting

### Common Issues

**"SAM Resource is not available" in the dashboard**

The server starts even if no SAM reader is connected or no SAM is inserted, and the dashboard displays this message
until a SAM is detected (no restart needed once the reader is connected and the SAM inserted). If the message persists:
- Verify reader is connected and drivers installed
- Check reader filter pattern (`sam.pcsc.reader.filter`) matches your device name
- Test with `pcsc_scan` on Linux/macOS or Device Manager on Windows
- Ensure SAM is properly inserted in reader
- Verify SAM is compatible with your cards (Test vs Production keys)

**"Port already in use"**
- Kill existing process: `lsof -ti:8080 | xargs kill -9` (Linux/macOS) or check Task Manager (Windows)

**Web dashboard not loading**
- Verify Node.js dependencies: `cd dashboard-app && npm install`
- Clear browser cache and cookies
- Check console for JavaScript errors

### Debug Mode

Enable the detailed logging of the demo (`DEBUG` level: secure sessions, blocks read or written, SAM availability):

```bash
java -Dquarkus.log.category."org.calypsonet.keyple".level=DEBUG -jar kdt-reload-server-X.Y.Z-full.jar
```

The `TRACE` level cannot be used: the minimum level of Quarkus (`quarkus.log.min-level`, `DEBUG` by default) is fixed at
build time.

### Server State

Check that the server is reachable with its SAM available (as the clients and the dashboard do):

```bash
curl http://localhost:8080/card/sam-availability
```

## Security Considerations

See the [Security Considerations](../../../README.md#security-considerations) of the main README.

## License

This server application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.