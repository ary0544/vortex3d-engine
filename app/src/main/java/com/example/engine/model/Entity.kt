package com.example.engine.model

import com.example.engine.math.Vec3

data class Entity(
    val id: String,
    val name: String,
    val transform: TransformComponent = TransformComponent(),
    val meshRenderer: MeshRendererComponent? = null,
    val rigidBody: RigidBodyComponent? = null,
    val light: LightComponent? = null,
    val luaScript: LuaScriptComponent? = null,
    val camera: CameraComponent? = null,
    val isEnabled: Boolean = true,
    val isLocked: Boolean = false,
    val tag: String = "Untagged"
) {
    fun withPosition(pos: Vec3) = copy(transform = transform.copy(position = pos))
    fun withRotation(rot: Vec3) = copy(transform = transform.copy(rotation = rot))
    fun withScale(s: Vec3) = copy(transform = transform.copy(scale = s))
}
