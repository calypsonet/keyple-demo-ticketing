///////////////////////////////////////////////////////////////////////////////
// GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

plugins {
  // Plugins loaded in the root project, sharing the classes of the Android plugin (which provides
  // the Kotlin support) with the Kotlin compiler plugins, KSP and Hilt
  alias(libs.plugins.androidApplication) apply false
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
