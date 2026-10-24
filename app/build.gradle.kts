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
        applicationId = "com.example.androidpdfviewwer"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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