import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
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
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.spotless)
}

val jvmToolchainVersion: String by project
val javaSourceLevel: String by project
val javaTargetLevel: String by project

///////////////////////////////////////////////////////////////////////////////
// APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

kotlin {
  jvmToolchain(jvmToolchainVersion.toInt())
  if (System.getProperty("os.name").lowercase().contains("mac")) {
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
      iosTarget.binaries.framework {
        baseName = rootProject.name
        isStatic = true
      }
    }
  }
  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions { jvmTarget.set(JvmTarget.fromTarget(javaTargetLevel)) }
  }
  jvm("desktop") { kotlin { jvmToolchain(jvmToolchainVersion.toInt()) } }
  sourceSets {
    val desktopMain by getting
    commonMain.dependencies {
      implementation(libs.keypleInteropJsonapiClientKmpLib)
      implementation(libs.keypleInteropLocalreaderNfcmobileKmpLib)
      implementation(libs.kotlinxSerializationCore)
      implementation(libs.ktorSerializationKotlinxJson)
      implementation(project.dependencies.platform(libs.composeBom))
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material)
      implementation(compose.material3)
      implementation(compose.animation)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)
      implementation(libs.koinCore)
      implementation(libs.koinCompose)
      implementation(libs.androidxNavigationCompose)
      implementation(libs.androidxDatastorePreferences)
      implementation(libs.compottie)
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
    androidMain.dependencies {
      implementation(libs.androidxActivityCompose)
      implementation(libs.koinAndroid)
      implementation(libs.koinAndroidxCompose)
      implementation(libs.ktorClientOkhttp)
    }
    desktopMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinxCoroutinesSwing)
      implementation(libs.ktorClientCio)
    }
  }
}

///////////////////////////////////////////////////////////////////////////////
// STANDARD CONFIGURATION FOR KOTLIN MULTIPLATFORM APP-TYPE PROJECTS
///////////////////////////////////////////////////////////////////////////////

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
    compose = true
  }
  buildTypes { getByName("release") { isMinifyEnabled = false } }
  dependencies { debugImplementation(compose.uiTooling) }
  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
  }
  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  sourceSets["main"].res.srcDirs("src/androidMain/res")
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
  applicationVariants.all {
    outputs.all {
      val outputImpl = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
      val variantName = name
      val versionName = project.version.toString()
      val newName = "${rootProject.name}-android-$versionName-$variantName.apk"
      outputImpl.outputFileName = newName
    }
  }
  publishing { singleVariant("debug") {} }
  lint { abortOnError = false }
}

tasks.withType<JavaExec>().configureEach {
  val customArgs: String? by project
  args = customArgs?.split(" ") ?: emptyList()
}

compose.desktop {
  application {
    mainClass = "org.calypsonet.keyple.demo.reload.remote.MainKt"
    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = rootProject.name
      packageVersion = project.version.toString().drop(2)
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
