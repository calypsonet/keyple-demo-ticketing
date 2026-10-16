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
boarding, etc.). It validates contracts loaded by the [Reload Demo](../reload-remote/) and creates validation events
that can be later verified by the [Inspection Demo](../inspection/).

**Role in Ecosystem**: Second step in the ticketing workflow - validates loaded contracts and grants/denies access to
transportation networks.

## Prerequisites

### Hardware Requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- **SAM (Secure Access Module)** for the Calypso cards, matching the keys of the cards (test SAM for the demo cards)
- Compatible terminal hardware (see [tested terminals](#tested-terminals))

### Card Requirements
- Cards personalized and loaded using the [Reload Demo](../reload-remote/)
- Calypso cards, or storage cards (MIFARE Ultralight, ST25 SRT512, MIFARE Classic) with the storage card
  libraries (see [Proprietary Plugins](#proprietary-plugins))
- Supported AIDs (see [Common Library](../common/README.md#supported-card-applications))

## Installation

### Download
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

### Validation Settings
The **Settings** screen, displayed after the terminal selection, configures the validation:
- **Battery powered mode**: for the portable terminals, which display the Home screen before each validation
- **Location**: location of the terminal, written in the validation event and checked by the inspection
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

The content and the messages of each result are described in the
[Functional Documentation](../../docs/functional-documentation/index.adoc#validation-results). The overlay shows:
- **Accepted** (`ValidationResult.Accepted`): green background with the success animation
- **Invalid card** (`ValidationResult.Rejected`, card data rejected: environment, versions, already validated; or
  `ValidationResult.NotRecognized`): orange background with the error animation
- **No ticket** (`ValidationResult.Rejected`, no contract to use: no valid contract, expired contract, no trips left):
  red background with the error animation
- **Other errors** (`ValidationResult.CardRemoved`, `ValidationResult.Failed`): red background with the error
  animation
- The sounds and LEDs of the terminal signal the success or the failure

## Technical Architecture

### Supported Card Types

#### Calypso Cards
**Security Model**:
- Secure session with the `DEBIT` access level, authenticated by the SAM
- Support for multiple contracts (1-4 depending on card type)

**Processing Flow**: see the [Best Contract Search Algorithm](#best-contract-search-algorithm)

#### Storage Cards
**Simplified Model**:
- Reading and writing without cryptographic operation
- Single contract per card
- Streamlined validation procedure

**Processing Flow**:
1. Card detection and data reading
2. Contract validation and date checking
3. Counter updates for applicable contract types
4. Event creation and data writing

### Validation Procedure Details

#### Best Contract Search Algorithm

The contracts are processed in the order of their priorities, described in the
[Functional Documentation](../../docs/functional-documentation/index.adoc#ticket-priority):

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
6. Create new validation event, or write the updated priorities if the card is rejected
7. Close secure session
```

Additions to the procedure of the data model, described in the Functional Documentation:
- [Anti-passback](../../docs/functional-documentation/index.adoc#anti-passback-rule), with the ratification of a
  session interrupted by the removal of the card
- [Rejected card](../../docs/functional-documentation/index.adoc#rejected-card): priorities of the expired or
  exhausted contracts written on the card

Not implemented by the demo (steps of the data model): verification of the contract authenticator with the PSO
Verify Signature command of the SAM and SAM black list (traceability mode: the contracts loaded by the demo have no
authenticator), black list of the card applications, and stored value contracts (`ContractTariff` 3).

### Key Classes

**CardReaderSession** (`domain/CardReaderSession.kt`)
- Manages card and SAM reader lifecycle, and the user feedback devices
- Schedules card selection scenarios for supported cards, and selects the presented card

**ValidateCard** (`domain/usecases/ValidateCard.kt`)
- Validates the presented card: selection, execution of the validation procedure supporting it and user feedback of
  the result (`ValidationResult`)

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

### Proprietary Plugins

The proprietary components used by this application are available on request and replaced by mocks by default (their
terminal types being then grayed out, and the storage cards not supported). They are installed as described in the
[main README](../../README.md#proprietary-plugins-available-on-request):

- **Bluebird EF501**
- **Arrive Axio 4**: for the LEDs and sounds of the terminal, also copy the Parkeon SDK
  (`AndroidParkeonCommon-release.aar`) to the `libs` directory: the build then uses the `arrive` source set instead of
  `arrive-mock`
- **Storage cards**: storage card extension (`keyple-card-cna-storagecard-java-lib`) and reader plugin
  (`keyple-plugin-cna-storagecard-java-lib`)

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

The common issues of the Android applications (NFC, SAM, terminal type) are described in the
[main README](../../README.md#troubleshooting).

**"Card validation failed"**
- Check card has valid contracts loaded via Reload Demo
- Verify card AID is supported by application
- Ensure contract validity dates are current
- Check trips left for validation

## Contributing

See the [Contributing](../../README.md#contributing) section of the main README. Specific to this application: validate
the changes with Calypso cards and storage cards, on the supported terminal types.

## Related Documentation

- [Main Project Overview](../../README.md)
- [Functional Documentation](../../docs/functional-documentation/index.adoc) - Use cases and business rules
- [Common Library](../common/README.md) - Data structures and constants
- [Reload Demo](../reload-remote/README.md) - Contract loading process
- [Inspection Demo](../inspection/README.md) - Post-validation verification
- [Eclipse Keyple Documentation](https://keyple.org)

## License

This validation application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.