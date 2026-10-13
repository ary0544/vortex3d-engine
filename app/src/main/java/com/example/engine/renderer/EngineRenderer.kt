package com.example.engine.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.engine.assets.AssetManager
import com.example.engine.math.Mat4
import com.example.engine.math.MathUtils
import com.example.engine.math.Vec3
import com.example.engine.model.Entity
import com.example.engine.model.Material
import com.example.engine.model.Mesh
import com.example.engine.model.Scene
import com.example.engine.model.ShaderType
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

data class RenderStats(
    val drawCalls: Int = 0,
    val trianglesDrawn: Int = 0,
    val verticesProcessed: Int = 0,
    val fps: Int = 60
)

data class ProjectedTriangle(
    val p0: Offset,
    val p1: Offset,
    val p2: Offset,
    val avgDepth: Float,
    val color: Color,
    val isWireframe: Boolean = false
)

object EngineRenderer {

    private val reusablePath = Path()
    private val triangleBuffer = ArrayList<ProjectedTriangle>(512)

    fun renderScene(
        drawScope: DrawScope,
        scene: Scene,
        camera: Camera3D,
        assets: AssetManager,
        selectedEntityId: String?,
        timeSeconds: Float,
        renderWireframeOnly: Boolean = false
    ): RenderStats {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0f || height <= 0f) return RenderStats()

        val aspect = width / height
        val viewMatrix = camera.getViewMatrix()
        val projMatrix = camera.getProjectionMatrix(aspect)
        val viewProj = projMatrix * viewMatrix

        // 1. Draw Background Sky
        drawScope.drawRect(
            color = Color(scene.skyColor),
            size = drawScope.size
        )

        // 2. Draw 3D Ground Grid
        drawGrid(drawScope, viewProj, width, height)

        var totalTriangles = 0
        var totalVertices = 0
        var drawCalls = 0
        triangleBuffer.clear()

        val lightDir = scene.lightDirection.normalized()
        val eyePos = camera.getEyePosition()

        // 3. Process entities for rendering
        for (entity in scene.entities) {
            if (!entity.isEnabled) continue
            val mr = entity.meshRenderer ?: continue
            if (!mr.isVisible) continue

            val mesh = assets.meshes[mr.meshId] ?: continue
            val material = assets.materials[mr.materialId] ?: Material.defaultMaterial()
            val shader = assets.shaders[material.shaderProgramId]

            val modelMatrix = entity.transform.getLocalMatrix()
            val mvp = viewProj * modelMatrix

            drawCalls++
            totalVertices += mesh.vertices.size

            // Transform vertices
            val transformedVerts = FloatArray(mesh.vertices.size * 4) // x, y, depth, screenValid (1 or 0)
            val worldPositions = arrayOfNulls<Vec3>(mesh.vertices.size)
            val worldNormals = arrayOfNulls<Vec3>(mesh.vertices.size)

            for (vi in mesh.vertices.indices) {
                val v = mesh.vertices[vi]
                var localPos = v.position

                // Water shader vertex displacement
                if (shader?.type == ShaderType.WATER_WAVE) {
                    val waveH = shader.uniforms["waveHeight"] ?: 0.12f
                    val waveY = sin(localPos.x * 2f + timeSeconds * 3f) * waveH + cos(localPos.z * 2f + timeSeconds * 2.5f) * waveH
                    localPos = Vec3(localPos.x, localPos.y + waveY, localPos.z)
                }

                val worldP = modelMatrix.transformPoint(localPos)
                val worldN = modelMatrix.transformDirection(v.normal).normalized()
                worldPositions[vi] = worldP
                worldNormals[vi] = worldN

                // Project to clip space
                val x = localPos.x * mvp.data[0] + localPos.y * mvp.data[1] + localPos.z * mvp.data[2] + mvp.data[3]
                val y = localPos.x * mvp.data[4] + localPos.y * mvp.data[5] + localPos.z * mvp.data[6] + mvp.data[7]
                val z = localPos.x * mvp.data[8] + localPos.y * mvp.data[9] + localPos.z * mvp.data[10] + mvp.data[11]
                val w = localPos.x * mvp.data[12] + localPos.y * mvp.data[13] + localPos.z * mvp.data[14] + mvp.data[15]

                val offset = vi * 4
                if (w > 0.1f) {
                    val ndcX = x / w
                    val ndcY = y / w
                    val scrX = (ndcX + 1f) * 0.5f * width
                    val scrY = (1f - ndcY) * 0.5f * height
                    transformedVerts[offset] = scrX
                    transformedVerts[offset + 1] = scrY
                    transformedVerts[offset + 2] = z / w // Depth
                    transformedVerts[offset + 3] = 1f   // Valid
                } else {
                    transformedVerts[offset + 3] = 0f   // Clipped behind camera
                }
            }

            // Build triangles
            for (tri in mesh.triangles) {
                val o0 = tri.i0 * 4
                val o1 = tri.i1 * 4
                val o2 = tri.i2 * 4

                if (transformedVerts[o0 + 3] == 0f || transformedVerts[o1 + 3] == 0f || transformedVerts[o2 + 3] == 0f) {
                    continue
                }

                val p0 = Offset(transformedVerts[o0], transformedVerts[o0 + 1])
                val p1 = Offset(transformedVerts[o1], transformedVerts[o1 + 1])
                val p2 = Offset(transformedVerts[o2], transformedVerts[o2 + 1])

                // Backface culling in screen space (2D cross product)
                val edge1X = p1.x - p0.x
                val edge1Y = p1.y - p0.y
                val edge2X = p2.x - p0.x
                val edge2Y = p2.y - p0.y
                val cross = edge1X * edge2Y - edge1Y * edge2X

                if (!material.isDoubleSided && cross >= 0f) {
                    continue // Cull backfacing
                }

                val avgDepth = (transformedVerts[o0 + 2] + transformedVerts[o1 + 2] + transformedVerts[o2 + 2]) / 3f

                // Calculate lighting and shader effect
                val norm = worldNormals[tri.i0] ?: Vec3.UP
                val worldP = worldPositions[tri.i0] ?: Vec3.ZERO
                val viewDir = (eyePos - worldP).normalized()

                val shadedColor = computeShadedColor(
                    material = material,
                    shader = shader,
                    normal = norm,
                    lightDir = lightDir,
                    viewDir = viewDir,
                    time = timeSeconds,
                    isSelected = entity.id == selectedEntityId
                )

                triangleBuffer.add(
                    ProjectedTriangle(
                        p0 = p0,
                        p1 = p1,
                        p2 = p2,
                        avgDepth = avgDepth,
                        color = shadedColor,
                        isWireframe = material.isWireframe || renderWireframeOnly
                    )
                )
                totalTriangles++
            }
        }

        // 4. Sort triangles by depth (Painter's algorithm: farthest first)
        triangleBuffer.sortByDescending { it.avgDepth }

        // 5. Draw sorted triangles using single reusable path (zero GC pressure)
        for (tri in triangleBuffer) {
            reusablePath.reset()
            reusablePath.moveTo(tri.p0.x, tri.p0.y)
            reusablePath.lineTo(tri.p1.x, tri.p1.y)
            reusablePath.lineTo(tri.p2.x, tri.p2.y)
            reusablePath.close()

            if (tri.isWireframe) {
                drawScope.drawPath(reusablePath, color = tri.color, style = Stroke(width = 1.5f))
            } else {
                drawScope.drawPath(reusablePath, color = tri.color)
            }
        }

        // 6. Draw Selection Gizmo and Bounding Indicator
        if (selectedEntityId != null) {
            val selected = scene.findEntity(selectedEntityId)
            if (selected != null) {
                drawTransformGizmo(drawScope, selected.transform.position, viewProj, width, height)
            }
        }

        return RenderStats(
            drawCalls = drawCalls,
            trianglesDrawn = totalTriangles,
            verticesProcessed = totalVertices,
            fps = 60
        )
    }

    private fun computeShadedColor(
        material: Material,
        shader: com.example.engine.model.ShaderProgram?,
        normal: Vec3,
        lightDir: Vec3,
        viewDir: Vec3,
        time: Float,
        isSelected: Boolean
    ): Color {
        val baseR = ((material.albedoColor shr 16) and 0xFF) / 255f
        val baseG = ((material.albedoColor shr 8) and 0xFF) / 255f
        val baseB = (material.albedoColor and 0xFF) / 255f

        val nDotL = max(0f, normal.dot(lightDir))

        return when (shader?.type) {
            ShaderType.TOON_CEL -> {
                val bands = shader.uniforms["bands"] ?: 3.0f
                val bandFactor = max(0.2f, kotlin.math.floor(nDotL * bands + 0.5f) / bands)
                val rim = max(0f, 1f - normal.dot(viewDir))
                val rimBoost = if (rim > 0.7f) 0.35f else 0f
                val r = (baseR * bandFactor + rimBoost).coerceIn(0f, 1f)
                val g = (baseG * bandFactor + rimBoost).coerceIn(0f, 1f)
                val b = (baseB * bandFactor + rimBoost).coerceIn(0f, 1f)
                Color(r, g, b, 1f)
            }
            ShaderType.HOLOGRAPHIC -> {
                val rim = max(0f, 1f - normal.dot(viewDir))
                val scanline = sin(time * 8f) * 0.15f + 0.85f
                val r = (0.1f + rim * 0.8f * scanline).coerceIn(0f, 1f)
                val g = (0.9f * scanline).coerceIn(0f, 1f)
                val b = 1.0f
                Color(r, g, b, 0.9f)
            }
            ShaderType.WATER_WAVE -> {
                val shimmer = sin(time * 5f) * 0.15f + 0.85f
                val r = (baseR * shimmer).coerceIn(0f, 1f)
                val g = (baseG * shimmer).coerceIn(0f, 1f)
                val b = (baseB * shimmer).coerceIn(0f, 1f)
                Color(r, g, b, 0.85f)
            }
            else -> {
                // PBR Standard lighting
                val diffuse = nDotL * 0.75f + 0.25f
                val halfVec = (lightDir + viewDir).normalized()
                val specDot = max(0f, normal.dot(halfVec))
                val specPower = (1f - material.roughness) * 32f + 2f
                val spec = specDot.toDouble().pow(specPower.toDouble()).toFloat() * material.metallic * 0.6f

                var r = (baseR * diffuse + spec).coerceIn(0f, 1f)
                var g = (baseG * diffuse + spec).coerceIn(0f, 1f)
                var b = (baseB * diffuse + spec).coerceIn(0f, 1f)

                // Selection highlight tint
                if (isSelected) {
                    r = (r * 0.7f + 0.3f).coerceIn(0f, 1f)
                    g = (g * 0.7f + 0.3f).coerceIn(0f, 1f)
                    b = (b * 0.5f).coerceIn(0f, 1f)
                }

                Color(r, g, b, 1f)
            }
        }
    }

    private fun drawGrid(drawScope: DrawScope, mvp: Mat4, width: Float, height: Float) {
        val gridLines = 10
        val size = 10f
        val step = size / gridLines
        val gridColor = Color(0x334FC3F7)
        val centerAxisColor = Color(0x66FF5252)

        for (i in -gridLines..gridLines) {
            val coord = i * step
            // X-aligned line
            val pA = projectPoint(Vec3(-size, 0f, coord), mvp, width, height)
            val pB = projectPoint(Vec3(size, 0f, coord), mvp, width, height)
            if (pA != null && pB != null) {
                drawScope.drawLine(if (i == 0) centerAxisColor else gridColor, pA, pB, strokeWidth = if (i == 0) 1.5f else 0.8f)
            }

            // Z-aligned line
            val pC = projectPoint(Vec3(coord, 0f, -size), mvp, width, height)
            val pD = projectPoint(Vec3(coord, 0f, size), mvp, width, height)
            if (pC != null && pD != null) {
                drawScope.drawLine(if (i == 0) Color(0x66448AFF) else gridColor, pC, pD, strokeWidth = if (i == 0) 1.5f else 0.8f)
            }
        }
    }

    private fun drawTransformGizmo(drawScope: DrawScope, origin: Vec3, mvp: Mat4, width: Float, height: Float) {
        val o2D = projectPoint(origin, mvp, width, height) ?: return

        // X Axis (Red)
        val x2D = projectPoint(origin + Vec3(1.2f, 0f, 0f), mvp, width, height)
        if (x2D != null) {
            drawScope.drawLine(Color(0xFFFF3366), o2D, x2D, strokeWidth = 4f)
            drawScope.drawCircle(Color(0xFFFF3366), radius = 6f, center = x2D)
        }

        // Y Axis (Green)
        val y2D = projectPoint(origin + Vec3(0f, 1.2f, 0f), mvp, width, height)
        if (y2D != null) {
            drawScope.drawLine(Color(0xFF00E676), o2D, y2D, strokeWidth = 4f)
            drawScope.drawCircle(Color(0xFF00E676), radius = 6f, center = y2D)
        }

        // Z Axis (Blue)
        val z2D = projectPoint(origin + Vec3(0f, 0f, 1.2f), mvp, width, height)
        if (z2D != null) {
            drawScope.drawLine(Color(0xFF2979FF), o2D, z2D, strokeWidth = 4f)
            drawScope.drawCircle(Color(0xFF2979FF), radius = 6f, center = z2D)
        }

        // Center origin ring
        drawScope.drawCircle(Color.White, radius = 4f, center = o2D)
    }

    private fun projectPoint(p: Vec3, mvp: Mat4, width: Float, height: Float): Offset? {
        val x = p.x * mvp.data[0] + p.y * mvp.data[1] + p.z * mvp.data[2] + mvp.data[3]
        val y = p.x * mvp.data[4] + p.y * mvp.data[5] + p.z * mvp.data[6] + mvp.data[7]
        val w = p.x * mvp.data[12] + p.y * mvp.data[13] + p.z * mvp.data[14] + mvp.data[15]
        if (w <= 0.1f) return null

        val ndcX = x / w
        val ndcY = y / w
        return Offset((ndcX + 1f) * 0.5f * width, (1f - ndcY) * 0.5f * height)
    }
}
