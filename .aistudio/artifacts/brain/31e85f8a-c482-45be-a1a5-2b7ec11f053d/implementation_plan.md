# Godot 4 Mobile UI Refinement & Density Optimization Plan

Perapihan menyeluruh antarmuka Vortex3D Mobile Engine sesuai preferensi pengguna: merampingkan kontainer tombol dan toolbar utama menjadi kompak (tinggi 32dp-36dp), merapatkan padding panel dock (6dp-8dp) demi memaksimalkan ruang kerja viewport 3D, serta menghadirkan desain kartu proyek yang modern dengan border tipis dan tombol aksi ringkas di Project Hub.

## User Review & Critical Decisions

> [!IMPORTANT]
> Hasil konfirmasi preferensi UI dari pengguna:
> - **Toolbar & Tombol Editor**: Dibuat kompak dan minimalis ala Godot 4 mobile (tinggi tombol/toolbar 32dp - 36dp), menghilangkan kesan kontainer yang terlalu besar/bulky.
> - **Padding & Spacing Panel Dock**: Diperapat ke rentang 6dp - 8dp pada Scene Tree, Inspector, dan Asset Manager agar area kerja 3D Viewport jauh lebih lega dan maksimal.
> - **Project Hub Cards**: Desain kartu proyek menggunakan border tipis (`1.dp`), sudut melingkar proporsional (`10.dp`), dan tombol aksi ringkas di sebelah kanan.

---

## 1. Overview & Core Concept

- **Apa yang Diperbaiki**: Kepadatan elemen UI (*UI Density*) di seluruh layar editor (`LandscapeStudioScreen`, `MainStudioScreen`, `ProjectHubScreen`, dan view dock terkait) agar terasa seperti software desktop profesional (Godot Engine 4) di perangkat mobile.
- **Nilai Utama**: Ruang pandang 3D yang lebih luas, navigasi tombol yang tidak memakan tempat berlebih, serta konsistensi estetika gelap profesional (`#0D1117`, `#161B24`, aksen `#478CBF` dan `#00E5FF`).

---

## 2. User Experience & Visual Design

### 1. Project Hub (Lobby) UI Refinement
- **Kartu Proyek (`ProjectCard`)**:
  - Tinggi card dan padding internal dirampingkan menjadi compact (`10.dp` padding).
  - Thumbnail box diperkecil menjadi `44.dp x 44.dp`.
  - Tombol aksi (Play, Edit, Duplicate, Delete) menggunakan ukuran icon kecil dan button compact (`height = 32.dp`).
  - Badge orientasi dan status aktif didesain rapi berdampingan dengan jumlah nodes dan tanggal modifikasi.

### 2. Editor Toolbar & Top Bar (Landscape & Portrait Studio)
- **Top Bar**:
  - Tinggi top bar diperkecil dengan padding horizontal/vertikal minimalis.
  - Tombol-tombol aksi utama (Simulation Play/Pause, Reset, Wireframe, Fullscreen) menggunakan ukuran icon compact (`18.dp - 20.dp`) di dalam kontainer `32.dp`.
- **Panel Dock (Kiri, Kanan, Bawah)**:
  - Spacing antar item list dan tree nodes dirapatkan ke `6.dp` - `8dp`.
  - Tab header di dock menggunakan font `11.sp` dan padding tipis.

---

## 3. Key Product Decisions & Trade-Offs

- **Compact Touch Targets (Godot 4 Mobile Ergonomics)**:
  - *Pendekatan*: Menyesuaikan tinggi tombol toolbar dan item list menjadi 32dp–36dp, mempertahankan kenyamanan sentuh mobile namun menghilangkan kesan "terlalu besar/boros tempat".
- **Density Optimization**:
  - *Pendekatan*: Mengurangi padding yang berlebihan di sekeliling card dan tab, memberikan ekstra ruang bagi canvas 3D Viewport.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   LandscapeStudioScreen                │
│    (Compact 32-36dp Top Bar & Tight Dock Padding 6-8dp)│
└───────────────▲────────────────────────▲───────────────┘
                │                        │
       Rapat & Kompak           Lebih Luas untuk 3D
                │                        │
┌───────────────┴────────┐      ┌────────▼───────────────┐
│     ProjectHubScreen   │      │       Viewport3D       │
│ (Modern Thin-Bordered  │      │ (Full screen view with │
│     Project Cards)     │      │   compact overlay HUD) │
└────────────────────────┘      └────────────────────────┘
```

### Rincian Perubahan Kode:
1. **`ProjectHubScreen.kt`**:
   - Merampingkan padding dan ukuran elemen pada `ProjectCard` dan template cards agar lebih ringkas dan modern.
2. **`LandscapeStudioScreen.kt` & `MainActivity.kt` (`MainStudioScreen`)**:
   - Memperkecil tinggi Top Bar dan kontainer tombol aksi/toolbar menjadi kompak (32dp - 36dp).
3. **`HierarchyView.kt`, `InspectorView.kt`, `AssetBrowserView.kt`, `FileManagerView.kt`**:
   - Merapatkan spacing antar baris item menjadi `6.dp` - `8.dp`.
4. **Verifikasi**:
   - Jalankan `compile_applet` untuk memastikan seluruh perubahan terkompilasi bersih tanpa error.
