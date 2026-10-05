# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Changed
- Android applications: layered architecture with ports and adapters, documented in the README ("Android Application
  Architecture") and checked by the CI (`.github/scripts/check-android-architecture.sh`) for the domain, data and ui
  layers. The domain layer no longer
  depends on Android, Timber or the dependency injection framework: the domain services are provided by a Dagger
  `DomainModule`, and the card managers receive their dependencies through their constructor.
- Android applications: logging through the `Logger` port (same `d`/`i`/`w`/`e` interface in all applications) in the
  domain, data and di layers. Timber is only used by `LoggerImpl`, `Application` and the activities.
- Android applications: card processing is now finalized with `ObservableCardReader.finalizeCardProcessing()` after
  each card.
- Android applications: when the storage card extension is not available (e.g. mocked library), the storage cards are
  not supported and a warning is logged once, instead of failing.
- CI/CD workflows: GitHub actions `checkout`, `setup-java`, `setup-node` and `setup-dotnet` upgraded from v4 to v5, and
  .NET upgraded from 7.0 to 8.0.
### Upgraded
- `keyple-java-bom`: `2026.03.19` -> `2026.09.29`
- `keyple-card-cna-storagecard-java-lib` (mock): `2.3.0` -> `2.3.1`
- `keyple-plugin-cna-storagecard-java-lib` (mock): `1.1.0` -> `1.1.1`
### Removed
- Unused mocks: `keyple-card-cna-storagecard-java-lib-2.3.0` and `keyple-plugin-cna-storagecard-java-lib-1.1.0`.
- Stored value contract type (`PriorityCode.STORED_VALUE`, contract tariff `3`), which could not be created by the demo
  (neither loaded by the server nor issued at personalization) and was only partially handled (e.g. not debited when
  validating a Calypso card). The contract tariff `3` is now reserved for future use and handled as unknown.
### Common library
#### Added
- `RemoteServiceStatus` enum defining the status codes of the remote services (`statusCode` field of
  `AnalyzeContractsOutputDto`, `WriteContractOutputDto` and `CardIssuanceOutputDto`, still transmitted as an integer).
#### Changed
- `Location.toString()` now returns the name of the location.
- Documentation of the status codes of `AnalyzeContractsOutputDto`, `WriteContractOutputDto` and
  `CardIssuanceOutputDto` aligned with the codes actually returned by the server.
### Validation app
#### Added
- Anti-passback check for storage cards: a card presented again within the anti-passback delay is rejected.
#### Changed
- The user feedback (sounds, LEDs) is provided by the dedicated `UiManager` port instead of `ReaderManager`. The Arrive
  and Android implementations are renamed `ArriveFeedbackDevice` and `AndroidFeedbackDevice`.
- The settings are accessed through the `AppSettingsRepository` port instead of the global `AppSettings` object.
- The UI uses the domain models directly (UI models removed, as no object is passed between activities).
#### Removed
- Unused `CardSummaryActivity` and its layouts (replaced by the summary overlay of the reader screen).
#### Fixed
- Crash at startup when the storage card library is mocked.
- Calypso cards: contracts of unknown type (contract tariff not supported by the demo) are no longer validated (they
  were accepted without any debit).
### Control app
#### Changed
- Major architectural refactoring to apply the layered architecture with ports and adapters, isolating the business
  logic (domain) from the Android and UI implementations.
- The locations are provided by the `common` library (local `locations.json` file removed), and are displayed by their
  name only.
- The SAM selection no longer filters the SAM on its power-on data (SAM C1).
- The user feedback (sounds) is provided by the `UiManager` port, and the settings are accessed through the
  `AppSettingsRepository` port.
### Reloading remote Android client
#### Changed
- Major architectural refactoring to apply the layered architecture with ports and adapters: the plugins and readers
  management is moved from the activities to `ReaderManagerImpl`, and the remote services are called through the
  `RemoteServiceManager` port.
- The business logic is moved from the activities to the domain: card selection (AIDs depending on the device),
  interpretation of the server status codes, building of the card titles and check that the reloaded card is the one
  read before. The activities no longer handle the Keypop card types.
- The settings and the server status are accessed through the `AppSettingsRepository` and `ServerStatusProvider` ports,
  instead of the shared preferences and the REST client.
#### Fixed
- Typo in the "Invalid storage card" message.
- Interpretation of the status codes returned by the server, which was shifted by one: each error now displays the
  right message (e.g. "card not personalized" instead of "expired environment"), and the expired environment (contracts
  reading) and the rejected card (reload, personalization) no longer leave the screen stuck on the loading animation.
### Reloading remote server
#### Changed
- The storage card extension is now registered with `SmartCardService.checkCardExtension(...)` at startup, as required
  by `keyple-service-java-lib` `3.5.0` for card extensions outside the `org.eclipse.keyple` package whose types are
  received as JSON data.
- Replaced the deprecated `RemoteReaderServer.getInitialCardContent()` by `getInitialCardContent(Class)`
  (`keyple-distributed-remote-java-lib` `2.6.0`).
- Dashboard: removed the Jest-specific ESLint configuration.
- The status codes of the remote services are produced with the `RemoteServiceStatus` enum.
#### Removed
- Unreachable "card not read" status code (`4`) of the contract writing.
### Reloading remote .NET client
#### Changed
- Target framework: `net7.0` -> `net8.0`.

## [26.03.26]
### Added
- Mifare Classic card support across validation, control and reloading-remote apps.
- `UiManager` interface with two implementations in validation app: `ArriveUiManagerImpl` (Parkeon SDK — LEDs + sounds)
  and `AndroidUiManagerImpl` (MediaPlayer — sounds only), selected at build time via conditional source sets.
- Arrive Axio 4 proprietary plugin support (replaces Flowbird Axio 2).
### Changed
- Dashboard UI (reloading-remote server): refreshed layout and theme.
### Upgraded
- `keyple-java-bom`: `2025.11.21` -> `2026.03.19`
- `keyple-card-cna-storagecard-java-lib` (mock): `2.1.0` -> `2.3.0`
- `keyple-plugin-cna-storagecard-java-lib` (mock): `1.0.0` -> `1.1.0`
- CI/CD workflows: Node.js upgraded from 16 to 20.
### Removed
- Flowbird Android plugin mock.

## [25.12.01]
### Changed
- Standardization of naming conventions and package structure to harmonize all modules.
### Upgraded
- `keyple-java-bom`: `2025.10.24` -> `2025.11.21`
- `keyple-card-cna-storagecard-java-lib` (mock): `1.1.0` -> `2.1.0`
### Validation app
#### Changed
- Major architectural refactoring to apply **Clean Architecture** principles, isolating business logic (Domain) from
  Android and UI implementations.

## [25.10.30]
### Changed
- Switched to [Keyple Java BOM](https://github.com/eclipse-keyple/keyple-java-bom) `2025.10.24` for dependency
  management, replacing individual Keyple component definitions.
### Validation app
#### Added
- Added ratification feature with anti-passback management & communication failure recovery

## [25.09.10]
### Changed
- `androidMinSdk` moved from `24` to `26` for all Android apps.
### Upgraded
- Keyple components
    - `keyple-card-calypso-java-lib`: `3.1.8` -> `3.1.9`
    - `keyple-card-calypso-crypto-pki-java-lib`: `0.2.3` (new)
### Control app
#### Added
- Added PKI card authentication mode when no SAM is available and when the card is PKI compliant.

## [2025.08.27]

🆕 Initial Unified Release
This is the first release of the consolidated Keyple Ticketing demo applications, now hosted in a single repository.
It brings together previously independent projects into a unified codebase to simplify development, maintenance, and distribution.

🔄 Merged archived repositories:
  - [Common Lib](https://github.com/calypsonet/keyple-demo-ticketing-common-lib)
  - [Reloading Remote](https://github.com/calypsonet/keyple-demo-ticketing-reloading-remote)
  - [Validation App](https://github.com/calypsonet/keyple-demo-ticketing-validation-app)
  - [Control App](https://github.com/calypsonet/keyple-demo-ticketing-control-app)

[Unreleased]: https://github.com/calypsonet/keyple-demo-ticketing/compare/26.03.26...HEAD
[26.03.26]: https://github.com/calypsonet/keyple-demo-ticketing/compare/25.12.01...26.03.26
[25.12.01]: https://github.com/calypsonet/keyple-demo-ticketing/compare/25.10.30...25.12.01
[25.10.30]: https://github.com/calypsonet/keyple-demo-ticketing/compare/25.09.10...25.10.30
[25.09.10]: https://github.com/calypsonet/keyple-demo-ticketing/compare/2025.08.27...25.09.10
[2025.08.27]: https://github.com/calypsonet/keyple-demo-ticketing/releases/tag/2025.08.27