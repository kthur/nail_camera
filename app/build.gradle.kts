import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.nailnutri"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.nailnutri"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("debugSign") {
            storeFile = file(
                System.getenv("ANDROID_KEYSTORE_PATH") 
                ?: "${System.getProperty("user.home")}/.android/debug.keystore"
            )
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs["debugSign"]
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
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
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
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

  // CameraX
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)

  // Google Generative AI (Gemini)
  implementation(libs.google.generativeai)

  // MediaPipe GenAI Tasks (Gemma)
  implementation(libs.mediapipe.tasks.genai)

// TFLite: use full AAR for runtime + extracted API JAR (no manifest) for compilation
// to avoid AGP 9.x manifest-namespace conflict between tensorflow-lite and tensorflow-lite-api.
val tfliteVersion = "2.15.0"
val extractTfliteApi by tasks.registering {
    val outputJar = layout.buildDirectory.file("tflite/tensorflow-lite-api.jar")
    outputs.file(outputJar)
    doLast {
        val aarUrl = URI("https://repo1.maven.org/maven2/org/tensorflow/tensorflow-lite-api/$tfliteVersion/tensorflow-lite-api-$tfliteVersion.aar").toURL()
        val jarFile = outputJar.get().asFile
        jarFile.parentFile.mkdirs()
        val jarBytes = ByteArrayOutputStream()
        aarUrl.openStream().use { input ->
            val buf = ByteArray(8192)
            val zis = ZipInputStream(input)
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name == "classes.jar") {
                    var len = zis.read(buf)
                    while (len != -1) {
                        jarBytes.write(buf, 0, len)
                        len = zis.read(buf)
                    }
                    break
                }
                entry = zis.nextEntry
            }
            zis.close()
        }
        jarFile.writeBytes(jarBytes.toByteArray())
        check(jarFile.exists()) { "Failed to extract classes.jar from tensorflow-lite-api AAR" }
    }
}
dependencies {
    implementation("org.tensorflow:tensorflow-lite:$tfliteVersion") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
    }
    implementation(files(extractTfliteApi.map { it.outputs.files.singleFile }))
}

// The guice dependency pulled by TFLite requires minSdk 26
configurations.all {
    exclude(group = "com.google.inject")
}

  // Kotlinx Serialization JSON
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}

// ---------------------------------------------------------------------------
// Build-time task: train nail classifier model from Kaggle dataset
// ---------------------------------------------------------------------------
val nailModelTask = tasks.register("trainNailModel") {
    description = "Download nail dataset from Kaggle and train TFLite model"
    group = "build"
    notCompatibleWithConfigurationCache()
    doLast {
        val projectDir = project.projectDir
        val tfliteFile = File(projectDir, "src/main/assets/nail_classifier.tflite")
        val scriptFile = File(projectDir.parentFile, "scripts/train_nail_model.py")
        if (tfliteFile.exists()) {
            logger.lifecycle("Nail model already exists at $tfliteFile, skipping training.")
            logger.lifecycle("  Use --force to retrain, or delete the file manually.")
        } else {
            logger.lifecycle("Training nail classifier model from Kaggle dataset...")
            logger.lifecycle("  (Requires Python 3.8+ with tensorflow, kagglehub, pillow)")
            val pythonCmd = if (System.getProperty("os.name").lowercase().contains("windows")) "python" else "python3"
            val proc = ProcessBuilder(pythonCmd, scriptFile.absolutePath)
                .directory(projectDir)
                .inheritIO()
                .start()
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                logger.warn("Model training exited with code $exitCode.")
                logger.warn("  The app will fall back to the rule-based classifier.")
            } else if (tfliteFile.exists()) {
                logger.lifecycle("Nail classifier model trained successfully!")
            }
        }
    }
}

// Run training before merging assets so the .tflite is packaged into the APK
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(nailModelTask)
}
