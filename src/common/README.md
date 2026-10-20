# Keyple Demo Common Library

[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../LICENSE)

[⬅️ Back to Main Project](../../README.md)

The shared foundation for all Keyple Demo applications, providing standardized data structures, constants, and utilities
for building interoperable ticketing applications.

## Overview

This library defines the common elements used across the Keyple Demo ecosystem:
- `model`: data model structures of the card (EnvironmentHolderStructure, EventStructure, ContractStructure), and
  `model/type`: types of their fields (priority codes, compact date and time, version number)
- `codecs`: record codecs (decoding and encoding of the card records)
- `constants`: organization of the card (application identifiers, files, blocks, key identifiers) and PKI test
  certificates
- `dto`: protocol of the remote services between the clients and the server (service identifiers, input and
  output data, status codes)
- `reference`: reference data of the network (locations)

## Used by

- [Keyple Sales Demo](../sales)
- [Keyple Validation Demo](../validation)
- [Keyple Inspection Demo](../inspection)

## Installation

This library is included in the builds of the applications using it (Gradle included build, `includeBuild` in their
`settings.gradle.kts`): it is built with them and does not need to be installed.

## Data model

The data model of the card (structures of the records, types of their fields, priority codes, and supported card
applications) is specified in the annex
[Card data model](../../docs/functional-documentation/index.adoc#card-data-model) of the Functional Documentation.
This library implements it:

| Class                        | Content                                                                                      |
|------------------------------|----------------------------------------------------------------------------------------------|
| `EnvironmentHolderStructure` | Environment and holder record                                                                |
| `EventStructure`             | Event record, with the priorities of the contracts                                           |
| `ContractStructure`          | Contract record, with its counter (`counterValue`, null for a contract without counter)      |
| `model/type`                 | `DateCompact`, `TimeCompact`, `VersionNumber`, and `PriorityCode` (code 3 read as `UNKNOWN`) |

## Record codecs

The records of the card are decoded and encoded by codecs implementing `RecordCodec<T>` (`decode` and `encode`
methods), one per structure and card technology:

| Structure                    | Calypso cards (29-byte records)  | Storage cards (16-byte records)     |
|------------------------------|----------------------------------|-------------------------------------|
| `EnvironmentHolderStructure` | `CalypsoEnvironmentHolderCodec`  | `StorageCardEnvironmentHolderCodec` |
| `EventStructure`             | `CalypsoEventCodec`              | `StorageCardEventCodec`             |
| `ContractStructure`          | `CalypsoContractCodec`           | `StorageCardContractCodec`          |

The counter of a contract is stored in a separate file on the Calypso cards, and in the contract record on the storage
cards.

## Card constants

| Object                | Content                                                                          |
|-----------------------|----------------------------------------------------------------------------------|
| `CalypsoAids`         | AIDs of the Calypso applications, and the DF name check (`matches`)              |
| `CalypsoFiles`        | SFIs and record sizes of the Calypso files, supported file structures            |
| `DefaultKifs`         | KIFs of the SAM keys used by the secure sessions                                 |
| `StorageCardBlocks`   | Layout of the storage cards with 4-byte blocks (MIFARE Ultralight, ST25 SRT512)  |
| `MifareClassicBlocks` | Layout of the MIFARE Classic 1K cards (sector 1)                                 |
| `PkiTestCertificates` | Test certificates of the PKI                                                     |

## Usage examples

### Environment structure creation

```kotlin
val env = EnvironmentHolderStructure(
    envVersionNumber = VersionNumber.CURRENT_VERSION,
    envApplicationNumber = generateUniqueNumber(),
    envIssuingDate = DateCompact(LocalDate.now()),
    envEndDate = DateCompact(LocalDate.now().withDayOfMonth(1).plusYears(6)),
    holderCompany = null,
    holderIdNumber = null
)
```

### Event creation

```kotlin
val event = EventStructure(
    eventVersionNumber = VersionNumber.CURRENT_VERSION,
    eventDateStamp = DateCompact(LocalDate.now()),
    eventTimeStamp = TimeCompact(LocalDateTime.now()),
    eventLocation = validatorLocationId,
    eventContractUsed = selectedContractIndex,
    contractPriorities = List(EventStructure.CONTRACT_COUNT) { PriorityCode.FORBIDDEN }
)
```

The structures are immutable: a modified structure is a copy, for example,
`event.withContractPriority(1, PriorityCode.EXPIRED)` or `contract.withCounterValue(10)`.

### Working with compact date/time

```kotlin
// Create compact date from LocalDate
val dateCompact = DateCompact(LocalDate.now())
val dateValue: Int = dateCompact.value

// Convert back to LocalDate
val localDate: LocalDate = dateCompact.date

// Create compact time from LocalDateTime
val timeCompact = TimeCompact(LocalDateTime.now())
val timeValue: Int = timeCompact.value
```

## Common procedures

The procedures using these structures, the personalization of the cards and the loading of the contracts, are executed
by the server of the sales demo. The steps of all the procedures (personalization, loading, validation, and
inspection) are described in the annex
[Card procedures](../../docs/functional-documentation/index.adoc#card-procedures) of the Functional Documentation.

## Version compatibility

The library is not published on its own: it is versioned and released with the demo applications (`project` version
of the `libs.versions.toml` version catalog, `99.99.99`, replaced by the release tag in the YY.MM.DD format), and uses
the Keyple components of the same Keyple Java BOM (`keypleJavaBom` version of the catalog). It targets Java 17, as the
applications using it.

## Contributing

When contributing to this library, ensure:
- All data structures maintain backward compatibility
- The library is versioned with the demo applications (see [Version compatibility](#version-compatibility))
- Update all demo applications when making breaking changes
- Include comprehensive unit tests for new utilities

## License

This library is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.