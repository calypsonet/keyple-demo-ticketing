import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.tools.ant.taskdefs.condition.Os

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
  java
  alias(libs.plugins.spotless)
  alias(libs.plugins.quarkus)
}

///////////////////////////////////////////////////////////////////////////////
// APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

dependencies {
  // Demo common
  implementation(project(":common"))

  // Proprietary libs
  implementation(fileTree(mapOf("dir" to "../../../libs", "include" to listOf("*.jar"))))

  // Keyple BOM
  implementation(platform(libs.keypleJavaBom))

  // Keypop (API)
  implementation(libs.keypopReaderApi)
  implementation(libs.keypopCalypsoCardApi)
  implementation(libs.keypopCalypsoCryptoLegacysamApi)
  implementation(libs.keypopStoragecardApi)

  // Keyple
  implementation(libs.keypleCommonApi)
  implementation(libs.keypleUtilJavaLib)
  implementation(libs.keypleServiceLib)
  implementation(libs.keypleServiceResourceLib)
  implementation(libs.keypleCardCalypsoLib)
  implementation(libs.keypleCardCalypsoCryptoLegacysamLib)
  implementation(libs.keyplePluginPcscLib)
  implementation(libs.keypleDistributedNetworkLib)
  implementation(libs.keypleDistributedRemoteLib)

  // Quarkus
  implementation(enforcedPlatform(libs.quarkusBom))
  implementation(libs.quarkusResteasy)
  implementation(libs.quarkusResteasyJsonb)

  // Google GSON
  implementation(libs.gson)

  // Logging
  implementation(libs.slf4jApi)
  implementation(libs.slf4jSimple)
}

val syncPackageVersion by
    tasks.registering {
      group = "versioning"
      description = "Synchronize version in package.json with Gradle project version"
      val packageJsonFile = file("dashboard-app/package.json")
      inputs.file(packageJsonFile)
      outputs.file(packageJsonFile)
      doLast {
        val jsonText = packageJsonFile.readText()
        @Suppress("UNCHECKED_CAST")
        val json = JsonSlurper().parseText(jsonText) as MutableMap<String, Any>
        json["version"] = project.version
        val updatedJsonText = JsonOutput.prettyPrint(JsonOutput.toJson(json))
        packageJsonFile.writeText(updatedJsonText)
        println("Updated package.json version to ${project.version}")
      }
    }
val buildDashboard by
    tasks.creating(Exec::class) {
      dependsOn.add("syncPackageVersion")
      workingDir = File("dashboard-app")
      var npm = "npm"
      if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        npm = "npm.cmd"
      }
      commandLine(npm, "run", "build")
    }
val copyDashboard by
    tasks.creating(Copy::class) {
      from("dashboard-app/build")
      into("build/resources/main/META-INF/resources")
      dependsOn.add("buildDashboard")
    }
val startServer by
    tasks.creating(Exec::class) {
      group = "server"
      workingDir = File("build")
      commandLine("java", "-jar", "${quarkus.finalName()}-full.jar")
    }

tasks {
  clean { delete("dashboard-app/build") }
  jar { dependsOn.add("copyDashboard") }
}

///////////////////////////////////////////////////////////////////////////////
// STANDARD CONFIGURATION FOR JAVA APP-TYPE PROJECTS
///////////////////////////////////////////////////////////////////////////////

val javaSourceLevel: String by project
val javaTargetLevel: String by project

java {
  sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
  targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
}

tasks.withType<JavaCompile> { options.compilerArgs.add("-Xlint:deprecation") }

tasks {
  spotless {
    java {
      target("src/**/*.java")
      licenseHeaderFile("../../../LICENSE_HEADER")
      importOrder("java", "javax", "org", "com", "")
      removeUnusedImports()
      googleJavaFormat()
    }
    kotlinGradle {
      target("**/*.kts")
      ktfmt()
    }
  }
}
