# Keyple Inspection Demo

[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../LICENSE)

Android inspection terminal application, checking after their validation the cards of the passengers. This application
represents the last step of the ticketing workflow.

[⬅️ Back to Main Project](../../README.md)

## Overview

This Android application simulates the terminals used by the inspectors of a transport network to check that the
passengers have validated their trip. It analyzes the validation events written by the
[Validation Demo](../validation/) and the contracts loaded by the [Sales Demo](../sales/).

**Role in ecosystem**: Last step of the ticketing workflow, which checks the cards after their validation.

**Inspection rules**: see [Inspection business rules](../../docs/functional-documentation/index.adoc#inspection-checks)
in the Functional Documentation.

## Prerequisites

### Hardware requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- **SAM (Secure Access Module)** for the authentication of the Calypso cards (optional)
- Compatible terminal hardware (see [tested terminals](#tested-terminals))

### Card requirements
- Cards personalized and loaded with the [Sales Demo](../sales/)
- Cards validated with the [Validation Demo](../validation/), to display a validated contract
- Calypso cards, or storage cards with the storage card libraries (see [Proprietary plugins](#proprietary-plugins)),
  depending on the terminal (see the
  [storage cards supported](../../docs/functional-documentation/index.adoc#storage-card-support))
- Supported AIDs (see [Card data model](../../docs/functional-documentation/index.adoc#supported-card-applications))

## Installation

### Download
1. Visit [Releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Download latest `kdt-inspection-app-YY.MM.DD-debug.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK file

### Build from source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/inspection  
./gradlew assembleDebug
```

## Configuration

### Terminal selection
1. Launch application
2. Select the terminal type from **Terminal selection** screen:
  - **Famoco**: terminal with a SAM reader
  - **NFC terminal**: any Android device with NFC (no SAM: the Calypso cards are authenticated by PKI when they
    support it, otherwise not authenticated); the MIFARE Ultralight and MIFARE Classic storage cards are also
    supported (with the storage card libraries, see [Proprietary plugins](#proprietary-plugins))
  - **Bluebird**: proprietary plugin (grayed out by default, see [Proprietary plugins](#proprietary-plugins))

### Inspection settings
The **Settings** screen, displayed after the terminal selection, configures the inspection:
- **Location**: location of the inspection, compared with the location of the last validation
- **Validation period** (minutes): period after the validation during which the validated contract is considered
  as used for the current trip
- **Time and date**: shortcut to the date and time settings of Android

## Usage

### Standard inspection flow

```
Terminal selection → Settings → Home → Card reader → Card content or Invalid card
```

### Detailed screen guide

**Terminal selection (`TerminalSelectionActivity`)**
- Choice of the terminal type, which selects the reader plugins
- Proprietary plugins require additional setup (see [Proprietary plugins](#proprietary-plugins))

**Settings (`SettingsActivity`)**
- Location of the inspection, validation period, access to the date and time settings

**Home (`HomeActivity`)**
- Start of the inspection

**Card reader (`CardReaderActivity`)**
- Waits for a card ("Please present a contactless card for inspection")
- Selects the card and runs the inspection procedure supporting it
- Displays the authentication mode of the card (by the SAM, by PKI, or not authenticated)
- Displays the result of the inspection above the screen, as the validation app does, until "New inspection" (or
  back) is pressed, the next card being then detected:
  - **Card content**: last validation and contracts of the card, the contract used by the last validation being
    marked as validated when the rules of the inspection are met (see the Functional Documentation:
    [Inspection results](../../docs/functional-documentation/index.adoc#inspection-results),
    [Inspection business rules](../../docs/functional-documentation/index.adoc#inspection-checks))
  - **Invalid card**: reason why the card is rejected (see
    [Inspection rejection conditions](../../docs/functional-documentation/index.adoc#inspection-rejection-conditions)
    in the Functional Documentation) or error message

## Technical architecture

### Inspection procedure

The steps of the inspection procedure are described in the annex
[Card procedures](../../docs/functional-documentation/index.adoc#inspection-procedure) of the Functional
Documentation, with the authentication modes and the steps not implemented by the demo. They are implemented by:
- `CalypsoCardInspectionProcedure`: Calypso cards, in a secure session with the `DEBIT` access level when a SAM is
  available, in a PKI secure session when the card supports it, otherwise without authentication;
- `StorageCardInspectionProcedure`: storage cards (single contract), read without authentication of the card (the
  MIFARE Classic cards requiring the authentication of their sector with the key A).

The comments of these classes reference the step numbers of the annex.

### Key classes

**CardReaderSession** (`domain/CardReaderSession.kt`)
- Manages the readers of the terminal, the SAM being optional: without SAM, the Calypso cards are authenticated by PKI
  when they support it, otherwise read without authentication

**InspectCard** (`domain/usecases/InspectCard.kt`)
- **Purpose**: Inspects the presented card
- **Responsibilities**: Selection of the card, execution of the inspection procedure supporting it
  (`CalypsoCardInspectionProcedure`, `StorageCardInspectionProcedure`) and user feedback of the result
  (`InspectionResult`)

**BaseInspectionProcedure** (`domain/procedures/BaseInspectionProcedure.kt`)
- Business rules common to the inspection procedures: validity of the last validation (location, day, and validation
  period compared to the minute), validation event displayed

**CalypsoCardInspectionProcedure** (`domain/procedures/CalypsoCardInspectionProcedure.kt`)
- Inspection procedure of the Calypso cards
- Secure session when a SAM is available (or PKI secure session), analysis of the environment, of the last event, and
  of the contracts

**StorageCardInspectionProcedure** (`domain/procedures/StorageCardInspectionProcedure.kt`)
- Inspection procedure of the storage cards (MIFARE Ultralight, ST25 SRT512, MIFARE Classic)
- Direct reading of the environment, of the event, and of the contract, without authentication of the card

## Hardware integration

### Tested terminals

#### Standard plugins (open source)

**Famoco FX205**
- **Configuration**: NFC reader and SAM reader
- **Plugins**: [Android NFC](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)
  for the contactless cards (Calypso cards, and storage cards with the storage card libraries), and
  [Famoco Plugin](https://github.com/calypsonet/keyple-famoco) for the SAM

**NFC terminal (any Android device with NFC)**
- **Plugin**: [Android NFC Plugin](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)
- **Limitations**: No SAM (Calypso cards authenticated by PKI when they support it)
- **Use case**: Development and testing

#### Proprietary plugins (on request)

**Bluebird EF501**
- **Plugin**: [Bluebird Plugin](https://github.com/calypsonet/keyple-plugin-cna-bluebird-specific-nfc-java-lib)
- **Access**: Contact [CNA](https://calypsonet.org/contact-us/) for plugin

### Proprietary plugins

The proprietary components used by this application are available on request and replaced by mocks by default (their
terminal types being then grayed out, and the storage cards not supported). They are installed as described in the
[main README](../../README.md#proprietary-plugins-available-on-request):

- **Bluebird EF501**
- **Storage cards**: storage card extension (`keyple-card-cna-storagecard-java-lib`) and reader plugin
  (`keyple-plugin-cna-storagecard-java-lib`)

## Development

### Project structure

```
inspection/app/
├── src/main/
│   ├── kotlin/org/calypsonet/keyple/demo/inspection/
│   │   ├── data/                            # Adapters (readers, Keypop API, sounds, settings, logging)
│   │   ├── di/                              # Dependency injection (Hilt modules)
│   │   ├── domain/                          # Business logic
│   │   │   ├── builders/                    # Builders of the domain models displayed by the inspection
│   │   │   ├── model/                       # Domain models
│   │   │   ├── procedures/                  # Inspection procedures (Calypso, Storage)
│   │   │   ├── spi/                         # Ports
│   │   │   └── usecases/                    # Use cases invoked by the UI
│   │   └── ui/                              # UI layer
│   │       └── activities/                  # Android activities
│   │           ├── cardcontent/             # Lists of the card content (contracts, last validation)
│   │           └── terminalselection/       # Terminal selection screen
│   ├── res/                                 # Android resources
│   └── AndroidManifest.xml                  # Application configuration
├── build.gradle.kts                         # Build configuration (Kotlin DSL)
└── proguard-rules.pro                       # Code obfuscation rules
```

### Building and testing

```bash
# Clean project
./gradlew clean

# Build debug version
./gradlew assembleDebug

# Unit tests (business rules and card procedures)
./gradlew testDebugUnitTest
```

## Troubleshooting

The common issues of the Android applications (NFC, SAM, terminal type) are described in the
[main README](../../README.md#troubleshooting).

**"Not valid on this network"**
- The reason displayed below the title is one of the
  [inspection rejection conditions](../../docs/functional-documentation/index.adoc#inspection-rejection-conditions)
  of the Functional Documentation
- A storage card is not recognized when the storage card libraries are not available (see
  [Proprietary plugins](#proprietary-plugins))

**No contract marked as validated**
- The last validation does not meet the rules of the inspection (location, day, validation period, contract used),
  described in [Inspection business rules](../../docs/functional-documentation/index.adoc#inspection-checks) in the
  Functional Documentation: check the location and the validation period set in the **Settings** screen

## Contributing

See the [Contributing](../../README.md#contributing) section of the main README. Specific to this application: keep one
presentation of the card for one result, and verify the results after validations at various locations and times.

## Related documentation

- [Main Project Overview](../../README.md)
- [Functional Documentation](../../docs/functional-documentation/index.adoc): Use cases and business rules
- [Common Library](../common/README.md): Data structures and constants
- [Validation Demo](../validation/README.md): Previous step in workflow
- [Sales Demo](../sales/README.md): Contract loading procedures
- [Eclipse Keyple Documentation](https://keyple.org): SDK reference

## License

This inspection application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.
