# 🌪️ Vortex3D Mobile Engine

[![Android CI Release Build](https://github.com/aryodedenwilianaramdani/Vortex3D/actions/workflows/main.yml/badge.svg)](https://github.com/aryodedenwilianaramdani/Vortex3D/actions/workflows/main.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20%28API%2024%2B%29-brightgreen.svg)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64--v8a%20%2864--bit%29-blue.svg)](https://developer.android.com/ndk/guides/abis)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Tested On](https://img.shields.io/badge/Tested%20On-Android%208%20%28aarch64%29-success.svg)](https://developer.android.com)

**Vortex3D** adalah game engine 3D mandiri dan berkinerja tinggi untuk perangkat seluler Android, terinspirasi oleh alur kerja ergonomis **Godot Engine 4 Mobile Editor**. Engine ini menggabungkan rendering 3D interaktif, simulasi fisika real-time, runtime scripting Lua, editor shader GLSL, manajemen aset hierarkis, antarmuka dock panel adaptif, dan asisten AI co-pilot.

---

## 🤖 AI Co-Pilot & Coding Agent Directives (Panduan untuk AI)

> **PENTING UNTUK AGENT AI / LLM (Claude, ChatGPT, Gemini, Copilot, Cursor, dll.):**  
> Saat Anda membaca atau memodifikasi repositori ini, patuhi aturan mutlak berikut:

1. **Arsitektur Wajib ARM64 (64-bit Only):**
   - File `app/build.gradle.kts` menetapkan `abiFilters.add("arm64-v8a")`. **JANGAN PERNAH** menambahkan `armeabi-v7a`, `x86`, atau `x86_64` kecuali diminta secara eksplisit oleh pengguna. CI/CD akan gagal otomatis jika library 32-bit terdeteksi dalam artefak.
2. **Integritas Penandatanganan & Signing Configs:**
   - **JANGAN HAPUS** blok `signingConfigs` (`release` dan `debugConfig`) di `app/build.gradle.kts`. CI/CD mengandalkan pipeline `apksigner` dengan skema **V1 + V2 + V3** otomatis.
3. **Kompatibilitas Versi SDK:**
   - Pertahankan `minSdk = 24` (Android 7.0+) dan `targetSdk = 36` / `compileSdk = 36`.
   - Pastikan kode berjalan kompatibel di Android 8.0/8.1 Oreo (API 26/27) arsitektur `aarch64`.
4. **UI & Desain Sistem:**
   - Semua UI dibuat menggunakan **Jetpack Compose** dengan **Material 3 (M3)**. Jangan membuat tata letak XML baru kecuali untuk drawable/mipmap asset.
   - Gunakan layout adaptif (`Compact`, `Medium`, `Expanded`) dengan densitas kompak terinspirasi oleh dock panel Godot 4.
5. **Konvensi Kode & Penamaan:**
   - Bahasa utama: **Kotlin** modern dengan Coroutines & StateFlow.
   - Arsitektur: **MVVM** (`EngineViewModel` sebagai orkestrator state sentral).
   - Selalu sertakan `Modifier.testTag("snake_case_name")` pada elemen aksi utama untuk pengujian otomatis.

---

## 📱 Panduan Kompatibilitas Android 8 (API 26/27) & ARM64

Jika Anda mengalami kendala saat memasang APK di perangkat Android 8 (Oreo) atau arsitektur `aarch64`, berikut adalah ringkasan teknis penyebab dan solusinya:

| Masalah yang Sering Terjadi | Penyebab Teknis | Solusi yang Diterapkan di Vortex3D |
| :--- | :--- | :--- |
| **"App not installed / Package corrupt"** | Android 8 wajib memverifikasi tanda tangan APK Skema V1 (JAR) & V2 (APK Signature Scheme v2). Build tool modern terkadang hanya menyematkan V3 atau V4. | Workflow CI/CD secara eksplisit menjalankan: `apksigner sign --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true`. |
| **Gagal Aligned Halaman 4KB** | Android versi lama dan runtime biner membutuhkan 4KB page alignment pada zip file. | CI/CD menjalankan `zipalign -f -p 4 <apk> aligned.apk` sebelum proses signing. |
| **Arsitektur Tidak Cocok** | Perangkat Android 8 `aarch64` menolak binary 32-bit jika flag native 64-bit diterapkan parsial. | `abiFilters.add("arm64-v8a")` diterapkan konsisten di Gradle dan divalidasi dengan `unzip -l *.apk`. |
| **ClassDefNotFound / R8 Crash** | ProGuard / R8 meng-strip kelas refleksi atau Compose internal tanpa Keep rules yang tepat. | `isMinifyEnabled = false` dipertahankan pada build release agar seluruh runtime Lua dan Compose stabil. |

---

## 🏛️ Arsitektur Proyek

Struktur direktori utama Vortex3D:

```text
Vortex3D/
├── .github/
│   └── workflows/
│       └── main.yml           # Pipeline CI/CD GitHub Actions (Auto-Heal, Build, Sign, Validate)
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt          # Entry point aplikasi & orkestrator mode layar
│   │   │   │   ├── engine/
│   │   │   │   │   ├── ai/                  # Integrasi AI Co-Pilot (Scene, Script, Shader Synthesis)
│   │   │   │   │   ├── assets/              # Manajemen aset 3D, material, tekstur, & shader
│   │   │   │   │   ├── lua/                 # Interpreter & Sandbox Runtime Lua Scripting
│   │   │   │   │   ├── math/                # Vector3D, Matrix4x4, Quaternion, & Raycast 3D
│   │   │   │   │   ├── model/               # Node3D, Scene Graph, Mesh Primitives, Kamera, Lampu
│   │   │   │   │   ├── physics/             # Simulasi rigid body, gravitasi, collision & impuls
│   │   │   │   │   ├── renderer/            # Viewport 3D Canvas Pipeline & Shading
│   │   │   │   │   └── storage/             # Serialisasi JSON proyek, eksportir & importir scene
│   │   │   │   ├── ui/
│   │   │   │   │   ├── editor/              # Panel editor Godot-style (ProjectHub, Studio, Hierarchy, dll.)
│   │   │   │   │   └── theme/               # Material 3 Color Schemes, Typography, Shapes
│   │   │   │   └── viewmodel/
│   │   │   │       └── EngineViewModel.kt   # State holder terpadu untuk Engine & UI
│   │   │   └── res/                         # Asset strings, drawables, mipmaps, XML rules
│   └── build.gradle.kts                     # Konfigurasi modul aplikasi (SDK 36, minSdk 24, arm64)
├── gradle/
│   └── libs.versions.toml                   # Gradle Version Catalog terpusat
├── keystore/                                # Kunci penandatanganan rilis otomatis
├── metadata.json                            # AI Studio Applet Metadata
└── README.md                                # Dokumentasi ini
```

---

## 🚀 Fitur Utama & Antarmuka Godot 4 Mobile

### 1. Project Hub & Lobby (Godot 4 Inspired)
- **Manajemen Proyek**: Membuat proyek baru, membuka proyek yang disimpan, atau memilih template siap pakai:
  - 🎮 *3D First-Person Controller*
  - 🧪 *Physics & Rigid Body Sandbox*
  - 🌲 *Low-Poly RPG Landscape*
  - 🎨 *GLSL Shader Laboratory*
- **Pengaturan Orientasi Cepat**: Beralih langsung antara mode **Landscape Studio**, **Portrait Mode**, atau **Sensor Auto-Rotate**.

### 2. Dock Panel Collapsible (Studio Mode)
- **Left Dock**: 
  - *Hierarchy (Scene Tree)*: Tambahkan Cube, Sphere, Cylinder, Plane, Torus, Directional Light, Point Light, atau Camera.
  - *File Manager & Asset Browser*: Navigasi file skrip `.lua`, shader `.glsl`, dan mesh `.obj`.
- **Center Canvas**:
  - *3D Viewport*: Interaksi sentuh multi-finger (orbit rotasi, panning, zoom kamera pinch-to-zoom).
  - *Play / Pause Toolbar*: Eksekusi simulasi fisika dan skrip Lua secara langsung di viewport dengan indikator FPS dan delta-time.
- **Right Dock**:
  - *Inspector Panel*: Pengaturan Transform (Position, Rotation, Scale), Material (Color, Roughness, Metallic), Physics (Mass, Restitution, Friction, IsKinematic), dan ikatan Skrip Lua.
- **Bottom Dock**:
  - *Lua Editor*: Editor kode dengan syntax highlighting, eksekusi runtime, dan template skrip.
  - *Shader Editor*: Editor vertex & fragment GLSL dengan live compilation check.
  - *Console Logs*: Output log real-time dari skrip Lua dan sistem engine.
  - *AI Co-Pilot*: Prompt cerdas untuk menghasilkan skrip game atau memodifikasi scene secara instan.

---

## 🛠️ Panduan Build Lokal & CI/CD

### Prasyarat:
- **JDK 17** (disarankan OpenJDK 17)
- **Android SDK** dengan Build Tools `34.0.0` ke atas
- NDK (opsional, jika mengompilasi biner C/C++)

### Perintah Build Terminal:

```bash
# 1. Bersihkan dan periksa sintaks kode
./gradlew check

# 2. Build APK Debug untuk pengujian cepat
./gradlew assembleDebug

# 3. Build Release APK (Strictly ARM64)
./gradlew assembleRelease
```

### Pipeline GitHub Actions CI/CD (`.github/workflows/main.yml`):
Setiap push ke branch utama secara otomatis menjalankan:
1. **Setup JDK 17 & Android SDK Tools**.
2. **Auto-Generate Keystore**: Membuat `keystore/release.jks` secara otomatis jika belum ada.
3. **Auto-Heal & Version Injection**: Menyelaraskan `versionCode` dan `versionName` dengan nomor run CI.
4. **Compile Release APK**: Menghasilkan biner release khusus `arm64-v8a`.
5. **Zipalign 4KB Page-Alignment**: Memastikan efisiensi paging memory kernel Android.
6. **Triple Signature (V1 + V2 + V3)**: Menandatangani APK agar kompatibel dari Android 7/8 hingga Android 15+.
7. **Verifikasi Biner 64-bit**: Memastikan tidak ada biner 32-bit yang lolos ke rilis.
8. **Upload Artefak**: File `app-arm64-release.apk` langsung dapat diunduh dari tab *Actions* di GitHub.

---

## 💡 Contoh Penggunaan Scripting Lua

Di dalam Vortex3D, Anda dapat menyematkan skrip Lua langsung ke entitas Node3D apa pun:

```lua
-- Contoh: Rotasi otomatis objek dan osilasi posisi
function on_init()
    print("Entity Vortex3D dimulai!")
    self.speed = 45.0 -- derajat per detik
end

function on_update(dt)
    -- Rotasi pada sumbu Y
    self.rotation.y = self.rotation.y + (self.speed * dt)
    
    -- Osilasi halus pada sumbu Y
    self.position.y = math.sin(os.clock() * 2.0) * 1.5
end

function on_collision(other_entity)
    print("Tabrakan terdeteksi dengan: " .. other_entity.name)
end
```

---

## 📄 Lisensi & Kontribusi

Proyek ini dibangun untuk komunitas pengembang game seluler independen dan eksperimen AI kreatif. Bebas digunakan, dimodifikasi, dan dikembangkan lebih lanjut.

Untuk berkontribusi, silakan buat *Pull Request* atau ajukan *Issue* di repositori resmi! 🚀
