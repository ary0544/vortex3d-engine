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
    var horizontal: Float = 0f,
    var vertical: Float = 0f,
    var actionJump: Boolean = false,
    var actionFire: Boolean = false
)
