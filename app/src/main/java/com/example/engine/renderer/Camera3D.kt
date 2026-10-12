package com.example.engine.renderer

import com.example.engine.math.Mat4
import com.example.engine.math.MathUtils
import com.example.engine.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

class Camera3D(
    var target: Vec3 = Vec3(0f, 1f, 0f),
    var distance: Float = 8.0f,
    var pitchDegrees: Float = 30.0f,
    var yawDegrees: Float = 45.0f,
    var fovDegrees: Float = 60.0f,
    var near: Float = 0.1f,
    var far: Float = 100.0f
) {
    fun getEyePosition(): Vec3 {
        val pitchRad = MathUtils.toRadians(pitchDegrees.coerceIn(-85f, 85f))
        val yawRad = MathUtils.toRadians(yawDegrees)

        val x = target.x + distance * cos(pitchRad) * sin(yawRad)
        val y = target.y + distance * sin(pitchRad)
        val z = target.z + distance * cos(pitchRad) * cos(yawRad)

        return Vec3(x, y, z)
    }

    fun getViewMatrix(): Mat4 {
        val eye = getEyePosition()
        return Mat4.lookAt(eye, target, Vec3.UP)
    }

    fun getProjectionMatrix(aspect: Float): Mat4 {
        return Mat4.perspective(MathUtils.toRadians(fovDegrees), aspect, near, far)
    }

    fun orbit(deltaYaw: Float, deltaPitch: Float) {
        yawDegrees = (yawDegrees + deltaYaw) % 360f
        pitchDegrees = (pitchDegrees + deltaPitch).coerceIn(-85f, 85f)
    }

    fun zoom(deltaDist: Float) {
        distance = (distance + deltaDist).coerceIn(1.5f, 50.0f)
    }

    fun pan(deltaX: Float, deltaY: Float) {
        val eye = getEyePosition()
        val forward = (target - eye).normalized()
        val right = forward.cross(Vec3.UP).normalized()
        val up = right.cross(forward).normalized()

        target = target + right * deltaX + up * deltaY
    }
}
