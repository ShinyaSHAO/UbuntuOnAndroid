plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

abstract class GenerateLegalAssetsTask : DefaultTask() {
    @get:InputFiles
    abstract val documents: ConfigurableFileCollection

    @get:InputDirectory
    abstract val licensesDirectory: DirectoryProperty

    @get:InputDirectory
    abstract val sbomDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        val legal = output.resolve("legal").apply { mkdirs() }

        documents.files.forEach { source ->
            source.copyTo(legal.resolve(source.name), overwrite = true)
        }
        licensesDirectory.get().asFile.copyRecursively(
            legal.resolve("licenses"),
            overwrite = true,
        )
        sbomDirectory.get().asFile.copyRecursively(
            legal.resolve("sbom"),
            overwrite = true,
        )
    }
}

val generateLegalAssets by tasks.registering(GenerateLegalAssetsTask::class) {
    documents.from(
        rootProject.file("LICENSE"),
        rootProject.file("PRIVACY.md"),
        rootProject.file("SOURCE_CODE.md"),
        rootProject.file("THIRD_PARTY_NOTICES.md"),
    )
    licensesDirectory.set(rootProject.layout.projectDirectory.dir("licenses"))
    sbomDirectory.set(rootProject.layout.projectDirectory.dir("sbom"))
    outputDirectory.set(layout.buildDirectory.dir("generated/legalAssets"))
}

android {
    namespace = "com.example.ubuntuonandroid"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.ubuntuonandroid"
        minSdk = 24
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            generateLegalAssets,
            GenerateLegalAssetsTask::outputDirectory,
        )
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  implementation("com.github.termux.termux-app:terminal-view:v0.118.0")
}
