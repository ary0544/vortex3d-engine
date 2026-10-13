# Kontrol Multi-Touch Viewport 3D & Kustomisasi Kamera

Rencana peningkatan interaksi Viewport 3D pada Vortex3D untuk mendukung pemisahan gestur 1-jari (Orbit Turntable) dan 2-jari (Pinch-to-Zoom & Two-Finger Pan), dilengkapi opsi penyesuaian arah geser (Invert X & Invert Y), slider sensitivitas, dan tombol reset view instan.

## User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan preferensi yang telah Anda konfirmasi:
> - **Pemisahan Gestur**: 1 jari didedikasikan untuk Orbit kamera mengelilingi target 3D; 2 jari secara bersamaan menangani Pinch-to-Zoom (perbesar/perkecil) dan Panning (menggeser titik fokus kamera).
> - **Inversi Sumbu Terbalik**: Disediakan toggle interaktif `Invert X` dan `Invert Y` langsung di viewport (dan status tersimpan) agar arah geser dapat dibalik sesuai preferensi intuisi Anda.
> - **Sensitivitas & Reset**: Ditambahkan menu overlay ringan di sudut viewport berisi slider sensitivitas rotasi/pan serta tombol "Reset View" (mengembalikan sudut dan posisi kamera ke preset standar).

---

## 1. Overview & Core Concept

- **Apa yang Dibangun**: Sistem input gestur multi-touch khusus pada Viewport 3D yang memisahkan jumlah pointer sentuhan secara presisi:
  - **1 Jari**: Melakukan orbit rotasi kamera (`yaw` dan `pitch`) terhadap target objek.
  - **2 Jari**: Melakukan transformasi gabungan pinch-to-zoom (mengubah jarak kamera) dan two-finger pan (menggeser titik target kamera pada bidang sejajar pandangan layar).
  - **Menu Quick Camera Settings**: Popover ringkas di toolbar viewport untuk mengubah Invert X, Invert Y, sensitivitas geser, dan tombol Reset Camera.
- **Target Pengguna**: Pembuat game dan desainer 3D mobile yang membutuhkan navigasi scene yang presisi, luwes, dan ergonomis tanpa rasa canggung atau arah putar terbalik.
- **Nilai Tambah**: Menghilangkan konflik antara zoom, pan, dan orbit yang sebelumnya tercampur di satu handler, serta memberikan kontrol penuh atas orientasi rotasi.

---

## 2. User Experience & Visual Design

### Alur Interaksi Pengguna (User Flow)
1. **Navigasi 1 Jari (Orbit)**:
   - Pengguna menyentuh kanvas 3D dengan 1 jari dan menggesernya.
   - Kamera berputar mengelilingi objek fokus secara mulus.
   - Jika arah terasa terbalik, pengguna membuka ikon gear/kamera di sudut viewport lalu mengaktifkan toggle `Invert X` atau `Invert Y`.
2. **Navigasi 2 Jari (Zoom & Pan)**:
   - Pengguna menempelkan 2 jari di layar:
     - Mencubit (pinch in/out) memperbesar atau memperkecil jarak kamera secara langsung dan mulus.
     - Menggeser kedua jari bersamaan menggeser (pan) target kamera horizontal dan vertikal di ruang 3D.
3. **Reset & Presets Cepat**:
   - Jika kamera tersesat atau terlalu jauh, pengguna cukup menekan tombol `Reset View` untuk kembali ke posisi awal (sudut isometrik 45°, pitch 30°, jarak 8 unit).

### Identitas Visual & Komponen UI
- **Viewport Floating HUD**:
  - Tombol ikon `ControlCamera` / `Tune` di samping kontrol play/pause saat ini dengan gaya semi-transparan Material 3 Surface (`tonalElevation = 3.dp`).
  - Dropdown/Popover Floating Card bernuansa dark editor (`Color(0xFF1E2129)`), border halus (`Color(0xFF2C3240)`), dan aksen Cyan/Primary.
  - Switch interaktif untuk `Invert Orbit X` dan `Invert Orbit Y`.
  - Slider responsif untuk `Orbit Sensitivity` dan `Pan/Zoom Sensitivity`.
  - Action chips untuk `Reset View`, `Front View`, `Top View`.

---

## 3. Key Product Decisions & Trade-Offs

- **Deteksi Gestur: Pointer Input Kustom vs `detectTransformGestures` Bawaan**:
  - *Pendekatan Terpilih*: Menggunakan `pointerInput` dengan pembacaan multi-pointer (`awaitPointerEventScope`) yang secara eksplisit menghitung jumlah sentuhan aktif (`pointers.size`).
  - *Alasan*: `detectTransformGestures` bawaan Compose mencampuradukkan 1 jari dan 2 jari ke dalam callback `pan` yang sama, sehingga 1 jari dan 2 jari tidak bisa dibedakan. Dengan loop pointer event kustom, gestur 1-jari murni memicu orbit, sedangkan gestur 2-jari murni memicu pan + pinch-zoom.
- **Penyimpanan Preferensi Inversi & Sensitivitas**:
  - *Pendekatan Terpilih*: Disimpan di dalam `EngineViewModel` (dan dapat dipertahankan selama sesi aktif) dengan default yang lebih intuitif, plus toggle langsung di UI.
  - *Alasan*: Pengguna bisa langsung merasakan efek pembalikan arah secara real-time tanpa harus membuka menu pengaturan tersembunyi.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      Viewport3D Canvas                      │
│                                                             │
│   pointerInput (awaitPointerEventScope)                     │
│    ├─ pointerCount == 1 ──► Orbit Camera (yaw / pitch)      │
│    │                        ├─ Apply Invert X / Invert Y    │
│    │                        └─ Apply Orbit Sensitivity      │
│    │                                                        │
│    └─ pointerCount >= 2 ──► Dual Gestures:                  │
│                             ├─ Pinch Distance ─► Zoom Camera│
│                             └─ Centroid Delta ─► Pan Camera │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      EngineViewModel                        │
│  - cameraSettingsState:                                     │
│      invertOrbitX: Boolean, invertOrbitY: Boolean            │
│      orbitSensitivity: Float, panSensitivity: Float         │
│  - camera.orbit(deltaYaw, deltaPitch)                       │
│  - camera.zoom(deltaDistance)                               │
│  - camera.pan(deltaX, deltaY)                               │
│  - camera.reset(distance, pitch, yaw, target)               │
└─────────────────────────────────────────────────────────────┘
```

### State & Event Mapping
- `cameraSettings`: StateFlow di `EngineViewModel` memuat preferensi inversi dan sensitivitas.
- `onPointerEvent`:
  - 1 pointer: `deltaX * (if (invertX) -1 else 1) * sensitivity`, `deltaY * (if (invertY) 1 else -1) * sensitivity` diteruskan ke `orbitCamera`.
  - 2 pointer: Perhitungan jarak Euclidean antar-jari untuk delta zoom, dan perpindahan titik tengah (centroid) untuk `panCamera`.
- `Camera3D`: Menambahkan fungsi `resetView()` dan opsi preset (Front, Top, Isometric).
