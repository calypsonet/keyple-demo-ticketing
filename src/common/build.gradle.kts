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
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.spotless)
}

///////////////////////////////////////////////////////////////////////////////
// APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

dependencies {
  implementation(libs.bitLib4j) { exclude(group = "org.slf4j") }
  implementation(libs.keypleUtilJavaLib)
  testImplementation(libs.kotlinTest)
  testImplementation(libs.assertjCore)
  testImplementation(libs.bitLib4j)
}

///////////////////////////////////////////////////////////////////////////////
// STANDARD CONFIGURATION FOR KOTLIN APP-TYPE PROJECTS
///////////////////////////////////////////////////////////////////////////////

val jvmToolchainVersion: String by project
val javaSourceLevel: String by project
val javaTargetLevel: String by project

kotlin { jvmToolchain(jvmToolchainVersion.toInt()) }

java {
  sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
  targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
}

tasks {
  spotless {
    kotlin {
      target("src/**/*.kt")
      licenseHeaderFile("../../LICENSE_HEADER")
      ktfmt()
    }
    kotlinGradle {
      target("**/*.kts")
      ktfmt()
    }
  }
  test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
  }
}
