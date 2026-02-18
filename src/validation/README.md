# Keyple Validation Demo

[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../LICENSE)

Android validation terminal application for checking transportation access authorization and creating validation events.
This application represents the entry point to controlled transportation networks where passengers present their cards
for validation.

[⬅️ Back to Main Project](../../README.md)

## Overview

This Android application simulates validation terminals found at transportation network entry points (metro gates, bus boarding, etc.). It validates contracts loaded by the [Reload Demo](../reloading-remote/) and creates validation events that can be later verified by the [Control Demo](../control/).

**Role in Ecosystem**: Second step in the ticketing workflow - validates loaded contracts and grants/denies access to transportation networks.

**Validation Flow**: Season Pass → Multi-trip Ticket → Stored Value (in order of priority)

## Prerequisites

### Hardware Requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- **SAM (Security Access Module)** for Calypso card operations
- Compatible terminal hardware (see [tested terminals](#tested-terminals))

### Software Requirements
- Cards personalized using the [Reload Demo](../reloading-remote/)
- Proper SAM configuration matching card security keys
- Network connectivity (optional, for centralized logging)

### Card Requirements
- Calypso cards with valid contracts loaded
- Storage cards with compatible contract structures
- Supported AIDs (see [Common Library](../common/README.md#supported-card-applications))

## Installation

### Download APK
1. Visit [Releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Download latest `kdt-validation-app-X.Y.Z-debug.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK file

### Build from Source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/validation
./gradlew assembleDebug
```

## Configuration

### Device Selection
1. Launch application
2. Select device type from **Device Selection** screen:
  - **Famoco FX205**: Enterprise terminal with dual readers
  - **Coppernic C-One 2**: Rugged Android terminal
  - **Standard NFC**: Consumer Android device
  - Proprietary terminals (grayed out by default)

### Settings Configuration
1. Open **Settings** from main menu
2. Configure validation parameters:

**Location Settings**:
- **Validation Location**: Unique identifier for this terminal
- **Location Name**: Human-readable location description
- Used for validation event logging and control verification

**Operational Settings**:
- **Battery Powered**: Enable for portable terminals (shows Home screen)
- **Validation Amount**: Cost for stored value contracts (default: 1 unit)
- **Auto-validation**: Immediate validation on card detection

**Security Settings**:
- **SAM Authentication**: Require SAM for all operations (recommended)
- **Event Logging**: Enable transaction history storage
- **Debug Mode**: Detailed logging for troubleshooting

## Usage

### Standard Operation Flow

#### Battery-Powered Mode (Portable Terminals)
```
Device Selection → Settings → Home → Reader Activity → Validation Result
```

#### Fixed Terminal Mode
```
Device Selection → Settings → Reader Activity → Validation Result
```

### Detailed Screen Guide

**Device Selection (`DeviceSelectionActivity`)**
- Choose appropriate hardware plugin
- Proprietary plugins require additional setup (see [Proprietary Plugins](#proprietary-plugins))

**Settings (`SettingsActivity`)**
- Configure location identifier and operational parameters
- Set battery mode and validation amounts
- Access diagnostic and debug options

**Home (`HomeActivity`)** _(Battery-powered mode only)_
- Displays current terminal status
- Quick access to settings and diagnostics
- Manual trigger for card detection phase

**Reader Activity (`ReaderActivity`)**
- Initializes selected Keyple plugin and SAM integration
- Displays "Present Card" message to user
- Shows real-time status during card processing
- Handles card detection and validation procedure

**Validation Results (`CardSummaryActivity`)**

The `CardSummaryActivity` displays both success and failure results:

**Success Screen** (Status.SUCCESS):
- **Location**: Where validation occurred
- **Date/Time**: When validation was processed
- **Contract Details**:
  - Season Pass: Shows validity end date
  - Multi-trip: Shows remaining ticket count
  - Stored Value: Shows remaining balance
- **Visual Feedback**: Green background with success animation

**Failure Screens**:
- **Invalid Card** (Status.INVALID_CARD): Orange background with error details
- **Empty Card** (Status.EMPTY_CARD): Red background showing no tickets available
- **Other Errors**: Red background with specific error message
- **Visual Feedback**: Error animation with sound/haptic feedback

### Validation Scenarios

#### Successful Validations

**Season Pass Validation**:
- Card detected with valid Season Pass
- Contract validity date checked
- Access granted immediately
- No counter decrementation

**Multi-trip Validation**:
- Card detected with remaining trips
- Trip counter decremented by 1
- Access granted
- Remaining count displayed

**Stored Value Validation**:
- Card detected with sufficient balance
- Balance decremented by validation amount
- Access granted
- Remaining balance displayed

#### Failed Validations

**Insufficient Funds/Trips**:
- Multi-trip counter = 0
- Stored value < validation amount
- Access denied with balance information

**Expired Contracts**:
- Season Pass validity date passed
- Contract marked as expired
- Access denied with expiration information

**Invalid Cards**:
- Unsupported AID or card type
- Corrupted data structures
- Hardware communication errors
- Access denied with error details

## Technical Architecture

### Supported Card Types

#### Calypso Cards
**Security Model**:
- Mandatory SAM authentication for all operations
- Secure session management with cryptographic verification
- Full transaction traceability and audit logging
- Support for multiple contracts (1-4 depending on card type)

**Processing Flow**:
1. Card detection and AID selection
2. Environment record validation
3. Event analysis and contract priority evaluation
4. Best contract search with cryptographic verification
5. Counter updates within secure session
6. Event creation and session closure

#### Storage Cards
**Simplified Model**:
- Basic read/write operations without SAM requirements
- Single contract per card
- Streamlined validation procedure
- **Note**: Production implementations should add appropriate security measures

**Processing Flow**:
1. Card detection and data reading
2. Contract validation and date checking
3. Counter updates for applicable contract types
4. Event creation and data writing

### Key Classes

**TicketingService** (`domain/TicketingService.kt`)
- Orchestrates reader initialization, card selection and validation flows
- Manages card and SAM reader lifecycle
- Schedules card selection scenarios for supported cards
- Executes validation procedures based on detected card type

**ReaderManager** (`domain/spi/ReaderManager.kt`)
- Interface for managing card and SAM readers
- Handles plugin registration and reader initialization
- Provides UI feedback hooks (success/failure)
- Implementation: `ReaderManagerImpl`

**CalypsoCardValidationManager** (`domain/managers/CalypsoCardValidationManager.kt`)
- Secure validation procedure for Calypso cards with SAM integration
- Handles contract priority logic (Season Pass → Multi-trip → Stored Value)
- Creates cryptographically verified validation events

**StorageCardValidationManager** (`domain/managers/StorageCardValidationManager.kt`)
- Simplified validation for storage cards (MIFARE Ultralight, ST25 SRT512)
- Direct read/write operations without SAM requirements
- Single contract processing per card

### Validation Procedure Details

#### Contract Priority System

The validation procedure processes contracts in priority order:

| Priority | Contract Type | Validation Logic                   |
|:---------|:--------------|:-----------------------------------|
| 1        | Season Pass   | Check validity date only           |
| 2        | Multi-trip    | Check counter > 0, decrement       |
| 3        | Stored Value  | Check balance >= amount, decrement |
| 31       | Expired       | Skip (automatic marking)           |

#### Best Contract Search Algorithm

```
1. Read environment and validate card expiration
2. Analyze last event for contract priorities
3. Create ordered list of valid contract priorities
4. For each contract in priority order:
   a. Read and validate contract record
   b. Check contract-specific conditions
   c. If valid, select for validation
   d. If expired, mark priority as expired
5. Update selected contract counter
6. Create new validation event
7. Close secure session
```

## Hardware Integration

### Tested Terminals

#### Standard Plugins (Open Source)

**Famoco FX205**
- **Readers**: Dual NFC + SAM reader
- **Plugins**: [Famoco Plugin](https://github.com/calypsonet/keyple-famoco) + [Android NFC](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)
- **Features**: Enterprise-grade security, robust construction
- **Use Case**: Fixed terminal installations

**Coppernic C-One 2**
- **Plugin**: [Coppernic Plugin](https://github.com/calypsonet/keyple-android-plugin-coppernic)
- **Features**: Rugged design, multiple connectivity options
- **Use Case**: Mobile validation scenarios

**Standard NFC Smartphones**
- **Plugin**: [Android NFC Plugin](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)
- **Limitations**: No SAM support (Storage Cards only)
- **Use Case**: Development and testing

#### Proprietary Plugins (On Request)

**Bluebird EF501**
- **Plugin**: [Bluebird Plugin](https://github.com/calypsonet/keyple-plugin-cna-bluebird-specific-nfc-java-lib)
- **Features**: Integrated barcode scanning, enterprise management
- **Access**: Contact [CNA](https://calypsonet.org/contact-us/) for plugin

**Arrive Axio 4**
- **Plugin**: [Arrive Plugin](https://github.com/calypsonet/keyple-plugin-cna-arrive-android-jvm-lib)
- **Features**: Transportation-specific design, multiple payment methods
- **Access**: Contact [CNA](https://calypsonet.org/contact-us/) for plugin

## Development

### Project Structure

```
validation/app/
├── src/main/
│   ├── kotlin/org/calypsonet/keyple/demo/validation/
│   │   ├── data/                        # Data layer implementations
│   │   ├── di/                          # Dependency injection (Dagger)
│   │   │   └── scope/                   # DI scopes
│   │   ├── domain/                      # Business logic layer
│   │   │   ├── builders/                # Data builders
│   │   │   ├── managers/                # Validation managers (Calypso, Storage)
│   │   │   ├── model/                   # Domain models
│   │   │   └── spi/                     # Service provider interfaces
│   │   └── ui/                          # UI layer
│   │       ├── activities/              # Android activities
│   │       │   └── deviceselection/     # Device selection screens
│   │       ├── adapters/                # UI adapters
│   │       ├── mappers/                 # Domain to UI mappers
│   │       └── model/                   # UI models
│   ├── res/                             # Android resources
│   └── AndroidManifest.xml              # App configuration
├── build.gradle.kts                     # Build configuration (Kotlin DSL)
└── proguard-rules.pro                   # Code obfuscation rules
```

### Building and Testing

```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug
```

### Common Issues

**"SAM not detected"**
- Verify SAM is properly inserted in reader
- Check SAM compatibility with cards (Test vs Production keys)
- Ensure SAM reader is powered and connected
- Validate SAM status in terminal diagnostics

**"Card validation failed"**
- Check card has valid contracts loaded via Reload Demo
- Verify card AID is supported by application
- Ensure contract validity dates are current
- Check sufficient balance/trips for validation

**"NFC detection not working"**
- Enable NFC in Android system settings
- Grant NFC permissions to application
- Keep device stable during card reading
- Try different card positioning on NFC antenna

**"Plugin initialization failed"**
- Ensure proper plugin libraries are included
- Check hardware drivers are installed
- Verify device compatibility with selected plugin
- Review device selection matches actual hardware

### Performance Optimization

**Card Reading Speed**:
- Minimize APDU command exchanges
- Use efficient file reading strategies
- Cache frequently accessed data
- Optimize secure session management

**Battery Life** (for portable terminals):
- Implement sleep mode between validations
- Manage NFC discovery cycles efficiently
- Reduce screen brightness during idle
- Use background processing for non-critical tasks

## Proprietary Plugins

### Activation Process

By default, proprietary plugins are deactivated. To enable:

1. **Request Access**: Contact [CNA](https://calypsonet.org/contact-us/) for desired plugin
2. **Install Plugin**: Copy provided `.aar` file to `/libs/` directory
3. **Remove Mock**: Delete corresponding `-mock.aar` file from `/libs/`
4. **Build Project**: Execute `./gradlew assembleDebug` to compile with new plugin
5. **Deploy**: Install updated APK on target device

### Available Proprietary Plugins

- **Bluebird EF501**: Enterprise terminal with barcode integration
- **Arrive Axio 4**: Transportation-specific validation terminal
- **Additional terminals**: Contact CNA for availability

## Security Considerations

### SAM Security
- Use production SAMs only in live environments
- Secure physical access to SAM readers
- Implement SAM lifecycle management procedures
- Monitor SAM authentication failures

### Data Protection
- All validation events are cryptographically secured
- Card data transmission uses secure channels
- No sensitive data stored on mobile device
- Audit logging for compliance and forensics

### Network Security
- Use HTTPS for any server communication
- Implement certificate pinning for production
- Secure API endpoints with proper authentication
- Monitor for unusual transaction patterns

## Contributing

When contributing to this validation application:

1. **Follow Android best practices** for UI and architecture
2. **Test on multiple terminal types** to ensure compatibility
3. **Validate with both Calypso and Storage cards**
4. **Maintain security standards** for cryptographic operations
5. **Update documentation** for new features or terminals

## Related Documentation

- [Main Project Overview](../../README.md)
- [Common Library](../common/README.md) - Data structures and constants
- [Reload Demo](../reloading-remote/README.md) - Contract loading process
- [Control Demo](../control/README.md) - Post-validation verification
- [Eclipse Keyple Documentation](https://keyple.org)

## License

This validation application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.