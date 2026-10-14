package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Dialog Pratinjau & Pengaturan Impor Model 3D GLB tingkat profesional.
 * Sisi kiri: Full Viewport 3D GLB dengan kontrol orbit kamera, normalisasi bounding box otomatis,
 * visualisasi tulang, wireframe, grid lantai, serta timeline animasi terintegrasi.
 * Sisi kanan: Panel pengatur & tab informasi lengkap GLB (Spesifikasi Penuh, Pengaturan Impor, Hierarki & Animasi).
 */
@Composable
fun GlbImportPreviewDialog(
    inspection: GlbInspectionResult,
    viewModel: EngineViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Kamera Orbit Mini Preview dengan jarak dan skala optimal agar memenuhi frame
    val previewCamera = remember {
        Camera3D(
            target = Vec3.ZERO,
            distance = 3.2f,
            pitchDegrees = 15f,
            yawDegrees = 35f
        )
    }

    var yawState by remember { mutableFloatStateOf(previewCamera.yawDegrees) }
    var pitchState by remember { mutableFloatStateOf(previewCamera.pitchDegrees) }
    var distanceState by remember { mutableFloatStateOf(previewCamera.distance) }

    var isWireframe by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var showBones by remember { mutableStateOf(inspection.boneCount > 0) }
    var cameraRevision by remember { mutableStateOf(0L) }

    // Mode Gestur 2-Jari: "pan" (Geser / Pan Kamera) atau "orbit" (Putar Sudut Kamera)
    var twoFingerMode by remember { mutableStateOf("pan") }
    var invertX by remember { mutableStateOf(false) }
    var invertY by remember { mutableStateOf(false) }

    fun resetCameraView() {
        yawState = 35f
        pitchState = 15f
        distanceState = 3.2f
        previewCamera.reset()
        previewCamera.yawDegrees = yawState
        previewCamera.pitchDegrees = pitchState
        previewCamera.distance = distanceState
        cameraRevision++
    }

    fun setFrontView() {
        yawState = 0f
        pitchState = 0f
        previewCamera.yawDegrees = yawState
        previewCamera.pitchDegrees = pitchState
        cameraRevision++
    }

    fun setTopView() {
        yawState = 0f
        pitchState = 85f
        previewCamera.yawDegrees = yawState
        previewCamera.pitchDegrees = pitchState
        cameraRevision++
    }

    fun setIsometricView() {
        yawState = 45f
        pitchState = 30f
        previewCamera.yawDegrees = yawState
        previewCamera.pitchDegrees = pitchState
        cameraRevision++
    }

    // Tab Pengatur di Sisi Kanan (0: Info Lengkap, 1: Pengaturan Impor, 2: Hierarki & Tulang, 3: Animasi)
    var selectedRightTab by remember { mutableIntStateOf(0) }

    // Konfigurasi Skala & Spawning
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

    // Coroutine Pemutar Animasi Scrubber
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

    // Normalisasi skala otomatis agar model dengan ukuran berapapun tampil optimal dan memenuhi frame
    val maxDim = inspection.boundingBox.maxDimension
    val fitScale = remember(maxDim) {
        if (maxDim > 0.00001f) (4.6f / maxDim) else 1.0f
    }
    val centerOffset = remember(inspection.boundingBox.center) {
        inspection.boundingBox.center
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("glb_import_preview_dialog"),
            color = Color(0xFF0B0F17)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. TOP HEADER BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFF141A24))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = inspection.modelName.ifBlank { inspection.rawMetadata.fileName },
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1F6FEB))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "3D GLB Importer",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${inspection.rawMetadata.fileName} • ${inspection.rawMetadata.fileSizeFormatted} • ${inspection.mesh.vertexCount} Verts • ${inspection.mesh.triangleCount} Tris • ${inspection.boneCount} Tulang • ${animations.size} Animasi",
                                color = Color(0xFF8B949E),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Header Action Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        OutlinedButton(
                            onClick = {
                                val fullInfo = inspection.formatFullInfoString()
                                viewModel.copyToClipboard(context, fullInfo, "Info Lengkap GLB")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("header_copy_glb_info_button"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Salin Info",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("close_glb_dialog_btn")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 2. MAIN SPLIT CONTENT
                // Sisi Kiri: KOTAK 1:1 VIEWPORT (Murni Canvas 3D, tanpa pengatur di dalam viewport, gestur 2-jari saja)
                // Sisi Kanan: SEMUA PENGONTROL TERATUR & EXPANDED
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // SISI KIRI: Kotak 1:1 Viewport Murni
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070A0F))
                            .border(1.5.dp, Color(0xFF1E2838), RoundedCornerShape(8.dp))
                            .pointerInput(twoFingerMode, invertX, invertY) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var prevDistance = 0f
                                    var prevCenter = Offset.Zero

                                    do {
                                        val event = awaitPointerEvent()
                                        val activePointers = event.changes.filter { it.pressed }

                                        // HANYA merespons jika 2 jari (atau lebih) aktif!
                                        if (activePointers.size >= 2) {
                                            val p1 = activePointers[0].position
                                            val p2 = activePointers[1].position
                                            val currentDist = hypot(p1.x - p2.x, p1.y - p2.y)
                                            val currentCenter = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                                            if (prevDistance > 0f) {
                                                // Pinch zoom (perbesar / perkecil)
                                                val zoom = currentDist / prevDistance
                                                if (zoom > 0.01f && zoom < 100f) {
                                                    distanceState = (distanceState / zoom).coerceIn(1.0f, 35.0f)
                                                    previewCamera.distance = distanceState
                                                }

                                                // Geser 2-jari (Pan atau Orbit)
                                                val panDelta = currentCenter - prevCenter
                                                if (panDelta != Offset.Zero) {
                                                    val signX = if (invertX) -1f else 1f
                                                    val signY = if (invertY) -1f else 1f
                                                    if (twoFingerMode == "pan") {
                                                        val factor = 0.005f * (distanceState / 4f)
                                                        previewCamera.pan(-panDelta.x * factor * signX, panDelta.y * factor * signY)
                                                    } else {
                                                        val factor = 0.4f
                                                        yawState = (yawState + panDelta.x * factor * signX) % 360f
                                                        pitchState = (pitchState - panDelta.y * factor * signY).coerceIn(-85f, 85f)
                                                        previewCamera.yawDegrees = yawState
                                                        previewCamera.pitchDegrees = pitchState
                                                    }
                                                    cameraRevision++
                                                }
                                            }

                                            prevDistance = currentDist
                                            prevCenter = currentCenter
                                            activePointers.forEach { it.consume() }
                                        } else {
                                            prevDistance = 0f
                                            prevCenter = Offset.Zero
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                    ) {
                        // RENDER CANVAS 3D (Murni tanpa overlay pengatur/chip di dalam viewport)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            cameraRevision.let {} // Memastikan Canvas recompose ketika kamera digeser/diputar
                            val w = size.width
                            val h = size.height
                            if (w <= 0f || h <= 0f) return@Canvas

                            val aspect = w / h
                            val viewMatrix = previewCamera.getViewMatrix()
                            val projMatrix = previewCamera.getProjectionMatrix(aspect)
                            val viewProj = projMatrix * viewMatrix

                            // 1. Gambar Grid 3D Lantai (y = 0)
                            if (showGrid) {
                                val gridColor = Color(0xFF1E2838)
                                val gridCenterColor = Color(0xFF00E5FF).copy(alpha = 0.5f)
                                val range = 4
                                val spacing = 0.6f

                                for (i in -range..range) {
                                    val pStart = viewProj.transformPoint(Vec3(i * spacing, -1.6f, -range * spacing))
                                    val pEnd = viewProj.transformPoint(Vec3(i * spacing, -1.6f, range * spacing))

                                    if (pStart.z in -1.5f..1.5f && pEnd.z in -1.5f..1.5f) {
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

                                    val zStart = viewProj.transformPoint(Vec3(-range * spacing, -1.6f, i * spacing))
                                    val zEnd = viewProj.transformPoint(Vec3(range * spacing, -1.6f, i * spacing))
                                    if (zStart.z in -1.5f..1.5f && zEnd.z in -1.5f..1.5f) {
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

                            // 2. Render Mesh Model GLB
                            val mesh = inspection.mesh
                            val baseColor = Color(inspection.material.albedoColor)
                            val lightDir = Vec3(0.5f, 0.9f, 0.6f).normalized()

                            val vertCount = mesh.vertices.size
                            val projX = FloatArray(vertCount)
                            val projY = FloatArray(vertCount)
                            val projZ = FloatArray(vertCount)

                            for (vi in 0 until vertCount) {
                                val v = mesh.vertices[vi]
                                // Normalisasi centering dan autofit agar model terlihat jelas
                                val centeredPos = (v.position - centerOffset) * fitScale
                                val ndc = viewProj.transformPoint(centeredPos)
                                projX[vi] = (ndc.x * 0.5f + 0.5f) * w
                                projY[vi] = (-ndc.y * 0.5f + 0.5f) * h
                                projZ[vi] = ndc.z
                            }

                            val triCount = mesh.triangles.size
                            val triStep = if (triCount > 1500) maxOf(1, triCount / 1200) else 1

                            if (isWireframe) {
                                val wfColor = Color(0xFF00E5FF).copy(alpha = 0.8f)
                                val wfPath = Path()
                                for (ti in 0 until triCount step triStep) {
                                    val tri = mesh.triangles[ti]
                                    val z0 = projZ[tri.i0]
                                    val z1 = projZ[tri.i1]
                                    val z2 = projZ[tri.i2]
                                    if (z0 !in -1.5f..1.5f && z1 !in -1.5f..1.5f && z2 !in -1.5f..1.5f) continue

                                    wfPath.moveTo(projX[tri.i0], projY[tri.i0])
                                    wfPath.lineTo(projX[tri.i1], projY[tri.i1])
                                    wfPath.lineTo(projX[tri.i2], projY[tri.i2])
                                    wfPath.close()
                                }
                                drawPath(wfPath, color = wfColor, style = Stroke(width = 1f))
                            } else {
                                val triPath = Path()
                                for (ti in 0 until triCount step triStep) {
                                    val tri = mesh.triangles[ti]
                                    val z0 = projZ[tri.i0]
                                    val z1 = projZ[tri.i1]
                                    val z2 = projZ[tri.i2]

                                    if (z0 < -1.5f || z0 > 1.5f || z1 < -1.5f || z1 > 1.5f || z2 < -1.5f || z2 > 1.5f) continue

                                    val x0 = projX[tri.i0]
                                    val y0 = projY[tri.i0]
                                    val x1 = projX[tri.i1]
                                    val y1 = projY[tri.i1]
                                    val x2 = projX[tri.i2]
                                    val y2 = projY[tri.i2]

                                    if ((x0 < -100f && x1 < -100f && x2 < -100f) ||
                                        (x0 > w + 100f && x1 > w + 100f && x2 > w + 100f) ||
                                        (y0 < -100f && y1 < -100f && y2 < -100f) ||
                                        (y0 > h + 100f && y1 > h + 100f && y2 > h + 100f)
                                    ) continue

                                    val v0 = mesh.vertices[tri.i0]
                                    val normal = v0.normal
                                    val lightIntensity = (abs(normal.dot(lightDir)) * 0.65f + 0.35f).coerceIn(0.25f, 1.0f)
                                    val shadeColor = baseColor.copy(
                                        red = (baseColor.red * lightIntensity).coerceIn(0f, 1f),
                                        green = (baseColor.green * lightIntensity).coerceIn(0f, 1f),
                                        blue = (baseColor.blue * lightIntensity).coerceIn(0f, 1f),
                                        alpha = 1.0f
                                    )

                                    triPath.reset()
                                    triPath.moveTo(x0, y0)
                                    triPath.lineTo(x1, y1)
                                    triPath.lineTo(x2, y2)
                                    triPath.close()
                                    drawPath(triPath, color = shadeColor)
                                }
                            }

                            // 3. Visualisasi Sendi Tulang (Skeleton Bones)
                            if (showBones && inspection.boneCount > 0) {
                                val boneJointColor = Color(0xFFFFD54F)
                                fun drawBoneJoints(node: GlbNodeInfo) {
                                    if (node.type == GlbNodeType.BONE) {
                                        val bp = (node.translation - centerOffset) * fitScale
                                        val p = viewProj.transformPoint(bp)
                                        if (p.z in -1.2f..1.2f) {
                                            val bx = (p.x * 0.5f + 0.5f) * w
                                            val by = (-p.y * 0.5f + 0.5f) * h
                                            drawCircle(color = boneJointColor, radius = 4f, center = Offset(bx, by))
                                        }
                                    }
                                    node.children.forEach { drawBoneJoints(it) }
                                }
                                inspection.rootNodes.forEach { drawBoneJoints(it) }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // SISI KANAN: SEMUA PENGONTROL TERATUR & EXPANDED
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF0E131C), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1B2433), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        // 1. KONTROL TAMPILAN, KAMERA & GESTUR 2-JARI
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141A24)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF212B3B)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Row 1: Tampilan & Presets Sudut Pandang
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilterChip(
                                            selected = isWireframe,
                                            onClick = { isWireframe = !isWireframe },
                                            label = { Text("Wireframe", fontSize = 9.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF00E5FF),
                                                selectedLabelColor = Color.Black
                                            ),
                                            modifier = Modifier.height(24.dp)
                                        )
                                        FilterChip(
                                            selected = showGrid,
                                            onClick = { showGrid = !showGrid },
                                            label = { Text("Grid", fontSize = 9.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF00E5FF),
                                                selectedLabelColor = Color.Black
                                            ),
                                            modifier = Modifier.height(24.dp)
                                        )
                                        if (inspection.boneCount > 0) {
                                            FilterChip(
                                                selected = showBones,
                                                onClick = { showBones = !showBones },
                                                label = { Text("Tulang", fontSize = 9.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFFFD54F),
                                                    selectedLabelColor = Color.Black
                                                ),
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { setFrontView() },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(24.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Depan", fontSize = 9.sp, color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = { setTopView() },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(24.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Atas", fontSize = 9.sp, color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = { setIsometricView() },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(24.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Iso", fontSize = 9.sp, color = Color.White)
                                        }
                                        IconButton(
                                            onClick = { resetCameraView() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Auto-Fit / Reset Kamera",
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                // Row 2: Mode Gestur 2-Jari & Invert
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Gestur 2-Jari:", fontSize = 9.sp, color = Color(0xFF8B949E))
                                        FilterChip(
                                            selected = twoFingerMode == "pan",
                                            onClick = { twoFingerMode = "pan" },
                                            label = { Text("Geser (Pan)", fontSize = 9.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF1F6FEB),
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.height(22.dp)
                                        )
                                        FilterChip(
                                            selected = twoFingerMode == "orbit",
                                            onClick = { twoFingerMode = "orbit" },
                                            label = { Text("Putar (Orbit)", fontSize = 9.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF1F6FEB),
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.height(22.dp)
                                        )
                                        Text("• Cubit (Pinch) = Zoom", fontSize = 8.5.sp, color = Color(0xFF00E5FF))
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilterChip(
                                            selected = invertX,
                                            onClick = { invertX = !invertX },
                                            label = { Text("Inv X", fontSize = 8.5.sp) },
                                            modifier = Modifier.height(20.dp)
                                        )
                                        FilterChip(
                                            selected = invertY,
                                            onClick = { invertY = !invertY },
                                            label = { Text("Inv Y", fontSize = 8.5.sp) },
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. PEMUTAR ANIMASI (JIKA MODEL MEMILIKI TREK ANIMASI)
                        if (animations.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141A24)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF212B3B)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { isPlayingAnim = !isPlayingAnim },
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlayingAnim) Color(0xFF00E5FF) else Color(0xFF238636))
                                            .testTag("glb_preview_play_pause_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAnim) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = if (isPlayingAnim) Color.Black else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    Box {
                                        OutlinedButton(
                                            onClick = { animDropdownExpanded = true },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier
                                                .height(26.dp)
                                                .testTag("glb_preview_anim_dropdown_btn")
                                        ) {
                                            Text(
                                                text = currentAnim?.name ?: "Animasi",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }

                                        DropdownMenu(
                                            expanded = animDropdownExpanded,
                                            onDismissRequest = { animDropdownExpanded = false }
                                        ) {
                                            animations.forEachIndexed { idx, anim ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "${idx + 1}. ${anim.name} (${String.format(Locale.US, "%.2fs", anim.duration)})",
                                                            fontSize = 11.sp
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

                                    Spacer(Modifier.width(6.dp))

                                    val duration = currentAnim?.duration ?: 1f
                                    Text(
                                        text = String.format(Locale.US, "%.2fs", animCurrentTime),
                                        fontSize = 9.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                    Slider(
                                        value = animCurrentTime.coerceIn(0f, duration),
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
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                            .height(24.dp)
                                            .testTag("glb_preview_anim_scrubber_slider")
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2fs", duration),
                                        fontSize = 9.sp,
                                        color = Color(0xFF8B949E)
                                    )

                                    Spacer(Modifier.width(4.dp))

                                    OutlinedButton(
                                        onClick = {
                                            animSpeed = when (animSpeed) {
                                                1.0f -> 2.0f
                                                2.0f -> 0.5f
                                                else -> 1.0f
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(26.dp)
                                            .testTag("glb_preview_anim_speed_btn")
                                    ) {
                                        Text("${animSpeed}x", fontSize = 9.sp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // 3. TAB HEADER
                        TabRow(
                            selectedTabIndex = selectedRightTab,
                            containerColor = Color(0xFF141A24),
                            contentColor = Color(0xFF00E5FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Tab(
                                selected = selectedRightTab == 0,
                                onClick = { selectedRightTab = 0 },
                                text = { Text("Info Lengkap", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedRightTab == 1,
                                onClick = { selectedRightTab = 1 },
                                text = { Text("Pengaturan", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            Tab(
                                selected = selectedRightTab == 2,
                                onClick = { selectedRightTab = 2 },
                                text = { Text("Hierarki & Tulang", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                            Tab(
                                selected = selectedRightTab == 3,
                                onClick = { selectedRightTab = 3 },
                                text = { Text("Animasi (${animations.size})", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // 4. TAB CONTENT CONTAINER (EXPANDED DENGAN weight 1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0A0E14))
                                .border(1.dp, Color(0xFF1B2433), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            when (selectedRightTab) {
                                0 -> ModelFullInfoTab(
                                    inspection = inspection,
                                    onCopyFullInfo = {
                                        viewModel.copyToClipboard(
                                            context,
                                            inspection.formatFullInfoString(),
                                            "Info Lengkap GLB"
                                        )
                                    }
                                )
                                1 -> ImportSettingsTab(
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
                                    },
                                    onCopyFullGlbInfo = {
                                        viewModel.copyToClipboard(
                                            context,
                                            inspection.formatFullInfoString(),
                                            "Spesifikasi Lengkap GLB"
                                        )
                                    }
                                )
                                2 -> NodeTreeAndSkeletonTab(inspection)
                                3 -> AnimationsListTab(
                                    animations = animations,
                                    selectedAnimIndex = selectedAnimIndex,
                                    onSelectAnimation = {
                                        selectedAnimIndex = it
                                        animCurrentTime = 0f
                                        isPlayingAnim = true
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // 5. ACTION BUTTONS FOOTER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("cancel_glb_import_button"),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Batal", fontSize = 11.sp, color = Color(0xFFE6EDF3))
                            }

                            OutlinedButton(
                                onClick = {
                                    inspection.file?.let { viewModel.importGlbToAssets(it) }
                                    onDismiss()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(36.dp)
                                    .testTag("save_glb_to_assets_only_button"),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF8B949E))
                                Spacer(Modifier.width(4.dp))
                                Text("Simpan ke Assets", fontSize = 11.sp, color = Color(0xFFE6EDF3), maxLines = 1)
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
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1.7f)
                                    .height(36.dp)
                                    .testTag("confirm_spawn_glb_button"),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Spawn ke Scene 3D ★", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 0: Informasi Lengkap Model GLB.
 * Menampilkan seluruh spesifikasi file, geometri & mesh, material & PBR shading,
 * skeleton rigging, animasi tertanam, dan rekomendasi engine.
 */
@Composable
private fun ModelFullInfoTab(
    inspection: GlbInspectionResult,
    onCopyFullInfo: () -> Unit
) {
    var showRawText by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Prominent Copy Button Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCopyFullInfo,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("tab_copy_full_glb_info_btn")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Salin Info Lengkap Model (untuk AI)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.width(8.dp))

            OutlinedButton(
                onClick = { showRawText = !showRawText },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(if (showRawText) "Kartu UI" else "Teks Mentah", fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (showRawText) {
            // Raw text view (Monospace syntax card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070A0F)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF212B3B)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = inspection.formatFullInfoString(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFFC9D1D9),
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        } else {
            // 1. IDENTITAS FILE & MODEL
            InfoSectionCard(title = "IDENTITAS FILE & MODEL", icon = Icons.Default.Folder) {
                InfoItemRow("Nama File", "${inspection.rawMetadata.fileName} (${inspection.rawMetadata.fileSizeFormatted})")
                InfoItemRow("Nama Model", inspection.modelName.ifBlank { "Casual_Body" })
                inspection.file?.let {
                    InfoItemRow("Lokasi Path", it.absolutePath)
                }
            }

            Spacer(Modifier.height(6.dp))

            // 2. GEOMETRI & MESH
            InfoSectionCard(title = "GEOMETRI & MESH", icon = Icons.Default.ViewInAr) {
                InfoItemRow("Total Vertices", "${inspection.mesh.vertexCount}")
                InfoItemRow("Total Triangles", "${inspection.mesh.triangleCount}")
                InfoItemRow("Jumlah Submesh", "${inspection.rawMetadata.meshCount}")
                val size = inspection.boundingBox.size
                InfoItemRow(
                    "Bounding Box Size",
                    "W: ${String.format(Locale.US, "%.2f", size.x)}m × H: ${String.format(Locale.US, "%.2f", size.y)}m × D: ${String.format(Locale.US, "%.2f", size.z)}m"
                )
                InfoItemRow("Dimensi Terbesar", "${String.format(Locale.US, "%.2f", inspection.boundingBox.maxDimension)}m")
                val center = inspection.boundingBox.center
                InfoItemRow(
                    "Titik Tengah (Center)",
                    "(${String.format(Locale.US, "%.2f", center.x)}, ${String.format(Locale.US, "%.2f", center.y)}, ${String.format(Locale.US, "%.2f", center.z)})"
                )
                InfoItemRow(
                    "Rekomendasi Skala",
                    "${String.format(Locale.US, "%.3fx", inspection.suggestedScale)} (Normalisasi ke ~1.8m)",
                    valueColor = Color(0xFF00E5FF)
                )
            }

            Spacer(Modifier.height(6.dp))

            // 3. MATERIAL & PBR SHADING
            InfoSectionCard(title = "MATERIAL & PBR SHADING", icon = Icons.Default.Palette) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Warna Dasar (BaseColor):", fontSize = 10.sp, color = Color(0xFF8B949E))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(inspection.material.albedoColor))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = inspection.rawMetadata.baseColorHex,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                InfoItemRow("Roughness", "${inspection.rawMetadata.roughness}")
                InfoItemRow("Metallic", "${inspection.rawMetadata.metallic}")
                InfoItemRow("Jumlah Material", "${inspection.rawMetadata.materialCount}")
                InfoItemRow("Jumlah Tekstur", "${inspection.rawMetadata.textureCount}")
            }

            Spacer(Modifier.height(6.dp))

            // 4. SKELETON & RIGGING TULANG
            InfoSectionCard(title = "SKELETON & RIGGING TULANG", icon = Icons.Default.AccountTree) {
                InfoItemRow("Jumlah Tulang (Bones)", "${inspection.boneCount}", valueColor = if (inspection.boneCount > 0) Color(0xFFFFD54F) else Color.White)
                InfoItemRow("Total Nodes", "${inspection.totalNodesCount}")
                val boneNames = mutableListOf<String>()
                fun collectBoneNames(node: GlbNodeInfo) {
                    if (node.type == GlbNodeType.BONE) boneNames.add(node.name)
                    node.children.forEach { collectBoneNames(it) }
                }
                inspection.rootNodes.forEach { collectBoneNames(it) }
                if (boneNames.isNotEmpty()) {
                    val previewBones = boneNames.take(8).joinToString(", ")
                    val moreBones = if (boneNames.size > 8) " ... (+${boneNames.size - 8} lainnya)" else ""
                    InfoItemRow("Daftar Tulang", "$previewBones$moreBones")
                }
            }

            Spacer(Modifier.height(6.dp))

            // 5. KLIP ANIMASI TERTANAM
            InfoSectionCard(title = "KLIP ANIMASI TERTANAM", icon = Icons.Default.Animation) {
                InfoItemRow("Jumlah Trek Animasi", "${inspection.animations.size}")
                if (inspection.animations.isEmpty()) {
                    Text(
                        "• Model Statis / Tanpa Klip Animasi",
                        fontSize = 10.sp,
                        color = Color(0xFF8B949E),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                } else {
                    inspection.animations.take(4).forEachIndexed { idx, anim ->
                        Text(
                            text = "${idx + 1}. \"${anim.name}\" (${String.format(Locale.US, "%.2fs", anim.duration)}, ${anim.channelsCount} Saluran)",
                            fontSize = 10.sp,
                            color = Color(0xFFC9D1D9),
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                    if (inspection.animations.size > 4) {
                        Text(
                            text = "+${inspection.animations.size - 4} animasi lainnya di tab Animasi",
                            fontSize = 9.sp,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 6. REKOMENDASI PENGGUNAAN ENGINE
            InfoSectionCard(title = "REKOMENDASI PENGGUNAAN ENGINE", icon = Icons.Default.Info) {
                InfoItemRow("Peran Rekomendasi", inspection.rawMetadata.recommendedRole, valueColor = Color(0xFF00E5FF))
                InfoItemRow("Folder Penempatan", inspection.rawMetadata.recommendedFolder)
                Text(
                    text = "• ${inspection.rawMetadata.recommendationTip}",
                    fontSize = 10.sp,
                    color = Color(0xFF8B949E),
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2B3E)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            }
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun InfoItemRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            fontSize = 10.sp,
            color = Color(0xFF8B949E),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.weight(1.3f)
        )
    }
}

/**
 * Tab 1: Pengaturan Impor & Konfigurasi Spawn
 */
@Composable
private fun ImportSettingsTab(
    inspection: GlbInspectionResult,
    modelScale: Float,
    onScaleChange: (Float) -> Unit,
    selectedCollider: ColliderShape,
    onColliderChange: (ColliderShape) -> Unit,
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    onCopyPath: () -> Unit,
    onCopyFullGlbInfo: () -> Unit
) {
    var customScaleText by remember(modelScale) {
        mutableStateOf(String.format(Locale.US, "%.3f", modelScale))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Skala Model 3D
        Text("1. Skala Model 3D", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Faktor Skala: ${String.format(Locale.US, "%.3fx", modelScale)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )

            // Tombol Normalisasi Cepat
            OutlinedButton(
                onClick = { onScaleChange(inspection.suggestedScale) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text("Normalisasi (${String.format(Locale.US, "%.1fx", inspection.suggestedScale)})", fontSize = 9.sp)
            }
        }

        // Preset Tombol Skala Cepat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Pair("1.0x (Asli)", 1.0f),
                Pair("Auto (${String.format(Locale.US, "%.1fx", inspection.suggestedScale)})", inspection.suggestedScale),
                Pair("0.1x", 0.1f),
                Pair("2.0x", 2.0f),
                Pair("5.0x", 5.0f),
                Pair("10x", 10.0f),
                Pair("100x", 100.0f)
            ).forEach { (label, sc) ->
                FilterChip(
                    selected = abs(modelScale - sc) < 0.01f,
                    onClick = { onScaleChange(sc) },
                    label = { Text(label, fontSize = 9.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }

        // Slider dengan jangkauan dinamis yang mendukung model berskala besar maupun kecil
        val maxSlider = remember(inspection.suggestedScale) {
            maxOf(200f, inspection.suggestedScale * 1.5f)
        }
        Slider(
            value = modelScale.coerceIn(0.001f, maxSlider),
            onValueChange = onScaleChange,
            valueRange = 0.001f..maxSlider,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00E5FF),
                activeTrackColor = Color(0xFF00E5FF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Custom numerical input field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Input Skala Tepat:", fontSize = 10.sp, color = Color(0xFF8B949E))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = customScaleText,
                onValueChange = { str ->
                    customScaleText = str
                    str.toFloatOrNull()?.let { onScaleChange(it) }
                },
                singleLine = true,
                modifier = Modifier
                    .width(100.dp)
                    .height(42.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF263040)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White)
            )
        }

        Spacer(Modifier.height(6.dp))

        val size = inspection.boundingBox.size
        Text(
            text = "Dimensi di Scene 3D: W: ${String.format(Locale.US, "%.2f", size.x * modelScale)}m × H: ${String.format(Locale.US, "%.2f", size.y * modelScale)}m × D: ${String.format(Locale.US, "%.2f", size.z * modelScale)}m",
            fontSize = 10.sp,
            color = Color(0xFF8B949E)
        )

        Spacer(Modifier.height(10.dp))

        // 2. Collider Fisika
        Text("2. Collider Fisika", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Pair(ColliderShape.BOX, "Box AABB"),
                Pair(ColliderShape.SPHERE, "Sphere"),
                Pair(ColliderShape.PLANE, "Plane (Lantai)")
            ).forEach { (shape, label) ->
                FilterChip(
                    selected = selectedCollider == shape,
                    onClick = { onColliderChange(shape) },
                    label = { Text(label, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 3. Peran di Scene 3D
        Text("3. Peran Objek di Scene", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                "Kostum Karakter TPP (Dynamic Actor)",
                "Permukaan / Ground Statis",
                "3D Prop Fisika (Dynamic Prop)",
                "Item Koleksi / Interaktif"
            ).forEach { role ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onRoleChange(role) }
                        .background(if (selectedRole == role) Color(0xFF1B2C44) else Color(0xFF131A26))
                        .border(
                            1.dp,
                            if (selectedRole == role) Color(0xFF00E5FF) else Color(0xFF1F2B3E),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedRole == role) Icons.Default.Check else Icons.Default.Tune,
                        contentDescription = null,
                        tint = if (selectedRole == role) Color(0xFF00E5FF) else Color(0xFF8B949E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = role,
                        fontSize = 11.sp,
                        fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedRole == role) Color(0xFF00E5FF) else Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4. Rekomendasi Godot / Engine
        Text("4. Folder Penempatan di Assets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = inspection.rawMetadata.recommendedFolder,
                fontSize = 11.sp,
                color = Color(0xFF00E5FF),
                fontFamily = FontFamily.Monospace
            )
            OutlinedButton(
                onClick = onCopyPath,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text("Salin Path", fontSize = 9.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // 5. Spesifikasi Lengkap Model GLB Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162234)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin Info Lengkap Model GLB", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Dapatkan seluruh spesifikasi GLB (Geometri, Vertices, Tris, Material PBR, Bounding Box, Daftar Tulang Skeleton, dan Trek Animasi) dalam 1 kali tap untuk panduan AI atau script game.",
                    fontSize = 10.sp,
                    color = Color(0xFF8B949E),
                    lineHeight = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onCopyFullGlbInfo,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .testTag("import_tab_copy_full_info_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin Info Lengkap GLB (untuk AI)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Tab 2: Pohon Hierarki Node & Tulang (Skeleton)
 */
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

/**
 * Tab 3: Daftar Trek Animasi Model GLB
 */
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
                                    text = "Durasi: ${String.format(Locale.US, "%.2f detik", anim.duration)} • ${anim.channelsCount} Saluran • ${anim.samplersCount} Samplers",
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
