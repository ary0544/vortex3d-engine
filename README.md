# 🌪️ Vortex3D Mobile Engine

[![Android CI Release Build](https://github.com/aryodedenwilianaramdani/Vortex3D/actions/workflows/main.yml/badge.svg)](https://github.com/aryodedenwilianaramdani/Vortex3D/actions/workflows/main.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20%28API%2024%2B%29-brightgreen.svg)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64--v8a%20%2864--bit%29-blue.svg)](https://developer.android.com/ndk/guides/abis)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-orange.svg)](https://kotlinlang.org)
[![Tested On](https://img.shields.io/badge/Tested%20On-Android%208%20%28aarch64%29%20to%20Android%2016-success.svg)](https://developer.android.com)

**Vortex3D** adalah game engine 3D mandiri (*standalone mobile 3D game engine*) yang dirancang khusus untuk ekosistem Android. Mengadopsi alur kerja ergonomis dari **Godot Engine 4 Mobile Editor**, Vortex3D memungkinkan seluruh tahapan pembuatan game—mulai dari penyusunan adegan 3D, pemrograman logika dengan skrip Lua, perancangan shader GLSL, simulasi fisika rigid body, inspeksi model 3D glTF biner (`.glb`), sintesis audio prosedural, hingga pengujian interaktif—dijalankan secara langsung di perangkat smartphone dan tablet tanpa memerlukan komputer desktop.

---

## 📚 Navigasi Dokumentasi Resmi

Untuk kemudahan navigasi dan pemisahan dokumentasi yang terstruktur, repositori ini membagi panduan ke dalam dokumen-dokumen terpisah berikut:

* 📖 **[Panduan Penggunaan Dalam Aplikasi & Pemetaan Source Code (PANDUAN_APLIKASI.md)](./PANDUAN_APLIKASI.md)**  
  *Panduan langkah demi langkah cara mengoperasikan seluruh fitur di dalam aplikasi seluler (Project Hub, 3D Viewport, Hierarchy, Inspector, Lua Editor, GLSL Shaders, File Manager, Audio Synthesizer, AI Co-Pilot, Play Mode) beserta pemetaan langsung ke fungsi, kelas, dan method asli pada kode sumber.*
* 🛠️ **[Panduan Build, Kompilasi & CI/CD (PANDUAN_BUILD.md)](./PANDUAN_BUILD.md)**  
  *Panduan kompilasi biner APK lokal menggunakan Gradle, konfigurasi toolchain SDK 36, penandatanganan Keystore (V1+V2+V3), ketentuan mutlak arsitektur ARM64-v8a, zipalign 4KB, dan otomatisasi GitHub Actions CI/CD.*

---

## 🌟 Visi & Filosofi Desain

Pengembangan game pada perangkat bergerak sering kali terhambat oleh antarmuka desktop yang dipaksakan ke layar sentuh kecil. Vortex3D memecahkan masalah ini dengan pendekatan:

1. **Ergonomi Layar Sentuh Kompak (Touch-First UX):** Mengimplementasikan sistem dock adaptif yang dapat dilipat (*collapsible panel dock*) dan bilah alat (*toolbar*) horizontal yang aman dari lekukan layar (*safe-area insets*).
2. **Kemandirian Penuh (Zero-Dependency Runtime):** Tidak membutuhkan instalasi server eksternal atau runtime pihak ketiga. Semua subsistem (audio, interpreter skrip, fisika, rendering) dibangun native di atas JVM Kotlin dan Android Framework.
3. **Kinerja Tinggi Khusus 64-bit (ARM64 Native):** Dioptimalkan secara ketat untuk arsitektur 64-bit (`arm64-v8a`) guna memastikan kalkulasi aljabar linier matriks 3D dan floating-point fisika berjalan mulus pada 60 FPS stabil.
4. **Bantuan AI Generatif Terintegrasi:** Dilengkapi asisten co-pilot berbasis model mutakhir Gemini untuk menghasilkan skrip logika, mendiagnosis galat, dan memodifikasi adegan secara instan.

---

## 🏛️ Arsitektur Sistem Engine

Vortex3D dirancang menggunakan prinsip modularitas tinggi dengan pola arsitektur **MVVM (Model-View-ViewModel)** dan **Entity-Component-System (ECS)** ringan:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                   VORTEX3D ENGINE ARCHITECTURE                          │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                 PRESENTATION LAYER (UI)                                 │
│    ProjectHubScreen  │  LandscapeStudioScreen  │  Viewport3D  │  PlayModeOverlay        │
│    HierarchyView     │  InspectorView          │  LuaEditor   │  ShaderEditor           │
│    FileManagerView   │  ConsoleLogView         │  AiPrompt    │  DocumentationView      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                             STATE & ORCHESTRATION LAYER                                 │
│                 EngineViewModel (Central Reactive StateFlows & Coroutines)              │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                 CORE ENGINE SUBSYSTEMS                                  │
│  ┌───────────────────────┐  ┌───────────────────────┐  ┌─────────────────────────────┐  │
│  │   Scene Graph & ECS   │  │    Physics Engine     │  │      3D Canvas Pipeline     │  │
│  │  - Scene / Entity     │  │  - RigidBody Solver   │  │  - Camera3D (Orbit/Pan/Zoom)│  │
│  │  - Component System   │  │  - CollisionDetector  │  │  - Backface Culling         │  │
│  │  - Transform/Mat4/Vec3│  │  - Contact Resolution │  │  - Depth Sorting & Shading  │  │
│  └───────────────────────┘  └───────────────────────┘  └─────────────────────────────┘  │
│  ┌───────────────────────┐  ┌───────────────────────┐  ┌─────────────────────────────┐  │
│  │ Lua Scripting Runtime │  │  GLB & Asset Pipeline │  │  Audio & AI Co-Pilot        │  │
│  │  - LuaInterpreter     │  │  - GlbParser (Binary) │  │  - AudioSynthesizer (PCM)   │  │
│  │  - EngineCallbacks    │  │  - AssetManager       │  │  - GeminiEngineAssistant    │  │
│  │  - Virtual InputState │  │  - Material & Shaders │  │  - OkHttp3 REST Client      │  │
│  └───────────────────────┘  └───────────────────────┘  └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1. Subsistem Matematika 3D (`com.example.engine.math`)
* **`Vec3`**: Representasi vektor 3 dimensi dengan operasi lengkap (penjumlahan, pengurangan, perkalian skalar, perkalian silang *cross product*, perkalian titik *dot product*, normalisasi, dan perhitungan jarak).
* **`Mat4`**: Matriks transformasi $4 \times 4$ untuk komputasi translasi, rotasi sudut Euler, skala, matriks pandangan kamera (*lookAt*), dan proyeksi perspektif (*perspective*).
* **`Ray` & `MathUtils`**: Komputasi sinar untuk interaksi *raycasting* serta utilitas konversi derajat ke radian.

### 2. Subsistem Model & Scene Graph (`com.example.engine.model`)
* **`Scene`**: Wadah kontainer utama yang memuat daftar seluruh entitas, konfigurasi warna langit (*sky color*), pencahayaan ambien, arah cahaya matahari (*directional light*), serta vektor gravitasi dunia.
* **`Entity`**: Objek dalam dunia 3D yang memiliki identitas unik (`id`, `name`) dan kumpulan komponen modular.
* **`Component`**:
  * `TransformComponent`: Menyimpan posisi, rotasi Euler, skala, dan menghasilkan matriks dunia lokal (`getLocalMatrix()`).
  * `MeshRendererComponent`: Menghubungkan ID mesh geometri dan material visual.
  * `RigidBodyComponent`: Menyimpan massa, parameter pantulan (*restitution*), gesekan (*friction*), status statis/dinamis, kecepatan linier, kecepatan sudut, dan bentuk batas tumbukan (*collider shape*).
  * `LightComponent`: Sumber pencahayaan tipe `DIRECTIONAL`, `POINT`, atau `SPOT`.
  * `LuaScriptComponent`: Menautkan skrip logika pemrograman ke entitas.
  * `CameraComponent`: Parameter kamera perspektif (FOV, near plane, far plane).

### 3. Subsistem Simulasi Fisika (`com.example.engine.physics`)
* **`PhysicsEngine`**: Solver fisika rigid body real-time yang menjalankan integrasi gravitasi Euler semi-implisit, peredaman gesekan udara (*air damping*), deteksi tumbukan bidang lantai ($Y=0$), serta resolusi impuls momentum antar objek.
* **`Collider` & `CollisionDetector`**: Algoritma uji irisan geometri antara *Sphere-Sphere*, *Sphere-Box*, *Box-Box*, dan *Sphere/Box-Plane* yang menghasilkan titik kontak normal dan kedalaman penetrasi (*penetration depth*).

### 4. Subsistem Rendering 3D (`com.example.engine.renderer`)
* **`Camera3D`**: Sistem kamera 3D bola (*spherical turntable camera*) yang mendukung rotasi orbit sudut elevasi dan azimuth, pergeseran lateral (*panning*), pembesaran jarak (*zoom*), serta preset sudut pandang ortogonal (Depan, Atas, Samping, Isometrik).
* **`EngineRenderer`**: Pipeline rendering teroptimasi dengan:
  * Pengalokasian buffer simpul dinamis tanpa *garbage collection spike*.
  * *Backface Culling* untuk mengabaikan segitiga yang membelakangi kamera.
  * *Painter's Depth Sorting* untuk mengurutkan segitiga dari belakang ke depan.
  * Shading dinamis dengan interpolasi normal pencahayaan Lambertian dan Blinn-Phong.
  * Pelacakan FPS nyata dan latensi frame milidetik menggunakan rata-rata bergerak (*moving average*).

### 5. Subsistem Scripting Lua (`com.example.engine.lua`)
* **`LuaInterpreter`**: Virtual machine interpretasi skrip yang mengeksekusi fungsi siklus hidup:
  * `init()`: Inisialisasi awal entitas saat dibuat.
  * `update(dt)`: Pembaruan logika setiap frame dengan parameter delta-time.
  * `on_collision(other)`: Penanganan event tabrakan fisika.
* Menyediakan jembatan API native untuk membaca kontrol input joystick virtual, memanipulasi posisi/rotasi entitas, menerapkan gaya fisika, memutar audio, dan mencetak log ke konsol.

### 6. Subsistem Aset & Glb Parser (`com.example.engine.model.GlbParser`)
* Pengurai mandiri biner **glTF 2.0 (`.glb`)** yang membaca struktur header, mengurai chunk JSON metadata, membaca chunk biner BIN (posisi simpul, normal, koordinat UV, dan indeks elemen), serta mengekstrak parameter PBR material (*base color*, *roughness*, *metallic*).
* Menghitung statistik poligon dan memberikan rekomendasi peran aset (*Karakter TPP*, *Terrain/Ground*, atau *Props*).

### 7. Subsistem Audio Sintesis (`com.example.engine.assets.AudioSynthesizer`)
* Pembangkit audio prosedural berbasis `AudioTrack` pada laju sampel 22.050 Hz PCM 16-bit. Mampu menghasilkan efek suara sintetis (*jump*, *impact*, *laser*, *explosion*, *coin*) secara real-time tanpa memerlukan file audio eksternal.

### 8. Subsistem AI Co-Pilot (`com.example.engine.ai.GeminiEngineAssistant`)
* Integrasi asisten AI menggunakan model **Gemini 3.1 Pro Preview** yang dioptimalkan untuk memprogram skrip Lua game yang aman, memperbaiki bug kode secara otomatis, dan merekonstruksi scene berdasarkan bahasa alami pengguna.

---

## 📁 Struktur Direktori Proyek

```text
Vortex3D/
├── .github/
│   └── workflows/
│       └── main.yml           # Pipeline CI/CD GitHub Actions (Auto-Build, Sign, ARM64 Validate)
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt          # Entry point aplikasi & pengatur layar penuh (Immersive)
│   │   │   │   ├── engine/
│   │   │   │   │   ├── ai/                  # Integrasi AI Co-Pilot (GeminiEngineAssistant)
│   │   │   │   │   ├── assets/              # AudioSynthesizer prosedural & AssetManager
│   │   │   │   │   ├── lua/                 # Interpreter, sandbox, & template skrip Lua
│   │   │   │   │   ├── math/                # Vec3, Mat4, Ray, & utilitas matematika 3D
│   │   │   │   │   ├── model/               # Scene, Entity, Component, GlbParser, & Shaders
│   │   │   │   │   ├── physics/             # PhysicsEngine, Collider, & CollisionDetector
│   │   │   │   │   ├── renderer/            # Camera3D, EngineRenderer, & RenderStats
│   │   │   │   │   └── storage/             # Serialisasi JSON proyek & manajemen penyimpanan
│   │   │   │   ├── ui/
│   │   │   │   │   ├── editor/              # Seluruh antarmuka editor Compose (Studio, Hub, Viewport, dll.)
│   │   │   │   │   └── theme/               # Material 3 Color Schemes, Typography, Shapes
│   │   │   │   └── viewmodel/
│   │   │   │       └── EngineViewModel.kt   # State holder sentral reaktif untuk seluruh aplikasi
│   │   │   └── res/                         # String resources, launcher icons, drawables, themes
│   └── build.gradle.kts                     # Konfigurasi Gradle aplikasi (SDK 36, minSdk 24, ARM64)
├── gradle/
│   └── libs.versions.toml                   # Version catalog dependensi terpusat
├── keystore/                                # Kunci penandatanganan rilis APK otomatis
├── metadata.json                            # AI Studio Applet Metadata
├── PANDUAN_APLIKASI.md                      # Manual lengkap penggunaan dalam aplikasi & source code
├── PANDUAN_BUILD.md                         # Panduan kompilasi Gradle lokal, penandatanganan & CI/CD
└── README.md                                # Dokumentasi utama ini
```

---

## ⚙️ Spesifikasi Kebutuhan Sistem & Kompatibilitas Perangkat

| Parameter Sistem | Spesifikasi Minimum | Spesifikasi Rekomendasi |
| :--- | :--- | :--- |
| **Sistem Operasi** | Android 7.0 (Nougat, API 24) | Android 10 s/d Android 16 (API 29 - 36) |
| **Arsitektur CPU** | ARM64 (`arm64-v8a`, 64-bit) | ARM64 modern (Snapdragon, Dimensity, Tensor) |
| **Memori RAM** | 2 GB RAM | 4 GB RAM ke atas |
| **Ukuran Layar** | 5.0 inci (Smartphone) | 6.5 inci ke atas atau Tablet 10 inci |
| **Orientasi Layar** | Landscape atau Portrait | Landscape Studio Mode |
| **Dukungan Grafis** | OpenGL ES 3.0+ / Android Canvas Pipeline | Perangkat keras dengan akselerasi GPU penuh |

---

## 🤖 AI Co-Pilot & Coding Agent Directives

> **PANDUAN MUTLAK UNTUK AGENT AI / LLM (Claude, ChatGPT, Gemini, Copilot, Cursor, dll.):**  
> Saat Anda membaca atau memodifikasi repositori ini, patuhi aturan integritas berikut:

1. **Arsitektur Wajib ARM64 (64-bit Only):**  
   File `app/build.gradle.kts` menetapkan `abiFilters.add("arm64-v8a")`. **DILARANG** menambahkan ABI 32-bit (`armeabi-v7a`, `x86`) agar tidak merusak pipeline kompilasi dan pengujian biner.
2. **Integritas Penandatanganan & Signing Configs:**  
   **JANGAN PERNAH** menghapus atau mengubah konfigurasi `signingConfigs` (`release` dan `debugConfig`) di `app/build.gradle.kts`. Build otomatis dan platform AI Studio mengandalkan penandatanganan ini.
3. **Kompatibilitas Versi SDK:**  
   Pertahankan `minSdk = 24` dan `compileSdk = 36` / `targetSdk = 36`. Pastikan backward-compatibility tetap terjaga untuk perangkat Android 8.0/8.1 Oreo (API 26/27).
4. **UI & Desain Sistem:**  
   Semua antarmuka pengguna dibangun menggunakan **Jetpack Compose Material 3 (M3)**. Jangan membuat layout XML baru kecuali untuk drawable asset.
5. **Konvensi Kode & Penamaan:**  
   Bahasa pemrograman utama adalah **Kotlin**. Gunakan Kotlin Coroutines dan StateFlow untuk reaktivitas data. Pertahankan `Modifier.testTag("snake_case_name")` pada tombol dan elemen interaktif untuk kemudahan uji otomatis.

---

## 📄 Lisensi & Kontribusi

Vortex3D dikembangkan sebagai game engine terbuka untuk memberdayakan komunitas pengembang game seluler independen, pelajar, dan kreator konten interaktif. 

Jika Anda ingin berkontribusi, silakan buat *Pull Request* atau ajukan *Issue* baru pada repositori resmi:
- Laporkan bug atau perbaikan performa pada pelacak isu.
- Ajukan fitur baru atau template game baru melalui pull request.

Selamat berkarya dan menciptakan dunia 3D impian Anda langsung dari genggaman tangan! 🚀
