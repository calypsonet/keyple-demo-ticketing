# Keyple Validation - C++/Qt Template

Template de migration de l'application Android Keyple Validation vers C++/Qt avec lecteurs PC/SC.

## 📋 Prérequis

### Outils de build
- **CMake** >= 3.16
- **Compilateur C++17** (GCC 9+, Clang 10+, MSVC 2019+)
- **Qt 6** (Widgets, Concurrent, Multimedia)

### Bibliothèques
- **Keyple C++** (core, card-calypso, plugin-pcsc)
- **spdlog** (logging)
- **Fruit** (dependency injection, optionnel)
- **PC/SC** (pcsclite sur Linux, WinSCard sur Windows)

## 🛠️ Installation des dépendances

### Windows
```bash
# Qt
choco install qt6

# vcpkg pour les dépendances C++
vcpkg install spdlog:x64-windows
vcpkg install fruit:x64-windows

# PC/SC inclus dans Windows (WinSCard.dll)
```

### Linux (Ubuntu/Debian)
```bash
# Qt
sudo apt install qt6-base-dev qt6-multimedia-dev

# PC/SC
sudo apt install libpcsclite-dev pcscd

# Autres dépendances
sudo apt install libspdlog-dev

# Fruit (optionnel, build depuis source)
git clone https://github.com/google/fruit.git
cd fruit && mkdir build && cd build
cmake .. && make && sudo make install
```

### macOS
```bash
# Qt
brew install qt@6

# PC/SC inclus dans macOS
# spdlog
brew install spdlog

# Fruit (optionnel)
brew install fruit
```

## 🔧 Configuration Keyple C++

Les bibliothèques Keyple C++ doivent être installées séparément :

```bash
# Cloner et installer Keyple C++
git clone https://github.com/eclipse-keyple/keyple-cpp.git
cd keyple-cpp

# Build et install
mkdir build && cd build
cmake -DCMAKE_INSTALL_PREFIX=/usr/local ..
make
sudo make install
```

## 🏗️ Build du projet

```bash
# Cloner le projet
cd keyple-validation-qt

# Créer le répertoire de build
mkdir build && cd build

# Configurer avec CMake
cmake .. -DCMAKE_PREFIX_PATH=/path/to/Qt/6.x.x/gcc_64

# Compiler
cmake --build . -j8

# Exécuter
./validation
```

### Build avec vcpkg (Windows)
```bash
cmake .. -DCMAKE_TOOLCHAIN_FILE=C:/vcpkg/scripts/buildsystems/vcpkg.cmake
cmake --build . --config Release
```

## 📁 Structure du projet

```
keyple-validation-qt/
├── CMakeLists.txt              # Configuration CMake principale
├── src/
│   ├── main.cpp                # Point d'entrée Qt
│   ├── core/                   # Infrastructure (logging, DI, constants)
│   ├── data/                   # Repositories (accès données/hardware)
│   ├── domain/                 # Logique métier (services, règles, modèles)
│   └── ui/                     # Interface Qt (widgets, dialogs)
├── resources/                  # Ressources Qt (images, animations)
└── docs/                       # Documentation
```

## 🎯 Architecture

L'application suit une **Clean Architecture** en 3 couches :

1. **UI (Presentation)** : QWidget/QML, gestion des événements utilisateur
2. **Domain (Business)** : Logique métier, règles de validation
3. **Data (Repository)** : Accès aux lecteurs PC/SC, données

Voir [ARCHITECTURE.md](docs/ARCHITECTURE.md) pour plus de détails.

## 🔌 Configuration des lecteurs PC/SC

### Tester la disponibilité des lecteurs
```bash
# Linux/macOS
pcsc_scan

# Liste des lecteurs
opensc-tool --list-readers

# Windows
certutil -scinfo
```

### Lecteurs compatibles testés
- ✅ ACS ACR122U (contactless)
- ✅ Identiv SCR3500 (contact + contactless)
- ✅ HID Omnikey 5022 (contact)

## 🚀 Utilisation

1. **Connecter un lecteur PC/SC** compatible
2. **Lancer l'application**
3. **Configurer** : Sélectionner la localisation dans Settings
4. **Valider** : Présenter une carte Calypso ou Storage Card

## 🧪 Tests

```bash
# Build avec tests
cmake .. -DBUILD_TESTING=ON
cmake --build .

# Exécuter les tests
ctest --verbose
```

## 📝 TODO Migration

- [x] Structure de base CMake + Qt
- [x] Modèles de domaine (Location, Status, etc.)
- [ ] ReaderRepository avec PC/SC
- [ ] CalypsoCardRepository
- [ ] StorageCardRepository
- [ ] TicketingService
- [ ] ValidationRules
- [ ] UI Widgets (MainWindow, Settings, Reader, CardSummary)
- [ ] Animations et ressources
- [ ] Tests unitaires

## 📚 Documentation

- [Architecture détaillée](docs/ARCHITECTURE.md)
- [Guide de migration Android → Qt](docs/MIGRATION.md)
- [API Keyple C++](https://keyple.org/)
- [Documentation Qt 6](https://doc.qt.io/qt-6/)

## 📄 Licence

BSD 3-Clause (identique au projet Android d'origine)

## 🤝 Contribution

Voir le projet Android d'origine : [keyple-demo-ticketing](https://github.com/calypsonet/keyple-demo-ticketing)
