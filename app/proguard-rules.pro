# Keep rules for Visualize PDF release builds.
# Pdfium (android-pdf-viewer) ships native libs; keep its API via default R8.
-keep class com.shockwave.pdfium.** { *; }
-keep class com.github.barteksc.pdfviewer.** { *; }
-dontwarn com.shockwave.pdfium.**
