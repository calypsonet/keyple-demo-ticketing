import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKmpLibrary)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.spotless)
}

val jvmToolchainVersion = project.property("jvmToolchainVersion") as String
val javaTargetLevel = project.property("javaTargetLevel") as String

///////////////////////////////////////////////////////////////////////////////
// APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

kotlin {
  jvmToolchain(jvmToolchainVersion.toInt())
  // The platform specific implementations (e.g. Buzzer, DataStore) use expect/actual classes
  compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
  if (System.getProperty("os.name").lowercase().contains("mac")) {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
      iosTarget.binaries.framework {
        baseName = rootProject.name
        isStatic = true
      }
    }
  }
  // Android library used by the Android application (androidApp module): AGP 9 does not support the
  // Kotlin Multiplatform and Android application plugins in the same module
  android {
    namespace = "${project.property("androidAppNamespace")}.shared"
    compileSdk = (project.property("androidCompileSdk") as String).toInt()
    minSdk = (project.property("androidMinSdk") as String).toInt()
    compilerOptions { jvmTarget.set(JvmTarget.fromTarget(javaTargetLevel)) }
    // Packages the Compose Multiplatform resources (composeResources) in the Android library
    androidResources { enable = true }
  }
  jvm("desktop") { kotlin { jvmToolchain(jvmToolchainVersion.toInt()) } }
  sourceSets {
    commonMain.dependencies {
      // Keyple BOM
      implementation(project.dependencies.platform(libs.keypleJavaBom))
      implementation(libs.keypleInteropJsonapiClientKmpLib)
      implementation(libs.keypleInteropLocalreaderNfcmobileKmpLib)

      implementation(libs.kotlinxSerializationCore)
      implementation(libs.ktorSerializationKotlinxJson)
      implementation(project.dependencies.platform(libs.composeBom))
      implementation(libs.composeRuntime)
      implementation(libs.composeFoundation)
      implementation(libs.composeMaterial)
      implementation(libs.composeMaterial3)
      implementation(libs.composeMaterialIconsExtended)
      implementation(libs.composeAnimation)
      implementation(libs.composeUi)
      implementation(libs.composeComponentsResources)
      implementation(libs.composeUiToolingPreview)
      implementation(libs.androidxNavigationCompose)
      implementation(libs.androidxLifecycleViewmodel)
      implementation(libs.androidxDatastorePreferences)

      implementation(libs.compottieRes)
      implementation(libs.compottieLite)
      implementation(libs.ktorClientCore)
      implementation(libs.ktorClientContentNegotiation)
      implementation(libs.ktorClientLogging)
      implementation(libs.ktorClientAuth)
      implementation(libs.ktorClientContentNegotiation)
      implementation(libs.ktorSerializationKotlinxJson)
      implementation(libs.napier)
    }
    if (System.getProperty("os.name").lowercase().contains("mac")) {
      iosMain.dependencies { implementation(libs.ktorClientDarwin) }
    }
    androidMain.dependencies { implementation(libs.ktorClientOkhttp) }
    getByName("desktopMain").dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinxCoroutinesSwing)
      implementation(libs.ktorClientCio)
    }
  }
}

///////////////////////////////////////////////////////////////////////////////
// STANDARD CONFIGURATION FOR KOTLIN MULTIPLATFORM PROJECTS
///////////////////////////////////////////////////////////////////////////////

tasks.withType<JavaExec>().configureEach {
  val customArgs = project.findProperty("customArgs") as String?
  args = customArgs?.split(" ") ?: emptyList()
}

compose.desktop {
  application {
    mainClass = "org.calypsonet.keyple.demo.reload.remote.MainKt"
    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = rootProject.name
      packageVersion = project.version.toString()
    }
  }
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
