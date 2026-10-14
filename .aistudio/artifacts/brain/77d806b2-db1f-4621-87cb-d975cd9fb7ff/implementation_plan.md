# Penyempurnaan Tekstur UV Atlas, Multi-Material GLB, Optimasi Performa, & Perapihan UI

Rencana penyempurnaan komprehensif pada visualisasi 3D GLB di Vortex3D Engine: menampilkan tekstur asli dan warna multi-material secara akurat (mengeliminasi tampilan warna abu-abu polos), akselerasi performa rendering untuk model berpoligon tinggi (hingga belasan ribu vertices), merapikan tata letak UI header bar dan kontrol kamera yang padat, serta memastikan stabilitas build project.

## User Review & Critical Decisions

> [!IMPORTANT]
> Keputusan penting yang telah dikonfirmasi oleh pengguna:
> - **Tekstur & Warna GLB**: Menerapkan sampling tekstur UV atlas per-segitiga dari bitmap tertanam serta dukungan warna albedo multi-material per submesh.
> - **Optimasi Performa**: Menggunakan cache proyeksi vertex dan adaptive Level of Detail (LOD) cerdas saat mengorbit model berpoligon tinggi (>3.000 triangles) agar tetap lancar 60 FPS tanpa lag.
> - **Perapihan UI**: Merapikan header bar atas agar tidak bertumpuk/terpotong dan mengelompokkan tombol kontrol kamera ke dalam toolbar yang ergonomis dan elegan.
> - **Restorasi .env**: Mengembalikan konfigurasi `.env` dengan placeholder `GEMINI_API_KEY=MY_GEMINI_API_KEY` agar Secrets Gradle Plugin tidak menghasilkan error sintaks Java saat kompilasi.

- **Confirmed Decision 1**: Parser GLB (`GlbParser.kt`) akan mengekstrak seluruh palet material (`materials`) dan koordinat UV (`TEXCOORD_0`) per primitive, sehingga model 3D (seperti karakter Quaternius pada screenshot pengguna) menampilkan warna rambut, kulit, pakaian, dan sepatu asli dari tekstur atlas, bukan satu warna abu-abu polos.
- **Confirmed Decision 2**: Rendering kanvas pada `GlbImportPreviewDialog.kt` akan melakukan sampling warna tekstur langsung dari `textureBitmap` menggunakan koordinat UV segitiga, dipadukan dengan pencahayaan directional Lambertian untuk menghasilkan visual 3D yang hidup dan tajam.
- **Confirmed Decision 3**: Menyederhanakan header bar dan toolbar kontrol kamera menjadi chip segmented yang rapi, memisahkan scrubber animasi dengan layout yang bersih, dan memaksimalkan ruang pandang kanvas 3D.

---

## 1. Overview & Core Concept

- **What It Does**: Membawa visualisasi GLB dari sekadar wireframe/clay abu-abu menjadi render 3D penuh warna dengan tekstur atlas embedded asli, multi-material shading, dan performa tinggi yang responsif saat diputar dengan gestur sentuh.
- **Target Audience / Persona**: Kreator game yang mengimpor karakter 3D GLB (Quaternius, Kenney, Mixamo, Blender) ke dalam HP/tablet Android dan ingin melihat model persis seperti tampilan aslinya.
- **Key Value**: Model karakter dan prop 3D tampil dengan visual detail, warna kulit/kostum akurat, tidak lag saat rotasi, dan UI bersih tanpa elemen visual yang bertumpuk.

---

## 2. User Experience & Visual Design

### Key Improvements

1. **Warna & Tekstur Asli Model 3D**:
   - Model karakter wanita (pada screenshot `Animated Woman by Quaternius`) akan langsung menampilkan warna kulit, rambut cokelat/pirang, baju/celana warna-warni dari tekstur atlas bawaan GLB.
   - Model non-tekstur dengan multi-material (misalnya mobil dengan kaca transparan, ban hitam, bodi merah) akan menampilkan warna masing-masing komponen submesh secara tepat.
2. **Performa Halus (Zero Stutter Rotation)**:
   - Saat pengguna memutar (orbit) atau zoom model dengan 12.000+ vertices, render loop memanfaatkan cache proyeksi titik vertex dan adaptive LOD sehingga framerate stabil dan tidak terasa berat.
3. **Perapihan Header & Toolbar Kontrol Kamera**:
   - **Header Bar**: Nama model, ukuran file, jumlah polygon, dan jumlah tulang tertata dalam baris rapi dengan badge ringkas tanpa teks berhimpitan.
   - **Kamera & Tampilan Toolbar**: Menggabungkan tombol presets sudut pandang (*Depan, Atas, Samping, Iso, Reset*) ke dalam segmented group yang kompak. Tombol *Wireframe*, *Grid*, dan *Tulang* menggunakan chip toggle beraksen neon yang jelas.
   - **Animasi Bar**: Scrubber timeline animasi ditata rapi dengan indikator waktu detik dan speed selector (0.5x, 1.0x, 2.0x).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Per-Triangle UV Texture Sampling**:
  - *Chosen Approach*: Menghitung titik tengah UV segitiga `(u0+u1+u2)/3, (v0+v1+v2)/3`, lalu mengambil warna pixel dari `textureBitmap` terurai.
  - *Why*: Sangat cepat (O(1) memory lookup), kompatibel 100% dengan software canvas Android, dan ideal untuk model game bertekstur atlas (palette texture) seperti Quaternius/Synty.
  - *Alternatives Considered*: Menggunakan OpenGL ES Texture Shader penuh — ditolak karena memerlukan GLSurfaceView terpisah yang lebih lambat diinisialisasi dalam dialog Compose.
- **Decision 2: Adaptive LOD Saat Rotasi Cepat**:
  - *Chosen Approach*: Jika model memiliki > 3.000 segitiga dan kamera sedang diputar secara aktif (`isInteracting == true`), render menggunakan stride adaptif atau prioritas segitiga besar, lalu langsung merender 100% detail penuh saat jari dilepas.
  - *Why*: Menjamin interaksi gestur sentuh selalu instan dan responsif di berbagai spesifikasi prosesor ponsel.

---

## 4. Technical Architecture & Implementation Steps

```
┌────────────────────────────────────────────────────────┐
│                      GlbParser                         │
│  - Extract ALL Materials & baseColorFactors            │
│  - Extract TEXCOORD_0 (u, v) for every vertex          │
│  - Decode Embedded Images (PNG/JPG) to Bitmap          │
│  - Map Material Index & Colors to Triangles            │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│               GlbImportPreviewDialog                   │
│   ┌────────────────────────────────────────────────┐   │
│   │ Neatly Arranged Header Bar & Stats Badges      │   │
│   └──────────────────────┬─────────────────────────┘   │
│                          │                             │
│         ┌────────────────┴───────────────┐             │
│         ▼                                ▼             │
│  [Viewport 3D with Texture]     [Clean Segmented Docks]│
│  - UV Pixel Color Sampling      - Camera View Presets  │
│  - Backface Culling (2D Cross)  - Compact Filter Chips │
│  - Vertex Projection Cache      - Tab Hierarchy/Info   │
│  - Adaptive LOD on Touch Drag   - Sleek Scrubber Bar   │
└────────────────────────────────────────────────────────┘
```

### Rencana File yang Dimodifikasi:
1. `/.env`: Dibuat kembali dengan `GEMINI_API_KEY=MY_GEMINI_API_KEY`.
2. `app/src/main/java/com/example/engine/model/GlbParser.kt`:
   - Menyimpan warna albedo per-submesh / per-primitive.
   - Mengaitkan koordinat UV per-vertex secara utuh.
   - Menghubungkan material index ke daftar segitiga (*Triangle material mapping*).
3. `app/src/main/java/com/example/ui/editor/GlbImportPreviewDialog.kt`:
   - Implementasi sampling warna tekstur UV atlas dari `inspection.textureBitmap` per segitiga.
   - Cache proyeksi vertex `projX`, `projY`, `projZ` dan adaptive LOD saat gestur sentuh.
   - Merapikan header bar, segmented camera controls, dan timeline scrubber.
