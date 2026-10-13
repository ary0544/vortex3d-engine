package com.example.engine.lua

import com.example.engine.math.Vec3
import com.example.engine.model.Entity

interface EngineCallbacks {
    fun log(tag: String, message: String)
    fun playSound(soundName: String)
    fun updateEntity(updated: Entity)
    fun getEntity(id: String): Entity?
    fun setCameraTarget(target: Vec3) {}
    fun setCameraDistance(distance: Float) {}
    fun setCameraOrbit(yaw: Float, pitch: Float) {}
}

data class InputState(
    @Volatile var horizontal: Float = 0f,
    @Volatile var vertical: Float = 0f,
    @Volatile var actionJump: Boolean = false,
    @Volatile var actionFire: Boolean = false
)
