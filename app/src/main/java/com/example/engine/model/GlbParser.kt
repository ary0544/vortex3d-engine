package com.example.engine.model

import com.example.engine.math.Vec3
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

object GlbParser {
    private const val GLB_MAGIC = 0x46546C67 // "glTF"
    private const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON"
    private const val CHUNK_TYPE_BIN = 0x004E4942 // "BIN\0"

    fun inspectFile(file: java.io.File): GlbMetadata {
        return try {
            val bytes = file.readBytes()
            inspectBytes(bytes, file.name, file.length())
        } catch (e: Exception) {
            fallbackMetadata(file.name, file.length())
        }
    }

    fun inspectBytes(bytes: ByteArray, fileName: String, fileSize: Long = bytes.size.toLong()): GlbMetadata {
        return try {
            if (bytes.size < 12) return fallbackMetadata(fileName, fileSize)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.int
            if (magic != GLB_MAGIC) return fallbackMetadata(fileName, fileSize)
            val version = buffer.int
            val totalLength = buffer.int

            var jsonString: String? = null
            while (buffer.remaining() >= 8) {
                val chunkLength = buffer.int
                val chunkType = buffer.int
                if (chunkLength < 0 || buffer.remaining() < chunkLength) break
                if (chunkType == CHUNK_TYPE_JSON) {
                    val jsonBytes = ByteArray(chunkLength)
                    buffer.get(jsonBytes)
                    jsonString = String(jsonBytes, Charsets.UTF_8).trim()
                    break
                } else {
                    buffer.position(buffer.position() + chunkLength)
                }
            }

            val parseResult = try {
                parseBytes(bytes, fileName.substringBeforeLast("."))
            } catch (e: Exception) {
                null
            }

            var modelName = parseResult?.name ?: fileName.substringBeforeLast(".")
            var meshCount = 1
            var materialCount = 1
            var textureCount = 0
            var animationCount = 0
            var roughness = parseResult?.material?.roughness ?: 0.5f
            var metallic = parseResult?.material?.metallic ?: 0.2f
            var baseColorHex = "#42A5F5"

            if (jsonString != null) {
                val root = JSONObject(jsonString)
                meshCount = root.optJSONArray("meshes")?.length() ?: 1
                materialCount = root.optJSONArray("materials")?.length() ?: 1
                textureCount = root.optJSONArray("textures")?.length() ?: 0
                animationCount = root.optJSONArray("animations")?.length() ?: 0

                val materials = root.optJSONArray("materials")
                if (materials != null && materials.length() > 0) {
                    val m0 = materials.optJSONObject(0)
                    val pbr = m0?.optJSONObject("pbrMetallicRoughness")
                    if (pbr != null) {
                        roughness = pbr.optDouble("roughnessFactor", 0.5).toFloat()
                        metallic = pbr.optDouble("metallicFactor", 0.2).toFloat()
                        val colorArr = pbr.optJSONArray("baseColorFactor")
                        if (colorArr != null && colorArr.length() >= 3) {
                            val r = (colorArr.getDouble(0) * 255).toInt().coerceIn(0, 255)
                            val g = (colorArr.getDouble(1) * 255).toInt().coerceIn(0, 255)
                            val b = (colorArr.getDouble(2) * 255).toInt().coerceIn(0, 255)
                            baseColorHex = String.format("#%02X%02X%02X", r, g, b)
                        }
                    }
                }
            }

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

            GlbMetadata(
                fileName = fileName,
                fileSizeFormatted = formatFileSize(fileSize),
                modelName = modelName,
                meshCount = meshCount,
                vertexCount = parseResult?.vertexCount ?: 0,
                triangleCount = parseResult?.triangleCount ?: 0,
                materialCount = materialCount,
                textureCount = textureCount,
                animationCount = animationCount,
                roughness = roughness,
                metallic = metallic,
                baseColorHex = baseColorHex,
                recommendedRole = role,
                recommendedFolder = folder,
                recommendationTip = tip
            )
        } catch (e: Exception) {
            fallbackMetadata(fileName, fileSize)
        }
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

    fun parse(inputStream: InputStream, defaultName: String = "ImportedModel"): GlbParseResult {
        val bytes = inputStream.readBytes()
        return parseBytes(bytes, defaultName)
    }

    fun parseBytes(bytes: ByteArray, defaultName: String = "ImportedModel"): GlbParseResult {
        require(bytes.size >= 12) { "Invalid GLB file: File size is less than 12 bytes." }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Read Header
        val magic = buffer.int
        require(magic == GLB_MAGIC) { "Invalid GLB magic header: Expected 0x46546C67, found ${Integer.toHexString(magic)}" }

        val version = buffer.int
        require(version == 2) { "Unsupported glTF version: $version. Only glTF 2.0 is supported." }

        val totalLength = buffer.int

        // Read Chunks
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
                    // Skip unknown chunk
                    buffer.position(buffer.position() + chunkLength)
                }
            }
        }

        requireNotNull(jsonString) { "Missing required JSON chunk in GLB file." }
        val binBuffer = binBytes?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN) }

        return decodeGltfJson(jsonString, binBuffer, defaultName)
    }

    private fun decodeGltfJson(
        jsonString: String,
        binBuffer: ByteBuffer?,
        defaultName: String
    ): GlbParseResult {
        val root = JSONObject(jsonString)
        val bufferViews = root.optJSONArray("bufferViews")
        val accessors = root.optJSONArray("accessors")
        val meshes = root.optJSONArray("meshes")

        val modelName = meshes?.optJSONObject(0)?.optString("name", defaultName) ?: defaultName

        val vertices = mutableListOf<Vertex>()
        val triangles = mutableListOf<Triangle>()

        var albedoColor: Long = 0xFF42A5F5
        var roughness = 0.5f
        var metallic = 0.2f

        // Parse Material if present
        val materials = root.optJSONArray("materials")
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
                }
            }
        }

        if (meshes != null && meshes.length() > 0 && binBuffer != null && accessors != null && bufferViews != null) {
            val meshObj = meshes.getJSONObject(0)
            val primitives = meshObj.getJSONArray("primitives")

            for (p in 0 until primitives.length()) {
                val prim = primitives.getJSONObject(p)
                val attributes = prim.getJSONObject("attributes")

                val posAccessorIdx = attributes.getInt("POSITION")
                val normAccessorIdx = attributes.optInt("NORMAL", -1)
                val uvAccessorIdx = attributes.optInt("TEXCOORD_0", -1)
                val indicesAccessorIdx = prim.optInt("indices", -1)

                // Read Positions
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

                // Read Indices
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
                    // Non-indexed triangle list
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

        val uid = java.util.UUID.randomUUID().toString().take(8)
        val finalMesh = if (vertices.isNotEmpty() && triangles.isNotEmpty()) {
            Mesh(
                id = "glb_$uid",
                name = modelName,
                vertices = vertices,
                triangles = triangles,
                primitiveType = PrimitiveType.CUBE
            )
        } else {
            Mesh.createCube("glb_$uid", 1.5f).copy(name = modelName)
        }

        val finalMaterial = Material(
            id = "mat_glb_$uid",
            name = "$modelName Material",
            albedoColor = albedoColor.toLong(),
            roughness = roughness,
            metallic = metallic
        )

        return GlbParseResult(
            mesh = finalMesh,
            material = finalMaterial,
            name = modelName,
            vertexCount = finalMesh.vertexCount,
            triangleCount = finalMesh.triangleCount
        )
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
