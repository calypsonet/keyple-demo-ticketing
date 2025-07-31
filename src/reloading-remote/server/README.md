# Keyple Reload Demo - Java Server

[![Java](https://img.shields.io/badge/java-8%2B-orange.svg)](https://openjdk.java.net/)
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
- **JDK 8+** (OpenJDK recommended)
- **Node.js 14+** (for dashboard development)
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
# Server Configuration
quarkus.http.port=8080
quarkus.http.host=0.0.0.0

# PC/SC Reader Filter (regex format)
sam.pcsc.reader.filter=.*(Cherry TC|SCM Microsystems|Identive|HID|Generic).*

# Database Configuration  
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1

# Logging Configuration
quarkus.log.level=INFO
quarkus.log.category."org.calypsonet".level=DEBUG

# CORS Configuration (for web clients)
quarkus.http.cors=true
quarkus.http.cors.origins=*
```

### Environment Variables

Override configuration using environment variables:

```bash
export SAM_PCSC_READER_FILTER=".*Your Reader Name.*"
export QUARKUS_HTTP_PORT=9090
export QUARKUS_LOG_LEVEL=DEBUG
```

### SSL/TLS Configuration (Production)

For production deployments, enable HTTPS:

```properties
quarkus.http.ssl-port=8443
quarkus.http.ssl.certificate.key-store-file=server-keystore.jks
quarkus.http.ssl.certificate.key-store-password=your-password
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
npm cache clear --force
npm install
cd ..
```

3. **Build** the project:
```bash
./gradlew build
```

4. **Start** development server:
```bash  
./gradlew startServer
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
POST /api/cards/personalize
POST /api/cards/load-contract
GET  /api/cards/{cardId}/status
GET  /api/cards/{cardId}/contracts
```

### System Management

```http
GET  /api/system/status
GET  /api/system/readers
POST /api/system/readers/reset
GET  /api/system/health
```

### Monitoring

```http
GET  /api/monitoring/transactions
GET  /api/monitoring/statistics
GET  /api/monitoring/logs
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
- Change port with `-Dquarkus.http.port=9090`
- Kill existing process: `lsof -ti:8080 | xargs kill -9`

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

- [ ] Configure proper SSL certificates
- [ ] Set production database (PostgreSQL/MySQL)
- [ ] Configure logging to external system
- [ ] Set up monitoring and alerting
- [ ] Secure API endpoints with authentication
- [ ] Configure firewall rules
- [ ] Set up backup procedures

### Docker Deployment

```dockerfile
FROM openjdk:11-jre-slim
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

### Connection Pooling

```properties
quarkus.datasource.jdbc.max-size=20
quarkus.datasource.jdbc.min-size=5
```

## Security Considerations

- **SAM Security**: Ensure SAMs contain appropriate keys for your environment
- **Network Security**: Use HTTPS in production environments
- **Access Control**: Implement authentication for administrative functions
- **Audit Logging**: Enable comprehensive transaction logging
- **Key Management**: Secure storage and rotation of cryptographic keys

## License

This server application is part of the Keyple Demo project and is licensed under the BSD 3-Clause License.