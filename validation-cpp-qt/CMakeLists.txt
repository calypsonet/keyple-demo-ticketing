cmake_minimum_required(VERSION 3.16)

project(keyple-validation
    VERSION 1.0.0
    DESCRIPTION "Keyple Validation Desktop Application"
    LANGUAGES CXX
)

################################################################################
# C++ Standard
################################################################################
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

################################################################################
# Qt Configuration
################################################################################
set(CMAKE_AUTOMOC ON)  # Meta-Object Compiler (pour Q_OBJECT)
set(CMAKE_AUTOUIC ON)  # UI Compiler (.ui -> ui_*.h)
set(CMAKE_AUTORCC ON)  # Resource Compiler (.qrc)

# Recherche Qt6
find_package(Qt6 REQUIRED COMPONENTS
    Core
    Widgets
    Concurrent
    Multimedia
)

################################################################################
# Keyple C++ Libraries
################################################################################
# NOTE: Adapter selon votre installation de Keyple C++
find_package(KeypleCore CONFIG REQUIRED)
find_package(KeypleCardCalypso CONFIG REQUIRED)
find_package(KeyplePluginPcsc CONFIG REQUIRED)
find_package(KeypleStorageCard CONFIG REQUIRED)

################################################################################
# Logging : spdlog
################################################################################
find_package(spdlog REQUIRED)

################################################################################
# Dependency Injection : Fruit (optionnel)
################################################################################
option(USE_FRUIT_DI "Use Fruit for dependency injection" OFF)
if(USE_FRUIT_DI)
    find_package(Fruit REQUIRED)
endif()

################################################################################
# PC/SC Library
################################################################################
if(WIN32)
    # Windows : WinSCard inclus par défaut
    set(PCSC_LIBRARIES winscard)
elseif(APPLE)
    # macOS : PCSC framework inclus
    find_library(PCSC_FRAMEWORK PCSC REQUIRED)
    set(PCSC_LIBRARIES ${PCSC_FRAMEWORK})
else()
    # Linux : pcsclite
    find_package(PkgConfig REQUIRED)
    pkg_check_modules(PCSC REQUIRED libpcsclite)
endif()

################################################################################
# Options de build
################################################################################
option(BUILD_TESTING "Build unit tests" OFF)

################################################################################
# Sous-projets
################################################################################
add_subdirectory(src)

# Tests (optionnel)
if(BUILD_TESTING)
    enable_testing()
    add_subdirectory(tests)
endif()

################################################################################
# Installation
################################################################################
install(TARGETS validation
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
)

# Installer les DLLs Qt sur Windows
if(WIN32)
    # windeployqt sera appelé manuellement après build
    message(STATUS "After build, run: windeployqt validation.exe")
endif()

################################################################################
# Configuration summary
################################################################################
message(STATUS "")
message(STATUS "═══════════════════════════════════════════════")
message(STATUS "  Keyple Validation - Configuration Summary")
message(STATUS "═══════════════════════════════════════════════")
message(STATUS "  CMake version    : ${CMAKE_VERSION}")
message(STATUS "  C++ compiler     : ${CMAKE_CXX_COMPILER_ID} ${CMAKE_CXX_COMPILER_VERSION}")
message(STATUS "  C++ standard     : C++${CMAKE_CXX_STANDARD}")
message(STATUS "  Build type       : ${CMAKE_BUILD_TYPE}")
message(STATUS "  Qt version       : ${Qt6_VERSION}")
message(STATUS "  Install prefix   : ${CMAKE_INSTALL_PREFIX}")
message(STATUS "  Use Fruit DI     : ${USE_FRUIT_DI}")
message(STATUS "  Build tests      : ${BUILD_TESTING}")
message(STATUS "═══════════════════════════════════════════════")
message(STATUS "")
