# Keyple Sales Demo — Android client

[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../../../LICENSE)

Native Android client for the Keyple Sales Demo, enabling secure contract loading onto Calypso and storage cards
through a distributed architecture with remote server management.

[⬅️ Back to Main Project](../../../../README.md)

## Overview

This Android application demonstrates distributed ticketing where all card operations are managed remotely by a Java
server with SAM integration. The mobile client provides the user interface and NFC card communication while delegating
all business logic and cryptographic operations to the server.

**Role in ecosystem**: First step in the ticketing workflow, which loads season passes and multi-trip tickets onto cards
for later validation and inspection.

## Prerequisites

### Hardware requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- Internet connectivity for server communication
- Compatible cards: Calypso cards (see the
  [supported AIDs](../../../../docs/functional-documentation/index.adoc#supported-card-applications)), or storage cards
  (MIFARE Ultralight and MIFARE Classic 1K, and ST25 SRT512 with the Bluebird terminals), with the storage card
  libraries (see [Plugin integration](#plugin-integration))

### Server requirements
- Running [Keyple Sales Demo server](../../server/README.md) with SAM integration
- Network connectivity between client and server
- Proper firewall configuration for server access

## Installation

### Download
1. Visit [Releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Download latest `kdt-sales-android-app-YY.MM.DD-debug.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK file

### Build from source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/sales/client/keyple-mobile-android
./gradlew assembleDebug
```

## Configuration

### Server connection
1. Open **Settings** → **Server**
2. Configure the server connection:
  - **Server IP** (IP address or host name) and **Server port** (for example, `192.168.1.10` and `8080`)
  - **Protocol** (`http` or `https`)
3. Check that the server is ready (reachable, with its SAM available) with the **Server state** button, then **Save**
   (both buttons are enabled when the entries are valid)

### NFC settings
1. Ensure NFC is enabled in Android settings
2. Grant NFC permissions to the application
3. Keep screen on during card operations for stable connection

### Card reader selection
The app supports two card media, each with its reader:
- **Contactless card**: Android NFC reader (all devices), or the reader of the Bluebird terminals
- **SIM card**: OMAPI reader (work in progress)

## Usage

### Quick start workflow

1. **Launch application**
  - Select **Contactless card** from main menu
  - Ensure server is running and accessible

2. **Card personalization** (if needed)
  - Go to **Settings** → **Personalization**, then choose the card medium
  - Present the card to personalize (its data is reset)
  - Wait for confirmation message

3. **Read card content**
  - From main screen, tap **Contactless card**
  - Present card to NFC reader
  - View existing contracts and validity status

4. **Load new contract**
  - Select desired contract type from available options
  - Follow payment simulation workflow
  - Present card again to complete loading

### Screen flow

```
Splash screen → Home → Card reader → Card content → Select product → Checkout → Payment confirmed → Load contract → Result
```

### Detailed screen guide

**Splash screen (`SplashActivity`)**
- Displayed at startup, then opens the home screen

**Home screen (`HomeActivity`)**
- Menu for different card reader types
- Quick access to settings and personalization

**Settings menu (`SettingsMenuActivity`)**
- **Server**: Connection configuration
- **Card media**: Card media shown in the home screen (enabled, disabled, or hidden)
- **Personalization**: Personalization of a card (reset of its data)

**Card reader (`CardReaderActivity`)**
- Initializes selected Keyple plugin
- Establishes server connection with SAM integration
- Reads card content through secure session
- Displays connection status and progress

**Card content (`CardContentActivity`)**
- Shows current card content (Season pass, Multi-trip ticket)
- Displays contract validity and usage status
- Option to load additional contracts

**Select product (`SelectProductActivity`)**
- Lists the products for sale (see the
  [Functional Documentation](../../../../docs/functional-documentation/index.adoc#sales-clients))
- Shows their prices
- Product selection for purchase

**Payment simulation**
- **Checkout (`CheckoutActivity`)**: Simulates credit card payment
- **Payment confirmed (`PaymentConfirmedActivity`)**: Confirms payment success

**Contract loading**
- **Load contract (`LoadContractActivity`)**: Loads selected contract onto card
- **Card operation result (`CardOperationResultActivity`)**: Shows the success or failure of the loading or of the
  personalization

## Known limitations

- The SIM card reader (OMAPI) is a work in progress
- Payment simulation only (no real payment processing)
- Limited offline capability (server connection required)
- Single server connection (no failover support)

## Technical architecture

### Key classes

**CardReaderSession**
- Manages Keyple plugin initialization and cleanup
- Handles NFC detection start/stop and card selection

**ReadContracts**, **LoadContract**, **PersonalizeCard** (use cases)
- Coordinate with server for card operations on the selected card

**TerminalReaders** (`domain/spi/TerminalReaders.kt`)
- Port giving access to the readers of the terminal
- Registers the reader plugins of the card medium (NFC, Bluebird, or OMAPI), then provides its reader
- Implementation: `KeypleTerminalReaders`

**TicketingServer** (`domain/spi/TicketingServer.kt`)
- Port giving access to the remote services of the server (analysis of the contracts, personalization, writing
  of a contract)
- Implementation: `KeypleTicketingServer` (Keyple Distributed)

**BaseCardActivity**
- Base class for card-related activities
- Implements `CardReaderObserverSpi` from Keyple middleware
- Responds to card insertion/removal events
- Triggers appropriate business logic for card processing

**Server communication** (`data/server`)
- `KeypleSyncEndpointClient`: synchronous endpoint of the Keyple Distributed client, exchanging the messages with the
  server (`/card/remote-plugin`)
- `RestServerReadinessChecker`: checks that the server is reachable with its SAM available (`/card/sam-availability`)

### Plugin integration

The application supports multiple reader plugins:

**Standard plugins** (always available):
- Android NFC plugin for standard contactless reading
- OMAPI plugin for SIM card access (work in progress)

**Proprietary components** (on request, replaced by mocks by default; installed as described in the
[main README](../../../../README.md#proprietary-plugins-available-on-request)):
- Bluebird plugin for the reader of the Bluebird terminals
- Storage card extension (`keyple-card-cna-storagecard-java-lib`) and reader plugin
  (`keyple-plugin-cna-storagecard-java-lib`), without which the storage cards are not supported

## Development

### Building and testing

```bash
# Clean build
./gradlew clean

# Build debug version
./gradlew assembleDebug
```

The business rules of the personalization and of the loading are implemented and unit tested by the server (see the
[Server Documentation](../../server/README.md)).

### Development configuration

**Debug settings**:
- Use test server URL for development
- Read the logs in Logcat (Timber, all levels)

### Adding new readers

To integrate additional reader types:

1. Add plugin dependency to `build.gradle.kts`
2. Implement reader-specific initialization in `KeypleTerminalReaders`
3. Add the card medium (`CardMedium`) and its button in the home screen
4. Test with target hardware platform

## Troubleshooting

### Common issues

The common issues of the Android applications (NFC, terminal type) are described in the
[main README](../../../../README.md#troubleshooting).

**"Server error" when reading the card, or "Server not ready" in the server settings**
- Verify server URL and network connectivity (**Server state** button of the server settings)
- Check firewall settings on server and client
- Ensure server is running with its SAM available (see the [Server Documentation](../../server/README.md))

**"Undetermined card type" or "Not valid on this network"**
- Verify that the card has a
  [supported card application](../../../../docs/functional-documentation/index.adoc#supported-card-applications)
- Check that the card is personalized and not expired (the reason is displayed with "Not valid on this network")
- Try different card positioning on NFC antenna

**"Ticket not loaded"**
- The reason is displayed with the result, for example "No space left on the card for a new ticket" (see the
  [business rules of the loading](../../../../docs/functional-documentation/index.adoc#loading-use-case) in the
  Functional Documentation)
- Ensure server has SAM properly configured

### Debug mode

The logs are written to Logcat (Timber): view them in the Android Studio Logcat or with `adb logcat`.

### Server connectivity test

Test server connection:
```bash
# From development machine
curl http://your-server:8080/card/sam-availability
```

## Contributing

See the [Contributing](../../../../README.md#contributing) section of the main README. Specific to this client: keep the
compatibility with the API level 26, and update the server when a remote service changes.

## Related documentation

- [Main Project Overview](../../../../README.md)
- [Common Library](../../../common/README.md): Data structures and types
- [Server Documentation](../../server/README.md): Server setup and API
- [Validation App](../../../validation/README.md): Next step in workflow

## License

This Android application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.