# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

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
    - `keyple-card-calypso-java-lib` `3.1.8` -> `3.1.9`
    - `keyple-card-calypso-crypto-pki-java-lib` `0.2.3` (new)
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

[Unreleased]: https://github.com/calypsonet/keyple-demo-ticketing/compare/25.10.30...HEAD
[25.10.30]: https://github.com/calypsonet/keyple-demo-ticketing/compare/25.09.10...25.10.30
[25.09.10]: https://github.com/calypsonet/keyple-demo-ticketing/compare/2025.08.27...25.09.10
[2025.08.27]: https://github.com/calypsonet/keyple-demo-ticketing/releases/tag/2025.08.27