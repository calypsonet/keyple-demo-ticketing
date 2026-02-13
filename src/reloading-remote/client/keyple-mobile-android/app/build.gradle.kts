import org.jetbrains.kotlin.gradle.dsl.JvmTarget

///////////////////////////////////////////////////////////////////////////////
// GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

if (project.hasProperty("releaseTag")) {
  project.version = project.property("releaseTag") as String
  println("Release mode: version set to ${project.version}")
} else {
  project.version = libs.versions.project.get()
  println("Development mode: version is ${project.version}")
}

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.kotlinParcelize)
  alias(libs.plugins.kotlinKapt)
  alias(libs.plugins.spotless)
}

///////////////////////////////////////////////////////////////////////////////
// APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

// Logging configuration: using SLF4J + Timber bridge for Android

dependencies {
  // Demo common
  implementation(project(":common"))

  // Proprietary libs
  // Storage card specific components
  // Conditional dependency for the storage card library
  val storageCardLibName = "keyple-card-cna-storagecard-java-lib-2.2.0"
  val storageCardLibFile = file("../../../../../libs/${storageCardLibName}.jar")
  if (storageCardLibFile.exists()) {
    println("Using private storage card library: ${storageCardLibFile.name}")
    implementation(files(storageCardLibFile))
  } else {
    println("Using mock storage card library")
    implementation(files("../../../../../libs/${storageCardLibName}-mock.jar"))
  }

  // Conditional dependency for the storage card plugin library
  val pluginStorageCardLibName = "keyple-plugin-cna-storagecard-java-lib-1.1.0"
  val pluginStorageCardLibFile = file("../../../../../libs/${pluginStorageCardLibName}.jar")
  if (pluginStorageCardLibFile.exists()) {
    println("Using private storage card plugin library: ${pluginStorageCardLibFile.name}")
    implementation(files(pluginStorageCardLibFile))
  } else {
    println("Using mock storage card plugin library")
    implementation(files("../../../../../libs/${pluginStorageCardLibName}-mock.jar"))
  }

  // Bluebird specific components
  val bluebirdPluginLibName = "keyple-plugin-cna-bluebird-specific-nfc-java-lib-3.2.0"
  val bluebirdPluginLibFile = file("../../../../../libs/${bluebirdPluginLibName}.aar")
  if (bluebirdPluginLibFile.exists()) {
    println("Using release Bluebird plugin library: ${bluebirdPluginLibFile.name}")
    implementation(files(bluebirdPluginLibFile))
  } else {
    println("Using debug Bluebird plugin library")
    implementation(files("../../../../../libs/${bluebirdPluginLibName}-debug.aar"))
  }

  // Keyple BOM
  implementation(platform(libs.keypleJavaBom))

  // Keypop (API)
  implementation(libs.keypopReaderApi)
  implementation(libs.keypopCalypsoCardApi)
  implementation(libs.keypopCalypsoCryptoLegacysamApi)
  // TEMPORARY SNAPSHOT: Required for Mifare Classic support (MifareClassicKeyType,
  // hasAuthentication())
  // TODO: Remove when version 1.1.0 (or later) is officially released and added to BOM
  // Original: implementation(libs.keypopStoragecardApi)
  implementation("org.eclipse.keypop:keypop-storagecard-java-api:1.1.0-SNAPSHOT") {
    isChanging = true
  }

  // Keyple
  implementation(libs.keypleCommonApi)
  implementation(libs.keyplePluginStoragecardApi)
  implementation(libs.keypleUtilJavaLib)
  implementation(libs.keypleServiceLib)
  implementation(libs.keypleCardCalypsoLib)
  implementation(libs.keypleCardCalypsoCryptoLegacysamLib)
  // TEMPORARY SNAPSHOT: Required for KeyProvider SPI support in Android NFC plugin
  // TODO: Remove when version 3.2.0 (or later) is officially released and added to BOM
  // Original: implementation(libs.keyplePluginAndroidNfcLib)
  implementation("org.eclipse.keyple:keyple-plugin-android-nfc-java-lib:3.2.0-SNAPSHOT") {
    isChanging = true
  }
  implementation(libs.keyplePluginAndroidOmapiLib)
  implementation(libs.keypleDistributedNetworkLib)
  implementation(libs.keypleDistributedLocalLib)

  // Android components
  implementation(libs.androidxAppcompat)
  implementation(libs.material)
  implementation(libs.androidxConstraintLayout)
  implementation(libs.androidxActivity)
  implementation(libs.androidxFragment)
  implementation(libs.androidxMultidex)

  // Kotlin
  implementation(libs.androidxCore)
  implementation(libs.kotlinStdlibJdk8)

  // Coroutines
  implementation(libs.kotlinxCoroutinesCore)
  implementation(libs.kotlinxCoroutinesAndroid)

  // Dagger
  implementation(libs.dagger)
  implementation(libs.daggerAndroid)
  implementation(libs.daggerAndroidSupport)
  kapt(libs.daggerCompiler)
  kapt(libs.daggerAndroidProcessor)
  annotationProcessor(libs.daggerCompiler)
  annotationProcessor(libs.daggerAndroidProcessor)
  compileOnly(libs.glassfishAnnotations)

  // RxJava
  implementation(libs.rxjava)
  implementation(libs.rxandroid)

  // Retrofit
  implementation(libs.retrofit)
  implementation(libs.retrofitConverterGson)
  implementation(libs.retrofitConverterScalars)
  implementation(libs.retrofitAdapterRxjava2)
  implementation(libs.okhttpLoggingInterceptor)

  // Server status
  implementation(libs.eventbus)

  // Lottie
  implementation(libs.lottie)

  // Google GSON
  implementation(libs.gson)

  // Devnied - Byte Utils
  implementation(libs.bitLib4j) { exclude(group = "org.slf4j") }

  // Logging libraries used in the project:
  // - SLF4J API provides a common logging interface for the app and third-party libraries (e.g.,
  //   Keyple).
  // - slf4j-timber bridges SLF4J calls to Timber for Android logging.
  // - Timber is used as the primary Android logging framework, offering lightweight and flexible
  //   logging.
  implementation(libs.slf4jApi)
  implementation(libs.slf4jTimber)
  implementation(libs.timber)
}

///////////////////////////////////////////////////////////////////////////////
// STANDARD CONFIGURATION FOR ANDROID KOTLIN-BASED APP-TYPE PROJECTS
///////////////////////////////////////////////////////////////////////////////

val javaSourceLevel: String by project
val javaTargetLevel: String by project

android {
  namespace = project.findProperty("androidAppNamespace") as String
  compileSdk = (project.findProperty("androidCompileSdk") as String).toInt()
  defaultConfig {
    applicationId = project.findProperty("androidAppId") as String
    minSdk = (project.findProperty("androidMinSdk") as String).toInt()
    targetSdk = (project.findProperty("androidCompileSdk") as String).toInt()
    versionCode = (project.findProperty("androidAppVersionCode") as String).toInt()
    versionName = project.findProperty("androidAppVersionName") as String
  }
  buildFeatures {
    viewBinding = true
    buildConfig = true
  }
  buildTypes {
    // Configuration for the debug build variant:
    // - Minification, resource shrinking, and ProGuard rules are enabled here as an example
    //   to test release-like performance and optimizations during development.
    // - To see full, unoptimized logs during debug, this block can be commented out or adjusted.
    getByName("debug") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
  }
  kotlin { compilerOptions { jvmTarget.set(JvmTarget.fromTarget(javaTargetLevel)) } }
  sourceSets {
    getByName("main").java.srcDirs("src/main/kotlin")
    getByName("debug").java.srcDirs("src/debug/kotlin")
  }
  packagingOptions {
    // Exclude 'META-INF/NOTICE.md' to resolve the conflict that occurs when multiple dependencies
    // include this file
    resources.excludes.add("META-INF/NOTICE.md")
  }
  applicationVariants.all {
    outputs.all {
      val outputImpl = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
      val variantName = name
      val versionName = project.version.toString()
      val newName = "${rootProject.name}-$versionName-$variantName.apk"
      outputImpl.outputFileName = newName
    }
  }
  lint { abortOnError = false }
}

tasks.withType<AbstractArchiveTask>().configureEach { archiveBaseName.set(rootProject.name) }

tasks {
  spotless {
    kotlin {
      target("src/**/*.kt")
      licenseHeaderFile("../../../../../LICENSE_HEADER")
      ktfmt()
    }
    kotlinGradle {
      target("**/*.kts")
      ktfmt()
    }
  }
}
