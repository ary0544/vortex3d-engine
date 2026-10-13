# 🎮 Dialog Import & Preview Model GLB 3D Interaktif Ala Godot Engine

Rencana implementasi panel inspeksi dan preview 3D komprehensif saat mengimpor file GLB/glTF ke dalam Vortex3D Engine, terinspirasi oleh sistem *Advanced Import Settings* pada Godot Engine 4. Fitur ini menyelesaikan masalah model tidak terlihat di viewport dengan normalisasi skala otomatis, pemusatan kamera, visualisasi hierarki node & tulang (skeleton), serta pemutar animasi interaktif.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan konfirmasi pada sesi tanya-jawab sebelumnya, berikut keputusan desain yang disepakati:

- **Alur Peluncuran Otomatis (Confirmed)**: Saat file `.glb` dipilih di File Manager atau Asset Browser, aplikasi langsung membuka dialog modal interaktif layar penuh (*Godot-style 3D Import Dialog*) tanpa memerlukan navigasi manual tambahan.
- **Normalisasi Skala & Auto-Focus Kamera (Confirmed)**: Setiap model GLB yang di-spawn otomatis dihitung *bounding box*-nya, diskalakan ke ukuran proporsional (tinggi ~2 meter untuk karakter atau prop standar), dan kamera 3D otomatis mengunci target ke objek baru agar tidak ada model yang tertimbun objek lain atau hilang di luar pandangan.
- **Fitur Prioritas Lengkap (Confirmed)**: Dialog dilengkapi viewport mini 3D interaktif mandiri (orbit 1-jari, zoom 2-jari), pemutar animasi dengan timeline slider & klip selector, pohon hierarki node & tulang (Bone/Skeleton Tree), serta pemilih bentuk fisika (*Collider Shape generator*).

---

## 1. Overview & Core Concept

### What It Does
Menghadirkan antarmuka inspeksi dan persiapan aset 3D profesional (*3D Asset Importer & Inspector*) yang secara otomatis aktif ketika pengguna memilih file model `.glb` di penyimpanan perangkat. Pengguna dapat:
1. Memutar dan memeriksa model 3D secara langsung dalam mini-viewport 3D mandiri sebelum memasukkannya ke dalam *Scene*.
2. Memilih dan memutar klip animasi yang tertanam di dalam file GLB (seperti *Idle*, *Walk*, *Run*, *Attack*).
3. Memeriksa hierarki node serta struktur tulang (*Skeleton & Bones*) layaknya panel Scene Tree di Godot.
4. Mengonfigurasi parameter impor: skala, tipe entitas (*Dynamic Actor / Character*, *Static Prop*, *Terrain*), serta bentuk *Collider* fisika (Box, Sphere, Capsule, Mesh).
5. Menekan tombol **"Spawn ke Viewport 3D"** dengan jaminan model muncul tepat di depan kamera viewport utama tanpa terhalang entitas bawaan lain.

### Target Audience & Persona
Pengembang game seluler indie yang mengunduh model 3D dari Sketchfab, Mixamo, atau mengekspor dari Blender/Godot dan ingin mengintegrasikannya ke proyek Vortex3D dengan alur kerja yang intuitif dan bebas rasa bingung.

### Key Value
- **Menghilangkan Bug "Model Tak Terlihat"**: Tidak ada lagi kasus model hanya menampilkan kubus/silinder hijau pengganti karena kegagalan parsing atau model terlalu kecil/raksasa di koordinat tak terduga.
- **Verifikasi Aset Seketika**: Pengembang tahu persis berapa jumlah simpul (vertices), segitiga (triangles), material, dan apakah animasi/tulang terbaca dengan benar sebelum masuk ke tahap scripting atau level design.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌─────────────────────────┐
│ File Manager / Browser  │
│  Tap pada "character.glb│
└────────────┬────────────┘
             │ (Otomatis memicu dialog)
             ▼
┌────────────────────────────────────────────────────────────────────────┐
│  🛸 DIALOG GODOT 3D IMPORT & PREVIEW MODAL                             │
├──────────────────────────────────┬─────────────────────────────────────┤
│  [LEFT PANE: 3D PREVIEW & PLAY]  │  [RIGHT PANE: NODE, BONES & CONFIG] │
│  ┌─────────────────────────────┐ │  • Tab: Node Tree & Skeleton         │
│  │ Mini Orbit Viewport 3D      │ │    ├─ RootNode                      │
│  │ [Shaded / Wireframe Toggle] │ │    │   ├─ Armature (Skeleton)       │
│  │ [Tampilkan Grid & Sumbu]    │ │    │   │   ├─ Hips / Spine / Head   │
│  └─────────────────────────────┘ │    │   └─ Character_Mesh            │
│  ┌─────────────────────────────┐ │  • Tab: Animasi & Klip              │
│  │ [▶ Play] [⏸ Pause] [🔁 Loop]│ │    [Walk_Cycle ▼] [Timeline Slider] │
│  │ Durasi: 0.0s / 1.4s         │ │  • Tab: Konfigurasi Impor           │
│  └─────────────────────────────┘ │    Skala: [1.0x] | Collider: [Capsule]│
├──────────────────────────────────┴─────────────────────────────────────┤
│  [Batal]              [Simpan ke Assets]          [Spawn ke Scene 3D ★]│
└────────────────────────────────────────────────────────────────────────┘
```

1. **Pemicu Alur**: Pengguna memilih file GLB dari *File Manager* atau *Asset Browser*. Dialog "Godot-Style 3D Model Importer" langsung terbuka secara halus.
2. **Interaksi Preview**:
   - Pengguna menyapu jari di viewport preview mini untuk memutar sudut pandang (turntable orbit).
   - Pengguna mencubit dengan dua jari untuk zoom in/out.
   - Pengguna dapat beralih antara mode *Shaded PBR* dan *Wireframe* serta menyalakan indikator tulang.
3. **Pemeriksaan Animasi**:
   - Jika GLB memiliki klip animasi, dropdown klip menampilkan nama klip (misal: "Idle", "Walk", "Run").
   - Tombol Play/Pause dan slider scrubber waktu menggerakkan model dan tulang secara real-time.
4. **Finalisasi & Spawn**:
   - Pengguna memilih bentuk collider (misal: Capsule untuk karakter).
   - Menekan **"Spawn ke Scene 3D"**: Model ditambahkan ke Scene, transform dinormalisasi, kamera utama di-framing ke target baru, dan tab otomatis berpindah ke Viewport 3D.

### Visual Identity & Theme
- **Nuansa & Mood**: *Dark Pro Studio* konsisten dengan tema engine (`#0D1117`, aksen cyan neon `#00E5FF`, aksen amber `#FFB300`, dan deep navy `#161F2E`).
- **Komponen**:
  - Kartu kontainer bergaya flat slate dengan batas tepi halus (1dp `#263040`).
  - Badge info poligon, simpul, dan jumlah tulang beraksen kontras tinggi (`#00E5FF` & `#4CAF50`).
  - Tree node view dengan ikon khusus untuk Node, Mesh, Skeleton, Bone, dan Material.

---

## 3. Key Product Decisions & Trade-Offs

### Keputusan 1: Mini Viewport Render Mandiri vs Viewport Utama
- **Pendekatan Terpilih**: Menjalankan instansi render terisolasi di dalam dialog preview dengan kamera orbit lokal dan pencahayaan studio terkalibrasi.
- **Alasan**: Mencegah perubahan state scene utama saat pengguna hanya ingin memeriksa aset. Pengguna dapat membatalkan tanpa meninggalkan sampah entitas di scene editor.
- **Alternatif**: Me-render di scene utama lalu menghapusnya jika dibatalkan (rawan *undo/redo desync* dan merusak posisi entitas yang sudah ada).

### Keputusan 2: Normalisasi Bounding Box Otomatis
- **Pendekatan Terpilih**: Menghitung titik ekstrem vertex ($minX, maxX, minY, maxY, minZ, maxZ$). Jika ukuran terbesar model melebihi 10 unit atau kurang dari 0.2 unit, engine otomatis mengusulkan faktor skala penyeimbang ($scale = 2.0 / maxDimension$) serta menempatkan pivot dasar di permukaan tanah ($y = 0$).
- **Alasan**: File GLB dari internet sering kali dibuat dalam milimeter (skala 1000x) atau sentimeter (skala 100x), yang menyebabkan model raksasa menutupi seluruh layar atau terlalu mikroskopis sehingga tampak seperti tidak muncul sama sekali.

### Keputusan 3: Penanganan Data Skeleton & Animasi glTF
- **Pendekatan Terpilih**: Menguraikan blok JSON glTF 2.0 untuk `skins` (joints/bones), `nodes` (hierarki pohon), dan `animations` (samplers, channels, translation/rotation/scale keyframes). Struktur tulang dirender sebagai visual wireframe garis sendi jika opsi "Tampilkan Tulang" diaktifkan.
- **Alasan**: Sesuai dengan kapabilitas Godot 4 di mana pengembang dapat melihat nama sendi (misal: *Hips*, *Spine*, *Head*) dan memeriksa apakah rigging model sudah terpasang dengan baik.

---

## 4. Technical Architecture & Data Strategy

### Diagram Arsitektur Komponen

```
┌────────────────────────────────────────────────────────┐
│                   Storage / File Item                  │
└──────────────────────────┬─────────────────────────────┘
                           │ Membaca ByteArray GLB
                           ▼
┌────────────────────────────────────────────────────────┐
│               GlbParser (Enhanced Engine)              │
│  ├─ decodeGltfJson()                                   │
│  ├─ parseNodesAndHierarchy()                           │
│  ├─ parseSkinsAndBones() (Joints & Skeletons)          │
│  ├─ parseAnimations() (Channels, Keyframes & Duration) │
│  └─ calculateBoundingBox() & normalizeScaleFactor()    │
└──────────────────────────┬─────────────────────────────┘
                           │ GlbSceneInspectionData
                           ▼
┌────────────────────────────────────────────────────────┐
│           GlbImportPreviewDialog (Compose UI)          │
│  ├─ Preview3DCanvas (Local Camera3D + Orbit + Draw)   │
│  ├─ AnimationTimelineBar (Play/Pause, Scrubber)        │
│  ├─ HierarchyTreeExplorer (Nodes, Meshes, Bones)       │
│  └─ ImportSettingsForm (Scale, Collision, Role)        │
└──────────────────────────┬─────────────────────────────┘
                           │ Konfirmasi Spawn
                           ▼
┌────────────────────────────────────────────────────────┐
│                   EngineViewModel                      │
│  ├─ spawnGlbWithNormalizedTransform(result, config)    │
│  ├─ camera.frameTarget(entityPosition, entityRadius)   │
│  └─ setWorkspaceTab(SCENE_VIEW)                        │
└────────────────────────────────────────────────────────┘
```

### Model Data Baru

1. **`GlbNodeInfo`**:
   - `id`: Int / String
   - `name`: String
   - `type`: `NODE`, `MESH`, `SKIN_BONE`
   - `children`: List<GlbNodeInfo>
   - `translation`: Vec3, `rotation`: Vec3, `scale`: Vec3

2. **`GlbAnimationTrack`**:
   - `name`: String
   - `durationSec`: Float
   - `targetNode`: String
   - `property`: String ("translation" / "rotation" / "scale")

3. **`GlbAnimationInfo`**:
   - `index`: Int
   - `name`: String
   - `duration`: Float
   - `channelsCount`: Int

4. **`GlbInspectionResult`**:
   - `mesh`: Mesh
   - `material`: Material
   - `boundingBox`: BoundingBox3D (min, max, center, size)
   - `suggestedScale`: Float
   - `rootNodes`: List<GlbNodeInfo>
   - `boneCount`: Int
   - `animations`: List<GlbAnimationInfo>
   - `rawMetadata`: GlbMetadata

### Alur Implementasi
1. **Engine Model (`GlbParser.kt`)**:
   - Tambahkan ekstraksi lengkap untuk `nodes`, `skins`, dan `animations`.
   - Hitung Bounding Box ($min, max$) dari kumpulan vertex `POSITION`.
   - Hitung skala normalisasi otomatis agar model pas di unit ruang dunia engine (~2.0 unit).
2. **Komponen UI (`GlbImportPreviewDialog.kt`)**:
   - Buat dialog modal landscape komprehensif yang membagi layar menjadi 2 kolom (Kiri: 3D Viewport Orbit Preview + Pemutar Animasi; Kanan: Tab Node Tree, Tulang, Metadata Tekstur, dan Form Konfigurasi Spawn).
   - Sediakan kontrol orbit turntable 1-jari dan pinch zoom di mini-viewport preview.
3. **Integrasi File Picker & Browser (`FileManagerView.kt` & `FullScreenFilePickerDialog.kt`)**:
   - Saat file berekstensi `.glb` diklik, langsung munculkan `GlbImportPreviewDialog`.
4. **Logika Spawn & Framing Kamera (`EngineViewModel.kt`)**:
   - Implementasikan fungsi `spawnGlbWithFraming()` yang memposisikan model baru di atas tanah ($y = 0$), menerapkan collider pilihan (Box/Sphere/Capsule), dan memposisikan kamera editor agar langsung membidik pusat model baru.
