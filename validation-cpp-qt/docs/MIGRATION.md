# Guide de Migration Android/Kotlin → C++/Qt

Guide pratique pour porter l'application Keyple Validation depuis Android vers Desktop.

## Tableau de correspondance

### Frameworks & Bibliothèques

| Android/Kotlin | C++/Qt | Notes |
|----------------|---------|-------|
| Activity | QWidget / QDialog | Pas de lifecycle complexe |
| ViewBinding | Qt Designer (.ui) | Génération auto avec `uic` |
| Dagger 2 | Fruit (optionnel) | Ou DI manuel via constructeurs |
| Coroutines | QtConcurrent / QThread | `QtConcurrent::run()` pour tâches simples |
| Timber | spdlog | Logging haute performance |
| Lottie | QMovie (GIF) | Ou QtLottie (limité) |
| Keyple Java | Keyple C++ | API similaire |
| Plugins Android | PC/SC | Standard desktop |
| Parcelable | QVariant | Ou structs normaux |

### Components Android → Qt

| Android | Qt Equivalent |
|---------|---------------|
| `onCreate()` | Constructeur |
| `onResume()` | `showEvent()` |
| `onPause()` | `hideEvent()` |
| `onDestroy()` | Destructeur `~Widget()` |
| `startActivity()` | `dialog->show()` ou `widget->show()` |
| `Intent + Bundle` | Paramètres constructeur |
| `LiveData` | Qt Signals |
| `ViewModel` | Service avec signaux |
| `findViewById()` | `ui->widgetName` (auto-généré) |
| `Toast` | `QMessageBox::information()` |

## Migration par couche

### 1. Modèles (data class → class/struct)

**Android (Kotlin)**:
```kotlin
data class Location(
    val id: Int,
    val name: String
) : Parcelable
```

**C++/Qt**:
```cpp
class Location {
public:
    Location(int id, const QString& name)
        : m_id(id), m_name(name) {}

    int id() const { return m_id; }
    QString name() const { return m_name; }

private:
    int m_id;
    QString m_name;
};

Q_DECLARE_METATYPE(Location)  // Pour signaux/slots
```

### 2. Repositories

**Android (Kotlin)**:
```kotlin
class ReaderRepository @Inject constructor() {
    fun registerPlugin(activity: Activity, readerType: ReaderType) {
        when (readerType) {
            ReaderType.BLUEBIRD -> initBluebirdReader()
            ReaderType.COPPERNIC -> initCoppernicReader()
            // ...
        }
    }
}
```

**C++/Qt**:
```cpp
class ReaderRepository {
public:
    void registerPlugin() {
        auto& service = SmartCardServiceProvider::getService();

        // Un seul type: PC/SC
        auto pluginFactory = PcscPluginFactoryBuilder::builder()->build();
        m_pcscPlugin = service.registerPlugin(pluginFactory);
    }
};
```

### 3. Services

**Android (Kotlin)**:
```kotlin
@AppScoped
class TicketingService @Inject constructor(
    private var readerRepository: ReaderRepository
) {
    fun init(observer: CardReaderObserverSpi?, activity: Activity, readerType: ReaderType) {
        readerRepository.registerPlugin(activity, readerType)
        // ...
    }
}
```

**C++/Qt**:
```cpp
class TicketingService : public QObject {
    Q_OBJECT
public:
    explicit TicketingService(ReaderRepository* readerRepo, QObject* parent = nullptr)
        : QObject(parent), m_readerRepo(readerRepo) {}

    void init() {
        m_readerRepo->registerPlugin();
        // ...
    }

signals:
    void cardDetected(const QString& cardType);
    void validationComplete(const CardReaderResponse& response);

private:
    ReaderRepository* m_readerRepo;
};
```

### 4. Activities → Widgets

**Android (Kotlin)**:
```kotlin
class SettingsActivity : BaseActivity() {
    @Inject lateinit var locationRepository: LocationRepository

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startButton.setOnClickListener {
            startActivity(Intent(this, ReaderActivity::class.java))
        }
    }
}
```

**C++/Qt**:
```cpp
class SettingsDialog : public QDialog {
    Q_OBJECT
public:
    explicit SettingsDialog(QWidget *parent = nullptr)
        : QDialog(parent), ui(new Ui::SettingsDialog) {
        ui->setupUi(this);

        connect(ui->startButton, &QPushButton::clicked, [this]() {
            auto reader = new ReaderWidget();
            reader->show();
            accept();  // Close settings dialog
        });
    }

private:
    Ui::SettingsDialog *ui;
    LocationRepository m_locationRepository;
};
```

### 5. Coroutines → QtConcurrent

**Android (Kotlin)**:
```kotlin
GlobalScope.launch {
    withContext(Dispatchers.Main) { showProgress() }
    val result = withContext(Dispatchers.IO) {
        ticketingService.executeValidationProcedure(locations)
    }
    withContext(Dispatchers.Main) {
        dismissProgress()
        showResult(result)
    }
}
```

**C++/Qt**:
```cpp
m_progressDialog->show();

QFuture<CardReaderResponse> future = QtConcurrent::run([this]() {
    return m_ticketingService->executeValidationProcedure(m_locations);
});

auto watcher = new QFutureWatcher<CardReaderResponse>(this);
connect(watcher, &QFutureWatcherBase::finished, [this, watcher]() {
    m_progressDialog->hide();
    auto result = watcher->result();
    showResult(result);
    watcher->deleteLater();
});
watcher->setFuture(future);
```

### 6. Dependency Injection

**Android (Dagger 2)**:
```kotlin
@AppScoped
@Component(modules = [AppModule::class, UIModule::class])
interface AppComponent {
    fun inject(activity: MainActivity)
}

// Usage
class MyActivity : DaggerAppCompatActivity() {
    @Inject lateinit var ticketingService: TicketingService
}
```

**C++/Qt (Fruit)**:
```cpp
// Module definition
fruit::Component<TicketingService> getServiceModule() {
    return fruit::createComponent()
        .bind<ReaderRepository, ReaderRepositoryImpl>();
}

// Container
class Container {
public:
    static Container* instance();

    TicketingService* getTicketingService() {
        return m_injector.get<TicketingService*>();
    }

private:
    fruit::Injector<TicketingService> m_injector;
};

// Usage
auto service = Container::instance()->getTicketingService();
```

**C++/Qt (Manuel)**:
```cpp
// main.cpp
int main(int argc, char *argv[]) {
    QApplication app(argc, argv);

    // Create dependencies manually
    auto readerRepo = new ReaderRepository();
    auto ticketingService = new TicketingService(readerRepo);

    MainWindow window(ticketingService);
    window.show();

    return app.exec();
}
```

## Patterns de migration

### Pattern 1: Object Kotlin → Static Class C++

**Android (Kotlin)**:
```kotlin
object Messages {
    const val ERROR_NO_VALID_TITLE_DETECTED = "No valid title detected"
}
```

**C++**:
```cpp
class Messages {
public:
    static const QString ERROR_NO_VALID_TITLE_DETECTED;
private:
    Messages() = delete;  // No instantiation
};

// .cpp
const QString Messages::ERROR_NO_VALID_TITLE_DETECTED = "No valid title detected";
```

### Pattern 2: Sealed Class → enum class

**Android (Kotlin)**:
```kotlin
sealed class Status {
    object Loading : Status()
    object Success : Status()
    data class Error(val message: String) : Status()
}
```

**C++**:
```cpp
enum class Status {
    LOADING,
    SUCCESS,
    ERROR
};

// Si besoin de données associées, utiliser std::variant
using StatusData = std::variant<
    std::monostate,           // LOADING
    std::monostate,           // SUCCESS
    std::string               // ERROR with message
>;
```

### Pattern 3: Extension Functions → Static Methods

**Android (Kotlin)**:
```kotlin
fun String.toHexByteArray(): ByteArray {
    return HexUtil.toByteArray(this)
}
```

**C++**:
```cpp
class StringUtils {
public:
    static QByteArray toHexByteArray(const QString& str) {
        return QByteArray::fromHex(str.toLatin1());
    }
};
```

## Pièges courants

### 1. Gestion mémoire

**Android**: GC automatique
**C++**: Manuel (ou smart pointers)

```cpp
// ❌ Mauvais (memory leak)
CardReaderResponse* response = new CardReaderResponse(...);
// Jamais delete

// ✅ Bon (smart pointer)
auto response = std::make_unique<CardReaderResponse>(...);

// ✅ Bon (Qt parent ownership)
auto dialog = new CardSummaryDialog(this);  // 'this' est le parent
dialog->show();  // Qt delete automatiquement
```

### 2. Null Safety

**Android (Kotlin)**:
```kotlin
val name: String? = null
val length = name?.length ?: 0
```

**C++**:
```cpp
std::optional<QString> name = std::nullopt;
int length = name.has_value() ? name.value().length() : 0;

// Ou
int length = name.value_or("").length();
```

### 3. Collections

| Kotlin | C++ |
|--------|-----|
| `List<T>` | `std::vector<T>` ou `QList<T>` |
| `Map<K,V>` | `std::map<K,V>` ou `QMap<K,V>` |
| `Set<T>` | `std::set<T>` ou `QSet<T>` |
| `listOf(...)` | `{...}` initializer list |
| `list.map { }` | `std::transform` + lambda |
| `list.filter { }` | `std::copy_if` + lambda |

### 4. Strings

| Kotlin | C++ |
|--------|-----|
| `String` | `QString` (Qt) ou `std::string` |
| `"text $variable"` | `QString("text %1").arg(variable)` |
| `str.contains("x")` | `str.contains("x")` (QString) |
| `str.toInt()` | `str.toInt()` (QString) |

## Checklist de migration

### Par composant

- [ ] **Modèles**: Convertir data classes en classes C++
- [ ] **Enums**: Convertir en `enum class`
- [ ] **Repositories**: Adapter pour PC/SC
- [ ] **Services**: Ajouter Q_OBJECT et signaux
- [ ] **Activities**: Convertir en QWidget/QDialog
- [ ] **Layouts XML**: Recréer avec Qt Designer (.ui)
- [ ] **Resources**: Copier dans .qrc
- [ ] **Strings**: Extraire dans constants ou i18n

### Tests

- [ ] Compilation sans erreur
- [ ] Linking correct (toutes bibliothèques trouvées)
- [ ] Détection lecteur PC/SC
- [ ] Lecture carte Calypso
- [ ] Lecture carte Storage
- [ ] Navigation UI complète
- [ ] Validation métier

## Timeline estimée

| Phase | Durée | Détails |
|-------|-------|---------|
| **Setup** | 1-2 jours | CMake, Qt, Keyple C++, dépendances |
| **Modèles** | 1 jour | 8 classes simples |
| **Repositories** | 1 semaine | PC/SC + Keyple intégration |
| **Services** | 3 jours | TicketingService + ValidationRules |
| **UI** | 1 semaine | 5 écrans Qt |
| **Tests** | 3 jours | Tests avec hardware réel |
| **Polish** | 2 jours | Animations, audio, i18n |
| **TOTAL** | **3-4 semaines** | Pour développeur C++/Qt expérimenté |

## Ressources utiles

- **Qt 6 Migration**: https://doc.qt.io/qt-6/portingguide.html
- **Keyple C++ Docs**: https://keyple.org/
- **Kotlin → C++ Cheatsheet**: https://github.com/Kotlin/kotlin-by-example
- **PC/SC API**: https://ludovicrousseau.blogspot.com/p/pcsc-api.html

## Support

Pour questions spécifiques:
1. **Keyple**: https://github.com/eclipse-keyple/keyple-cpp/issues
2. **Qt**: https://forum.qt.io/
3. **PC/SC**: https://muscle.apdu.fr/
