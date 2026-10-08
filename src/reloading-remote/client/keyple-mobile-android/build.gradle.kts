///////////////////////////////////////////////////////////////////////////////
// GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

plugins {
  // Android and Kotlin plugins loaded once for all the projects (the application, with the Kotlin
  // support built into the Android plugin, and the common module)
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.hilt) apply false
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
