package com.example.engine.model

import com.example.engine.math.Mat4
import com.example.engine.math.MathUtils
import com.example.engine.math.Vec3

sealed interface Component

data class TransformComponent(
    val position: Vec3 = Vec3.ZERO,
    val rotation: Vec3 = Vec3.ZERO, // Euler angles in degrees (pitch, yaw, roll)
    val scale: Vec3 = Vec3.ONE
) : Component {
    @Transient
    private var cachedMatrix: Mat4? = null

    fun getLocalMatrix(): Mat4 {
        var m = cachedMatrix
        if (m == null) {
            val t = Mat4.translation(position)
            val r = Mat4.euler(
                MathUtils.toRadians(rotation.x),
                MathUtils.toRadians(rotation.y),
                MathUtils.toRadians(rotation.z)
            )
            val s = Mat4.scale(scale)
            m = t * r * s
            cachedMatrix = m
        }
        return m
    }
}

data class MeshRendererComponent(
    val meshId: String = "mesh_cube",
    val materialId: String = "mat_default",
    val isVisible: Boolean = true,
    val castShadows: Boolean = true
) : Component

enum class ColliderShape {
    BOX,
    SPHERE,
    PLANE
}

data class RigidBodyComponent(
    val mass: Float = 1.0f,
    val isStatic: Boolean = false,
    val restitution: Float = 0.1f, // Bounciness [0..1] - sensible default, was 0.6f causing infinite bouncing
    val friction: Float = 0.4f,
    val useGravity: Boolean = true,
    val colliderShape: ColliderShape = ColliderShape.BOX,
    val velocity: Vec3 = Vec3.ZERO,
    val angularVelocity: Vec3 = Vec3.ZERO,
    val isSleeping: Boolean = false,
    val isGrounded: Boolean = false
) : Component

enum class LightType {
    DIRECTIONAL,
    POINT,
    SPOT
}

data class LightComponent(
    val type: LightType = LightType.DIRECTIONAL,
    val color: Long = 0xFFFFF9E6,
    val intensity: Float = 1.0f,
    val range: Float = 15f
) : Component

data class LuaScriptComponent(
    val scriptId: String = "script_player",
    val scriptName: String = "PlayerController.lua",
    val scriptContent: String = "",
    val isEnabled: Boolean = true
) : Component

data class CameraComponent(
    val fov: Float = 60f,
    val near: Float = 0.1f,
    val far: Float = 100f,
    val isMainCamera: Boolean = true
) : Component
