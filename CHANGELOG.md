# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Changed
- Android applications: layered architecture with ports and adapters, documented in the README ("Android Application
  Architecture") and checked by the CI (`.github/scripts/check-android-architecture.sh`) for the domain, data and ui
  layers. The domain layer no longer depends on Android, Timber or the dependency injection framework: the domain
  services are provided by a Hilt `DomainModule`, and the card procedures receive their dependencies through their
  constructor.
- Android applications: the domain layer is designed to be transposable to another language (e.g. C++ or Rust). The
  current date and time are provided by a `Clock` port (`JavaTimeClock` adapter) instead of being read from the
  system clock by the use cases, and `java.util.Locale` and `javaClass` are no longer used. The domain is
  synchronous (no `suspend` function), the threads being chosen by the UI. The architecture check of the domain is
  an allowlist of imports (Kotlin standard library, `java.time`, Keypop APIs, Keyple utilities, `common` library and
  the domain itself) and rejects the `suspend` functions.
- Control and validation apps: the procedures of each card technology (`CalypsoCardControlProcedure`,
  `StorageCardControlProcedure`, `CalypsoCardValidationProcedure`, `StorageCardValidationProcedure`, previously
  `...Manager` in the `managers` package) implement a common interface (`ControlProcedure`, `ValidationProcedure`), and
  the use case executes the one supporting the selected card instead of testing the card type. The data of the
  transaction is provided by a context object (`ControlContext`, `ValidationContext`) instead of 6 to 8 parameters.
- Android applications: `TicketingService` is split into a `CardReaderSession` (initialization of the readers, of the
  SAM and of the user feedback devices, detection and selection of the cards) and one use case per user operation
  (`domain/usecases`): `ControlCard`, `ValidateCard`, and in the reloading client `ReadContracts`, `LoadContract` and
  `PersonalizeCard`:
  - the control and validation use cases select the card, execute the procedure supporting it and give the user
    feedback (sound, LEDs) of the result, the UI only displaying it;
  - the selection errors are typed (`SelectionError`, `NotSelected` result) instead of texts built by the domain, the
    texts being slightly harmonized ("Selection error: card not recognized", "Selection error: unexpected DF name");
  - an unexpected error of the control or validation procedure is returned as a `Failed` result, instead of an exception
    to be handled by the UI (the validation app stopped);
  - the reader of a control app card is also released (end of card processing) after a selection error.
- Android applications: read-only `List` instead of `ArrayList` in the results and UI models; the `errorTitle` field
  of the control result, never set, is removed.
- Control and validation apps: typed results of the procedures, translated into texts by the UI:
  - `ControlResult` and `ValidationResult` are sealed interfaces (`CardContent`/`Accepted`, `EmptyCard`, `Rejected`,
    `Failed` and, for the validation, `CardLost`), instead of a status with an error message built by the domain;
  - the refusals of a card are expressed by a `RejectionReason` enum and the technical errors by a `TechnicalError`
    enum, displayed with the texts of `strings.xml` (the 13 messages of `BaseValidationManager` and the messages of the
    control procedures are removed from the domain);
  - the business rules no longer throw exceptions to refuse a card (`ValidationException`, and the
    `EnvironmentException`, `EventCleanCardException` and `EventWrongVersionNumberException` duplicated in the two
    control procedures, are removed), the procedures returning their result at each step; the recovery of a validation
    interrupted by the removal of the card is no longer reported by an exception carrying a success status;
  - validation: the presented card is described by a `CardDescription` (DF name of a Calypso card, product type of a
    storage card), formatted by the UI; the unused `contract` field of the result is removed, as well as the
    `Status` enum (the UI handles the result types);
  - control: the `Status` enum, only used to choose the screen displaying a result, moves to the UI model, and the
    `authenticationMode` field, never read, is removed from `UiControlResult`;
  - texts slightly harmonized: "Event error: wrong version number" and "Contract error: wrong version number" in both
    applications, and "An error occurred while reading the card." when a control error has no message.
- Android applications: logging through the `Logger` port (same `d`/`i`/`w`/`e` interface in all applications) in the
  domain, data and di layers. Timber is only used by `TimberLogger`, `Application` and the activities.
- Android applications: the adapters are named after the technology they rely on instead of the `Impl` suffix
  (`KeypleTerminalReaders`, `KeypleKeypopApiProvider`, `InMemoryAppSettingsRepository` and
  `SharedPreferencesAppSettingsRepository`, `MediaPlayerUserFeedback` and `TerminalUserFeedback`,
  `KeypleTicketingServer`, `RestServerReadinessChecker`, `DemoMifareClassicKeyProvider`, `TimberLogger`).
  `UiContextImpl` is renamed `ActivityUiContext` and moves from `ui/adapters` to `ui`, the `adapters` package being
  confusing with the RecyclerView adapters.
- Android applications: the ports are named after their responsibility instead of the `Manager` suffix:
  `TerminalReaders` (readers of the terminal, formerly `ReaderManager`) and, in the reloading client,
  `TicketingServer` (remote services of the server, formerly `RemoteServiceManager`). The readers are released by
  `release` instead of `onDestroy` (Android vocabulary), and `unregisterPlugin` is no longer part of the port of the
  reloading client, being only used by its implementation.
- Android applications: packages aligned between the applications. The mappers (conversion of a representation
  into another one) are extension functions named `to<Type>` or `toUi`, and the builders (construction from
  several sources) top-level functions named `build<Type>`: in the control app, the `ContractMapper` and
  `ValidationMapper` objects become the `buildContract` and `buildValidation` builders (`domain/builders`), the
  search of the location of the event (`LocationMapper`) being a private function of `buildValidation`. Reloading
  client: the `Status` enum, only
  used by the screens, moves to the UI model (as in the control app), `KeypleTicketingServer` moves to `data/server`
  (formerly `data/network`) with the other adapters of the server communication, and the Retrofit interface of the
  REST API of the server is named `ServerRestApi` instead of `RestClient`. Control app: the `RecyclerView` extension
  moves from the root package to the `ui` layer.
- Validation app: an accepted validation (`ValidationResult.Accepted`) carries the location of the terminal instead
  of a `ValidationData` object, which only carried a placeholder name, the date and time already provided by the
  result and empty fields, its location being searched again among the known locations from the event just built
  (the `ValidationData` model, its builder and the `locations` field of `ValidationContext` are removed). The
  location is now displayed when a validation interrupted by the removal of the card is recovered ("null" was
  displayed).
- Control app: the `destination` and `provider` fields of the last validation (`Validation`, `UiValidation`), never
  set nor displayed, are removed.
- Reloading Android client: "ticket" is no longer used in the code for the products and the trips (it remains in
  the displayed texts): `SelectTicketsActivity` becomes `SelectProductActivity` (layout `activity_select_product`),
  the number of trips to load is named `tripsToLoad` and the tariff of the chosen product `contractTariff` (intent
  extra `CONTRACT_TARIFF`), the `TICKETS_FOUND` status becomes `CONTRACTS_FOUND`, and the dimensions named after
  the tickets are renamed (`block_space`, `product_button_height`, `elevation`). The number of trips passed to the
  result screen, never read, is removed.
- Reloading demo (common library, server, Android and multiplatform clients, dashboard): one verb per operation.
  "Load" designates the loading of a contract, "write" being kept for the technical writing of the card:
  `WriteContractInputDto`/`OutputDto` become `LoadContractInputDto`/`OutputDto`, the `READ_CARD_AND_WRITE_CONTRACT`
  remote service `READ_CARD_AND_LOAD_CONTRACT`, the `ReloadCard` use case `LoadContract`, and the `writeContract`
  methods (port of the Android client, server) `loadContract`. "Personalize" designates the personalization of
  the card (instead of "issuance" and "init"): `CardIssuanceInputDto`/`OutputDto` become
  `PersonalizeCardInputDto`/`OutputDto`, the `initCard` methods of the server `personalizeCard`, and the
  transactions of the dashboard are of type `PERSONALIZATION` (instead of `ISSUANCE`, also in its menu). The
  multiplatform client writes `analyze` as the other components (`selectAppAndAnalyzeContracts`).
- Control app and reloading Android client: the `Status` enum of the UI, which gives the type of the result to
  display, is named `ResultType` (property `resultType` of the UI models, intent extra `RESULT_TYPE` of the result
  screen of the reloading client), and the `TICKETS_FOUND` value of the control app becomes `CONTRACTS_FOUND`.
  Reloading Android client: the `ServerStatusProvider` port, which checks that the server is reachable with its SAM
  ready, is named `ServerReadinessChecker` (`isServerReady` instead of `isSamReady`, adapter
  `RestServerReadinessChecker`), and the last known state is stored as `lastServerReady` (same preference key).
- Names describing the role of the elements: on the server, the transaction log of the dashboard is named
  `Transaction` (`Transaction`, `TransactionService`, `TransactionController`, `TransactionRecorder`, package
  `transaction`, REST endpoints `/transactions` and `/transactions/stream`) instead of "activity", which
  was confused with the Android activities, the data read in the card `CardData` instead of `Card`, and the services
  of the JSON API `JsonApiService` instead of `CardSelectionApi`. Multiplatform client: `KeypleService` becomes
  `KeypleInteropService`, which names the integration of the Keyple Interop libraries more precisely, and
  `CardRepository` (cache of the last card read) `ReadCardCache`. Android applications: the start screen
  `MainActivity` becomes `SplashActivity` (layout `activity_splash`).
- All the applications: same vocabulary in the displayed texts (see the "Ticketing vocabulary" of the README):
  "ticket" for the contracts and the products (instead of "contract" and "product" in some texts, e.g. "No ticket
  available", "Ticket error: wrong version number", "Ticket loaded" in the multiplatform client), "card" instead of
  "support" (e.g. "Please present a contactless card"), and the same names for the two tickets: "Season pass" and
  "Multi-trip ticket" (including the `PriorityCode` labels and the contract names sent by the server JSON API). The
  names of the text resources follow the terms of the code (e.g. `product_price`, `trips_count`, `buy_product`,
  `choose_product`, `contract_loaded`, `present_card_label`, `no_contract_desc`, `valid_season_pass`).
- Vocabulary audit: the terminal is no longer called "device" (control and validation apps:
  `DeviceSelectionActivity` becomes `TerminalSelectionActivity`, package `terminalselection`, texts `terminal_*` and
  the same title "Select the terminal"; reloading Android client: the choice of the card medium is
  `card_medium_selection`, "Choose a card"). Dashboard: the loading transactions are of type `LOAD` (instead of
  `RELOAD`) and describe the loaded contract with the names of the tickets (e.g. "Multi-trip ticket: 3"), "SAM" being
  written in capital letters. Reloading Android client: `PaymentValidatedActivity` becomes `PaymentConfirmedActivity`
  ("Payment confirmed", "validation" designating the validation of the tickets), and the resources are named after
  their content (`loading_success_label`, `loading_failed_label`, `personalization_success_label`,
  `personalization_failed_label`). Control app: duplicated texts removed (`start_validation_label`, `card_invalid_aid`).
  Multiplatform client: platform-neutral reading message ("Place your card on the reader") and texts aligned on the
  Android client. Same slogan in all the applications.
- Validation app: as in the control app, the success and error sounds of the Bluebird and Famoco terminals are played
  by a dedicated thread (`AndroidFeedbackDevice`), the start of a `MediaPlayer` no longer delaying the display of the
  validation result, and the operations on the readers (initialization, start and stop of the card detection,
  preparation of the next transaction, end of the card processing, release) are executed one after the other by a
  dedicated thread instead of the main thread: leaving the validation screen is no longer delayed by the stop of the
  card detection (about 440 ms on the Bluebird terminals).
- Reloading Android client: the reader is released outside the main thread when the card screens (reading, loading,
  personalization) are left, the stop of the card detection taking about 440 ms on the Bluebird terminals: the next
  screen (card content, result) is no longer delayed. A card screen displayed again initializes the reader once the
  release in progress is completed.
- Control app: the result of the control (card content or invalid card) is displayed in an overlay of the card reader
  screen, as in the validation app, instead of opening a new screen (`CardContentActivity`, `InvalidCardActivity`):
  the result is displayed at once, without the creation of a screen (about 290 ms on the Bluebird terminals) nor the
  stop and restart of the card detection. The processing of the card is ended when the result is hidden ("New control"
  or back), the next card being then detected. The UI models (`UiControlResult`, `UiContract`, `UiValidation`,
  `UiLocation`, `ResultType`) and their mappers, needed only to pass the result to another screen, are removed: the
  lists of the card content display the domain models.
- Multiplatform client: the contract type passed to `selectAppAndLoadContract()` is named `contractTariff` (instead
  of `code`), as the field of the input data of the server.
- Parameter names: `contractTariff` (instead of `priorityCode`) and `inputData` (instead of `input`, as in the server
  and the other clients) in the reloading Android client; `tripsToLoad` (instead of `trips`), `terminal` and
  `serviceId` (instead of `remote` and `service`, the terminal being also named `service` in a lambda) and `logLevel`
  (instead of `debugLog`) in the multiplatform client.
- Control and validation apps: `contract` and `validation` (instead of `contractItem` and `validationItem`) in the
  adapters of the control, as in the reloading client; in the validation, `validationDateTime` (instead of
  `eventDate`, the date and time of the validation), `animationFileName` (instead of `animationFile`, as in the
  multiplatform client) and `fileName` (instead of `filename`) in the playing of the sounds of the Arrive terminal.
- Reloading Android client: the result of a card operation passed between the activities is named `cardResult` on
  both sides (instead of `cardContent` when received, also in the key of the intent extra, now `CARD_RESULT`), and
  the contract bound by the adapter of the contracts `contract` (instead of `contractItem`).
- Validation app: the timer returning to the home screen on the battery powered terminals is named `autoReturnTimer`
  (instead of `timer`), as the auto return of the reloading clients.
- Validation app: the description of the presented card is named `cardDescription` (instead of `card`, also the name of
  the `SmartCard` of the validation context) in the validation result, the procedures, the use case logs and the UI.
- Variable names (eleventh audit): `today` (instead of `now`, a date) in the server, as in the other components;
  `requiresAuthentication` (instead of `requiresAuth`) in the storage card procedures; in the control procedures, the
  booleans `isContractExpired`, `isContractValidated` and `isEventValid` (instead of `contractExpired`,
  `contractValidated` and `contractEventValid`), as `isMifareClassic` and the fields of the contract they fill.
- Variable names (tenth audit): in the adapters of the readers of the Android apps, the card reader is named
  `reader` in the whole block configuring it (instead of `it` and `cardReader`), and the protocols of the card reader
  `physicalName` and `logicalName` (instead of `entry.key` and `entry.value`), as those of the SAM reader.
- Variable names (ninth audit): in the tests of the codecs, `contract` and `event` (instead of `contractStructure`
  and `eventStructure`, as in the decoding tests and the server) and `eventDateTime` (instead of `eventDate`, a
  date and time).
- Build scripts: in the validation app, the presence of the Parkeon SDK is named `hasParkeonSdk` (instead of
  `hasArriveSdk`, the file being `parkeonSdkFile`), as the build config field `HAS_PARKEON_SDK` (instead of
  `HAS_ARRIVE_SDK`); the
  storage card plugin library is named `storageCardPluginLibName` and `storageCardPluginLibFile` (instead of
  `pluginStorageCardLib…`, as `bluebirdPluginLibName` and `arrivePluginLibName`) in the Android apps; the content of
  `package.json` is named `packageJson`, `packageJsonText` and `updatedPackageJsonText` in the server.
- Variable names (eighth audit): `missingPermissions` (instead of `permissionDenied`, a list) in the control and
  validation apps; `contract` (instead of `contractStructure`, as everywhere else in the server) in the contract counter
  service; `soundDirectory`, `fileNames`, `fileName` and `destinationFile` (instead of `soundDir`, `files`, `name` and
  `dest`) in the deployment of the sounds of the Arrive terminal.
- Variable names (seventh audit): in the .NET client, the host of the server (with its protocol) is named `host`
  (instead of `baseUrl`, also the name of the field containing the port) as in the configuration and the multiplatform
  client, and the unused `logLevel` variable (read from a missing `logging` section) is removed; in the dashboard,
  `error` (instead of `e`) and `samAvailability` (instead of `json`).
- Variable names (sixth audit), .NET client: the body of the response of the `TRANSMIT_CARD_REQUEST` service is
  named in the singular, as the service (`TransmitCardRequestRespBody`, as in the Keyple Interop JSON API client
  library, and `transmitCardRequestCmdBody`); the bodies of the messages are named after their type
  (`executeRemoteServiceBody` instead of `bodyContent`, `endRemoteServiceBody` instead of `outputDto`), the JSON
  strings by their content (`outputJson` instead of `output`, `jsonResponse` instead of `result`), and the number of
  the displayed contracts `contractNumber` (instead of `i`).
- Variable names (fifth audit): `recordNumber` (instead of `record`, as in the control app) and
  `contractPriorities` (instead of `priorities`, as the field of the event it fills) in the Calypso card validation
  procedure; `networkClient` (instead of `server`) and the lambda parameter `terminal` (instead of `it`) in the
  remote service of the multiplatform client.
- Variable names (fourth audit): the server distinguishes the number of contract records of the card
  (`getContractRecordCount()`, `contractRecordCount`, as in the control and validation apps) from the number of
  contracts read (`contractCount`), and names `recordNumber` the number of the contract record analyzed (instead of
  `contractIndex`, also in the log key); `responseBody` (instead of `jsonBodyString`) in the .NET client and
  `missedTransactions` (instead of `newer`) in the dashboard.
- Variable names (third audit): `displayedContracts` (a list) in the control procedures, `shouldWriteEvent` (a
  boolean, as `shouldCloseSession`) in the Calypso validation procedure, `responseMessages` (decoded messages, instead
  of `json`) in the HTTP client of the multiplatform client, and `matchingSamReaders` (instead of `filteredByName`) in
  the `KeypleTerminalReaders` of the control and validation apps.
- Variable names (second audit): the multiplatform client names `appViewModel`, `serverSettingsViewModel` and
  `appState` the view models and the state of the application (instead of `vm`, `serverSettingsVm` and `state`), and
  `readerFilter` the filter of the reader name of the desktop client (instead of `filter`).
- Variable names harmonized: the same name for the same concept (`efContracts` and `efCounters` in the validation
  procedure as in the control one, `environment` instead of `env`, `uid` instead of `cardUID` as in the logs,
  `isServerReady` instead of `isUp` and `isReady`, `analysisOutput` and `loadingOutput` as `personalizationOutput`,
  `isMultiTripContractPresent`, `smartCardService` and `result` instead of `sc` used for two different values in the
  .NET client), and no abbreviation nor type in the names (`contractRecordCount` instead of `nbContractRecords`,
  `resultBinding`, `mediaPlayer`, `expiryDateFormat`, `enableButton`, `message`, `inputDataJson`, `outputJson`,
  `readerNamePattern`, `transaction`, `cardSelectorCount`, `baseTheme`...).
- README ("Method Names"): a name without verb promises a value without hidden cost (access to the card, the network
  or the storage, heavy computation), and a Kotlin value without parameter nor computation is a property.
- Control app: the success and error sounds are played by a dedicated thread (`MediaPlayerUserFeedback`), the start
  of a `MediaPlayer` no longer blocking the thread of the control, and the card detection is stopped once, by the card
  reader screen when it is left, instead of being stopped again on the main thread by the result screens. The
  operations on the readers (initialization, start and stop of the card detection, release) are executed one after
  the other by a dedicated thread instead of the main thread: the stop of the card detection, which takes about 440 ms
  on the Bluebird terminals (end of the monitoring of the card removal and stop of the NFC reader), no longer delays
  the display of the result screen. The result screens are opened without the transition animation of the system.
- Control and validation apps: the waiting indicator ("Please wait") is removed. In the control app, displayed without
  transition for the short duration of the control of a card, then hidden before the opening of the result screen, it
  flashed on the screen (since the replacement of the deprecated `ProgressDialog`); it is no longer displayed during
  the initialization of the readers either, as in the validation app (overlay of the layouts and `please_wait` text
  removed).
- Reloading Android client: the "Wearable" and "Embedded Secure Element" card media, which had no reader, are
  removed (`CardMedium.WEARABLE` and `EMBEDDED`, buttons of the home screen, settings of their visibility, texts and
  images). Selecting them led to an empty card presentation screen, or to a "reader not found" error after the
  registration of the OMAPI plugin, with fictitious reader names, plugin types and AIDs. The client offers the
  contactless card (NFC or Bluebird reader) and the SIM card (OMAPI reader).
- Field names (fourth audit): the loading route of the multiplatform client names `productType` the type of the
  product (instead of `type`) and no longer carries its unused price; the .NET client names `reader` its PC/SC reader
  (instead of `cardService`).
- Field names (third audit): the serial number of the card passed between the screens of the reloading Android
  client is named `APPLICATION_SERIAL_NUMBER` (instead of `CARD_APPLICATION_NUMBER`, the application number being
  another data of the card); the multiplatform client names `SERVER_HOST_KEY` and `DEFAULT_SERVER_HOST` the host of
  the server (instead of `SERVER_IP_KEY` and `DEFAULT_SERVER_IP`); the failure feedback of the terminals is named
  `failureMedia`, `SOUND_FAILURE` and `LEDS_FAILURE` (instead of `errorMedia`, `SOUND_ERROR` and `LEDS_FAILED`), as
  `signalFailure()`; `ANIMATION_DURATION_MS` (instead of `ANIMATION_DURATION`) in the top bar of the multiplatform
  client. The values of the extras of the reloading Android client are in camelCase (e.g. `tripsToLoad` instead of
  `TRIPS_TO_LOAD`), and the keys of the preferences of the reloading clients are in snake_case without prefix nor
  suffix (e.g. `server_ip` instead of `server_ip_key`, `contactless_visibility` instead of
  `setting_contactless_visibility`): the settings saved by a previous version are not read again.
- Field names (second audit): `host` and `port` in the state of the server settings screen of the multiplatform
  client (instead of `serverHost` and `serverPort`, as `protocol`, `endpoint` and `KeypleServerConfig`),
  `recordNumber` in the contracts of the control app (instead of `record`, which was a number), `NON_KEYPLE_PLUGIN`
  on the server (instead of `CUSTOM_PLUGIN`, the plugin name recorded for the .NET client); the constants of the .NET
  client are in PascalCase as the convention of C# (e.g. `ExecuteRemoteServiceAction`, `IsContactlessService`,
  `ApiLevel`, instead of `EXECUTE_REMOTE_SERVICE`, `IS_CONTACTLESS`, `API_LEVEL`); the sizes of the fields of the
  codecs, in bits, are suffixed by `_BITS` (e.g. `CONTRACT_TARIFF_SIZE_BITS`), as the sizes in bytes by `_BYTES`; the
  control and validation apps recognize the simulated plugins by the `MOCK_PLUGIN_MARKER` constant (instead of a
  `mock` field).
- Field names harmonized:
  - glossary and readability: `CHOOSE_CARD_MEDIUM_FOR_PERSONALIZATION` (instead of `CHOOSE_DEVICE_FOR_PERSO`) in the
    reloading Android client, `seasonPassValidityEndDate` (instead of `passValidityEndDate`, also in the log of the
    validation), `VALIDATION_RESULT_DELAY_MS` (instead of `SUMMARY_DELAY_MS`) and `cardDetectedAt` (instead of
    `cardInsertedAt`) in the validation app, `ENV_VERSION_NUMBER_SIZE` and `ENV_APPLICATION_NUMBER_SIZE` (instead of
    `ENV_EVN_SIZE` and `ENV_AVN_SIZE`) in the codecs and `EventStructure.eventDateTime` (instead of `eventDatetime`)
    in the common library;
  - no type in the names: `contracts` and `lastValidations` in the UI models of the control app and of the reloading
    Android client (instead of `contractsList` and `lastValidationsList`), `locations` in `LocationRepository`
    (instead of `locationList`), and `cardSecuritySetting` in the validation app (instead of `cardSecuritySettings`,
    the property being null until a SAM is selected);
  - booleans and units: `isBatteryPowered` (validation app), `isServerReady` (`AppState` of the multiplatform client),
    `shouldCloseSession` (validation app), `wasServerReady` (reloading clients, instead of `lastServerReady`) and
    `validationPeriodMinutes` (control app, instead of `validationPeriod`). The keys of the preferences of the
    reloading Android client are all suffixed by `_KEY` (e.g. `CARD_MEDIUM_KEY`, `CONTACTLESS_VISIBILITY_KEY`), the
    last known readiness of the server being stored under `was_server_ready`;
  - `SAMPLE_RATE` constant of the desktop buzzer of the multiplatform client (instead of a variable),
    `PERMISSIONS_REQUEST_CODE` (instead of `MY_PERMISSIONS_REQUEST_ALL`) in the control and validation apps,
    `CalypsoFiles.SFI_EVENT_LOG` (instead of `SFI_EVENTS_LOG`), `preferences` (instead of `prefs`) in the reloading
    Android client, and `FAILURE` status of the failed transactions displayed by the dashboard (instead of `FAIL`).
- Method names (fifth audit): the server creates the card transaction managers with `createCardTransactionManager()`
  (instead of `initCardTransactionManager()`), as the card selection manager (`createCardSelectionManager()`); the
  README describes the `create...` verb.
- Method names (fourth audit): the multiplatform client executes the remote services with `executeRemoteService()`
  and `executeRemoteServiceWithStatus()` (instead of `executeService()` and `executeServiceWithStatusOutput()`), as
  the Android and .NET clients; the .NET client processes the commands of the server with `ProcessServerCommands()`
  (instead of `ProcessTransaction()`, "transaction" designating the card and dashboard transactions); the server
  builds the environment of a personalized card with `buildEnvironmentHolder()` (instead of
  `buildEnvironmentHolderStructure()`, as `buildEvent()`); the documentation of the readiness checks of the server
  refers to the availability of the SAM instead of its "status".
- Method names (third audit): the .NET client builds the responses of the remote reader with
  `CreateIsContactlessResponse()` and `CreateIsCardPresentResponse()` (instead of `IsContactless()` and
  `IsCardPresent()`, which returned a JSON text and not a boolean); the description of the loaded contract of a
  transaction is named `loadedContract` (server and dashboard, JSON key included) instead of `contractLoaded`, which
  suggested a boolean; the cache of the multiplatform client also clears the serial number of the last card read
  (`ReadCardCache.clear()`, which only cleared its contracts). Harmonized names: `describeContract()` on the server (as
  `describeLoadedContract()`, instead of `formatContractStructure()`), `updateServerReadinessIndicator()` and
  `shouldFinishActivityAfterResult()` in the reloading Android client (instead of `updateServerReadyIndicator()` and
  `isFinishActivityAfterResult()`), and `AppState.serverReady` in the multiplatform client (instead of `serverOnline`).
- Method names (second audit): the result screens of the reloading Android client are displayed by
  `displayResult()` (instead of `changeDisplay()`), as in the control and validation apps; the control and validation
  apps request the missing permissions with `PermissionHelper.requestMissingPermissions()` (instead of
  `checkPermission()`, which did not tell that it requests them); the "Server state" button of the settings of the
  reloading Android client tests the readiness of the server with `testServerReadiness()` (instead of `ping()`) and
  takes into account the availability of the SAM, which was ignored: it displays "Server ready" or "Server not
  ready" (instead of "Ping succeeded" even when the SAM was not available), as the indicator of the toolbar. The
  multiplatform client sets the message of the NFC reader with `setScanMessage()` (instead of `updateReaderMessage()`)
  and no longer contains the unused `Success` composable of the loading screen, and the validation app sorts the
  contracts with `sortContractPriorities()` (instead of `sortContractPrioritiesByPriority()`). The README states that
  a value computed without side effect may be named after this value, and that the ports of the domain are named
  after the operations of the glossary.
- Method names harmonized (README section "Method Names"):
  - names which were misleading: the reading screen of the reloading Android client displays its progress with
    `showReadInProgress()` (instead of `showNowLoadingInformation()`, "load" designating the loading of a contract),
    the loading and personalization screens with `showLoadingInProgress()` and `showPersonalizationInProgress()`, and
    the instructions with `showPresentCardInstructions()` (also for the SIM cards, instead of
    `showPresentNfcCardInstructions()`); the multiplatform client applies the server settings with
    `applyServerSettings()` and an "Apply" button (instead of `restartServer()` and "Restart", the server not being
    restarted) and waits for a card with `waitForCard()` only (instead of `waitCard()` and `waitForCard(callback)`);
    the server returns the transactions with `getTransactions()` on `GET /transactions` (instead of `getEvents()` on
    `/transactions/events`, an event being a record of the card);
  - display verbs: the validation app displays its result with `displayResult()` as the control app (instead of
    `changeDisplay()` and `showValidationResult()`), its overlay with `showResultOverlay()` and `hideResultOverlay()`,
    and both apps hide their progress with `hideProgress()` (instead of `dismissProgress()`). The user feedback of the
    terminal (`UserFeedback`, `FeedbackDevice`) is given with `signalSuccess()`, `signalFailure()` and
    `signalWaiting()` (instead of `displayResultSuccess()`, `displayResultFailed()` and `displayWaiting()`), and the
    security setting of the card transactions is created by `createSecuritySetting()` in both apps;
  - reloading Android client: `executeRemoteService()` (instead of `remoteServiceExecution()`), `showError()`,
    `showStatusError()`, `showInvalidCard()`, `showCardCommunicationError()` and `showServerError()` (instead of
    `launch...Response()`), `initCardReader()` and `releaseCardReader()` (instead of `initAndActivateCardReader()` and
    `deactivateAndClearReader()`), `setUpCardMediumButton()` (instead of `setupBtn()`), and `KeypleSyncEndpointClient`
    (instead of `KeypleSyncEndPointClient`, after the Keyple `SyncEndpointClientSpi`);
  - multiplatform client: `selectAppAndPersonalizeCard()` as the two other remote services (instead of
    `personalizeCard()`), `launchCardSelectionScenarioRetrieval()` and `retrieveCardSelectionScenario()`, Compose
    functions `SuccessMessage` and `PlatformAnimation` (instead of `DisplaySuccess` and `ScreenAnimByPlatform`), screen
    states `DisplaySuccess` and `DisplayCardContent` (instead of `ShowTransactionSuccess` and `ShowCardContent`), and
    `playTone()` on desktop (instead of `tone()`);
  - .NET client: PascalCase methods `GetInstance()` and `TransmitRequest()` (instead of `getInstance()` and
    `transmitRequest()`), `GetInstance()` also for the provider of the ticketing service (instead of `GetService()`),
    and `WaitForCard()` in the API, the reader SPI and the application (instead of `WaitForCardInsertion()` and
    `WaitForCardPresent()`); `HexStringToByteArrayConverter.StringToByteArray()`, which only called
    `HexUtil.ToByteArray()`, is removed;
  - server and dashboard: `buildSeasonPassContract()` (instead of `buildSeasonContract()`), `findReader()` (instead of
    `searchReader()`), `initCardResourceService()` (instead of `setupCardResourceService()`), `decodeCardData()`
    (instead of `parse()`); the availability of the SAM is named in the same way by the server, the dashboard and the
    clients: endpoint `GET /card/sam-availability` returning `isSamAvailable` (instead of `/card/sam-status` returning
    `isSamReady`), `getSamAvailability()`, `SamAvailability`, `fetchSamAvailability()`, and "SAM resource is
    available" in the header of the dashboard (instead of "ready");
  - validation app: the rules returning the reason of a refusal are named `environmentRejection()`,
    `eventVersionRejection()` and `contractVersionRejection()` (instead of `check...()`, which suggested a boolean or
    an exception); Android applications: `initTerminalReader()` (instead of `initTerminalType()`); common library: the
    tests of the codecs are named after the tested case (e.g. `decodeEnvironmentWithHolder()` instead of
    `decodeEnv1()`).
- Control and validation apps: the dialog displayed when the readers cannot be initialized shows "The readers of the
  terminal could not be initialized." instead of the message of the exception (method `showReaderInitializationErrorDialog`,
  instead of `showNoProxyReaderDialog`, a term of a former version of Keyple), and the authentication failures of the
  MIFARE Classic cards are recognized by their type (`SCAuthenticationFailedException`) instead of the word "auth" in
  the message of the exception.
- The last technical messages displayed are replaced: the NFC sheet of the iOS client displays "Card reading error"
  (instead of "Error: " and the message of the error, which is logged), and the .NET client no longer displays the
  message of a fatal exception after the text referring to its log file. The browser console of the dashboard follows
  the convention of the log messages ("Server ready", "Server not ready [cause=...]", availability of the SAM logged
  when it changes instead of at each check every 3 s), and the CI checks the JavaScript sources of the dashboard too.
- The Android applications no longer display the messages of the exceptions: an unexpected technical error is
  displayed with the generic text of the application ("An error occurred while reading the card." in the control app,
  "Please try again." in the validation app, "An error occurred" or "Server error" in the reloading client), its
  cause being logged; the `detail` of the `Failed` results of the control and validation domains is removed, and the
  error screen of the reloading client receives a text (`showError`) instead of an exception built to carry
  it (`launchExceptionResponse`).
- Exception messages written with the template of the log messages (README section "Exception Messages"): English
  sentence without final punctuation nor class or method name, variable values in the context (e.g. "Reader not
  found [reader=...]" instead of "Reader '...' is not found in the list of readers." or "OpenPhysicalChannel: ..."),
  cause chained instead of copied in the message (control app, .NET client), specific types instead of `Exception`
  and `RuntimeException`; the exception classes of the server are "Card not personalized", "Environment expired"
  and "Card full: ...". The server returns the same messages of the statuses of its remote services to the
  multiplatform and .NET clients (new `RemoteServiceMessages` class, texts of the Android applications), instead of
  two vocabularies and of exception messages ("Runtime exception: ..."); the multiplatform client displays these
  messages, or "Communication error", "Card reader error" and "Unexpected error", instead of technical texts
  ("Server side error: 7 / ...", "Error: ..."). The CI checks the exception messages too (`check-messages.py`, job
  "Log and exception messages").
- CI: the log messages of all the components are checked against the convention of `README.md` ("Log Messages") by
  the new `check-messages.py` script (job "Log and exception messages"): form of the messages, warnings without stack trace,
  catch blocks logging, throwing or explaining in a comment why they do not log (comments added to the catch blocks
  of the refusals of the server, of the removal of the card in the validation app and of the readiness of the
  server).
- Log messages (sixth audit): the incidents without consequence of the end of the card processing and of the stop
  of the card detection are logged at the `WARN` level with their cause instead of their stack trace, and the failures
  of the remote services returned by the interop library (multiplatform client) at the `WARN` level ("Remote service
  execution failed") instead of the `ERROR` level, as described by the convention.
- Log messages (fifth audit): the errors which were silently ignored are logged: stop of the card detection
  (validation app, at the `WARN` level as in the control app, which logged it at the `ERROR` level), wait for the card
  (multiplatform client), wait for a card and errors of the card or of the reader returned to the server (.NET client).
- Log messages (fourth audit): the results of the reloading operations are logged with the same messages by the
  three clients ("Contract loaded", "Contract loading failed", "Card personalized", "Personalization failed" in the
  multiplatform client instead of "Remote service executed/failed"; "Contracts reading failed" and "Trips loading
  failed" in the .NET client instead of "...not read" and "...not loaded"); the `TRACE` level is no longer used (its two
  server messages, removed by the minimum level of Quarkus fixed at build time, are logged at the `DEBUG` level); the
  README describes where to read the logs of each component and how to see the `DEBUG` messages, and the inexact
  logging instructions of the server, .NET client and reloading Android client READMEs are corrected.
- Log messages (third audit): the reloading clients log the result of each operation (contracts read, contract
  loaded, card personalized, or failure with its status), the reading, loading and personalization use cases of the
  Android client receiving the `Logger` port; the multiplatform client logs a refusal of the server at the `WARN`
  level (instead of "Service executed" at the `INFO` level) and no longer gives an explicit tag; the readiness of the
  server is logged by the two clients when it changes only ("Server ready", "Server not ready" with its cause), instead
  of a warning every 5 s (multiplatform client) or nothing (Android client).
- Log messages (second audit): the results of the validation and of the control (card validated, controlled, refused,
  removed, not selected) are logged once by the `ValidateCard` and `ControlCard` use cases, whatever the type of card
  (the Calypso validation procedure logged nothing, the storage card one logged its results, and the card removal
  was logged twice); the Calypso validation procedure logs its technical errors; the .NET client logs messages
  following the convention, its console texts being no longer copied in the log; the same keys are used for the
  same information (`readerNameRegex`, `file`, `eventType`).
- Log messages written in all the components with the same template, `Message [key1=value1, key2=value2]`, described
  in `README.md` (section "Log Messages"): English sentence without final punctuation and without variable value, the
  values being given in the context with the terms of the glossary, no class or method name. The refusals of the
  cards (card not personalized, environment expired, card full, file structure not supported, different card) are
  logged once at the `WARN` level where they are detected, instead of an `ERROR` "An error occurred while..." in each
  service; the card communication errors are logged at the `WARN` level with their cause, the unexpected errors at the
  `ERROR` level with their stack trace. The technical details of the server (secure session, blocks read or written)
  are logged at the `DEBUG` level, a single `INFO` line being written per operation (instead of the `=== ... ===`
  banners). The errors of the reloading Android client are logged with their context (instead of `Timber.e(e)`
  alone), the exceptions are attached to the messages of the Android applications (instead of their message copied
  in the text), and an exception thrown again is no longer logged twice. The commented logs of the card content are
  removed from the server.
- Vocabulary harmonized with the glossary (eighth audit): the serial number of the card is named
  `cardSerialNumber` in the multiplatform client (instead of `cardSerial`, also in the `LoadContract` route and in
  `ReadCardCache`) and `applicationSerialNumber` in the server (instead of `appSerialNumber`); the dimension of the
  validation result overlay is `validation_result_desc_top_margin` (instead of `summary_desc_top_margin`); the
  activities of the control app take the name of the application (their labels were their class names); the
  unused `date` of the products of the multiplatform client is removed. The comments follow the glossary
  (terminal selection, card content, invalid card, card medium, user feedback, instead of "device", "present",
  "network invalid", "UI manager"), the `KeypleTerminalReaders` adapters of the three applications have the same
  description, and the reloading Android client names `isBluebirdTerminal` the detection of a Bluebird terminal
  (instead of `isBluebirdDevice`).
- Vocabulary harmonized with the glossary (seventh audit): the theme and the top bar of the multiplatform client
  are named `AppTheme` and `AppTopBar` (instead of `KeypleTheme` and `KeypleTopAppBar`), the "Keyple" prefix
  being kept for the integration of the Keyple libraries; the .NET client names its service API
  `TicketingServiceApi` (instead of `MainServiceApi`, also for its adapter and its provider, whose method is
  `GetInstance`) and the input data of its services `ReadContractsInputData` and `IncreaseContractCounterInputData`
  (instead of `InputDataRead` and `InputDataWrite`); the CI jobs of the multiplatform client are named
  "Reload Interop (KMP, ...)"; the text styles of the reloading Android client follow the naming of the other
  styles (`smallRegularDarkBlue`, `smallMediumDarkBlue`, `smallBoldDarkBlue`, `smallRegularGrey`, instead of
  `buy_text_info_*`).
- Vocabulary harmonized with the glossary (sixth audit): the status of an expired environment is
  `RemoteServiceStatus.ENVIRONMENT_EXPIRED` (instead of `EXPIRED_ENVIRONMENT`, same code), as the
  `RejectionReason.ENVIRONMENT_EXPIRED` of the Android applications, and the exception of the server
  `EnvironmentExpiredException`; the reloading Android client names the texts of these refusals as the control and
  validation apps (`rejection_environment_wrong_version`, `rejection_environment_expired`, instead of
  `card_not_personalized` and `expired_environment`), and its "Embedded Secure Element" text
  `embedded_secure_element`; the label of `PriorityCode.FORBIDDEN` is "Forbidden"; the server names the
  personalization and the loading in its comments and logs (instead of "init", "initialization" and "issuing");
  "MIFARE Classic" in the comments, logs and documentation; the iOS application is identified by
  `org.calypsonet.keyple.demo.reload.remote.interop` (as the Android application of the multiplatform client).
- Vocabulary harmonized with the glossary (fifth audit): the glossary of `README.md` describes the operations
  (reading, loading, renewal of a season pass, personalization, validation, control) and the use of "reload",
  "issuance", "initialization" and "write"; the documentation of the reloading demo uses them (personalization and
  loading procedures, secure sessions named after their `PERSONALIZATION` and `LOAD` access levels, instead of
  "issuance", "Issuer session" and "Reload session"). Contract models: `remainingTrips` in the reloading Android client
  (instead of `counterValue`), `saleDate`, `validityEndDate`, `isValidated` and `isExpired` in the control app (instead
  of `contractValidityStartDate`, `contractValidityEndDate`, `valid` and `expired`), `isValid` in the UI contract of the
  reloading Android client. Displayed texts: "Quit" (instead of the French "Quitter"), "MIFARE", "DD" date
  placeholders, "No ticket" for an empty card in all the applications, card verification code named CVC, payment
  expiry date in the future, capitalization of the server settings and of the contract states. Dashboard: transaction
  table `TransactionTable` (column "Type"), without the commented code of its template. The README of the control and
  validation apps only describe the actual features (settings, screens, terminals, activation of the proprietary
  plugins).
- Vocabulary harmonized with the glossary (fourth audit): the loading of a contract is no longer called
  "reload" (comments of the use case and of the server, renewal of a season pass, `loadContractBtn` instead of
  `chargeBtn`, texts of the .NET client which loads trips instead of "units"); the result overlay of the
  validation app is named `layout_validation_result_overlay` (instead of `layout_card_summary_overlay`), as its
  `ValidationResult` model, and its unused `activity_main` landscape layout is removed; the server takes the labels of the products from `PriorityCode` in the JSON API; the
  client methods are named after the services they call (`selectAppAndAnalyzeContracts`,
  `selectAppAndLoadContract`, `SelectAppAndReadContracts`, `SelectAppAndIncreaseContractCounter`); the
  applications are named "Keyple Demo - Reload (Interop)", "Keyple Demo - Reload (.NET)", "Keyple Demo - Reload
  Server" and "Keyple Demo - Reload Dashboard", as the Android applications; in the multiplatform client, the
  settings menu is `SettingsMenuScreen` (route `SettingsMenu`), as `SettingsMenuActivity`, and the card content
  screen is in the `cardcontent` package. Reloading Android client: identifiers of the "Card media" entry of
  the settings menu (`cardMediumBtn`), accessibility description of its "Personalization" entry, and
  `no_contract_label`/`no_contract_desc` texts as in the validation app.
- Screens named after what they do, as the use cases and the multiplatform client. Reloading Android client:
  `ReloadActivity` becomes `LoadContractActivity`, `PersonalizationActivity` `PersonalizeCardActivity`,
  `CardSummaryActivity` `CardContentActivity` (package `cardcontent`, as in the control app), `ReloadResultActivity`
  (result of a loading or of a personalization) `CardOperationResultActivity`, and `ConfigurationSettingsActivity`
  (visibility of the card media) `CardMediumSettingsActivity` ("Card media" in the settings menu), with their
  layouts; the UI model of a card result is `UiCardResult` (instead of `UiCardReaderResponse`). Multiplatform
  client: routes `CardContent` and `Success` (instead of `Card` and `AppSuccess`), and `ServerSettingsScreen`
  (instead of `ServerConfigScreen`), as in the Android client.
- Android applications: the values of `CardProtocol` are named without the redundant `_LOGICAL_PROTOCOL` suffix
  (e.g. `ISO_14443_4`), the enum being documented as the logical protocols of the cards.
- Android applications: each application stays standalone (no shared Android module), and the technical files
  duplicated between the applications which must stay identical are checked by the CI
  (`.github/scripts/check-android-twin-files.sh`).
- Android applications: the messages logged through the `Logger` port are tagged in Logcat with the name of the
  calling class (they were all tagged `LoggerImpl`).
- Android applications: card processing is now finalized with `ObservableCardReader.finalizeCardProcessing()` after
  each card.
- Android applications: when the storage card extension is not available (e.g. mocked library), the storage cards are
  not supported and a warning is logged once, instead of failing.
- Android applications: the coroutines launched by the activities are bound to their lifecycle (`lifecycleScope`
  instead of `GlobalScope`), and deprecated Android APIs are replaced (`ProgressDialog`, `startActivityForResult`,
  `onBackPressed`, `Resources.getColor`, `Intent.getParcelableExtra`).
- Control and validation apps: the plugins are registered by a blocking call made on the IO thread of the activity,
  instead of wrapping the whole registration in `runBlocking`. Only the Arrive plugin factory, provided by a suspend
  function, is awaited with `runBlocking` in the adapter. Reloading Android client: the plugins are registered
  directly, as before the refactoring.
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
    `DataModule`, and `ServerModule` (formerly `RestModule`) providing the communication with the server (Keyple
    Distributed local service, ticketing server, server status), `ReaderModule` only providing the readers.
- All the applications: same ticketing vocabulary, documented in the README ("Ticketing vocabulary"):
  - `Contract` instead of "title" (translation of the French "titre de transport") for the contracts of the card:
    `ContractsRecyclerAdapter`, `contract_recycler_row.xml`, `contractsList`, and in the reloading Android client
    `Contract`, `UiContract`, `ContractMapper` and `ContractUiMapper` (`CardTitle`, `UiCardTitle`...);
  - `Product` and `ProductType` (values `MULTI_TRIP` and `SEASON_PASS`) for the products sold by the KMP client
    (`Title`, `TitleType` with `SINGLE` and `SEASON`), loaded through the `LoadContract` route (`WriteTitleCard`);
  - `remainingTrips` and `tripsToLoad` for the trips of a multi-trip contract (`nbTicketsLeft`, `ticketsToLoad`), also
    in the JSON API (`tripsToLoad` instead of `ticketToLoad`) and `name` instead of `title` for the contracts
    returned to the KMP client;
  - `contracts` instead of `validContracts` for the contracts returned by the contracts analysis (JSON API and
    Keyple distributed DTOs), which also include the expired contracts;
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
- `keyple-java-bom`: `2026.03.19` -> `2026.10.14` (with `keyple-card-calypso-java-lib` `3.3.0`, which fixes the
  session signature of the PKI cards when the CA certificate is not registered, and
  `keypop-calypso-card-java-api` `2.3.0`)
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
    `UiCardResult`, layout and resources).
  - Reloading remote KMP client: unused Compose resources (images, animation, strings), colors and navigation helper,
    and the unused `Scan` route with its `ScanNavArgs` arguments.
  - Reloading remote .NET client: unused `MessageDto` setters, unused `WaitForCardAbsent()` and `IsCardPresent()`
    methods of the PC/SC reader adapter, and unused `Serilog.Sinks.Console` and
    `Microsoft.Extensions.Logging.Abstractions` packages.
  - Common library: unused `CardConstants.SC_ENVIRONMENT_AND_HOLDER_SIZE_BYTES` constant.
  - Reloading remote server dashboard: unused `@testing-library/*` and `web-vitals` dependencies.
### Common library
#### Added
- `RemoteServiceStatus` enum defining the status codes of the remote services (`statusCode` field of
  `AnalyzeContractsOutputDto`, `LoadContractOutputDto`, `PersonalizeCardOutputDto` and of the `SelectAppAnd...OutputDto`
  of the Server JSON API, still transmitted as an integer), with the new `CARD_FULL` status (`7`): no empty contract
  record nor expired or exhausted contract to replace.
#### Changed
- `Location.toString()` now returns the name of the location.
- Packages: `Location` and `LocationRepository` move to the new `reference` package (reference data of the
  network), instead of `model` (data structures of the card) and `data` (a name used by the layer of the adapters in
  the applications); `RemoteServiceId` moves from `constants` to `dto`, which gathers the protocol of the remote
  services (service identifiers, input and output data, status codes).
- Immutable model (used by all the applications and the server):
  - `EnvironmentHolderStructure`, `EventStructure` and `ContractStructure` are `data class` with read-only properties,
    modified by copy (`copy()`, or `EventStructure.withContractPriority()` and `ContractStructure.withCounterValue()`,
    also usable from Java);
  - the counter value of a contract is a property of its constructor (it was declared apart, and ignored by `equals`);
  - the four `contractPriority1..4` fields of the event are replaced by the `contractPriorities` list (4 elements,
    checked at the creation), read with `getContractPriority(contractNumber)`; `eventDatetime` is computed on demand;
  - `DateCompact` and `TimeCompact` are `data class` (instead of hand-written `equals` and `hashCode`), the date of a
    `DateCompact` being provided by the `date` property (`getDate()` for Java);
  - `PriorityCode` and `VersionNumber`: `code`, `label` and `fromCode()` instead of `key`, `value` and `findEnumByKey()`,
    as in `RemoteServiceStatus`.
- The parsers are replaced by codecs: the `RecordCodec` interface (`decode`, `encode`) instead of `Parser` (`parse`,
  `generate`), implemented by one object per structure and card technology (`codecs` package):
  `CalypsoEnvironmentHolderCodec`, `CalypsoEventCodec`, `CalypsoContractCodec`, and for the storage cards
  `StorageCardEnvironmentHolderCodec`, `StorageCardEventCodec`, `StorageCardContractCodec` (the `Sc...Parser` classes).
  The tests are renamed accordingly (`CalypsoContractCodecTest`...).
- `CardConstants` is split into objects by theme: `CalypsoAids` (with `matches()` instead of `aidMatch()`),
  `CalypsoFiles`, `DefaultKifs`, `StorageCardBlocks`, `MifareClassicBlocks` and `PkiTestCertificates`, accessed from
  Java as static fields.
- Documentation of the status codes of `AnalyzeContractsOutputDto`, `LoadContractOutputDto`, `PersonalizeCardOutputDto`
  and of the `SelectAppAnd...OutputDto` aligned with the codes actually returned by the server.
#### Removed
- `java.io.Serializable` interface of the structures, not used (they are only transmitted as JSON).
#### Upgraded
- Kotlin: `1.7.22` -> `2.3.21` (same version as the other projects).
- Java 17 as source and target level instead of Java 8, as the applications using the library: building it no
  longer requires a JDK 8.
### Validation app
#### Added
- Anti-passback check for storage cards: a card presented again within the anti-passback delay is rejected.
#### Changed
- The user feedback (sounds, LEDs) is provided by the dedicated `UserFeedback` port instead of the reader port. The Arrive
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
- Calypso cards: when no contract can be used, the priority `31` set to the expired or exhausted contracts is now
  written in the event (the secure session was cancelled, the card being refused), so that the next validations no
  longer analyze these contracts.
- A card that cannot be selected (unknown AID, unexpected DF name, unsupported file structure) is refused with the
  selection error, instead of executing the validation procedure anyway (on the previous card when no application was
  found).
- Bluebird terminals running Android 13 or later: the Bluebird reader can be selected again. The storage permission,
  requested with the SAM access and refused without prompt since Android 13, is only declared and requested up to
  Android 12. The unused `WRITE_EXTERNAL_STORAGE` permission is removed.
- Calypso cards: contracts of unknown type (contract tariff not supported by the demo) are no longer validated (they
  were accepted without any debit).
- Calypso cards: the type of the contract used (multi-trip ticket, season pass) is the `ContractTariff` of the contract
  read, as specified, instead of its `ContractPriority` in the event, which only gives the order of the contracts. Both
  are equal for a consistent card; a card whose event and contract differ is now validated according to the type of
  the contract. In the storage card procedure and the business rules, the contract type is named `contractTariff`
  (instead of `contractPriority` and `priority`).
- Anti-passback: a card validated again a few seconds after a first validation is refused. The time of the event being
  recorded to the minute, it was compared with the exact time of the new validation: a validation at 10:57:55
  (recorded as 10:57) let a second one be accepted at 10:58:00, debiting a trip twice. The times are now compared to
  the minute, the card being refused until the end of the minute following the delay (effective delay between 1 and 2
  minutes, never shorter than the configured one), for the Calypso and storage cards.
### Control app
#### Changed
- Major architectural refactoring to apply the layered architecture with ports and adapters, isolating the business
  logic (domain) from the Android and UI implementations.
- The locations are provided by the `common` library (local `locations.json` file removed), and are displayed by their
  name only.
- The SAM selection no longer filters the SAM on its power-on data (SAM C1).
- The user feedback (sounds) is provided by the `UserFeedback` port, and the settings are accessed through the
  `AppSettingsRepository` port.
- `clear()` is no longer part of the `TerminalReaders` port, being only used by its implementation (as in the other
  applications).
#### Removed
- Values of the `Status` enum never produced by the control procedure (`LOADING`, `SUCCESS`, `WRONG_CARD`,
  `DEVICE_CONNECTED`).
- Gson dependency, not used by the application (still provided to the Keyple libraries by their own dependencies), as
  in the validation app.
#### Fixed
- The failure feedback (sound) of a refused card or of an error is given once (it was given twice).
- Bluebird terminals running Android 13 or later: the Bluebird reader can be selected again. The storage permission,
  requested with the SAM access and refused without prompt since Android 13, is only declared and requested up to
  Android 12. The unused `WRITE_EXTERNAL_STORAGE` permission is removed.
- The waiting indicator is now hidden when the control procedure fails, instead of staying displayed when returning to
  the reader screen.
- Validation period: a validation is considered valid until the end of the minute of the end of the validation period.
  The time of the validation being recorded to the minute, the period was compared with the exact time of the control,
  and could end up to 59 seconds too early.
### Reloading remote Android client
#### Changed
- Major architectural refactoring to apply the layered architecture with ports and adapters: the plugins and readers
  management is moved from the activities to `KeypleTerminalReaders`, and the remote services are called through the
  `TicketingServer` port.
- The business logic is moved from the activities to the domain: card selection (AIDs depending on the device),
  interpretation of the server status codes, building of the contracts and check that the reloaded card is the one
  read before. The activities no longer handle the Keypop card types.
- The settings and the server status are accessed through the `AppSettingsRepository` and `ServerReadinessChecker` ports,
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
- The error screens return automatically to the previous screen after 5 seconds, as the success screen does: the card
  presentation screen to try again (or the home screen when it was closed), unless the user chooses before with the
  buttons. This applies to the reload and personalization result in error, and to the card summary for an invalid card
  or a technical error. The automatic return uses a coroutine bound to the screen lifecycle instead of a `Timer`.
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
- Transaction monitoring: the long polling endpoint `/activity/events/wait` is replaced by the Server-Sent Events stream
  `/transactions/stream`, which broadcasts each new transaction to all the subscribers.
- Dashboard: the entry page (`index.html`) is no longer cached by the browsers (`Cache-Control: no-cache`), so that a
  server update is taken into account immediately; the built assets, whose names change at each build, remain cached.
- The status codes of the remote services are produced with the `RemoteServiceStatus` enum.
- Server JSON API: same status codes for all the services. `SELECT_APP_AND_READ_CONTRACTS` and
  `SELECT_APP_AND_INCREASE_CONTRACT_COUNTER` now return `4` (instead of `3`) for a card not personalized, `5` (instead
  of `4`) for an expired environment, and distinguish the card communication errors (`1`) from the other errors (`2`)
  instead of returning `1` for all errors. `SELECT_APP_AND_LOAD_CONTRACT` now returns the dedicated code `6` (instead
  of `2`) when the presented card is not the one read before.
- The card processing is split by business operation instead of a single `CardService` class:
  `ContractAnalysisService`, `ContractLoadingService` and `CardPersonalizationService` (Keyple clients),
  `JsonApiService` (JSON API: selection of the application by the server, then delegation to the previous services)
  and `ContractCounterService` (.NET client). The remote services are dispatched by `RemoteServiceDispatcher` with a
  `switch` on `RemoteServiceId` (`CardConfigurator` only setting up the Keyple infrastructure), the transactions of
  the dashboard are recorded by `TransactionRecorder`, and the selection of the application and the result messages
  of the JSON API are written once instead of three times. No functional change.
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
- Dashboard: each request of the .NET client gives exactly one transaction, recorded after the operation with its
  actual result: the reading of the contracts (`SELECT_APP_AND_READ_CONTRACTS`) is a `SECURED READ` transaction (it was
  recorded twice, once as a successful `SECURED READ` and once as a `LOAD`), and the increase of the contract counter
  (`SELECT_APP_AND_INCREASE_CONTRACT_COUNTER`) a `LOAD` transaction, with the description of the loaded trips only
  when it succeeds (it was recorded twice, once as a successful loading before the card was written).
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
  reload or personalization, and to the previous screen 5 seconds after an error (same action as the back arrow), the
  remaining time being shown by a bar emptied at the bottom of the screen. The back arrow of the success screen now
  replaces the screens of the transaction with the home screen instead of adding it on top of them.
- iOS: the Intel simulator target (`iosX64`) is removed, no longer supported by Compose Multiplatform 1.11 and
  Compottie: the application targets the iOS devices (`iosArm64`) and the Apple Silicon simulator
  (`iosSimulatorArm64`), and the related CI job is removed.
- The Android APK files are named through `base.archivesName` instead of an internal Android Gradle plugin API
  (`ApkVariantOutputImpl`). The debug APK keeps its name.
- Android target SDK defined by its own `androidTargetSdk` property (still `35`) instead of `androidCompileSdk`.
- The background tasks are no longer launched in `GlobalScope`: the retrieval of the card selection scenario
  (`KeypleInteropService`) and the tones of the desktop buzzer in a scope owned by their class, the periodic check of
  the server in the scope of `AppViewModel`.
- Code organized in two packages, as in the Android applications: `ui`, with one package per screen (`home`, `card`,
  `read`, `load`, `personalize`, `success`, `settings`) and the shared elements (`components`, `theme`, `nav`,
  `model`), and `data` for everything else: `dto` (DTOs of the JSON API), `service` (`KeypleInteropService`,
  `ReadCardCache`, `ServerConfigRepository`), `network` and `platform` (buzzer and data store, with their
  `expect`/`actual` declarations). `App` and `AppViewModel` stay in the root package. The `nfc` package (wrong name on
  desktop, where the cards are read through PC/SC) is removed, and the `Product` and `ProductType` models move out of
  the card content screen.
- The contract loading screen and its view model are named `LoadContractScreen` and `LoadContractScreenViewModel`
  (instead of `WriteCardScreen...`), and `KeypleInteropService.selectCardAndWriteContract` is renamed
  `selectAppAndLoadContract`, as the `LoadContract` route and the `SELECT_APP_AND_LOAD_CONTRACT` service.
- CI: the architecture check of the Android applications (`check-android-architecture.sh`) is not run on this
  client (new `check-architecture` input of the reusable Android workflow): it follows the MVVM pattern, its view
  models calling the data services directly, without domain layer.
- The DTOs of the JSON API are named as in the `common` library which they mirror (e.g.
  `SelectAppAndLoadContractInputDto` instead of `WriteContract`, `SelectAppAndPersonalizeCardInputDto` and
  `SelectAppAndAnalyzeContractsInputDto` instead of `GenericSelectAppInputDto`), and the outputs of the
  personalization and of the loading are read with their own DTO instead of a generic `OutputData`. The unused
  `InputData` and `InputDataIncreaseCounter` DTOs are removed. The exchanged JSON is unchanged.
- `KeypleInteropService` is limited to the integration of the Keyple Interop libraries (Keyple terminal, card
  selection scenario, local reader, remote services). The check of the server moves to `ServerReadinessChecker`
  (same name and contract as in the Android client, the SAM being now required for the server to be shown online),
  repeated every 5 seconds by `AppViewModel` with the current server configuration; the sound and vibration on card
  detection are triggered by the view models, the read card is kept in `ReadCardCache` by the reading screen, and
  the serial number of the card to load is provided by the loading screen (from its route).
- The server configuration (address, endpoint, basic authentication) is read and saved by a dedicated
  `ServerConfigRepository` instead of `KeypleInteropService`, and is loaded without blocking the main thread
  (`KeypleInteropService.start()` is a suspend function instead of using `runBlocking`).
- The `expect`/`actual` classes (beta feature of Kotlin) are explicitly enabled (`-Xexpect-actual-classes`), removing
  the related compilation warnings (and the related `@Suppress` annotations).
- Android: Java 17 as source and target level instead of Java 11, as in the other Android applications. The desktop
  application was already compiled for Java 17 (Kotlin toolchain).
- Android: code and resource shrinking (R8) enabled in the debug and release builds, as in the other Android
  applications (release APK of about 2 MB). The libraries provide their own R8 rules (e.g. the Ktor
  engine loaded by a service loader).
- The platform specific files of `DataStorePathProducer` are suffixed by their platform (`DataStore.android.kt`,
  `DataStore.desktop.kt`, `DataStore.ios.kt`), as the other `actual` files.
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