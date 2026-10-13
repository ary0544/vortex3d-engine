package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.math.Mat4
import com.example.engine.math.MathUtils
import com.example.engine.math.Vec3
import com.example.engine.model.ColliderShape
import com.example.engine.model.GlbAnimationInfo
import com.example.engine.model.GlbInspectionResult
import com.example.engine.model.GlbNodeInfo
import com.example.engine.model.GlbNodeType
import com.example.engine.renderer.Camera3D
import com.example.viewmodel.EngineViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlbImportPreviewDialog(
    inspection: GlbInspectionResult,
    viewModel: EngineViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Kamera Orbit Mini Preview
    val previewCamera = remember {
        val maxD = inspection.boundingBox.maxDimension.coerceAtLeast(0.5f)
        Camera3D(
            target = Vec3.ZERO,
            distance = (maxD * 2.2f).coerceIn(2.0f, 35.0f),
            pitchDegrees = 25f,
            yawDegrees = 40f
        )
    }

    var yawState by remember { mutableFloatStateOf(previewCamera.yawDegrees) }
    var pitchState by remember { mutableFloatStateOf(previewCamera.pitchDegrees) }
    var distanceState by remember { mutableFloatStateOf(previewCamera.distance) }

    var isWireframe by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var showBones by remember { mutableStateOf(true) }

    // Tab Inspector (0: Node & Tulang, 1: Animasi, 2: Konfigurasi Impor)
    var selectedRightTab by remember { mutableIntStateOf(0) }

    // Konfigurasi Spawn
    var modelScale by remember { mutableFloatStateOf(inspection.suggestedScale) }
    var selectedCollider by remember { mutableStateOf(ColliderShape.BOX) }
    var selectedRole by remember {
        mutableStateOf(
            if (inspection.boneCount > 0 || inspection.rawMetadata.recommendedRole.contains("Karakter")) {
                "Kostum Karakter TPP (Dynamic Actor)"
            } else if (inspection.rawMetadata.recommendedRole.contains("Terrain")) {
                "Permukaan / Ground Statis"
            } else {
                "3D Prop Fisika"
            }
        )
    }

    // State Animasi
    val animations = inspection.animations
    var selectedAnimIndex by remember { mutableIntStateOf(0) }
    var isPlayingAnim by remember { mutableStateOf(animations.isNotEmpty()) }
    var animCurrentTime by remember { mutableFloatStateOf(0f) }
    var animSpeed by remember { mutableFloatStateOf(1.0f) }
    var animDropdownExpanded by remember { mutableStateOf(false) }

    val currentAnim: GlbAnimationInfo? = if (animations.isNotEmpty()) {
        animations.getOrNull(selectedAnimIndex) ?: animations[0]
    } else null

    // Coroutine Player Animasi
    LaunchedEffect(isPlayingAnim, currentAnim, animSpeed) {
        if (isPlayingAnim && currentAnim != null && currentAnim.duration > 0.01f) {
            val stepMs = 33L // ~30 FPS scrubber update
            while (isActive && isPlayingAnim) {
                delay(stepMs)
                val dt = (stepMs / 1000f) * animSpeed
                animCurrentTime = (animCurrentTime + dt) % currentAnim.duration
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            color = Color(0xFF0D1117)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. TOP HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161F2E))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = inspection.modelName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1F6FEB))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Godot 3D Scene Importer", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "${inspection.rawMetadata.fileName} • ${inspection.rawMetadata.fileSizeFormatted} • ${inspection.mesh.vertexCount} Verts • ${inspection.mesh.triangleCount} Tris • ${inspection.boneCount} Tulang • ${animations.size} Animasi",
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                // 2. MAIN SPLIT CONTENT (Left: 3D Preview Canvas; Right: Inspector Tabs)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // LEFT COLUMN: Mini 3D Preview Canvas & Player
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(Color(0xFF0A0E14))
                            .padding(8.dp)
                    ) {
                        // 3D Orbit Viewport Canvas
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F1520))
                                .border(1.dp, Color(0xFF263040), RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        if (zoom != 1.0f) {
                                            distanceState = (distanceState / zoom).coerceIn(1.5f, 40.0f)
                                            previewCamera.distance = distanceState
                                        }
                                        if (pan != Offset.Zero) {
                                            yawState = (yawState + pan.x * 0.4f) % 360f
                                            pitchState = (pitchState - pan.y * 0.4f).coerceIn(-85f, 85f)
                                            previewCamera.yawDegrees = yawState
                                            previewCamera.pitchDegrees = pitchState
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        yawState = (yawState + dragAmount.x * 0.5f) % 360f
                                        pitchState = (pitchState - dragAmount.y * 0.5f).coerceIn(-85f, 85f)
                                        previewCamera.yawDegrees = yawState
                                        previewCamera.pitchDegrees = pitchState
                                    }
                                }
                        ) {
                            // Render Canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                if (w <= 0f || h <= 0f) return@Canvas

                                val aspect = w / h
                                val viewMatrix = previewCamera.getViewMatrix()
                                val projMatrix = previewCamera.getProjectionMatrix(aspect)
                                val viewProj = projMatrix * viewMatrix

                                // Gambar Grid 3D di lantai (y = 0)
                                if (showGrid) {
                                    val gridColor = Color(0xFF263238).copy(alpha = 0.6f)
                                    val gridCenterColor = Color(0xFF00E5FF).copy(alpha = 0.4f)
                                    val range = 4
                                    val spacing = 0.5f

                                    for (i in -range..range) {
                                        val pStart = viewProj.transformPoint(Vec3(i * spacing, 0f, -range * spacing))
                                        val pEnd = viewProj.transformPoint(Vec3(i * spacing, 0f, range * spacing))

                                        if (pStart.z in -1f..1f && pEnd.z in -1f..1f) {
                                            val x0 = (pStart.x * 0.5f + 0.5f) * w
                                            val y0 = (-pStart.y * 0.5f + 0.5f) * h
                                            val x1 = (pEnd.x * 0.5f + 0.5f) * w
                                            val y1 = (-pEnd.y * 0.5f + 0.5f) * h
                                            drawLine(
                                                color = if (i == 0) gridCenterColor else gridColor,
                                                start = Offset(x0, y0),
                                                end = Offset(x1, y1),
                                                strokeWidth = if (i == 0) 1.5f else 1f
                                            )
                                        }

                                        val zStart = viewProj.transformPoint(Vec3(-range * spacing, 0f, i * spacing))
                                        val zEnd = viewProj.transformPoint(Vec3(range * spacing, 0f, i * spacing))
                                        if (zStart.z in -1f..1f && zEnd.z in -1f..1f) {
                                            val x0 = (zStart.x * 0.5f + 0.5f) * w
                                            val y0 = (-zStart.y * 0.5f + 0.5f) * h
                                            val x1 = (zEnd.x * 0.5f + 0.5f) * w
                                            val y1 = (-zEnd.y * 0.5f + 0.5f) * h
                                            drawLine(
                                                color = if (i == 0) gridCenterColor else gridColor,
                                                start = Offset(x0, y0),
                                                end = Offset(x1, y1),
                                                strokeWidth = if (i == 0) 1.5f else 1f
                                            )
                                        }
                                    }
                                }

                                // Offset titik pusat model agar tepat berada di origin
                                val centerOffset = inspection.boundingBox.center

                                // Render Model Mesh
                                val mesh = inspection.mesh
                                val baseColor = Color(inspection.material.albedoColor)
                                val lightDir = Vec3(0.5f, 1.0f, 0.4f).normalized()

                                // Project semua vertex
                                val projected = Array(mesh.vertices.size) { i ->
                                    val v = mesh.vertices[i]
                                    val centeredPos = v.position - centerOffset
                                    val ndc = viewProj.transformPoint(centeredPos)
                                    val sx = (ndc.x * 0.5f + 0.5f) * w
                                    val sy = (-ndc.y * 0.5f + 0.5f) * h
                                    Triple(sx, sy, ndc.z)
                                }

                                if (isWireframe) {
                                    val wfColor = Color(0xFF00E5FF).copy(alpha = 0.7f)
                                    val path = Path()
                                    for (tri in mesh.triangles) {
                                        val p0 = projected.getOrNull(tri.i0) ?: continue
                                        val p1 = projected.getOrNull(tri.i1) ?: continue
                                        val p2 = projected.getOrNull(tri.i2) ?: continue
                                        if (p0.third !in -1f..1f && p1.third !in -1f..1f && p2.third !in -1f..1f) continue

                                        path.moveTo(p0.first, p0.second)
                                        path.lineTo(p1.first, p1.second)
                                        path.lineTo(p2.first, p2.second)
                                        path.close()
                                    }
                                    drawPath(path, color = wfColor, style = Stroke(width = 1f))
                                } else {
                                    // Shaded Solid Triangles
                                    val path = Path()
                                    for (tri in mesh.triangles) {
                                        val p0 = projected.getOrNull(tri.i0) ?: continue
                                        val p1 = projected.getOrNull(tri.i1) ?: continue
                                        val p2 = projected.getOrNull(tri.i2) ?: continue

                                        // Frustum check
                                        if (p0.third < -1f || p0.third > 1f || p1.third < -1f || p1.third > 1f || p2.third < -1f || p2.third > 1f) continue

                                        // Backface Culling sederhana
                                        val edge1X = p1.first - p0.first
                                        val edge1Y = p1.second - p0.second
                                        val edge2X = p2.first - p0.first
                                        val edge2Y = p2.second - p0.second
                                        val crossZ = edge1X * edge2Y - edge1Y * edge2X
                                        if (crossZ <= 0f) continue // backface

                                        val v0 = mesh.vertices[tri.i0]
                                        val diffuse = (v0.normal.dot(lightDir)).coerceIn(0.2f, 1.0f)
                                        val shadeColor = baseColor.copy(
                                            red = (baseColor.red * diffuse).coerceIn(0f, 1f),
                                            green = (baseColor.green * diffuse).coerceIn(0f, 1f),
                                            blue = (baseColor.blue * diffuse).coerceIn(0f, 1f)
                                        )

                                        path.reset()
                                        path.moveTo(p0.first, p0.second)
                                        path.lineTo(p1.first, p1.second)
                                        path.lineTo(p2.first, p2.second)
                                        path.close()
                                        drawPath(path, color = shadeColor)
                                    }
                                }

                                // Visualisasi Tulang / Bone Joints jika opsi aktif
                                if (showBones && inspection.boneCount > 0) {
                                    val boneJointColor = Color(0xFFFFD54F)
                                    fun drawNodeHierarchy(node: GlbNodeInfo) {
                                        if (node.type == GlbNodeType.BONE) {
                                            val ndc = viewProj.transformPoint(node.translation - centerOffset)
                                            if (ndc.z in -1f..1f) {
                                                val sx = (ndc.x * 0.5f + 0.5f) * w
                                                val sy = (-ndc.y * 0.5f + 0.5f) * h
                                                drawCircle(color = boneJointColor, radius = 3.5f, center = Offset(sx, sy))
                                            }
                                        }
                                        for (ch in node.children) {
                                            drawNodeHierarchy(ch)
                                        }
                                    }
                                    for (root in inspection.rootNodes) {
                                        drawNodeHierarchy(root)
                                    }
                                }
                            }

                            // Viewport Controls Overlay (Top-Right Floating)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF161F2E).copy(alpha = 0.85f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = isWireframe,
                                    onClick = { isWireframe = !isWireframe },
                                    label = { Text("Wireframe", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF),
                                        selectedLabelColor = Color.Black
                                    ),
                                    modifier = Modifier.height(26.dp)
                                )
                                FilterChip(
                                    selected = showGrid,
                                    onClick = { showGrid = !showGrid },
                                    label = { Text("Grid", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF),
                                        selectedLabelColor = Color.Black
                                    ),
                                    modifier = Modifier.height(26.dp)
                                )
                                if (inspection.boneCount > 0) {
                                    FilterChip(
                                        selected = showBones,
                                        onClick = { showBones = !showBones },
                                        label = { Text("Tulang", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFFD54F),
                                            selectedLabelColor = Color.Black
                                        ),
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        yawState = 40f
                                        pitchState = 25f
                                        distanceState = (inspection.boundingBox.maxDimension * 2.2f).coerceIn(2.0f, 35.0f)
                                        previewCamera.yawDegrees = yawState
                                        previewCamera.pitchDegrees = pitchState
                                        previewCamera.distance = distanceState
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset Kamera", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            // Gesture Hint (Bottom-Left)
                            Text(
                                text = "1-Jari: Orbit • 2-Jari: Zoom Pinch",
                                color = Color(0xFF8B949E).copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        // Animation Player Bar
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263040)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (animations.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play/Pause Button
                                    IconButton(
                                        onClick = { isPlayingAnim = !isPlayingAnim },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlayingAnim) Color(0xFF00E5FF) else Color(0xFF238636))
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAnim) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = if (isPlayingAnim) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Dropdown Pemilih Animasi
                                    Box {
                                        OutlinedButton(
                                            onClick = { animDropdownExpanded = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text(
                                                text = currentAnim?.name ?: "Animasi",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }

                                        DropdownMenu(
                                            expanded = animDropdownExpanded,
                                            onDismissRequest = { animDropdownExpanded = false }
                                        ) {
                                            animations.forEachIndexed { idx, anim ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "${anim.name} (${String.format(Locale.US, "%.1fs", anim.duration)})",
                                                            fontSize = 12.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedAnimIndex = idx
                                                        animCurrentTime = 0f
                                                        isPlayingAnim = true
                                                        animDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Timeline Scrubber
                                    val duration = currentAnim?.duration ?: 1.0f
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.2fs", animCurrentTime)} / ${String.format(Locale.US, "%.2fs", duration)}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF00E5FF),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${animSpeed}x • Loop",
                                                fontSize = 10.sp,
                                                color = Color(0xFF8B949E)
                                            )
                                        }
                                        Slider(
                                            value = animCurrentTime,
                                            onValueChange = {
                                                animCurrentTime = it
                                                isPlayingAnim = false
                                            },
                                            valueRange = 0f..duration,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF00E5FF),
                                                activeTrackColor = Color(0xFF00E5FF)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(20.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Toggle Speed (0.5x, 1x, 2x)
                                    OutlinedButton(
                                        onClick = {
                                            animSpeed = when (animSpeed) {
                                                1.0f -> 2.0f
                                                2.0f -> 0.5f
                                                else -> 1.0f
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("${animSpeed}x", fontSize = 10.sp)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Animation, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Model GLB ini tidak memiliki track animasi tertanam (Static Model).",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8B949E)
                                    )
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Tabs (Node Tree & Tulang, Animasi, Konfigurasi Impor)
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .background(Color(0xFF111722))
                            .padding(8.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = selectedRightTab,
                            containerColor = Color(0xFF161F2E),
                            contentColor = Color(0xFF00E5FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Tab(
                                selected = selectedRightTab == 0,
                                onClick = { selectedRightTab = 0 },
                                text = { Text("Hierarki & Tulang", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            Tab(
                                selected = selectedRightTab == 1,
                                onClick = { selectedRightTab = 1 },
                                text = { Text("Animasi (${animations.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            Tab(
                                selected = selectedRightTab == 2,
                                onClick = { selectedRightTab = 2 },
                                text = { Text("Pengaturan Impor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // TAB CONTENT CONTAINER
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0E131C))
                                .border(1.dp, Color(0xFF1F2B3E), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            when (selectedRightTab) {
                                0 -> NodeTreeAndSkeletonTab(inspection)
                                1 -> AnimationsListTab(
                                    animations = animations,
                                    selectedAnimIndex = selectedAnimIndex,
                                    onSelectAnimation = {
                                        selectedAnimIndex = it
                                        animCurrentTime = 0f
                                        isPlayingAnim = true
                                    }
                                )
                                2 -> ImportSettingsTab(
                                    inspection = inspection,
                                    modelScale = modelScale,
                                    onScaleChange = { modelScale = it },
                                    selectedCollider = selectedCollider,
                                    onColliderChange = { selectedCollider = it },
                                    selectedRole = selectedRole,
                                    onRoleChange = { selectedRole = it },
                                    onCopyPath = {
                                        viewModel.copyToClipboard(
                                            context,
                                            inspection.rawMetadata.recommendedFolder,
                                            "Path Folder Rekomendasi"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. BOTTOM ACTION BAR (Buttons: Batal, Simpan ke Assets, Spawn ke Scene 3D)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161F2E))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Batal", fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                inspection.file?.let { viewModel.importGlbToAssets(it) }
                                onDismiss()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Simpan ke Assets Saja", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.spawnGlbFromInspection(
                                    inspection = inspection,
                                    colliderShape = selectedCollider,
                                    customScale = modelScale,
                                    role = selectedRole
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("confirm_spawn_glb_button")
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Spawn ke Scene 3D ★", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeTreeAndSkeletonTab(inspection: GlbInspectionResult) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pohon Hierarki Node & Tulang (${inspection.totalNodesCount} Nodes)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (inspection.boneCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFD54F).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Skeleton: ${inspection.boneCount} Tulang",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }
        }

        if (inspection.rootNodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada node yang terdaftar.", color = Color(0xFF8B949E), fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("glb_node_tree_list")
            ) {
                items(inspection.rootNodes) { node ->
                    NodeTreeItemRow(node = node, depth = 0)
                }
            }
        }
    }
}

@Composable
private fun NodeTreeItemRow(node: GlbNodeInfo, depth: Int) {
    var isExpanded by remember { mutableStateOf(depth < 2) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { isExpanded = !isExpanded }
                .padding(start = (depth * 14).dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (badgeColor, typeLabel) = when (node.type) {
                GlbNodeType.BONE -> Pair(Color(0xFFFFD54F), "BONE")
                GlbNodeType.MESH -> Pair(Color(0xFF00E5FF), "MESH")
                else -> Pair(Color(0xFF8B949E), "NODE")
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(typeLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = badgeColor)
            }

            Spacer(Modifier.width(6.dp))

            Text(
                text = node.name,
                fontSize = 11.sp,
                fontWeight = if (node.type == GlbNodeType.BONE) FontWeight.SemiBold else FontWeight.Normal,
                color = if (node.type == GlbNodeType.BONE) Color(0xFFFFD54F) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (node.children.isNotEmpty()) {
                Text(
                    text = "${node.children.size} child",
                    fontSize = 9.sp,
                    color = Color(0xFF8B949E)
                )
            }
        }

        if (isExpanded && node.children.isNotEmpty()) {
            for (ch in node.children) {
                NodeTreeItemRow(node = ch, depth = depth + 1)
            }
        }
    }
}

@Composable
private fun AnimationsListTab(
    animations: List<GlbAnimationInfo>,
    selectedAnimIndex: Int,
    onSelectAnimation: (Int) -> Unit
) {
    if (animations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Animation, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(6.dp))
                Text("Model GLB ini tidak memiliki klip animasi.", color = Color(0xFF8B949E), fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(animations) { anim ->
                val isSelected = anim.index == selectedAnimIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onSelectAnimation(anim.index) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1B2C44) else Color(0xFF141A24)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF263040)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.PlayArrow else Icons.Default.Animation,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF8B949E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = anim.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White
                                )
                                Text(
                                    text = "Durasi: ${String.format(Locale.US, "%.2f detik", anim.duration)} • ${anim.channelsCount} Saluran",
                                    fontSize = 10.sp,
                                    color = Color(0xFF8B949E)
                                )
                            }
                        }

                        Button(
                            onClick = { onSelectAnimation(anim.index) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF238636)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = if (isSelected) "Aktif" else "Pilih",
                                fontSize = 10.sp,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSettingsTab(
    inspection: GlbInspectionResult,
    modelScale: Float,
    onScaleChange: (Float) -> Unit,
    selectedCollider: ColliderShape,
    onColliderChange: (ColliderShape) -> Unit,
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    onCopyPath: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Skala Normalisasi
        Text("Skala Model 3D", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Faktor Skala: ${String.format(Locale.US, "%.3fx", modelScale)}",
                fontSize = 11.sp,
                color = Color(0xFF00E5FF)
            )
            OutlinedButton(
                onClick = { onScaleChange(inspection.suggestedScale) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text("Normalisasi (${String.format(Locale.US, "%.2fx", inspection.suggestedScale)})", fontSize = 9.sp)
            }
        }

        Slider(
            value = modelScale.coerceIn(0.01f, 5.0f),
            onValueChange = onScaleChange,
            valueRange = 0.01f..5.0f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        val size = inspection.boundingBox.size
        Text(
            text = "Dimensi Bounding Box: W: ${String.format(Locale.US, "%.2f", size.x * modelScale)}m × H: ${String.format(Locale.US, "%.2f", size.y * modelScale)}m × D: ${String.format(Locale.US, "%.2f", size.z * modelScale)}m",
            fontSize = 10.sp,
            color = Color(0xFF8B949E)
        )

        Spacer(Modifier.height(10.dp))

        // 2. Bentuk Collider Fisika
        Text("Bentuk Collider Fisika", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(ColliderShape.BOX, ColliderShape.SPHERE, ColliderShape.PLANE).forEach { shape ->
                FilterChip(
                    selected = selectedCollider == shape,
                    onClick = { onColliderChange(shape) },
                    label = { Text(shape.name, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 3. Tipe Entitas / Peran di Scene
        Text("Tipe Peran Model", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        listOf(
            "Kostum Karakter TPP (Dynamic Actor)",
            "3D Prop Fisika (Dynamic RigidBody)",
            "Permukaan / Ground Statis (Static Body)"
        ).forEach { role ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRoleChange(role) }
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (selectedRole == role) Color(0xFF00E5FF) else Color(0xFF263040)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedRole == role) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Black))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(text = role, fontSize = 11.sp, color = if (selectedRole == role) Color.White else Color(0xFF8B949E))
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4. Folder Penempatan & Rekomendasi
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141D2B)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Folder Rekomendasi:", fontSize = 10.sp, color = Color(0xFF8B949E))
                    OutlinedButton(
                        onClick = onCopyPath,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                        modifier = Modifier.height(22.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFF00E5FF))
                        Spacer(Modifier.width(3.dp))
                        Text("Salin Path", fontSize = 8.sp, color = Color(0xFF00E5FF))
                    }
                }
                Text(
                    text = inspection.rawMetadata.recommendedFolder,
                    fontSize = 11.sp,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = inspection.rawMetadata.recommendationTip,
                    fontSize = 9.sp,
                    color = Color(0xFFC9D1D9),
                    lineHeight = 12.sp
                )
            }
        }
    }
}
