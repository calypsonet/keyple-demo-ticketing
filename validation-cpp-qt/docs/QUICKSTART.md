# Quick Start Guide

Guide de démarrage rapide en 5 étapes.

## 1. Installation express (Ubuntu/Debian)

```bash
# Dépendances système
sudo apt update
sudo apt install -y \
    cmake build-essential \
    qt6-base-dev qt6-multimedia-dev \
    libpcsclite-dev pcscd \
    libspdlog-dev \
    git

# Démarrer daemon PC/SC
sudo systemctl start pcscd
sudo systemctl enable pcscd

# (Optionnel) Vérifier lecteur
pcsc_scan
```

## 2. Build Keyple C++

```bash
cd /tmp
git clone https://github.com/eclipse-keyple/keyple-cpp.git
cd keyple-cpp
mkdir build && cd build
cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local
make -j$(nproc)
sudo make install
```

## 3. Build application

```bash
cd keyple-validation-qt
mkdir build && cd build

# Configuration
cmake .. -DCMAKE_BUILD_TYPE=Release \
         -DCMAKE_PREFIX_PATH=/usr/lib/x86_64-linux-gnu/cmake/Qt6

# Compilation
cmake --build . -j$(nproc)
```

## 4. Exécution

```bash
cd bin
./validation
```

## 5. Test avec carte

1. Connecter lecteur PC/SC (ex: ACR122U)
2. Lancer l'application
3. Sélectionner localisation dans Settings
4. Cliquer "Start"
5. Présenter carte Calypso ou MIFARE

---

## Démarrage Windows (vcpkg)

```powershell
# 1. Installer vcpkg
git clone https://github.com/Microsoft/vcpkg.git
cd vcpkg
.\bootstrap-vcpkg.bat

# 2. Installer dépendances
.\vcpkg install qt6:x64-windows spdlog:x64-windows

# 3. Build Keyple (manuel)
# Voir BUILD.md

# 4. Build application
cd keyple-validation-qt
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=C:/vcpkg/scripts/buildsystems/vcpkg.cmake
cmake --build . --config Release

# 5. Déployer DLLs Qt
cd Release
C:\Qt\6.x.x\msvc2019_64\bin\windeployqt.exe validation.exe

# 6. Exécuter
.\validation.exe
```

---

## Démarrage macOS

```bash
# 1. Installer dépendances
brew install qt@6 spdlog cmake

# 2-5. Identique à Linux
# Voir section Ubuntu ci-dessus
```

---

## Structure minimale à implémenter

Pour avoir une application fonctionnelle:

### Phase 1: Stub UI (déjà fait ✅)
- [x] Splash → Settings → Reader → Results
- [x] Modèles de domaine
- [x] Stubs repositories

### Phase 2: Intégration Keyple (TODO)
```cpp
// ReaderRepository.cpp
void ReaderRepository::registerPlugin() {
    auto& service = SmartCardServiceProvider::getService();
    m_pcscPlugin = service.registerPlugin(
        PcscPluginFactoryBuilder::builder()->build()
    );
}

std::shared_ptr<CardReader> ReaderRepository::initCardReader() {
    auto readers = m_pcscPlugin->getReaders();
    // Trouver lecteur contactless
    // Activer protocoles
    return m_cardReader;
}
```

### Phase 3: Validation Calypso (TODO)
```cpp
// CalypsoCardRepository.cpp
CardReaderResponse CalypsoCardRepository::executeValidationProcedure(...) {
    // 1. Open secure session
    // 2. Read environment/event/contracts
    // 3. Validate with ValidationRules
    // 4. Process best contract
    // 5. Write event
    // 6. Close session
    return response;
}
```

---

## Tester sans hardware

### Mock lecteur PC/SC

```cpp
// Pour tests sans lecteur réel
#define MOCK_PCSC 1

#ifdef MOCK_PCSC
    // Retourner stub responses
    return CardReaderResponse(
        Status::SUCCESS,
        "MOCK: Calypso Card",
        "Multi-trip",
        std::nullopt,
        10
    );
#endif
```

---

## Prochaines étapes

1. ✅ **Build template** (fait)
2. 🔄 **Intégrer Keyple C++** (en cours)
3. ⏳ **Implémenter ReaderRepository PC/SC**
4. ⏳ **Implémenter CalypsoCardRepository**
5. ⏳ **Tests avec carte réelle**

---

## Ressources

- 📚 [Architecture complète](ARCHITECTURE.md)
- 🔧 [Guide de build détaillé](BUILD.md)
- 🔄 [Guide de migration](MIGRATION.md)
- 🌐 [Keyple docs](https://keyple.org/)
- 🌐 [Qt docs](https://doc.qt.io/qt-6/)
