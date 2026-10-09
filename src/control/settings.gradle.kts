rootProject.name = "kdt-control-app"

include(":app")

// Common library of the demo: its own build, included instead of being shared as a subproject
// (one build state for all the applications, its outputs remaining up to date between builds)
includeBuild("../common")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
  }
  versionCatalogs { create("libs") { from(files("../../libs.versions.toml")) } }
}
