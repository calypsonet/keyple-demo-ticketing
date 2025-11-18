################################################################################
# Source files collection
################################################################################

set(CORE_SOURCES
    core/logging/Logger.cpp
    core/constants/Messages.cpp
)

set(DATA_SOURCES
    data/repository/LocationRepository.cpp
    data/repository/ReaderRepository.cpp
    data/repository/CalypsoCardRepository.cpp
    data/repository/StorageCardRepository.cpp
)

set(DOMAIN_SOURCES
    domain/service/TicketingService.cpp
    domain/rules/ValidationRules.cpp
    domain/model/Location.cpp
    domain/model/Validation.cpp
    domain/model/CardReaderResponse.cpp
    domain/model/AppSettings.cpp
    domain/mapper/LocationMapper.cpp
    domain/mapper/ValidationMapper.cpp
)

set(UI_SOURCES
    ui/main/MainWindow.cpp
    ui/splash/SplashScreen.cpp
    ui/settings/SettingsDialog.cpp
    ui/reader/ReaderWidget.cpp
    ui/cardsummary/CardSummaryDialog.cpp
)

set(UI_FORMS
    ui/main/MainWindow.ui
    ui/splash/SplashScreen.ui
    ui/settings/SettingsDialog.ui
    ui/reader/ReaderWidget.ui
    ui/cardsummary/CardSummaryDialog.ui
)

set(RESOURCES
    ../resources/resources.qrc
)

# Headers (pour IDE)
set(HEADERS
    core/logging/Logger.h
    core/constants/Messages.h
    data/repository/LocationRepository.h
    data/repository/ReaderRepository.h
    data/repository/CalypsoCardRepository.h
    data/repository/StorageCardRepository.h
    domain/service/TicketingService.h
    domain/rules/ValidationRules.h
    domain/exception/ValidationException.h
    domain/model/Status.h
    domain/model/Location.h
    domain/model/Validation.h
    domain/model/CardReaderResponse.h
    domain/model/AppSettings.h
    domain/model/ReaderType.h
    domain/model/CardProtocolEnum.h
    domain/mapper/LocationMapper.h
    domain/mapper/ValidationMapper.h
    ui/main/MainWindow.h
    ui/splash/SplashScreen.h
    ui/settings/SettingsDialog.h
    ui/reader/ReaderWidget.h
    ui/cardsummary/CardSummaryDialog.h
)

################################################################################
# Executable
################################################################################

add_executable(validation
    main.cpp
    ${CORE_SOURCES}
    ${DATA_SOURCES}
    ${DOMAIN_SOURCES}
    ${UI_SOURCES}
    ${UI_FORMS}
    ${RESOURCES}
    ${HEADERS}
)

################################################################################
# Include directories
################################################################################

target_include_directories(validation PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_BINARY_DIR}  # Pour les fichiers générés (ui_*.h)
)

################################################################################
# Link libraries
################################################################################

target_link_libraries(validation PRIVATE
    # Qt
    Qt6::Core
    Qt6::Widgets
    Qt6::Concurrent
    Qt6::Multimedia

    # Keyple (adapter selon les noms de vos targets)
    # KeypleCore::KeypleCore
    # KeypleCardCalypso::KeypleCardCalypso
    # KeyplePluginPcsc::KeyplePluginPcsc
    # KeypleStorageCard::KeypleStorageCard

    # Logging
    spdlog::spdlog

    # PC/SC
    ${PCSC_LIBRARIES}
)

# Fruit DI (si activé)
if(USE_FRUIT_DI)
    target_sources(validation PRIVATE core/di/Container.cpp)
    target_link_libraries(validation PRIVATE Fruit::fruit)
endif()

################################################################################
# Compiler options
################################################################################

if(MSVC)
    target_compile_options(validation PRIVATE /W4 /WX-)
else()
    target_compile_options(validation PRIVATE -Wall -Wextra -Wpedantic)
endif()

# Release optimizations
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(MSVC)
        target_compile_options(validation PRIVATE /O2)
    else()
        target_compile_options(validation PRIVATE -O3 -march=native)
    endif()
endif()

################################################################################
# Properties
################################################################################

set_target_properties(validation PROPERTIES
    OUTPUT_NAME "validation"
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)

# Enable Qt debug info in Debug mode
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    target_compile_definitions(validation PRIVATE QT_QML_DEBUG)
endif()
