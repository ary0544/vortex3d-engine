package com.example.engine.math

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class Ray(
    val origin: Vec3,
    val direction: Vec3
) {
    val dirNormalized: Vec3 = direction.normalized()

    fun pointAt(distance: Float): Vec3 = origin + dirNormalized * distance

    /**
     * Ray-Sphere intersection test. Returns distance to hit or null if missed.
     */
    fun intersectSphere(center: Vec3, radius: Float): Float? {
        val oc = origin - center
        val a = dirNormalized.dot(dirNormalized)
        val b = 2f * oc.dot(dirNormalized)
        val c = oc.dot(oc) - radius * radius
        val discriminant = b * b - 4f * a * c
        if (discriminant < 0) return null

        val sq = sqrt(discriminant)
        val t0 = (-b - sq) / (2f * a)
        val t1 = (-b + sq) / (2f * a)
        val hit = when {
            t0 > 0.001f -> t0
            t1 > 0.001f -> t1
            else -> null
        }
        return hit
    }

    /**
     * Ray-AABB intersection test. Returns distance to hit or null.
     */
    fun intersectAABB(minBound: Vec3, maxBound: Vec3): Float? {
        var tmin = -Float.MAX_VALUE
        var tmax = Float.MAX_VALUE

        val d = dirNormalized
        val o = origin

        // X axis
        if (kotlin.math.abs(d.x) < 0.00001f) {
            if (o.x < minBound.x || o.x > maxBound.x) return null
        } else {
            val invD = 1f / d.x
            var t1 = (minBound.x - o.x) * invD
            var t2 = (maxBound.x - o.x) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tmin = max(tmin, t1)
            tmax = min(tmax, t2)
            if (tmin > tmax) return null
        }

        // Y axis
        if (kotlin.math.abs(d.y) < 0.00001f) {
            if (o.y < minBound.y || o.y > maxBound.y) return null
        } else {
            val invD = 1f / d.y
            var t1 = (minBound.y - o.y) * invD
            var t2 = (maxBound.y - o.y) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tmin = max(tmin, t1)
            tmax = min(tmax, t2)
            if (tmin > tmax) return null
        }

        // Z axis
        if (kotlin.math.abs(d.z) < 0.00001f) {
            if (o.z < minBound.z || o.z > maxBound.z) return null
        } else {
            val invD = 1f / d.z
            var t1 = (minBound.z - o.z) * invD
            var t2 = (maxBound.z - o.z) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tmin = max(tmin, t1)
            tmax = min(tmax, t2)
            if (tmin > tmax) return null
        }

        return if (tmax >= max(0f, tmin)) {
            if (tmin >= 0f) tmin else tmax
        } else null
    }

    /**
     * Ray-Plane intersection test (plane: point on plane and normal).
     */
    fun intersectPlane(planePoint: Vec3, planeNormal: Vec3): Float? {
        val denom = planeNormal.dot(dirNormalized)
        if (kotlin.math.abs(denom) > 0.00001f) {
            val p0l0 = planePoint - origin
            val t = p0l0.dot(planeNormal) / denom
            return if (t >= 0f) t else null
        }
        return null
    }
}
