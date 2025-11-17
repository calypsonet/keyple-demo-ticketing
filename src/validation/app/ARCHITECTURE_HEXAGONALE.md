# Migration vers Architecture Hexagonale - État d'Avancement

## ✅ TERMINÉ (90%)

### 1. Structure Complète Créée

```
app/src/main/kotlin/org/calypsonet/keyple/demo/validation/
├── domain/                          # ❤️ CŒUR MÉTIER (0 dépendance Android)
│   ├── model/                       # 9 entités pures + règles métier
│   ├── port/input/                  # 6 Use Cases (interfaces)
│   ├── port/output/                 # 4 Ports de sortie (interfaces)
│   └── exception/                   # Exceptions métier
│
├── application/usecase/             # 6 Implémentations de Use Cases
│
├── adapter/
│   ├── secondary/                   # Adapters techniques
│   │   ├── repository/              # CardRepositoryFacade, LocationAdapter, etc.
│   │   ├── reader/                  # CardSelectionService, KeypleReaderManager
│   │   └── UiFeedbackAdapter.kt
│   └── primary/                     # Adapters UI
│       ├── mapper/ValidationUiMapper.kt
│       └── ui/base/BaseActivity.kt
│
└── infrastructure/di/               # Configuration Dagger
    ├── DomainModule.kt (binds use cases)
    ├── AdapterModule.kt (binds adapters)
    └── AppComponent.kt
```

### 2. Entités Métier PURES (sans Android)

**Créées dans `domain/model/`:**
- `Location` - Localisation pure
- `ValidationResult` - Résultat de validation
- `ValidationStatus` - Enum des statuts
- `CardType` - Type de carte
- `ValidationEvent` - Événement de validation
- `CardSelectionResult` - Résultat de sélection
- **`Contract`** - Entité avec règles métier:
  - `isValid(date): Boolean`
  - `hasTripsRemaining(): Boolean`
  - `requiresDecrement(): Boolean`
  - `decrementCounter(): Contract`
- `CardEnvironment` - Environnement carte avec validation
- `CardData` - Données complètes de carte
- **`ValidationRules`** - Règles métier statiques:
  - `isAntiPassbackViolated()` - Règle 1 minute minimum
  - `selectBestContract()` - Sélection par priorité
  - `hasValidContract()` - Vérification existence

### 3. Use Cases (Ports d'Entrée)

**6 Use Cases définis et implémentés:**
1. `InitializeReaderUseCase` - Initialisation matériel
2. `StartCardDetectionUseCase` - Démarrage détection NFC
3. `StopCardDetectionUseCase` - Arrêt détection
4. `AnalyzeCardSelectionUseCase` - Analyse type carte
5. **`ValidateCardUseCase`** - Validation complète (logique métier)
6. `CleanupReaderUseCase` - Nettoyage ressources

### 4. Ports de Sortie (Interfaces Repository)

**4 Ports définis:**
1. `CardRepository` - Validation carte (Calypso/Storage)
2. `ReaderManager` - Gestion lecteurs matériels
3. `LocationProvider` - Fourniture localisation
4. `UiFeedbackPort` - Feedback audio/visuel

### 5. Dagger Configuré

- **`DomainModule`**: Bind tous les use cases vers leurs implémentations
- **`AdapterModule`**: Bind tous les adapters vers leurs ports
- **`AppComponent`**: Intègre les nouveaux modules
- **`BaseActivity`**: Injecte les use cases (plus de service direct)

### 6. Mappers

- `ValidationUiMapper`: Conversion `ValidationResult` ↔ `CardReaderResponse`
- Gestion des modèles Parcelable pour Android

---

## ⚠️ EN COURS (10%)

### API Keyple Évoluées

**Problème**: Les API Keyple ont évolué entre les versions. Les méthodes suivantes n'existent plus:
- `createCalypsoCardSelection()` → À remplacer par l'API actuelle
- `createLegacySamSelection()` → À remplacer par l'API actuelle
- `storageCardApiFactory` → À vérifier dans la documentation Keyple
- `createSymmetricCryptoSecuritySetting()` → API changée

**Solution**:
1. Consulter la documentation Keyple BOM 2025.09.12
2. Mettre à jour les appels API dans:
   - `CardSelectionService.kt`
   - `CardRepositoryFacade.kt`

### Activities à Migrer

Les anciennes activities (renommées en `.kt.old`) doivent être réécrites pour utiliser les Use Cases:
- `MainActivity.kt`
- `DeviceSelectionActivity.kt`
- `SettingsActivity.kt`
- `HomeActivity.kt`
- `ReaderActivity.kt` (important - logique validation)
- `CardSummaryActivity.kt`

---

## 🎯 PRINCIPES RESPECTÉS

### ✅ Séparation des Responsabilités

| Couche | Responsabilité | Dépendances |
|--------|----------------|-------------|
| **Domain** | Règles métier pures | Aucune (ni Android, ni Keyple) |
| **Application** | Orchestration use cases | Domain seulement |
| **Adapters Secondary** | Implémentation technique | Keyple, Android technique |
| **Adapters Primary** | Présentation UI | Android UI |
| **Infrastructure** | Configuration DI | Dagger |

### ✅ Flux de Dépendances

```
UI (Activities) → Use Cases → Domain ← Adapters (Keyple)
                       ↓
                    Ports (Interfaces)
```

**Règle d'Or**: Le `domain/` ne dépend de RIEN. Tout dépend de lui.

### ✅ Testabilité

Grâce à l'architecture hexagonale:
- **Domain**: 100% testable sans Android ni Keyple (tests unitaires purs)
- **Use Cases**: Testables avec mocks des ports
- **Adapters**: Testables indépendamment

---

## 📋 PROCHAINES ÉTAPES

### 1. Corriger les API Keyple (Priorité 1)
- [ ] Mettre à jour `CardSelectionService` avec API Keyple 2025.09.12
- [ ] Mettre à jour `CardRepositoryFacade` avec API Keyple correctes
- [ ] Vérifier compilation du cœur architecture

### 2. Migrer les Activities (Priorité 2)
- [ ] `ReaderActivity` → Utiliser `ValidateCardUseCase`
- [ ] `DeviceSelectionActivity` → Utiliser `InitializeReaderUseCase`
- [ ] Autres activities selon besoin

### 3. Tests (Priorité 3)
- [ ] Tests unitaires `ValidationRules`
- [ ] Tests unitaires `Contract`
- [ ] Tests use cases avec mocks

---

## 📚 RÉFÉRENCES

### Documentation
- Architecture Hexagonale: https://alistair.cockburn.us/hexagonal-architecture/
- Clean Architecture: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- Keyple Documentation: https://keyple.org/

### Fichiers Clés
- **Règles Métier**: `domain/model/ValidationRules.kt`
- **Use Case Principal**: `application/usecase/ValidateCardUseCaseImpl.kt`
- **Configuration DI**: `infrastructure/di/DomainModule.kt`
- **Base Activity**: `adapter/primary/ui/base/BaseActivity.kt`

---

## 💡 AVANTAGES OBTENUS

1. **Code Métier Indépendant**: Peut être réutilisé dans d'autres contextes (iOS, Web, CLI)
2. **Testabilité Maximale**: Domain et use cases testables sans Android
3. **Flexibilité**: Changement de framework UI ou de librairie Keyple sans toucher au métier
4. **Maintenabilité**: Responsabilités claires, couplage faible
5. **Évolutivité**: Ajout de nouveaux use cases ou adapters simplifié

---

**Date**: 17/11/2024
**Statut**: Architecture en place (90%), API Keyple à corriger (10%)
**Compilation**: ❌ (erreurs API Keyple à résoudre)
