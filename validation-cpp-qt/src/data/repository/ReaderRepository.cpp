/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#include "ReaderRepository.h"
#include "core/logging/Logger.h"

// TODO: Uncomment when Keyple C++ is integrated
// #include <keyple/core/service/SmartCardServiceProvider.h>
// #include <keyple/plugin/pcsc/PcscPluginFactoryBuilder.h>

namespace data::repository {

ReaderRepository::ReaderRepository()
{
    core::Logger::info("ReaderRepository created");
}

ReaderRepository::~ReaderRepository()
{
    clear();
}

void ReaderRepository::registerPlugin()
{
    core::Logger::info("Registering PC/SC plugin...");

    // TODO: Implémenter avec Keyple C++
    /*
    auto& service = SmartCardServiceProvider::getService();

    // Register PC/SC plugin
    auto pluginFactory = PcscPluginFactoryBuilder::builder()->build();
    m_pcscPlugin = service.registerPlugin(pluginFactory);

    core::Logger::info("PC/SC plugin registered: {}", m_pcscPlugin->getName());
    */

    core::Logger::warn("PC/SC plugin registration not implemented yet");
}

/*
std::shared_ptr<CardReader> ReaderRepository::initCardReader()
{
    core::Logger::info("Initializing contactless card reader...");

    if (!m_pcscPlugin) {
        throw std::runtime_error("PC/SC plugin not registered");
    }

    // List available readers
    auto readers = m_pcscPlugin->getReaders();
    core::Logger::info("Found {} PC/SC reader(s)", readers.size());

    for (const auto& reader : readers) {
        core::Logger::info("  - {}", reader->getName());
    }

    // Try to find common contactless readers
    std::vector<std::string> contactlessPatterns = {
        "ACR122",      // ACS ACR122U
        "SCR3500",     // Identiv SCR3500
        "Omnikey 5022" // HID Omnikey
    };

    for (const auto& pattern : contactlessPatterns) {
        m_cardReader = findReaderByName(pattern);
        if (m_cardReader) {
            core::Logger::info("Selected contactless reader: {}", m_cardReader->getName());
            activateProtocols(m_cardReader);
            return m_cardReader;
        }
    }

    throw std::runtime_error("No contactless PC/SC reader found");
}

std::vector<std::shared_ptr<CardReader>> ReaderRepository::initSamReaders()
{
    core::Logger::info("Initializing SAM readers...");

    if (!m_pcscPlugin) {
        throw std::runtime_error("PC/SC plugin not registered");
    }

    auto readers = m_pcscPlugin->getReaders();

    // Filter for contact readers (SAM)
    for (const auto& reader : readers) {
        std::string name = reader->getName();
        // SAM readers are typically contact readers
        if (name.find("SAM") != std::string::npos ||
            name.find("Contact") != std::string::npos) {
            m_samReaders.push_back(reader);
            core::Logger::info("Found SAM reader: {}", name);
        }
    }

    if (!m_samReaders.empty()) {
        m_samReader = m_samReaders[0];
    }

    return m_samReaders;
}

std::shared_ptr<CardReader> ReaderRepository::getCardReader() const
{
    return m_cardReader;
}

std::shared_ptr<CardReader> ReaderRepository::getSamReader() const
{
    return m_samReader;
}
*/

bool ReaderRepository::isStorageCardSupported() const
{
    // TODO: Check if MIFARE/ST25 protocols are activated
    return true; // Assume supported for now
}

bool ReaderRepository::displayResultSuccess() const
{
    // TODO: Play success sound with Qt Multimedia
    // QSound::play(":/sounds/success.wav");
    core::Logger::info("Success sound played");
    return true;
}

bool ReaderRepository::displayResultFailed() const
{
    // TODO: Play error sound with Qt Multimedia
    // QSound::play(":/sounds/error.wav");
    core::Logger::info("Error sound played");
    return true;
}

void ReaderRepository::clear()
{
    core::Logger::info("Clearing ReaderRepository...");

    // TODO: Unregister plugin and release readers
    /*
    m_cardReader.reset();
    m_samReader.reset();
    m_samReaders.clear();
    m_pcscPlugin.reset();
    */

    m_initialized = false;
}

/*
std::shared_ptr<CardReader> ReaderRepository::findReaderByName(const std::string& namePattern)
{
    if (!m_pcscPlugin) {
        return nullptr;
    }

    auto readers = m_pcscPlugin->getReaders();
    for (const auto& reader : readers) {
        if (reader->getName().find(namePattern) != std::string::npos) {
            return reader;
        }
    }

    return nullptr;
}

void ReaderRepository::activateProtocols(std::shared_ptr<CardReader> reader)
{
    core::Logger::info("Activating protocols on reader: {}", reader->getName());

    // Activate ISO 14443-4 for Calypso cards
    auto observable = std::dynamic_pointer_cast<ObservableCardReader>(reader);
    if (observable) {
        observable->activateProtocol("ISO_14443_4", "ISO14443-4");
        observable->activateProtocol("MIFARE_ULTRALIGHT", "MIFARE_UL");
        observable->activateProtocol("ST25_SRT512", "ST25");
        core::Logger::info("Protocols activated");
    }
}
*/

} // namespace data::repository
