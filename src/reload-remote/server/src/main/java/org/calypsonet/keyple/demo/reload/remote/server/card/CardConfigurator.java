/* ******************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.reload.remote.server.card;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.calypsonet.keyple.card.storagecard.StorageCardExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService;
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.*;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObservationExceptionHandlerSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.distributed.RemotePluginServer;
import org.eclipse.keyple.distributed.RemotePluginServerFactoryBuilder;
import org.eclipse.keyple.distributed.RemoteReaderServer;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.eclipse.keyple.plugin.pcsc.PcscReader;
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set up of the Keyple infrastructure of the server: card extensions, PC/SC plugin and card
 * resource service of the SAM, and remote plugin receiving the requests of the clients, whose
 * remote services are executed by the {@link RemoteServiceDispatcher}.
 */
@ApplicationScoped
public class CardConfigurator {

  private static final Logger logger = LoggerFactory.getLogger(CardConfigurator.class);

  static final String REMOTE_PLUGIN_NAME = "REMOTE_PLUGIN_#1";
  static final String SAM_RESOURCE_PROFILE_NAME = "SAM C1";

  @ConfigProperty(name = "sam.pcsc.reader.filter")
  String samReaderFilter;

  @Inject CardSamObserver cardSamObserver;
  @Inject RemoteServiceDispatcher remoteServiceDispatcher;

  public void init() {
    registerCardExtensions();
    initSamPlugin();
    initCardPlugin();
  }

  private void registerCardExtensions() {
    // Card extensions outside the "org.eclipse.keyple" package must be registered so that the
    // types they provide (for example, the initial card content) can be rebuilt from JSON data.
    StorageCardExtensionService storageCardExtension = StorageCardExtensionService.getInstance();
    if (storageCardExtension == null) {
      // E.g. mocked storage card library
      logger.warn("Storage card extension not available: storage cards are not supported");
      return;
    }
    SmartCardServiceProvider.getService().checkCardExtension(storageCardExtension);
  }

  private void initSamPlugin() {
    // Register the SAM plugin
    SmartCardService smartCardService = SmartCardServiceProvider.getService();
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());
    if (plugin.getReaders().isEmpty()) {
      // The server starts anyway: the SAM reader is taken into account as soon as it is connected,
      // the dashboard showing meanwhile that the SAM is not available.
      logger.warn("No PC/SC reader connected: waiting for the SAM reader to be connected");
    }
    // Initialize the associated card resource service
    initCardResourceService(plugin);

    // Start monitoring the SAM plugin (reader connection) and reader (SAM insertion)
    cardSamObserver.startMonitoring((ObservablePlugin) plugin);
  }

  private void initCardResourceService(Plugin plugin) {
    logger.info(
        "Card resource service set up [plugin={}, readerNameRegex={}, samProfile={}]",
        plugin.getName(),
        samReaderFilter,
        SAM_RESOURCE_PROFILE_NAME);

    // Create a card resource extension expecting a SAM "C1".
    CardResourceProfileExtension samResourceProfileExtension =
        LegacySamExtensionService.getInstance()
            .createLegacySamResourceProfileExtension(
                LegacySamExtensionService.getInstance()
                    .getLegacySamApiFactory()
                    .createLegacySamSelectionExtension(),
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null));

    // Create a minimalist configuration (no plugin/reader observation)
    CardResourceService cardResourceService = CardResourceServiceProvider.getService();
    cardResourceService
        .getConfigurator()
        .withPlugins(
            PluginsConfigurator.builder()
                .addPluginWithMonitoring(
                    plugin,
                    new SamReaderConfigurator(),
                    new MonitoringExceptionHandler(),
                    new MonitoringExceptionHandler())
                .build())
        .withCardResourceProfiles(
            CardResourceProfileConfigurator.builder(
                    SAM_RESOURCE_PROFILE_NAME, samResourceProfileExtension)
                .withReaderNameRegex(samReaderFilter)
                .build())
        .configure();
    cardResourceService.start();
    // verify the resource availability
    CardResource cardResource = cardResourceService.getCardResource(SAM_RESOURCE_PROFILE_NAME);
    if (cardResource == null) {
      // E.g. no reader connected or no SAM inserted: the card resource service monitors the plugin
      // and its readers, so the SAM resource becomes available as soon as a SAM is detected.
      logger.warn(
          "No SAM card resource available yet [samProfile={}, readerNameRegex={}, plugin={}]",
          SAM_RESOURCE_PROFILE_NAME,
          samReaderFilter,
          plugin.getName());
      return;
    }
    cardResourceService.releaseCardResource(cardResource);
  }

  private static class SamReaderConfigurator implements ReaderConfiguratorSpi {
    @Override
    public void setupReader(CardReader cardReader) {
      try {
        PcscReader readerExtension =
            SmartCardServiceProvider.getService()
                .getPlugin(cardReader)
                .getReaderExtension(PcscReader.class, cardReader.getName());
        readerExtension
            .setContactless(false)
            .setIsoProtocol(PcscReader.IsoProtocol.T0)
            .setSharingMode(PcscReader.SharingMode.SHARED);
      } catch (Exception e) {
        logger.error("Error while setting up the SAM reader [reader={}]", cardReader.getName(), e);
      }
    }
  }

  private void initCardPlugin() {
    // Register the remote plugin to the smart card service using the factory.
    ObservablePlugin plugin =
        (ObservablePlugin)
            SmartCardServiceProvider.getService()
                .registerPlugin(
                    RemotePluginServerFactoryBuilder.builder(REMOTE_PLUGIN_NAME)
                        .withSyncNode()
                        .build());
    // Init the remote plugin observer.
    plugin.setPluginObservationExceptionHandler(new CardRemotePluginObservationExceptionHandler());
    // Add the main observer
    plugin.addObserver(new CardRemotePluginObserver());
  }

  private static class CardRemotePluginObservationExceptionHandler
      implements PluginObservationExceptionHandlerSpi {
    @Override
    public void onPluginObservationError(String pluginName, Throwable e) {
      logger.error("Error while observing the remote plugin [plugin={}]", pluginName, e);
    }
  }

  private class CardRemotePluginObserver implements PluginObserverSpi {

    @Override
    public void onPluginEvent(PluginEvent pluginEvent) {

      // For a RemotePluginServer, the events can only be of the type READER_CONNECTED.
      // So there is no need to analyze the event type.
      logger.info(
          "Plugin event received [type={}, plugin={}, reader={}]",
          pluginEvent.getType(),
          pluginEvent.getPluginName(),
          pluginEvent.getReaderNames().first());

      // Retrieves the remote plugin using the plugin name contained in the event.
      ObservablePlugin plugin =
          (ObservablePlugin)
              SmartCardServiceProvider.getService().getPlugin(pluginEvent.getPluginName());
      RemotePluginServer pluginExtension = plugin.getExtension(RemotePluginServer.class);

      // Retrieves the name of the remote reader using the first reader name contains in the event.
      // Note that for a RemotePluginServer, there can be only one reader per event.
      String readerName = pluginEvent.getReaderNames().first();

      // Retrieves the remote reader from the plugin using the reader name.
      CardReader reader = plugin.getReader(readerName);
      RemoteReaderServer readerExtension =
          plugin.getReaderExtension(RemoteReaderServer.class, reader.getName());
      // Executes the business service matching the Service ID specified by the client.
      Object outputData = remoteServiceDispatcher.execute(reader, readerExtension);

      // Terminates the business service by providing the reader name and the optional output data.
      pluginExtension.endRemoteService(readerName, outputData);
    }
  }

  private static class MonitoringExceptionHandler
      implements PluginObservationExceptionHandlerSpi, CardReaderObservationExceptionHandlerSpi {
    @Override
    public void onPluginObservationError(String pluginName, Throwable e) {
      logger.error("Error while monitoring the SAM plugin [plugin={}]", pluginName, e);
    }

    @Override
    public void onReaderObservationError(String contextInfo, String readerName, Throwable e) {
      logger.error(
          "Error while monitoring the SAM reader [context={}, reader={}]",
          contextInfo,
          readerName,
          e);
    }
  }
}
