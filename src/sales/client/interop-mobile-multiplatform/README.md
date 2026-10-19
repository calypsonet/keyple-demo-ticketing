# Keyple Sales Demo — Kotlin Multiplatform client

[![Kotlin](https://img.shields.io/badge/kotlin-2.3-blue.svg)](https://kotlinlang.org/)
[![KMP](https://img.shields.io/badge/multiplatform-android%20%7C%20ios%20%7C%20desktop-green.svg)](https://kotlinlang.org/multiplatform/)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../../../LICENSE)

A Kotlin Multiplatform application demonstrating distributed remote client communications across Android, iOS, and
desktop platforms using the Keyple Interop KMP libraries (JSON API client and local reader) for seamless
cross-platform card operations.

[⬅️ Back to Main Project](../../../../README.md)

## Overview

This innovative client showcases the power of Kotlin Multiplatform by providing a single codebase that runs natively on
multiple platforms while maintaining full functionality with the Keyple server ecosystem. It demonstrates modern
cross-platform development practices for contactless card applications.

**Supported platforms**:
- **Android 8.0+** (API 26+) with native NFC support
- **iOS 15.3+** with Core NFC integration
- **JVM desktop** (Windows/macOS/Linux) with PC/SC readers

## Prerequisites

### Development environment
- **Android Studio** with Kotlin Multiplatform plugin
- **Xcode** (for iOS development on macOS)
- **JDK 17** to build the project (the desktop application runs on Java 17+)
- **Kotlin 2.3** with multiplatform support (provided by the Gradle build)

### Platform-specific requirements

#### Android
- Device with NFC capability
- Android 8.0+ (API level 26+)
- NFC enabled in system settings
- Android SDK installed locally, with a `local.properties` file at the project root pointing to it (see
  [Build from source](#build-from-source))

#### iOS
- iPhone with NFC support (iPhone 7+)
- iOS 15.3 or later
- Core NFC entitlements configured
- Apple Developer account for device deployment

#### Desktop
- **PC/SC compatible reader** connected via USB
- Platform-specific PC/SC libraries:
  - **Windows**: Built-in PC/SC support
  - **macOS**: PC/SC framework (usually pre-installed)
  - **Linux**: `pcscd` daemon and `libpcsclite-dev`

### Server requirements
- Running [Keyple Sales Demo server](../../server/README.md) with SAM integration
- Network connectivity from all target platforms

## Installation

### Download

From the [Releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases):
- **Android**: `kdt-sales-multiplatform-app-android-YY.MM.DD-debug.apk` (enable "Install from unknown sources" in
  the Android settings to install it)
- **Desktop**: the package of your system (`.dmg` for macOS, `.msi` for Windows, `.deb` for Linux), or the executable
  JAR of your system (`kdt-sales-multiplatform-app-...jar`), run with Java 17+
- **iOS**: not published, the application is built with Xcode (see [iOS development](#ios-development))

### Build from source

```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/sales/client/interop-mobile-multiplatform
```

Before building for Android, create a `local.properties` file at the root of this project
(`interop-mobile-multiplatform/local.properties`) pointing to your local Android SDK installation. This file is not
committed to the repository, so each developer must create their own:

```properties
sdk.dir=/path/to/your/Android/Sdk
```

On Windows, escape backslashes in the path, for example, `sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk`.

Without this file, Gradle fails with `SDK location not found`.

The build commands of each platform are given in [Building for different platforms](#building-for-different-platforms).

## Configuration

### Application configuration

The application configuration is managed through:
- Server settings UI for configuring the server address (protocol, host, port, endpoint) and the optional basic
  authentication
- Platform-specific datastore for persisting settings
- Shared network client for server communication

## Usage

### Mobile platforms (Android/iOS)

#### Initial setup
1. **Launch application**
2. **Configure server**: Tap the settings icon → **Server**, enter the address of the server, check it with
   **Server state**, then **Save**
3. **Grant permissions**: Allow NFC access when prompted

#### Card personalization
1. **Settings** → **Personalization**
2. **Present card** to device NFC reader
3. **Wait for confirmation** of successful personalization

#### Reading card content
1. **Home screen** → **Contactless card**
2. **Hold card** against device back (near NFC antenna)
3. **View existing contracts** and their status
4. **Select a new ticket** to load if desired

#### Contract loading
1. **Choose contract type** from available options
2. **Complete payment simulation**
3. **Present card again** when prompted
4. **Receive confirmation** of successful loading

### Desktop platform

#### Initial setup
1. **Connect PC/SC reader** via USB
2. **Launch application** with reader filter:
   ```bash
   ./gradlew :composeApp:run -PcustomArgs="-filter=ACS"
   ```
3. **Verify reader detection** in application logs

#### Card operations
1. **Place card** on connected PC/SC reader
2. **Follow same workflow** as mobile platforms
3. **Monitor operations** through desktop UI

### Shared user interface

The application uses Compose Multiplatform for consistent UI across all platforms:

```
┌─────────────────────────────────────┐
│            Home Screen              │
│  ┌─────────────────────────────────┐│
│  │         Settings menu           ││
│  │  ┌─────────────────────────────┐││
│  │  │  Server                    │││
│  │  │  Personalization           │││
│  │  └─────────────────────────────┘││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │      Card Operations            ││
│  │  • Read Card Content           ││
│  │  • Load a Contract             ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

## Technical architecture

### Multiplatform structure

```
composeApp/src/                   # Kotlin Multiplatform library, desktop and iOS applications
├── commonMain/                   # Shared business logic
│   └── kotlin/                   # App and AppViewModel (composition root of the shared UI)
│       ├── data/                 # Everything which is not UI
│       │   ├── dto/              # DTOs of the JSON API of the server (mirror of the common library)
│       │   ├── network/          # Server communication (HTTP client, server configuration)
│       │   ├── platform/         # Platform services (buzzer, data store), expect declarations
│       │   └── service/          # KeypleInteropService, ReadCardCache, ServerConfigRepository
│       └── ui/                   # Screens (one package per screen) and shared UI elements
│           ├── cardcontent/      # Card content and basket
│           ├── components/       # Shared components (top bar, scan screen, automatic return)
│           ├── home/             # Home screen
│           ├── load/             # Loading of a contract
│           ├── model/            # UI models (products proposed for sale)
│           ├── nav/              # Navigation routes
│           ├── personalize/      # Personalization of the card
│           ├── read/             # Reading of the card
│           ├── settings/         # Settings menu and server settings
│           ├── success/          # Success screen (animation by platform)
│           └── theme/            # Theme and colors
├── androidMain/                  # Android-specific implementations (buzzer, data store, success animation)
├── iosMain/                      # iOS-specific code
└── desktopMain/                  # Desktop-specific code
androidApp/src/main/              # Android application (activity, manifest, launcher icons)
```

The Android application is a separate module: the Android Gradle plugin 9 does not support the Kotlin
Multiplatform and Android application plugins in the same module.

### Key classes

#### Shared business logic (`commonMain`)

**Services** (`data/service`):
- `KeypleInteropService`: Integration of the Keyple Interop libraries (Keyple terminal, card selection scenario of
  the server, local reader, and remote services)
- `ServerReadinessChecker`: Checks that the server is reachable with its SAM available (as in the Android client), the
  check being repeated by `AppViewModel`
- `ReadCardCache`: Cache of the last card read (serial number, contracts)
- `ServerConfigRepository`: Server configuration persisted in the data store (address, endpoint, basic
  authentication)
- `SimpleHttpNetworkClient` (`data/network`): HTTP client for server communication

**DTOs** (`data/dto`):
- Inputs and outputs of the remote services of the JSON API (`SelectAppAndAnalyzeContracts...`,
  `SelectAppAndLoadContract...`, `SelectAppAndPersonalizeCard...`), named as in the `common` library which they
  mirror: `common` is a JVM library, which the multiplatform client cannot use
- `ContractInfo`: Contract information with name, description, and validity

**Screens** (`ui`):
- `ReadCardScreen`: Screen for reading card content
- `CardContentScreen`: Contracts of the card and products proposed for sale
- `LoadContractScreen`: Screen for loading contracts
- `PersonalizeCardScreen`: Screen for card personalization
- `SettingsMenuScreen`: Settings menu (server, personalization)
- `ServerSettingsScreen`: Server settings UI

#### Platform-specific implementations

Each platform provides:
- NFC/PC/SC reader integration through Keyple libraries
- DataStore implementation for settings persistence
- Platform-specific UI adaptations

## Development

### Building for different platforms

#### Android development
```bash
# Debug build
./gradlew :androidApp:assembleDebug

# Run on connected device
./gradlew :androidApp:installDebug
```

#### iOS development
1. **Xcode setup**:
  - Open `iosApp/iosApp.xcodeproj`
  - Configure the signing in `Signing & Capabilities`: enable "Automatically manage signing" and select your
    development team
  - Add NFC capability: `Signing & Capabilities` → `Near Field Communication Tag Reading`

2. **Entitlements** (`iosApp/iosApp/iosApp.entitlements`):
   ```xml
   <key>com.apple.developer.nfc.readersession.formats</key>
   <array>
       <string>TAG</string>
   </array>
   ```

3. **Build and run**:
  - Use Xcode to build and deploy to iPhone
  - Or use Android Studio's iOS run configuration

#### Desktop development
```bash
# Run with default settings
./gradlew :composeApp:run

# Run with custom PC/SC filter
./gradlew :composeApp:run -PcustomArgs="-filter=ACS"

# Package as executable (.dmg on macOS, .msi on Windows, .deb on Linux, in build/compose/binaries/main/)
./gradlew :composeApp:packageDistributionForCurrentOS
```

The filter is a part of the name of the reader, case insensitive. Without filter, or when no reader matches, the first
reader is used.

## Troubleshooting

### Common issues across platforms

**"Server connection failed"**
- Verify server URL is accessible from target platform
- Check network permissions on mobile platforms
- Ensure firewall allows connections on desktop

**"Card not detected"**
- **Android**: Enable NFC in system settings, grant app permissions
- **iOS**: Ensure Core NFC is supported (iPhone 7+), check entitlements
- **Desktop**: Verify PC/SC reader connection and drivers

### Platform-specific issues

#### Android
- **NFC not working**: Check `AndroidManifest.xml` for NFC permissions
- **App crashes on card detection**: Verify NFC intent filters

#### iOS
- **Core NFC session errors**: Check entitlements and provisioning profile

#### Desktop
- **PC/SC service not available**: Start PC/SC daemon (`pcscd` on Linux)
- **Reader not detected**: Check USB connection and driver installation
- **Permission denied**: Run with appropriate user permissions

## Contributing

See the [Contributing](../../../../README.md#contributing) section of the main README. Specific to this client: keep the
same features on Android, iOS, and desktop, the platform-specific code being minimal.

## Related documentation

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform/get-started.html)
- [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/)
- [Keyple Interop JSON API Client KMP Library](https://github.com/eclipse-keyple/keyple-interop-jsonapi-client-kmp-lib)
- [Keyple Interop Local Reader NFC Mobile KMP Library](https://github.com/eclipse-keyple/keyple-interop-localreader-nfcmobile-kmp-lib)
- [Main Project Overview](../../../../README.md)
- [Server Documentation](../../server/README.md)

## License

This Kotlin Multiplatform client is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.