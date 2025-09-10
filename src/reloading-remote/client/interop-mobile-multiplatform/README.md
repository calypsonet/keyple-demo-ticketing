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

### Cross-Platform Settings

The application uses shared configuration across platforms:

```kotlin
// commonMain/kotlin/Config.kt
object AppConfig {
    const val SERVER_BASE_URL = "http://192.168.1.100:8080"
    const val CONNECTION_TIMEOUT = 30_000L
    const val READ_TIMEOUT = 60_000L
    
    val SUPPORTED_AIDS = listOf(
        "A000000291FF9101",        // Keyple Generic
        "315449432E49434131",      // CD Light GTML
        "315449432E49434133",      // Calypso Light
        "A0000004040125090101"     // Navigo IDF
    )
}
```

### Platform-Specific Configuration

#### Android (`androidMain`)
```kotlin
// NFC configuration
object AndroidNfcConfig {
    const val DISCOVERY_TIMEOUT = 10_000L
    const val PRESENCE_CHECK_DELAY = 500L
    val SUPPORTED_TECHNOLOGIES = arrayOf(
        NfcA::class.java.name,
        IsoDep::class.java.name
    )
}
```

#### iOS (`iosMain`)
```kotlin
// Core NFC configuration
object IosNfcConfig {
    const val SESSION_TIMEOUT = 60.0
    const val READER_SESSION_INVALIDATION_ERROR_MESSAGE = "Session ended"
    val ISO14443_POLL_REQUEST_OPTIONS = mapOf(
        "ISO14443PollingRequestCodeSystemCode" to false
    )
}
```

#### Desktop (`jvmMain`)
```kotlin  
// PC/SC configuration
object DesktopPcscConfig {
    const val READER_CONNECTION_TIMEOUT = 5_000L
    const val CARD_DETECTION_POLLING_INTERVAL = 500L
    val DEFAULT_READER_FILTER = ".*"
}
```

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
│   ├── kotlin/
│   │   ├── domain/               # Domain models and interfaces
│   │   ├── data/                 # Data layer implementation
│   │   ├── ui/                   # Compose UI components
│   │   └── utils/                # Common utilities
│   └── resources/                # Shared resources
├── androidMain/                  # Android-specific code
│   └── kotlin/
│       ├── platform/             # Android NFC implementation
│       └── di/                   # Android dependency injection
├── iosMain/                      # iOS-specific code  
│   └── kotlin/
│       ├── platform/             # Core NFC implementation
│       └── di/                   # iOS dependency injection
├── jvmMain/                      # Desktop-specific code
│   └── kotlin/
│       ├── platform/             # PC/SC implementation
│       └── di/                   # Desktop dependency injection
└── nativeMain/                   # Native shared code (if any)
```

### Key Components

#### Shared Business Logic (`commonMain`)

**Domain Layer**:
```kotlin
interface CardRepository {
    suspend fun readCard(): CardData
    suspend fun writeContract(contract: ContractData): Result<Unit>
}

interface ServerService {
    suspend fun getAvailableContracts(cardId: String): List<ContractOption>
    suspend fun processContractLoading(request: LoadingRequest): LoadingResult
}
```

**Data Layer**:
```kotlin
@Serializable
data class CardData(
    val serialNumber: String,
    val applicationId: String,
    val contracts: List<Contract>,
    val environment: Environment
)

@Serializable
data class Contract(
    val index: Int,
    val type: ContractType,
    val validityEnd: LocalDate,
    val remainingValue: Int?
)
```

#### Platform-Specific Implementations

**Android NFC** (`androidMain`):
```kotlin
class AndroidNfcCardRepository : CardRepository {
    private val nfcAdapter: NfcAdapter = NfcAdapter.getDefaultAdapter(context)
    
    override suspend fun readCard(): CardData {
        return withContext(Dispatchers.IO) {
            // Android NFC implementation using Keyple Android NFC plugin
            nfcManager.processCardDetection()
        }
    }
}
```

**iOS Core NFC** (`iosMain`):
```kotlin
class IosNfcCardRepository : CardRepository {
    override suspend fun readCard(): CardData {
        return suspendCoroutine { continuation ->
            // Core NFC implementation using Keyple iOS NFC plugin
            nfcManager.startSession { result ->
                continuation.resume(result)
            }
        }
    }
}
```

**Desktop PC/SC** (`jvmMain`):
```kotlin
class DesktopPcscCardRepository(private val readerFilter: String) : CardRepository {
    private val pcscPlugin = PcscPluginFactory.createPlugin()
    
    override suspend fun readCard(): CardData {
        return withContext(Dispatchers.IO) {
            // PC/SC implementation using Keyple PC/SC plugin
            pcscManager.connectToCard(readerFilter)
        }
    }
}
```

### Dependency Injection

Using Koin for multiplatform dependency injection:

```kotlin
// commonMain
val commonModule = module {
    single<ServerService> { ServerServiceImpl(get()) }
    single<CardValidator> { CardValidatorImpl() }
}

// Platform-specific modules
val androidModule = module {
    single<CardRepository> { AndroidNfcCardRepository(androidContext()) }
    single<NetworkManager> { AndroidNetworkManager(get()) }
}

val iosModule = module {
    single<CardRepository> { IosNfcCardRepository() }
    single<NetworkManager> { IosNetworkManager() }
}

val desktopModule = module {
    single<CardRepository> { DesktopPcscCardRepository(getProperty("readerFilter")) }
    single<NetworkManager> { DesktopNetworkManager() }
}
```

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

### Adding New Platforms

To extend support to additional platforms:

1. **Create platform source set**:
   ```kotlin
   // build.gradle.kts
   kotlin {
       tvos() // Example: Apple TV
       watchos() // Example: Apple Watch
   }
   ```

2. **Implement platform interfaces**:
   ```kotlin
   // tvosMain/kotlin/platform/TvosPlatformService.kt
   class TvosPlatformService : PlatformService {
       override fun getCardReader(): CardRepository {
           // Platform-specific implementation
       }
   }
   ```

3. **Configure platform module**:
   ```kotlin
   val tvosModule = module {
       single<CardRepository> { TvosCardRepository() }
   }
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

### Debug Configuration

Enable platform-specific logging:

```kotlin
// commonMain
object Logger {
    fun debug(tag: String, message: String) {
        when (Platform.current) {
            is Platform.Android -> android.util.Log.d(tag, message)
            is Platform.Ios -> NSLog("$tag: $message")
            is Platform.Desktop -> println("[$tag] $message")
        }
    }
}
```

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