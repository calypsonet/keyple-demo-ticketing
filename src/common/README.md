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

## Data structures

### Environment/holder structure

Stores card metadata and holder information.

| Field name           | Bits | Description                                        |     Type      |  Status   |
|:---------------------|-----:|:---------------------------------------------------|:-------------:|:---------:|
| EnvVersionNumber     |    8 | Data structure version number                      | VersionNumber | Mandatory | 
| EnvApplicationNumber |   32 | Card application number (unique system identifier) |      Int      | Mandatory |
| EnvIssuingDate       |   16 | Card application issuing date                      |  DateCompact  | Mandatory | 
| EnvEndDate           |   16 | Card application expiration date                   |  DateCompact  | Mandatory | 
| HolderCompany        |    8 | Holder company                                     |      Int      | Optional  | 
| HolderIdNumber       |   32 | Holder Identifier within HolderCompany             |      Int      | Optional  | 
| EnvPadding           |  120 | Padding (bits to 0)                                |    Binary     | Optional  | 

### Event structure

Records validation events and transaction history.

| Field name         | Bits | Description                                   |     Type      |  Status   |
|:-------------------|-----:|:----------------------------------------------|:-------------:|:---------:|
| EventVersionNumber |    8 | Data structure version number                 | VersionNumber | Mandatory | 
| EventDateStamp     |   16 | Date of the event                             |  DateCompact  | Mandatory | 
| EventTimeStamp     |   16 | Time of the event                             |  TimeCompact  | Mandatory | 
| EventLocation      |   32 | Location identifier                           |      Int      | Mandatory | 
| EventContractUsed  |    8 | Index of the contract used for the validation |      Int      | Mandatory | 
| ContractPriority1  |    8 | Priority for contract #1                      | PriorityCode  | Mandatory | 
| ContractPriority2  |    8 | Priority for contract #2                      | PriorityCode  | Mandatory | 
| ContractPriority3  |    8 | Priority for contract #3                      | PriorityCode  | Mandatory | 
| ContractPriority4  |    8 | Priority for contract #4                      | PriorityCode  | Mandatory | 
| EventPadding       |  120 | Padding (bits to 0)                           |    Binary     | Optional  | 

### Contract structure

Defines the contracts (tickets) and their properties.

| Field name              | Bits | Description                          |        Type         |  Status   |
|:------------------------|-----:|:-------------------------------------|:-------------------:|:---------:|
| ContractVersionNumber   |    8 | Data structure version number        |    VersionNumber    | Mandatory | 
| ContractTariff          |    8 | Contract Type                        |    PriorityCode     | Mandatory | 
| ContractSaleDate        |   16 | Sale date of the contract            |     DateCompact     | Mandatory | 
| ContractValidityEndDate |   16 | Last day of validity of the contract |     DateCompact     | Mandatory | 
| ContractSaleSam         |   32 | SAM which loaded the contract        |         Int         | Optional  | 
| ContractSaleCounter     |   24 | SAM auth key counter value           |         Int         | Optional  | 
| ContractAuthKvc         |    8 | SAM auth key KVC                     |         Int         | Optional  | 
| ContractAuthenticator   |   24 | Security authenticator               | Authenticator (Int) | Optional  | 
| ContractPadding         |   96 | Padding (bits to 0)                  |       Binary        | Optional  | 

### Counter structure

Counts the trips left of a multi-trip ticket.

| Field name   | Bits | Description     | Type |  Status   |
|:-------------|-----:|:----------------|:----:|:---------:|
| CounterValue |   24 | Number of trips | Int  | Mandatory |

The counter of a contract is stored in a separate file on the Calypso cards (one counter per contract record), and in
the contract record on the storage cards. `ContractStructure` carries it in its `counterValue` field, null for a
contract without counter (season pass).

## Data types

### Base types

| Name          | Bits | Description                                                                                                                                                        |
|:--------------|-----:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DateCompact   |   16 | Number of days since January 1st, 2010 (being date 0). Maximum value is 16,383, last complete year being 2053. All dates are in legal local time.                  |
| TimeCompact   |   16 | Time in minutes, value = hour*60+minute (0 to 1,439)                                                                                                               |    
| VersionNumber |    8 | Data model version:<br>0 Forbidden (undefined)<br>1 Current version<br>2..254 RFU<br>255 Forbidden (reserved)                                                      |

### Priority codes

Contract types and status indicators of the event. The code 3 (stored value) is defined by the data model but not
supported by the demo: it is read as `UNKNOWN`. The use of the codes by the validation is described in the
[Functional Documentation](../../docs/functional-documentation/index.adoc#ticket-priority).

| Code | Name        | Description             |
|-----:|:------------|:------------------------|
|    0 | FORBIDDEN   | Prohibited usage        |
|    1 | SEASON_PASS | Unlimited travel period |
|    2 | MULTI_TRIP  | Count-based ticket      |
|    3 | -           | Stored value            |
| 4-30 | RFU         | Reserved for future use |
|   31 | EXPIRED     | Contract has expired    |

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

## Supported card applications

The demos work with cards from the [CNA Test Kit](https://calypsonet.org/technical-support-documentation/) containing these application identifiers:

| AID                  | Description                    | Compatibility          |
|:---------------------|:-------------------------------|:-----------------------|
| A000000291FF9101     | Keyple Generic test card       | All demo applications  |
| 315449432E49434131   | CD Light/GTML Compatibility    | Legacy system support  |
| 315449432E49434133   | Calypso Light                  | Standard Calypso       |
| A0000004040125090101 | Navigo IDF                     | Paris transport system |

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
by the server of the sales demo, as described in its
[README](../sales/README.md#card-personalization-and-contract-loading-procedures).

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