rootProject.name = "kdt-reloading-multiplatform-app"

include(":composeApp")

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
  versionCatalogs { create("libs") { from(files("../../../../libs.versions.toml")) } }
}
