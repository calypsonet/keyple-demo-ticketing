# Keyple Reload Demo - Android Client

[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../../../LICENSE)

Native Android client for the Keyple Reload Demo, enabling secure contract loading onto Calypso and storage cards
through a distributed architecture with remote server management.

[⬅️ Back to Main Project](../../../../README.md)

## Overview

This Android application demonstrates distributed ticketing where all card operations are managed remotely by a Java
server with SAM integration. The mobile client provides the user interface and NFC card communication while delegating
all business logic and cryptographic operations to the server.

**Role in Ecosystem**: First step in the ticketing workflow - loads Season pass and Multi-trip contracts onto cards for
later validation and control.

## Prerequisites

### Hardware Requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- Internet connectivity for server communication
- Compatible cards: Calypso cards (see [supported AIDs](../../../common/README.md#supported-card-applications)), or
  storage cards (MIFARE Ultralight and MIFARE Classic 1K, and ST25 SRT512 with the Bluebird terminals)

### Server Requirements
- Running [Keyple Reload Demo server](../../server/README.md) with SAM integration
- Network connectivity between client and server
- Proper firewall configuration for server access

## Installation

### Download APK
1. Visit [Releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Download latest `kdt-reloading-android-app-X.Y.Z-debug.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK file

### Build from Source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/reloading-remote/client/keyple-mobile-android
./gradlew assembleDebug
```

## Configuration

### Server Connection
1. Open **Settings** → **Server**
2. Configure the server connection:
  - **Server IP** and **Server port** (e.g. `192.168.1.10` and `8080`)
  - **Protocol** (e.g. `http://`)
3. Check that the server is ready (reachable, with its SAM available) with the **Server state** button, then **Save**

### NFC Settings
1. Ensure NFC is enabled in Android settings
2. Grant NFC permissions to the application
3. Keep screen on during card operations for stable connection

### Card Reader Selection
The app supports two card media, each with its reader:
- **Contactless card**: Android NFC reader (all devices), or the reader of the Bluebird terminals
- **SIM Card**: OMAPI reader (work in progress)

## Usage

### Quick Start Workflow

1. **Launch Application**
  - Select **Contactless card** from main menu
  - Ensure server is running and accessible

2. **Card Personalization** (if needed)
  - Go to **Settings** → **Personalization**, then choose the card medium
  - Present the card to personalize (its data is reset)
  - Wait for confirmation message

3. **Read Card Content**
  - From main screen, tap **Contactless card**
  - Present card to NFC reader
  - View existing contracts and validity status

4. **Load New Contract**
  - Select desired contract type from available options
  - Follow payment simulation workflow
  - Present card again to complete loading

### Screen Flow

```
Splash Screen → Home → Card Reader → Card Content → Select Product → Checkout → Payment Confirmed → Load Contract → Result
```

### Detailed Screen Guide

**Splash Screen (`SplashActivity`)**
- Displayed at startup, then opens the home screen

**Home Screen (`HomeActivity`)**
- Menu for different card reader types
- Quick access to settings and personalization

**Settings Menu (`SettingsMenuActivity`)**
- **Server Settings**: Connection configuration
- **Card media**: Card media shown in the home screen (enabled, disabled or hidden)
- **Personalization**: Personalization of a card (reset of its data)

**Card Reader (`CardReaderActivity`)**
- Initializes selected Keyple plugin
- Establishes server connection with SAM integration
- Reads card content through secure session
- Displays connection status and progress

**Card Content (`CardContentActivity`)**
- Shows current card content (Season pass, Multi-trip ticket)
- Displays contract validity and usage status
- Option to load additional contracts

**Select Product (`SelectProductActivity`)**
- Lists the products for sale: multi-trip tickets (1 to 4 trips) and season pass
- Shows their prices
- Product selection for purchase

**Payment Simulation**
- **Checkout (`CheckoutActivity`)**: Simulates credit card payment
- **Payment Confirmed (`PaymentConfirmedActivity`)**: Confirms payment success

**Contract Loading**
- **Load Contract (`LoadContractActivity`)**: Loads selected contract onto card
- **Card Operation Result (`CardOperationResultActivity`)**: Shows the success or failure of the loading or of the
  personalization

## Technical Architecture

### Key Classes

**CardReaderSession**
- Manages Keyple plugin initialization and cleanup
- Handles NFC detection start/stop and card selection

**ReadContracts**, **LoadContract**, **PersonalizeCard** (use cases)
- Coordinate with server for card operations on the selected card

**TerminalReaders** (`domain/spi/TerminalReaders.kt`)
- Port giving access to the readers of the terminal
- Registers the reader plugins of the card medium (NFC, Bluebird or OMAPI), then provides its reader
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

**Server Communication** (`data/server`)
- `KeypleSyncEndpointClient`: synchronous endpoint of the Keyple Distributed client, exchanging the messages with the
  server (`/card/remote-plugin`)
- `RestServerReadinessChecker`: checks that the server is reachable with its SAM available (`/card/sam-availability`)

### Plugin Integration

The application supports multiple reader plugins:

**Standard Plugins** (always available):
- Android NFC plugin for standard contactless reading
- OMAPI plugin for SIM card access (work in progress)

**Proprietary Plugin** (on request, replaced by a mock by default):
- Bluebird plugin for the reader of the Bluebird terminals

### Security Model

**Distributed Security Architecture**:
- All cryptographic operations performed on server
- Client transmits card data securely to server
- Server manages SAM integration and secure sessions
- No sensitive cryptographic material stored on mobile device

## Development

### Building the Project

```bash
# Clean build
./gradlew clean

# Build debug version
./gradlew assembleDebug
```

### Development Configuration

**Debug Settings**:
- Use test server URL for development
- Read the logs in Logcat (Timber, all levels)

**Testing**:
- The business rules of the personalization and of the loading are implemented and unit tested by the server (see the
  [Server Documentation](../../server/README.md))

### Adding New Readers

To integrate additional reader types:

1. Add plugin dependency to `build.gradle.kts`
2. Implement reader-specific initialization in `KeypleTerminalReaders`
3. Add the card medium (`CardMedium`) and its button in the home screen
4. Test with target hardware platform

## Troubleshooting

### Common Issues

**"Server connection failed"**
- Verify server URL and network connectivity
- Check firewall settings on server and client
- Ensure server is running with proper configuration

**"NFC not working"**
- Enable NFC in Android system settings
- Grant NFC permission to application
- Keep device stable during card reading

**"Card not recognized"**
- Verify card contains supported AID
- Check card is properly personalized
- Try different card positioning on NFC antenna

**"Loading failed"**
- Ensure server has SAM properly configured
- Check card has available contract slots

### Debug Mode

The logs are written to Logcat (Timber): view them in the Android Studio Logcat or with `adb logcat`.

### Server Connectivity Test

Test server connection:
```bash
# From development machine
curl http://your-server:8080/card/sam-availability
```

## Known Limitations

- The SIM card reader (OMAPI) is a work in progress
- Payment simulation only - no real payment processing
- Limited offline capability (server connection required)
- Single server connection (no failover support)

## Contributing

When contributing to this Android client:
1. Follow Android development best practices
2. Maintain compatibility with minimum API level 26
3. Test on various Android devices and versions
4. Update corresponding server integration if needed
5. Follow the project's code style and documentation standards

## Related Documentation

- [Main Project Overview](../../../../README.md)
- [Common Library](../../../common/README.md) - Data structures and types
- [Server Documentation](../../server/README.md) - Server setup and API
- [Validation App](../../../validation/README.md) - Next step in workflow

## License

This Android application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.