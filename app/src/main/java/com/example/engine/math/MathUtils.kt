package com.example.engine.math

import kotlin.math.PI

object MathUtils {
    const val PI_F = PI.toFloat()
    const val DEG_TO_RAD = PI_F / 180f
    const val RAD_TO_DEG = 180f / PI_F

    fun toRadians(degrees: Float): Float = degrees * DEG_TO_RAD
    fun toDegrees(radians: Float): Float = radians * RAD_TO_DEG

    fun clamp(value: Float, min: Float, max: Float): Float = value.coerceIn(min, max)

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * Converts a 2D screen coordinate (e.g. touch input) to a 3D ray in world space.
     */
    fun screenPointToRay(
        screenX: Float,
        screenY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        cameraPos: Vec3,
        cameraTarget: Vec3,
        fovYDegrees: Float = 60f
    ): Ray {
        if (viewportWidth <= 0f || viewportHeight <= 0f) {
            return Ray(cameraPos, (cameraTarget - cameraPos).normalized())
        }

        // Normalized Device Coordinates (-1 to 1)
        val ndcX = (2f * screenX / viewportWidth) - 1f
        val ndcY = 1f - (2f * screenY / viewportHeight) // Flip Y

        val aspect = viewportWidth / viewportHeight
        val fovYRad = toRadians(fovYDegrees)
        val tanHalfFov = kotlin.math.tan(fovYRad / 2f)

        val forward = (cameraTarget - cameraPos).normalized()
        val right = forward.cross(Vec3.UP).normalized()
        val up = right.cross(forward).normalized()

        val rayDir = (forward + right * (ndcX * tanHalfFov * aspect) + up * (ndcY * tanHalfFov)).normalized()
        return Ray(cameraPos, rayDir)
    }
}
