# Keyple Inspection Demo

[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../LICENSE)

Android inspection terminal application, checking after their validation the cards of the passengers. This application
represents the last step of the ticketing workflow: it displays the contracts of the card and indicates which one has
been validated at the location of the inspection within the validation period.

[⬅️ Back to Main Project](../../README.md)

## Overview

This Android application simulates the terminals used by the inspectors of a transport network to check that the
passengers have validated their trip. It analyzes the validation events written by the
[Validation Demo](../validation/) and the contracts loaded by the [Reload Demo](../reload-remote/).

**Role in Ecosystem**: Last step of the ticketing workflow - checks the cards after their validation.

**Inspection Purpose**: Check that the last validation used a contract of the card, at the location of the inspection
and within the validation period.

## Prerequisites

### Hardware Requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- **SAM (Secure Access Module)** for the authentication of the Calypso cards (optional)
- Compatible terminal hardware (see [tested terminals](#tested-terminals))

### Card Requirements
- Cards personalized and loaded with the [Reload Demo](../reload-remote/)
- Cards validated with the [Validation Demo](../validation/), to display a validated contract
- Supported AIDs (see [Common Library](../common/README.md#supported-card-applications))

## Installation

### Download APK
1. Visit [Releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Download latest `kdt-inspection-app-X.Y.Z-debug.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK file

### Build from Source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/inspection  
./gradlew assembleDebug
```

## Configuration

### Terminal Selection
1. Launch application
2. Select the terminal type from **Terminal Selection** screen:
  - **Famoco**: terminal with a SAM reader
  - **NFC terminal**: any Android device with NFC (no SAM: the Calypso cards are authenticated by PKI when they
    support it, otherwise not authenticated); the MIFARE Ultralight and MIFARE Classic storage cards are also
    supported (with the storage card libraries, see [Proprietary Plugins](#proprietary-plugins))
  - **Bluebird**: proprietary plugin (grayed out by default, see [Proprietary Plugins](#proprietary-plugins))

### Inspection Settings
The **Settings** screen, displayed after the terminal selection, configures the inspection:
- **Location**: location of the inspection, compared with the location of the last validation
- **Validation period** (minutes): period after the validation during which the validated contract is considered
  as used for the current trip
- **Time and date**: shortcut to the date and time settings of Android

## Usage

### Standard Inspection Flow

```
Terminal Selection → Settings → Home → Card Reader → Card Content or Invalid Card
```

### Detailed Screen Guide

**Terminal Selection (`TerminalSelectionActivity`)**
- Choice of the terminal type, which selects the reader plugins
- Proprietary plugins require additional setup (see [Proprietary Plugins](#proprietary-plugins))

**Settings (`SettingsActivity`)**
- Location of the inspection, validation period, access to the date and time settings

**Home (`HomeActivity`)**
- Start of the inspection

**Card Reader (`CardReaderActivity`)**
- Waits for a card ("Please present a contactless card for inspection")
- Selects the card and runs the inspection procedure supporting it
- Displays the authentication mode of the card (by the SAM, by PKI, or not authenticated)
- Displays the result of the inspection above the screen, as the validation app does, until "New inspection" (or
  back) is pressed, the next card being then detected:
  - **Card content**:
    - **Last validation**: date, time and location of the last validation, and contract used
    - **Contracts**: list of the contracts of the card, with their validity period (season pass) or their number
      of remaining trips (multi-trip ticket); the contract used by the last validation is marked as validated when
      the validation took place at the location of the inspection, the same day and within the validation period
    - "No ticket" when the card has never been loaded
  - **Invalid card**: reason why the card is rejected (see [Rejection Reasons](#rejection-reasons)) or error message

### Rejection Reasons

The reasons why a card is rejected, with the messages displayed, are described in the
[Functional Documentation](../../docs/functional-documentation/index.adoc#inspection-rejection-conditions).

## Technical Architecture

### Supported Card Types

#### Calypso Cards
**Inspection Procedure**:
1. **Selection**: selection of the application by its AID
2. **Authentication**: secure session with the `DEBIT` access level when a SAM is available, PKI secure session when
   the card supports it, otherwise reading without authentication
3. **Environment Analysis**: version and end date of the environment
4. **Event Analysis**: version, date, time and location of the last event, and contract used
5. **Contract Analysis**: reading of the contracts and of the counters, expiration of each contract, contract
   validated by the last event
6. **Result**: content of the card (`InspectionResult.CardContent`) or reason of the rejection
- **Not implemented** (steps of the data model): verification of the contract authenticator with the PSO Verify
  Signature command of the SAM and SAM black list (the contracts loaded by the demo have no authenticator)

#### Storage Cards
**Inspection Procedure** (MIFARE Ultralight, ST25 SRT512, MIFARE Classic):
1. **Reading**: reading of the environment, of the event and of the contract, without cryptographic operation
2. **Analysis**: same checks as for the Calypso cards, on the single contract of the card
- **Note**: Production implementations should add security measures

### Key Classes

**CardReaderSession** (`domain/CardReaderSession.kt`)
- **Purpose**: Manages the readers of the terminal
- **Responsibilities**:
  - Plugin, reader and SAM initialization
  - Card detection and selection
- **Integration**: Works with both SAM-enabled and SAM-less configurations

**InspectCard** (`domain/usecases/InspectCard.kt`)
- **Purpose**: Inspects the presented card
- **Responsibilities**: Selection of the card, execution of the inspection procedure supporting it
  (`CalypsoCardInspectionProcedure`, `StorageCardInspectionProcedure`) and user feedback of the result
  (`InspectionResult`)

**TerminalReaders** (`domain/spi/TerminalReaders.kt`)
- Port giving access to the readers of the terminal
- Registers the reader plugins of the terminal type, then provides the card reader and the SAM reader
- Implementation: `KeypleTerminalReaders`

**BaseInspectionProcedure** (`domain/procedures/BaseInspectionProcedure.kt`)
- Business rules common to the inspection procedures: validity of the last validation (location, day and validation
  period compared to the minute), validation event displayed

**CalypsoCardInspectionProcedure** (`domain/procedures/CalypsoCardInspectionProcedure.kt`)
- Inspection procedure of the Calypso cards
- Secure session when a SAM is available (or PKI secure session), analysis of the environment, of the last event and
  of the contracts

**StorageCardInspectionProcedure** (`domain/procedures/StorageCardInspectionProcedure.kt`)
- Inspection procedure of the storage cards (MIFARE Ultralight, ST25 SRT512, MIFARE Classic)
- Direct reading of the contract and of the event, without cryptographic operation

## Hardware Integration

### Tested Terminals

#### Standard Plugins (Open Source)

**Famoco FX205**
- **Configuration**: NFC reader and SAM reader
- **Plugins**: [Famoco Plugin](https://github.com/calypsonet/keyple-famoco) + [Android NFC](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)

**NFC Terminal (any Android device with NFC)**
- **Plugin**: [Android NFC Plugin](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)
- **Limitations**: No SAM (Calypso cards authenticated by PKI when they support it)
- **Use Case**: Development and testing

#### Proprietary Plugins (On Request)

**Bluebird EF501**
- **Plugin**: [Bluebird Plugin](https://github.com/calypsonet/keyple-plugin-cna-bluebird-specific-nfc-java-lib)
- **Access**: Contact [CNA](https://calypsonet.org/contact-us/) for plugin

## Development

### Project Structure

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

### Building and Testing

```bash
# Clean project
./gradlew clean

# Build debug version
./gradlew assembleDebug

# Unit tests (business rules and card procedures)
./gradlew testDebugUnitTest
```

## Troubleshooting

**No contract marked as validated**
- The card has not been validated, or the last validation used another contract
- The location of the validation differs from the location of the inspection
- The validation did not take place the same day, or is older than the validation period

**"SAM not available"**
- Check that the SAM is properly inserted in the SAM reader of the terminal
- Check that the SAM matches the keys of the cards (test SAM for the demo cards)
- Restart the application to initialize the SAM reader again

**Card reading intermittent**
- Ensure the card is held steady during the reading
- Check for interference from other NFC devices

**Terminal type not available**
- Confirm that the selected terminal type matches the actual hardware
- Proprietary terminals require their plugin (see [Proprietary Plugins](#proprietary-plugins))

## Proprietary Plugins

By default, the proprietary plugins and libraries are replaced by mocks: their terminal types are grayed out, and the
storage cards are not supported.

**Activation Process**:
1. **Request Plugin**: Contact [CNA](https://calypsonet.org/contact-us/) for the desired proprietary plugin
2. **Install Plugin**: Copy the provided library (`.aar` or `.jar` file) to the `libs` directory at the root of the
   repository: the build uses it instead of the corresponding mock (`-mock.aar` or `-mock.jar` file)
3. **Rebuild Application**: Execute `./gradlew assembleDebug`

**Available Proprietary Plugins**:
- **Bluebird EF501**
- **Storage cards**: storage card extension (`keyple-card-cna-storagecard-java-lib`) and reader plugin
  (`keyple-plugin-cna-storagecard-java-lib`)

## Contributing

When contributing to this inspection application:

1. **Keep the inspection flow simple**: one presentation of the card, one result
2. **Test Inspection Scenarios**: Verify the results after validations at various locations and times
3. **Hardware Compatibility**: Test on supported terminal types
4. **Security Standards**: Maintain or enhance existing security measures
5. **Documentation Updates**: Keep this document consistent with the application

## Related Documentation

- [Main Project Overview](../../README.md)
- [Functional Documentation](../../docs/functional-documentation/index.adoc) - Use cases and business rules
- [Common Library](../common/README.md) - Data structures and constants
- [Validation Demo](../validation/README.md) - Previous step in workflow
- [Reload Demo](../reload-remote/README.md) - Contract loading procedures
- [Eclipse Keyple Documentation](https://keyple.org) - SDK reference

## License

This inspection application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.
