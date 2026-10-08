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
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.spotless)
}

val javaSourceLevel = project.property("javaSourceLevel") as String
val javaTargetLevel = project.property("javaTargetLevel") as String

///////////////////////////////////////////////////////////////////////////////
// APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

dependencies {
  // Shared code of the application (Kotlin Multiplatform library)
  implementation(project(":composeApp"))

  // Libraries used by the activity to build the Keyple service
  implementation(platform(libs.keypleJavaBom))
  implementation(libs.keypleInteropJsonapiClientKmpLib)
  implementation(libs.keypleInteropLocalreaderNfcmobileKmpLib)
  implementation(libs.androidxDatastorePreferences)

  // Android and Compose
  implementation(libs.androidxActivityCompose)
  implementation(libs.composeRuntime)
  debugImplementation(libs.composeUiTooling)

  // Logging
  implementation(libs.napier)
}

///////////////////////////////////////////////////////////////////////////////
// STANDARD CONFIGURATION FOR ANDROID KOTLIN-BASED APP-TYPE PROJECTS
///////////////////////////////////////////////////////////////////////////////

android {
  namespace = project.property("androidAppNamespace") as String
  compileSdk = (project.property("androidCompileSdk") as String).toInt()
  defaultConfig {
    applicationId = project.property("androidAppId") as String
    minSdk = (project.property("androidMinSdk") as String).toInt()
    targetSdk = (project.property("androidTargetSdk") as String).toInt()
    versionCode = (project.property("androidAppVersionCode") as String).toInt()
    versionName = project.property("androidAppVersionName") as String
  }
  buildFeatures { compose = true }
  buildTypes { getByName("release") { isMinifyEnabled = false } }
  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
  }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
  lint { abortOnError = false }
}

// Name of the APK files: <root project name>-android-<version>-<variant>.apk
base { archivesName.set("${rootProject.name}-android-${project.version}") }

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
