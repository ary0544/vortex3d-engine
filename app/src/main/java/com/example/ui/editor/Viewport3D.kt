package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.renderer.EngineRenderer
import com.example.viewmodel.EngineViewModel
import kotlin.math.hypot

@Composable
fun Viewport3D(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val scene by viewModel.scene.collectAsState()
    val selectedId by viewModel.selectedEntityId.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val isWireframe by viewModel.isWireframeMode.collectAsState()
    val cameraRev by viewModel.cameraRevision.collectAsState()
    val cameraSettings by viewModel.cameraSettings.collectAsState()

    var showCameraSettings by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("viewport_canvas")
                .pointerInput(cameraSettings) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var isTap = true
                        val touchSlop = viewConfiguration.touchSlop
                        var totalDragDistance = 0f

                        var prevPositions = mapOf(down.id to down.position)
                        var prevCentroid = down.position
                        var prevDistance = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { it.pressed }
                            if (pressedChanges.isEmpty()) break

                            val currentPositions = pressedChanges.associate { it.id to it.position }

                            if (pressedChanges.size == 1) {
                                // 1 Finger: Orbit Camera Turntable
                                val change = pressedChanges[0]
                                val prevPos = prevPositions[change.id]
                                if (prevPos != null) {
                                    val delta = change.position - prevPos
                                    totalDragDistance += delta.getDistance()
                                    if (totalDragDistance > touchSlop) {
                                        isTap = false
                                    }

                                    if (!isTap && (delta.x != 0f || delta.y != 0f)) {
                                        val multX = if (cameraSettings.invertX) -1f else 1f
                                        val multY = if (cameraSettings.invertY) -1f else 1f

                                        val deltaYaw = multX * delta.x * 0.45f * cameraSettings.orbitSensitivity
                                        val deltaPitch = multY * delta.y * 0.45f * cameraSettings.orbitSensitivity

                                        viewModel.orbitCamera(deltaYaw, deltaPitch)
                                        change.consume()
                                    }
                                }
                                prevCentroid = change.position
                                prevDistance = 0f
                            } else if (pressedChanges.size >= 2) {
                                // 2 Fingers: Pinch-to-Zoom + Two-Finger Pan
                                isTap = false
                                val p1 = pressedChanges[0]
                                val p2 = pressedChanges[1]

                                val currentCentroid = Offset(
                                    (p1.position.x + p2.position.x) / 2f,
                                    (p1.position.y + p2.position.y) / 2f
                                )
                                val currentDistance = hypot(
                                    (p1.position.x - p2.position.x),
                                    (p1.position.y - p2.position.y)
                                )

                                if (prevDistance > 0f) {
                                    // Pinch Zoom (Fingers spreading apart -> zoom in / decrease distance)
                                    val distanceDiff = currentDistance - prevDistance
                                    if (distanceDiff != 0f) {
                                        val deltaDist = -distanceDiff * 0.04f * cameraSettings.zoomSensitivity
                                        viewModel.zoomCamera(deltaDist)
                                    }

                                    // Two-Finger Pan across viewport plane
                                    val centroidDelta = currentCentroid - prevCentroid
                                    if (centroidDelta.x != 0f || centroidDelta.y != 0f) {
                                        val deltaX = -centroidDelta.x * 0.015f * cameraSettings.panSensitivity
                                        val deltaY = centroidDelta.y * 0.015f * cameraSettings.panSensitivity
                                        viewModel.panCamera(deltaX, deltaY)
                                    }

                                    p1.consume()
                                    p2.consume()
                                }

                                prevCentroid = currentCentroid
                                prevDistance = currentDistance
                            }

                            prevPositions = currentPositions
                        }

                        // Pick object on single quick tap
                        if (isTap && totalDragDistance <= touchSlop) {
                            viewModel.pickEntityAtScreen(
                                screenX = down.position.x,
                                screenY = down.position.y,
                                width = size.width.toFloat(),
                                height = size.height.toFloat()
                            )
                        }
                    }
                }
        ) {
            // Read cameraRev to trigger redraw on camera change
            if (cameraRev >= 0) {
                val renderStats = EngineRenderer.renderScene(
                    drawScope = this,
                    scene = scene,
                    camera = viewModel.camera,
                    assets = viewModel.assetManager,
                    selectedEntityId = selectedId,
                    timeSeconds = viewModel.elapsedTimeSeconds,
                    renderWireframeOnly = isWireframe
                )
                viewModel.updateRenderStats(renderStats)
            }
        }

        // Compact Viewport Top Control HUD
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .testTag("viewport_controls_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleSimulation() },
                    modifier = Modifier.size(28.dp).testTag("toggle_simulation_button")
                ) {
                    Icon(
                        imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isSimulating) "Pause Simulation" else "Play Simulation",
                        tint = if (isSimulating) Color(0xFFFFB300) else Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.resetSimulation() },
                    modifier = Modifier.size(28.dp).testTag("reset_simulation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Simulation",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleWireframe() },
                    modifier = Modifier.size(28.dp).testTag("wireframe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Wireframe Toggle",
                        tint = if (isWireframe) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Camera settings dialog toggle button
                IconButton(
                    onClick = { showCameraSettings = !showCameraSettings },
                    modifier = Modifier.size(28.dp).testTag("camera_settings_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Camera Settings",
                        tint = if (showCameraSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick camera reset button
                IconButton(
                    onClick = { viewModel.resetCamera() },
                    modifier = Modifier.size(28.dp).testTag("quick_reset_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Camera View",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.setPlayMode(true) },
                    modifier = Modifier.size(28.dp).testTag("play_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Enter Game Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Camera Settings & Invert HUD Card
        if (showCameraSettings) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 42.dp)
                    .widthIn(max = 280.dp)
                    .testTag("camera_settings_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2161922)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kontrol Kamera 3D",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { showCameraSettings = false },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Invert Orbit Horizontal (X)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Invert Horizontal (X)",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                        Switch(
                            checked = cameraSettings.invertX,
                            onCheckedChange = { viewModel.setInvertX(it) },
                            modifier = Modifier.testTag("switch_invert_x"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Invert Orbit Vertical (Y)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Invert Vertikal (Y)",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                        Switch(
                            checked = cameraSettings.invertY,
                            onCheckedChange = { viewModel.setInvertY(it) },
                            modifier = Modifier.testTag("switch_invert_y"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Orbit Sensitivity Slider
                    Text(
                        text = "Kecepatan Orbit: ${"%.1f".format(cameraSettings.orbitSensitivity)}x",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                    Slider(
                        value = cameraSettings.orbitSensitivity,
                        onValueChange = { viewModel.setOrbitSensitivity(it) },
                        valueRange = 0.2f..2.5f,
                        modifier = Modifier.height(28.dp).testTag("slider_orbit_sensitivity"),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                    )

                    // Pan & Zoom Sensitivity Slider
                    Text(
                        text = "Kecepatan 2-Jari Pan & Zoom: ${"%.1f".format(cameraSettings.panSensitivity)}x",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                    Slider(
                        value = cameraSettings.panSensitivity,
                        onValueChange = {
                            viewModel.setPanSensitivity(it)
                            viewModel.setZoomSensitivity(it)
                        },
                        valueRange = 0.2f..2.5f,
                        modifier = Modifier.height(28.dp).testTag("slider_pan_sensitivity"),
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Presets
                    Text(
                        text = "Preset Sudut Kamera:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetCamera() },
                            modifier = Modifier.weight(1f).height(28.dp).testTag("btn_preset_iso"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Reset", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setCameraFrontView() },
                            modifier = Modifier.weight(1f).height(28.dp).testTag("btn_preset_front"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Depan", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setCameraTopView() },
                            modifier = Modifier.weight(1f).height(28.dp).testTag("btn_preset_top"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Atas", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.setCameraSideView() },
                            modifier = Modifier.weight(1f).height(28.dp).testTag("btn_preset_side"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Samping", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Isolated Viewport Stats Badge
        RenderStatsBadge(
            viewModel = viewModel,
            entityCount = scene.entities.size,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Camera Orbit / Zoom Hint on Bottom
        Text(
            text = "1 Jari: Putar Orbit • 2 Jari: Pan (Geser) & Zoom • Tap: Pilih Objek",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}

@Composable
fun RenderStatsBadge(
    viewModel: EngineViewModel,
    entityCount: Int,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.renderStats.collectAsState()
    val fpsColor = when {
        stats.fps >= 50 -> Color(0xFF00E676)
        stats.fps >= 30 -> Color(0xFFFFEE58)
        else -> Color(0xFFFF7043)
    }

    Card(
        modifier = modifier.padding(6.dp).testTag("render_stats_badge"),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC0D1117)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stats.fps} FPS",
                color = fpsColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = "${String.format(java.util.Locale.US, "%.1f", stats.frameTimeMs)} ms",
                color = Color(0xFF80CBC4),
                fontSize = 10.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = "${stats.trianglesDrawn} Tris",
                color = Color(0xFF81D4FA),
                fontSize = 10.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = "$entityCount Ents",
                color = Color(0xFFFFCC80),
                fontSize = 10.sp
            )
        }
    }
}
