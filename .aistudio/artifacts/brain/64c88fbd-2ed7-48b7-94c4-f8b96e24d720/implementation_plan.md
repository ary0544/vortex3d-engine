# Vortex3D Engine: Godot 4 Mobile Architecture, Project Lobby & Collapsible Dock Studio

Implementation plan for integrating Godot Engine 4 Mobile-inspired ergonomics into Vortex3D: a dedicated **Project Manager & Scene Hub (Lobby)**, **Collapsible Edge Dock Panels** (Left, Right, and Bottom Docks), **Compact Density Adaptive Scaling** across all screen sizes (phones, foldables, tablets), and bulletproof **LazyList Key Stability**.

## User Decisions Summary

> [!IMPORTANT]
> Choices confirmed during clarification:
> - **Lobby / Project Hub Concept**: **Model Hub Proyek dan Scene Baru** — Dedicated Godot 4-style Project Manager to browse existing projects/scenes, create new scenes with custom templates, and switch between Lobby and Editor.
> - **Editor Panel Ergonomics**: **Dock panel collapsible seperti Godot 4 Android** — Multi-dock layout with 1-tap edge collapse toggles (Scene Tree / Inspector / Bottom Console & File Drawer) so the 3D Viewport can effortlessly claim 100% of the screen.
> - **Screen Adaptivity**: **Skala dinamis teks dan ikon dengan compact density** — Pro-grade compact UI density with dynamic font scaling, tight padding, and responsive touch bounds adapted for small smartphones up to foldables and tablets.
> - **Crash Resolution**: Guaranteed unique and stable keys for all LazyColumn / LazyRow items across the entire app.

---

## 1. Architecture & Component Blueprint

```
                     ┌──────────────────────────────────────────────┐
                     │            VORTEX3D ENGINE HUB               │
                     │  (Godot 4 Mobile Project Manager / Lobby)    │
                     └──────────────────────┬───────────────────────┘
                                            │ Open Scene / Create New
                                            ▼
                     ┌──────────────────────────────────────────────┐
                     │         VORTEX3D DOCK-BASED STUDIO           │
                     └──────────────────────┬───────────────────────┘
                                            │
         ┌──────────────────────────────────┼──────────────────────────────────┐
         ▼                                  ▼                                  ▼
┌───────────────────┐            ┌──────────────────────┐            ┌───────────────────┐
│   LEFT DOCK       │            │   CENTER VIEWPORT    │            │   RIGHT DOCK      │
│ (Collapsible)     │            │    (3D Interactive)  │            │ (Collapsible)     │
│                   │            │                      │            │                   │
│ • Scene Tree      │◄──────────►│ • Camera Orbit / Pan │◄──────────►│ • Inspector       │
│ • Add Entity      │  [Toggle]  │ • Transform Gizmo    │  [Toggle]  │ • Component Props │
│ • Node Visibility │            │ • Play/Simulate HUD  │            │ • Lua Attachments │
└───────────────────┘            └──────────┬───────────┘            └───────────────────┘
                                            │
                                            ▼
                                 ┌──────────────────────┐
                                 │     BOTTOM DOCK      │
                                 │  (Collapsible Drawer)│
                                 │                      │
                                 │ • File Manager / SAF │
                                 │ • Asset Browser / GLB│
                                 │ • Console Logs & Fix │
                                 └──────────────────────┘
```

---

## 2. Key Modules to Implement

### A. Dedicated Project Lobby & Scene Hub (`ProjectHubView.kt`)
1. **Header & Quick Actions**:
   - Engine Version badge (`Vortex3D v2.4 (Godot 4 Mobile Edition)`).
   - `+ New Scene / Project` button with template selection:
     - **Blank 3D Space** (Grid, Ambient Light, Main Camera).
     - **Physics Playground** (Spheres, Cubes, Ground Plane, Gravity).
     - **FPS / Character Arena** (Spawn point, Obstacles, Kinematic Controller).
     - **Solar Orbit Simulation** (Sun, Planets, Scripted Revolution).
2. **Project Card Grid & List**:
   - Shows Scene title, last modified timestamp, entity count, and thumbnail badge.
   - Actions per scene: **Open in Editor**, **Clone**, **Rename**, and **Delete**.
   - Direct button in Editor TopBar: `Lobby / Projects` to navigate back to the Project Manager anytime without loss of state.

### B. Godot 4-Style Collapsible Dock Architecture (`StudioDocks.kt` & `EditorScreen.kt`)
1. **Left Dock (Scene / Nodes)**:
   - Houses the **Scene Tree / Hierarchy** with search filter, node visibility eye icon, and `+ Add Primitive / Light / Camera / GLB`.
   - Collapsible tab trigger on the left margin (can be folded with one tap to give full screen width to the 3D viewport).
2. **Right Dock (Inspector & Node Properties)**:
   - Houses the **Inspector** (Entity Name, Transform Position/Rotation/Scale with numeric scrubbers, RigidBody Physics, Material colors, Shaders, Lua script attachments).
   - Collapsible tab trigger on the right margin.
3. **Bottom Dock (Drawer for Logs, Files & Assets)**:
   - Houses **Bottom Panels** like Godot 4: `Output / Console`, `FileSystem / ZArchiver Manager`, `Asset Library`, and `Shader Editor`.
   - Has height toggle: Minimized bar (showing log count & status), Half-screen expanded, or Hidden.
4. **Adaptive Phone vs Tablet/Landscape Layout**:
   - **Landscape / Tablet**: Left Dock (220dp), Center Viewport (flexible weight), Right Dock (260dp), Bottom Drawer (expandable). Docks can be independently collapsed or expanded.
   - **Portrait Mobile**: Smart compact mode where bottom navigation allows quick switching between Viewport, Hierarchy, Inspector, and Files, with quick-collapse floating buttons.

### C. Dynamic Scaling & Compact UI Density
1. **Responsive Dimension Tokens (`StudioDensity.kt`)**:
   - Provide compact typography scales: 10.sp labels, 11.sp secondary, 12.sp headers, 13.sp titles.
   - Space-saving component heights (32dp - 36dp for tool buttons, 44dp minimum touch surface bounds with tight visible padding).
   - Icon sizing tailored to viewport width class (`WindowWidthSizeClass` / screen density).
2. **Godot-style High-Contrast Dark Theme**:
   - Deep slate engine background (`#1E1F22` / `#2B2D30`), accented with Godot-inspired cyan/blue highlights (`#478CBF`), emerald green for transform Y (`#47B347`), ruby red for X (`#E05555`), and sapphire blue for Z (`#477CE0`).

### D. Crash Prevention & LazyList Key Uniqueness Audit
1. Audit and ensure **guaranteed uniqueness** across all LazyColumn and LazyRow composables:
   - `HierarchyView.kt`: `key = { index, entity -> "entity_${entity.id}_${index}" }`
   - `AssetBrowserView.kt`: `key = { index, item -> "asset_${item.id}_${index}" }`
   - `FileManagerView.kt`: `key = { index, file -> "file_${file.absolutePath}_${index}" }`
   - `ConsoleLogView.kt`: `key = { index, log -> "log_${log.timestamp}_${log.id}_${index}" }`
   - `ProjectHubView.kt`: `key = { index, proj -> "project_${proj.id}_${index}" }`
   - Eliminates all `IllegalArgumentException: Key was already used` crashes definitively.

---

## 3. Implementation Steps

1. **Phase 1: Project Lobby / Manager**
   - Implement `ProjectHubView.kt` with project listing, template creation, and scene switching logic in `EngineViewModel.kt`.
   - Add state in ViewModel for `currentScreen: EditorScreenState` (`LOBBY` vs `EDITOR` vs `PLAY_MODE`).

2. **Phase 2: Collapsible Docks & Compact Layout System**
   - Create dock wrappers `CollapsibleLeftDock`, `CollapsibleRightDock`, and `CollapsibleBottomDock`.
   - Update `EditorScreen.kt` to integrate the docks in both Landscape (3-pane collapsible) and Portrait modes.
   - Add Godot-style edge toggle strip buttons (`Icons.Default.ChevronLeft`, `Icons.Default.ChevronRight`, etc.).

3. **Phase 3: Density Tuning & LazyList Stability**
   - Apply compact typography tokens and tight margins.
   - Refactor all `LazyColumn` key lambdas across the codebase to ensure prefix + ID + index uniqueness.

4. **Phase 4: Compilation & Verification**
   - Run `compile_applet` to confirm clean compilation and zero build errors.
