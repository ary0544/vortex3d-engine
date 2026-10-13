# 🛠️ Panduan Build, Kompilasi & CI/CD — Vortex3D Engine

Dokumen ini memuat panduan teknis lengkap mengenai konfigurasi lingkungan pengembangan, tata cara kompilasi APK lokal, manajemen penandatanganan (keystore/signing), arsitektur biner ARM64, serta otomatisasi pipeline GitHub Actions CI/CD untuk **Vortex3D Mobile Engine**.

Untuk panduan dokumentasi engine dan arsitektur umum, lihat **[README.md](./README.md)**.  
Untuk panduan tata cara penggunaan fitur dalam aplikasi dan pemetaan fungsi source code, lihat **[PANDUAN_APLIKASI.md](./PANDUAN_APLIKASI.md)**.

---

## 📋 Daftar Isi
1. [Prasyarat Lingkungan Pengembangan](#1-prasyarat-lingkungan-pengembangan)
2. [Spesifikasi Toolchain & Versi SDK](#2-spesifikasi-toolchain--versi-sdk)
3. [Tata Cara Build Lokal Menggunakan Gradle](#3-tata-cara-build-lokal-menggunakan-gradle)
4. [Konfigurasi Keystore & Penandatanganan (Signing)](#4-konfigurasi-keystore--penandatanganan-signing)
5. [Ketentuan Mutlak Arsitektur ARM64-v8a (64-bit Only)](#5-ketentuan-mutlak-arsitektur-arm64-v8a-64-bit-only)
6. [Kompatibilitas Khusus Android 8 (API 26/27) & Zipalign 4KB](#6-kompatibilitas-khusus-android-8-api-2627--zipalign-4kb)
7. [Automasi Pipeline GitHub Actions CI/CD](#7-automasi-pipeline-github-actions-cicd)
8. [Pemecahan Masalah (Troubleshooting) Build](#8-pemecahan-masalah-troubleshooting-build)

---

## 1. Prasyarat Lingkungan Pengembangan

Sebelum menjalankan build lokal, pastikan workstation Anda memenuhi prasyarat berikut:

* **Java Development Kit (JDK):** JDK 17 atau JDK 21 (direkomendasikan Eclipse Temurin atau OpenJDK).
* **Android SDK:**
  * Android SDK Command-line Tools / Android Studio Ladybug atau yang lebih baru.
  * Platform SDK: **Android 16 (API 36)** (`platforms;android-36`).
  * Build-Tools: Versi `34.0.0` hingga `36.0.0`.
* **Sistem Operasi:** Linux (Ubuntu 20.04+), macOS (Apple Silicon / Intel), atau Windows (via WSL2 atau PowerShell).
* **Variabel Lingkungan:**
  * `JAVA_HOME` diarahkan ke direktori instalasi JDK.
  * `ANDROID_HOME` atau `ANDROID_SDK_ROOT` diarahkan ke direktori SDK Android.
  * Path `PATH` mencakup `$JAVA_HOME/bin`, `$ANDROID_HOME/cmdline-tools/latest/bin`, dan `$ANDROID_HOME/platform-tools`.

---

## 2. Spesifikasi Toolchain & Versi SDK

Proyek Vortex3D menggunakan konfigurasi Gradle modern dengan Kotlin DSL (`.gradle.kts`) dan Gradle Version Catalog (`gradle/libs.versions.toml`):

| Komponen Toolchain | Versi yang Dikonfigurasi | Catatan Teknis |
| :--- | :--- | :--- |
| **Android Gradle Plugin (AGP)** | `8.7.3` / `9.1.x` | Plugin aplikasi Android utama |
| **Kotlin** | `2.0.21` / `2.2.x` | Dengan Compose Compiler Plugin terintegrasi |
| **compileSdk** | `36` (Minor API Level 1) | Memungkinkan pemanfaatan API Android terbaru |
| **targetSdk** | `36` | Target keamanan & runtime Android modern |
| **minSdk** | `24` (Android 7.0 Nougat) | Jaminan kompatibilitas ke perangkat lama |
| **Java Target** | `JavaVersion.VERSION_11` | Kompatibilitas bytecode JVM |
| **Jetpack Compose** | Compose BOM modern | Material 3 & UI Runtime |

---

## 3. Tata Cara Build Lokal Menggunakan Gradle

Jalankan perintah berikut di direktori root proyek:

### 3.1. Pemeriksaan Sintaks & Unit Test
```bash
# Memberikan izin eksekusi pada wrapper (Linux / macOS)
chmod +x gradlew

# Menjalankan verifikasi kode dan pengujian unit lokal
./gradlew check
```

### 3.2. Kompilasi APK Debug
Gunakan perintah ini untuk pengujian cepat di emulator streaming atau perangkat pengembang lokal:
```bash
./gradlew assembleDebug
```
* **Lokasi Output:** `app/build/outputs/apk/debug/app-debug.apk`
* **Karakteristik:** Menggunakan penandatanganan otomatis kunci debug (`debug.keystore`).

### 3.3. Kompilasi APK Release
Untuk menghasilkan biner APK rilis yang siap dipublikasikan atau diuji performa penuhnya:
```bash
./gradlew assembleRelease
```
* **Lokasi Output:** `app/build/outputs/apk/release/app-release.apk`
* **Karakteristik:** Menggunakan kunci release (`release.jks`) dengan optimasi biner ARM64.

---

## 4. Konfigurasi Keystore & Penandatanganan (Signing)

File `app/build.gradle.kts` telah dikonfigurasi untuk menangani penandatanganan otomatis baik mode debug maupun release tanpa perlu intervensi manual:

```kotlin
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/keystore/release.jks"
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD") ?: "vortex3d_pass"
        keyAlias = System.getenv("KEY_ALIAS") ?: "vortex3d_key"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "vortex3d_pass"
    }
    create("debugConfig") {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }
}
```

### Membuat Keystore Release Mandiri (Jika Diperlukan):
Jika Anda ingin membuat keystore rilis kustom di luar CI/CD:
```bash
keytool -genkeypair -v \
  -keystore keystore/release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias vortex3d_key \
  -storepass vortex3d_pass -keypass vortex3d_pass \
  -dname "CN=Vortex3D, OU=Engine, O=Vortex, L=Jakarta, ST=DKI, C=ID"
```

> ⚠️ **PENTING:** Jangan pernah menghapus atau mengubah nama blok `debugConfig` dan `release` di `app/build.gradle.kts` karena sistem build platform AI Studio dan CI/CD mengandalkan konfigurasi ini.

---

## 5. Ketentuan Mutlak Arsitektur ARM64-v8a (64-bit Only)

Vortex3D dirancang secara eksklusif untuk arsitektur 64-bit guna memaksimalkan performa floating-point matematika 3D vektor, SIMD NEON, dan eksekusi engine:

* Di dalam `app/build.gradle.kts`:
  ```kotlin
  ndk {
      abiFilters.add("arm64-v8a")
  }
  ```
* **Aturan Kepatuhan:**
  1. **Dilarang** menambahkan ABI 32-bit (`armeabi-v7a` atau `x86`).
  2. Biner rilis diverifikasi otomatis oleh workflow CI/CD untuk memastikan tidak ada native library 32-bit yang tidak sengaja terpasang ke dalam APK.

---

## 6. Kompatibilitas Khusus Android 8 (API 26/27) & Zipalign 4KB

Perangkat dengan sistem operasi Android 8.0/8.1 Oreo (API 26/27) dan arsitektur `aarch64` memiliki persyaratan verifikasi penandatanganan dan penjajaran memori yang ketat:

### 6.1. Zipalign 4KB Page Alignment
Android mewajibkan library dan aset dalam APK disejajarkan pada kelipatan 4 kilobyte (4096 bytes) agar kernel OS dapat melakukan `mmap` langsung tanpa menyalin buffer ke RAM:
```bash
zipalign -f -p 4 app-release-unsigned.apk app-aligned.apk
```

### 6.2. Triple Signature Scheme (V1 + V2 + V3)
Jika APK hanya ditandatangani dengan Skema V3 (Android 9+) atau V4 (Android 11+), Android 8 akan menolak pemasangan dengan pesan error `"App not installed / Package corrupt"`. Solusi wajib:
```bash
apksigner sign \
  --ks keystore/release.jks \
  --ks-pass pass:vortex3d_pass \
  --ks-key-alias vortex3d_key \
  --key-pass pass:vortex3d_pass \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out app-arm64-release.apk \
  app-aligned.apk
```

### 6.3. Verifikasi Hasil Penandatanganan
```bash
apksigner verify --verbose app-arm64-release.apk
```
Output yang valid harus menunjukkan:
* `Verified using v1 scheme (JAR signing): true`
* `Verified using v2 scheme (APK Signature Scheme v2): true`
* `Verified using v3 scheme (APK Signature Scheme v3): true`

---

## 7. Automasi Pipeline GitHub Actions CI/CD

Pipeline CI/CD otomatis berada pada `.github/workflows/main.yml`. Setiap ada komit atau pull request ke branch `main`, workflow menjalankan langkah-langkah berikut:

1. **Checkout Repository**: Mengambil kode sumber terkini.
2. **Setup Temurin JDK 17 & Android SDK Cache**: Mempercepat proses download dependensi.
3. **Auto-Generate Keystore**: Jika file `keystore/release.jks` belum ada, CI akan membuatnya secara instan.
4. **Auto-Heal & Version Injection**: Menyelaraskan `versionCode` dan `versionName` dengan build number GitHub.
5. **Kompilasi Gradle Release**: Menjalankan `./gradlew assembleRelease --no-daemon`.
6. **Zipalign 4KB**: Memastikan keselarasan byte biner APK.
7. **Penandatanganan Apksigner V1+V2+V3**: Menandatangani APK secara sah untuk semua versi Android.
8. **Verifikasi Integritas Biner 64-bit**: Memeriksa isi APK via `unzip -l` untuk memastikan tidak ada `.so` 32-bit.
9. **Upload Artefak Rilis**: File `app-arm64-release.apk` langsung dipublikasikan di halaman GitHub Actions Artifacts.

---

## 8. Pemecahan Masalah (Troubleshooting) Build

### Masalah 1: `OutOfMemoryError: Java heap space` saat build
* **Penyebab:** Gradle memerlukan alokasi RAM yang memadai untuk kompilasi Kotlin dan Compose.
* **Solusi:** Periksa `gradle.properties` dan pastikan memuat parameter berikut:
  ```properties
  org.gradle.jvmargs=-Xmx3072m -Dfile.encoding=UTF-8
  ```

### Masalah 2: `Namespace not specified` atau `Unresolved reference: R`
* **Penyebab:** Konfigurasi namespace di `app/build.gradle.kts` hilang atau diubah.
* **Solusi:** Pastikan `namespace = "com.example"` tetap dipertahankan pada blok `android { ... }`.

### Masalah 3: `Package appears to be invalid` saat sideload di HP Android fisik
* **Penyebab:** Perangkat menggunakan arsitektur non-ARM64 (misal emulator x86_64) atau versi Android yang tidak kompatibel dengan signature scheme tunggal.
* **Solusi:** Pastikan perangkat fisik adalah ARM64 (`arm64-v8a`) dan tanda tangani dengan skema V1+V2+V3 menggunakan perintah di Bab 6.

---

Kembali ke:
* 📖 **[Dokumentasi Utama Vortex3D (README.md)](./README.md)**
* 🕹️ **[Panduan Fitur & Source Code Dalam Aplikasi (PANDUAN_APLIKASI.md)](./PANDUAN_APLIKASI.md)**
