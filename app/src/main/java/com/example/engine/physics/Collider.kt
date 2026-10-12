package com.example.engine.physics

import com.example.engine.math.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ContactPoint(
    val point: Vec3,
    val normal: Vec3,
    val penetration: Float
)

object CollisionDetector {

    fun testSphereSphere(
        posA: Vec3, radiusA: Float,
        posB: Vec3, radiusB: Float
    ): ContactPoint? {
        val delta = posB - posA
        val distSq = delta.lengthSquared()
        val totalRadius = radiusA + radiusB
        if (distSq >= totalRadius * totalRadius || distSq < 0.000001f) {
            return null
        }
        val dist = sqrt(distSq)
        val normal = delta / dist
        val penetration = totalRadius - dist
        val point = posA + normal * (radiusA - penetration * 0.5f)
        return ContactPoint(point, normal, penetration)
    }

    fun testSpherePlane(
        spherePos: Vec3, radius: Float,
        planeY: Float = 0f
    ): ContactPoint? {
        val dist = spherePos.y - planeY
        if (dist < radius) {
            val penetration = radius - dist
            return ContactPoint(
                point = Vec3(spherePos.x, planeY, spherePos.z),
                normal = Vec3.UP,
                penetration = penetration
            )
        }
        return null
    }

    fun testBoxPlane(
        boxPos: Vec3, halfSize: Vec3,
        planeY: Float = 0f
    ): ContactPoint? {
        val bottomY = boxPos.y - halfSize.y
        if (bottomY < planeY) {
            val penetration = planeY - bottomY
            return ContactPoint(
                point = Vec3(boxPos.x, planeY, boxPos.z),
                normal = Vec3.UP,
                penetration = penetration
            )
        }
        return null
    }

    fun testSphereBox(
        spherePos: Vec3, radius: Float,
        boxPos: Vec3, halfSize: Vec3
    ): ContactPoint? {
        // Find closest point on box to sphere center
        val closestX = spherePos.x.coerceIn(boxPos.x - halfSize.x, boxPos.x + halfSize.x)
        val closestY = spherePos.y.coerceIn(boxPos.y - halfSize.y, boxPos.y + halfSize.y)
        val closestZ = spherePos.z.coerceIn(boxPos.z - halfSize.z, boxPos.z + halfSize.z)
        val closestPoint = Vec3(closestX, closestY, closestZ)

        val delta = spherePos - closestPoint
        val distSq = delta.lengthSquared()

        if (distSq < radius * radius && distSq > 0.000001f) {
            val dist = sqrt(distSq)
            val normal = delta / dist
            val penetration = radius - dist
            return ContactPoint(closestPoint, normal, penetration)
        }
        return null
    }

    fun testBoxBox(
        posA: Vec3, halfA: Vec3,
        posB: Vec3, halfB: Vec3
    ): ContactPoint? {
        val delta = posB - posA
        val overlapX = (halfA.x + halfB.x) - abs(delta.x)
        val overlapY = (halfA.y + halfB.y) - abs(delta.y)
        val overlapZ = (halfA.z + halfB.z) - abs(delta.z)

        if (overlapX > 0 && overlapY > 0 && overlapZ > 0) {
            // Find minimum penetration axis
            return when {
                overlapY <= overlapX && overlapY <= overlapZ -> {
                    val sign = if (delta.y > 0) 1f else -1f
                    ContactPoint(
                        point = Vec3(posA.x, posA.y + halfA.y * sign, posA.z),
                        normal = Vec3(0f, sign, 0f),
                        penetration = overlapY
                    )
                }
                overlapX <= overlapZ -> {
                    val sign = if (delta.x > 0) 1f else -1f
                    ContactPoint(
                        point = Vec3(posA.x + halfA.x * sign, posA.y, posA.z),
                        normal = Vec3(sign, 0f, 0f),
                        penetration = overlapX
                    )
                }
                else -> {
                    val sign = if (delta.z > 0) 1f else -1f
                    ContactPoint(
                        point = Vec3(posA.x, posA.y, posA.z + halfA.z * sign),
                        normal = Vec3(0f, 0f, sign),
                        penetration = overlapZ
                    )
                }
            }
        }
        return null
    }
}
