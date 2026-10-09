# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Changed
- Android applications: layered architecture with ports and adapters, documented in the README ("Android Application
  Architecture") and checked by the CI (`.github/scripts/check-android-architecture.sh`) for the domain, data and ui
  layers. The domain layer no longer
  depends on Android, Timber or the dependency injection framework: the domain services are provided by a Hilt
  `DomainModule`, and the card managers receive their dependencies through their constructor.
- Android applications: logging through the `Logger` port (same `d`/`i`/`w`/`e` interface in all applications) in the
  domain, data and di layers. Timber is only used by `LoggerImpl`, `Application` and the activities.
- Android applications: card processing is now finalized with `ObservableCardReader.finalizeCardProcessing()` after
  each card.
- Android applications: when the storage card extension is not available (e.g. mocked library), the storage cards are
  not supported and a warning is logged once, instead of failing.
- Android applications: the coroutines launched by the activities are bound to their lifecycle (`lifecycleScope`
  instead of `GlobalScope`), and deprecated Android APIs are replaced (`ProgressDialog`, `startActivityForResult`,
  `onBackPressed`, `Resources.getColor`, `Intent.getParcelableExtra`).
- Control and validation apps: the plugin registration is a suspend function instead of using `runBlocking`. Reloading
  Android client: the plugins are registered directly, as before the refactoring.
- Android applications: Java 17 as source and target level instead of Java 8. No impact on the supported devices: the
  bytecode is converted for the minimum SDK by the Android build.
- Android applications: target SDK defined by its own `androidTargetSdk` property (still `35`) instead of
  `androidCompileSdk`.
- Android applications: build scripts updated:
  - the APK files are named through `base.archivesName` instead of an internal Android Gradle plugin API
    (`ApkVariantOutputImpl`). The debug APK keeps its name; the unsigned release APK is now suffixed
    `-release-unsigned`;
  - `packagingOptions` is replaced by `packaging`, with the same structure in all the applications;
  - the properties are read with `project.property()`, which fails with an explicit message when a property is
    missing, instead of `findProperty()`;
  - the Android and Kotlin Gradle plugins are declared once in the root project, instead of being loaded separately by
    the application and the common module.
- Android applications: dependency injection with Hilt instead of Dagger `2.25` and dagger-android (deprecated), and
  annotation processing with KSP instead of kapt:
  - the application is annotated `@HiltAndroidApp`, the activities receiving dependencies `@AndroidEntryPoint`, and
    the modules are installed in the `SingletonComponent` (`@Singleton` bindings);
  - the `AppComponent`, the activity injectors (`UIModule`), the `AppModule` and the custom scopes (`AppScoped`,
    `ActivityScoped`) are removed.
- Android applications: names harmonized between the applications (conventions documented in the README):
  - application class `DemoApplication` instead of `Application`, the name of the Android class it extends (also in
    the KMP client, instead of `KeypleDemoApp`);
  - card presentation screen `CardReaderActivity` in all the applications (`ReaderActivity` in the control and
    validation apps), and base activities `BaseActivity` and `BaseCardActivity` in the reloading client
    (`AbstractDemoActivity` and `AbstractCardActivity`);
  - toolbar layout `toolbar.xml` (`logo_toolbar.xml` in the control and validation apps), and reload result layout
    `activity_reload_result.xml` (`activity_charge_result.xml`) named after its activity;
  - enums without the `Enum` suffix: `CardProtocol` (`CardProtocolEnum`) and, in the reloading client, `CardMedium`
    (`DeviceEnum`);
  - `Status` enums reduced to their values, the labels they carried being displayed nowhere (the reloading client
    passes the status name between its activities);
  - Hilt modules of the reloading client: `AppSettingsModule` (settings and shared preferences) instead of
    `DataModule`, the server status provider being provided by `RestModule` with the other network adapters.
- All the applications: same ticketing vocabulary, documented in the README ("Ticketing vocabulary"):
  - `Contract` instead of "title" (translation of the French "titre de transport") for the contracts of the card:
    `ContractsRecyclerAdapter`, `contract_recycler_row.xml`, `contractsList`, and in the reloading Android client
    `Contract`, `UiContract`, `ContractMapper` and `ContractUiMapper` (`CardTitle`, `UiCardTitle`...);
  - `Product` and `ProductType` (values `MULTI_TRIP` and `SEASON_PASS`) for the products sold by the KMP client
    (`Title`, `TitleType` with `SINGLE` and `SEASON`), loaded through the `LoadContract` route (`WriteTitleCard`);
  - `remainingTrips` and `tripsToLoad` for the trips of a multi-trip contract (`nbTicketsLeft`, `ticketsToLoad`), also
    in the JSON API (`tripsToLoad` instead of `ticketToLoad`) and `name` instead of `title` for the contracts
    returned to the KMP client;
  - `TerminalType` instead of `ReaderType` (terminal running the application), and in the reloading Android client
    `CardMedium` and `CardMediumVisibility` instead of `DeviceType` and `DeviceVisibility` (contactless card, SIM...);
    the card medium is saved under a new key of the settings (`card_medium`);
  - `UserFeedback` instead of `UiManager` (sounds and LEDs of the terminal) in the control and validation apps;
  - control app: invalid card screen `InvalidCardActivity` instead of `NetworkInvalidActivity`;
  - texts displayed: "ticket" instead of "title" (e.g. "Buy ticket", "Ticket loaded", "No valid ticket detected").
- Android applications: the empty `themes.xml` files are removed. The control app defines its `AppTheme` theme (same
  appearance), applied to the whole application instead of being repeated on each activity.
- Android applications and KMP client: Jetifier is disabled, no library requiring it anymore (Dagger `2.25` and the
  Coppernic plugin were the last ones).
- Android applications and server: the common library is included as a separate build (`includeBuild`, Gradle
  composite build) instead of being declared as a subproject of each build (`include(":common")`). The builds no
  longer share the outputs of the common library with their own build state: the library is no longer rebuilt by each
  build (e.g. when opening the projects in the IDE), and the Kotlin JVM plugin is no longer declared in the root
  projects of the Android applications. The library is referenced by its coordinates (`demoCommon` in the version
  catalog). Its checks (code format, unit tests), no longer run by the builds of the applications, are run by the
  `check` task of the server, and therefore by its CI job.
- CI/CD workflows: GitHub actions `checkout`, `setup-java`, `setup-node` and `setup-dotnet` upgraded from v4 to v5,
  .NET upgraded from 7.0 to 10.0, and Node.js upgraded from 20 (end of life) to 24.
- The changelogs of the archived repositories, no longer maintained, are moved from the modules to `docs/history/`.
- Versions in the READMEs aligned with the build: JDK 17 to build, Java 17+ for the KMP desktop client, Kotlin 2.3,
  Android 8.0+ and iOS 15.3+ for the KMP client.
### Upgraded
- `keyple-java-bom`: `2026.03.19` -> `2026.09.29`
- `keyple-card-cna-storagecard-java-lib` (mock): `2.3.0` -> `2.3.1`
- `keyple-plugin-cna-storagecard-java-lib` (mock): `1.1.0` -> `1.1.1`
- Android applications: Dagger `2.25` -> Hilt `2.60.1`, with KSP `2.3.12`.
- Android applications and KMP client: Android Gradle plugin `8.10.1` -> `9.4.1` and Gradle wrapper `8.11.1` ->
  `9.8.1` (also for the common library):
  - the Kotlin support built into the Android Gradle plugin replaces the `org.jetbrains.kotlin.android` plugin (the
    Kotlin JVM target follows the Java target, and `src/main/kotlin` is a default source directory);
  - Kotlin `2.2.10` -> `2.3.21` (the Kotlin compiler plugins, e.g. Parcelize, support the built-in Kotlin since
    `2.2.20`);
  - the build scripts read the Gradle properties with `project.property()` instead of the delegates deprecated by
    Gradle 9.6.
- Control and validation apps: the runtime of the obsolete `kotlin-android-extensions` plugin, declared but not used by
  the Famoco plugin, is excluded: it duplicated the classes of the Parcelize runtime.
- Android applications: Lottie `3.4.4` -> `6.7.1`. SLF4J remains in `1.7.36`, the last 1.7 version, the `slf4j-timber`
  binding not supporting SLF4J 2.
### Removed
- Android applications: dagger-android, the `javax.annotation` (GlassFish) dependency and the kapt plugin, replaced by
  Hilt and KSP.
- Control and validation apps: support of the Coppernic C-One 2 terminal (device selection button, reader
  configuration, `keyple-plugin-cna-coppernic-cone2-java-lib` `2.0.2` and Coppernic Maven repository). The plugin, not
  updated since 2022, required Jetifier through the Coppernic SDK.
- `kotlin-stdlib-jdk8` dependency of the Android applications, merged into `kotlin-stdlib` (added by the Kotlin Gradle
  plugin) since Kotlin 1.8.
- `log4j` `1.2.17` (end of life), declared but not used by `bit-lib4j`: excluded from the dependencies of the common
  library, the Android applications and the server.
- Android applications: MultiDex (`androidx.multidex`), useless since the minimum SDK is 26.
- Unused mocks: `keyple-card-cna-storagecard-java-lib-2.3.0` and `keyple-plugin-cna-storagecard-java-lib-1.1.0`.
- Stored value contract type (`PriorityCode.STORED_VALUE`, contract tariff `3`), which could not be created by the demo
  (neither loaded by the server nor issued at personalization) and was only partially handled (e.g. not debited when
  validating a Calypso card). The contract tariff `3` is now reserved for future use and handled as unknown.
- Unused elements detected by Android Lint and code analysis:
  - Android applications: unused resources (strings, dimensions, styles, drawables, layouts, menu, sound), the unused
    `FragmentScoped` annotation and extension functions, and duplicated sounds in the control app assets.
  - Reloading remote Android client: unused "last validations" display (`UiValidation`, related fields of
    `UiCardReaderResponse`, layout and resources).
  - Reloading remote KMP client: unused Compose resources (images, animation, strings), colors and navigation helper.
  - Reloading remote .NET client: unused `MessageDto` setters, unused `WaitForCardAbsent()` and `IsCardPresent()`
    methods of the PC/SC reader adapter, and unused `Serilog.Sinks.Console` and
    `Microsoft.Extensions.Logging.Abstractions` packages.
  - Common library: unused `CardConstants.SC_ENVIRONMENT_AND_HOLDER_SIZE_BYTES` constant.
  - Reloading remote server dashboard: unused `@testing-library/*` and `web-vitals` dependencies.
### Common library
#### Added
- `RemoteServiceStatus` enum defining the status codes of the remote services (`statusCode` field of
  `AnalyzeContractsOutputDto`, `WriteContractOutputDto`, `CardIssuanceOutputDto` and of the `SelectAppAnd...OutputDto`
  of the Server JSON API, still transmitted as an integer), with the new `CARD_FULL` status (`7`): no empty contract
  record nor expired or exhausted contract to replace.
#### Changed
- `Location.toString()` now returns the name of the location.
- The test class `ContractInfoStructureParserTest` is renamed `ContractStructureParserTest`, after the tested class.
- Documentation of the status codes of `AnalyzeContractsOutputDto`, `WriteContractOutputDto`, `CardIssuanceOutputDto`
  and of the `SelectAppAnd...OutputDto` aligned with the codes actually returned by the server.
#### Upgraded
- Kotlin: `1.7.22` -> `2.3.21` (same version as the other projects).
- Java 17 as source and target level instead of Java 8, as the applications using the library: building it no
  longer requires a JDK 8.
### Validation app
#### Added
- Anti-passback check for storage cards: a card presented again within the anti-passback delay is rejected.
#### Changed
- The user feedback (sounds, LEDs) is provided by the dedicated `UserFeedback` port instead of `ReaderManager`. The Arrive
  and Android implementations are renamed `ArriveFeedbackDevice` and `AndroidFeedbackDevice`.
- The settings are accessed through the `AppSettingsRepository` port instead of the global `AppSettings` object.
- The UI uses the domain models directly (UI models removed, as no object is passed between activities).
- Only the main activity is exported (launched by the launcher and the Arrive terminal): the other activities are no
  longer accessible to the other applications, and the reader activity loses its unused `VIEW` intent filter.
#### Removed
- ProGuard rule keeping the `Location` class, which is not accessed by reflection.
- Unused `CardSummaryActivity` and its layouts (replaced by the summary overlay of the reader screen).
#### Fixed
- Crash at startup when the storage card library is mocked.
- Bluebird terminals running Android 13 or later: the Bluebird reader can be selected again. The storage permission,
  requested with the SAM access and refused without prompt since Android 13, is only declared and requested up to
  Android 12. The unused `WRITE_EXTERNAL_STORAGE` permission is removed.
- Calypso cards: contracts of unknown type (contract tariff not supported by the demo) are no longer validated (they
  were accepted without any debit).
### Control app
#### Changed
- Major architectural refactoring to apply the layered architecture with ports and adapters, isolating the business
  logic (domain) from the Android and UI implementations.
- The locations are provided by the `common` library (local `locations.json` file removed), and are displayed by their
  name only.
- The SAM selection no longer filters the SAM on its power-on data (SAM C1).
- The user feedback (sounds) is provided by the `UserFeedback` port, and the settings are accessed through the
  `AppSettingsRepository` port.
- `ReaderManager.clear()` is no longer part of the port, being only used by its implementation (as in the other
  applications).
#### Removed
- Values of the `Status` enum never produced by the control procedure (`LOADING`, `SUCCESS`, `WRONG_CARD`,
  `DEVICE_CONNECTED`).
- Gson dependency, not used by the application (still provided to the Keyple libraries by their own dependencies), as
  in the validation app.
#### Fixed
- Bluebird terminals running Android 13 or later: the Bluebird reader can be selected again. The storage permission,
  requested with the SAM access and refused without prompt since Android 13, is only declared and requested up to
  Android 12. The unused `WRITE_EXTERNAL_STORAGE` permission is removed.
- The waiting indicator is now hidden when the control procedure fails, instead of staying displayed when returning to
  the reader screen.
### Reloading remote Android client
#### Changed
- Major architectural refactoring to apply the layered architecture with ports and adapters: the plugins and readers
  management is moved from the activities to `ReaderManagerImpl`, and the remote services are called through the
  `RemoteServiceManager` port.
- The business logic is moved from the activities to the domain: card selection (AIDs depending on the device),
  interpretation of the server status codes, building of the contracts and check that the reloaded card is the one
  read before. The activities no longer handle the Keypop card types.
- The settings and the server status are accessed through the `AppSettingsRepository` and `ServerStatusProvider` ports,
  instead of the shared preferences and the REST client.
- The texts displayed by the activities (error messages, contract names and descriptions, server settings checks) are
  defined as string resources instead of being hardcoded.
- The REST client uses synchronous Retrofit calls instead of RxJava, and the server status indicator is updated by the
  coroutine requesting the status instead of an EventBus event.
- The HTTP exchanges with the server are logged in the debug builds through the `Logger` port. The logging interceptor
  had no level set, so it logged nothing.
- The server settings are saved without restarting the application: the "Restart" button, which killed the
  application, is replaced by a "Save" button. The remote services use the server configured at the time of each
  request.
- The server IP address entered in the settings is checked as an IPv4 address (four numbers from 0 to 255) instead of
  using the deprecated `Patterns.IP_ADDRESS`.
- The error screens return automatically to the previous screen after 8 seconds, as the success screen does after 5
  seconds: the card presentation screen to try again (or the home screen when it was closed), unless the user
  chooses before with the buttons. This applies to the reload and personalization result in error, and to the card
  summary for an invalid card or a technical error. The automatic return uses a coroutine bound to the screen
  lifecycle instead of a `Timer`.
- The screens returning automatically show the remaining time: a bar emptied at the bottom of the screen (white on the
  result screens, of the color of the message on the card summary), and the remaining seconds in the "Retry" button
  of the result screen in error ("Retry (8)"), which performs the same action.
#### Upgraded
- `okhttp-logging-interceptor`: `3.9.1` -> `3.14.9`, the version of the OkHttp library used by Retrofit.
#### Removed
- RxJava (`rxjava`, `rxandroid`, Retrofit `adapter-rxjava2`), EventBus and Retrofit `converter-scalars` dependencies.
- Unused SAM reader settings of the reader manager (the SAM is managed by the server), which used the deprecated
  `ContactCardCommonProtocol`.
#### Fixed
- Typo in the "Invalid storage card" message.
- Interpretation of the status codes returned by the server, which was shifted by one: each error now displays the
  right message (e.g. "card not personalized" instead of "expired environment"), and the expired environment (contracts
  reading) and the rejected card (reload, personalization) no longer leave the screen stuck on the loading animation.
- When the card presented for the reload is not the one read before, the type of the presented card is displayed
  instead of "Undetermined card type".
- An expired season pass is displayed as "Season pass - Expired" according to its validity end date, the server no
  longer replacing its tariff by `31` (`EXPIRED`) in the contracts it returns.
- A card without space for a new contract displays "No space left on the card for a new ticket" (`CARD_FULL` status).
### Reloading remote server
#### Upgraded
- Quarkus: `1.8.1.Final` -> `3.40.1` (LTS):
  - Java 17 required (instead of 11);
  - `jakarta.*` packages instead of `javax.*`;
  - `io.quarkus.platform:quarkus-bom` instead of `io.quarkus:quarkus-universe-bom`;
  - configuration properties renamed: `quarkus.http.cors.enabled` (all the origins still allowed with
    `quarkus.http.cors.origins`) and `quarkus.package.jar.type`.
- Gradle wrapper: `8.4` -> `9.8.1` (same version as the other projects; Quarkus 3.40 is tested with Gradle 8.14+, and
  Quarkus 4 will require Gradle 9.6+).
- Dashboard: React `18.2` -> `19.3`, Material UI (`@mui/material`, `@mui/icons-material`) `5.17` -> `9.4`.
#### Changed
- Build script: the tasks are registered with `tasks.register()` instead of the property delegates deprecated by
  Gradle 9 (`by registering`, `by creating`), `syncPackageVersion` no longer accesses the project when it is executed,
  and the declaration of the nonexistent `app` subproject is removed (an error with Gradle 9).
- REST layer: RESTEasy classic (`quarkus-resteasy`, `quarkus-resteasy-jsonb`) replaced by Quarkus REST (`quarkus-rest`,
  `quarkus-rest-jsonb`), the default REST stack of Quarkus. The endpoints and their JSON content are unchanged (still
  executed on worker threads). The remote plugin endpoint reads the request explicitly: only an unreadable request
  (malformed JSON, no content) gets a "400 Bad Request" response, the processing errors remaining server errors.
- The dashboard is embedded as static resources by the `processResources` task, instead of being copied into the build
  output (not supported by the Quarkus 3 build tasks).
- Logging: the SLF4J implementation is provided by Quarkus (JBoss Log Manager) and configured by the `quarkus.log.*`
  properties (`slf4j-simple` and `simplelogger.properties` removed).
- The storage card extension is now registered with `SmartCardService.checkCardExtension(...)` at startup, as required
  by `keyple-service-java-lib` `3.5.0` for card extensions outside the `org.eclipse.keyple` package whose types are
  received as JSON data.
- Replaced the deprecated `RemoteReaderServer.getInitialCardContent()` by `getInitialCardContent(Class)`
  (`keyple-distributed-remote-java-lib` `2.6.0`).
- Dashboard: built with Vite instead of Create React App (`react-scripts`, deprecated). The development server
  (`npm start`, port 3000) forwards the API calls to the server through the proxy defined in `vite.config.js`. The
  icons are imported from the `@mui/icons-material` ES module entry point. Removed: the unused `env-cmd` dependency and
  `.env` files, the `ajv` workaround, the obsolete `build-dev` script and the Create React App ESLint configuration.
- Dashboard: the legacy `@mui/styles` (JSS) styles are replaced by the `sx` prop of Material UI, with the same
  rendering, and the `propTypes` declarations (ignored by React 19) are removed. Removed as well: the theme overrides of
  components not used by the dashboard (buttons, tabs, tooltips), the obsolete `-o-` CSS prefixes and the empty
  `index.css` file.
- Dashboard: the selected entry of the side menu ("Transactions") is highlighted (lighter background and white mark,
  `aria-current="page"`).
- Dashboard: the source code is checked with ESLint (`npm run lint`, React hooks rules), also run by the `check` task of
  the server build, and therefore by the CI.
- Activity monitoring: the long polling endpoint `/activity/events/wait` is replaced by the Server-Sent Events stream
  `/activity/stream`, which broadcasts each new transaction to all the subscribers.
- Dashboard: the entry page (`index.html`) is no longer cached by the browsers (`Cache-Control: no-cache`), so that a
  server update is taken into account immediately; the built assets, whose names change at each build, remain cached.
- The status codes of the remote services are produced with the `RemoteServiceStatus` enum.
- Server JSON API: same status codes for all the services. `SELECT_APP_AND_READ_CONTRACTS` and
  `SELECT_APP_AND_INCREASE_CONTRACT_COUNTER` now return `4` (instead of `3`) for a card not personalized, `5` (instead
  of `4`) for an expired environment, and distinguish the card communication errors (`1`) from the other errors (`2`)
  instead of returning `1` for all errors. `SELECT_APP_AND_LOAD_CONTRACT` now returns the dedicated code `6` (instead
  of `2`) when the presented card is not the one read before.
#### Removed
- Unreachable "card not read" status code (`4`) of the contract writing.
#### Fixed
- The server starts even if no PC/SC reader is connected or no SAM is inserted (it stopped at startup): the dashboard
  displays that the SAM is not available, as when the SAM reader is disconnected after the startup, until a SAM is
  detected.
- Startup failure with the mocked storage card library: when the storage card extension is not available, a warning is
  logged and the storage cards are not supported.
- Dashboard: the transactions already processed are displayed when the dashboard is opened or reloaded (they were
  lost), no transaction is missed anymore when several transactions occur close together (the server kept only the
  last one), and each open dashboard receives all the transactions (they were shared between the dashboards). The
  history is reloaded after a connection loss.
- Dashboard: the transactions table could be corrupted (rows displayed several times) when two transactions had the
  same identifier, generated with only 4 hexadecimal characters (collisions after a few hundred transactions). The
  transactions now have a unique identifier (UUID), whose first 4 characters are displayed as before.
- Contract priorities of the event written by the contract loading, now compliant with the loading procedure:
  - the event keeps its contract priorities, only the priority of the loaded contract being updated: the priorities
    were rebuilt from the contract tariffs, which removed the priority `31` set by the validation for an expired or
    exhausted contract (and ignored the priorities computed by the loading);
  - the priority of the contracts whose validity end date is in the past is set to `31` by the loading;
  - a new contract is written in the first record whose priority is `0` (empty), otherwise `31` (expired or
    exhausted): the expired records were never reused, being searched by their tariff, which is never `31`.
- Contracts reading and analysis: the tariff of an expired contract is returned as recorded in the card, instead of
  being replaced by `31` (`EXPIRED`) in the result, which lost the type of the contract. The "Season pass - Expired"
  label returned to the KMP client is evaluated from the validity end date.
- Season pass reload: the validity of a season pass not expired is extended by 30 days from its validity end date, as
  required by the loading procedure (it was reset to 30 days from the reload date, losing the remaining days).
- A card without empty contract record nor expired or exhausted contract to replace is rejected with the new
  `CARD_FULL` status (`7`): the loading returned a success without writing the contract.
### Reloading remote KMP client
#### Changed
- The Android application is moved to a new `androidApp` module, the Android Gradle plugin 9 not supporting the Kotlin
  Multiplatform and Android application plugins in the same module: `composeApp` becomes a Kotlin Multiplatform library
  (`com.android.kotlin.multiplatform.library` plugin) with the shared code, the Android specific implementations and
  the desktop and iOS applications. The Android APK is built by `:androidApp:assembleDebug` and keeps its name; the
  iOS framework and the desktop application are still built by `composeApp`.
- Compose Multiplatform: `1.8.2` -> `1.11.1` (support of the Android Gradle plugin 9 since `1.9.3`; Kotlin 2.3 required
  for iOS), Material 3 `1.9.0`. The Compose libraries are declared explicitly in the version catalog instead of using the
  deprecated `compose.*` accessors, and the previews use `androidx.compose.ui.tooling.preview.Preview`.
- Compottie (Lottie animations): `2.0.0` -> `2.2.4` (`2.3.0`+ requires Kotlin 2.4).
- Navigation Compose: `2.9.0-rc01` -> `2.9.2` (stable), Lifecycle ViewModel Compose: `2.9.2` -> `2.10.0` (`2.11`+
  requires compileSdk 37).
- The result screens return automatically, as in the Android client: to the home screen 5 seconds after a successful
  reload or personalization, and to the previous screen 8 seconds after an error (same action as the back arrow), the
  remaining time being shown by a bar emptied at the bottom of the screen. The back arrow of the success screen now
  replaces the screens of the transaction with the home screen instead of adding it on top of them.
- iOS: the Intel simulator target (`iosX64`) is removed, no longer supported by Compose Multiplatform 1.11 and
  Compottie: the application targets the iOS devices (`iosArm64`) and the Apple Silicon simulator
  (`iosSimulatorArm64`), and the related CI job is removed.
- The Android APK files are named through `base.archivesName` instead of an internal Android Gradle plugin API
  (`ApkVariantOutputImpl`). The debug APK keeps its name.
- Android target SDK defined by its own `androidTargetSdk` property (still `35`) instead of `androidCompileSdk`.
- The background tasks of `KeypleService` (server ping, card selection scenario) and the tones of the desktop buzzer
  are launched in a scope owned by their class instead of `GlobalScope`.
- The `expect`/`actual` classes (beta feature of Kotlin) are explicitly enabled (`-Xexpect-actual-classes`), removing
  the related compilation warnings (and the related `@Suppress` annotations).
- Android: Java 17 as source and target level instead of Java 11, as in the other Android applications. The desktop
  application was already compiled for Java 17 (Kotlin toolchain).
- Android: code and resource shrinking (R8) enabled in the debug and release builds, as in the other Android
  applications (release APK of about 2 MB). The libraries provide their own R8 rules (e.g. the Ktor
  engine loaded by a service loader).
- The platform specific files of `DataStorePathProducer` are suffixed by their platform (`Datastore.android.kt`,
  `Datastore.desktop.kt`, `Datastore.ios.kt`), as the other `actual` files.
#### Removed
- Koin dependencies (`koin-core`, `koin-compose`, `koin-android`, `koin-androidx-compose`), declared but not used.
- Ktor dependencies declared twice, the toolchain repeated in the desktop target, and the `android.nonTransitiveRClass`
  property (default value since the Android Gradle plugin 8).
- Unused error screen (`ErrorScreen`, `AppError` route), never displayed and showing a fixed message, and its two
  animations (`anim_warning.json`, `anim_error_white.json`).
- Material icons extended (`material-icons-extended`, no longer updated, about 8 MB of the debug APK): the only icon
  used, the back arrow, is now the Material Symbols `arrow_back` vector, provided as a Compose resource.
#### Fixed
- The status code returned by the server when reading the card is now checked: a rejected card, a card not
  personalized, an expired environment or a communication error are displayed as an error, instead of a card without
  contract.
### Reloading remote .NET client
#### Changed
- Target framework: `net7.0` -> `net10.0` (README updated accordingly), .NET 8 reaching its end of support in
  November 2026.
- `Microsoft.Extensions.Configuration*`, `System.Configuration.ConfigurationManager` and
  `System.ServiceProcess.ServiceController`: `7.0.0` -> `10.0.12`.
- `Serilog`: `2.12.0` -> `4.4.0`, `Serilog.Sinks.File`: `5.0.0` -> `7.0.0`.
- `PCSC.Iso7816`: `6.1.3` -> `7.0.1`, `Newtonsoft.Json`: `13.0.3` -> `13.0.4`.

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