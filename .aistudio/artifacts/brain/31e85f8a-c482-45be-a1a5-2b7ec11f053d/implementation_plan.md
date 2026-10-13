# Rencana Optimasi Performa 3D Viewport & Redesain UI File Manager

Solusi menyeluruh untuk mengatasi keluhan lag/lambat pada render 3D viewport, memperluas area pemilihan file dengan Dialog Pop-up Layar Penuh (menghilangkan kendala celah pemilihan yang sempit), serta merapikan header tab dock kiri dan kanan dengan ikon ringkas tanpa teks terpotong.

## User Review & Critical Decisions

> [!IMPORTANT]
> Keputusan yang dikonfirmasi oleh pengguna:
> - **File Manager / Pemilihan File**: Menghadirkan **Dialog Pop-up Layar Penuh** khusus untuk penjelajahan dan pemilihan file (.glb, .obj, .lua, audio, shaders) dengan layout kartu yang lega, preview jelas, dan tombol aksi yang nyaman disentuh.
> - **Optimasi Performa**: Memprioritaskan **Render 3D Viewport dan Respon Sentuhan Orbit Kamera** dengan mengeliminasi alokasi objek per-frame (zero-allocation rendering buffer), mengurangi recomposition berlebih saat dragging, serta memperhalus render loop.
> - **Penataan Tab Dock**: Menggunakan **Tab Ikon Ringkas Berlebar Fleksibel** pada dock kiri dan kanan, mencegah teks terpotong dan menjaga tampilan tetap rapi serta simetris.

---

## 1. Overview & Core Concept

- **Tujuan**: Mengubah pengalaman editor mobile Vortex3D menjadi responsif (60 FPS stabil tanpa stuttering saat orbit kamera) dan nyaman digunakan untuk manipulasi file serta navigasi tab.
- **Masalah Pengguna Saat Ini**:
  1. Area daftar file pada tab file manager dock terlalu sempit dan terhimpit oleh search bar & tombol preset, menyulitkan pengguna menyentuh file yang diinginkan.
  2. Perputaran kamera 3D terasa berat dan lambat akibat alokasi memori ribuan objek per detik dalam loop Canvas rendering.
  3. Label pada tab dock kanan dan tombol aksi asset manager mengalami pemotongan teks akibat lebar kontainer dock yang terbatas.

---

## 2. User Experience & Visual Design

### 1. Dialog Pemilihan File Layar Penuh (Full-Screen File Picker Modal)
- **Akses Langsung**: Tombol mencolok *"Buka File Manager Layar Penuh"* pada dock kiri dan header Asset Manager.
- **Header Modal**:
  - Tombol Kembali / Navigasi Folder Induk yang jelas (`40dp` touch target).
  - Breadcrumb path aktif (contoh: `/storage/emulated/0/Download`).
  - Bar pencarian terintegrasi dengan aksi clear filter.
  - Tombol Tutup dialog yang mudah diakses.
- **Area Daftar File Luas & Nyaman**:
  - Tampilan kartu file modern dengan tinggi nyaman (`64dp`), ikon format berwarna (GLB: Cyan, LUA: Kuning, Audio: Oranye, Shaders: Hijau).
  - Informasi ukuran file dan tanggal modifikasi yang terbaca jelas.
  - Tombol aksi instan: *"Spawn ke Scene"*, *"Impor ke Assets"*, dan *"Buka Editor"*.

### 2. Tab Ikon Ringkas Dock Kiri & Kanan (Anti Teks Terpotong)
- **Left Dock Tabs (Scene, Assets, Files)**:
  - Tab ringkas dengan ikon proporsional (`18dp`) dan label fleksibel auto-fit.
  - Perbaikan tombol `+ Impor .GLB` pada header Asset Manager agar tidak terpotong (menggunakan ikon tambah ringkas dan teks fleksibel).
- **Right Dock Tabs (Inspector, Lua, Shaders, AI Fix, Docs)**:
  - Tab bergaya compact pill indicator dengan ikon representatif dan label ringkas tanpa min-width berlebih, sehingga seluruh tab muat rapi tanpa terpotong.

### 3. Responsivitas 3D Viewport & Gesture Orbit
- **Zero-Allocation Render Pipeline**:
  - Pre-alokasi static buffer untuk proyeksi verteks dan segitiga di `EngineRenderer.kt` guna menghindari garbage collection freeze saat mengorbit kamera.
  - Throttling dan penyatuan state pembaruan kamera untuk mencegah recomposition Compose yang tidak perlu pada overlay UI.
  - Respon sentuhan drag orbit yang instan, mulus, dan presisi.

---

## 3. Key Product Decisions & Trade-Offs

- **Full-Screen Dialog vs Narrow Dock File List**:
  - *Pendekatan*: Menampilkan dialog modal layar penuh ketika pengguna ingin memilih atau mengimpor file. Dock kiri tetap memiliki pratinjau cepat, namun aksi pemilihan utama dilakukan di layar penuh dengan ruang sentuh maksimal.
  - *Alasan*: Layar mobile landscape memiliki keterbatasan tinggi vertikal pada dock samping. Layar penuh memberikan 100% ruang pandang untuk melihat nama file panjang dan detail metadata.
- **Reusable Primitive Buffers vs Per-Frame Object Instantiation**:
  - *Pendekatan*: Menggunakan array primitif reusable (`FloatArray`) untuk transformasi 3D dan proyeksi layar alih-alih membuat ribuan objek data class per frame.
  - *Alasan*: Mengeliminasi pemicu GC (Garbage Collection) Android yang menjadi penyebab utama keluhan "masih sangat lambat".

---

## 4. Technical Architecture & Component Flow

```
┌───────────────────────────────────────────────────────────────┐
│                    LandscapeStudioScreen                      │
│   ┌─────────────────────┬───────────────────────┬──────────┐  │
│   │   Left Dock         │      Viewport3D       │Right Dock│  │
│   │ [Scene|Assets|Files]│  (Zero-GC 3D Orbit)   │[Insp|Lua]│  │
│   │                     │                       │          │  │
│   │  Quick Asset Hub    │  Fast Touch Response  │Inspector │  │
│   └──────────┬──────────┴───────────────────────┴──────────┘  │
│              │                                                │
│              ▼ Klik "Pilih File"                              │
│   ┌────────────────────────────────────────────────────────┐  │
│   │        FullScreenFilePickerDialog (Layar Penuh)        │  │
│   │  • Breadcrumb Path & Search Bar Nyaman                 │  │
│   │  • List Card Luas (Touch target 64dp, no squishing)    │  │
│   │  • Aksi Langsung: Spawn 3D / Import Asset / Edit       │  │
│   └────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
```

### Rencana Langkah Perubahan Kode:
1. **`FullScreenFilePickerDialog.kt` & `FileManagerView.kt`**:
   - Membuat komponen dialog layar penuh khusus untuk penjelajahan dan pemilihan file dengan touch target lega dan aksi jelas.
   - Mengintegrasikan dialog ini pada tab Files dan tombol impor Asset Manager.
2. **`EngineRenderer.kt` & `Viewport3D.kt`**:
   - Mengganti alokasi per-frame dengan reusable float buffer untuk verteks, normal, dan segitiga terproyeksi.
   - Mengoptimalkan gesture `pointerInput` drag orbit kamera agar rendering instan tanpa stuttering GC.
3. **`LandscapeStudioScreen.kt` & `AssetBrowserView.kt`**:
   - Menata ulang tab header dock kiri dan kanan menjadi tab ikon fleksibel yang tidak memotong teks.
   - Merapikan header Asset Manager agar tombol impor tidak terdesak.
4. **Verifikasi & Build**:
   - Menjalankan `compile_applet` untuk memastikan seluruh komponen terbangun dengan sempurna tanpa error.
