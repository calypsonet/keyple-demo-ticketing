# TODO - Implémentation Keyple C++/Qt

Liste des tâches pour finaliser l'application.

## Phase 1: Configuration environnement

- [ ] Installer Qt 6.5+
- [ ] Installer spdlog
- [ ] Installer PC/SC (pcsclite ou WinSCard)
- [ ] Build Keyple C++ depuis source
- [ ] Tester compilation template vide

## Phase 2: Infrastructure (core/)

- [x] Logger avec spdlog
- [x] Messages constants
- [ ] DI avec Fruit (optionnel)
- [ ] Configuration management (QSettings)

## Phase 3: Repositories (data/repository/)

### ReaderRepository
- [ ] Inclure headers Keyple PC/SC
- [ ] Implémenter `registerPlugin()` avec PcscPluginFactory
- [ ] Implémenter `initCardReader()` avec recherche lecteur
  - [ ] Chercher patterns ACR122, SCR3500, Omnikey
  - [ ] Activer protocoles ISO 14443-4
  - [ ] Activer protocoles MIFARE/ST25
- [ ] Implémenter `initSamReaders()`
- [ ] Implémenter `displayResultSuccess()` avec QSound
- [ ] Implémenter `displayResultFailed()` avec QSound
- [ ] Tests unitaires

### CalypsoCardRepository
- [ ] Inclure headers Keyple Calypso
- [ ] Implémenter workflow complet:
  - [ ] `openSecureSession()`
  - [ ] `readEnvironment()`
  - [ ] `readEvent()`
  - [ ] `readContracts()` (1-4 contracts)
  - [ ] Validation avec ValidationRules
  - [ ] Filtrage contracts expirés
  - [ ] Tri par priorité
  - [ ] Sélection meilleur contract
  - [ ] Décrémentation compteur
  - [ ] `writeEvent()`
  - [ ] `closeSession()`
- [ ] Gestion erreurs Keyple
- [ ] Tests avec carte réelle

### StorageCardRepository
- [ ] Inclure headers Keyple StorageCard
- [ ] Implémenter workflow complet:
  - [ ] `readBlocks()` (environment, event, contract)
  - [ ] Validation environment/event/contract
  - [ ] Traitement par type contract
  - [ ] Décrémentation si counter-based
  - [ ] `writeBlocks()`
  - [ ] Close transaction
- [ ] Tests MIFARE Ultralight
- [ ] Tests ST25 SRT512

### LocationRepository
- [x] Implémentation basique (hardcodé)
- [ ] Charger depuis fichier JSON (optionnel)

## Phase 4: Services (domain/service/)

### TicketingService
- [ ] Inclure headers Keyple Core
- [ ] Implémenter `init()`:
  - [ ] Appel `readerRepository->registerPlugin()`
  - [ ] Appel `readerRepository->initCardReader()`
  - [ ] Appel `readerRepository->initSamReaders()`
  - [ ] Sélection SAM
  - [ ] Setup observers
- [ ] Implémenter `prepareAndScheduleCardSelectionScenario()`:
  - [ ] Keyple Generic (AID)
  - [ ] CD Light/GTML (AID)
  - [ ] Calypso Light (AID)
  - [ ] Navigo IDF (AID)
  - [ ] MIFARE Ultralight
  - [ ] ST25 SRT512
- [ ] Implémenter `startNfcDetection()`
- [ ] Implémenter `stopNfcDetection()`
- [ ] Implémenter `analyseSelectionResult()`:
  - [ ] Check active selection index
  - [ ] Verify AID match
  - [ ] Validate file structure
- [ ] Implémenter `executeValidationProcedure()`:
  - [ ] Route vers CalypsoCardRepository
  - [ ] Route vers StorageCardRepository
- [ ] Connecter signaux Qt
- [ ] Tests workflow complet

### ValidationRules
- [x] Implémentation basique
- [ ] Affiner constantes (CURRENT_VERSION, etc.)
- [ ] Tests unitaires pour chaque règle

## Phase 5: UI (ui/)

### SplashScreen
- [x] Structure de base
- [ ] Ajouter logo réel
- [ ] Transition animée vers Settings

### SettingsDialog
- [x] Structure de base
- [ ] Sauvegarder settings dans QSettings
- [ ] Restaurer dernière sélection
- [ ] Bouton "Time settings" vers système

### ReaderWidget
- [x] Structure de base
- [ ] Connecter à TicketingService réel
- [ ] Implémenter `initializeReaders()`
- [ ] Implémenter `startDetection()`
- [ ] Handler événements carte via signaux
- [ ] Animation GIF ou Lottie
- [ ] Progress dialog pendant init

### CardSummaryDialog
- [x] Structure de base
- [ ] Appliquer couleurs selon Status
- [ ] Afficher icônes (succès/warning/error)
- [ ] Implémenter `playAudioFeedback()`
- [ ] Auto-fermeture après X secondes (mode battery)

## Phase 6: Ressources

### Images
- [ ] Logo Calypso (resources/images/logo.png)
- [ ] Icône app (resources/images/icon.png)
- [ ] Icône succès (resources/images/success.png)
- [ ] Icône warning (resources/images/warning.png)
- [ ] Icône erreur (resources/images/error.png)

### Animations
- [ ] Card tap animation (GIF ou Lottie)
- [ ] Success animation
- [ ] Error animation

### Sons (optionnel)
- [ ] success.wav
- [ ] error.wav

## Phase 7: Tests

### Tests unitaires
- [ ] ValidationRules tests
- [ ] LocationMapper tests
- [ ] ValidationMapper tests
- [ ] Status enum tests

### Tests d'intégration
- [ ] ReaderRepository avec mock PC/SC
- [ ] CalypsoCardRepository avec mock card
- [ ] StorageCardRepository avec mock card
- [ ] TicketingService workflow complet

### Tests end-to-end
- [ ] Splash → Settings → Reader workflow
- [ ] Détection carte Calypso réelle
- [ ] Validation carte Calypso
- [ ] Détection carte MIFARE réelle
- [ ] Validation carte MIFARE
- [ ] Gestion erreurs

## Phase 8: Polish

### Performance
- [ ] Profiler avec Valgrind/Instruments
- [ ] Optimiser hot paths
- [ ] Réduire allocations mémoire

### UX
- [ ] Feedback visuel amélioré
- [ ] Transitions fluides
- [ ] Gestion timeouts
- [ ] Messages d'erreur clairs

### Internationalisation
- [ ] Extraire strings vers .ts
- [ ] Traduction française
- [ ] Traduction anglaise

### Documentation
- [ ] Code comments Doxygen
- [ ] User manual
- [ ] API documentation

## Phase 9: Packaging

### Linux
- [ ] AppImage
- [ ] .deb package
- [ ] .rpm package
- [ ] Snap (optionnel)

### Windows
- [ ] Inno Setup installer
- [ ] MSI package
- [ ] Portable version

### macOS
- [ ] DMG image
- [ ] Code signing
- [ ] Notarization

## Phase 10: Déploiement

- [ ] CI/CD avec GitHub Actions
- [ ] Tests automatisés
- [ ] Build multi-plateforme
- [ ] Release automation

## Bugs connus

_Aucun pour le moment (template non exécutable sans Keyple C++)_

## Notes

- Priorité 1: Phases 1-4 (fonctionnalité de base)
- Priorité 2: Phase 5 (UI complète)
- Priorité 3: Phases 6-7 (polish + tests)
- Priorité 4: Phases 8-10 (production-ready)

## Timeline estimée

- Phase 1-2: 1 semaine
- Phase 3: 2 semaines
- Phase 4: 1 semaine
- Phase 5: 1 semaine
- Phase 6-10: 1-2 semaines

**Total: 6-8 semaines pour développeur C++/Qt expérimenté**
