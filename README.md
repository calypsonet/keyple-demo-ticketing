# Keyple Demo Ticketing ecosystem

[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-17%2B-orange.svg)](https://openjdk.org/)
[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)

A comprehensive open source ticketing ecosystem demonstrating the [Eclipse Keyple middleware](https://keyple.org) in
real-world use cases. This project is provided by the [Calypso Networks Association](https://calypsonet.org) and serves
as a foundation for building contactless card and NFC smartphone-based ticketing systems.

**Contents**: [Overview](#overview) · [Quick start](#quick-start) ·
[Supported card technologies](#supported-card-technologies) · [Data model](#data-model) ·
[Supported hardware](#supported-hardware) · [Architecture](#architecture) ·
[Security considerations](#security-considerations) · [Development](#development) ·
[Coding conventions](#coding-conventions) · [Troubleshooting](#troubleshooting) · [Contributing](#contributing) ·
[Support](#support) · [Related projects](#related-projects) · [License](#license)

## Overview
The Keyple Demo Ticketing ecosystem consists of three interconnected applications that simulate a complete public
transportation ticketing workflow:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   SALES DEMO    │────>│ VALIDATION DEMO │────>│ INSPECTION DEMO │
│                 │     │                 │     │                 │
│ Load contracts  │     │ Validate entry  │     │ Check validity  │
│ onto cards      │     │ to transport    │     │ after use       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Application roles

- **[Sales Demo](src/sales/)**: Sale and remote loading of the contracts using a distributed client/server architecture
- **[Validation Demo](src/validation/)**: Entry validation for transportation networks
- **[Inspection Demo](src/inspection/)**: Post-validation ticket inspection, checking the validity of the last
  validation

The use cases and business rules are described for a functional audience (project managers, functional architects)
in the [Functional Documentation](docs/functional-documentation/index.adoc), also attached as PDF and HTML files
to each [release](https://github.com/calypsonet/keyple-demo-ticketing/releases). Its annexes are a technical
reference for the developers: the [data model of the card](docs/functional-documentation/index.adoc#card-data-model),
and the [procedures executed on the card](docs/functional-documentation/index.adoc#card-procedures), step by step.

## Quick start

All the components are released together, their version being the release tag in the YY.MM.DD format (date of
the release); the local builds are versioned `99.99.99`.

### 1. Server setup

Download `kdt-sales-server-YY.MM.DD-full.jar` from the [releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases), then:

```bash
# Start server (requires PC/SC reader with SAM)
java -jar kdt-sales-server-YY.MM.DD-full.jar

# Access dashboard at http://localhost:8080
```

### 2. Card personalization

Use the personalization feature of the Android or multiplatform sales client to personalize cards with:

- Environment data (validity dates, application number)
- Clean contract slots
- Reset event logs and counters

### 3. Workflow execution

1. **Load contracts**: Use Sales Demo to add season passes or multi-trip tickets
2. **Validate entry**: Present card to Validation Demo terminal
3. **Ticket inspection**: Verify card status using the Inspection Demo

## Supported card technologies

The demo supports Calypso cards, with secure sessions authenticated by a SAM (or by PKI for the inspection), and
storage cards (MIFARE Ultralight, ST25 SRT512, MIFARE Classic), read and written without cryptographic operation and
supported with the proprietary storage card libraries (see
[Proprietary plugins](#proprietary-plugins-available-on-request)). Their features and the tickets they hold are
described in the [Functional Documentation](docs/functional-documentation/index.adoc#cards).

### Card application identifiers

The Calypso card applications supported by the demos, those of the test cards of the
[CNA Test Kit](https://calypsonet.org/technical-support-documentation/), are listed in the annex
[Card data model](docs/functional-documentation/index.adoc#supported-card-applications) of the Functional
Documentation.

## Data model

The data model of the card (environment and holder, event, contracts, and counters) and the procedures using it are
specified in the annexes [Card data model](docs/functional-documentation/index.adoc#card-data-model) and
[Card procedures](docs/functional-documentation/index.adoc#card-procedures) of the Functional Documentation. The
[Common Library](src/common/README.md) implements the data model, shared by all the applications.

## Supported hardware

### Tested terminals

The terminals supported by each application (Famoco FX205, Bluebird EF501, Arrive Axio 4, any Android device with NFC)
are listed in the [Functional Documentation](docs/functional-documentation/index.adoc#tested-terminals), and their
reader plugins in the README of each application ([validation](src/validation/README.md#tested-terminals),
[inspection](src/inspection/README.md#tested-terminals),
[sales Android client](src/sales/client/keyple-mobile-android/README.md#plugin-integration)). The server and
the desktop clients use PC/SC readers.

### Proprietary plugins (available on request)

- Bluebird EF501 (reader plugin)
- Arrive Axio 4 (reader plugin)
- Storage cards: card extension (`keyple-card-cna-storagecard-java-lib`) and, in the Android applications, reader
  plugin (`keyple-plugin-cna-storagecard-java-lib`)

By default, they are replaced by mocks (`libs` directory): the corresponding terminals are grayed out, and the storage
cards are not supported. To use a component:

1. Request it from [CNA](https://calypsonet.org/contact-us/)
2. Copy the provided library (`.aar` or `.jar` file) to the `libs` directory at the root of the repository: the build
   uses it instead of the corresponding mock (`-mock.aar` or `-mock.jar` file)
3. Rebuild the applications (for example, `./gradlew assembleDebug`)

The README of each application lists the components it uses.

## Architecture

### Distributed client/server model

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Mobile/Web    │     │   Java Server   │     │      SAM        │
│    Clients      │────>│   + Dashboard   │────>│   (Security)    │
│                 │     │                 │     │                 │
│ User Interface  │     │ Business Logic  │     │ Cryptographic   │
│ Card Interface  │     │ Data Management │     │   Operations    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Supported platforms

**Client applications:**

- Android 8.0+ (Native and KMP)
- iOS 15.3+ (KMP)
- Windows Desktop (.NET 10.0)
- JVM Desktop (Kotlin Multiplatform)

**Server requirements:**

- Java 17+ with PC/SC reader
- SAM (Secure Access Module) for Calypso cards
- Web dashboard for monitoring

### Android application architecture

The Android applications (`validation`, `inspection`, and `sales/client/keyple-mobile-android`) follow a
**layered architecture with ports and adapters** (hexagonal style). They do not use the MVVM pattern: the activities
drive the user flow and call the domain directly.

The `domain` layer is designed to be easily transposed to another language (for example, C++ or Rust): only the `ui` and
`data` layers are specific to the target language and platform.

Each application is standalone and can be read, built, or copied on its own: no module is shared between the Android
applications apart from the `common` library. The duplication of some technical files (ports and adapters of the
platform, terminal configuration, permissions, etc.) is assumed. The files which must stay identical are checked by
the CI (`.github/scripts/check-android-twin-files.sh`): a change of one of them must be reported in each
application.

| Package  | Content                                                                                                                                             |
|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `domain` | Business logic: `CardReaderSession` and `usecases` (entry points of the UI), `procedures`, `model`, `mappers` and `builders`, and the ports (`spi`) |
| `data`   | Adapters implementing the ports (readers, Keypop API factories, user feedback, settings, clock, logging, etc.)                                      |
| `ui`     | Activities, UI-bound adapters (`ActivityUiContext`), and UI models with their mappers when needed                                                   |
| `di`     | Hilt modules binding the adapters to the ports and providing the domain services                                                                    |

**Dependency rules**

- The `domain` layer only depends on the Kotlin standard library, the `java.time` date types (`LocalDate`,
  `LocalDateTime`, which have an equivalent in any language), the Keypop APIs, the Keyple utilities, and the `common`
  library. It depends neither on Android (including the application resources `R`), Timber, the dependency injection
  framework (no Hilt, Dagger, or `javax.inject` annotation), or any other JVM-specific API, nor on the `data`, `ui`, and
  `di` layers. The platform services, including the system clock, are accessed through ports.
- The `domain` layer is synchronous: its operations are blocking calls (no `suspend` function), the choice of the
  threads belonging to the `ui` layer (coroutines bound to the activity lifecycle). An asynchronous operation of the
  platform is either awaited by the adapter, the call being blocking (for example, creation of the Arrive plugin
  factory), or expressed in the port by a callback (for example, registration of the OMAPI plugin).
- Everything the domain needs from the outside world is expressed as a port in `domain/spi` (for example,
  `TerminalReaders`, `KeypopApiProvider`, `UserFeedback`, `AppSettingsRepository`, `Clock`, `Logger`, `UiContext`,
  `TicketingServer`, `ServerReadinessChecker`) and implemented by an adapter in `data`, or in `ui` for the UI-bound
  ones.
- A port is named after what it represents or provides (for example, `TerminalReaders`, `TicketingServer`, `Clock`), not
  with a vague suffix such as `Manager`, and uses a platform-neutral vocabulary (for example, `release` rather than
  `onDestroy`).
- An adapter is named after the technology it relies on, followed by the name of its port (for example,
  `KeypleTerminalReaders`, `InMemoryAppSettingsRepository`, `SharedPreferencesAppSettingsRepository`, `TimberLogger`,
  `ActivityUiContext`). The `Impl` suffix is not used.
- The `ui` layer calls the use cases of the domain services, or the ports directly for simple accesses (for example,
  settings). It never accesses the `data` layer, and does not handle the Keypop card types (`CalypsoCard`,
  `StorageCard`, etc.).
- The `data` layer implements the ports and does not depend on the `ui` layer.
- The `di` layer is the only place where adapters are bound to ports. The domain services (`CardReaderSession`, the
  use cases, and the procedures) carry no annotation and are provided by the `DomainModule`. The stable dependencies of
  the procedures are injected through their constructor; only the data of the current transaction is passed to them
  (`InspectionContext`, `ValidationContext`). The bindings are application-wide singletons (`SingletonComponent`); the
  application is annotated `@HiltAndroidApp`, and each activity receiving dependencies `@AndroidEntryPoint`.

**Domain services**

- `CardReaderSession` manages the readers of the terminal: initialization of the readers and of the SAM (and of the
  user feedback devices), detection of the cards, selection of the presented card, end of its processing.
- One use case per user operation (`domain/usecases`), invoked by the UI: `InspectCard` (inspection app), `ValidateCard`
  (validation app), `ReadContracts`, `LoadContract`, and `PersonalizeCard` (sales client). The use cases of the
  inspection and validation apps select the card, execute the procedure supporting it, and give the user feedback
  (sound, LEDs) of the result.
- The inspection and validation procedures implement a common interface (`InspectionProcedure`,
  `ValidationProcedure`), one implementation per card technology: the use case executes the procedure supporting the
  selected card, a new card technology only requiring a new procedure.
- The application settings are accessed through the `AppSettingsRepository` port, never through a global object.

**Logging**

- `domain`, `data`, and `di` log through the `Logger` port. Timber is only used by its implementation
  (`data/TimberLogger`, which tags each message with the name of the calling class), by `DemoApplication` (Timber
  initialization) and by the activities.
- The messages follow the convention of all the components (see [Log messages](#log-messages)).

**UI models**

- A UI model (`ui/model`, `Parcelable`) is only created when an object must be passed between activities through an
  `Intent`. Otherwise, the UI uses the domain models directly. When a UI model has a domain counterpart, the mapping
  from the domain model to the UI model is done in `ui/mappers`.

**Results**

- The domain produces no text displayed to the users. The procedures return typed results (`sealed interface`, for
  example, `ValidationResult.Accepted`, `Rejected(reason)`, or `Failed(error)`), and the UI translates them into the
  texts of `strings.xml`, as the sales client does with the status codes of the server (`RemoteServiceStatus`).
- The rejection of a card by a business rule is a result (`RejectionReason`), not an exception: the exceptions are kept
  for the technical errors (card communication, etc.).

These rules are checked by the CI (`.github/scripts/check-android-architecture.sh`, and
`.github/scripts/check-android-twin-files.sh` for the twin files), which can also be run locally:

```sh
bash .github/scripts/check-android-architecture.sh src/inspection
bash .github/scripts/check-android-twin-files.sh
```

**Naming conventions**

- Application class `DemoApplication`, start screen `SplashActivity`, base activity `BaseActivity` (and
  `BaseCardActivity` for the card screens of the sales client), card presentation screen `CardReaderActivity`.
- Layouts named after their activity (`activity_<name>.xml`), shared toolbar `toolbar.xml`, result overlays
  `layout_<name>_overlay.xml`, rows of the lists `<item>_recycler_row.xml`, and items of the drop-down lists
  `spinner_item_<item>.xml`.
- Hilt modules named after what they provide (for example, `AppSettingsModule`, `DomainModule`, `ReaderModule`), with
  `provide<Type>` methods.
- Enums named without suffix (for example, `TerminalType`, `CardProtocol`, `ResultType`).
- Mappers and builders, files named after the domain type concerned:
  - a mapper converts a representation into another one, from a layer to another or from an external
    representation (Keypop card, DTO of the server) to the domain. Extension function of the converted object:
    `to<Type>()` in `domain/mappers` (for example, `SmartCard.toCardInfo()`, file `CardInfoMapper.kt`), `toUi()` in
    `ui/mappers` (for example, `Contract.toUi(resources)` of the sales Android client, file `ContractUiMapper.kt`);
  - a builder builds an object from several sources, none of them prevailing (for example, data read in the card and
    results computed by a procedure). Top-level function `build<Type>(...)` in `domain/builders` (for example,
    `buildContract(...)`, file `ContractBuilder.kt`).
- The root package of an application only contains `DemoApplication`: the display states (for example, `ResultType`) and
  the Android helpers (for example, `RecyclerView` extensions) belong to the `ui` layer.

**Ticketing vocabulary**

The applications use the same business terms, in the code and in the displayed texts:

| Concept                                    | In the code                                  | Displayed to the users                                |
|--------------------------------------------|----------------------------------------------|-------------------------------------------------------|
| Right recorded in the card                 | `Contract` (contract record)                 | "ticket", or the name of the ticket                   |
| What is sold (type, price, quantity)       | `Product`, `ProductType`                     | "ticket", or the name of the ticket                   |
| Names of the two tickets                   | `SEASON_PASS`, `MULTI_TRIP` (`PriorityCode`) | "Season pass", "Multi-trip ticket"                    |
| Unit of the counter of a multi-trip ticket | `trip` (`remainingTrips`, `tripsToLoad`)     | "trip", "trips"                                       |
| Medium holding the card application        | `CardMedium`                                 | "card" (contactless card, SIM card, etc.)             |
| Terminal running the application           | `TerminalType`                               | name of the terminal (Bluebird, Famoco, Arrive, etc.) |
| Sounds and LEDs of the terminal            | `UserFeedback`                               | -                                                     |

- A product becomes a contract when it is loaded in the card ("load a contract").
- "Ticket" is only used in the displayed texts, as the word understood by the users; in the code, it is ambiguous
  (a trip, a product, or a contract) and only appears in "ticketing", the name of the domain.
- "Title" (literal translation of the French "titre de transport") is not used: it is a false friend in English.
- "Support" (French "support") is not used either: the displayed texts say "card".
- The names of the resources of the displayed texts follow the terms of the code (for example, `product_price`,
  `trips_count` plurals, `no_contract_label`).

The operations are named in the same way in all the components:

| Operation                               | In the code                                         | Displayed to the users                 |
|-----------------------------------------|-----------------------------------------------------|----------------------------------------|
| Reading of the contracts of a card      | `ReadContracts`, `analyzeContracts` (by the server) | "Read in progress"                     |
| Loading of a product in the card        | `LoadContract`, `loadContract`, `tripsToLoad`       | "Load ticket", "Ticket loaded"         |
| Extension of a season pass by a loading | `renewedSeasonPass`                                 | -                                      |
| Preparation of a card for the demo      | `PersonalizeCard`, `personalizeCard`                | "Personalization", "Card personalized" |
| Use of a contract at the entry          | `ValidateCard`, `ValidationResult`                  | "Validation"                           |
| Check of the card by an inspector       | `InspectCard`, `InspectionResult`                   | "Inspection"                           |

- The demo selling and loading the tickets is named "Sales"; the operation is "load", including when trips are
  added to a multi-trip ticket or when a season pass is renewed ("reload" is not used).
- "Issuance" and "initialization" do not designate the personalization ("initialization" only designates the
  technical start of a component, for example, a reader plugin).
- "Write" only designates the technical operation (writing of a record), never the business operation.
- The secure sessions are named after their Calypso access level: `PERSONALIZATION` (personalization), `LOAD`
  (loading) and `DEBIT` (validation, inspection).

The terms of the card data model (`EnvironmentHolder`, `Event`, `Contract`, `ContractTariff`, `ContractPriority`,
`PriorityCode`) follow its specification (see the `common` library).

**Differences between the applications**

The applications share these conventions, except for the following deliberate differences:

| Difference                                                                                                      | Reason                                                                                                                                          |
|-----------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| Validation app: free orientation, `arrive`/`arrive-mock` source sets and `com.parkeon.app.customer` action      | Arrive (Parkeon) terminal: landscape screen, optional proprietary SDK, application launched by the terminal                                     |
| Sales Android client: reader detected from the device, no terminal selection screen                             | Only the Bluebird terminals use a specific reader; the other devices use the Android NFC reader                                                 |
| Sales Android client: settings persisted (`SharedPreferences`), in memory in the inspection and validation apps | The server address must survive a restart; the inspection and validation apps ask for the terminal at each startup                              |
| Sales Android client: no local SAM, but NFC and OMAPI (SIM) readers                                             | The SAM is managed by the server (Keyple Distributed)                                                                                           |
| KMP client: `ui` and `data` packages without `domain` layer (MVVM), without Hilt nor the `common` library       | Compose code shared by Android, desktop and iOS; the `common` library relies on Java libraries (bit-lib4j, Keyple utilities) unavailable on iOS |
| KMP client: Android APK suffixed `-android-`                                                                    | The same project name is used by the desktop packages                                                                                           |
| Directory and project names (`keyple-mobile-android`, `pc-dotnet`, `kdt-...`) differing from the packages       | Referenced by the CI, the names of the released artifacts and the existing links                                                                |

## Security considerations

The demo is not a production system:

- The cryptographic operations of the Calypso cards are performed by a SAM: by the server for the sales clients,
  which hold no cryptographic material, and by the terminal for the validation and inspection apps (the inspection
  authenticating the cards by PKI when the terminal has no SAM).
- The storage cards are read and written without cryptographic protection: a production system adds signature
  verification and secure key management.
- The SAMs and the cards of the demo use test keys: a production system uses the keys of its own network, stored and
  renewed securely.
- The server is designed for a local network (HTTP): a production system uses HTTPS and authenticates the clients and
  the administrative functions (dashboard).

## Development

### Prerequisites

- JDK 17 to build the Java and Kotlin components (Android apps, KMP client, server)
- Android Studio for mobile development, and Xcode for the iOS version of the KMP client
- Node.js for the web dashboard of the server, and the .NET SDK for the .NET client
- PC/SC compatible readers for testing

The versions required by each component are given in its README.

### Project structure

```
keyple-demo-ticketing/
├── README.md                               # This file
├── CHANGELOG.md                            # Changes of the demo since the repository merge
├── .github/                                # CI workflows and checks (messages, architecture, twin files)
├── docs/history/                           # Changelogs of the archived repositories (before the merge)
├── docs/functional-documentation/          # Functional documentation (AsciiDoc, PlantUML diagrams, CNA charter)
├── libs/                                   # Proprietary libraries (on request) and their mocks
├── libs.versions.toml                      # Version catalog shared by the Gradle projects
├── src/common/                             # Shared data structures and utilities
├── src/sales/                              # Sales clients and server
│   ├── server/                             # Java server application
│   ├── client/
│   │   ├── keyple-mobile-android/          # Android native client
│   │   ├── pc-dotnet/                      # .NET desktop client  
│   │   └── interop-mobile-multiplatform/   # Kotlin Multiplatform client
├── src/validation/                         # Android validation terminal
└── src/inspection/                         # Android inspection terminal
```

### Building from source

Each module (for example, `src/common`, `src/sales/server`, `src/validation`, `src/inspection`) is an
independent project and should be built using the tools and instructions provided within its own directory. For
Gradle-based projects, navigate to the module's directory and use its `./gradlew` wrapper.

### Unit tests

The unit tests check the business rules of the data model, independently of the cards and of the terminals (simulated
with MockK when a procedure is tested end to end). They are run by the CI:

| Component          | Tests                                                                                               | Command                                         |
|--------------------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------|
| `src/common`       | Record codecs, data types, event structure, DF name check                                           | `./gradlew test` (also run by the server build) |
| `src/sales/server` | Contract analysis, personalization and contract loading rules                                       | `./gradlew test`                                |
| `src/validation`   | Validation rules (anti-passback, versions, contracts, priorities) and Calypso validation procedure  | `./gradlew testDebugUnitTest`                   |
| `src/inspection`   | Inspection rules (validity of the last validation), displayed data and Calypso inspection procedure | `./gradlew testDebugUnitTest`                   |

The current date is provided by a clock (`Clock` port of the Android applications, `java.time.Clock` of the server),
replaced by a fixed date in the tests. The domain being transposable to another language, its tests describe the
expected behavior as an executable specification.

### Build optimization

The Android applications (`validation`, `inspection`, and `sales` client) are configured with ProGuard
minification enabled for both debug and release builds. This removes SLF4J debug/trace logs from the application and
third-party libraries (like Keyple), resulting in smaller APKs and better runtime performance: the messages of the
applications (Timber) are all kept, those of Keyple from the `INFO` level only. To see full unoptimized
logs during debugging, comment out the `debug` build type configuration in the respective `app/build.gradle.kts` files.

## Coding conventions

Conventions followed by the code of all the components, checked by the CI where possible (see
[Contributing](#contributing)).

### Log messages

All the components (server and its dashboard, Android applications, multiplatform and .NET clients) write their log
messages with the same template:

```
Message [key1=value1, key2=value2]
```

- **Message**: English sentence starting with a capital letter, without final punctuation, acronyms in capitals (SAM,
  NFC, REST, DF, UID). It names the event without its variable values, so that the same event always gives the same
  message (search, counting): the values are only in the context.
- **Context** (optional): `key=value` pairs between square brackets, separated by `, `. The keys are in camelCase and
  use the terms of the glossary (`plugin`, `reader`, `serialNumber`, `uid`, `productType`, `contractTariff`,
  `tripsToLoad`, `block`, etc.), the values are written without quotes.
- **No class or method name** in the message: the logger gives it (tag of `TimberLogger`, logger of the class with
  SLF4J, Napier, and Serilog).
- **Exceptions**:
  - unexpected error: `ERROR` level, the exception, when there is one, being attached to the call (stack trace) and
    not copied in the message, for example, `logger.error("Unexpected error while loading the contract", e)`;
  - expected incident (card rejected or removed, server not ready): `WARN` level without stack trace, the message of
    the exception being given in the `cause` key, for example,
    `Card communication error while loading the contract [cause=...]`;
  - an exception thrown again is not logged: it is logged once, where it is handled.

| Level   | Use                                                                                                                           |
|---------|-------------------------------------------------------------------------------------------------------------------------------|
| `ERROR` | Unexpected error, with the stack trace                                                                                        |
| `WARN`  | Rejection or expected incident (card rejected or removed, SAM or server unavailable), without stack trace                     |
| `INFO`  | One line per business operation (for example, `Loading a contract in a Calypso card [...]`) and main events of the life cycle |
| `DEBUG` | Technical details (blocks read or written, secure session, states of the screens) and repetitive events                       |

The `TRACE` level is not used. A repetitive event (for example, periodic check of the server) is logged at the `DEBUG`
level, or only when its state changes (for example, "Server ready", "Server not ready").

These rules, and the ones of the exception messages (see [Exception messages](#exception-messages)), are checked by the
CI (`.github/scripts/check-messages.py`, Python 3), which can also be run locally from the root of the repository:

```bash
python3 .github/scripts/check-messages.py
```

It checks the form of the messages (capital letter, final punctuation, class name, and variable values and keys in
camelCase in the context at the end), the warnings logged without stack trace, and the catch blocks: a catch block
logs the exception, throws an exception, or explains in a comment why it does not log it (for example,
`// Rejection logged where it is detected`).

Where to read the logs:

| Component            | Logs                                                                   | Default level | To see the `DEBUG` messages                                                |
|----------------------|------------------------------------------------------------------------|---------------|----------------------------------------------------------------------------|
| Server               | Console                                                                | `INFO`        | `java -Dquarkus.log.category."org.calypsonet.keyple".level=DEBUG -jar ...` |
| Android applications | Logcat (Android Studio or `adb logcat`)                                | All levels    | Keyple messages: see [Build optimization](#build-optimization)             |
| Multiplatform client | Logcat (Android), console (desktop), Xcode console (iOS)               | All levels    | -                                                                          |
| .NET client          | File `logs/demo-keyple-less-<date>.log` (kept 7 days), not the console | `DEBUG`       | -                                                                          |

Examples:

```
INFO  Loading a contract in a Calypso card [plugin=Android NFC, serialNumber=0000000011223344, productType=PRIME_REVISION_3, contractTariff=MULTI_TRIP, tripsToLoad=10]
WARN  Card rejected: environment expired [envEndDate=2025-05-01]
WARN  Card communication error while loading the contract [cause=Card removed]
ERROR Unexpected error while loading the contract
```

### Exception messages

The messages of the exceptions follow the template of the log messages, `Message [key1=value1, key2=value2]`, as
they are often logged (`cause` key) or displayed:

- English sentence starting with a capital letter, without final punctuation, without class or method name, the
  variable values being given in the context (for example, `Reader not found [reader=...]`).
- The cause is chained to the exception (`IllegalStateException("Error while registering the reader plugins", e)`),
  not copied in its message. When the exception type does not accept a cause, its message is given in the `cause` key
  (for example, `ServerIOException("Communication error [cause=...]")`).
- The exception is of a specific type (for example, `IllegalStateException`, `InvalidOperationException`), never
  `Exception` nor `RuntimeException`.

The messages displayed to the users are never the messages of the exceptions: the Android applications display their
texts (`strings.xml`), a technical error being displayed with a generic text, and the server returns to the clients
which do not use Keyple the messages of the statuses of its remote services (`RemoteServiceMessages`, texts of the
Android applications), the causes being logged.

### Method names

All the components name their methods with the same verbs:

| Verb                     | Use                                                                    | Examples                                                                         |
|--------------------------|------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `display...`             | Display of the result of an operation                                  | `displayResult(result)` (inspection and validation apps), `DisplaySuccess` state |
| `show...` / `hide...`    | Display or masking of an element of the screen                         | `showResultOverlay()` / `hideResultOverlay()`, `showError(message)`              |
| `signal...`              | User feedback of the terminal (sounds, LEDs)                           | `signalSuccess()`, `signalFailure()`, `signalWaiting()`                          |
| `init...` / `release...` | Technical start and stop of a component (plugin, reader, resources)    | `initCardReader()`, `initSamPlugin()`, `releaseCardReader()`                     |
| `start...` / `stop...`   | Start and stop of a continuous activity (detection, monitoring)        | `startCardDetection()`, `startReaderMonitoring()`                                |
| `wait...`                | Blocking or suspending wait                                            | `waitForCard()` (multiplatform client), `WaitForCard()` (.NET client)            |
| `execute...`             | Execution of a procedure or of a remote service                        | `execute(context)`, `executeRemoteService()`                                     |
| `find...`                | Search of an element, which may not be found                           | `findReader()`, `findLocation()`, `findAvailablePosition()`                      |
| `build...` / `to...`     | Building from several sources / conversion                             | `buildContract()`, `buildSeasonPassContract()`, `toCardInfo()`, `toUi()`         |
| `create...`              | Creation of a new object, returned to the caller                       | `createCardSelectionManager()`, `createSecuritySetting()`, `createDataStore()`   |
| `decode` / `encode`      | Conversion between the content of the card and its structures          | codecs of the `common` library, `decodeCardData()`                               |
| `is...` / `has...`       | Boolean                                                                | `isServerReady()`, `isSamAvailable()`, `hasTripsLeft()`                          |
| `...Rejection`           | Reason of the rejection of the card by a rule, null if the rule is met | `environmentRejection()`, `contractVersionRejection()`                           |

- A method is named after what it does for the user or the domain, not after its technical effect (for example,
  `applyServerSettings()` saves the settings and reconnects the client, `showReadInProgress()` displays "Read in
  progress").
- An action is named with a verb. A function returning a value computed without side effect may be named after this
  value, as a property (for example, `technicalError()`, `rejectionMessage()`, `environmentRejection()`), and the
  Compose functions, which describe an element of the screen, are named with a noun (for example, `SuccessMessage`,
  `PlatformAnimation`).
- A name without verb promises a value without hidden cost: a function accessing the card, the network, or the
  storage, or making a heavy computation, is named with the verb telling it (for example, `readCard()`, `findReader()`,
  `load()`), even when it returns a value.
- In Kotlin, a value without parameter nor computation is a property rather than a function (for example,
  `CardReaderSession.cardReader`, `cardSecuritySetting`, instead of `getCardReader()`).
- The same operation has the same name in all the components. The methods executing a remote service of the server
  (`JsonApiService` of the server, multiplatform, and .NET clients) are named after its identifier
  (`SELECT_APP_AND_PERSONALIZE_CARD`: `selectAppAndPersonalizeCard()`); the ports of the domain are named after the
  operation of the glossary (`TicketingServer.personalizeCard()`).
- No abbreviation (`btn`, `anim`, `env`, etc.), except the acronyms (`Sam`, `Aid`, `Apdu`) and the terms of the card
  data model specification (`envEndDate`, etc.).
- Each language keeps its case convention: camelCase in Kotlin, Java, and JavaScript, PascalCase in C#.
- The names imposed by the frameworks and by the Keyple interfaces (`onCreate`, `onPluginEvent`, `setupReader`, etc.)
  are kept.

## Troubleshooting

Common issues of the Android applications (validation, inspection, and sales Android client), the README of each
component giving its specific issues:

**NFC detection not working**
- Enable NFC in the Android settings and grant the NFC permission to the application
- Keep the card steady during the reading, and try different positions on the NFC antenna

**SAM not detected or not available** (validation and inspection apps)
- Check that the SAM is properly inserted in the SAM reader of the terminal
- Check that the SAM matches the keys of the cards (test SAM for the demo cards)
- Restart the application to initialize the SAM reader again

**Terminal type not available**
- Check that the selected terminal type matches the actual hardware
- The proprietary terminals require their plugin (see [Proprietary plugins](#proprietary-plugins-available-on-request))

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the procedure (fork, feature branch, signed-off commits, pull request).

A contribution follows the conventions of this README
([Android application architecture](#android-application-architecture), [Log messages](#log-messages),
[Exception messages](#exception-messages), [Method names](#method-names)), updates the documentation of the components
concerned, and passes the checks of the CI:

- Code format: `./gradlew spotlessCheck` in each Gradle project
- Unit tests: see [Unit tests](#unit-tests)
- Log and exception messages: `python3 .github/scripts/check-messages.py`
- Architecture and twin files of the Android applications: `.github/scripts/check-android-architecture.sh` and
  `.github/scripts/check-android-twin-files.sh`

## Support

- **Documentation**: [keyple.org](https://keyple.org)
- **Community**: [Calypso Networks Association](https://calypsonet.org)
- **Issues**: Use GitHub Issues for bug reports and feature requests
- **Technical support**: [Contact CNA](https://calypsonet.org/contact-us/)

## Related projects

- [Eclipse Keyple middleware](https://keyple.org): Core SDK and plugins
- [Calypso Specifications](https://calypsonet.org/calypso-specifications/)
- [Keyple Server JSON API](https://keyple.org/user-guides/non-keyple-client/server-json-api/)

## License

This project is licensed under the BSD 3-Clause License. See the [LICENSE](LICENSE) file for details.