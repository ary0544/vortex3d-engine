package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.renderer.EngineRenderer
import com.example.viewmodel.EngineViewModel

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

    var touchOffset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("viewport_canvas")
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        viewModel.pickEntityAtScreen(
                            screenX = tapOffset.x,
                            screenY = tapOffset.y,
                            width = size.width.toFloat(),
                            height = size.height.toFloat()
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1.0f) {
                            viewModel.zoomCamera((1.0f - zoom) * 8f)
                        }
                        if (pan != Offset.Zero) {
                            viewModel.orbitCamera(
                                deltaYaw = pan.x * 0.45f,
                                deltaPitch = -pan.y * 0.45f
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

        // Compact Viewport Top Control HUD (Godot 4 Mobile Style)
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .testTag("viewport_controls_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
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

        // Isolated Viewport Stats Badge
        RenderStatsBadge(
            viewModel = viewModel,
            entityCount = scene.entities.size,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Camera Orbit / Zoom Hint on Bottom
        Text(
            text = "Drag to rotate view • Tap 3D object to select",
            color = Color.White.copy(alpha = 0.5f),
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
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA0D1117)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stats.fps} FPS",
                color = Color(0xFF00E676),
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "${stats.trianglesDrawn} Tris",
                color = Color(0xFF81D4FA),
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "$entityCount Ents",
                color = Color(0xFFFFCC80),
                fontSize = 11.sp
            )
        }
    }
}
