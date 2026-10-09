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
  // Keyple BOM
  implementation(platform(libs.keypleJavaBom))
  implementation(libs.keypleUtilJavaLib)

  implementation(libs.bitLib4j) {
    // Logging dependencies declared but not used by the library (log4j 1.x is end of life)
    exclude(group = "org.slf4j")
    exclude(group = "log4j")
  }

  testImplementation(libs.kotlinTest)
  testImplementation(libs.assertjCore)
  testImplementation(libs.bitLib4j)
}

///////////////////////////////////////////////////////////////////////////////
// STANDARD CONFIGURATION FOR KOTLIN LIB-TYPE PROJECTS
///////////////////////////////////////////////////////////////////////////////

val jvmToolchainVersion = project.property("jvmToolchainVersion") as String
val javaSourceLevel = project.property("javaSourceLevel") as String
val javaTargetLevel = project.property("javaTargetLevel") as String

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
