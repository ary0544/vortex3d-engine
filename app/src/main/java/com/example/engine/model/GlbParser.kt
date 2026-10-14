package com.example.engine.model

import com.example.engine.math.Vec3
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

data class BoundingBox3D(
    val min: Vec3,
    val max: Vec3,
    val center: Vec3,
    val size: Vec3
) {
    val maxDimension: Float
        get() = max(size.x, max(size.y, size.z))
}

enum class GlbNodeType {
    ROOT, NODE, MESH, BONE
}

data class GlbNodeInfo(
    val index: Int,
    val name: String,
    val type: GlbNodeType,
    val meshIndex: Int = -1,
    val skinIndex: Int = -1,
    val children: List<GlbNodeInfo> = emptyList(),
    val translation: Vec3 = Vec3.ZERO,
    val rotation: Vec3 = Vec3.ZERO,
    val scale: Vec3 = Vec3.ONE
)

data class GlbAnimationInfo(
    val index: Int,
    val name: String,
    val duration: Float, // durasi dalam detik
    val channelsCount: Int,
    val samplersCount: Int
)

data class GlbParseResult(
    val mesh: Mesh,
    val material: Material,
    val name: String,
    val vertexCount: Int,
    val triangleCount: Int
)

data class GlbMetadata(
    val fileName: String,
    val fileSizeFormatted: String,
    val modelName: String,
    val meshCount: Int,
    val vertexCount: Int,
    val triangleCount: Int,
    val materialCount: Int,
    val textureCount: Int,
    val animationCount: Int,
    val roughness: Float,
    val metallic: Float,
    val baseColorHex: String,
    val recommendedRole: String,
    val recommendedFolder: String,
    val recommendationTip: String
)

data class GlbInspectionResult(
    val mesh: Mesh,
    val material: Material,
    val modelName: String,
    val boundingBox: BoundingBox3D,
    val suggestedScale: Float,
    val rootNodes: List<GlbNodeInfo>,
    val totalNodesCount: Int,
    val boneCount: Int,
    val animations: List<GlbAnimationInfo>,
    val rawMetadata: GlbMetadata,
    val file: File? = null
) {
    fun formatFullInfoString(): String {
        return buildString {
            appendLine("=== INFORMASI LENGKAP MODEL 3D GLB ===")
            appendLine("Nama File: ${rawMetadata.fileName} (${rawMetadata.fileSizeFormatted})")
            appendLine("Nama Model: $modelName")
            file?.let { appendLine("Lokasi Path: ${it.absolutePath}") }
            appendLine()
            appendLine("[GEOMETRI & MESH]")
            appendLine("• Total Vertices: ${mesh.vertexCount}")
            appendLine("• Total Triangles: ${mesh.triangleCount}")
            appendLine("• Jumlah Submesh: ${rawMetadata.meshCount}")
            appendLine("• Bounding Box Size: W: ${String.format(java.util.Locale.US, "%.2f", boundingBox.size.x)}m × H: ${String.format(java.util.Locale.US, "%.2f", boundingBox.size.y)}m × D: ${String.format(java.util.Locale.US, "%.2f", boundingBox.size.z)}m")
            appendLine("• Dimensi Terbesar: ${String.format(java.util.Locale.US, "%.2f", boundingBox.maxDimension)}m")
            appendLine("• Titik Tengah (Center): (${String.format(java.util.Locale.US, "%.2f", boundingBox.center.x)}, ${String.format(java.util.Locale.US, "%.2f", boundingBox.center.y)}, ${String.format(java.util.Locale.US, "%.2f", boundingBox.center.z)})")
            appendLine("• Rekomendasi Skala Impor: ${String.format(java.util.Locale.US, "%.3fx", suggestedScale)}")
            appendLine()
            appendLine("[MATERIAL & PBR SHADING]")
            appendLine("• Warna Dasar (BaseColor): ${rawMetadata.baseColorHex}")
            appendLine("• Roughness: ${rawMetadata.roughness}")
            appendLine("• Metallic: ${rawMetadata.metallic}")
            appendLine("• Jumlah Material: ${rawMetadata.materialCount}")
            appendLine("• Jumlah Tekstur: ${rawMetadata.textureCount}")
            appendLine()
            appendLine("[SKELETON & RIGGING TULANG]")
            appendLine("• Jumlah Tulang (Bones): $boneCount")
            appendLine("• Total Nodes: $totalNodesCount")
            val boneNames = mutableListOf<String>()
            fun collectBoneNames(node: GlbNodeInfo) {
                if (node.type == GlbNodeType.BONE) boneNames.add(node.name)
                node.children.forEach { collectBoneNames(it) }
            }
            rootNodes.forEach { collectBoneNames(it) }
            if (boneNames.isNotEmpty()) {
                appendLine("• Daftar Tulang (${boneNames.size}): ${boneNames.take(15).joinToString(", ")}${if (boneNames.size > 15) " ... (+${boneNames.size - 15} lainnya)" else ""}")
            }
            appendLine()
            appendLine("[KLIP ANIMASI TERTANAM]")
            appendLine("• Jumlah Trek Animasi: ${animations.size}")
            if (animations.isEmpty()) {
                appendLine("• (Model Statis / Tanpa Klip Animasi)")
            } else {
                animations.forEachIndexed { i, anim ->
                    appendLine("  ${i + 1}. \"${anim.name}\" - Durasi: ${String.format(java.util.Locale.US, "%.2fs", anim.duration)}, Saluran: ${anim.channelsCount}, Samplers: ${anim.samplersCount}")
                }
            }
            appendLine()
            appendLine("[REKOMENDASI PENGGUNAAN ENGINE]")
            appendLine("• Peran Rekomendasi: ${rawMetadata.recommendedRole}")
            appendLine("• Folder Penempatan: ${rawMetadata.recommendedFolder}")
            appendLine("• Petunjuk: ${rawMetadata.recommendationTip}")
            appendLine("======================================")
        }
    }
}

object GlbParser {
    private const val GLB_MAGIC = 0x46546C67 // "glTF"
    private const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON"
    private const val CHUNK_TYPE_BIN = 0x004E4942 // "BIN\0"

    fun inspectFile(file: File): GlbMetadata {
        return try {
            val bytes = file.readBytes()
            inspectBytes(bytes, file.name, file.length())
        } catch (e: Exception) {
            fallbackMetadata(file.name, file.length())
        }
    }

    fun inspectBytes(bytes: ByteArray, fileName: String, fileSize: Long = bytes.size.toLong()): GlbMetadata {
        val inspection = try {
            inspectAndParseBytes(bytes, fileName, fileSize, null)
        } catch (e: Exception) {
            null
        }
        return inspection?.rawMetadata ?: fallbackMetadata(fileName, fileSize)
    }

    fun inspectAndParseGlbFile(file: File): GlbInspectionResult {
        val bytes = file.readBytes()
        return inspectAndParseBytes(bytes, file.name, file.length(), file)
    }

    fun inspectAndParseBytes(
        bytes: ByteArray,
        fileName: String,
        fileSize: Long = bytes.size.toLong(),
        sourceFile: File? = null
    ): GlbInspectionResult {
        require(bytes.size >= 12) { "Ukuran file GLB terlalu kecil (< 12 byte)." }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        val magic = buffer.int
        require(magic == GLB_MAGIC) { "Magic header bukan GLB yang valid (0x46546C67)." }

        val version = buffer.int
        require(version == 2) { "Versi glTF $version tidak didukung. Hanya glTF 2.0 didukung." }

        val totalLength = buffer.int

        var jsonString: String? = null
        var binBytes: ByteArray? = null

        while (buffer.remaining() >= 8) {
            val chunkLength = buffer.int
            val chunkType = buffer.int
            if (chunkLength < 0 || buffer.remaining() < chunkLength) break

            when (chunkType) {
                CHUNK_TYPE_JSON -> {
                    val jsonBytes = ByteArray(chunkLength)
                    buffer.get(jsonBytes)
                    jsonString = String(jsonBytes, Charsets.UTF_8).trim()
                }
                CHUNK_TYPE_BIN -> {
                    binBytes = ByteArray(chunkLength)
                    buffer.get(binBytes)
                }
                else -> {
                    buffer.position(buffer.position() + chunkLength)
                }
            }
        }

        requireNotNull(jsonString) { "Chunk JSON glTF tidak ditemukan di file GLB." }
        val binBuffer = binBytes?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN) }

        return decodeGltfInspection(jsonString, binBuffer, fileName, fileSize, sourceFile)
    }

    fun parse(inputStream: InputStream, defaultName: String = "ImportedModel"): GlbParseResult {
        val bytes = inputStream.readBytes()
        return parseBytes(bytes, defaultName)
    }

    fun parseBytes(bytes: ByteArray, defaultName: String = "ImportedModel"): GlbParseResult {
        val insp = inspectAndParseBytes(bytes, defaultName)
        return GlbParseResult(
            mesh = insp.mesh,
            material = insp.material,
            name = insp.modelName,
            vertexCount = insp.mesh.vertexCount,
            triangleCount = insp.mesh.triangleCount
        )
    }

    private fun decodeGltfInspection(
        jsonString: String,
        binBuffer: ByteBuffer?,
        fileName: String,
        fileSize: Long,
        sourceFile: File?
    ): GlbInspectionResult {
        val root = JSONObject(jsonString)
        val bufferViews = root.optJSONArray("bufferViews")
        val accessors = root.optJSONArray("accessors")
        val meshes = root.optJSONArray("meshes")
        val nodes = root.optJSONArray("nodes")
        val skins = root.optJSONArray("skins")
        val animations = root.optJSONArray("animations")
        val materials = root.optJSONArray("materials")

        val modelName = meshes?.optJSONObject(0)?.optString("name")
            ?: fileName.substringBeforeLast(".")

        val vertices = mutableListOf<Vertex>()
        val triangles = mutableListOf<Triangle>()

        var albedoColor: Long = 0xFF42A5F5
        var roughness = 0.5f
        var metallic = 0.2f
        var baseColorHex = "#42A5F5"

        // Parse Material PBR
        if (materials != null && materials.length() > 0) {
            val matObj = materials.getJSONObject(0)
            val pbr = matObj.optJSONObject("pbrMetallicRoughness")
            if (pbr != null) {
                roughness = pbr.optDouble("roughnessFactor", 0.5).toFloat()
                metallic = pbr.optDouble("metallicFactor", 0.2).toFloat()
                val baseColor = pbr.optJSONArray("baseColorFactor")
                if (baseColor != null && baseColor.length() >= 3) {
                    val r = (baseColor.getDouble(0) * 255).toInt().coerceIn(0, 255)
                    val g = (baseColor.getDouble(1) * 255).toInt().coerceIn(0, 255)
                    val b = (baseColor.getDouble(2) * 255).toInt().coerceIn(0, 255)
                    albedoColor = ((0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
                    baseColorHex = String.format("#%02X%02X%02X", r, g, b)
                }
            }
        }

        // Parse ALL Meshes & Primitives
        if (meshes != null && binBuffer != null && accessors != null && bufferViews != null) {
            for (m in 0 until meshes.length()) {
                val meshObj = meshes.getJSONObject(m)
                val primitives = meshObj.optJSONArray("primitives") ?: continue

                for (p in 0 until primitives.length()) {
                    val prim = primitives.getJSONObject(p)
                    val attributes = prim.optJSONObject("attributes") ?: continue

                    val posAccessorIdx = attributes.optInt("POSITION", -1)
                    if (posAccessorIdx < 0) continue

                    val normAccessorIdx = attributes.optInt("NORMAL", -1)
                    val uvAccessorIdx = attributes.optInt("TEXCOORD_0", -1)
                    val indicesAccessorIdx = prim.optInt("indices", -1)

                    val positions = readVec3List(posAccessorIdx, accessors, bufferViews, binBuffer)
                    val normals = if (normAccessorIdx >= 0) {
                        readVec3List(normAccessorIdx, accessors, bufferViews, binBuffer)
                    } else {
                        List(positions.size) { Vec3.UP }
                    }

                    val uvs = if (uvAccessorIdx >= 0) {
                        readVec2List(uvAccessorIdx, accessors, bufferViews, binBuffer)
                    } else {
                        List(positions.size) { Pair(0f, 0f) }
                    }

                    val baseVertexIndex = vertices.size
                    for (i in positions.indices) {
                        val pos = positions[i]
                        val norm = normals.getOrElse(i) { Vec3.UP }
                        val (u, v) = uvs.getOrElse(i) { Pair(0f, 0f) }
                        vertices.add(Vertex(pos, norm, u, v))
                    }

                    if (indicesAccessorIdx >= 0) {
                        val idxList = readIndicesList(indicesAccessorIdx, accessors, bufferViews, binBuffer)
                        for (i in 0 until idxList.size - 2 step 3) {
                            triangles.add(
                                Triangle(
                                    baseVertexIndex + idxList[i],
                                    baseVertexIndex + idxList[i + 1],
                                    baseVertexIndex + idxList[i + 2]
                                )
                            )
                        }
                    } else {
                        for (i in 0 until positions.size - 2 step 3) {
                            triangles.add(
                                Triangle(
                                    baseVertexIndex + i,
                                    baseVertexIndex + i + 1,
                                    baseVertexIndex + i + 2
                                )
                            )
                        }
                    }
                }
            }
        }

        // Bounding Box Calculation
        val boundingBox = if (vertices.isNotEmpty()) {
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE

            for (v in vertices) {
                if (v.position.x < minX) minX = v.position.x
                if (v.position.x > maxX) maxX = v.position.x
                if (v.position.y < minY) minY = v.position.y
                if (v.position.y > maxY) maxY = v.position.y
                if (v.position.z < minZ) minZ = v.position.z
                if (v.position.z > maxZ) maxZ = v.position.z
            }

            val bMin = Vec3(minX, minY, minZ)
            val bMax = Vec3(maxX, maxY, maxZ)
            val bCenter = Vec3((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f)
            val bSize = Vec3(maxX - minX, maxY - minY, maxZ - minZ)
            BoundingBox3D(bMin, bMax, bCenter, bSize)
        } else {
            BoundingBox3D(Vec3(-1f, 0f, -1f), Vec3(1f, 2f, 1f), Vec3(0f, 1f, 0f), Vec3(2f, 2f, 2f))
        }

        // Auto Scale Normalization (target ~ 2.0 unit tall / wide for smooth editor viewport view)
        val maxDim = boundingBox.maxDimension
        val suggestedScale = if (maxDim > 0.001f) {
            val ratio = 2.0f / maxDim
            if (ratio in 0.8f..1.4f) 1.0f else ratio
        } else {
            1.0f
        }

        // Parse Bone / Joint Indices from skins
        val jointIndices = mutableSetOf<Int>()
        var totalBoneCount = 0
        if (skins != null) {
            for (sk in 0 until skins.length()) {
                val skinObj = skins.optJSONObject(sk) ?: continue
                val joints = skinObj.optJSONArray("joints")
                if (joints != null) {
                    for (j in 0 until joints.length()) {
                        jointIndices.add(joints.getInt(j))
                    }
                }
            }
            totalBoneCount = jointIndices.size
        }

        // Parse Node Tree Hierarchy
        val totalNodesCount = nodes?.length() ?: 0
        val rootNodes = mutableListOf<GlbNodeInfo>()
        if (nodes != null && nodes.length() > 0) {
            val childIndexSet = mutableSetOf<Int>()
            for (i in 0 until nodes.length()) {
                val nObj = nodes.optJSONObject(i) ?: continue
                val childrenArr = nObj.optJSONArray("children")
                if (childrenArr != null) {
                    for (c in 0 until childrenArr.length()) {
                        childIndexSet.add(childrenArr.getInt(c))
                    }
                }
            }

            fun buildNode(idx: Int): GlbNodeInfo {
                val nObj = nodes.optJSONObject(idx)
                val name = nObj?.optString("name", "Node $idx") ?: "Node $idx"
                val meshIdx = nObj?.optInt("mesh", -1) ?: -1
                val skinIdx = nObj?.optInt("skin", -1) ?: -1

                val type = when {
                    jointIndices.contains(idx) -> GlbNodeType.BONE
                    meshIdx >= 0 -> GlbNodeType.MESH
                    else -> GlbNodeType.NODE
                }

                val tArr = nObj?.optJSONArray("translation")
                val translation = if (tArr != null && tArr.length() >= 3) {
                    Vec3(tArr.getDouble(0).toFloat(), tArr.getDouble(1).toFloat(), tArr.getDouble(2).toFloat())
                } else Vec3.ZERO

                val sArr = nObj?.optJSONArray("scale")
                val scale = if (sArr != null && sArr.length() >= 3) {
                    Vec3(sArr.getDouble(0).toFloat(), sArr.getDouble(1).toFloat(), sArr.getDouble(2).toFloat())
                } else Vec3.ONE

                val rArr = nObj?.optJSONArray("rotation")
                val rotation = if (rArr != null && rArr.length() >= 3) {
                    Vec3(rArr.getDouble(0).toFloat(), rArr.getDouble(1).toFloat(), rArr.getDouble(2).toFloat())
                } else Vec3.ZERO

                val childrenList = mutableListOf<GlbNodeInfo>()
                val chArr = nObj?.optJSONArray("children")
                if (chArr != null) {
                    for (c in 0 until chArr.length()) {
                        val cIdx = chArr.getInt(c)
                        if (cIdx in 0 until nodes.length() && cIdx != idx) {
                            childrenList.add(buildNode(cIdx))
                        }
                    }
                }

                return GlbNodeInfo(
                    index = idx,
                    name = name,
                    type = type,
                    meshIndex = meshIdx,
                    skinIndex = skinIdx,
                    children = childrenList,
                    translation = translation,
                    rotation = rotation,
                    scale = scale
                )
            }

            for (i in 0 until nodes.length()) {
                if (!childIndexSet.contains(i)) {
                    rootNodes.add(buildNode(i))
                }
            }
        }

        // Parse Animations & Duration
        val animationInfos = mutableListOf<GlbAnimationInfo>()
        if (animations != null) {
            for (a in 0 until animations.length()) {
                val aObj = animations.optJSONObject(a) ?: continue
                val aName = aObj.optString("name", "Animation $a")
                val channels = aObj.optJSONArray("channels")
                val samplers = aObj.optJSONArray("samplers")

                var maxDuration = 0f
                if (samplers != null && accessors != null) {
                    for (s in 0 until samplers.length()) {
                        val sampler = samplers.optJSONObject(s) ?: continue
                        val inputAccIdx = sampler.optInt("input", -1)
                        if (inputAccIdx in 0 until accessors.length()) {
                            val acc = accessors.optJSONObject(inputAccIdx)
                            val maxArr = acc?.optJSONArray("max")
                            if (maxArr != null && maxArr.length() > 0) {
                                val t = maxArr.optDouble(0, 0.0).toFloat()
                                if (t > maxDuration) maxDuration = t
                            }
                        }
                    }
                }
                if (maxDuration <= 0.01f) maxDuration = 1.0f

                animationInfos.add(
                    GlbAnimationInfo(
                        index = a,
                        name = aName,
                        duration = maxDuration,
                        channelsCount = channels?.length() ?: 0,
                        samplersCount = samplers?.length() ?: 0
                    )
                )
            }
        }

        val uid = java.util.UUID.randomUUID().toString().take(8)
        val finalMesh = if (vertices.isNotEmpty() && triangles.isNotEmpty()) {
            Mesh(
                id = "glb_$uid",
                name = modelName,
                vertices = vertices,
                triangles = triangles,
                primitiveType = PrimitiveType.CHARACTER
            )
        } else {
            Mesh.createCube("glb_$uid", 1.5f).copy(name = modelName)
        }

        val finalMaterial = Material(
            id = "mat_glb_$uid",
            name = "$modelName Material",
            albedoColor = albedoColor,
            roughness = roughness,
            metallic = metallic
        )

        val lower = (fileName + " " + modelName).lowercase()
        val (role, folder, tip) = when {
            lower.contains("char") || lower.contains("hero") || lower.contains("player") ||
            lower.contains("kostum") || lower.contains("costume") || lower.contains("avatar") ||
            lower.contains("skin") || lower.contains("body") || lower.contains("tpp") -> {
                Triple(
                    "Kostum Karakter TPP (Dynamic Actor)",
                    "assets/models/characters/",
                    "Direkomendasikan ditempatkan di folder 'assets/models/characters/'. Pasang script player_controller.lua dan kamera TPP untuk menggerakkan kostum ini."
                )
            }
            lower.contains("terrain") || lower.contains("ground") || lower.contains("map") ||
            lower.contains("surface") || lower.contains("permukaan") || lower.contains("world") ||
            lower.contains("land") || lower.contains("floor") || lower.contains("level") -> {
                Triple(
                    "Permukaan Kostum GLB / Terrain (Static Ground)",
                    "assets/models/world/terrain/",
                    "Direkomendasikan ditempatkan di folder 'assets/models/world/terrain/'. Beri MeshCollider statis agar karakter open-world tidak tembus lantai."
                )
            }
            else -> {
                Triple(
                    "3D Asset / Prop Lingkungan",
                    "assets/models/props/",
                    "Dapat ditempatkan di folder 'assets/models/props/'. Cocok untuk item, senjata, bangunan, atau dekorasi open-world."
                )
            }
        }

        val metadata = GlbMetadata(
            fileName = fileName,
            fileSizeFormatted = formatFileSize(fileSize),
            modelName = modelName,
            meshCount = meshes?.length() ?: 1,
            vertexCount = finalMesh.vertexCount,
            triangleCount = finalMesh.triangleCount,
            materialCount = materials?.length() ?: 1,
            textureCount = root.optJSONArray("textures")?.length() ?: 0,
            animationCount = animationInfos.size,
            roughness = roughness,
            metallic = metallic,
            baseColorHex = baseColorHex,
            recommendedRole = role,
            recommendedFolder = folder,
            recommendationTip = tip
        )

        return GlbInspectionResult(
            mesh = finalMesh,
            material = finalMaterial,
            modelName = modelName,
            boundingBox = boundingBox,
            suggestedScale = suggestedScale,
            rootNodes = rootNodes,
            totalNodesCount = totalNodesCount,
            boneCount = totalBoneCount,
            animations = animationInfos,
            rawMetadata = metadata,
            file = sourceFile
        )
    }

    private fun fallbackMetadata(fileName: String, fileSize: Long): GlbMetadata {
        val lower = fileName.lowercase()
        val isCharacter = lower.contains("char") || lower.contains("kostum") || lower.contains("player")
        return GlbMetadata(
            fileName = fileName,
            fileSizeFormatted = formatFileSize(fileSize),
            modelName = fileName.substringBeforeLast("."),
            meshCount = 1,
            vertexCount = 0,
            triangleCount = 0,
            materialCount = 1,
            textureCount = 0,
            animationCount = 0,
            roughness = 0.5f,
            metallic = 0.2f,
            baseColorHex = "#42A5F5",
            recommendedRole = if (isCharacter) "Kostum Karakter TPP" else "Permukaan / Prop 3D",
            recommendedFolder = if (isCharacter) "assets/models/characters/" else "assets/models/world/",
            recommendationTip = "File model 3D glTF binary. Tempatkan pada struktur folder proyek untuk modularitas aset."
        )
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(java.util.Locale.US, "%.2f MB", mb)
    }

    private fun readVec3List(
        accessorIdx: Int,
        accessors: org.json.JSONArray,
        bufferViews: org.json.JSONArray,
        binBuffer: ByteBuffer
    ): List<Vec3> {
        val acc = accessors.getJSONObject(accessorIdx)
        val count = acc.getInt("count")
        val bufferViewIdx = acc.getInt("bufferView")
        val accOffset = acc.optInt("byteOffset", 0)

        val bv = bufferViews.getJSONObject(bufferViewIdx)
        val bvOffset = bv.optInt("byteOffset", 0)
        val byteStride = bv.optInt("byteStride", 12)

        val list = ArrayList<Vec3>(count)
        val start = bvOffset + accOffset

        for (i in 0 until count) {
            val pos = start + i * byteStride
            if (pos + 12 <= binBuffer.capacity()) {
                binBuffer.position(pos)
                val x = binBuffer.float
                val y = binBuffer.float
                val z = binBuffer.float
                list.add(Vec3(x, y, z))
            }
        }
        return list
    }

    private fun readVec2List(
        accessorIdx: Int,
        accessors: org.json.JSONArray,
        bufferViews: org.json.JSONArray,
        binBuffer: ByteBuffer
    ): List<Pair<Float, Float>> {
        val acc = accessors.getJSONObject(accessorIdx)
        val count = acc.getInt("count")
        val bufferViewIdx = acc.getInt("bufferView")
        val accOffset = acc.optInt("byteOffset", 0)

        val bv = bufferViews.getJSONObject(bufferViewIdx)
        val bvOffset = bv.optInt("byteOffset", 0)
        val byteStride = bv.optInt("byteStride", 8)

        val list = ArrayList<Pair<Float, Float>>(count)
        val start = bvOffset + accOffset

        for (i in 0 until count) {
            val pos = start + i * byteStride
            if (pos + 8 <= binBuffer.capacity()) {
                binBuffer.position(pos)
                val u = binBuffer.float
                val v = binBuffer.float
                list.add(Pair(u, v))
            }
        }
        return list
    }

    private fun readIndicesList(
        accessorIdx: Int,
        accessors: org.json.JSONArray,
        bufferViews: org.json.JSONArray,
        binBuffer: ByteBuffer
    ): List<Int> {
        val acc = accessors.getJSONObject(accessorIdx)
        val count = acc.getInt("count")
        val compType = acc.getInt("componentType")
        val bufferViewIdx = acc.getInt("bufferView")
        val accOffset = acc.optInt("byteOffset", 0)

        val bv = bufferViews.getJSONObject(bufferViewIdx)
        val bvOffset = bv.optInt("byteOffset", 0)

        val list = ArrayList<Int>(count)
        val start = bvOffset + accOffset

        when (compType) {
            5121 -> { // UNSIGNED_BYTE
                for (i in 0 until count) {
                    val p = start + i
                    if (p < binBuffer.capacity()) {
                        binBuffer.position(p)
                        list.add(binBuffer.get().toInt() and 0xFF)
                    }
                }
            }
            5123 -> { // UNSIGNED_SHORT
                for (i in 0 until count) {
                    val p = start + i * 2
                    if (p + 2 <= binBuffer.capacity()) {
                        binBuffer.position(p)
                        list.add(binBuffer.short.toInt() and 0xFFFF)
                    }
                }
            }
            5125 -> { // UNSIGNED_INT
                for (i in 0 until count) {
                    val p = start + i * 4
                    if (p + 4 <= binBuffer.capacity()) {
                        binBuffer.position(p)
                        list.add(binBuffer.int)
                    }
                }
            }
        }
        return list
    }
}
