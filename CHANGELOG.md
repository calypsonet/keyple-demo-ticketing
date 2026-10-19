# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Breaking changes
- Components renamed with the English ticketing terms, the applications being installed as new applications (the
  previous ones are to be uninstalled, and their settings are not taken over):
  - "inspection" instead of "control": `src/inspection`, `kdt-inspection-app`, application "Keyple Demo — Inspection";
  - "sales" instead of "reloading" for the demo selling and loading the tickets ("reload" designating, on a Calypso
    card, the reloading of its stored value): `src/sales`, `kdt-sales-*` artifacts, applications "Keyple Demo —
    Sales", "Keyple Demo — Sales (Interop)", and "Keyple Demo — Sales (.NET)".
- Build requirements: Java 17 for all the Java and Kotlin components, .NET 10 for the .NET client, and Node.js 24 for
  the dashboard.

### Added
- Functional documentation (`docs/functional-documentation`): use cases, business rules, and messages of the demo, with
  diagrams and a glossary, in the graphic charter of the CNA documents, attached as PDF and HTML files to each release.
- Unit tests of the business rules (common library, sales server, validation and inspection apps), and CI checks of
  the log and exception messages, of the architecture of the Android applications, and of the dashboard code.
- Validation app: anti-passback check for the storage cards.
- Sales clients: the result screens return automatically after 5 seconds, the remaining time being displayed.

### Changed
- All the applications: same English ticketing vocabulary in the displayed texts ("ticket", "card", "Season pass",
  "Multi-trip ticket", "Card rejected", etc.) and English typography; technical messages are no longer displayed (the
  cause being logged), and the clients display the same messages for the statuses of the server.
- Reactivity of the terminals: the operations on the readers and the sounds run on dedicated threads, the stop of the
  card detection (about 440 ms on the Bluebird terminals) no longer delaying the display; the inspection result is
  displayed in an overlay of the card reader screen; the "Please wait" indicator is removed.
- Sales clients: server settings checked and saved in the same way, without restarting the application (entries
  validated, "Server state" button taking the availability of the SAM into account, "Save" button returning to the
  settings menu).
- Code quality: layered architecture with ports and adapters in the Android applications (Hilt instead of Dagger),
  sales server split by business operation, remote services and data named after the business operations, same log
  template in all the components, and conventions described in the README.
- READMEs reviewed against the code; the changelogs of the archived repositories are moved to `docs/history/`.

### Fixed
- Common library: a field value whose high bit is set was truncated when encoded (for example, a counter of 16,777,215
  written as 65,535 on a storage card).
- Validation app:
  - anti-passback: a card could be validated again a few seconds after a first validation, debiting a trip twice;
  - Calypso cards: the expired or exhausted contracts are marked in the event when the card is rejected, contracts of
    unknown type are no longer accepted without debit, and the type of the contract used is its tariff;
  - a card that cannot be selected is rejected, instead of being validated with the data of the previous card;
  - crash at startup with the mocked storage card library, and on an unexpected error of the validation procedure.
- Inspection app: a Calypso contract of an unexpected version rejects the card; the validation period could end 59
  seconds too early; the failure sound was given twice.
- Validation and inspection apps: the Bluebird reader can be selected again on Android 13 and later.
- Sales server:
  - loading: the expired contracts keep their priority and their record is reused, a season pass is renewed for 30
    days from its end date, a single contract card keeps a usable ticket of another type, and a card without space is
    rejected instead of a success without writing;
  - a card whose last event has an unexpected version is rejected instead of being loaded;
  - each card gets a unique application number at personalization (it was 1 for all the cards);
  - the server starts without PC/SC reader or SAM;
  - dashboard: transactions no longer lost, missed, or displayed twice.
- Sales Android client: the statuses returned by the server were interpreted with a shift of one (wrong message, screen
  stuck on the loading animation).
- Sales multiplatform client: an error of the server when reading the card is displayed, instead of an empty card.
- Sales .NET client: the number of trips was sent in hexadecimal (10 trips were refused and 20 trips loaded 14).

### Upgraded
- `keyple-java-bom`: `2026.03.19` -> `2026.10.14` (`keyple-card-calypso-java-lib` `3.3.0`, fixing the session signature
  of the PKI cards when the CA certificate is not registered).
- Android Gradle plugin `8.10.1` -> `9.4.1`, Gradle -> `9.8.1`, Kotlin -> `2.3.21`.
- Multiplatform client: Compose Multiplatform `1.8.2` -> `1.11.1`.
- Sales server: Quarkus `1.8.1.Final` -> `3.40.1` (LTS); dashboard: React `18.2` -> `19.3`, Material UI `5.17` -> `9.4`.
- Sales .NET client: `net7.0` -> `net10.0`.

### Removed
- Coppernic C-One 2 terminal support (plugin not updated since 2022).
- Stored value contract type, which could not be created by the demo.
- Sales Android client: "Wearable" and "Embedded Secure Element" card media, which had no reader.
- Multiplatform client: iOS Intel simulator target, no longer supported by Compose Multiplatform.
- Unused dependencies (dagger-android, RxJava, EventBus, Koin, `log4j`, Material icons extended, etc.), code, and
  resources.

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

🔄 Merged archived repositories (their changelogs are kept in [docs/history](docs/history)):
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