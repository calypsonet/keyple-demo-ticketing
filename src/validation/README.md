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
boarding, etc.). It validates contracts loaded by the [Sales Demo](../sales/) and creates validation events
that can be later verified by the [Inspection Demo](../inspection/).

**Role in ecosystem**: Second step in the ticketing workflow, which validates loaded contracts and grants or denies
access to transportation networks.

## Prerequisites

### Hardware requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- **SAM (Secure Access Module)** for the Calypso cards, matching the keys of the cards (test SAM for the demo cards)
- Compatible terminal hardware (see [tested terminals](#tested-terminals))

### Card requirements
- Cards personalized and loaded using the [Sales Demo](../sales/)
- Calypso cards, or storage cards (MIFARE Ultralight and MIFARE Classic 1K on the Bluebird and Famoco terminals, ST25
  SRT512 on the Bluebird terminals only), with the storage card libraries (see
  [Proprietary plugins](#proprietary-plugins) and the
  [storage cards supported](../../docs/functional-documentation/index.adoc#storage-card-support))
- Supported AID (see [Card data model](../../docs/functional-documentation/index.adoc#supported-card-applications))

## Installation

### Download
1. Visit [Releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Download latest `kdt-validation-app-YY.MM.DD-debug.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK file

### Build from source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/validation
./gradlew assembleDebug
```

## Configuration

### Terminal selection
1. Launch application
2. Select the terminal type from **Terminal selection** screen:
  - **Famoco**: terminal with a SAM reader
  - **Arrive**, **Bluebird**: proprietary plugins (grayed out by default, see
    [Proprietary plugins](#proprietary-plugins))

### Validation settings
The **Settings** screen, displayed after the terminal selection, configures the validation:
- **Battery-powered mode**: for the portable terminals, which display the Home screen before each validation
- **Location**: location of the terminal, written in the validation event and checked by the inspection
- **Time and date**: shortcut to the date and time settings of Android

## Usage

### Standard operation flow

#### Battery-powered mode (portable terminals)
```
Terminal selection → Settings → Home → Card reader → Validation result
```

#### Fixed terminal mode
```
Terminal selection → Settings → Card reader → Validation result
```

### Detailed screen guide

**Terminal selection (`TerminalSelectionActivity`)**
- Choose appropriate hardware plugin
- Proprietary plugins require additional setup (see [Proprietary plugins](#proprietary-plugins))

**Settings (`SettingsActivity`)**
- Battery-powered mode, location of the terminal, access to the date and time settings

**Home (`HomeActivity`)** _(Battery-powered mode only)_
- Start of the validation

**Card reader (`CardReaderActivity`)**
- Waits for a card ("Please present your contactless card for validation")
- Selects the card and runs the validation procedure supporting it

**Validation result**

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

## Technical architecture

### Validation procedure

The steps of the validation procedure are described in the annex
[Card procedures](../../docs/functional-documentation/index.adoc#validation-procedure) of the Functional
Documentation, with the anti-passback rule and the steps not implemented by the demo. They are implemented by:
- `CalypsoCardValidationProcedure`: Calypso cards (1 to 4 contracts depending on the card product), in a secure session
  with the `DEBIT` access level, authenticated by the SAM;
- `StorageCardValidationProcedure`: storage cards (single contract), read and written without SAM nor secure
  session (the MIFARE Classic cards requiring the authentication of their sector with the key A).

The comments of these classes reference the step numbers of the annex.

### Key classes

**CardReaderSession** (`domain/CardReaderSession.kt`)
- Manages card and SAM reader lifecycle, and the user feedback devices
- Schedules card selection scenarios for supported cards, and selects the presented card

**ValidateCard** (`domain/usecases/ValidateCard.kt`)
- Validates the presented card: selection, execution of the validation procedure supporting it, and user feedback of
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
- Direct reading and writing, without secure session (authentication of the sector of the MIFARE Classic cards)
- Single contract processing per card

## Hardware integration

### Tested terminals

#### Standard plugins (open source)

**Famoco FX205**
- **Readers**: NFC reader and SAM reader
- **Plugins**: [Android NFC](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)
  for the contactless cards (Calypso cards, and storage cards with the storage card libraries), and
  [Famoco Plugin](https://github.com/calypsonet/keyple-famoco) for the SAM

#### Proprietary plugins (on request)

**Bluebird EF501**
- **Plugin**: [Bluebird Plugin](https://github.com/calypsonet/keyple-plugin-cna-bluebird-specific-nfc-java-lib)
- **Access**: Contact [CNA](https://calypsonet.org/contact-us/) for plugin

**Arrive Axio 4**
- **Plugin**: [Arrive Plugin](https://github.com/calypsonet/keyple-plugin-cna-arrive-android-jvm-lib)
- **Feedback**: LEDs and sounds of the terminal through the Parkeon SDK (`AndroidParkeonCommon-release.aar`)
- **Access**: Contact [CNA](https://calypsonet.org/contact-us/) for plugin

### Proprietary plugins

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

### Project structure

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

### Building and testing

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

**"Card rejected" or "Please load a ticket."**
- The reason displayed with the result is one of the
  [validation rejections and errors](../../docs/functional-documentation/index.adoc#validation-rejection-conditions)
  of the Functional Documentation

**"Selection error: …" or "Invalid card / File structure … not supported"**
- The card is not recognized (see the
  [validation rejections and errors](../../docs/functional-documentation/index.adoc#validation-rejection-conditions)
  in the Functional Documentation): it does not have the
  [supported card application](../../docs/functional-documentation/index.adoc#supported-card-applications), its DF
  name does not match the AID, or its file structure is not supported
- A storage card is not recognized when the storage card libraries are not available (see
  [Proprietary plugins](#proprietary-plugins))

## Contributing

See the [Contributing](../../README.md#contributing) section of the main README. Specific to this application: validate
the changes with Calypso cards and storage cards, on the supported terminal types.

## Related documentation

- [Main Project Overview](../../README.md)
- [Functional Documentation](../../docs/functional-documentation/index.adoc): Use cases and business rules
- [Common Library](../common/README.md): Data structures and constants
- [Sales Demo](../sales/README.md): Contract loading process
- [Inspection Demo](../inspection/README.md): Post-validation verification
- [Eclipse Keyple Documentation](https://keyple.org)

## License

This validation application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.