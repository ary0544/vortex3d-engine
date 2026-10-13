package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.renderer.EngineRenderer
import com.example.viewmodel.EngineViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun PlayModeOverlay(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val scene by viewModel.scene.collectAsState()
    val isWireframe by viewModel.isWireframeMode.collectAsState()
    val cameraRev by viewModel.cameraRevision.collectAsState()
    val cameraSettings by viewModel.cameraSettings.collectAsState()

    var stickOffset by remember { mutableStateOf(Offset.Zero) }
    val maxStickRadius = 60f

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 3D Game Viewport Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("play_mode_canvas")
                .pointerInput(cameraSettings) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Right screen camera drag
                        if (change.position.x > size.width * 0.4f) {
                            val multX = if (cameraSettings.invertX) -1f else 1f
                            val multY = if (cameraSettings.invertY) -1f else 1f
                            viewModel.orbitCamera(
                                deltaYaw = multX * dragAmount.x * 0.4f * cameraSettings.orbitSensitivity,
                                deltaPitch = multY * dragAmount.y * 0.4f * cameraSettings.orbitSensitivity
                            )
                        }
                    }
                }
        ) {
            if (cameraRev >= 0) {
                val renderStats = EngineRenderer.renderScene(
                    drawScope = this,
                    scene = scene,
                    camera = viewModel.camera,
                    assets = viewModel.assetManager,
                    selectedEntityId = null,
                    timeSeconds = viewModel.elapsedTimeSeconds,
                    renderWireframeOnly = isWireframe
                )
                viewModel.updateRenderStats(renderStats)
            }
        }

        // Top Status HUD & Exit Button
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayModeFpsChip(viewModel = viewModel)

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { viewModel.resetSimulation() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Game", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = { viewModel.setPlayMode(false) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp).testTag("exit_play_mode_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit to Editor", modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Exit Editor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Virtual Joystick on Bottom-Left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp)
                .size(130.dp)
                .background(Color(0x55000000), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            stickOffset = Offset.Zero
                            viewModel.inputState.horizontal = 0f
                            viewModel.inputState.vertical = 0f
                        },
                        onDragCancel = {
                            stickOffset = Offset.Zero
                            viewModel.inputState.horizontal = 0f
                            viewModel.inputState.vertical = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val newOffset = stickOffset + dragAmount
                        val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        stickOffset = if (dist > maxStickRadius) {
                            val angle = atan2(newOffset.y, newOffset.x)
                            Offset(cos(angle) * maxStickRadius, sin(angle) * maxStickRadius)
                        } else {
                            newOffset
                        }
                        viewModel.inputState.horizontal = (stickOffset.x / maxStickRadius).coerceIn(-1f, 1f)
                        viewModel.inputState.vertical = -(stickOffset.y / maxStickRadius).coerceIn(-1f, 1f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Stick thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(stickOffset.x.toInt(), stickOffset.y.toInt()) }
                    .size(52.dp)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.85f), CircleShape)
            )
        }

        // Action Buttons on Bottom-Right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    viewModel.inputState.actionFire = true
                    viewModel.playSound("laser")
                },
                modifier = Modifier
                    .size(52.dp)
                    .testTag("action_fire_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = "Fire Action", tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.inputState.actionJump = true
                    viewModel.playSound("jump")
                },
                modifier = Modifier
                    .size(62.dp)
                    .testTag("action_jump_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                shape = CircleShape
            ) {
                Text("JUMP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun PlayModeFpsChip(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.renderStats.collectAsState()
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xAA0D1117)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("GAME RUNTIME", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF00E676))
            Spacer(Modifier.width(8.dp))
            Text("${stats.fps} FPS", fontSize = 11.sp, color = Color.White)
        }
    }
}
