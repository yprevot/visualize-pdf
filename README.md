# Visor PDF para Android (Google Play Ready) 📄📱

[![Android CI](https://github.com/yprevot/visualize-pdf/actions/workflows/android.yml/badge.svg)](https://github.com/yprevot/visualize-pdf/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-blue.svg)](https://developer.android.com/about/versions/15)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-green.svg)](https://developer.android.com/about/versions/nuget/android-7.0)

Aplicación nativa de Android moderna, ligera y ultrarrápida para visualizar documentos en formato PDF. Desarrollada en **Kotlin** cumpliendo estrictamente las políticas de seguridad y almacenamiento de **Google Play Store**.

---

## ✨ Características Principales

- **Renderizado Nativo de PDF**: Basado en `android.graphics.pdf.PdfRenderer` para máxima velocidad sin librerías binarias de terceros.
- **Cero Permisos Invasivos**: Integración con Storage Access Framework (SAF) usando `ContentResolver` y `ActivityResultContracts.OpenDocument()`. **No requiere `MANAGE_EXTERNAL_STORAGE`**, garantizando aprobación instantánea en Google Play Console.
- **Control Gestual Multitáctil**: Zoom (*Pinch-to-zoom*), desplazamiento (*pan/drag*) y doble toque para restablecer la vista.
- **Paginación Vertical Continua**: Implementación mediante `RecyclerView` con adaptador optimizado.
- **Caché LRU de Memoria**: Renderizado asíncrono con límite dinamico de RAM para evitar errores de memoria en documentos extensos.
- **Intercepción de Intents**: Capacidad para abrir archivos PDF desde administradores de archivos, navegador, correo electrónico o mensajería instantánea.
- **Generador de Documento de Muestra Integrado**: Permite probar la aplicación de forma inmediata tras la instalación sin requerir archivos PDF previos en el dispositivo.

---

## 🛠️ Tecnologías y Requisitos

- **Lenguaje**: Kotlin
- **Min SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 35 (Android 15+)
- **Build System**: Gradle 9.5 con Kotlin DSL (`build.gradle.kts`)
- **Arquitectura**: Clean & Lightweight Native Components (Material Design 3)

---

## 🔨 Instrucciones de Compilación

### Compilar APK Debug
```bash
./gradlew assembleDebug
```
El APK se genera en: `app/build/outputs/apk/debug/app-debug.apk`

### Compilar APK Release
```bash
./gradlew assembleRelease
```
El APK se genera en: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 🔒 Seguridad y Privacidad

- Consulta la directiva de seguridad en [SECURITY.md](SECURITY.md).
- Los archivos `.jks`, `.keystore` y `local.properties` están protegidos e ignorados vía `.gitignore`.

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.
