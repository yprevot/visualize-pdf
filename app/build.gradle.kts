import java.util.Properties
import java.io.FileInputStream

// NOTE (AGP 9+): no Kotlin plugin here — AGP 9 compiles Kotlin natively
// and derives the Kotlin jvmTarget from compileOptions (Java 17).
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.androidpdfviewwer"
    // core-ktx 1.19.0 requires compiling against API 37+ (AAR metadata check).
    // targetSdk stays at 35 (Play requirement); compileSdk may exceed it.
    compileSdk = 37

    val appVersionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull()
        ?: System.getenv("VERSION_CODE")?.toIntOrNull()
        ?: 1
    val appVersionName = project.findProperty("versionName")?.toString()
        ?: System.getenv("VERSION_NAME")
        ?: "1.0.0"

    defaultConfig {
        applicationId = "com.yprevot.visualizepdf"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
                ?: project.findProperty("storeFile")?.toString()
                ?: System.getenv("KEYSTORE_FILE")
            val storePass = keystoreProperties.getProperty("storePassword")
                ?: project.findProperty("storePassword")?.toString()
                ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAl = keystoreProperties.getProperty("keyAlias")
                ?: project.findProperty("keyAlias")?.toString()
                ?: System.getenv("KEY_ALIAS")
            val keyPass = keystoreProperties.getProperty("keyPassword")
                ?: project.findProperty("keyPassword")?.toString()
                ?: System.getenv("KEY_PASSWORD")

            if (storeFilePath != null && file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = keyAl
                keyPassword = keyPass
            } else {
                // Fallback a debug keystore para desarrollo local o builds sin clave de producción
                val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
                if (debugKeystore.exists()) {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.pdf.viewer)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}