package com.example.engine.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Mat4(val data: FloatArray = FloatArray(16)) {
    init {
        require(data.size == 16) { "Mat4 requires 16 float elements" }
    }

    operator fun get(row: Int, col: Int): Float = data[row * 4 + col]
    operator fun set(row: Int, col: Int, value: Float) {
        data[row * 4 + col] = value
    }

    operator fun times(other: Mat4): Mat4 {
        val result = FloatArray(16)
        for (r in 0..3) {
            for (c in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += this[r, k] * other[k, c]
                }
                result[r * 4 + c] = sum
            }
        }
        return Mat4(result)
    }

    fun transformPoint(p: Vec3): Vec3 {
        val x = p.x * data[0] + p.y * data[1] + p.z * data[2] + data[3]
        val y = p.x * data[4] + p.y * data[5] + p.z * data[6] + data[7]
        val z = p.x * data[8] + p.y * data[9] + p.z * data[10] + data[11]
        val w = p.x * data[12] + p.y * data[13] + p.z * data[14] + data[15]
        return if (w != 0f && w != 1f) {
            Vec3(x / w, y / w, z / w)
        } else {
            Vec3(x, y, z)
        }
    }

    fun transformDirection(d: Vec3): Vec3 {
        val x = d.x * data[0] + d.y * data[1] + d.z * data[2]
        val y = d.x * data[4] + d.y * data[5] + d.z * data[6]
        val z = d.x * data[8] + d.y * data[9] + d.z * data[10]
        return Vec3(x, y, z)
    }

    companion object {
        fun identity(): Mat4 {
            val d = FloatArray(16)
            d[0] = 1f; d[5] = 1f; d[10] = 1f; d[15] = 1f
            return Mat4(d)
        }

        fun translation(x: Float, y: Float, z: Float): Mat4 {
            val m = identity()
            m.data[3] = x
            m.data[7] = y
            m.data[11] = z
            return m
        }

        fun translation(v: Vec3): Mat4 = translation(v.x, v.y, v.z)

        fun scale(sx: Float, sy: Float, sz: Float): Mat4 {
            val m = identity()
            m.data[0] = sx
            m.data[5] = sy
            m.data[10] = sz
            return m
        }

        fun scale(v: Vec3): Mat4 = scale(v.x, v.y, v.z)

        fun rotationX(rad: Float): Mat4 {
            val m = identity()
            val c = cos(rad)
            val s = sin(rad)
            m.data[5] = c
            m.data[6] = -s
            m.data[9] = s
            m.data[10] = c
            return m
        }

        fun rotationY(rad: Float): Mat4 {
            val m = identity()
            val c = cos(rad)
            val s = sin(rad)
            m.data[0] = c
            m.data[2] = s
            m.data[8] = -s
            m.data[10] = c
            return m
        }

        fun rotationZ(rad: Float): Mat4 {
            val m = identity()
            val c = cos(rad)
            val s = sin(rad)
            m.data[0] = c
            m.data[1] = -s
            m.data[4] = s
            m.data[5] = c
            return m
        }

        fun euler(pitchRad: Float, yawRad: Float, rollRad: Float): Mat4 {
            return rotationY(yawRad) * rotationX(pitchRad) * rotationZ(rollRad)
        }

        fun perspective(fovYRad: Float, aspect: Float, near: Float, far: Float): Mat4 {
            val m = FloatArray(16)
            val f = 1f / tan(fovYRad / 2f)
            m[0] = f / aspect
            m[5] = f
            m[10] = (far + near) / (near - far)
            m[11] = (2f * far * near) / (near - far)
            m[14] = -1f
            m[15] = 0f
            return Mat4(m)
        }

        fun lookAt(eye: Vec3, target: Vec3, up: Vec3): Mat4 {
            val f = (target - eye).normalized()
            val s = f.cross(up).normalized()
            val u = s.cross(f)

            val m = FloatArray(16)
            m[0] = s.x;  m[1] = s.y;  m[2] = s.z;  m[3] = -s.dot(eye)
            m[4] = u.x;  m[5] = u.y;  m[6] = u.z;  m[7] = -u.dot(eye)
            m[8] = -f.x; m[9] = -f.y; m[10] = -f.z; m[11] = f.dot(eye)
            m[15] = 1f
            return Mat4(m)
        }
    }
}
