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

val parkeonSdkFile = file("../../../libs/AndroidParkeonCommon-release.aar")
val hasArriveSdk = parkeonSdkFile.exists()

if (hasArriveSdk) {
  println("Parkeon SDK found - Arrive UI features enabled")
} else {
  println("Parkeon SDK not found - Arrive UI features disabled (stub mode)")
}

dependencies {
  // Demo common
  implementation(project(":common"))

  // Proprietary libs
  // Storage card specific components
  // Conditional dependency for the storage card library
  val storageCardLibName = "keyple-card-cna-storagecard-java-lib-2.2.0"
  val storageCardLibFile = file("../../../libs/${storageCardLibName}.jar")
  if (storageCardLibFile.exists()) {
    println("Using private storage card library: ${storageCardLibFile.name}")
    implementation(files(storageCardLibFile))
  } else {
    println("Using mock storage card library")
    implementation(files("../../../libs/${storageCardLibName}-mock.jar"))
  }

  // Conditional dependency for the storage card plugin library
  val pluginStorageCardLibName = "keyple-plugin-cna-storagecard-java-lib-1.1.0"
  val pluginStorageCardLibFile = file("../../../libs/${pluginStorageCardLibName}.jar")
  if (pluginStorageCardLibFile.exists()) {
    println("Using private storage card plugin library: ${pluginStorageCardLibFile.name}")
    implementation(files(pluginStorageCardLibFile))
  } else {
    println("Using mock storage card plugin library")
    implementation(files("../../../libs/${pluginStorageCardLibName}-mock.jar"))
  }

  // Bluebird specific components
  val bluebirdPluginLibName = "keyple-plugin-cna-bluebird-specific-nfc-java-lib-3.2.0-debug"
  val bluebirdPluginLibFile = file("../../../libs/${bluebirdPluginLibName}.aar")
  if (bluebirdPluginLibFile.exists()) {
    println("Using Bluebird plugin library: ${bluebirdPluginLibFile.name}")
    implementation(files(bluebirdPluginLibFile))
  } else {
    println("Using mock Bluebird plugin library")
    implementation(files("../../../libs/${bluebirdPluginLibName}-mock.aar"))
  }

  // Arrive specific components
  val arrivePluginLibName = "keyple-plugin-cna-arrive-android-jvm-lib-3.0.0-debug"
  val arrivePluginLibFile = file("../../../libs/${arrivePluginLibName}.aar")
  if (arrivePluginLibFile.exists()) {
    println("Using Arrive plugin library: ${arrivePluginLibFile.name}")
    implementation(files(arrivePluginLibFile))
  } else {
    println("Using mock Arrive plugin library")
    implementation(files("../../../libs/${arrivePluginLibName}-mock.aar"))
  }

  // Arrive/Parkeon SDK (UI: LEDs, sounds) — optional, enables ArriveUiManager real implementation
  if (hasArriveSdk) {
    implementation(files(parkeonSdkFile))
  }

  // Keyple BOM
  implementation(platform(libs.keypleJavaBom))

  // Keypop (API)
  implementation(libs.keypopReaderApi)
  implementation(libs.keypopCalypsoCardApi)
  implementation(libs.keypopCalypsoCryptoLegacysamApi)
  // implementation(libs.keypopStoragecardApi)
  // TEMPORARY SNAPSHOT:
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
  // implementation(libs.keyplePluginAndroidNfcLib)
  // TEMPORARY SNAPSHOT:
  implementation("org.eclipse.keyple:keyple-plugin-android-nfc-java-lib:3.2.0-SNAPSHOT") {
    isChanging = true
  }

  // Other Keyple plugins
  implementation(libs.keyplePluginCnaCoppernicCone2Lib)
  implementation(libs.keyplePluginCnaFamocoSeCommunicationLib)

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

  // Lottie
  implementation(libs.lottie)

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
    buildConfigField("Boolean", "HAS_ARRIVE_SDK", "$hasArriveSdk")
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
    // ArriveUiManager: real impl (Parkeon SDK) or no-op stub, mutually exclusive source sets
    getByName("main").java.srcDir(if (hasArriveSdk) "src/arrive/kotlin" else "src/no-arrive/kotlin")
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
      licenseHeaderFile("../../../LICENSE_HEADER")
      ktfmt()
    }
    kotlinGradle {
      target("**/*.kts")
      ktfmt()
    }
  }
}
