# Keyple Demo Ticketing Ecosystem

[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-orange.svg)](https://openjdk.java.net/)
[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)

A comprehensive open source ticketing ecosystem demonstrating the [Eclipse Keyple middleware](https://keyple.org) in
real-world use cases. This project is provided by the [Calypso Networks Association](https://calypsonet.org) and serves
as a foundation for building contactless card and NFC smartphone-based ticketing systems.

## Overview
The Keyple Demo Ecosystem consists of three interconnected applications that simulate a complete public transportation
ticketing workflow:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   RELOAD DEMO   │────>│ VALIDATION DEMO │────>│  CONTROL DEMO   │
│                 │     │                 │     │                 │
│ Load contracts  │     │ Validate entry  │     │ Check validity  │
│ onto cards      │     │ to transport    │     │ after use       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Application Roles

- **[Reload Demo](src/reloading-remote/)**: Remote contract loading using distributed client/server architecture
- **[Validation Demo](src/validation/)**: Entry validation for transportation networks
- **[Control Demo](src/control/)**: Post-validation card inspection and compliance checking

## Supported Card Technologies

### Calypso Cards

Standard Calypso contactless cards supporting:

- Secure sessions with SAM authentication
- Multiple contracts (1-4 depending on product type)
- Cryptographic security operations
- Full transaction traceability

### Storage Cards

Simple storage cards featuring:

- Basic read/write operations without SAM requirements
- Single contract storage
- Simplified validation procedures
- **Note**: This demo implementation is intentionally basic for demonstration purposes

**Security Consideration**: Storage Card implementations in this demo lack production-level security mechanisms.
Production deployments should implement appropriate cryptographic protections, signature verification, and secure key
management.

## Architecture

### Distributed Client/Server Model

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Mobile/Web    │     │   Java Server   │     │      SAM        │
│    Clients      │────>│   + Dashboard   │────>│   (Security)    │
│                 │     │                 │     │                 │
│ User Interface  │     │ Business Logic  │     │ Cryptographic   │
│ Card Interface  │     │ Data Management │     │   Operations    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Supported Platforms

**Client Applications:**

- Android 8.0+ (Native and KMP)
- iOS 15.3+ (KMP)
- Windows Desktop (.NET 10.0)
- JVM Desktop (Kotlin Multiplatform)

**Server Requirements:**

- Java 17+ with PC/SC reader
- SAM (Security Access Module) for Calypso cards
- Web dashboard for monitoring

## Quick Start

### 1. Server Setup

```bash
# Download latest server release
wget https://github.com/calypsonet/keyple-demo-ticketing/releases/latest

# Start server (requires PC/SC reader with SAM)
java -jar kdt-reloading-server-X.Y.Z-full.jar

# Access dashboard at http://localhost:8080
```

### 2. Card Personalization

Use any client application's personalization feature to initialize cards with:

- Environment data (validity dates, application number)
- Clean contract slots
- Reset event logs and counters

### 3. Workflow Execution

1. **Load Contracts**: Use Reload Demo to add Season Pass or Multi-trip tickets
2. **Validate Entry**: Present card to Validation Demo terminal
3. **Control Check**: Verify card status using Control Demo

## Supported Hardware

### Tested Terminals

- **Famoco FX205** - Enterprise NFC terminal
- **Standard NFC Smartphones** - Consumer devices
- **PC/SC Readers** - Desktop integration

### Proprietary Plugins (Available on Request)

- Bluebird EF501
- Arrive Axio 4

Contact [CNA](https://calypsonet.org/contact-us/) for access to proprietary terminal plugins.

## Data Model

The ecosystem uses standardized data structures defined in the [Common Library](src/common/):

- **Environment Record**: Card metadata and validity information
- **Event Log**: Transaction history and validation events
- **Contract Records**: Contracts (tickets) and their properties
- **Counter Files**: Usage tracking for multi-trip tickets

See [Common Library Documentation](src/common/README.md) for detailed specifications.

## Development

### Prerequisites

- JDK 17 to build the Java and Kotlin components (Android apps, KMP client, server)
- Android Studio for mobile development
- Node.js for web dashboard
- PC/SC compatible readers for testing

### Project Structure

```
keyple-demo-ticketing/
├── README.md                               # This file
├── CHANGELOG.md                            # Changes of the demo since the repository merge
├── docs/history/                           # Changelogs of the archived repositories (before the merge)
├── src/common/                             # Shared data structures and utilities
├── src/reloading-remote/                   # Remote reload clients and server
│   ├── server/                             # Java server application
│   ├── client/
│   │   ├── keyple-mobile-android/          # Android native client
│   │   ├── pc-dotnet/                      # .NET desktop client  
│   │   └── interop-mobile-multiplatform/   # Kotlin Multiplatform client
├── src/validation/                         # Android validation terminal
└── src/control/                            # Android control terminal
```

### Android Application Architecture

The Android applications (`validation`, `control` and `reloading-remote/client/keyple-mobile-android`) follow a
**layered architecture with ports and adapters** (hexagonal style). They do not use the MVVM pattern: the activities
drive the user flow and call the domain directly.

| Package  | Content                                                                                               |
|----------|-------------------------------------------------------------------------------------------------------|
| `domain` | Business logic: `TicketingService` (entry point of the UI), `procedures`, `model`, and the ports (`spi`) |
| `data`   | Adapters implementing the ports (readers, Keypop API factories, user feedback, settings, logging...)  |
| `ui`     | Activities, UI adapters (e.g. `UiContextImpl`), and UI models with their mappers when needed          |
| `di`     | Hilt modules binding the adapters to the ports and providing the domain services                      |

**Dependency rules**

- The `domain` layer only depends on the `common` library, the Keypop APIs and the Keyple utilities. It depends neither
  on Android (including the application resources `R`), Timber or the dependency injection framework (no Hilt, Dagger
  or `javax.inject` annotation), nor on the `data`, `ui` and `di` layers.
- Everything the domain needs from the outside world is expressed as a port in `domain/spi` (e.g. `ReaderManager`,
  `KeypopApiProvider`, `UserFeedback`, `AppSettingsRepository`, `Logger`, `UiContext`, `RemoteServiceManager`,
  `ServerStatusProvider`) and implemented by an adapter in `data`, or in `ui/adapters` for the UI-bound ones.
- The `ui` layer calls the use cases of the domain services, or the ports directly for simple accesses (e.g. settings).
  It never accesses the `data` layer, and does not handle the Keypop card types (`CalypsoCard`, `StorageCard`...).
- The `data` layer implements the ports and does not depend on the `ui` layer.
- The `di` layer is the only place where adapters are bound to ports. The domain services (`TicketingService` and the
  procedures) carry no annotation and are provided by the `DomainModule`. The stable dependencies of the procedures are
  injected through their constructor; only the data of the current transaction is passed to them (`ControlContext`,
  `ValidationContext`).
- The control and validation procedures implement a common interface (`ControlProcedure`, `ValidationProcedure`), one
  implementation per card technology: `TicketingService` executes the procedure supporting the selected card, a new
  card technology only requiring a new procedure. The
  bindings are application-wide singletons (`SingletonComponent`); the application is annotated `@HiltAndroidApp`, and
  each activity receiving dependencies (through its base activity) `@AndroidEntryPoint`.
- The application settings are accessed through the `AppSettingsRepository` port, never through a global object.

**Logging**

- `domain`, `data` and `di` log through the `Logger` port. Timber is only used by its implementation (`data/LoggerImpl`),
  by `DemoApplication` (Timber initialization) and by the activities.

**UI models**

- A UI model (`ui/model`, `Parcelable`) is only created when an object must be passed between activities through an
  `Intent`. Otherwise, the UI uses the domain models directly. When a UI model has a domain counterpart, the mapping
  from the domain model to the UI model is done in `ui/mappers`.

**Results**

- The domain produces no text displayed to the users. The procedures return typed results (`sealed interface`, e.g.
  `ValidationResult.Accepted`, `Rejected(reason)` or `Failed(error)`), and the UI translates them into the texts of
  `strings.xml`, as the reloading client does with the status codes of the server (`RemoteServiceStatus`).
- The refusal of a card by a business rule is a result (`RejectionReason`), not an exception: the exceptions are kept
  for the technical errors (card communication...).

These rules are checked by the CI (`.github/scripts/check-android-architecture.sh`), which can also be run locally:

```sh
bash .github/scripts/check-android-architecture.sh src/control
```

**Naming conventions**

- Application class `DemoApplication`, base activity `BaseActivity` (and `BaseCardActivity` for the card screens of
  the reloading client), card presentation screen `CardReaderActivity`.
- Layouts named after their activity (`activity_<name>.xml`), shared toolbar `toolbar.xml`.
- Hilt modules named after what they provide (e.g. `AppSettingsModule`, `DomainModule`, `ReaderModule`), with
  `provide<Type>` methods.
- Enums named without suffix (e.g. `TerminalType`, `CardProtocol`, `Status`).

**Ticketing vocabulary**

The code uses the same business terms in all the applications:

| Term                                        | Meaning                                                                                  | Replaces                 |
|---------------------------------------------|------------------------------------------------------------------------------------------|--------------------------|
| `Contract`                                  | Right recorded in the card (contract record), e.g. a season pass or a multi-trip ticket   | "title"                  |
| `Product`, `ProductType`                    | What the reloading client sells (type, price, quantity), loaded as a contract            | "title"                  |
| `trip` (`remainingTrips`, `tripsToLoad`)    | Unit of the counter of a multi-trip contract                                             | "ticket", "nbTickets"    |
| `TerminalType`                              | Terminal running the application (Bluebird, Famoco, Arrive, standard NFC terminal)      | "reader type"            |
| `CardMedium`                                | Medium holding the card application (contactless card, SIM, wearable, embedded)         | "device"                 |
| `UserFeedback`                              | Sounds and LEDs of the terminal                                                          | "UI manager"             |

The texts displayed to the users use "ticket" for the contracts and products. The terms of the card data model
(`EnvironmentHolder`, `Event`, `Contract`, `ContractTariff`, `ContractPriority`, `PriorityCode`) follow its
specification (see the `common` library).

**Differences between the applications**

The applications share these conventions, except for the following deliberate differences:

| Difference                                                                                                         | Reason                                                                                                                                  |
|--------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Validation app: free orientation, `arrive`/`arrive-mock` source sets and `com.parkeon.app.customer` action          | Arrive (Parkeon) terminal: landscape screen, optional proprietary SDK, application launched by the terminal                             |
| Reloading Android client: reader detected from the device, no device selection screen                             | Only the Bluebird terminals use a specific reader; the other devices use the Android NFC reader                                         |
| Reloading Android client: settings persisted (`SharedPreferences`), in memory in the control and validation apps   | The server address must survive a restart; the control and validation apps ask for the terminal at each startup                         |
| Reloading Android client: no local SAM, but NFC and OMAPI (SIM) readers                                            | The SAM is managed by the server (Keyple Distributed)                                                                                   |
| KMP client: organized by feature (no `domain`/`data`/`ui` layers), without Hilt nor the `common` library           | Compose code shared by Android, desktop and iOS; the `common` library relies on Java libraries (bit-lib4j, Keyple utilities) unavailable on iOS |
| KMP client: Android APK suffixed `-android-`                                                                       | The same project name is used by the desktop packages                                                                                   |
| Directory and project names (`reloading-remote`, `keyple-mobile-android`, `kdt-...`) differing from the packages   | Referenced by the CI, the names of the released artifacts and the existing links                                                        |

### Building from Source

Each module (e.g., `src/common`, `src/reloading-remote/server`, `src/validation`, `src/control`) is an independent
project and should be built using the tools and instructions provided within its own directory. For Gradle-based
projects, navigate to the module's directory and use its `./gradlew` wrapper.

### Build Optimization

The Android applications (`validation`, `control`, and `reloading-remote` client) are configured with ProGuard
minification enabled for both debug and release builds. This removes SLF4J debug/trace logs from the application and
third-party libraries (like Keyple), resulting in smaller APKs and better runtime performance. To see full unoptimized
logs during debugging, comment out the `debug` build type configuration in the respective `app/build.gradle.kts` files.

## Card Application Identifiers

Compatible Calypso card AIDs:

- `A000000291FF9101` - Keyple Generic test card
- `315449432E49434131` - CD Light/GTML Compatibility
- `315449432E49434133` - Calypso Light
- `A0000004040125090101` - Navigo IDF

Test cards available in the [CNA Test Kit](https://calypsonet.org/technical-support-documentation/).

## Contributing

1. Fork the repository
2. Create a feature branch
3. Follow the coding standards and documentation templates
4. Submit a pull request with clear description
5. Ensure all tests pass and documentation is updated

## License

This project is licensed under the BSD 3-Clause License - see the [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: [keyple.org](https://keyple.org)
- **Community**: [Calypso Networks Association](https://calypsonet.org)
- **Issues**: Use GitHub Issues for bug reports and feature requests
- **Technical Support**: [Contact CNA](https://calypsonet.org/contact-us/)

## Related Projects

- [Eclipse Keyple middleware](https://keyple.org) - Core SDK and plugins
- [Calypso Card Specification](https://calypsonet.org/technical-support-documentation/)
- [Keyple Distributed JSON API](https://keyple.org/user-guides/non-keyple-client/server-json-api/)