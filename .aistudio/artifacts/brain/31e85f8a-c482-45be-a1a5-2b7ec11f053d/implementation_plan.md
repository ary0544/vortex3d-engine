# Rencana Perbaikan: Optimasi Viewport 3D, Real FPS Statistic, & Redesain File Manager Landscape

Rencana komprehensif untuk mengatasi masalah viewport lambat/stuttering pada pergerakan kamera, mengganti metrik FPS palsu dengan pengukuran frame time nyata, serta merombak layout File Manager agar area daftar file luas dan nyaman digulir pada orientasi landscape.

## User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan masukan dan konfirmasi Anda:
> - **File Manager Landscape**: Menghapus tumpukan 4 baris header yang memakan 70% tinggi layar ponsel. Menggantinya dengan **toolbar kompak 1 baris** yang memuat breadcrumb, aksi cepat, dan tombol toggle lipat (collapsible) untuk bilah cari/filter. Dengan ini, area daftar file mendapatkan lebih dari 75% tinggi layar sehingga leluasa dan tidak sempit lagi saat di-scroll.
> - **Statistik FPS Nyata & Latency**: Menghapus angka hardcoded `fps = 60` yang tidak akurat. Mengimplementasikan kalkulator FPS rolling-average berbasis `System.nanoTime()` yang mengukur waktu render frame sebenarnya (menampilkan FPS nyata dan durasi render dalam milidetik, misal `59 FPS • 16.8 ms`).
> - **Optimasi Pipeline 3D (Backface Culling)**: Mengaktifkan culling segitiga 2D di ruang layar (*screen-space winding check*). Segitiga yang membelakangi kamera diabaikan sebelum tahap kalkulasi pencahayaan, depth-sorting, dan canvas path drawing—mengurangi beban CPU dan memori hingga ~50%.

---

## 1. Overview & Core Concept

- **Masalah Saat Ini**:
  1. Header File Manager menumpuk vertikal (Judul + Baris Shortcut + Breadcrumb + Input Search + Kategori Filter) sehingga pada layar HP landscape (tinggi ~360-400dp), hanya tersisa ~80-100dp untuk daftar file (hanya terlihat 1-1.5 baris file).
  2. Angka `fps = 60` pada statistik renderer bersifat statis (hardcoded), sehingga pengguna tidak mendapatkan informasi performa yang valid.
  3. Canvas software rendering memproses, mengurutkan (*depth-sorting*), dan menggambar seluruh poligon termasuk sisi belakang objek yang tidak terlihat, memicu penurunan fluiditas saat kamera digeser.
- **Solusi yang Dibangun**:
  - **File Manager Collapsible Single-Row**: Toolbar ringkas dan adaptif dengan tombol toggle pencarian/filter opsional, memaksimalkan tinggi kontainer daftar file.
  - **Real FPS Engine**: Stopwatch frame-to-frame dengan perataan bergerak (rolling buffer 20 frame) untuk menghitung FPS riil dan frame time ms yang jujur.
  - **Screen-Space Backface Culling & Event Smoothing**: Melewati pemrosesan poligon yang tidak menghadap ke depan layar pada objek solid.

---

## 2. User Experience & Visual Design

### Alur Interaksi Pengguna (User Flow)
1. **Navigasi File yang Lega di Mode Landscape**:
   - Saat membuka dock "Files" maupun dialog "File Explorer & Asset Importer", pengguna langsung melihat daftar file yang luas (bisa menampilkan 5-7 item sekaligus tanpa terhimpit).
   - Jalur folder (breadcrumb) diringkas secara elegan dengan tombol kembali cepat.
   - Bilah pencarian dan filter tipe file dapat dimunculkan/disembunyikan hanya saat dibutuhkan melalui tombol ikon praktis.
2. **Indikator Statistik Jujur & Informatif**:
   - Badge performa di sudut kanan atas menampilkan:
     - `FPS Nyata` (berwarna hijau jika >= 50 FPS, kuning jika 30-49 FPS, oranye jika < 30 FPS).
     - `Frame Time` (misal `16.4 ms`).
     - `Jumlah Tris Aktif` (menampilkan jumlah segitiga yang benar-benar digambar setelah proses culling).

---

## 3. Key Product Decisions & Trade-Offs

- **Desain Header File Manager: Collapsible Toolbar vs Fixed Multi-Row**:
  - *Keputusan*: Menggabungkan breadcrumb, shortcut penting, dan tombol expand filter ke dalam satu baris horizontal kompak (`height = 36.dp`). Filter chip dan input search hanya muncul saat ditekan atau digulir.
  - *Manfaat*: Menambah area pandang daftar file dari semula ~30% menjadi ~80% dari total tinggi layar.
- **Backface Culling pada Software Renderer**:
  - *Keputusan*: Menghitung tanda *cross-product* determinan 2D dari titik `(p0, p1, p2)` di layar:
    `val cross = (p1X - p0X) * (p2Y - p0Y) - (p1Y - p0Y) * (p2X - p0X)`
    Jika `cross <= 0f` pada material solid, segitiga langsung diabaikan. Untuk mode wireframe, kedua sisi tetap digambar.
  - *Manfaat*: Menghilangkan pemborosan alokasi, sorting, dan path drawing untuk 50% segitiga pada kubus, bola, silinder, dan model 3D lainnya.

---

## 4. Technical Architecture & Implementation Steps

```
┌─────────────────────────────────────────────────────────────┐
│                 EngineRenderer (Optimized)                  │
│                                                             │
│   Project Vertices (Mat4 Transformation)                    │
│   ▼                                                         │
│   Screen-Space Backface Culling Check (cross <= 0)          │
│   ├─ True (Backfacing & Solid) ──► DISCARD (Skip shading,   │
│   │                                pooling, & sorting)      │
│   └─ False (Visible / Front)   ──► Shading & Pool           │
│   ▼                                                         │
│   Painter's Algorithm Depth Sort (Hanya 50% Segitiga)       │
│   ▼                                                         │
│   Real Frame Time & FPS Calculation (System.nanoTime())     │
└─────────────────────────────────────────────────────────────┘
                               ▲
┌──────────────────────────────┴──────────────────────────────┐
│                    FileManagerView & Dialog                 │
│  - Single-row Compact Header with Collapsible Search/Filter │
│  - Maximized LazyColumn Scroll Area                         │
└─────────────────────────────────────────────────────────────┘
```

1. **`EngineRenderer.kt`**:
   - Tambahkan 2D Backface Culling pada loop segitiga.
   - Tambahkan tracking frame start/end time dan rolling average FPS & frame latency.
2. **`EngineViewModel.kt`**:
   - Perbarui model `RenderStats` untuk menyertakan `frameTimeMs: Float`.
3. **`Viewport3D.kt`**:
   - Perbarui `RenderStatsBadge` untuk menampilkan FPS riil dan frame latency milidetik secara estetik.
4. **`FileManagerView.kt` & `FullScreenFilePickerDialog.kt`**:
   - Restrukturisasi header menjadi baris tunggal yang ramping dengan expand/collapse toggle untuk search dan filter, memperluas area scroll file.
