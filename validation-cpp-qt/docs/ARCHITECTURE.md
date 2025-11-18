# Architecture C++/Qt - Keyple Validation

## Vue d'ensemble

Application desktop de validation de cartes de transport basée sur Keyple C++ et Qt 6.

### Stack technique

- **Language**: C++17
- **UI Framework**: Qt 6 (Widgets)
- **Card Framework**: Keyple C++
- **Readers**: PC/SC (standard desktop)
- **Logging**: spdlog
- **Build**: CMake

## Architecture en couches

```
┌─────────────────────────────────────────────────────────────┐
│                    UI LAYER (ui/)                           │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Splash → Settings → Reader → CardSummary               │ │
│  │ (QWidget + .ui files)                                  │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│              DOMAIN LAYER (domain/)                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ service/  : TicketingService                           │ │
│  │ rules/    : ValidationRules                            │ │
│  │ model/    : Status, Location, CardReaderResponse, etc. │ │
│  │ mapper/   : ValidationMapper, LocationMapper           │ │
│  │ exception/: ValidationException                        │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│               DATA LAYER (data/repository/)                 │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ReaderRepository        : PC/SC reader management      │ │
│  │ CalypsoCardRepository   : Calypso card validation      │ │
│  │ StorageCardRepository   : MIFARE/ST25 validation       │ │
│  │ LocationRepository      : Location data                │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│         CORE INFRASTRUCTURE (core/)                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ logging/   : Logger (spdlog wrapper)                   │ │
│  │ constants/ : Messages                                  │ │
│  │ di/        : Dependency Injection (optionnel)          │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Flux de navigation

```
main()
  → SplashScreen (2s)
    → SettingsDialog (location, battery mode)
      → ReaderWidget (NFC detection)
        → CardSummaryDialog (results)
          → Back to ReaderWidget
```

## Composants principaux

### 1. TicketingService

**Rôle**: Orchestrateur central pour les opérations de ticketing

**Responsabilités**:
- Initialisation des lecteurs PC/SC
- Gestion de la détection NFC
- Orchestration des scénarios de sélection de carte
- Routage vers le repository approprié (Calypso vs Storage)

**Signaux Qt**:
- `cardDetected(QString)`: Carte détectée
- `validationComplete(CardReaderResponse)`: Validation terminée
- `error(QString)`: Erreur

### 2. ReaderRepository

**Rôle**: Gestion des lecteurs PC/SC

**Responsabilités**:
- Enregistrement du plugin PC/SC Keyple
- Initialisation des lecteurs contactless et SAM
- Activation des protocoles (ISO 14443-4, MIFARE, etc.)
- Feedback audio (succès/échec)

**Différence avec Android**: Utilise PC/SC au lieu des plugins propriétaires (Bluebird, Coppernic, etc.)

### 3. CalypsoCardRepository

**Rôle**: Validation des cartes Calypso

**Workflow**:
1. Ouverture session sécurisée (avec SAM)
2. Lecture environment/event/contracts
3. Validation règles métier (ValidationRules)
4. Traitement par priorité de contrat
5. Décrémentation compteur si nécessaire
6. Écriture nouvel event
7. Fermeture session

### 4. StorageCardRepository

**Rôle**: Validation des cartes de stockage (MIFARE, ST25)

**Workflow**:
1. Lecture blocs environment/event/contract
2. Validation règles métier
3. Traitement selon type contrat
4. Décrémentation compteur
5. Écriture blocs mis à jour
6. Clôture transaction

### 5. ValidationRules

**Rôle**: Logique de validation métier centralisée

**Règles implémentées**:
- Version environment/event/contract
- Dates de validité
- Anti-passback (1 minute minimum)
- Disponibilité compteurs
- Valeur stockée suffisante

**Throw**: `ValidationException` en cas de violation

## Modèles de domaine

### Status
```cpp
enum class Status {
    LOADING,      // En cours
    SUCCESS,      // Validation OK
    INVALID_CARD, // Carte invalide
    EMPTY_CARD,   // Carte vide/expirée
    ERROR         // Erreur générique
};
```

### Location
```cpp
class Location {
    int id;
    QString name;
};
```

### Validation
```cpp
class Validation {
    QString name;
    Location location;
    QString destination;
    QDateTime dateTime;
    std::optional<int> provider;
};
```

### CardReaderResponse
```cpp
class CardReaderResponse {
    Status status;
    QString cardType;
    QString contract;
    std::optional<Validation> validation;
    std::optional<int> nbTicketsLeft;
    std::optional<QDateTime> passValidityEndDate;
    std::optional<QString> errorMessage;
};
```

## Patterns utilisés

### 1. Repository Pattern
Abstraction de l'accès aux données (cartes, lecteurs)

### 2. Singleton
- `AppSettings`: Configuration globale
- `Logger`: Logging centralisé
- `ValidationRules`: Règles métier statiques

### 3. Observer Pattern (Qt Signals/Slots)
- Détection cartes
- Événements validation
- Communication UI ↔ Service

### 4. State Machine
`ReaderActivity` gère les états:
- `WAIT_SYSTEM_READY`
- `WAIT_CARD`
- `CARD_STATUS`

## Threading

### Qt Concurrent
```cpp
QFuture<CardReaderResponse> future = QtConcurrent::run([this]() {
    return m_ticketingService->executeValidationProcedure(locations);
});

auto watcher = new QFutureWatcher<CardReaderResponse>(this);
connect(watcher, &QFutureWatcherBase::finished, [this, watcher]() {
    auto response = watcher->result();
    // Update UI on main thread
});
watcher->setFuture(future);
```

### Règles
- **Main thread**: UI operations (widgets, dialogs)
- **Background thread**: Keyple operations (lecture carte, validation)
- **Signaux Qt**: Cross-thread communication

## Gestion des erreurs

### Hiérarchie
```
std::exception
  └── std::runtime_error
        └── ValidationException (domain)
```

### Stratégie
1. **ValidationRules** throw `ValidationException` → Status spécifique
2. **Repositories** throw `std::runtime_error` → Status::ERROR
3. **UI** catch & affiche via `CardSummaryDialog`

## Configuration build

### CMake Structure
```
CMakeLists.txt (root)
  └── src/CMakeLists.txt
       ├── Collect sources (.cpp)
       ├── Collect UI files (.ui)
       ├── Collect resources (.qrc)
       └── Link libraries (Qt, Keyple, spdlog, PC/SC)
```

### Options
- `USE_FRUIT_DI`: Activer Fruit dependency injection
- `BUILD_TESTING`: Compiler les tests unitaires

## Prochaines étapes

### Phase 1: Intégration Keyple C++
- [ ] Inclure headers Keyple
- [ ] Implémenter ReaderRepository avec PC/SC
- [ ] Tester détection lecteur

### Phase 2: Validation Calypso
- [ ] Implémenter CalypsoCardRepository
- [ ] Gestion session SAM
- [ ] Tests avec carte réelle

### Phase 3: Validation Storage
- [ ] Implémenter StorageCardRepository
- [ ] Support MIFARE Ultralight
- [ ] Support ST25 SRT512

### Phase 4: UI Polish
- [ ] Animations (GIF ou QtLottie)
- [ ] Feedback audio (Qt Multimedia)
- [ ] Traductions (i18n)

## Ressources

- **Keyple C++**: https://keyple.org/
- **Qt Documentation**: https://doc.qt.io/qt-6/
- **PC/SC**: https://pcsclite.apdu.fr/
- **spdlog**: https://github.com/gabime/spdlog
