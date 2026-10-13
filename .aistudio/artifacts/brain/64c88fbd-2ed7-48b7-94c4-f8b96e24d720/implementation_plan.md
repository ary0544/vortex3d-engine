# Rencana Implementasi: Sistem Log Clipboard, Integrasi Lua Langsung, UI Landscape Scrollable, dan Panduan GLB

Implementasi ini menghadirkan perbaikan alur kerja dan ergonomi studio game Vortex3D: menambahkan sistem log konsol yang dapat disalin ke clipboard dengan 1-ketukan, tombol *Attach Lua* langsung baik dari Inspector maupun Lua IDE, toolbar landscape berbasis horizontal scroll dengan proteksi safe-insets, penanganan otomatis immersive bar saat keyboard muncul/hilang, serta kartu informasi detail struktur GLB dengan rekomendasi folder penempatan yang dapat disalin langsung.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Seluruh keputusan telah diselaraskan dengan jawaban klarifikasi pengguna:

- **Konsol Log & Salin Cepat**: Konsol log terdedikasi dilengkapi tombol *Salin Cepat* (salin semua log dan salin log per-baris ke clipboard Android) untuk memudahkan pelaporan bug atau penempelan ke AI.
- **Attach Lua Dua Arah**: Tombol *Attach Script* hadir langsung di tab Inspector (dengan dialog pemilih skrip / template) dan tombol *Attach to Selected Entity* hadir langsung di header Lua IDE.
- **Toolbar Landscape Scrollable & Safe-Area**: Top bar / toolbar mode landscape diubah menjadi baris horizontal scrollable (`Modifier.horizontalScroll()`) dengan safe-insets padding dari tepi layar/tombol navigasi HP, memastikan tombol tidak pernah mengecil secara paksa.
- **Immersive Mode & Keyboard Fix**: Memperbaiki kontrol `WindowInsetsControllerCompat` agar saat soft-keyboard muncul dan ditutup, bar navigasi sistem kembali disembunyikan secara otomatis (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`) tanpa meninggalkan tombol fullscreen yang tersangkut.
- **Info GLB Lengkap & Salin Lokasi Rekomendasi**: Menambahkan dialog inspeksi GLB yang merinci jumlah simpul, segitiga, material, sub-mesh, animasi/pbr, serta box panduan rekomendasi penempatan folder (misal: `/assets/models/characters/` untuk karakter TPP dan `/assets/models/environments/` untuk permukaan/terrain) lengkap dengan tombol salin path folder.

---

### 1. Overview & Core Concept

- **Apa yang Dibangun**: Pembaruan menyeluruh pada 4 modul utama editor:
  1. **Log & Console Subsystem**: Pencatatan riwayat aksi (Engine Action Log) dengan filter (All, Lua, Physics, Render, Error) dan tombol *Copy to Clipboard* (salin log terpilih atau salin seluruh riwayat aksi untuk konsultasi ke AI).
  2. **Direct Lua Script Attachment**: Menghubungkan entitas yang dipilih di 3D Viewport langsung dengan skrip Lua tanpa harus membuat script manual dari scratch, baik melalui Entity Inspector maupun langsung dari Lua Editor.
  3. **Responsive Landscape Toolbar**: Toolbar atas di mode landscape menggunakan container scrollable yang menjaga ukuran touch target minimal 48dp tanpa membuat tombol menyusut (squishing), dilengkapi padding aman dari cutout kamera dan bar navigasi sistem.
  4. **GLB Detailed Inspector & Placement Advisor**: Dialog interaktif saat file `.glb` dipilih atau diimpor di File Manager/Asset Browser yang menampilkan ringkasan data 3D dan rekomendasi penempatan struktur folder game open-world.

---

### 2. User Experience & Visual Design

#### A. Konsol Log & Action History (Dedicated Console Dock)
- **Tampilan Header**: Memiliki badge jumlah log, filter chip (All, Info, Warn, Error, Lua), tombol *Clear*, dan tombol menonjol **"Salin Semua Log"** (dengan ikon clipboard & feedback visual saat tersalin).
- **Item Baris Log**: Setiap baris log memiliki tag warna spesifik (Biru untuk Lua, Merah untuk Error, Hijau untuk Fisika, Kuning untuk Aksi Engine) dan ikon kecil **Copy** di samping teks log untuk menyalin pesan spesifik tersebut.

#### B. Alur Penempelan Lua (Inspector + Lua IDE)
- **Di Inspector**: Bagian *Lua Script Component* kini menyediakan:
  - Tombol **"Attach Script"** dengan dropdown/menu pemilih skrip yang ada di proyek (misal: `player_controller.lua`, `terrain_collider.lua`, `orbit_camera.lua`, atau buat baru).
  - Indikator status entitas aktif yang sedang terpasang skrip.
  - Tombol **"Detach / Lepas"** dan **"Edit in Lua IDE"**.
- **Di Lua Editor**: Header atas kini menampilkan tombol **"Attach ke [Nama Entitas]"** jika ada entitas yang sedang aktif dipilih di scene. Menekan tombol ini langsung menautkan skrip yang sedang diedit ke entitas tersebut dengan konfirmasi toast/log instan.

#### C. Landscape Toolbar & Immersive Navigation
- **Toolbar Scroll Horizontal**: Row atas dibungkus dalam `Modifier.horizontalScroll(rememberScrollState())` dengan `Arrangement.spacedBy(6.dp)` dan `PaddingValues(horizontal = safeInsets)`. Tombol-tombol mempertahankan ukuran tetap (tidak menciut).
- **Perbaikan Keyboard & Fullscreen**: Penanganan listener insets (`WindowInsetsCompat.Type.ime()`) memastikan saat keyboard ditutup, sistem UI flags segera dipulihkan ke immersive sticky mode tanpa memunculkan bilah statis yang menutupi tombol game.

#### D. Detail GLB & Rekomendasi Penempatan
- **Dialog Modal Info GLB**: Saat pengguna mengetuk file `.glb` di File Manager atau Asset Browser:
  - Menampilkan ringkasan: Ukuran file, Nama Mesh, Vertex Count, Triangle Count, PBR Material (Roughness, Metallic, Base Color), status Textures & Buffer.
  - **Rekomendasi Folder**:
    - Kostum Karakter: `assets/models/characters/` (Tipe: Dynamic Actor, Mesh + Rig).
    - Permukaan / Terrain: `assets/models/world/terrain/` (Tipe: Static Ground, Mesh + Collision).
  - Tombol **"Salin Path Folder"** untuk menyalin lokasi tujuan ke clipboard perangkat.
  - Opsi tindakan langsung: **"Spawn Langsung sebagai Karakter TPP"**, **"Spawn sebagai Permukaan Dunia"**, atau **"Simpan ke Asset Library"**.

---

### 3. Key Product Decisions & Trade-Offs

- **Clipboard Integration via Android System Service**: Menggunakan `android.content.ClipboardManager` melalui `LocalContext.current` untuk menyalin log dan path folder secara native dengan umpan balik pesan ringkas (Toast / Status HUD).
- **Toolbar Non-Shrinking Policy**: Mengganti pembagian layout statis `weight()` pada toolbar dengan scrolling horizontal fleksibel. Hal ini mencegah tombol terpotong pada layar beresolusi sedang (seperti Android 8 720p/1080p).
- **Sinkronisasi Dua Arah Scripting**: Komponen `LuaScriptComponent` diperbarui agar menyimpan referensi `scriptId` yang sinkron dengan `AssetManager.scripts`, sehingga perubahan isi di editor langsung bereaksi ke entitas runtime.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        VORTEX3D STUDIO UI LAYER                        │
├────────────────────────────────────────────────────────────────────────┤
│  Top Landscape Toolbar (Horizontal Scroll + Safe Insets Padding)       │
│  [Hub] [Scene] [Docks: Left|Bottom|Right] [Play] [Reset] [Wire] [Full] │
├────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────┐ ┌─────────────────────────┐ ┌───────────────────┐ │
│ │  LEFT DOCK       │ │      CENTER PANE        │ │   RIGHT DOCK      │ │
│ │  - Hierarchy     │ │   3D Viewport / Engine  │ │ - Inspector       │ │
│ │  - Assets Browser│ │   (TPP Camera / Meshes) │ │   [+ Attach Lua]  │ │
│ │  - File Manager  │ │                         │ │ - Lua IDE         │ │
│ │    [GLB Info]    │ │                         │ │   [+ Attach to Ent]│ │
│ └──────────────────┘ └─────────────────────────┘ └───────────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│  BOTTOM CONSOLE DOCK (Dedicated Log & Action Stream)                   │
│  [Filter Chips] [Logs List] [Copy Row] [⚡ Copy All to Clipboard]     │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        VIEWMODEL & ENGINE CORE                         │
├────────────────────────────────────────────────────────────────────────┤
│ - EngineViewModel: attachScriptToEntity(), copyToClipboard()           │
│ - GlbParser / StorageManager: extractGlbMetadata(), recommendPath()   │
│ - WindowInsetsControllerCompat: Auto-rehide system bars after IME     │
└────────────────────────────────────────────────────────────────────────┘
```

#### Komponen yang Diperbarui:
1. `ConsoleLogView.kt`: Menambahkan aksi salin semua log dan salin per item ke clipboard Android.
2. `InspectorView.kt`: Menambahkan pemilih skrip dinamis dan tombol attach cepat ke entitas terpilih.
3. `LuaEditorView.kt`: Menambahkan tombol "Attach to Selected Entity" langsung di toolbar atas editor Lua.
4. `LandscapeStudioScreen.kt`: Membungkus tombol aksi dan toolbar atas dalam baris horizontal scrollable dengan padding insets navigasi layar.
5. `MainActivity.kt`: Menambahkan listener penutup keyboard (IME) agar kontrol layar penuh otomatis disembunyikan kembali.
6. `FileManagerView.kt` & `AssetBrowserView.kt`: Menambahkan tampilan info isi GLB lengkap beserta rekomendasi folder struktur dan tombol salin path.

---

### Verification Plan

- Kompilasi seluruh modul Android melalui `compile_applet` untuk memastikan kompatibilitas sintaks Compose M3 dan Kotlin.
- Memverifikasi fungsi tombol salin clipboard pada konsol log dan dialog info GLB.
- Memverifikasi alur attach script Lua dari Inspector maupun dari Lua Editor.
- Memverifikasi toolbar landscape dapat digulir horizontal tanpa mengecilkan ikon pada berbagai rasio layar.
