package com.example.engine.math

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun times(other: Vec3) = Vec3(x * other.x, y * other.y, z * other.z)
    operator fun div(scalar: Float) = if (scalar != 0f) Vec3(x / scalar, y / scalar, z / scalar) else ZERO
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lengthSquared(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 0.00001f) this / len else ZERO
    }

    fun distance(other: Vec3): Float = (this - other).length()
    fun distanceSquared(other: Vec3): Float = (this - other).lengthSquared()

    fun lerp(target: Vec3, t: Float): Vec3 = this + (target - this) * t.coerceIn(0f, 1f)

    fun reflect(normal: Vec3): Vec3 {
        val n = normal.normalized()
        return this - n * (2f * this.dot(n))
    }

    fun rotateY(radians: Float): Vec3 {
        val cosR = cos(radians)
        val sinR = sin(radians)
        return Vec3(x * cosR - z * sinR, y, x * sinR + z * cosR)
    }

    fun rotateX(radians: Float): Vec3 {
        val cosR = cos(radians)
        val sinR = sin(radians)
        return Vec3(x, y * cosR - z * sinR, y * sinR + z * cosR)
    }

    fun toFloatArray(): FloatArray = floatArrayOf(x, y, z)

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val ONE = Vec3(1f, 1f, 1f)
        val UP = Vec3(0f, 1f, 0f)
        val DOWN = Vec3(0f, -1f, 0f)
        val FORWARD = Vec3(0f, 0f, 1f)
        val BACK = Vec3(0f, 0f, -1f)
        val RIGHT = Vec3(1f, 0f, 0f)
        val LEFT = Vec3(-1f, 0f, 0f)
    }
}
