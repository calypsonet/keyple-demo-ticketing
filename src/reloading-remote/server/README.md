# Keyple Reload Demo - Java Server

[![Java](https://img.shields.io/badge/java-17-orange.svg)](https://openjdk.java.net/)
[![Quarkus](https://img.shields.io/badge/quarkus-2.x-blue.svg)](https://quarkus.io/)
[![License](https://img.shields.io/badge/license-BSD_3_Clause-blue.svg)](../../../LICENSE)

The server component of the Keyple Reload Demo, providing distributed ticketing services with web-based monitoring and
SAM integration for secure Calypso card operations.

[⬅️ Back to Main Project](../../../README.md)

## Overview

This Java server implements the business logic for the Keyple Demo ecosystem, managing:
- Secure card personalization and contract loading
- SAM (Security Access Module) integration
- Client authentication and session management
- Web dashboard for monitoring and administration
- RESTful API for client applications

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Web Clients   │     │   Java Server   │     │   PC/SC Reader  │
│                 │────>│                 │────>│                 │
│ Android/iOS/Web │     │ Quarkus + REST  │     │ SAM Integration │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                  │
                        ┌─────────▼─────────┐
                        │   Web Dashboard   │
                        │                   │
                        │ React + Monitoring│
                        └───────────────────┘
```

## Prerequisites

### Hardware Requirements
- PC/SC compatible card reader
- Calypso SAM (Security Access Module) for secure operations
- USB connection for reader

### Software Requirements
- **JDK 17** (OpenJDK recommended)
- **Node.js 18+** (for dashboard development)
- **Compatible PC/SC reader drivers**

### Tested Readers
- Cherry TC series
- SCM Microsystems
- Identive CLOUD series
- HID Global readers
- Generic PC/SC compatible devices

## Quick Start

### Using Pre-built JAR

1. **Download** the latest release:
```bash
wget https://github.com/calypsonet/keyple-demo-ticketing/releases/latest/kdt-reloading-server-X.Y.Z-full.jar
```

2. **Start** the server:
```bash
java -jar kdt-reloading-server-X.Y.Z-full.jar
```

3. **Access** the web dashboard at `http://localhost:8080`

### Custom Reader Configuration

If your PC/SC reader name doesn't match the default filter, specify a custom pattern:

**Windows Command Prompt:**
```cmd
java "-Dsam.pcsc.reader.filter=Identive CLOUD 2700 R Smart Card Reader.*" -jar kdt-reloading-server-X.Y.Z-full.jar
```

**Windows PowerShell:**
```powershell
java '-Dsam.pcsc.reader.filter=Identive CLOUD 2700 R Smart Card Reader.*' -jar .\kdt-reloading-server-X.Y.Z-full.jar
```

**Linux/macOS:**
```bash
java -Dsam.pcsc.reader.filter=".*ACS.*" -jar kdt-reloading-server-X.Y.Z-full.jar
```

## Configuration

### Application Properties

The server uses the following default configuration in `application.properties`:

```properties
# CORS Configuration (for web clients)
quarkus.http.cors=true

# Package Configuration
quarkus.package.type=uber-jar
quarkus.package.runner-suffix=-full

# Logging Configuration
quarkus.log.level=INFO
quarkus.log.category."org.calypsonet.keyple".level=INFO
quarkus.log.category."org.eclipse.keyple".level=INFO

# PC/SC Reader Filter (regex format)
sam.pcsc.reader.filter=.*(Cherry TC|SCM Microsystems|Identive|HID|Generic).*
```

### Environment Variables

Override configuration using environment variables:

```bash
export SAM_PCSC_READER_FILTER=".*Your Reader Name.*"
export QUARKUS_LOG_LEVEL=DEBUG
```

## Development Setup

### Building from Source

1. **Clone** the repository:
```bash
git clone https://github.com/calypsonet/keyple-demo-ticketing.git
cd keyple-demo-ticketing/src/reloading-remote/server
```

2. **Install** dashboard dependencies:
```bash
cd dashboard-app
npm install
cd ..
```

3. **Build** the project:
```bash
./gradlew build
```

4. **Start** development server:
```bash
./gradlew quarkusDev
```

### Development Mode

Run in development mode with hot reload:

```bash
./gradlew quarkusDev
```

This enables:
- Automatic code recompilation
- Live dashboard updates
- Debug logging
- Development UI at `http://localhost:8080/q/dev/`

## Web Dashboard

### Features

The React-based dashboard provides:

**Real-time Monitoring:**
- Active client connections
- Transaction history and status
- SAM reader connectivity status
- System performance metrics

**Card Management:**
- View connected cards and their status
- Monitor ongoing transactions
- Transaction success/failure rates
- Error logs and diagnostics

**System Administration:**
- Server configuration overview
- Reader management and diagnostics
- Database status and cleanup
- Log file access

### Dashboard URLs

- **Main Dashboard**: `http://localhost:8080`
- **API Documentation**: `http://localhost:8080/q/swagger-ui/`
- **Health Check**: `http://localhost:8080/q/health`
- **Metrics**: `http://localhost:8080/q/metrics`

## API Endpoints

### Card Operations

```http
GET  /card/export-card-selection-scenario
POST /card/remote-plugin
GET  /card/sam-status
```

### Activity Monitoring

```http
GET  /activity/events
GET  /activity/events/wait
```

## Troubleshooting

### Common Issues

**"No PC/SC reader found"**
- Verify reader is connected and drivers installed
- Check reader filter pattern matches your device name
- Test with `pcsc_scan` on Linux/macOS or Device Manager on Windows

**"SAM not detected"**
- Ensure SAM is properly inserted in reader
- Verify SAM is compatible with your cards (Test vs Production keys)
- Check SAM status in dashboard

**"Port already in use"**
- Kill existing process: `lsof -ti:8080 | xargs kill -9` (Linux/macOS) or check Task Manager (Windows)

**Web dashboard not loading**
- Verify Node.js dependencies: `cd dashboard-app && npm install`
- Clear browser cache and cookies
- Check console for JavaScript errors

### Debug Mode

Enable detailed logging:

```bash
java -Dquarkus.log.level=DEBUG -Dquarkus.log.category."org.calypsonet".level=TRACE -jar server.jar
```

### Health Checks

Monitor system health:

```bash
# Check overall health
curl http://localhost:8080/q/health

# Check specific components  
curl http://localhost:8080/q/health/ready
curl http://localhost:8080/q/health/live
```

## Deployment

### Production Checklist

- [ ] Configure logging to external system
- [ ] Set up monitoring and alerting
- [ ] Configure firewall rules
- [ ] Secure physical access to SAM reader
- [ ] Test PC/SC reader connectivity

### Docker Deployment

```dockerfile
FROM eclipse-temurin:17-jre
COPY kdt-reloading-server-X.Y.Z-full.jar /app/server.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/server.jar"]
```

```bash
docker build -t keyple-server .
docker run -p 8080:8080 --device=/dev/bus/usb keyple-server
```

### Systemd Service

```ini
[Unit]
Description=Keyple Demo Server
After=network.target

[Service]
Type=simple
User=keyple
ExecStart=/usr/bin/java -jar /opt/keyple/server.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

## Performance Tuning

### JVM Options

```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -jar server.jar
```

## Security Considerations

- **SAM Security**: Ensure SAMs contain appropriate keys for your environment
- **Network Security**: Use HTTPS in production environments
- **Access Control**: Implement authentication for administrative functions
- **Audit Logging**: Enable comprehensive transaction logging
- **Key Management**: Secure storage and rotation of cryptographic keys

## License

This server application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.