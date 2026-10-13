package com.example.engine.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.engine.assets.AssetManager
import com.example.engine.math.Mat4
import com.example.engine.math.Vec3
import com.example.engine.model.Material
import com.example.engine.model.Scene
import com.example.engine.model.ShaderType
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class RenderStats(
    val drawCalls: Int = 0,
    val trianglesDrawn: Int = 0,
    val verticesProcessed: Int = 0,
    val fps: Int = 60
)

class RenderableTriangle {
    var p0X: Float = 0f
    var p0Y: Float = 0f
    var p1X: Float = 0f
    var p1Y: Float = 0f
    var p2X: Float = 0f
    var p2Y: Float = 0f
    var avgDepth: Float = 0f
    var color: Color = Color.White
    var isWireframe: Boolean = false
}

object EngineRenderer {

    private val reusablePath = Path()
    private val trianglePool = ArrayList<RenderableTriangle>(2048)

    // Reusable vertex cache: stride of 10 floats per vertex
    // [0]: scrX, [1]: scrY, [2]: depth, [3]: valid (1f/0f), [4]: normX, [5]: normY, [6]: normZ, [7]: worldX, [8]: worldY, [9]: worldZ
    private const val VERTEX_STRIDE = 10
    private var vertexBuffer = FloatArray(4096 * VERTEX_STRIDE)

    // Reusable scratch buffers for line projection (avoids Vec3 & Offset allocations)
    private val linePointsScratch = FloatArray(4)
    private val gizmoScratch = FloatArray(8)

    private fun ensureVertexCapacity(requiredVertices: Int) {
        val requiredSize = requiredVertices * VERTEX_STRIDE
        if (vertexBuffer.size < requiredSize) {
            vertexBuffer = FloatArray(max(vertexBuffer.size * 2, requiredSize))
        }
    }

    private fun obtainTriangle(index: Int): RenderableTriangle {
        return if (index < trianglePool.size) {
            trianglePool[index]
        } else {
            val tri = RenderableTriangle()
            trianglePool.add(tri)
            tri
        }
    }

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

        // 2. Draw 3D Ground Grid (zero object allocations)
        drawGrid(drawScope, viewProj, width, height)

        var totalTriangles = 0
        var totalVertices = 0
        var drawCalls = 0
        var triangleCount = 0

        val lightNorm = scene.lightDirection.normalized()
        val lightDirX = lightNorm.x
        val lightDirY = lightNorm.y
        val lightDirZ = lightNorm.z

        val eyePos = camera.getEyePosition()
        val eyeX = eyePos.x
        val eyeY = eyePos.y
        val eyeZ = eyePos.z

        // 3. Process entities for rendering
        for (entity in scene.entities) {
            if (!entity.isEnabled) continue
            val mr = entity.meshRenderer ?: continue
            if (!mr.isVisible) continue

            val mesh = assets.meshes[mr.meshId] ?: continue
            val material = assets.materials[mr.materialId] ?: Material.defaultMaterial()
            val shader = assets.shaders[material.shaderProgramId]

            val modelMatrix = entity.transform.getLocalMatrix()
            val mData = modelMatrix.data
            val mvp = viewProj * modelMatrix
            val mvpData = mvp.data

            drawCalls++
            totalVertices += mesh.vertices.size
            ensureVertexCapacity(mesh.vertices.size)

            val isWaterShader = shader?.type == ShaderType.WATER_WAVE
            val waveHeight = if (isWaterShader) (shader.uniforms["waveHeight"] ?: 0.12f) else 0f

            // Transform vertices into flat vertexBuffer
            for (vi in mesh.vertices.indices) {
                val v = mesh.vertices[vi]
                var lx = v.position.x
                var ly = v.position.y
                var lz = v.position.z

                // Water vertex displacement
                if (isWaterShader) {
                    val waveY = sin(lx * 2f + timeSeconds * 3f) * waveHeight +
                            cos(lz * 2f + timeSeconds * 2.5f) * waveHeight
                    ly += waveY
                }

                // World transform
                val wx = lx * mData[0] + ly * mData[1] + lz * mData[2] + mData[3]
                val wy = lx * mData[4] + ly * mData[5] + lz * mData[6] + mData[7]
                val wz = lx * mData[8] + ly * mData[9] + lz * mData[10] + mData[11]

                // Normal transform
                val nx = v.normal.x * mData[0] + v.normal.y * mData[1] + v.normal.z * mData[2]
                val ny = v.normal.x * mData[4] + v.normal.y * mData[5] + v.normal.z * mData[6]
                val nz = v.normal.x * mData[8] + v.normal.y * mData[9] + v.normal.z * mData[10]
                val nLen = sqrt(nx * nx + ny * ny + nz * nz)
                val invN = if (nLen > 0.0001f) 1f / nLen else 0f
                val wnx = nx * invN
                val wny = ny * invN
                val wnz = nz * invN

                // Project to clip space
                val cx = lx * mvpData[0] + ly * mvpData[1] + lz * mvpData[2] + mvpData[3]
                val cy = lx * mvpData[4] + ly * mvpData[5] + lz * mvpData[6] + mvpData[7]
                val cz = lx * mvpData[8] + ly * mvpData[9] + lz * mvpData[10] + mvpData[11]
                val cw = lx * mvpData[12] + ly * mvpData[13] + lz * mvpData[14] + mvpData[15]

                val offset = vi * VERTEX_STRIDE
                if (cw > 0.1f) {
                    val invW = 1f / cw
                    val ndcX = cx * invW
                    val ndcY = cy * invW
                    vertexBuffer[offset] = (ndcX + 1f) * 0.5f * width
                    vertexBuffer[offset + 1] = (1f - ndcY) * 0.5f * height
                    vertexBuffer[offset + 2] = cz * invW
                    vertexBuffer[offset + 3] = 1f // Valid
                } else {
                    vertexBuffer[offset + 3] = 0f // Clipped behind near plane
                }

                vertexBuffer[offset + 4] = wnx
                vertexBuffer[offset + 5] = wny
                vertexBuffer[offset + 6] = wnz
                vertexBuffer[offset + 7] = wx
                vertexBuffer[offset + 8] = wy
                vertexBuffer[offset + 9] = wz
            }

            // Build triangles into pool
            val isSelectedEntity = entity.id == selectedEntityId
            for (tri in mesh.triangles) {
                val o0 = tri.i0 * VERTEX_STRIDE
                val o1 = tri.i1 * VERTEX_STRIDE
                val o2 = tri.i2 * VERTEX_STRIDE

                if (vertexBuffer[o0 + 3] == 0f || vertexBuffer[o1 + 3] == 0f || vertexBuffer[o2 + 3] == 0f) {
                    continue
                }

                val p0X = vertexBuffer[o0]
                val p0Y = vertexBuffer[o0 + 1]
                val p1X = vertexBuffer[o1]
                val p1Y = vertexBuffer[o1 + 1]
                val p2X = vertexBuffer[o2]
                val p2Y = vertexBuffer[o2 + 1]

                // Screen-space 2D backface culling
                val edge1X = p1X - p0X
                val edge1Y = p1Y - p0Y
                val edge2X = p2X - p0X
                val edge2Y = p2Y - p0Y
                val cross = edge1X * edge2Y - edge1Y * edge2X

                if (!material.isDoubleSided && cross >= 0f) {
                    continue
                }

                val avgDepth = (vertexBuffer[o0 + 2] + vertexBuffer[o1 + 2] + vertexBuffer[o2 + 2]) * 0.33333334f

                // Normal and view vector for shading
                val normX = vertexBuffer[o0 + 4]
                val normY = vertexBuffer[o0 + 5]
                val normZ = vertexBuffer[o0 + 6]

                val worldX = vertexBuffer[o0 + 7]
                val worldY = vertexBuffer[o0 + 8]
                val worldZ = vertexBuffer[o0 + 9]

                var vdx = eyeX - worldX
                var vdy = eyeY - worldY
                var vdz = eyeZ - worldZ
                val vdLen = sqrt(vdx * vdx + vdy * vdy + vdz * vdz)
                val invVd = if (vdLen > 0.0001f) 1f / vdLen else 0f
                vdx *= invVd
                vdy *= invVd
                vdz *= invVd

                val shadedColor = computeFastShadedColor(
                    material = material,
                    shader = shader,
                    normX = normX, normY = normY, normZ = normZ,
                    lightX = lightDirX, lightY = lightDirY, lightZ = lightDirZ,
                    viewX = vdx, viewY = vdy, viewZ = vdz,
                    time = timeSeconds,
                    isSelected = isSelectedEntity
                )

                val pooledTri = obtainTriangle(triangleCount)
                pooledTri.p0X = p0X
                pooledTri.p0Y = p0Y
                pooledTri.p1X = p1X
                pooledTri.p1Y = p1Y
                pooledTri.p2X = p2X
                pooledTri.p2Y = p2Y
                pooledTri.avgDepth = avgDepth
                pooledTri.color = shadedColor
                pooledTri.isWireframe = material.isWireframe || renderWireframeOnly

                triangleCount++
                totalTriangles++
            }
        }

        // 4. Sort triangles by depth (Painter's algorithm: farthest first)
        if (triangleCount > 1) {
            val subList = trianglePool.subList(0, triangleCount)
            subList.sortByDescending { it.avgDepth }
        }

        // 5. Draw sorted triangles using single reusable path (zero GC pressure)
        for (i in 0 until triangleCount) {
            val tri = trianglePool[i]
            reusablePath.reset()
            reusablePath.moveTo(tri.p0X, tri.p0Y)
            reusablePath.lineTo(tri.p1X, tri.p1Y)
            reusablePath.lineTo(tri.p2X, tri.p2Y)
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

    private fun computeFastShadedColor(
        material: Material,
        shader: com.example.engine.model.ShaderProgram?,
        normX: Float, normY: Float, normZ: Float,
        lightX: Float, lightY: Float, lightZ: Float,
        viewX: Float, viewY: Float, viewZ: Float,
        time: Float,
        isSelected: Boolean
    ): Color {
        val baseR = ((material.albedoColor shr 16) and 0xFF) / 255f
        val baseG = ((material.albedoColor shr 8) and 0xFF) / 255f
        val baseB = (material.albedoColor and 0xFF) / 255f

        val nDotL = max(0f, normX * lightX + normY * lightY + normZ * lightZ)

        return when (shader?.type) {
            ShaderType.TOON_CEL -> {
                val bands = shader.uniforms["bands"] ?: 3.0f
                val bandFactor = max(0.2f, kotlin.math.floor(nDotL * bands + 0.5f) / bands)
                val nDotV = normX * viewX + normY * viewY + normZ * viewZ
                val rim = max(0f, 1f - nDotV)
                val rimBoost = if (rim > 0.7f) 0.35f else 0f
                val r = (baseR * bandFactor + rimBoost).coerceIn(0f, 1f)
                val g = (baseG * bandFactor + rimBoost).coerceIn(0f, 1f)
                val b = (baseB * bandFactor + rimBoost).coerceIn(0f, 1f)
                Color(r, g, b, 1f)
            }
            ShaderType.HOLOGRAPHIC -> {
                val nDotV = normX * viewX + normY * viewY + normZ * viewZ
                val rim = max(0f, 1f - nDotV)
                val scanline = sin(time * 8f) * 0.15f + 0.85f
                val r = (0.1f + rim * 0.8f * scanline).coerceIn(0f, 1f)
                val g = (0.9f * scanline).coerceIn(0f, 1f)
                Color(r, g, 1.0f, 0.9f)
            }
            ShaderType.WATER_WAVE -> {
                val shimmer = sin(time * 5f) * 0.15f + 0.85f
                val r = (baseR * shimmer).coerceIn(0f, 1f)
                val g = (baseG * shimmer).coerceIn(0f, 1f)
                val b = (baseB * shimmer).coerceIn(0f, 1f)
                Color(r, g, b, 0.85f)
            }
            else -> {
                // PBR Standard lighting with fast half-vector specular
                val diffuse = nDotL * 0.75f + 0.25f
                var hx = lightX + viewX
                var hy = lightY + viewY
                var hz = lightZ + viewZ
                val hLen = sqrt(hx * hx + hy * hy + hz * hz)
                val invH = if (hLen > 0.0001f) 1f / hLen else 0f
                hx *= invH
                hy *= invH
                hz *= invH

                val specDot = max(0f, normX * hx + normY * hy + normZ * hz)
                val specPower = (1f - material.roughness) * 32f + 2f
                val spec = specDot.toDouble().pow(specPower.toDouble()).toFloat() * material.metallic * 0.6f

                var r = (baseR * diffuse + spec).coerceIn(0f, 1f)
                var g = (baseG * diffuse + spec).coerceIn(0f, 1f)
                var b = (baseB * diffuse + spec).coerceIn(0f, 1f)

                if (isSelected) {
                    r = (r * 0.7f + 0.3f).coerceIn(0f, 1f)
                    g = (g * 0.7f + 0.3f).coerceIn(0f, 1f)
                    b = (b * 0.5f).coerceIn(0f, 1f)
                }

                Color(r, g, b, 1f)
            }
        }
    }

    private fun projectPointDirect(
        x: Float, y: Float, z: Float,
        mvp: Mat4,
        width: Float, height: Float,
        out: FloatArray,
        outOffset: Int
    ): Boolean {
        val d = mvp.data
        val cx = x * d[0] + y * d[1] + z * d[2] + d[3]
        val cy = x * d[4] + y * d[5] + z * d[6] + d[7]
        val cw = x * d[12] + y * d[13] + z * d[14] + d[15]
        if (cw <= 0.1f) return false

        val invW = 1f / cw
        val ndcX = cx * invW
        val ndcY = cy * invW
        out[outOffset] = (ndcX + 1f) * 0.5f * width
        out[outOffset + 1] = (1f - ndcY) * 0.5f * height
        return true
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
            val validA = projectPointDirect(-size, 0f, coord, mvp, width, height, linePointsScratch, 0)
            val validB = projectPointDirect(size, 0f, coord, mvp, width, height, linePointsScratch, 2)
            if (validA && validB) {
                drawScope.drawLine(
                    color = if (i == 0) centerAxisColor else gridColor,
                    start = Offset(linePointsScratch[0], linePointsScratch[1]),
                    end = Offset(linePointsScratch[2], linePointsScratch[3]),
                    strokeWidth = if (i == 0) 1.5f else 0.8f
                )
            }

            // Z-aligned line
            val validC = projectPointDirect(coord, 0f, -size, mvp, width, height, linePointsScratch, 0)
            val validD = projectPointDirect(coord, 0f, size, mvp, width, height, linePointsScratch, 2)
            if (validC && validD) {
                drawScope.drawLine(
                    color = if (i == 0) Color(0x66448AFF) else gridColor,
                    start = Offset(linePointsScratch[0], linePointsScratch[1]),
                    end = Offset(linePointsScratch[2], linePointsScratch[3]),
                    strokeWidth = if (i == 0) 1.5f else 0.8f
                )
            }
        }
    }

    private fun drawTransformGizmo(drawScope: DrawScope, origin: Vec3, mvp: Mat4, width: Float, height: Float) {
        val validOrigin = projectPointDirect(origin.x, origin.y, origin.z, mvp, width, height, gizmoScratch, 0)
        if (!validOrigin) return

        val oX = gizmoScratch[0]
        val oY = gizmoScratch[1]
        val o2D = Offset(oX, oY)

        // X Axis (Red)
        if (projectPointDirect(origin.x + 1.2f, origin.y, origin.z, mvp, width, height, gizmoScratch, 2)) {
            val x2D = Offset(gizmoScratch[2], gizmoScratch[3])
            drawScope.drawLine(Color(0xFFFF3366), o2D, x2D, strokeWidth = 4f)
            drawScope.drawCircle(Color(0xFFFF3366), radius = 6f, center = x2D)
        }

        // Y Axis (Green)
        if (projectPointDirect(origin.x, origin.y + 1.2f, origin.z, mvp, width, height, gizmoScratch, 2)) {
            val y2D = Offset(gizmoScratch[2], gizmoScratch[3])
            drawScope.drawLine(Color(0xFF00E676), o2D, y2D, strokeWidth = 4f)
            drawScope.drawCircle(Color(0xFF00E676), radius = 6f, center = y2D)
        }

        // Z Axis (Blue)
        if (projectPointDirect(origin.x, origin.y, origin.z + 1.2f, mvp, width, height, gizmoScratch, 2)) {
            val z2D = Offset(gizmoScratch[2], gizmoScratch[3])
            drawScope.drawLine(Color(0xFF2979FF), o2D, z2D, strokeWidth = 4f)
            drawScope.drawCircle(Color(0xFF2979FF), radius = 6f, center = z2D)
        }

        // Center origin ring
        drawScope.drawCircle(Color.White, radius = 4f, center = o2D)
    }
}
