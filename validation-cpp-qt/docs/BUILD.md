# Guide de Build - Keyple Validation C++/Qt

Instructions détaillées pour compiler le projet sur différentes plateformes.

## Prérequis

### Tous systèmes

- **CMake** >= 3.16
- **Compilateur C++17**:
  - Linux: GCC 9+ ou Clang 10+
  - Windows: MSVC 2019+ ou MinGW
  - macOS: Clang 10+ (Xcode)

- **Qt 6**: Core, Widgets, Concurrent, Multimedia
- **spdlog**: Bibliothèque de logging
- **PC/SC**: Accès lecteurs de cartes
  - Linux: pcsclite-dev
  - Windows: Inclus (WinSCard)
  - macOS: Inclus (PCSC framework)

- **Keyple C++**: Framework de cartes à puce
  - Keyple Core
  - Keyple Card Calypso
  - Keyple Plugin PC/SC
  - Keyple Storage Card

## Installation des dépendances

### Windows

#### Option 1: vcpkg (recommandé)

```powershell
# Installer vcpkg
git clone https://github.com/Microsoft/vcpkg.git
cd vcpkg
.\bootstrap-vcpkg.bat

# Installer dépendances
.\vcpkg install qt6:x64-windows
.\vcpkg install spdlog:x64-windows

# (Optionnel) Fruit DI
.\vcpkg install fruit:x64-windows
```

#### Option 2: Installeurs

- **Qt**: https://www.qt.io/download-qt-installer
- **spdlog**: Build depuis source

#### Keyple C++ (build manuel requis)

```powershell
git clone https://github.com/eclipse-keyple/keyple-cpp.git
cd keyple-cpp
mkdir build && cd build
cmake -DCMAKE_INSTALL_PREFIX=C:/Keyple ..
cmake --build . --config Release
cmake --install .
```

### Linux (Ubuntu/Debian)

```bash
# Qt 6
sudo apt update
sudo apt install qt6-base-dev qt6-multimedia-dev cmake build-essential

# PC/SC
sudo apt install libpcsclite-dev pcscd

# spdlog
sudo apt install libspdlog-dev

# (Optionnel) Fruit DI
git clone https://github.com/google/fruit.git
cd fruit && mkdir build && cd build
cmake .. && make && sudo make install

# Keyple C++ (build manuel)
git clone https://github.com/eclipse-keyple/keyple-cpp.git
cd keyple-cpp && mkdir build && cd build
cmake -DCMAKE_INSTALL_PREFIX=/usr/local ..
make && sudo make install
```

### macOS

```bash
# Homebrew
brew install qt@6
brew install spdlog
brew install cmake

# (Optionnel) Fruit DI
brew install fruit

# Keyple C++ (build manuel)
git clone https://github.com/eclipse-keyple/keyple-cpp.git
cd keyple-cpp && mkdir build && cd build
cmake -DCMAKE_INSTALL_PREFIX=/usr/local ..
make && sudo make install
```

## Build du projet

### Configuration générale

```bash
cd keyple-validation-qt
mkdir build && cd build
```

### Linux / macOS

```bash
# Configuration
cmake .. -DCMAKE_BUILD_TYPE=Release \
         -DCMAKE_PREFIX_PATH=/path/to/Qt/6.x.x/gcc_64

# Compilation
cmake --build . -j$(nproc)

# Installation (optionnel)
sudo cmake --install .

# Exécution
./validation
```

### Windows (Visual Studio)

```powershell
# Configuration avec vcpkg
cmake .. -DCMAKE_TOOLCHAIN_FILE=C:/vcpkg/scripts/buildsystems/vcpkg.cmake `
         -DCMAKE_PREFIX_PATH=C:/Qt/6.x.x/msvc2019_64

# Compilation
cmake --build . --config Release

# Déployer DLLs Qt
cd Release
C:\Qt\6.x.x\msvc2019_64\bin\windeployqt.exe validation.exe

# Exécution
.\validation.exe
```

### Windows (MinGW)

```bash
# Configuration
cmake .. -G "MinGW Makefiles" \
         -DCMAKE_PREFIX_PATH=C:/Qt/6.x.x/mingw_64

# Compilation
cmake --build . -j8

# Exécution
./validation.exe
```

## Options CMake

### Options disponibles

```bash
cmake .. \
  -DCMAKE_BUILD_TYPE=Release \           # Release, Debug, RelWithDebInfo
  -DUSE_FRUIT_DI=ON \                    # Activer Fruit DI (défaut: OFF)
  -DBUILD_TESTING=ON \                   # Compiler tests (défaut: OFF)
  -DCMAKE_INSTALL_PREFIX=/usr/local \    # Répertoire installation
  -DCMAKE_PREFIX_PATH=/path/to/Qt        # Chemin vers Qt
```

### Exemples

**Build Debug avec tests**:
```bash
cmake .. -DCMAKE_BUILD_TYPE=Debug -DBUILD_TESTING=ON
cmake --build .
ctest --verbose
```

**Build Release optimisé**:
```bash
cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_CXX_FLAGS="-O3 -march=native"
cmake --build .
```

**Avec Fruit DI**:
```bash
cmake .. -DUSE_FRUIT_DI=ON
cmake --build .
```

## Résolution de problèmes

### Qt non trouvé

**Erreur**: `Could not find Qt6Core`

**Solution**:
```bash
# Spécifier le chemin Qt
cmake .. -DCMAKE_PREFIX_PATH=/path/to/Qt/6.x.x/gcc_64

# Ou définir variable d'environnement
export CMAKE_PREFIX_PATH=/path/to/Qt/6.x.x/gcc_64
cmake ..
```

### Keyple non trouvé

**Erreur**: `Could not find package KeypleCore`

**Solution**:
```bash
# Vérifier installation Keyple
ls /usr/local/lib/cmake/Keyple*

# Si absent, réinstaller Keyple avec CMAKE_INSTALL_PREFIX correct
cd keyple-cpp/build
cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local
sudo make install

# Puis spécifier le chemin
cmake .. -DKeyple_DIR=/usr/local/lib/cmake/KeypleCore
```

### PC/SC non trouvé (Linux)

**Erreur**: `Could not find PCSC`

**Solution**:
```bash
# Installer pcsclite
sudo apt install libpcsclite-dev pcscd

# Vérifier installation
pkg-config --modversion libpcsclite

# Démarrer daemon
sudo systemctl start pcscd
sudo systemctl enable pcscd
```

### spdlog non trouvé

**Erreur**: `Could not find spdlog`

**Solution**:
```bash
# Option 1: Installer via package manager
sudo apt install libspdlog-dev    # Ubuntu
brew install spdlog                # macOS
vcpkg install spdlog               # Windows

# Option 2: Build depuis source
git clone https://github.com/gabime/spdlog.git
cd spdlog && mkdir build && cd build
cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local
sudo make install
```

### DLL manquantes (Windows)

**Erreur**: `The program can't start because Qt6Core.dll is missing`

**Solution**:
```powershell
# Utiliser windeployqt
cd build/Release
C:\Qt\6.x.x\msvc2019_64\bin\windeployqt.exe validation.exe

# Ou copier manuellement les DLLs Qt dans le répertoire de l'exe
```

## Build depuis IDE

### Qt Creator

1. Ouvrir `CMakeLists.txt`
2. Configurer Kit (Desktop Qt 6.x.x)
3. Build → Build Project
4. Run → Run

### Visual Studio

1. Ouvrir dossier (File → Open → Folder)
2. Sélectionner `cpp-qt-template/`
3. CMake configuration automatique
4. Build → Build All
5. Debug → Start Debugging

### CLion

1. Ouvrir projet (CMakeLists.txt)
2. Configurer toolchain (Settings → Build → Toolchains)
3. Build → Build Project
4. Run → Run 'validation'

## Vérification de l'installation

### Test basique

```bash
# Vérifier exécutable
./validation --version

# Lancer avec logs debug
./validation --log-level=debug

# Test lecteur PC/SC
pcsc_scan    # Linux/macOS
certutil -scinfo    # Windows
```

### Tests unitaires

```bash
# Build avec tests
cmake .. -DBUILD_TESTING=ON
cmake --build .

# Exécuter tous les tests
ctest --verbose

# Exécuter test spécifique
ctest -R ValidationRules --verbose
```

## Performance

### Temps de compilation typiques

| Configuration | Temps | Threads |
|---------------|-------|---------|
| Debug (first build) | 3-5 min | 8 cores |
| Release (first build) | 4-6 min | 8 cores |
| Incremental | 10-30s | 8 cores |

### Accélération

```bash
# Utiliser tous les cores
cmake --build . -j$(nproc)    # Linux/macOS
cmake --build . -j8           # Windows

# Utiliser ccache (Linux/macOS)
sudo apt install ccache
cmake .. -DCMAKE_CXX_COMPILER_LAUNCHER=ccache
```

## Packaging

### Linux (AppImage)

```bash
# Utiliser linuxdeployqt
./linuxdeployqt validation -appimage
```

### Windows (Installer)

```bash
# Utiliser NSIS ou Inno Setup
# Créer setup.iss pour Inno Setup
iscc setup.iss
```

### macOS (DMG)

```bash
# Utiliser macdeployqt
macdeployqt validation.app -dmg
```

## Support

- **CMake issues**: https://cmake.org/cmake/help/latest/
- **Qt build**: https://doc.qt.io/qt-6/cmake-manual.html
- **Keyple**: https://github.com/eclipse-keyple/keyple-cpp/issues
