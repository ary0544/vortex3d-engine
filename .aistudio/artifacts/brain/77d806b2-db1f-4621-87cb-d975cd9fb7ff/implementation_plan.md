# Full Landscape Studio, Immersive Navigation Auto-Hide & Performa Engine

Mengubah seluruh aplikasi Vortex3D Engine menjadi mode **Fixed Landscape** secara penuh (baik di Lobby / Project Hub, 3D Studio, maupun Play Mode), memperbaiki bilah tombol navigasi sistem Android agar otomatis sembunyi seketika (*instant auto-hide*) setelah keyboard ditutup tanpa tersangkut, serta menerapkan optimasi performa komprehensif pada rendering 3D dan UI lists.

## User Review & Critical Decisions

> [!IMPORTANT]
> Keputusan penting yang telah dikonfirmasi oleh pengguna:
> - **Orientasi Aplikasi**: Fixed landscape satu arah penuh di seluruh layar (`SCREEN_ORIENTATION_LANDSCAPE`), termasuk Project Hub/Lobby, Studio Editor, Play Mode, dan seluruh dialog modal.
> - **Bilah Navigasi Sistem**: Imersif penuh langsung otomatis sembunyi seketika saat keyboard (IME) ditutup.
> - **Optimasi Performa**: Menyeluruh pada Viewport 3D (backface culling, pre-allocated vertex buffers, render-on-demand) serta efisiensi UI list (keyed lazy column, eliminasi rekomposisi berulang).

- **Confirmed Decision 1**: Mengunci orientasi di `AndroidManifest.xml` (`android:screenOrientation="landscape"`) dan menghapus logika rotasi ke portrait di `MainActivity.kt` (`requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE` permanen).
- **Confirmed Decision 2**: Mengimplementasikan controller imersif berbasis `WindowInsetsCompat` dan `onWindowFocusChanged` di mana event transisi keyboard tutup (`imeBottom == 0`) langsung memicu `insetsController.hide(WindowInsetsCompat.Type.systemBars())` dengan `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` secara agresif.
- **Confirmed Decision 3**: Optimasi grafis kanvas 3D dengan Backface Culling (memeriksa arah normal poligon terhadap kamera untuk membuang poligon belakang sebelum proses kalkulasi cahaya dan pewarnaan), meminimalkan alokasi objek dalam loop `onDraw` menggunakan buffer array yang dapat digunakan kembali, serta menyempurnakan layout Project Hub agar tampil elegan dalam format landscape lebar.

---

## 1. Overview & Core Concept

- **What It Does**: Menghadirkan lingkungan game engine profesional berbasis landscape penuh ala Godot Engine / Unity Mobile di perangkat Android. Layar tidak akan pernah berputar canggung ke portrait saat kembali ke Lobby, bilah navigasi perangkat tidak akan mengganggu kanvas kerja setelah pengguna mengetik kode Lua atau shader, dan frame rate rendering 3D meningkat drastis berkat pemangkasan kalkulasi poligon yang tidak terlihat.
- **Target Audience / Persona**: Kreator game 3D mobile yang membutuhkan konsistensi ruang kerja lebar (widescreen landscape) tanpa gangguan bilah navigasi sistem.
- **Key Value**: Pengalaman editor konsisten, area kerja maksimal tanpa distraksi, responsivitas keyboard yang bersih, dan latensi frame yang jauh lebih rendah.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Alur Kerja Landscape Widescreen di Project Hub (Lobby)**:
   - Pengguna membuka aplikasi atau kembali ke Lobby dari Editor.
   - Layar tetap kokoh dalam format landscape horizontal.
   - Project Hub menampilkan tata letak grid 2-kolom / 3-kolom lebar yang proporsional untuk kartu project, preview thumbnail 3D, serta panel aksi cepat tanpa ada teks atau tombol yang terpotong.
2. **Mengetik & Menutup Keyboard (Bilah Navigasi Imersif Bersih)**:
   - Pengguna mengetuk editor script Lua, nama file, atau input prompt AI.
   - Keyboard muncul ke atas, area teks otomatis menggeser (*ime-aware insets*).
   - Saat pengguna menekan "Done", menyembunyikan keyboard, atau mengetuk area di luar input, keyboard menghilang dan bilah navigasi Android seketika lenyap ke mode imersif penuh (*zero delay*), mengembalikan layar penuh untuk kanvas 3D.
3. **Viewport 3D Berkecepatan Tinggi (Backface Culling & Zero-Garbage Frame)**:
   - Saat mengorbit model berpoligon tinggi (GLB ratusan hingga ribuan poligon), frame rate tetap mulus stabil di 60 FPS tanpa jeda garbage collection (*stutter-free*).

### Visual Identity & Theme

- **Palet Warna**: Deep Navy Void (`#0B0F17`), Godot Slate (`#141A24`), Cyan Accent (`#00E5FF`), Neon Green (`#7EE787`).
- **Layout Ergonomis Landscape**:
  - Top bar ringkas dengan navigasi cepat ke Lobby, Simulation, dan Wireframe.
  - Dock kiri (Scene Tree, Assets, File Manager) dan Dock kanan (Inspector, Lua IDE, AI Copilot) dapat dibuka-tutup (*collapsible*).
  - Area Viewport 3D memanfaatkan ruang tengah secara leluasa.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Kunci Permanen Fixed Landscape**:
  - *Chosen Approach*: Menghilangkan mode portrait dari seluruh siklus hidup Activity (`ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE`).
  - *Why*: Sebuah game engine 3D membutuhkan ruang kerja horizontal untuk timeline, hierarki objek, kanvas 3D, dan editor script secara simultan. Menjaga landscape secara permanen menghilangkan jeda konfigurasi ulang tampilan layar (*orientation flicker*).
- **Decision 2: Backface Culling pada Software Renderer**:
  - *Chosen Approach*: Menghitung tanda dot product arah normal poligon di ruang layar (*screen-space normal z-component / 2D cross product*) sebelum shading.
  - *Why*: Lebih dari 45–50% segitiga model tertutup di sisi belakang. Membuang poligon ini sebelum kalkulasi pencahayaan memangkas beban CPU dan Canvas DrawPath hingga separuhnya.
- **Decision 3: Sistem Imersif Reaktif Terhadap IME & Window Focus**:
  - *Chosen Approach*: Menyambungkan `WindowInsetsAnimationCallback` / `imeNestedScrollConnection` serta lifecycle listener window focus ke `WindowInsetsControllerCompat`.
  - *Why*: Di perangkat Android modern, gesture bar / 3-tombol navigasi sering kali tetap tertinggal jika kontrol imersif hanya dipanggil satu kali saat pembuatan activity.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   AndroidManifest.xml                  │
│       screenOrientation = "landscape" (Permanent)      │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│                      MainActivity                      │
│  - Enforce SCREEN_ORIENTATION_LANDSCAPE                │
│  - ImmersiveController:                                │
│      * WindowInsetsCompat IME change listener          │
│      * onWindowFocusChanged instant systemBars.hide()  │
│      * BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE           │
└──────────────────────────┬─────────────────────────────┘
                           │
            ┌──────────────┴──────────────┐
            ▼                             ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│     ProjectHubScreen    │   │  LandscapeStudioScreen  │
│ - Widescreen grid 2/3   │   │ - Collapsible Docks     │
│ - Keyed LazyColumn      │   │ - Fast Canvas Viewport  │
│ - Landscape Modals      │   │ - Backface Culling      │
└─────────────────────────┘   └─────────────────────────┘
                                          │
                                          ▼
                              ┌─────────────────────────┐
                              │    3D Engine Renderer   │
                              │ - Screen-space Culling  │
                              │ - Reusable FloatBuffers │
                              │ - Skip idle frames      │
                              └─────────────────────────┘
```

### Rencana File yang Dimodifikasi:
1. `app/src/main/AndroidManifest.xml`: Kunci `screenOrientation="landscape"`.
2. `app/src/main/java/com/example/MainActivity.kt`: Kunci orientasi landscape permanen, mekanisme auto-hide navigation bar imersif saat keyboard tutup dan saat window mendapat fokus kembali.
3. `app/src/main/java/com/example/ui/editor/ProjectHubScreen.kt`: Penyesuaian tata letak grid dan dialog agar proporsional untuk landscape lebar.
4. `app/src/main/java/com/example/ui/editor/GlbImportPreviewDialog.kt` & `app/src/main/java/com/example/ui/editor/Viewport3D.kt`: Implementasi Backface Culling pada rendering segitiga, pre-alokasi buffer, dan penanganan navigasi imersif.
