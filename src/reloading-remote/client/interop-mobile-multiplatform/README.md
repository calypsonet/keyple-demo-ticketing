# Keyple Reload Demo - Kotlin Multiplatform Client

[![Kotlin](https://img.shields.io/badge/kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![KMP](https://img.shields.io/badge/multiplatform-android%20%7C%20ios%20%7C%20desktop-green.svg)](https://www.jetbrains.com/kotlin-multiplatform/)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../../../LICENSE)

A Kotlin Multiplatform application demonstrating distributed remote client communications across Android, iOS, and
desktop platforms using Keyple Distributed Client KMP libraries for seamless cross-platform card operations.

[⬅️ Back to Main Project](../../../../README.md)

## Overview

This innovative client showcases the power of Kotlin Multiplatform by providing a single codebase that runs natively on multiple platforms while maintaining full functionality with the Keyple server ecosystem. It demonstrates modern cross-platform development practices for contactless card applications.

**Supported Platforms**:
- **Android 7.0+** (API 24+) with native NFC support
- **iOS 14+** with Core NFC integration
- **JVM Desktop** (Windows/macOS/Linux) with PC/SC readers

## Prerequisites

### Development Environment
- **Android Studio** with Kotlin Multiplatform plugin
- **Xcode** (for iOS development on macOS)
- **JDK 11+** for desktop targets
- **Kotlin 1.9+** with multiplatform support

### Platform-Specific Requirements

#### Android
- Device with NFC capability
- Android 8.0+ (API level 26+)
- NFC enabled in system settings

#### iOS
- iPhone with NFC support (iPhone 7+)
- iOS 14.0 or later
- Core NFC entitlements configured
- Apple Developer account for device deployment

#### Desktop
- **PC/SC compatible reader** connected via USB
- Platform-specific PC/SC libraries:
  - **Windows**: Built-in PC/SC support
  - **macOS**: PC/SC framework (usually pre-installed)
  - **Linux**: `pcscd` daemon and `libpcsclite-dev`

### Server Requirements
- Running [Keyple Demo Server](../../server/README.md) with SAM integration
- Network connectivity from all target platforms

## Installation

### Building the Project

```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/reloading-remote/client/interop-mobile-multiplatform
```

#### Android App
```bash
./gradlew :composeApp:assembleDebug

# Install on connected device
./gradlew :composeApp:installDebug
```

#### iOS App
1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Configure signing in "Signing & Capabilities"
3. Enable "Automatically manage signing"
4. Select your development team
5. Build and run on connected iPhone

#### Desktop App
```bash
# Windows/Mac/Linux
./gradlew :composeApp:run

# With PC/SC reader filter (example for ACS reader)
./gradlew :composeApp:run -PcustomArgs="-filter=ACS"
```

## Configuration

### Application Configuration

The application configuration is managed through:
- Server settings UI for configuring server URL
- Platform-specific datastore for persisting settings
- Shared network client for server communication

## Usage

### Mobile Platforms (Android/iOS)

#### Initial Setup
1. **Launch Application**
2. **Configure Server**: Tap settings icon → Enter server URL
3. **Grant Permissions**: Allow NFC access when prompted

#### Card Personalization
1. **Settings** → **Personalize**
2. **Present Card** to device NFC reader
3. **Wait for confirmation** of successful initialization

#### Reading Card Content
1. **Main Screen** → **Contactless Support**
2. **Hold card** against device back (near NFC antenna)
3. **View existing contracts** and their status
4. **Select new title** to load if desired

#### Contract Loading
1. **Choose contract type** from available options
2. **Complete payment simulation**
3. **Present card again** when prompted
4. **Receive confirmation** of successful loading

### Desktop Platform

#### Initial Setup
1. **Connect PC/SC reader** via USB
2. **Launch application** with reader filter:
   ```bash
   ./gradlew :composeApp:run -PcustomArgs="-filter=ACS"
   ```
3. **Verify reader detection** in application logs

#### Card Operations
1. **Place card** on connected PC/SC reader
2. **Follow same workflow** as mobile platforms
3. **Monitor operations** through desktop UI

### Shared User Interface

The application uses Compose Multiplatform for consistent UI across all platforms:

```
┌─────────────────────────────────────┐
│            Main Screen              │
│  ┌─────────────────────────────────┐│
│  │         Settings                ││
│  │  ┌─────────────────────────────┐││
│  │  │  Server Configuration      │││
│  │  │  Reader Settings           │││
│  │  │  Personalization           │││
│  │  └─────────────────────────────┘││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │      Card Operations            ││
│  │  • Read Card Content           ││
│  │  • Load New Contracts          ││
│  │  • View Transaction History    ││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

## Technical Architecture

### Multiplatform Structure

```
composeApp/src/
├── commonMain/                    # Shared business logic
│   └── kotlin/
│       ├── card/                 # Card content screens
│       ├── nfc/                  # NFC operation screens
│       ├── network/              # Server communication
│       ├── settings/             # Settings screens
│       └── ui/                   # Common UI components
├── androidMain/                  # Android-specific code
├── iosMain/                      # iOS-specific code
└── desktopMain/                  # Desktop-specific code
```

### Key Components

#### Shared Business Logic (`commonMain`)

**Data Models**:
- `CardRepository` - Manages card data (serial number, contracts)
- `ContractInfo` - Contract information with title, description, and validity
- `KeypleService` - Handles Keyple Interop API communication
- `SimpleHttpNetworkClient` - HTTP client for server communication

**UI Components**:
- `ReadCardScreen` - Screen for reading card content
- `WriteCardScreen` - Screen for loading contracts
- `PersonalizeCardScreen` - Screen for card initialization
- `ServerConfigScreen` - Server configuration UI

#### Platform-Specific Implementations

Each platform provides:
- NFC/PC/SC reader integration through Keyple libraries
- Datastore implementation for settings persistence
- Platform-specific UI adaptations

## Development

### Building for Different Platforms

#### Android Development
```bash
# Debug build
./gradlew :composeApp:assembleDebug

# Run on connected device
./gradlew :composeApp:installDebug
```

#### iOS Development
1. **Xcode Setup**:
  - Open `iosApp/iosApp.xcodeproj`
  - Configure development team in signing
  - Add NFC capability: `Signing & Capabilities` → `Near Field Communication Tag Reading`

2. **Entitlements** (`iosApp/iosApp/iosApp.entitlements`):
   ```xml
   <key>com.apple.developer.nfc.readersession.formats</key>
   <array>
       <string>NDEF</string>
       <string>TAG</string>
   </array>
   ```

3. **Build and Run**:
  - Use Xcode to build and deploy to iPhone
  - Or use Android Studio's iOS run configuration

#### Desktop Development
```bash
# Run with default settings
./gradlew :composeApp:run

# Run with custom PC/SC filter
./gradlew :composeApp:run -PcustomArgs="-filter=ACS.*"

# Package as executable
./gradlew :composeApp:packageDistributionForCurrentOS
```


## Troubleshooting

### Common Issues Across Platforms

**"Server connection failed"**
- Verify server URL is accessible from target platform
- Check network permissions on mobile platforms
- Ensure firewall allows connections on desktop

**"Card not detected"**
- **Android**: Enable NFC in system settings, grant app permissions
- **iOS**: Ensure Core NFC is supported (iPhone 7+), check entitlements
- **Desktop**: Verify PC/SC reader connection and drivers

### Platform-Specific Issues

#### Android
- **NFC not working**: Check `AndroidManifest.xml` for NFC permissions
- **App crashes on card detection**: Verify NFC intent filters
- **Slow card reading**: Adjust discovery timeout settings

#### iOS
- **Core NFC session errors**: Check entitlements and provisioning profile
- **App rejection from App Store**: Ensure proper NFC usage description
- **Session timeout**: Increase Core NFC session duration

#### Desktop
- **PC/SC service not available**: Start PC/SC daemon (`pcscd` on Linux)
- **Reader not detected**: Check USB connection and driver installation
- **Permission denied**: Run with appropriate user permissions

## Performance Optimization

- **Coroutines**: Use structured concurrency for non-blocking operations
- **Memory Management**: Proper cleanup of platform resources (NFC sessions, PC/SC connections)
- **Network Caching**: Cache server responses to reduce bandwidth usage
- **UI Responsiveness**: Offload card operations to background threads

## Deployment

### Android
```bash
# Generate signed APK
./gradlew :composeApp:assembleRelease

# Upload to Google Play Console
# Or distribute via Firebase App Distribution
```

### iOS
1. **Archive in Xcode**: Product → Archive
2. **Upload to App Store Connect**
3. **TestFlight Distribution** for beta testing
4. **App Store Review** and release

### Desktop
```bash
# Create platform-specific distributables
./gradlew :composeApp:packageDistributionForCurrentOS

# Results in build/compose/binaries/main/
# - .dmg for macOS
# - .msi for Windows  
# - .deb/.rpm for Linux
```

## Contributing

When contributing to this KMP client:

1. **Maintain platform parity**: Ensure features work across all supported platforms
2. **Follow KMP best practices**: Keep platform-specific code minimal
3. **Test on all platforms**: Verify changes work on Android, iOS, and desktop
4. **Update documentation**: Include platform-specific setup instructions
5. **Performance considerations**: Profile on resource-constrained mobile devices

## Related Documentation

- [Kotlin Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Keyple Interop JSON API Client KMP Library](https://github.com/eclipse-keyple/keyple-interop-jsonapi-client-kmp-lib)
- [Keyple Interop Local Reader NFC Mobile KMP Library](https://github.com/eclipse-keyple/keyple-interop-localreader-nfcmobile-kmp-lib)
- [Main Project Overview](../../../../README.md)
- [Server Documentation](../../server/README.md)

## License

This Kotlin Multiplatform client is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.