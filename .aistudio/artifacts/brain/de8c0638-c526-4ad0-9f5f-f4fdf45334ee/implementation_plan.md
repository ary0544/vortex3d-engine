# Perbaikan Desain UI & Ergonomi Dialog GLB 3D Importer

Rencana restrukturisasi dan polesan visual menyeluruh pada antarmuka dialog pratinjau GLB (`GlbImportPreviewDialog`) untuk memberikan pengalaman yang lega, modern, dan nyaman di perangkat mobile/landscape.

## User Review & Critical Decisions

> [!IMPORTANT]
> Keputusan desain yang telah dikonfirmasi:
> - **Batas Dialog**: Fullscreen edge-to-edge penuh (`usePlatformDefaultWidth = false`, tanpa margin/celah hitam di sekeliling jendela).
> - **Area Pratinjau 3D**: Memperlebar porsi kolom preview (rasio split lebih lapang ~65:35), menyesuaikan skala auto-fit dan jarak kamera awal agar model 3D langsung memenuhi frame secara proporsional.
> - **Kerapian & Konsistensi Tombol**: Menyeragamkan tinggi dan padding semua tombol kontrol (playback animasi & footer), menghilangkan tombol duplikat ("Salin Info Lengkap"), dan menata tata letak kontrol agar simetris dan rapi.

---

## 1. Overview & Core Concept

- **Apa yang Diperbaiki**:
  1. **Eliminasi Celah Dialog**: Menghilangkan padding 6.dp luar dan border tebal yang menyisakan celah hitam di layar; dialog kini membuka secara penuh (edge-to-edge).
  2. **Viewport 3D Lebih Lega & Model Terbingkai Sempurna**:
     - Mengubah bobot kolom kiri dari `1.35f` menjadi `1.85f` (rasio preview lebih dominan dan imersif).
     - Menyetel kalkulasi auto-fit kamera (`fitScale` dan `distanceState`) sehingga model GLB dengan skala apa pun langsung tampil besar dan pas di tengah viewport.
     - Menyediakan tombol cepat *Maximize Preview* (toggle menyembunyikan panel samping saat ingin fokus menginspeksi model 3D).
  3. **Penyeragaman Elemen Tombol**:
     - Memadatkan bilah pemutar animasi dengan tinggi komponen seragam (32.dp), slider yang seimbang, dan badge durasi yang jelas.
     - Menyederhanakan footer aksi dengan menghapus tombol salin ganda dan hanya menampilkan tombol esensial: `Batal`, `Simpan ke Assets`, dan `Spawn ke Scene 3D ★`.
- **Target Pengguna**: Developer game mobile yang membutuhkan proses impor dan inspeksi aset 3D yang cepat, jelas, dan tanpa distraksi UI yang sempit atau bertumpuk.

---

## 2. User Experience & Visual Design

### Alur & Perubahan Tampilan
1. **Header Dialog**:
   - Tampil menyatu di bagian paling atas dengan latar *Dark Slate* (`#0E131C`) dan garis pemisah tipis.
   - Di sisi kanan: Tombol ikon `Salin Info` (kompak dengan tooltip/label ringkas) dan tombol `Close (X)`.
2. **Viewport 3D (Sisi Kiri)**:
   - Area kanvas 3D memanjang dan mengisi sebagian besar layar.
   - Model 3D terpusat dan berukuran optimal tanpa perlu zoom manual berulang kali.
   - Chip filter di sudut atas (Wireframe, Grid, Tulang, Reset) ditata dengan tinggi kompak (26.dp).
3. **Bilah Pemutar Animasi (Animation Bar)**:
   - Ditata dalam kontainer ramping 42.dp (sebelumnya 56.dp yang memakan tinggi kanvas).
   - Tombol Play/Pause (ukuran 32.dp), dropdown pemilih klip animasi (tinggi 32.dp), timeline scrubber slider terintegrasi dengan penunjuk waktu (0.00s / 1.33s), dan toggle kecepatan (0.5x, 1x, 2x) dengan tinggi 32.dp yang presisi.
4. **Panel Informasi & Tab (Sisi Kanan)**:
   - Tab navigasi (*Info Lengkap*, *Pengaturan*, *Hierarki*, *Animasi*) dengan tinggi 34.dp.
   - Konten tabel dan kartu parameter tertata rapi tanpa teks bertabrakan.
5. **Footer Bar**:
   - Sisi kiri: Tombol `Batal` (Outlined, 36.dp).
   - Sisi kanan: Tombol `Simpan ke Assets Saja` (Outlined, 36.dp) dan tombol utama `Spawn ke Scene 3D ★` (Filled Cyan, 36.dp, aksen menonjol).

---

## 3. Key Product Decisions & Trade-Offs

- **Fullscreen Surface vs Dialog Modal**:
  - *Pendekatan*: Menghilangkan outer padding, margin, dan clipping border luar pada `Surface` di dalam `Dialog`. Menggunakan `fillMaxSize()` murni berlatar `#0B0F17`.
  - *Alasan*: Menghilangkan celah hitam kosong di pinggiran layar smartphone landscape dan memberikan ruang vertikal maksimal untuk kanvas 3D.
- **Rasio Split Kolom**:
  - *Pendekatan*: Mengubah rasio dari 57%:43% menjadi 65%:35% (Preview : Info), ditambah tombol ikon maximize/minimize panel samping.
  - *Alasan*: Pengguna ingin memeriksa detail visual mesh, tekstur, dan gerak animasi model secara leluasa.
- **Eliminasi Redundansi Tombol Salin**:
  - *Pendekatan*: Menghapus tombol `Salin Info Lengkap` di footer dan mempertahankannya secara elegan di barisan header atau di dalam tab *Info Lengkap*.
  - *Alasan*: Mengurangi kepadatan tombol footer yang sebelumnya membuat tombol bertumpuk tidak beraturan.

---

## 4. Technical Architecture & Component Structure

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. TOP HEADER (44dp)                                                        │
│ [Icon] Casual_Body • 1.46MB • 6424 Tris               [Salin Info] [Tutup X]│
├─────────────────────────────────────────────────┬───────────────────────────┤
│ 2. LEFT VIEWPORT COLUMN (weight 1.85f)          │ 3. RIGHT TAB COLUMN (1.0f)│
│ ┌─────────────────────────────────────────────┐ │ ┌───────────────────────┐ │
│ │ 3D Canvas Viewport (Auto-fit Model)         │ │ │ [Info] [Set] [Bones]  │ │
│ │ [Wireframe] [Grid] [Tulang]         [Reset] │ │ ├───────────────────────┤ │
│ │                                             │ │ │                       │ │
│ │              [3D MODEL GLB]                 │ │ │ Tab Content Details   │ │
│ │                                             │ │ │                       │ │
│ └─────────────────────────────────────────────┘ │ │                       │ │
│ ┌─────────────────────────────────────────────┐ │ │                       │ │
│ │ Animation Bar: [▶] [Clip ▼] [===O==] [1.0x] │ │ │                       │ │
│ └─────────────────────────────────────────────┘ │ └───────────────────────┘ │
├─────────────────────────────────────────────────┴───────────────────────────┤
│ 4. BOTTOM ACTION BAR (48dp)                                                 │
│ [Batal]                                   [Simpan ke Assets] [Spawn ke Scene]│
└─────────────────────────────────────────────────────────────────────────────┘
```
