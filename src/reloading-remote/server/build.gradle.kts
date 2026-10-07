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
  // Storage card specific components
  // Conditional dependency for the storage card library
  val storageCardLibName = "keyple-card-cna-storagecard-java-lib-2.3.1"
  val storageCardLibFile = file("../../../libs/${storageCardLibName}.jar")
  if (storageCardLibFile.exists()) {
    println("Using private storage card library: ${storageCardLibFile.name}")
    implementation(files(storageCardLibFile))
  } else {
    println("Using mock storage card library")
    implementation(files("../../../libs/${storageCardLibName}-mock.jar"))
  }

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
  implementation(libs.quarkusRest)
  implementation(libs.quarkusRestJsonb)

  // Google GSON
  implementation(libs.gson)

  // Logging: SLF4J API used by the server and third-party libraries (e.g., Keyple). The SLF4J
  // implementation is provided by Quarkus (JBoss Log Manager), configured by the "quarkus.log.*"
  // properties.
  implementation(libs.slf4jApi)
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
        // Keep the final newline written by npm
        packageJsonFile.writeText(updatedJsonText + "\n")
        println("Updated package.json version to ${project.version}")
      }
    }
val npm = if (Os.isFamily(Os.FAMILY_WINDOWS)) "npm.cmd" else "npm"
val buildDashboard by
    tasks.creating(Exec::class) {
      dependsOn.add("syncPackageVersion")
      workingDir = File("dashboard-app")
      commandLine(npm, "run", "build")
    }
val lintDashboard by
    tasks.registering(Exec::class) {
      group = "verification"
      description = "Checks the dashboard source code with ESLint"
      workingDir = File("dashboard-app")
      commandLine(npm, "run", "lint")
    }
val startServer by
    tasks.creating(Exec::class) {
      group = "server"
      workingDir = File("build")
      commandLine("java", "-jar", "${quarkus.finalName()}-full.jar")
    }

tasks {
  clean { delete("dashboard-app/build") }
  check { dependsOn(lintDashboard) }
  // The dashboard is served by Quarkus as static resources (META-INF/resources)
  processResources {
    dependsOn(buildDashboard)
    from("dashboard-app/build") { into("META-INF/resources") }
  }
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
