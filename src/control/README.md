# Keyple Control Demo

[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)
[![Release](https://img.shields.io/github/v/release/calypsonet/keyple-demo-ticketing)](https://github.com/calypsonet/keyple-demo-ticketing/releases)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../LICENSE)

Android control terminal application for post-validation inspection of transportation cards. This application represents the final verification step in the ticketing workflow, allowing inspectors to verify the validity of validation events and contract usage.

[⬅️ Back to Main Project](../../README.md)

## Overview

This Android application simulates inspection terminals used by transportation authority personnel to verify that passengers have properly validated their travel. It analyzes validation events created by the [Validation Demo](../validation-app/) and provides comprehensive contract status information for compliance checking.

**Role in Ecosystem**: Final step in the ticketing workflow - inspects cards after validation to verify proper usage and contract compliance.

**Control Purpose**: Verify that the last validation event is legitimate and that contracts are being used appropriately within the transportation system.

## Prerequisites

### Hardware Requirements
- Android device with Android 8.0+ (API level 26+)
- NFC capability for contactless card reading
- **SAM (Security Access Module)** for secure Calypso operations (optional)
- Compatible terminal hardware (see [tested terminals](#tested-terminals))

### Software Requirements
- Cards with validation events from [Validation Demo](../validation/)
- Contracts loaded using [Reload Demo](../reloading-remote/)
- Inspector training on control procedures and result interpretation

### Card Requirements
- Previously validated cards with event history
- Valid contract structures and authentication data
- Supported AIDs (see [Common Library](../common/README.md#supported-card-applications))

## Installation

### Download APK
1. Visit [Releases page](https://github.com/calypsonet/keyple-demo-ticketing/releases)
2. Download latest `kdt-control-app-X.Y.Z-debug.apk`
3. Enable "Install from unknown sources" in Android settings
4. Install the APK file

### Build from Source
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/control  
./gradlew assembleDebug
```

## Configuration

### Device Selection
1. Launch application
2. Select device type from **Device Selection** screen:
  - **Famoco FX205**: Enterprise terminal with SAM reader
  - **Coppernic C-One 2**: Rugged Android terminal
  - **Standard NFC**: Consumer Android device (No card authentication)
  - Proprietary terminals (grayed out by default)

### Control Settings
1. Open **Settings** from main menu
2. Configure control parameters:

**Location Settings**:
- **Control Location**: Identifier for this inspection point
- **Location Name**: Human-readable location (e.g., "Central Station", "Bus Line 42")
- Used to verify validation occurred in correct zone/network

**Validity Settings**:
- **Validity Duration**: Time window (minutes) for considering validation events as current
- **Grace Period**: Additional time allowance for edge cases
- **Location Tolerance**: Accept validations from specific other locations

**Inspector Settings**:
- **Inspector ID**: Unique identifier for accountability
- **Shift Information**: Start/end times for reporting
- **Report Generation**: Enable automatic inspection reports

## Usage

### Standard Control Flow

```
Device Selection → Settings → Home → Reader Activity → Control Results
```

### Detailed Screen Guide

**Device Selection (`DeviceSelectionActivity`)**
- Choose appropriate hardware plugin for terminal type
- Proprietary plugins require additional setup (see [Proprietary Plugins](#proprietary-plugins))

**Settings (`SettingsActivity`)**
- Configure location and validity parameters
- Set inspector identification and reporting options
- Access diagnostic and maintenance functions

**Home (`HomeActivity`)**
- Inspector dashboard with current shift information
- Quick access to settings and help documentation
- Statistics on recent control operations

**Reader Activity (`ReaderActivity`)**
- Initializes selected Keyple plugin and optional SAM integration
- Displays "Present Card for Control" message
- Shows real-time analysis during card processing
- Handles card detection and control procedure execution

**Control Results**

**Valid Card Screen (`CardContentActivity`)**:
- **Last Validation Event**:
  - Date, time, and location of validation
  - Contract used for validation
  - Validation status (valid/expired/insufficient)
- **Contract Analysis**:
  - List of all contracts with current status
  - Validity periods and remaining balances
  - Priority assignments and usage history
- **Compliance Status**: Clear indication of card validity

**Invalid Card Screen (`NetworkInvalidActivity`)**:
- **Non-compliance Reason**: Specific issue detected
- **Recommended Action**: Guidance for inspector response
- **Supporting Evidence**: Technical details for documentation

### Control Scenarios

#### Valid Inspection Results

**Recent Valid Validation**:
- Card shows validation within validity duration
- Validation occurred at acceptable location
- Contract used was appropriate and had sufficient value
- **Result**: Passenger in compliance

**Valid Season Pass**:
- Card shows Season Pass contract within validity period
- Recent validation event confirms proper usage
- **Result**: Unlimited travel authorization confirmed

**Valid Multi-trip Usage**:
- Card shows Multi-trip contract with remaining trips
- Validation event shows proper trip deduction
- **Result**: Paid travel confirmed

#### Non-Compliance Detection

**No Recent Validation**:
- Last validation event outside validity duration
- Passenger may have entered without validation
- **Action**: Request proof of payment or issue citation

**Wrong Location Validation**:
- Validation occurred at incompatible location
- May indicate network boundary violations
- **Action**: Verify passenger's intended journey

**Expired Contract Usage**:
- Validation attempted with expired contract
- System should have rejected but may indicate tampering
- **Action**: Detailed inspection and possible citation

**Insufficient Balance**:
- Stored value validation with insufficient funds
- May indicate payment system bypass
- **Action**: Verify payment and request top-up

## Technical Architecture

### Supported Card Types

#### Calypso Cards
**Secure Analysis**:
- Optional SAM authentication for enhanced security
- Cryptographic verification of contract authenticity
- Secure session management for sensitive operations
- Full audit trail of all control operations

**Control Procedure**:
1. **Detection and Selection**: Verify supported AID
2. **Environment Analysis**: Check card validity and expiration
3. **Event Analysis**: Examine last validation event details
4. **Contract Analysis**: Review all contracts and their status
5. **Authentication Verification**: Validate contract signatures (if SAM available)
6. **Compliance Determination**: Generate pass/fail result

#### Storage Cards
**Simplified Analysis**:
- Basic read operations without SAM requirements
- Direct data validation and consistency checking
- Single contract analysis with simplified logic
- **Note**: Production implementations should add security measures

**Control Procedure**:
1. **Card Detection**: Read all card data in single operation
2. **Data Validation**: Verify structure integrity and versions
3. **Contract Analysis**: Check single contract status and validity
4. **Event Verification**: Analyze validation event details
5. **Compliance Assessment**: Determine card validity status

### Key Classes

**TicketingService**
- **Purpose**: Orchestrates control process lifecycle
- **Responsibilities**:
  - Plugin management and initialization
  - Card detection and selection handling
  - Control procedure coordination and result processing
- **Integration**: Works with both SAM-enabled and SAM-less configurations

**ReaderRepository**
- **Purpose**: Hardware abstraction and reader management
- **Functions**:
  - Reader discovery and connection management
  - Plugin-specific configuration and optimization
  - Error handling and recovery procedures
  - Status monitoring for operational reliability

**CardReaderObserver**
- **Purpose**: Handles card reader events from Keyple middleware
- **Event Processing**:
  - `CARD_INSERTED`: Initiates control procedure
  - `CARD_MATCHED`: Confirms supported card type
  - `CARD_REMOVED`: Cleans up resources
  - `READER_FAILURE`: Handles hardware errors gracefully

**Card Repository Implementations**

**CalypsoCardRepository**
- Implements comprehensive control procedure for Calypso cards
- Manages optional secure sessions and SAM integration
- Handles complex contract authentication and verification
- Provides detailed compliance analysis and reporting

**StorageCardRepository**
- Implements streamlined control for storage cards
- Direct data access without cryptographic overhead
- Simplified contract and event analysis
- Basic compliance checking and result generation

### Control Procedure Logic

#### Event Analysis Process

```
1. Read Environment Record
   ├─ Verify card application validity
   ├─ Check overall card expiration
   └─ Extract holder information (if available)

2. Read Last Event Record  
   ├─ Extract validation timestamp and location
   ├─ Identify contract used for validation
   ├─ Retrieve contract priority information
   └─ Assess event validity against current time/location

3. Contract Analysis Loop
   ├─ For each contract slot:
   │  ├─ Read contract record and metadata
   │  ├─ Verify contract version and structure
   │  ├─ Check validity dates and expiration
   │  ├─ Validate authentication (if SAM available)
   │  ├─ Read associated counters (if applicable)
   │  └─ Determine contract status and usability
   └─ Generate comprehensive status report

4. Compliance Determination
   ├─ Compare event location with control location
   ├─ Verify event timestamp within validity window
   ├─ Confirm contract used was appropriate
   ├─ Check contract had sufficient value/trips
   └─ Generate pass/fail result with details
```

#### Contract Status Classification

| Status           | Description                                        | Control Action           |
|:-----------------|:---------------------------------------------------|:-------------------------|
| **Validated**    | Used in recent valid validation                    | ✅ Accept                 |
| **Valid Unused** | Available for use but not recently validated       | ℹ️ Informational         |
| **Expired**      | Past validity date                                 | ❌ Cannot be used         |
| **Insufficient** | Multi-trip (0 trips) or Stored Value (low balance) | ❌ Requires reload        |
| **Unknown**      | Unrecognized contract type                         | ⚠️ Manual review         |
| **Blank**        | Empty contract slot                                | ℹ️ Available for loading |

## Hardware Integration

### Tested Terminals

#### Standard Plugins (Open Source)

**Famoco FX205**
- **Configuration**: Dual reader setup (NFC + SAM)
- **Plugins**: [Famoco Plugin](https://github.com/calypsonet/keyple-famoco) + [Android NFC](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)
- **Advantages**: Enterprise security, robust construction, integrated SAM
- **Use Case**: Fixed inspection points, high-security environments

**Coppernic C-One 2**
- **Configuration**: Integrated NFC reader
- **Plugin**: [Coppernic Plugin](https://github.com/calypsonet/keyple-android-plugin-coppernic)
- **Advantages**: Rugged design, mobile form factor, long battery life
- **Use Case**: Mobile inspectors, field operations

**Standard NFC Smartphones**
- **Configuration**: Built-in NFC radio
- **Plugin**: [Android NFC Plugin](https://keyple.org/components/standard-reader-plugins/keyple-plugin-android-nfc-lib/)
- **Limitations**: No SAM support (Storage Cards only)
- **Use Case**: Development, testing, basic inspections

#### SAM Integration Benefits

When SAM is available:
- **Enhanced Security**: Cryptographic verification of contract authenticity
- **Tamper Detection**: Identification of potentially modified cards
- **Audit Trail**: Complete cryptographic record of control operations
- **Compliance**: Meeting regulatory requirements for secure operations

Without SAM:
- **Basic Functionality**: Visual inspection and basic data validation
- **Faster Processing**: No cryptographic operations required
- **Wider Compatibility**: Works with any NFC-enabled Android device
- **Cost Effective**: No specialized hardware requirements

### Plugin Configuration Examples

```kotlin
// Famoco with SAM configuration
val famocoPlugin = KeyplePluginExtensionFactory.createPlugin(FamocoPluginFactory())
val samReader = famocoPlugin.getReader("Famoco SAM Reader")
val nfcReader = famocoPlugin.getReader("Famoco NFC Reader")

// Configure for secure operations
if (samReader.isCardPresent()) {
    ticketingService.setSecureSessionMode(true)
    logger.info("SAM detected - secure operations enabled")
}

// Standard NFC configuration  
val nfcPlugin = KeyplePluginExtensionFactory.createPlugin(AndroidNfcPluginFactory())
val reader = nfcPlugin.getReader("Android NFC Reader")

// Configure for basic operations
ticketingService.setSecureSessionMode(false)
logger.info("NFC-only mode - basic operations enabled")
```

## Development

### Project Architecture

```
control/app/
├── src/main/
│   ├── java/org/calypsonet/keyple/demo/control/
│   │   ├── activities/          # Android UI activities
│   │   │   ├── CardContentActivity.java     # Valid card display
│   │   │   ├── DeviceSelectionActivity.java # Hardware selection
│   │   │   ├── HomeActivity.java            # Inspector dashboard
│   │   │   ├── NetworkInvalidActivity.java  # Invalid card display
│   │   │   ├── ReaderActivity.java          # Card reading interface
│   │   │   └── SettingsActivity.java        # Configuration
│   │   ├── data/               # Data models and persistence
│   │   │   ├── model/          # Data transfer objects
│   │   │   └── repository/     # Data access implementations
│   │   ├── domain/             # Business logic interfaces
│   │   │   ├── model/          # Domain entities
│   │   │   └── repository/     # Repository contracts
│   │   ├── reader/             # Card reader management
│   │   │   ├── CalypsoCardRepository.java   # Calypso card operations
│   │   │   ├── StorageCardRepository.java   # Storage card operations
│   │   │   └── ReaderRepository.java        # Reader abstraction
│   │   ├── ticketing/          # Core business logic
│   │   │   ├── TicketingService.java        # Main orchestrator
│   │   │   └── procedure/      # Control procedures
│   │   └── ui/                 # UI utilities and components
│   ├── res/                    # Android resources (layouts, strings, etc.)
│   └── AndroidManifest.xml     # Application configuration
├── build.gradle                # Build configuration and dependencies
└── proguard-rules.pro         # Code obfuscation rules for release
```

### Building and Testing

```bash
# Clean project
./gradlew clean

# Build debug version
./gradlew assembleDebug
```

## Troubleshooting

### Common Control Issues

**"No validation event found"**
- **Cause**: Card has never been validated or event data is corrupted
- **Action**: Check if card was properly validated using Validation Demo
- **Inspector Response**: Request proof of payment or issue citation

**"Validation too old"**
- **Cause**: Last validation outside configured validity duration
- **Solution**: Adjust validity duration settings if policy allows
- **Inspector Response**: Ask passenger when they boarded/entered

**"Wrong location validation"**
- **Cause**: Validation occurred at different location than expected
- **Check**: Verify control location settings match network configuration
- **Inspector Response**: Confirm passenger's journey route and transfers

**"Contract authentication failed"**
- **Cause**: SAM unable to verify contract signature (security issue)
- **Action**: Report potential fraud case, escalate to security team
- **Inspector Response**: Detain card for investigation if policy permits

### Hardware Troubleshooting

**"SAM reader not responding"**
- Check SAM is properly seated in reader slot
- Verify SAM is compatible with card types being controlled
- Restart application to reinitialize SAM connection
- Check terminal's SAM reader hardware status

**"Card reading intermittent"**
- Clean NFC antenna area on terminal
- Ensure card is held steady during reading
- Check for interference from other NFC devices
- Verify card is not damaged or demagnetized

**"Plugin initialization failed"**
- Confirm selected device type matches actual hardware
- Check plugin libraries are properly installed
- Verify hardware drivers are up to date
- Restart terminal if persistent issues occur

### Debug and Diagnostics

#### Enable Debug Logging
```java
// In SettingsActivity
public void enableDebugMode(boolean enabled) {
    SharedPreferences prefs = getSharedPreferences("control_settings", MODE_PRIVATE);
    prefs.edit().putBoolean("debug_mode", enabled).apply();
    
    if (enabled) {
        Logger.setLogLevel(Logger.DEBUG);
        Logger.d("Control", "Debug mode enabled");
    }
}
```

#### Performance Monitoring
```java
// Track control operation timing
public class ControlPerformanceMonitor {
    
    public void measureControlTime(Runnable controlOperation) {
        long startTime = System.currentTimeMillis();
        
        try {
            controlOperation.run();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            Logger.i("Performance", "Control completed in " + duration + "ms");
            
            if (duration > MAX_ACCEPTABLE_TIME) {
                Logger.w("Performance", "Control operation exceeded expected time");
            }
        }
    }
}
```

## Inspector Training and Best Practices

### Standard Operating Procedures

#### Pre-Shift Checklist
- [ ] Verify terminal battery level and charging status
- [ ] Test NFC reader with known valid card
- [ ] Check SAM status and authentication capability
- [ ] Configure location and inspector ID settings
- [ ] Verify network connectivity (if required)

#### Control Procedure Guidelines
1. **Approach Passenger Politely**: Identify yourself and request card presentation
2. **Clear Instructions**: "Please place your card on the terminal"
3. **Wait for Analysis**: Allow terminal to complete full control procedure
4. **Review Results Carefully**: Check all displayed information
5. **Take Appropriate Action**: Follow organizational policies for violations
6. **Document Issues**: Record problems for system improvement

#### Result Interpretation

**Green Status (Valid)**:
- Passenger is in compliance
- Thank passenger and allow continuation
- No further action required

**Yellow Status (Warning)**:
- Minor issues detected (e.g., low balance, near expiration)
- Inform passenger of status
- Suggest remedial action (reload, renewal)

**Red Status (Invalid)**:
- Serious compliance issue detected
- Follow organizational enforcement procedures
- May require citation, fine, or card retention

### Reporting and Analytics

The application can generate reports for:
- **Daily Control Statistics**: Number of checks, compliance rates
- **Violation Patterns**: Common issues by location/time
- **Hardware Performance**: Reader reliability, SAM status
- **Inspector Productivity**: Controls per shift, issue resolution

## Proprietary Plugins

### Activation Instructions

By default, proprietary plugins are deactivated to maintain open-source compatibility.

**Activation Process**:
1. **Request Plugin**: Contact [CNA](https://calypsonet.org/contact-us/) for desired proprietary plugin
2. **Obtain License**: Complete any required licensing agreements
3. **Install Plugin**:
  - Copy provided `.aar` file to `/libs/` directory
  - Remove corresponding `-mock.aar` file from `/libs/`
4. **Rebuild Application**: Execute `./gradlew assembleDebug` command
5. **Deploy Updated APK**: Install on target terminals

**Available Proprietary Plugins**:
- **Bluebird EF501**: Professional inspection terminal with integrated barcode scanning
- **Flowbird Axio 2**: Transportation-specific control terminal with multi-modal support

## Security and Compliance

### Data Security
- **Card Data Protection**: No persistent storage of sensitive card information
- **Inspector Authentication**: Secure login and session management
- **Audit Logging**: Complete record of all control operations
- **Data Transmission**: Encrypted communication for networked operations

### Regulatory Compliance
- **Privacy Protection**: Minimal data collection, immediate disposal after use
- **Audit Requirements**: Comprehensive logging for compliance verification
- **Access Control**: Role-based permissions for inspector functions
- **Data Retention**: Policy-compliant retention and deletion procedures

### Anti-Fraud Measures
- **Cryptographic Verification**: SAM-based authentication when available
- **Pattern Recognition**: Detection of suspicious card usage patterns
- **Real-time Alerts**: Immediate notification of potential security issues
- **Forensic Support**: Detailed logging for investigation purposes

## Contributing

When contributing to this control application:

1. **Maintain Inspector Usability**: Keep UI simple and inspection workflow efficient
2. **Test Control Scenarios**: Verify all validation/control combinations work correctly
3. **Hardware Compatibility**: Test on supported terminal types
4. **Security Standards**: Maintain or enhance existing security measures
5. **Documentation Updates**: Keep troubleshooting and training materials current

## Related Documentation

- [Main Project Overview](../../README.md)
- [Common Library](../common/README.md) - Data structures and validation logic
- [Validation Demo](../validation/README.md) - Previous step in workflow
- [Reload Demo](../reloading-remote/README.md) - Contract loading procedures
- [Eclipse Keyple Documentation](https://keyple.org) - SDK reference

## License

This control application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.