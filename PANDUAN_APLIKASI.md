# 📖 Panduan Penggunaan Lengkap Dalam Aplikasi & Pemetaan Source Code — Vortex3D Engine

Selamat datang di panduan resmi penggunaan **Vortex3D Mobile Engine**. Dokumen ini dirancang sebagai manual komprehensif bagi pengembang game untuk memahami alur kerja di dalam aplikasi seluler, cara mengoperasikan setiap fitur, serta **pemetaan langsung setiap kemampuan ke fungsi, kelas, dan method asli dalam kode sumber (source code)**.

Untuk ringkasan gambaran umum dan arsitektur tingkat tinggi engine, baca **[README.md](./README.md)**.  
Untuk panduan kompilasi biner APK dan GitHub Actions CI/CD, baca **[PANDUAN_BUILD.md](./PANDUAN_BUILD.md)**.

---

## 📑 Daftar Isi
1. [Alur Navigasi Utama & Project Hub](#1-alur-navigasi-utama--project-hub)
2. [Tata Letak Studio & Ergonomi Dock Panel (Godot 4 Mobile Style)](#2-tata-letak-studio--ergonomi-dock-panel-godot-4-mobile-style)
3. [Viewport 3D, Navigasi Multi-Touch & Kamera 3D](#3-viewport-3d-navigasi-multi-touch--kamera-3d)
4. [Hierarchy & Scene Graph (Pohon Entitas)](#4-hierarchy--scene-graph-pohon-entitas)
5. [Inspector Panel & Entity Component System (ECS)](#5-inspector-panel--entity-component-system-ecs)
6. [Runtime Scripting Lua & In-App Code Editor](#6-runtime-scripting-lua--in-app-code-editor)
7. [GLSL Shader Editor & PBR Material Shading](#7-glsl-shader-editor--pbr-material-shading)
8. [File Manager, Asset Browser & GLB 3D Inspector](#8-file-manager-asset-browser--glb-3d-inspector)
9. [Audio Synthesizer Prosedural Real-time](#9-audio-synthesizer-prosedural-real-time)
10. [Asisten AI Co-Pilot (Gemini Engine Assistant)](#10-asisten-ai-co-pilot-gemini-engine-assistant)
11. [Konsol Log & Riwayat Debug Engine](#11-konsol-log--riwayat-debug-engine)
12. [Play Mode & Pengujian Interaktif](#12-play-mode--pengujian-interaktif)
13. [Tabel Rangkuman Pemetaan Fitur ke Source Code](#13-tabel-rangkuman-pemetaan-fitur-ke-source-code)

---

## 1. Alur Navigasi Utama & Project Hub

Saat aplikasi dibuka, layar pertama yang menyambut Anda adalah **Project Hub (Lobby)**, terinspirasi oleh Project Manager Godot 4.

```
                    ┌─────────────────────────┐
                    │    ProjectHubScreen     │
                    │   (Pilih/Buat Proyek)   │
                    └────────────┬────────────┘
                                 │ Buka Proyek
                                 ▼
                    ┌─────────────────────────┐
                    │  LandscapeStudioScreen  │
                    │  (Editor Mode Utama)    │
                    └────────────┬────────────┘
                                 │ Tekan "Play"
                                 ▼
                    ┌─────────────────────────┐
                    │     PlayModeOverlay     │
                    │  (Virtual Joystick 3D)  │
                    └─────────────────────────┘
```

### Cara Penggunaan:
1. **Membuka Proyek:** Ketuk kartu proyek pada daftar (misal *Physics Playground*, *Cyber FPS Arena*, atau *Neon Cyberpunk Gate*).
2. **Membuat Proyek Baru:** Ketuk tombol **"+ Proyek Baru"** di bagian atas, pilih salah satu template awal, beri nama proyek, lalu tekan **"Buat Proyek"**.
3. **Mengatur Orientasi Layar:** Setiap proyek memiliki pengaturan orientasi bawaan:
   - **Landscape (Horizontal):** Ideal untuk workstation tablet atau layar HP lebar.
   - **Portrait (Vertical):** Nyaman untuk pengeditan satu tangan.
   - **Auto-Sensor:** Orientasi mengikuti sensor giroskop perangkat.
4. **Menghapus Proyek:** Ketuk ikon tempat sampah pada kartu proyek untuk menghapus proyek yang tidak diperlukan.

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/ProjectHubScreen.kt`
* **Data Model:** `app/src/main/java/com/example/viewmodel/EngineViewModel.kt` (`data class ProjectItem`)
* **Fungsi Logika Utama:**
  * `EngineViewModel.createNewProject(name, template, orientation)`: Menginisialisasi scene baru dari template bawaan dan mendaftarkannya ke state flow `_projects`.
  * `EngineViewModel.loadProject(projectId)`: Mengaktifkan proyek, memuat scene, dan mengarahkan tampilan keluar dari lobby (`_isInLobby.value = false`).
  * `EngineViewModel.deleteProject(projectId)`: Menghapus proyek dari memori dan penyimpanan lokal.
  * `Scene.createDefaultPhysicsPlayground()`: Generator scene fisika (`app/src/main/java/com/example/engine/model/Scene.kt`).
  * `Scene.createFpsArena()`: Generator scene arena First-Person Shooter.
  * `Scene.createCyberpunkGlow()`: Generator scene futuristik dengan pencahayaan warna-warni.

---

## 2. Tata Letak Studio & Ergonomi Dock Panel (Godot 4 Mobile Style)

Antarmuka editor mengadopsi sistem panel modular yang dapat dilipat (*collapsible docks*) agar ruang kerja di layar smartphone tetap lapang dan bebas gangguan.

```
┌────────────────────────────────────────────────────────────────────────┐
│ [≡ Menu] [Proyek: Arena] [Undo] [Redo] [Wire] [Play ▶] [Layar Penuh ⛶] │ Top Toolbar
├──────────────┬──────────────────────────────────────────┬──────────────┤
│  LEFT DOCK   │              CENTER CANVAS               │  RIGHT DOCK  │
│  [Tree|Files]│                                          │  [Inspector] │
│              │               Viewport 3D                │  [Lua Script]│
│  Hierarchy / │        (Orbit / Pan / Pinch / Grid)      │  [Shaders]   │
│  File Mgr    │                                          │  [Docs]      │
├──────────────┴──────────────────────────────────────────┴──────────────┤
│  BOTTOM DOCK: [Console Log] [Render Stats] [AI Co-Pilot] [Asset Files] │
└────────────────────────────────────────────────────────────────────────┘
```

### Cara Penggunaan:
1. **Membuka/Menutup Panel:**
   - Gunakan tombol toggle pada bilah navigasi atau geser tepi layar untuk menyembunyikan/menampilkan Left Dock, Right Dock, atau Bottom Dock.
2. **Bilah Alat (Toolbar) Dapat Digulir:**
   - Toolbar atas mendukung gestur gulir horizontal (*horizontal scroll*). Tombol aksi tidak akan pernah terpotong pada smartphone dengan rasio sempit.
   - Aman dari poni/notch kamera berkat implementasi `safeDrawingPadding()`.
3. **Mode Layar Penuh (Immersive Mode):**
   - Ketuk tombol **"Layar Penuh"** di toolbar kanan untuk menyembunyikan status bar dan bar navigasi sistem Android.
   - Jika keyboard virtual muncul lalu ditutup, sistem secara otomatis mengembalikan mode layar penuh tanpa gangguan.

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/LandscapeStudioScreen.kt`
* **Entry Point Activity:** `app/src/main/java/com/example/MainActivity.kt`
* **Fungsi Logika Utama:**
  * `EngineViewModel.isLeftDockOpen`: `StateFlow<Boolean>` untuk status dock kiri.
  * `EngineViewModel.isRightDockOpen`: `StateFlow<Boolean>` untuk status dock kanan.
  * `EngineViewModel.isBottomDockOpen`: `StateFlow<Boolean>` untuk status dock bawah.
  * `EngineViewModel.toggleLeftDock()`, `toggleRightDock()`, `toggleBottomDock()`.
  * `MainActivity.setupSystemBars()`: Mengatur `WindowInsetsControllerCompat` untuk mode immersive transparan.

---

## 3. Viewport 3D, Navigasi Multi-Touch & Kamera 3D

Viewport 3D adalah inti dari tampilan visual tempat scene dirender secara real-time dengan depth-sorting dan backface culling efisien.

### Gestur Multi-Touch & Navigasi:
* **Orbit Kamera (1 Jari Geser):** Memutar sudut pandang kamera mengelilingi titik fokus sasaran (*turntable orbit*).
* **Panning Kamera (2 Jari Geser):** Menggeser titik fokus sasaran (*target position*) secara lateral (kiri/kanan/atas/bawah).
* **Zoom Kamera (2 Jari Cubit / Pinch):** Mendekatkan atau menjauhkan jarak kamera (*distance zoom*).
* **Preset Sudut Pandang:** Tombol cepat di panel kamera untuk beralih ke:
  * **Depan (Front View):** Pitch 0°, Yaw 0°
  * **Atas (Top View):** Pitch 85°, Yaw 0°
  * **Samping (Side View):** Pitch 0°, Yaw 90°
  * **Isometrik (Isometric 3D):** Pitch 30°, Yaw 45°
  * **Reset Kamera:** Mengembalikan kamera ke posisi default `(0, 1, 0)`.
* **Pengaturan Inversi & Sensitivitas:**
  * Toggle **Invert Sumbu X** dan **Invert Sumbu Y**.
  * Slider sensitivitas gerak Orbit, Pan, dan Zoom.

### Implementasi Source Code:
* **Komponen Viewport:** `app/src/main/java/com/example/ui/editor/Viewport3D.kt`
* **Model Kamera 3D:** `app/src/main/java/com/example/engine/renderer/Camera3D.kt`
  * `Camera3D.orbit(deltaYaw, deltaPitch)`: Menghitung rotasi sudut azimuth dan elevasi bola.
  * `Camera3D.pan(deltaX, deltaY)`: Menghitung vektor ortogonal kamera dan menggeser `target`.
  * `Camera3D.zoom(deltaDist)`: Menyesuaikan jarak kamera dengan pembatasan rentang aman `[1.5f .. 50.0f]`.
  * `Camera3D.getViewMatrix()`: Mengonversi posisi mata (*eye position*) dan target ke matriks `lookAt`.
  * `Camera3D.getProjectionMatrix(aspect)`: Membentuk matriks proyeksi perspektif dengan FOV dan bidang dekat/jauh (*near/far clipping plane*).
* **Pipeline Rendering:** `app/src/main/java/com/example/engine/renderer/EngineRenderer.kt`
  * `EngineRenderer.renderScene()`: Mengorkestrasi transformasi matriks dunia ke layar, algoritma *Backface Culling*, pengurutan kedalaman segitiga (*Painters Algorithm*), dan penggambaran bayangan pencahayaan Lambertian/Blinn-Phong.
  * `EngineRenderer.renderGrid()`: Menggambar garis kisi 3D horizontal untuk orientasi spasial.

---

## 4. Hierarchy & Scene Graph (Pohon Entitas)

Panel Hierarchy menampilkan seluruh objek (entitas) yang terdapat di dalam scene yang sedang aktif.

### Cara Penggunaan:
1. **Memilih Entitas:** Ketuk nama entitas pada daftar untuk menyorotnya di Viewport 3D dan membuka detailnya di panel Inspector.
2. **Menambahkan Entitas Baru:** Ketuk tombol **"+ Tambah Objek"**, pilih bentuk yang diinginkan:
   * **Cube:** Kubus 6 sisi.
   * **Sphere:** Bola poligon UV.
   * **Cylinder:** Silinder tabung.
   * **Plane:** Lantai datar 2D.
   * **Torus:** Donat cincin hologram.
   * **Directional Light / Point Light:** Sumber cahaya matahari atau titik lampu lokal.
3. **Menduplikasi Entitas:** Ketuk ikon salin (duplikat) di samping nama objek untuk menyalin seluruh properti, material, dan komponen fisika objek tersebut.
4. **Menghapus Entitas:** Ketuk ikon tempat sampah untuk mencopot objek dari scene.

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/HierarchyView.kt`
* **Data Model:** `app/src/main/java/com/example/engine/model/Scene.kt` & `app/src/main/java/com/example/engine/model/Entity.kt`
* **Fungsi Logika Utama:**
  * `EngineViewModel.selectEntity(entityId)`: Memperbarui StateFlow `selectedEntityId`.
  * `EngineViewModel.addEntity(entity)`: Memasukkan objek baru ke dalam scene aktif.
  * `EngineViewModel.addPrimitiveEntity(primitiveType)`: Membuat entitas instan dengan transform default dan mesh geometri yang sesuai.
  * `EngineViewModel.duplicateEntity(entityId)`: Menghasilkan salinan entitas lengkap dengan ID unik baru.
  * `EngineViewModel.removeEntity(entityId)`: Menghapus entitas dari daftar `scene.entities`.

---

## 5. Inspector Panel & Entity Component System (ECS)

Panel Inspector memungkinkan konfigurasi mendalam terhadap komponen dari entitas yang sedang dipilih.

```
┌──────────────────────────────────────────────┐
│ INSPECTOR: Player Orb (ent_hero_orb)         │
├──────────────────────────────────────────────┤
│ ▼ TRANSFORM                                  │
│   Position: [X: 0.0] [Y: 4.5] [Z: 0.0]       │
│   Rotation: [X: 0.0] [Y: 0.0] [Z: 0.0]       │
│   Scale:    [X: 1.0] [Y: 1.0] [Z: 1.0]       │
├──────────────────────────────────────────────┤
│ ▼ MATERIAL & SHADING                         │
│   Mesh: Sphere    Material: Emas Cyber       │
│   Albedo: [Color Picker]  Roughness: [0.3]   │
├──────────────────────────────────────────────┤
│ ▼ RIGIDBODY & FISIKA                         │
│   Mass: 2.0 kg    Bounciness: 0.75           │
│   [ ] Static Ground   Collider: SPHERE       │
├──────────────────────────────────────────────┤
│ ▼ LUA SCRIPT ATTACHMENT                      │
│   Skrip: HeroController.lua                  │
│   [Buka di Editor]  [Lepas Skrip]            │
└──────────────────────────────────────────────┘
```

### Komponen yang Didukung:
1. **TransformComponent:**
   * Posisi 3D ($X, Y, Z$)
   * Rotasi sudut Euler dalam derajat ($Pitch, Yaw, Roll$)
   * Skala 3D ($Scale X, Scale Y, Scale Z$)
2. **MeshRendererComponent:**
   * Tipe geometri (`mesh_cube`, `mesh_sphere`, `mesh_plane`, `mesh_torus`, atau mesh impor GLB).
   * Material ID & warna dasar albedo.
   * Parameter PBR: *Roughness* (kekasaran) dan *Metallic* (kilau logam).
3. **RigidBodyComponent & Collider:**
   * Massa (kg).
   * Tipe Gerak: Dinamis vs Statis (*isStatic / isKinematic*).
   * Bounciness (*restitution*): Koefisien pantulan objek saat bertabrakan ($0.0 - 1.0$).
   * Gesekan (*friction*): Mencegah pergeseran terus-menerus di atas lantai.
   * Bentuk Collider: `BOX`, `SPHERE`, atau `PLANE`.
4. **LuaScriptComponent:**
   * Menautkan skrip logika Lua ke entitas.
   * Terdapat tombol langsung **"+ Attach Script Lua"**, daftar pilihan skrip yang tersedia di proyek, atau buat skrip baru dari template.

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/InspectorView.kt`
* **Definisi Komponen:** `app/src/main/java/com/example/engine/model/Component.kt`
  * `TransformComponent.getLocalMatrix()`: Menghasilkan matriks transformasi $4 \times 4$ ($Translation \times Rotation \times Scale$).
  * `MeshRendererComponent`: Menentukan mesh dan material yang dirender.
  * `RigidBodyComponent`: Menampung parameter simulasi fisika rigid body.
  * `LuaScriptComponent`: Menampung ID skrip, nama, dan kode sumber Lua.
* **Fungsi Logika Utama:**
  * `EngineViewModel.updateEntityTransform(id, transform)`: Memperbarui posisi/rotasi/skala entitas.
  * `EngineViewModel.updateEntityMaterial(id, materialId, color, roughness, metallic)`: Memperbarui properti visual objek.
  * `EngineViewModel.updateEntityRigidBody(id, rigidBody)`: Mengubah parameter fisika objek secara interaktif.
  * `EngineViewModel.attachScriptToEntity(entityId, scriptAsset)`: Memasang skrip Lua ke objek aktif.
  * `EngineViewModel.detachScriptFromEntity(entityId)`: Melepas keterikatan skrip dari objek.

---

## 6. Runtime Scripting Lua & In-App Code Editor

Vortex3D dilengkapi lingkungan runtime scripting Lua mandiri. Anda dapat menulis skrip game langsung pada perangkat Android tanpa memerlukan PC.

### Alur Kerja Penulisan & Pemasangan Skrip:
1. **Buka Tab Lua Editor:** Akses dari tab bottom dock atau panel kanan.
2. **Pilih atau Buat Skrip:**
   * Gunakan tombol template untuk memuat skrip siap pakai: *Rotator*, *Hero Controller*, *Bouncer*, atau *Physics Spawner*.
3. **Hubungkan dengan Entitas Terpilih:**
   * Di bagian atas Lua Editor, terdapat bilah target entitas aktif dengan tombol **"Attach ke Entitas Terpilih"**. Satu ketukan langsung mengaitkan kode yang sedang diedit dengan objek 3D.
4. **Jalankan Simulasi:** Tekan tombol Play di toolbar atas untuk melihat skrip dieksekusi secara instan pada 60 FPS.

### Siklus Hidup & API Bawaan di Skrip Lua:
```lua
-- Dipanggil satu kali saat entitas dimunculkan ke scene
function init()
    engine:log("Entitas berhasil diinisialisasi!")
    entity:set_color(0.2, 0.8, 1.0, 1.0)
end

-- Dipanggil setiap frame (dt = delta time dalam detik)
function update(dt)
    -- Membaca input joystick virtual
    local h = input:get_axis("Horizontal")
    local v = input:get_axis("Vertical")
    
    if h ~= 0 or v ~= 0 then
        -- Mendorong entitas menggunakan solver fisika
        physics:apply_force(h * 20.0, 0, v * 20.0)
    end
    
    -- Memeriksa tombol lompat
    if input:is_action_pressed("Jump") then
        physics:apply_impulse(0, 8.0, 0)
        engine:play_sound("jump")
    end
end

-- Dipanggil saat terjadi tabrakan fisika dengan entitas lain
function on_collision(other)
    engine:play_sound("impact")
    engine:log("Tabrakan dengan entitas: " .. other)
end
```

### Tabel API Engine untuk Skrip Lua:
| Kategori API | Perintah / Fungsi Lua | Penjelasan Fungsi |
| :--- | :--- | :--- |
| **Transform** | `entity:rotate(rx, ry, rz)` | Memutar objek sebesar derajat tertentu per sumbu |
| **Transform** | `entity:translate(dx, dy, dz)` | Menggeser posisi objek secara bertahap |
| **Transform** | `entity:set_position(x, y, z)` | Mengatur koordinat posisi absolut objek |
| **Fisika** | `physics:apply_force(fx, fy, fz)` | Menerapkan gaya dorong kontinu ($F = m \cdot a$) |
| **Fisika** | `physics:apply_impulse(ix, iy, iz)` | Menerapkan impuls kejut instan (misal lompatan) |
| **Audio** | `engine:play_sound(nama)` | Memutar efek sintetis (`jump`, `impact`, `laser`, dll.) |
| **Logging** | `engine:log(pesan)` / `print(pesan)` | Menulis log ke panel konsol |
| **Input** | `input:get_axis("Horizontal" / "Vertical")` | Membaca nilai analog joystick ($-1.0$ s/d $1.0$) |
| **Input** | `input:is_action_pressed("Jump" / "Fire")` | Memeriksa apakah tombol aksi sedang ditekan |

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/LuaEditorView.kt`
* **Virtual Machine & Parser:** `app/src/main/java/com/example/engine/lua/LuaInterpreter.kt`
  * `LuaInterpreter.executeInit(script, entity)`: Mengeksekusi blok fungsi `init()`.
  * `LuaInterpreter.executeUpdate(script, dt, entity, input)`: Menginjeksi delta-time dan input lalu mengeksekusi `update(dt)`.
  * `LuaInterpreter.executeOnCollision(script, otherName, entity)`: Mengeksekusi event `on_collision(other)`.
  * `LuaInterpreter.executeLine()`: Parser instruksi baris perintah aman tanpa crash.
* **Template Generator:** `app/src/main/java/com/example/engine/lua/LuaScriptTemplate.kt`

---

## 7. GLSL Shader Editor & PBR Material Shading

Vortex3D menyediakan editor shader interaktif untuk memodifikasi pipeline visual vertex dan fragment shader.

### Fitur Shader Editor:
* **Dukungan Dua Tab:** Beralih antara tab **Vertex Shader** dan **Fragment Shader**.
* **Pemeriksaan Kompilasi Real-time:** Menampilkan status validasi sintaks shader secara instan di bagian bawah editor.
* **Uniform Bawaan:**
  * `u_Time`: Waktu berjalan simulasi dalam detik (untuk efek gelombang air atau hologram berdenyut).
  * `u_Resolution`: Ukuran resolusi viewport ($Lebar, Tinggi$).
  * `u_ModelViewProj`: Matriks gabungan Model-View-Projection $4 \times 4$.
  * `u_Color`: Vektor warna dasar albedo RGBA.

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/ShaderEditorView.kt`
* **Model Shader:** `app/src/main/java/com/example/engine/model/Shader.kt` (`data class ShaderProgram`)
* **Pipeline Rendering:** `app/src/main/java/com/example/engine/renderer/EngineRenderer.kt`
  * Mengevaluasi efek shader seperti air bergelombang (*Water Caustics*), grid pendar neon (*Cyber Hologram*), dan shading phong metalik.

---

## 8. File Manager, Asset Browser & GLB 3D Inspector

Sistem manajemen aset memungkinkan pengelolaan file proyek internal serta inspeksi mendalam terhadap file model 3D biner glTF (`.glb`).

```
┌─────────────────────────────────────────────────────────┐
│ GODOT-STYLE 3D MODEL IMPORTER & PREVIEW: hero.glb      │
├──────────────────────────────┬──────────────────────────┤
│ [MINI 3D ORBIT VIEWPORT]     │ [INSPEKTOR HIERARKI & PBR]│
│ • Orbit Turntable (1-Jari)   │ • Pohon Node & Skeleton  │
│ • Zoom Pinch (2-Jari)        │   ├─ RootNode            │
│ • Shaded / Wireframe / Grid  │   │   └─ Hips (BONE)     │
│ • Indikator Sendi Tulang     │ • Track Animasi: Walk/Run│
├──────────────────────────────┼──────────────────────────┤
│ [PEMUTAR ANIMASI REAL-TIME]  │ [PENGATURAN IMPOR]       │
│ [▶ Play] [Scrubber Slider]   │ • Skala Normalisasi Otom.│
│ Durasi: 0.00s / 1.45s        │ • Collider: Box / Sphere │
├──────────────────────────────┴──────────────────────────┤
│ [Batal]         [Simpan ke Assets]       [Spawn ke 3D ★]│
└─────────────────────────────────────────────────────────┘
```

### Alur Inspeksi & Impor Model GLB Ala Godot Engine:
1. **Pilih File GLB:** Ketuk file `.glb` apa pun di *File Manager*, *Asset Browser*, atau *Full Screen File Picker*.
2. **Dialog Preview Otomatis Terbuka:**
   - Aplikasi otomatis menampilkan modal dialog inspeksi layar penuh ala *Advanced Import Settings* Godot 4.
3. **Viewport Preview 3D Interaktif:**
   - **Orbit Kamera Turntable:** Geser 1 jari di viewport mini untuk memutar sudut pandang model secara 360 derajat.
   - **Zoom Pinch:** Cubit atau renggangkan 2 jari untuk memperbesar / memperkecil objek.
   - **Toggle Tampilan:** Tombol cepat untuk beralih antara *Shaded PBR*, *Wireframe*, *Grid Lantai*, dan *Indikator Tulang Skeleton*.
4. **Pemutar Animasi Terpadu:**
   - Jika model memuat animasi glTF, dropdown klip menampilkan seluruh daftar track (seperti *Idle*, *Walk*, *Run*).
   - Tombol Play/Pause dan slider scrubber waktu untuk mempratinjau jalannya animasi secara real-time.
   - Pilihan kecepatan putar: `0.5x`, `1.0x`, dan `2.0x`.
5. **Pemeriksaan Hierarki Node & Tulang (Skeleton Tree):**
   - Menelusuri pohon node glTF secara bertingkat dengan label badge khusus untuk `BONE` (kuning emas), `MESH` (cyan), dan `NODE` (abu-abu).
   - Menghitung total sendi tulang rigging karakter secara otomatis.
6. **Normalisasi Skala & Auto-Focus Kamera Saat Spawn:**
   - Bounding Box model dihitung otomatis saat parsing. Jika model terlalu raksasa atau terlalu kecil, engine otomatis menyarankan faktor skala normalisasi proporsional.
   - Saat tombol **"Spawn ke Scene 3D ★"** ditekan, model ditempatkan tepat di atas permukaan tanah ($y = 0$), collider fisika langsung diterapkan, dan kamera utama editor 3D otomatis membidik pusat objek baru sehingga model tidak pernah hilang atau luput dari pandangan.

### Implementasi Source Code:
* **Dialog Importer Godot UI:** `app/src/main/java/com/example/ui/editor/GlbImportPreviewDialog.kt`
* **File Manager & Picker Integration:** `app/src/main/java/com/example/ui/editor/FileManagerView.kt` & `app/src/main/java/com/example/ui/editor/FullScreenFilePickerDialog.kt`
* **Manajer & Integrasi ViewModel:** `app/src/main/java/com/example/viewmodel/EngineViewModel.kt`
  * `viewModel.openGlbInspection(file)`: Membaca file GLB dan memicu pembukaan dialog preview.
  * `viewModel.spawnGlbFromInspection(inspection, colliderShape, customScale, role)`: Men-spawn model ke scene, mengonfigurasi komponen fisika, dan mengunci kamera target ke model baru.
  * `viewModel.spawnMeshAsset(mesh)`: Men-spawn aset mesh terdaftar dari Asset Browser.
* **Parser GLB & Ekstraktor Struktur:** `app/src/main/java/com/example/engine/model/GlbParser.kt`
  * `GlbParser.inspectAndParseGlbFile(file)`: Menghasilkan objek `GlbInspectionResult` lengkap.
  * `GlbParser.decodeGltfInspection()`: Mengekstrak seluruh primitives pada semua sub-mesh, pohon node, array joints/tulang (`skins`), serta samplers/channels animasi glTF 2.0.
  * Penghitungan BoundingBox3D ($min, max, center, size$) dan rasio skala proporsional.

---

## 9. Audio Synthesizer Prosedural Real-time

Untuk menjaga ukuran APK tetap minimal tanpa memerlukan file audio MP3/WAV eksternal, Vortex3D menyematkan synthesizer audio prosedural gelombang PCM 16-bit.

### Efek Suara yang Didukung:
* `"jump"`: Sintesis gelombang sinus modulasi frekuensi naik ($200\text{ Hz} \to 600\text{ Hz}$) untuk efek lompatan.
* `"impact"`: Gelombang bass ($120\text{ Hz}$) dengan campuran noise acak untuk efek tumbukan tabrakan.
* `"laser"`: Modulasi frekuensi tinggi menurun tajam ($900\text{ Hz} \to 200\text{ Hz}$) untuk tembakan laser.
* `"explosion"`: White noise terdistorsi dengan peluruhan amplitudo eksponensial untuk ledakan.
* `"coin"`: Dua nada harmonis berkilau ($987\text{ Hz} \to 1318\text{ Hz}$) untuk pengambilan koin/poin.

### Cara Memanggil dari Skrip Lua:
```lua
engine:play_sound("jump")
engine:play_sound("coin")
```

### Implementasi Source Code:
* **Core File:** `app/src/main/java/com/example/engine/assets/AudioSynthesizer.kt`
* **Fungsi Utama:**
  * `AudioSynthesizer.play(soundName)`: Menggunakan coroutine background untuk membuat instansi `AudioTrack` mode `MODE_STATIC`.
  * `AudioSynthesizer.generateSamples(name)`: Menghasilkan array `ShortArray` 16-bit PCM pada laju sampel 22.050 Hz secara matematis.

---

## 10. Asisten AI Co-Pilot (Gemini Engine Assistant)

Vortex3D mengintegrasikan asisten kecerdasan buatan (*AI Co-Pilot*) untuk membantu Anda membuat skrip game baru, memodifikasi isi scene, atau memperbaiki error logika skrip secara instan.

### Fitur AI yang Tersedia:
1. **Generate Script Baru:** Masukkan prompt seperti *"Buat skrip controller mobil dengan akselerasi halus dan belokan drifting"* -> AI menghasilkan kode Lua siap pakai.
2. **Auto-Fix Script Error:** Jika skrip mengalami runtime error di konsol, ketuk tombol **"Perbaiki dengan AI"** pada log error -> AI mendiagnosis bug dan menyajikan kode perbaikan yang dapat langsung diterapkan.
3. **Modifikasi Scene:** Minta AI untuk menata tata letak arena, menduplikasi rintangan, atau mengatur pencahayaan dunia secara otomatis.

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/AiPromptView.kt`
* **Engine AI File:** `app/src/main/java/com/example/engine/ai/GeminiEngineAssistant.kt`
  * Menggunakan model mutakhir **Gemini 3.1 Pro Preview** (`gemini-3.1-pro-preview`) via REST API OkHttp3.
  * Menginjeksi aturan ketat sistem engine (`ENGINE_API_RULES`) ke dalam system prompt agar AI menghasilkan kode yang 100% kompatibel dengan API Lua Vortex3D.

---

## 11. Konsol Log & Riwayat Debug Engine

Panel konsol menampilkan seluruh output runtime sistem, pesan peringatan, log fisika, dan output cetak (`print` / `engine:log`) dari skrip Lua.

### Fitur Ergonomi Konsol:
* **Tombol Salin Per-Baris:** Ikon clipboard di sebelah kanan setiap baris pesan memungkinkan Anda menyalin teks error atau log tertentu secara instan.
* **Tombol "Salin Semua":** Menyalin seluruh riwayat kejadian engine dengan timestamp ke clipboard perangkat Android.
* **Filter Tag:** Memilah log berdasarkan kategori: *Semua*, *Lua*, *Fisika*, *Engine*, atau *Error*.

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/ConsoleLogView.kt`
* **Fungsi Logika Utama di `EngineViewModel.kt`:**
  * `EngineViewModel.logMessage(tag, message)`: Menambahkan item baru ke `_consoleLogs`.
  * `EngineViewModel.copyLogToClipboard(context, logItem)`: Menyalin satu baris log spesifik.
  * `EngineViewModel.copyAllLogsToClipboard(context)`: Menggabungkan seluruh pesan log dan menyalinnya ke sistem clipboard Android.

---

## 12. Play Mode & Pengujian Interaktif

Ketika tombol **Play (▶)** di toolbar atas ditekan, aplikasi beralih ke mode pengujian penuh (*Play Mode*).

### Kontrol Saat Play Mode:
* **Virtual Joystick Kiri:** Mengontrol gerakan sumbu horizontal ($X$) dan vertikal ($Z$).
* **Tombol Jump:** Memicu aksi impuls lompatan pada entitas pemain aktif.
* **Tombol Action:** Memicu aksi sekunder atau tembakan.
* **Overlay Statistik Performa:**
  * **FPS Nyata (Frames Per Second):** Dihitung menggunakan jendela bergerak (*moving average*) dari waktu delta antar frame nyata.
  * **Latency Frame (ms):** Durasi waktu yang dibutuhkan GPU/CPU untuk merender satu frame.
  * **Total Segitiga & Simpul:** Menampilkan jumlah geometri yang sedang diproses.

### Implementasi Source Code:
* **UI File:** `app/src/main/java/com/example/ui/editor/PlayModeOverlay.kt`
* **Fungsi Logika Utama:**
  * `EngineViewModel.isPlayMode`: Mengontrol visibilitas kontrol overlay game.
  * `EngineViewModel.inputState`: Menyimpan status koordinat joystick dan penekanan tombol.
  * `PhysicsEngine.step(entities, dt)`: Melakukan integrasi gaya gravitasi, pemisahan kontak tumbukan, dan pergerakan bola/kotak secara real-time.

---

## 13. Tabel Rangkuman Pemetaan Fitur ke Source Code

| Fitur Aplikasi | File UI (Compose) | Kelas / File Core Engine | Method & Fungsi Kunci |
| :--- | :--- | :--- | :--- |
| **Project Hub & Template** | `ProjectHubScreen.kt` | `EngineViewModel.kt` | `createNewProject()`, `loadProject()`, `deleteProject()` |
| **Dock Panels Godot Style** | `LandscapeStudioScreen.kt` | `EngineViewModel.kt` | `toggleLeftDock()`, `toggleRightDock()`, `toggleBottomDock()` |
| **Viewport & Multi-Touch** | `Viewport3D.kt` | `Camera3D.kt`, `EngineRenderer.kt` | `orbit()`, `pan()`, `zoom()`, `renderScene()` |
| **Pohon Entitas (Hierarchy)** | `HierarchyView.kt` | `Scene.kt`, `Entity.kt` | `addEntity()`, `removeEntity()`, `duplicateEntity()` |
| **Inspector Komponen** | `InspectorView.kt` | `Component.kt`, `PhysicsEngine.kt` | `updateEntityTransform()`, `attachScriptToEntity()` |
| **Editor Script Lua** | `LuaEditorView.kt` | `LuaInterpreter.kt`, `LuaScriptTemplate.kt` | `executeInit()`, `executeUpdate()`, `executeLine()` |
| **Editor Shader GLSL** | `ShaderEditorView.kt` | `Shader.kt`, `EngineRenderer.kt` | `ShaderProgram`, evaluasi uniform `u_Time` |
| **Inspeksi Model GLB** | `FileManagerView.kt` | `GlbParser.kt`, `AssetManager.kt` | `inspectFile()`, `parseBytes()`, `decodeGltfJson()` |
| **Audio Synthesizer** | N/A (Terintegrasi) | `AudioSynthesizer.kt` | `play("jump")`, `generateSamples()` |
| **AI Co-Pilot Assistant** | `AiPromptView.kt` | `GeminiEngineAssistant.kt` | `queryGenerateScript()`, `queryFixError()` |
| **Konsol Log & Salin Cepat** | `ConsoleLogView.kt` | `EngineViewModel.kt` | `copyLogToClipboard()`, `copyAllLogsToClipboard()` |
| **Simulasi & Play Mode** | `PlayModeOverlay.kt` | `PhysicsEngine.kt`, `EngineViewModel.kt` | `step()`, `pollCollisionEvents()`, `togglePlayMode()` |

---

Kembali ke:
* 🏠 **[Dokumentasi Utama Vortex3D (README.md)](./README.md)**
* 🛠️ **[Panduan Build & Kompilasi APK (PANDUAN_BUILD.md)](./PANDUAN_BUILD.md)**
