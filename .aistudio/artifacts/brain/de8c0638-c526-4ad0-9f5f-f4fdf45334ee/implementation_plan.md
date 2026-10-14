# Rencana Solusi: Kontrol Multi-Touch & Solid Textured GLB Rendering

Solusi komprehensif untuk mengaktifkan kembali kontrol **1-jari (Orbit Rotasi)** berdampingan dengan **2-jari (Pan & Pinch-Zoom)**, serta memperbaiki masalah visual model GLB yang tampak seperti **noise/titik** dengan **ekstraksi tekstur asli GLB** dan **rendering solid mesh penuh (Depth Sorting)**.

---

## Ringkasan Keputusan Terkonfirmasi

> [!IMPORTANT]
> Berdasarkan jawaban klarifikasi pengguna:
> 1. **Alokasi Gestur Sentuh**: 
>    - **1 Jari**: Orbit rotasi memutar model 3D (turntable inspection).
>    - **2 Jari**: Pan (geser posisi) dan pinch (perbesar/perkecil jarak kamera).
> 2. **Solusi Visual Model GLB**:
>    - Ekstrak tekstur gambar embedded (PNG/JPEG) dari binary chunk GLB.
>    - Gambar mesh secara utuh (solid 100% tanpa lompat titik `triStep`), dilengkapi sorting kedalaman (Painter's depth sorting) agar tidak berlubang atau tampak seperti noise titik.

---

## 1. Analisis Akar Masalah (Root Cause Analysis)

### Mengapa Beberapa GLB Tampak Hanya Titik & Noise?
1. **Lompat Segitiga yang Agresif (`triStep`)**:
   - Pada baris kode sebelumnya:
     `val triStep = if (triCount > 1500) maxOf(1, triCount / 1200) else 1`
   - Pada model dengan 15.000–60.000 poligon, algoritma melewatkan hingga 90%–98% segitiga. Akibatnya, permukaan solid terpecah menjadi segitiga terisolasi kecil yang tampak seperti sekumpulan titik atau noise acak.
2. **Tekstur Tidak Dimuat**:
   - `GlbParser.kt` sebelumnya hanya membaca nilai numerik `baseColorFactor` (yang pada banyak model glTF bernilai putih/abu-abu `[1, 1, 1, 1]` karena mengandalkan file tekstur).
   - Array `images` dan buffer gambar PNG/JPEG di dalam file binary GLB belum diekstrak menjadi `Bitmap` Android.
3. **Ketiadaan Depth Sorting (Z-Fighting/Transparansi Parsial)**:
   - Canvas 2D menggambar poligon tanpa Z-buffer perangkat keras, sehingga poligon belakang sering menimpa poligon depan secara acak jika urutan rendering tidak diurutkan dari belakang ke depan (Far-to-Near).

---

## 2. Alur Arsitektur & Rendering

```
┌─────────────────────────────────────────────────────────────┐
│                       GLB Binary File                       │
│  (Header 12B) -> JSON Chunk -> Binary Buffer (BIN Chunk)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
     [Geometry Extraction]           [Texture Extraction]
     - POSITION (Vec3)               - images[] (PNG / JPEG)
     - NORMAL (Vec3)                 - bufferView byteOffset/Length
     - TEXCOORD_0 (UV)               - BitmapFactory.decodeByteArray()
     - INDICES (Triangles)                           │
               │                                     ▼
               │                           Bitmap Texture 
               ▼                                     │
     100% Full Mesh Triangles                        │
               │                                     │
               └───────────────┬─────────────────────┘
                               ▼
            ┌──────────────────────────────────────┐
            │       Depth-Sorted 3D Canvas         │
            │  1. Proyeksi NDC (MVP Matrix)        │
            │  2. Hitung Z Rata-Rata Segitiga      │
            │  3. Urutkan Segitiga (Far -> Near)   │
            │  4. Shading Solid + Sample Warna     │
            │     Tekstur Asli + Pencahayaan Diffus│
            └──────────────────────────────────────┘
```

---

## 3. Rencana Komponen & File yang Dimodifikasi

### A. Ekstraksi Tekstur & Parser GLB (`GlbParser.kt`)
1. **Ekstrak Buffer Gambar Embedded**:
   - Baca array `images` di glTF JSON.
   - Dapatkan `bufferView` yang ditunjuk gambar (PNG/JPEG) beserta `byteOffset` dan `byteLength` di BIN buffer.
   - Dekode buffer byte menggunakan `BitmapFactory.decodeByteArray(...)` menjadi `Bitmap` Android.
2. **Model Data `GlbInspectionResult` & `GlbMaterialInfo`**:
   - Tambahkan properti `val textureBitmap: Bitmap?` dan `val diffuseColor: Long`.
   - Hitung warna dominan dari tekstur jika model menggunakan map UV, sehingga model bertekstur tetap memiliki warna shading yang akurat dan kaya.

### B. Gestur Terpadu 1-Jari & 2-Jari (`GlbImportPreviewDialog.kt`)
1. **Diferensiasi Gestur Sentuh Presisi**:
   - `activePointers.size == 1`:
     - Hitung perpindahan pointer tunggal -> Ubah `yawState` dan `pitchState` kamera (Orbit Turntable).
     - Terapkan opsi `invertX` dan `invertY` yang sudah tersedia di HUD pengontrol sisi kanan.
   - `activePointers.size >= 2`:
     - Hitung rasio jarak kedua pointer -> `distanceState` (Pinch Zoom).
     - Hitung perpindahan titik tengah (*centroid*) kedua pointer -> `previewCamera.pan()` (Geser Kamera).
2. **Reset State Halus**:
   - Hindari lonjakan posisi saat berganti dari 1 jari ke 2 jari dengan mereset pointer tracking saat jumlah sentuhan berubah.

### C. Rendering Solid Mesh Penuh & Depth Sorting (`GlbImportPreviewDialog.kt`)
1. **Hilangkan `triStep` Lompat Titik**:
   - Gambar segitiga secara kontinu (solid 100%) tanpa skipping agar model tampak padat, rapat, dan mulus.
2. **Depth Sorting (Painter's Algorithm)**:
   - Hitung rata-rata `ndc.z` dari ketiga titik sudut segitiga.
   - Urutkan segitiga dari nilai Z terjauh ke terdekat sebelum digambar ke Canvas, sehingga bagian depan model menutupi bagian belakang dengan benar dan bebas artefak transparansi.
3. **Dukungan Warna Tekstur Asli**:
   - Manfaatkan `textureBitmap` untuk mewarnai mesh, atau sampling UV warna dasar dari Bitmap pada setiap segitiga, dipadukan dengan *directional lighting* Lambertian 2-arah.

---

## 4. Rencana Pengujian & Verifikasi

1. **Pengujian Gestur**:
   - Tes sentuhan 1 jari: Memastikan model berputar lancar 360 derajat mengelilingi target.
   - Tes sentuhan 2 jari: Memastikan model dapat digeser posisinya dan diperbesar/diperkecil dengan pinch tanpa terjadi konflik dengan rotasi 1 jari.
2. **Pengujian Visual Mesh**:
   - Memastikan model GLB berdensitas tinggi (karakter, kendaraan, prop) tampil solid tanpa efek jaring bocor atau titik-titik noise.
   - Memastikan warna tekstur bawaan GLB (albedo texture) tampak hidup dan terpasang sesuai model.
3. **Pengujian Build & Unit Test**:
   - Menjalankan `compile_applet` untuk memastikan kompilasi Kotlin 0 error.
   - Menjalankan `:app:testDebugUnitTest` untuk memverifikasi kompatibilitas Robolectric.
