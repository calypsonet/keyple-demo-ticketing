# Keyple Reload Demo - Android Client

[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../../../LICENSE)

Native Android client for the Keyple Reload Demo, enabling secure contract loading onto Calypso cards through a
distributed architecture with remote server management.

[⬅️ Back to Main Project](../../../../README.md)

## Overview

This Android application demonstrates distributed ticketing where all card operations are managed remotely by a Java server with SAM integration. The mobile client provides the user interface and NFC card communication while delegating all business logic and cryptographic operations to the server.

**Role in Ecosystem**: First step in the ticketing workflow - loads Season Pass and Multi-trip contracts onto cards for later validation and control.

## Prerequisites

### Hardware Requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- Internet connectivity for server communication
- Compatible Calypso cards (see [supported AIDs](../common-lib/README.md#supported-card-applications))

### Server Requirements
- Running Keyple Demo Server with SAM integration
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
2. Configure server connection:
  - **Server URL**: `http://your-server:8080`
  - **Connection timeout**: 30 seconds (default)
  - **Read timeout**: 60 seconds (default)
3. Test connection using **Test Connection** button

### NFC Settings
1. Ensure NFC is enabled in Android settings
2. Grant NFC permissions to the application
3. Keep screen on during card operations for stable connection

### Card Reader Selection
The app supports multiple NFC reader types:
- **Contactless Support**: Standard Android NFC (all devices)
- **SIM Card**: OMAPI reader access (work in progress)
- **Embedded SE**: Wizway plugin for eSE (work in progress)

## Usage

### Quick Start Workflow

1. **Launch Application**
  - Select **Contactless Support** from main menu
  - Ensure server is running and accessible

2. **Card Personalization** (if needed)
  - Go to **Settings** → **Personalization**
  - Present card to initialize with clean state
  - Wait for confirmation message

3. **Read Card Content**
  - From main screen, tap **Contactless Support**
  - Present card to NFC reader
  - View existing contracts and validity status

4. **Load New Contract**
  - Select desired contract type from available options
  - Follow payment simulation workflow
  - Present card again to complete loading

### Screen Flow

```
Main Screen → Card Reader → Card Summary → Select Tickets → Checkout → Payment → Charge → Result
```

### Detailed Screen Guide

**Main Screen (`MainActivity`)**
- Setup and configuration access
- Reader type selection

**Home Screen (`HomeActivity`)**
- Menu for different card reader types
- Quick access to settings and personalization

**Settings Menu (`SettingsMenuActivity`)**
- **Server Settings**: Connection configuration
- **Configuration**: Plugin availability settings
- **Personalization**: Card reset and initialization

**Card Reader (`CardReaderActivity`)**
- Initializes selected Keyple plugin
- Establishes server connection with SAM integration
- Reads card content through secure session
- Displays connection status and progress

**Card Summary (`CardSummaryActivity`)**
- Shows current card content (Season Pass/Multi-trip)
- Displays contract validity and usage status
- Option to load additional contracts

**Select Tickets (`SelectTicketsActivity`)**
- Lists available products from server
- Shows pricing and validity information
- Contract selection for purchase

**Payment Simulation**
- **Checkout (`CheckoutActivity`)**: Simulates credit card payment
- **Payment Validated (`PaymentValidatedActivity`)**: Confirms payment success

**Contract Loading**
- **Charge (`ChargeActivity`)**: Loads selected contract onto card
- **Charge Result (`ChargeResultActivity`)**: Shows success/failure status

## Technical Architecture

### Key Classes

**TicketingService**
- Orchestrates the ticketing process lifecycle
- Manages Keyple plugin initialization and cleanup
- Handles NFC detection start/stop
- Coordinates with server for card operations

**ReaderRepository**
- Interface between business layer and card reader
- Abstracts reader-specific operations
- Provides consistent API for different reader types

**CardReaderObserver**
- Implements `CardReaderObserverSpi` from Keyple middleware
- Responds to card insertion/removal events
- Triggers appropriate business logic for card processing

**Server Communication**
- RESTful API client for server interaction
- Handles authentication and session management
- Manages secure data transmission for card operations

### Plugin Integration

The application supports multiple reader plugins:

**Standard Plugins** (always available):
- Android NFC plugin for standard contactless reading
- OMAPI plugin for SIM card access (development)

**Development Plugins**:
- Wizway plugin for embedded Secure Element access

### Security Model

**Distributed Security Architecture**:
- All cryptographic operations performed on server
- Client transmits card data securely to server
- Server manages SAM integration and secure sessions
- No sensitive cryptographic material stored on mobile device

## Development

### Building the Project

```bash
# Install dependencies
./gradlew clean

# Build debug version
./gradlew assembleDebug
```

### Development Configuration

**Debug Settings**:
- Enable debug logging in `application.properties`
- Use test server URL for development
- Mock server responses for offline development

**Testing**:
- Unit tests for business logic components
- Integration tests with mock server
- NFC simulation for testing without physical cards

### Adding New Readers

To integrate additional reader types:

1. Add plugin dependency to `build.gradle`
2. Implement reader-specific initialization in `TicketingService`
3. Add UI selection option in device selection screen
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
- Verify sufficient balance for stored value operations

### Debug Mode

Enable detailed logging:
1. Go to **Settings** → **Configuration**
2. Enable **Debug Mode**
3. View logs in Android Studio logcat or device logs

### Server Connectivity Test

Test server connection:
```bash
# From development machine
curl http://your-server:8080/api/system/status

# Expected response
{"status": "UP", "readers": [...]}
```

## Known Limitations

- SIM Card and Embedded SE readers are work in progress
- Payment simulation only - no real payment processing
- Limited offline capability (server connection required)
- Single server connection (no failover support)

## Contributing

When contributing to this Android client:
1. Follow Android development best practices
2. Maintain compatibility with minimum API level 24
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