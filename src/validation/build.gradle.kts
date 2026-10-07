///////////////////////////////////////////////////////////////////////////////
// GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

plugins {
  // Android and Kotlin plugins loaded once for all the projects (the application and the common
  // module), the Kotlin Android plugin needing the classes of the Android plugin
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.kotlinAndroid) apply false
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.kotlinKapt) apply false
  alias(libs.plugins.kotlinParcelize) apply false
  alias(libs.plugins.spotless)
}

tasks {
  spotless {
    kotlinGradle {
      target("**/*.kts")
      ktfmt()
    }
  }
}
