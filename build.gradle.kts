// Top-level build file where you can add configuration options common to all sub-projects/modules.
// NOTE (AGP 9+): the org.jetbrains.kotlin.android plugin is obsolete — AGP 9
// compiles Kotlin out of the box, so only the Android plugin is declared here.
plugins {
    alias(libs.plugins.android.application) apply false
}