# Keyple Demo Ticketing Ecosystem

[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-8%2B-orange.svg)](https://openjdk.java.net/)
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
- iOS 14+ (KMP)
- Windows Desktop (.NET 7.0)
- JVM Desktop (Kotlin Multiplatform)

**Server Requirements:**

- Java 8+ with PC/SC reader
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
- **Coppernic C-One 2** - Rugged Android terminal
- **Standard NFC Smartphones** - Consumer devices
- **PC/SC Readers** - Desktop integration

### Proprietary Plugins (Available on Request)

- Bluebird EF501
- Flowbird Axio 2

Contact [CNA](https://calypsonet.org/contact-us/) for access to proprietary terminal plugins.

## Data Model

The ecosystem uses standardized data structures defined in the [Common Library](src/common/):

- **Environment Record**: Card metadata and validity information
- **Event Log**: Transaction history and validation events
- **Contract Records**: Transportation titles and their properties
- **Counter Files**: Usage tracking for multi-trip and stored value

See [Common Library Documentation](src/common/README.md) for detailed specifications.

## Development

### Prerequisites

- JDK 8+ for server components
- Android Studio for mobile development
- Node.js for web dashboard
- PC/SC compatible readers for testing

### Project Structure

```
keyple-demo-ticketing/
├── README.md                               # This file
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
- [Calypso Card Specification](https://calypsonet.org/technical-specifications/)
- [Keyple Distributed JSON API](https://keyple.org/learn/user-guide/distributed-json-api-1-0/)