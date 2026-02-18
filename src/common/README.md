# Keyple Demo Common Library

[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../LICENSE)

The shared foundation for all Keyple Demo applications, providing standardized data structures, constants, and utilities
for building interoperable ticketing applications.

## Overview

This library defines the common elements used across the Keyple Demo ecosystem:
- Data model structures (EnvironmentHolderStructure, EventStructure, ContractStructure)
- Structure parser utilities
- Priority codes and enumeration types
- Date/time compact format types
- Card application identifiers

## Used By

- [Keyple Reload Demo](../reloading-remote)
- [Keyple Validation Demo](../validation)
- [Keyple Control Demo](../control)

## Installation

This library is automatically referenced by the various demos.

## Data Structures

### Environment/Holder Structure

Stores card metadata and holder information.

| Field Name           | Bits | Description                                        |     Type      |  Status   |
|:---------------------|-----:|:---------------------------------------------------|:-------------:|:---------:|
| EnvVersionNumber     |    8 | Data structure version number                      | VersionNumber | Mandatory | 
| EnvApplicationNumber |   32 | Card application number (unique system identifier) |      Int      | Mandatory |
| EnvIssuingDate       |   16 | Card application issuing date                      |  DateCompact  | Mandatory | 
| EnvEndDate           |   16 | Card application expiration date                   |  DateCompact  | Mandatory | 
| HolderCompany        |    8 | Holder company                                     |      Int      | Optional  | 
| HolderIdNumber       |   32 | Holder Identifier within HolderCompany             |      Int      | Optional  | 
| EnvPadding           |  120 | Padding (bits to 0)                                |    Binary     | Optional  | 

### Event Structure

Records validation events and transaction history.

| Field Name         | Bits | Description                                   |     Type      |  Status   |
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

### Contract Structure

Defines transportation titles and their properties.

| Field Name              | Bits | Description                          |        Type         |  Status   |
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

### Counter Field

The `ContractStructure` includes a `counterValue` field for tracking usage:

| Field Name   | Bits | Description     | Type |  Status  |
|:-------------|-----:|:----------------|:----:|:---------|
| counterValue |   24 | Number of trips | Int  | Optional | 

## Data Types

### Base Types

| Name          | Bits | Description                                                                                                                                                        |
|:--------------|-----:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DateCompact   |   16 | Number of days since January 1st, 2010 (being date 0). Maximum value is 16,383, last complete year being 2053. All dates are in legal local time.                  |
| TimeCompact   |   16 | Time in minutes, value = hour*60+minute (0 to 1,439)                                                                                                               |    
| VersionNumber |    8 | Data model version:<br>0 Forbidden (undefined)<br>1 Current version<br>2..254 RFU<br>255 Forbidden (reserved)                                                      |

### Priority Codes

Contract types and status indicators used throughout the system:

| Code | Name         | Description                    | Usage                           |
|-----:|:-------------|:-------------------------------|:--------------------------------|
|    0 | FORBIDDEN    | Prohibited usage               | Clean records only              |
|    1 | SEASON_PASS  | Unlimited travel period        | Highest priority validation     |
|    2 | MULTI_TRIP   | Count-based ticket             | Decremented per journey         |
|    3 | STORED_VALUE | Monetary value storage         | Decremented by fare amount      |
| 4-30 | RFU          | Reserved for future use        | -                               |
|   31 | EXPIRED      | Contract has expired           | Automatically set by system     |

## Supported Card Applications

The demos work with cards from the [CNA Test Kit](https://calypsonet.org/technical-support-documentation/) containing these application identifiers:

| AID                  | Description                    | Compatibility          |
|:---------------------|:-------------------------------|:-----------------------|
| A000000291FF9101     | Keyple Generic test card       | All demo applications  |
| 315449432E49434131   | CD Light/GTML Compatibility    | Legacy system support  |
| 315449432E49434133   | Calypso Light                  | Standard Calypso       |
| A0000004040125090101 | Navigo IDF                     | Paris transport system |

## Common Procedures

### Card Personalization Process

Prepares cards for use by initializing data structures:

1. **Application Selection** using configured AID
2. **Environment Setup**:
    - Set `EnvVersionNumber` = 1 (current version)
    - Assign unique `EnvApplicationNumber`
    - Set issuing and expiration dates
    - Clear holder information
3. **File Initialization**:
    - Clear event log (set to zeros)
    - Clear all contract records
    - Reset counter files
4. **Session Management**: Close secure session if applicable

### Contract Loading Process

Loads new transportation titles or extends existing contracts:

1. **Environment Validation**:
    - Verify version compatibility
    - Check card expiration status
2. **Contract Analysis**:
    - Identify available contract slots
    - Validate existing contract states
    - Determine priority assignments
3. **Contract Writing**:
    - Set contract metadata (type, dates, SAM info)
    - Update associated counters if applicable
    - Assign priority levels
4. **Event Logging**: Update priority information in event log

## Usage Examples

### Environment Structure Creation

```kotlin
val env = EnvironmentHolderStructure(
    envVersionNumber = VersionNumber.CURRENT_VERSION,
    envApplicationNumber = generateUniqueNumber(),
    envIssuingDate = DateCompact(LocalDate.now()),
    envEndDate = DateCompact(LocalDate.now().plusYears(6)),
    holderCompany = null,
    holderIdNumber = null
)
```

### Event Creation

```kotlin
val event = EventStructure(
    eventVersionNumber = VersionNumber.CURRENT_VERSION,
    eventDateStamp = DateCompact(LocalDate.now()),
    eventTimeStamp = TimeCompact(LocalDateTime.now()),
    eventLocation = validatorLocationId,
    eventContractUsed = selectedContractIndex,
    contractPriority1 = PriorityCode.FORBIDDEN,
    contractPriority2 = PriorityCode.FORBIDDEN,
    contractPriority3 = PriorityCode.FORBIDDEN,
    contractPriority4 = PriorityCode.FORBIDDEN
)
```

### Working with Compact Date/Time

```kotlin
// Create compact date from LocalDate
val dateCompact = DateCompact(LocalDate.now())
val dateValue: Int = dateCompact.value

// Convert back to LocalDate
val localDate: LocalDate = dateCompact.getDate()

// Create compact time from LocalDateTime
val timeCompact = TimeCompact(LocalDateTime.now())
val timeValue: Int = timeCompact.value
```

## Structure Parsers

### EnvironmentHolderStructureParser

- Parses binary data to `EnvironmentHolderStructure`
- Generates binary data from structure
- Implements `Parser<EnvironmentHolderStructure>` interface

### EventStructureParser

- Parses binary data to `EventStructure`
- Generates binary data from structure
- Implements `Parser<EventStructure>` interface

### ContractStructureParser

- Parses binary data to `ContractStructure`
- Generates binary data from structure
- Implements `Parser<ContractStructure>` interface

## Version Compatibility

| Library Version | Demo Applications | Keyple Middleware | Notes                    |
|:----------------|:------------------|:------------------|:-------------------------|
| 1.0.x           | 1.0.x             | 2.x               | Initial release          |
| 1.1.x           | 1.1.x             | 2.x               | Enhanced Storage Card    |
| 2.0.x           | 2.0.x             | 3.x               | Breaking changes         |

## Contributing

When contributing to this library, ensure:
- All data structures maintain backward compatibility
- Version numbers follow semantic versioning
- Update all demo applications when making breaking changes
- Include comprehensive unit tests for new utilities

## License

This library is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.