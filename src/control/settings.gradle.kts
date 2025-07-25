rootProject.name = "kdt-control-app"
include (":app")

include(":common")

project(":common").projectDir = file("../common")

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
    maven(url = "https://nexus.coppernic.fr/repository/libs-release")
    google()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
  }
  versionCatalogs { create("libs") { from(files("../../libs.versions.toml")) } }
}
