import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.kotlinParcelize)
  alias(libs.plugins.kotlinKapt)
  alias(libs.plugins.spotless)
}

///////////////////////////////////////////////////////////////////////////////
// APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////

configurations.all { exclude(group = "com.arcao", module = "slf4j-timber") }

dependencies {
  // Demo common
  implementation(project(":common"))

  // Proprietary libs
  implementation(fileTree(mapOf("dir" to "../../../libs", "include" to listOf("*.jar", "*.aar"))))

  // Keyple BOM
  implementation(platform(libs.keypleJavaBom))

  // Keypop (API)
  implementation(libs.keypopReaderApi)
  implementation(libs.keypopCalypsoCardApi)
  implementation(libs.keypopCalypsoCryptoLegacysamApi)
  implementation(libs.keypopStoragecardApi)

  // Keyple
  implementation(libs.keypleCommonApi)
  implementation(libs.keyplePluginStoragecardApi)
  implementation(libs.keypleUtilJavaLib)
  implementation(libs.keypleServiceLib)
  implementation(libs.keypleCardCalypsoLib)
  implementation(libs.keypleCardCalypsoCryptoLegacysamLib)
  implementation(libs.keyplePluginAndroidNfcLib)

  // Other Keyple plugins
  implementation(libs.keyplePluginCnaCoppernicCone2Lib)
  implementation(libs.keyplePluginCnaFamocoSeCommunicationLib)

  // Android components
  implementation(libs.androidxAppcompat)
  implementation(libs.material)
  implementation(libs.androidxConstraintLayout)
  implementation(libs.androidxActivity)
  implementation(libs.androidxFragment)
  implementation(libs.androidxMultidex)

  // Kotlin
  implementation(libs.androidxCore)
  implementation(libs.kotlinStdlibJdk8)

  // Coroutines
  implementation(libs.kotlinxCoroutinesCore)
  implementation(libs.kotlinxCoroutinesAndroid)

  // Dagger
  implementation(libs.dagger)
  implementation(libs.daggerAndroid)
  implementation(libs.daggerAndroidSupport)
  kapt(libs.daggerCompiler)
  kapt(libs.daggerAndroidProcessor)
  annotationProcessor(libs.daggerCompiler)
  annotationProcessor(libs.daggerAndroidProcessor)
  compileOnly(libs.glassfishAnnotations)

  // Lottie
  implementation(libs.lottie)

  // Devnied - Byte Utils
  implementation(libs.bitLib4j) { exclude(group = "org.slf4j") }

  // Logging
  implementation(libs.timber)
  implementation(libs.slf4jApi)
  implementation(libs.slf4jAndroid)
}

///////////////////////////////////////////////////////////////////////////////
// STANDARD CONFIGURATION FOR ANDROID KOTLIN-BASED APP-TYPE PROJECTS
///////////////////////////////////////////////////////////////////////////////

val javaSourceLevel: String by project
val javaTargetLevel: String by project

android {
  namespace = project.findProperty("androidAppNamespace") as String
  compileSdk = (project.findProperty("androidCompileSdk") as String).toInt()
  defaultConfig {
    applicationId = project.findProperty("androidAppId") as String
    minSdk = (project.findProperty("androidMinSdk") as String).toInt()
    targetSdk = (project.findProperty("androidCompileSdk") as String).toInt()
    versionCode = (project.findProperty("androidAppVersionCode") as String).toInt()
    versionName = project.findProperty("androidAppVersionName") as String
  }
  buildFeatures {
    viewBinding = true
    buildConfig = true
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
  }
  kotlin { compilerOptions { jvmTarget.set(JvmTarget.fromTarget(javaTargetLevel)) } }
  sourceSets {
    getByName("main").java.srcDirs("src/main/kotlin")
    getByName("debug").java.srcDirs("src/debug/kotlin")
  }
  packagingOptions {
    // Exclude 'META-INF/NOTICE.md' to resolve the conflict that occurs when multiple dependencies
    // include this file
    resources.excludes.add("META-INF/NOTICE.md")
  }
  applicationVariants.all {
    outputs.all {
      val outputImpl = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
      val variantName = name
      val versionName = project.version.toString()
      val newName = "${rootProject.name}-$versionName-$variantName.apk"
      outputImpl.outputFileName = newName
    }
  }
  lint { abortOnError = false }
}

tasks.withType<AbstractArchiveTask>().configureEach { archiveBaseName.set(rootProject.name) }

tasks {
  spotless {
    kotlin {
      target("src/**/*.kt")
      licenseHeaderFile("../../../LICENSE_HEADER")
      ktfmt()
    }
    kotlinGradle {
      target("**/*.kts")
      ktfmt()
    }
  }
}
