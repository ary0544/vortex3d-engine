package com.example.engine.model

import com.example.engine.math.Vec3

data class Material(
    val id: String,
    val name: String,
    val albedoColor: Long = 0xFF42A5F5, // ARGB
    val roughness: Float = 0.4f,
    val metallic: Float = 0.2f,
    val emissiveColor: Long = 0x00000000,
    val emissiveIntensity: Float = 0f,
    val shaderProgramId: String = "shader_pbr",
    val isWireframe: Boolean = false,
    val isDoubleSided: Boolean = false,
    val customUniforms: Map<String, Float> = emptyMap()
) {
    fun getAlbedoVec(): Vec3 {
        val r = ((albedoColor shr 16) and 0xFF) / 255f
        val g = ((albedoColor shr 8) and 0xFF) / 255f
        val b = (albedoColor and 0xFF) / 255f
        return Vec3(r, g, b)
    }

    companion object {
        fun defaultMaterial(): Material = Material(
            id = "mat_default",
            name = "Default Material",
            albedoColor = 0xFF29B6F6,
            roughness = 0.5f,
            metallic = 0.3f
        )

        fun createPbrGold(): Material = Material(
            id = "mat_gold",
            name = "Polished Gold",
            albedoColor = 0xFFFFD700,
            roughness = 0.2f,
            metallic = 0.95f
        )

        fun createCyberHologram(): Material = Material(
            id = "mat_hologram",
            name = "Cyber Cyan Glow",
            albedoColor = 0xFF00E5FF,
            roughness = 0.1f,
            metallic = 0.8f,
            emissiveColor = 0xFF00E5FF,
            emissiveIntensity = 0.8f,
            shaderProgramId = "shader_hologram"
        )

        fun createToonRuby(): Material = Material(
            id = "mat_ruby",
            name = "Toon Crimson",
            albedoColor = 0xFFFF1744,
            roughness = 0.3f,
            metallic = 0.1f,
            shaderProgramId = "shader_toon"
        )

        fun createWaterMaterial(): Material = Material(
            id = "mat_water",
            name = "Lagoon Water",
            albedoColor = 0xFF00B0FF,
            roughness = 0.05f,
            metallic = 0.9f,
            shaderProgramId = "shader_water"
        )

        fun createNeonEmerald(): Material = Material(
            id = "mat_emerald",
            name = "Neon Emerald",
            albedoColor = 0xFF00E676,
            roughness = 0.25f,
            metallic = 0.7f,
            emissiveColor = 0xFF00E676,
            emissiveIntensity = 0.5f
        )
    }
}
