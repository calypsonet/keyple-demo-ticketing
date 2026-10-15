# Keyple Validation Demo

[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../LICENSE)

Android validation terminal application for checking transportation access authorization and creating validation events.
This application represents the entry point to controlled transportation networks where passengers present their cards
for validation.

[⬅️ Back to Main Project](../../README.md)

## Overview

This Android application simulates validation terminals found at transportation network entry points (metro gates, bus
boarding, etc.). It validates contracts loaded by the [Reload Demo](../reloading-remote/) and creates validation events
that can be later verified by the [Control Demo](../control/).

**Role in Ecosystem**: Second step in the ticketing workflow - validates loaded contracts and grants/denies access to
transportation networks.

**Validation Flow**: Season pass → Multi-trip ticket (in order of priority)

## Prerequisites

### Hardware Requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- **SAM (Secure Access Module)** for the Calypso cards, matching the keys of the cards (test SAM for the demo cards)
- Compatible terminal hardware (see [tested terminals](#tested-terminals))

### Card Requirements
- Cards personalized and loaded using the [Reload Demo](../reloading-remote/)
- Calypso cards, or storage cards (MIFARE Ultralight, ST25 SRT512, MIFARE Classic)
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

### Terminal Selection
1. Launch application
2. Select the terminal type from **Terminal Selection** screen:
  - **Famoco**: terminal with a SAM reader
  - **Arrive**, **Bluebird**: proprietary plugins (grayed out by default, see
    [Proprietary Plugins](#proprietary-plugins))

### Settings Configuration
The **Settings** screen, displayed after the terminal selection, configures the validation:
- **Battery powered mode**: for the portable terminals, which display the Home screen before each validation
- **Location**: location of the terminal, written in the validation event and checked by the control
- **Time and date**: shortcut to the date and time settings of Android

## Usage

### Standard Operation Flow

#### Battery-Powered Mode (Portable Terminals)
```
Terminal Selection → Settings → Home → Card Reader → Validation Result
```

#### Fixed Terminal Mode
```
Terminal Selection → Settings → Card Reader → Validation Result
```

### Detailed Screen Guide

**Terminal Selection (`TerminalSelectionActivity`)**
- Choose appropriate hardware plugin
- Proprietary plugins require additional setup (see [Proprietary Plugins](#proprietary-plugins))

**Settings (`SettingsActivity`)**
- Battery powered mode, location of the terminal, access to the date and time settings

**Home (`HomeActivity`)** _(Battery-powered mode only)_
- Start of the validation

**Card Reader (`CardReaderActivity`)**
- Waits for a card ("Please present your contactless card for validation")
- Selects the card and runs the validation procedure supporting it

**Validation Result**

The validation result overlay of the reader screen (`CardReaderActivity`) displays both success and failure results
(`ValidationResult`):

**Success Screen** (`ValidationResult.Accepted`):
- **Location**: Where validation occurred
- **Date/Time**: When validation was processed
- **Contract Details**:
  - Season pass: Shows validity end date
  - Multi-trip ticket: Shows the number of remaining trips
- **Visual Feedback**: Green background with success animation

**Failure Screens**:
- **Invalid Card** (`ValidationResult.Rejected`, card data refused: environment, versions, already validated): Orange
  background with the reason of the refusal
- **No Ticket** (`ValidationResult.Rejected`, no contract to use: no valid contract, expired contract, no trips left):
  Red background with the reason of the refusal
- **Other Errors** (`ValidationResult.Failed`): Red background with the error message
- **Visual Feedback**: Error animation, with the sounds and LEDs of the terminal

### Validation Scenarios

#### Successful Validations

**Season pass validation**:
- Card detected with valid Season pass
- Contract validity date checked
- Access granted
- No counter decrement

**Multi-trip ticket validation**:
- Card detected with remaining trips
- Trip counter decremented by 1
- Access granted
- Remaining count displayed

#### Failed Validations

**No Trip Left**:
- Multi-trip counter = 0
- Access denied

**Already Validated**:
- Card validated again within the anti-passback delay
- Access denied

**Expired Contracts**:
- Season pass validity date passed
- Contract marked as expired
- Access denied with expiration information

**Invalid Cards**:
- Unsupported AID or card type
- Data of an unexpected version, or environment expired
- Access denied with the reason of the refusal

## Technical Architecture

### Supported Card Types

#### Calypso Cards
**Security Model**:
- Secure session with the `DEBIT` access level, authenticated by the SAM
- Support for multiple contracts (1-4 depending on card type)

**Processing Flow**:
1. Card detection and AID selection
2. Environment record validation
3. Event analysis and contract priority evaluation
4. Best contract search
5. Counter update within the secure session
6. Event creation and session closure

#### Storage Cards
**Simplified Model**:
- Reading and writing without cryptographic operation
- Single contract per card
- Streamlined validation procedure
- **Note**: Production implementations should add appropriate security measures

**Processing Flow**:
1. Card detection and data reading
2. Contract validation and date checking
3. Counter updates for applicable contract types
4. Event creation and data writing

### Key Classes

**CardReaderSession** (`domain/CardReaderSession.kt`)
- Manages card and SAM reader lifecycle, and the user feedback devices
- Schedules card selection scenarios for supported cards, and selects the presented card

**ValidateCard** (`domain/usecases/ValidateCard.kt`)
- Validates the presented card: selection, execution of the validation procedure supporting it and user feedback of
  the result (`ValidationResult`)

**TerminalReaders** (`domain/spi/TerminalReaders.kt`)
- Port giving access to the readers of the terminal
- Registers the reader plugins of the terminal type, then provides the card reader and the SAM reader
- Implementation: `KeypleTerminalReaders`

**BaseValidationProcedure** (`domain/procedures/BaseValidationProcedure.kt`)
- Business rules common to the validation procedures: versions, expiration, anti-passback, trips left, order of the
  contracts

**CalypsoCardValidationProcedure** (`domain/procedures/CalypsoCardValidationProcedure.kt`)
- Secure validation procedure for Calypso cards with SAM integration
- Handles contract priority logic (Season pass → Multi-trip ticket)
- Writes the validation event within the secure session

**StorageCardValidationProcedure** (`domain/procedures/StorageCardValidationProcedure.kt`)
- Simplified validation for storage cards (MIFARE Ultralight, ST25 SRT512, MIFARE Classic)
- Direct reading and writing, without cryptographic operation
- Single contract processing per card

### Validation Procedure Details

#### Contract Priority System

The validation procedure processes contracts in priority order:

| Priority | Contract Type | Validation Logic                   |
|:---------|:--------------|:-----------------------------------|
| 1        | Season pass   | Check validity date only           |
| 2        | Multi-trip    | Check counter > 0, decrement       |
| 3        | Stored value  | Skip (not supported by the demo)   |
| 31       | Expired       | Skip (automatic marking)           |
| Other    | Unknown       | Skip (never validated)             |

#### Best Contract Search Algorithm

```
1. Read environment and validate card expiration
2. Analyze last event for contract priorities, and apply the anti-passback
3. Create ordered list of valid contract priorities
4. For each contract in priority order:
   a. Read and validate contract record
   b. Check the conditions of its type (ContractTariff)
   c. If valid, select for validation
   d. If expired or exhausted, mark priority as expired
5. Update selected contract counter
6. Create new validation event, or write the updated priorities if the card is refused
7. Close secure session
```

Additions to the procedure of the data model:
- **Anti-passback**: a card validated again within the anti-passback delay (1 minute, the times being compared to the
  minute as the event) is refused, unless the previous validation session was interrupted by the removal of the card
  (session not ratified): this session is then considered as validated, without a new decrement.
- **Refused card**: when no contract can be used, the last event is written with the updated priorities (31 for the
  expired or exhausted contracts), so that the next validations do not read these contracts again.

Not implemented by the demo (steps of the data model): verification of the contract authenticator with the PSO
Verify Signature command of the SAM and SAM black list (traceability mode: the contracts loaded by the demo have no
authenticator), black list of the card applications, and stored value contracts (`ContractTariff` 3).

## Hardware Integration

### Tested Terminals

#### Standard Plugins (Open Source)

**Famoco FX205**
- **Readers**: NFC reader and SAM reader
- **Plugins**: [Famoco Plugin](https://github.com/calypsonet/keyple-famoco) + [Android NFC](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)

#### Proprietary Plugins (On Request)

**Bluebird EF501**
- **Plugin**: [Bluebird Plugin](https://github.com/calypsonet/keyple-plugin-cna-bluebird-specific-nfc-java-lib)
- **Access**: Contact [CNA](https://calypsonet.org/contact-us/) for plugin

**Arrive Axio 4**
- **Plugin**: [Arrive Plugin](https://github.com/calypsonet/keyple-plugin-cna-arrive-android-jvm-lib)
- **Feedback**: LEDs and sounds of the terminal through the Parkeon SDK (`AndroidParkeonCommon-release.aar`)
- **Access**: Contact [CNA](https://calypsonet.org/contact-us/) for plugin

## Development

### Project Structure

```
validation/app/
├── src/main/
│   ├── kotlin/org/calypsonet/keyple/demo/validation/
│   │   ├── data/                        # Data layer implementations
│   │   ├── di/                          # Dependency injection (Hilt modules)
│   │   ├── domain/                      # Business logic layer
│   │   │   ├── model/                   # Domain models
│   │   │   ├── procedures/              # Validation procedures (Calypso, Storage)
│   │   │   ├── spi/                     # Ports
│   │   │   └── usecases/                # Use cases invoked by the UI
│   │   └── ui/                          # UI layer
│   │       └── activities/              # Android activities
│   │           └── terminalselection/   # Terminal selection screen
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

# Unit tests (business rules and card procedures)
./gradlew testDebugUnitTest
```

## Troubleshooting

**"SAM not detected"**
- Verify SAM is properly inserted in reader
- Check SAM compatibility with cards (Test vs Production keys)
- Ensure SAM reader is powered and connected

**"Card validation failed"**
- Check card has valid contracts loaded via Reload Demo
- Verify card AID is supported by application
- Ensure contract validity dates are current
- Check trips left for validation

**"NFC detection not working"**
- Enable NFC in Android system settings
- Grant NFC permissions to application
- Keep device stable during card reading
- Try different card positioning on NFC antenna

**"Plugin initialization failed"**
- Ensure proper plugin libraries are included
- Check hardware drivers are installed
- Verify that the selected terminal type matches the actual hardware

## Proprietary Plugins

### Activation Process

By default, the proprietary plugins are replaced by mocks, and their terminal types are grayed out. To enable:

1. **Request Access**: Contact [CNA](https://calypsonet.org/contact-us/) for desired plugin
2. **Install Plugin**: Copy the provided `.aar` file to the `libs` directory at the root of the repository: the build
   uses it instead of the corresponding `-mock.aar` file
3. **Arrive Feedback**: For the LEDs and sounds of the Arrive terminal, also copy the Parkeon SDK
   (`AndroidParkeonCommon-release.aar`) to the `libs` directory: the build then uses the `arrive` source set instead
   of `arrive-mock`
4. **Build Project**: Execute `./gradlew assembleDebug`

### Available Proprietary Plugins

- **Bluebird EF501**
- **Arrive Axio 4**

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