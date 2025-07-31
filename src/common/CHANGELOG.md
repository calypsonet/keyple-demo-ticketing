# Changelog

> ⚠️ Note: This changelog reflects changes made prior to the repository merge.
> It is no longer maintained and has been preserved for historical reference only.
> Future changes are tracked in the unified repository's [CHANGELOG.md](../../CHANGELOG.md) file.

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added
- Added parsers `SCEnvironmentHolderStructureParser`, `SCContractStructureParser` and `SCEventStructureParser` for 
  environment, contract, and event structures dedicated to storage cards.

## [2.0.2] - 2024-10-15
### Added
- New service IDs in `RemoteServiceId` for C# Keyple-less clients and Kotlin Multiplatform Keyple-less clients.
- Introduced `SelectAppAndAnalyzeContractsInputDto` and `SelectAppAndAnalyzeContractsOutputDto` for analyzing contracts.
- Introduced `SelectAppAndLoadContractInputDto` and `SelectAppAndLoadContractOutputDto` for loading contracts.
- Introduced `SelectAppAndPersonalizeCardInputDto` and `SelectAppAndPersonalizeCardOutputDto` for personalizing cards.
### Changed
- Renamed repository and artifact from `keyple-demo-common-lib` to `keyple-demo-ticketing-common-lib`
- Renamed the test class `ContractStructureParserTest` to `ContractInfoStructureParserTest` to reflect the updated class
  name.

## [2.0.1] - 2024-04-23
### Upgraded
- keyple-util-java-lib `2.3.0` -> `2.4.0`

## [2.0.0] - 2023-05-31
### Added
- Added two new services `SELECT_APP_AND_READ_CONTRACTS` and `SELECT_APP_AND_INCREASE_CONTRACT_COUNTER` to be used by
native applications using the Json API.
- Associated DTOs.
### Changed
- Renamed services to use verbs.

## [1.0.0] - 2022-11-18
This is the initial release.

[Unreleased]: https://github.com/calypsonet/keyple-demo-ticketing-common-lib/compare/2.0.2...HEAD
[2.0.2]: https://github.com/calypsonet/keyple-demo-ticketing-common-lib/compare/2.0.1...2.0.2
[2.0.1]: https://github.com/calypsonet/keyple-demo-ticketing-common-lib/compare/2.0.0...2.0.1
[2.0.0]: https://github.com/calypsonet/keyple-demo-ticketing-common-lib/compare/1.0.0...2.0.0
[1.0.0]: https://github.com/calypsonet/keyple-demo-ticketing-common-lib/releases/tag/1.0.0