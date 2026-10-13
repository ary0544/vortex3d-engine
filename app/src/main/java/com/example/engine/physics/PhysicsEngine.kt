package com.example.engine.physics

import com.example.engine.math.Vec3
import com.example.engine.model.ColliderShape
import com.example.engine.model.Entity
import kotlin.math.abs
import kotlin.math.max

data class CollisionEvent(
    val entityA: Entity,
    val entityB: Entity,
    val contact: ContactPoint
)

class PhysicsEngine(
    var gravity: Vec3 = Vec3(0f, -9.81f, 0f),
    var isEnabled: Boolean = true
) {
    private val collisionEvents = mutableListOf<CollisionEvent>()

    fun pollCollisionEvents(): List<CollisionEvent> {
        val list = collisionEvents.toList()
        collisionEvents.clear()
        return list
    }

    /**
     * Steps the physical world forward by [dt] seconds.
     */
    fun step(entities: List<Entity>, dt: Float): List<Entity> {
        if (!isEnabled || dt <= 0f) return entities

        // Clamp delta time to avoid tunnel artifacts on mobile frame drops
        val safeDt = dt.coerceIn(0.001f, 0.05f)
        val updated = entities.map { it.copy() }.toMutableList()

        // 1. Integrate external forces (gravity) and update velocities & positions
        for (i in updated.indices) {
            val entity = updated[i]
            val rb = entity.rigidBody ?: continue
            if (rb.isStatic) continue

            var vel = rb.velocity
            if (rb.useGravity) {
                vel = vel + gravity * safeDt
            }

            // Apply air resistance / damping
            vel = vel * 0.995f

            // Batasi kecepatan terminal agar tidak terjadi pelontaran tak terbatas (runaway velocity)
            val maxSpeed = 35.0f
            if (vel.length() > maxSpeed) {
                vel = vel.normalized() * maxSpeed
            }

            var angVel = rb.angularVelocity * 0.98f

            val rawPos = entity.transform.position + vel * safeDt
            // Koordinat aman dunia 3D untuk mencegah letupan ke angka jutaan
            val newPos = Vec3(
                rawPos.x.coerceIn(-500.0f, 500.0f),
                rawPos.y.coerceIn(-50.0f, 250.0f),
                rawPos.z.coerceIn(-500.0f, 500.0f)
            )
            val newRot = entity.transform.rotation + Vec3(angVel.x, angVel.y, angVel.z) * safeDt

            updated[i] = entity.copy(
                transform = entity.transform.copy(position = newPos, rotation = newRot),
                rigidBody = rb.copy(velocity = vel, angularVelocity = angVel)
            )
        }

        // 2. Resolve ground plane collisions (Y = 0)
        for (i in updated.indices) {
            val entity = updated[i]
            val rb = entity.rigidBody ?: continue
            if (rb.isStatic) continue

            val pos = entity.transform.position
            val scale = entity.transform.scale

            val contact: ContactPoint? = when (rb.colliderShape) {
                ColliderShape.SPHERE -> {
                    val radius = max(0.1f, scale.x * 0.5f)
                    CollisionDetector.testSpherePlane(pos, radius, 0f)
                }
                ColliderShape.BOX -> {
                    val half = Vec3(scale.x * 0.5f, scale.y * 0.5f, scale.z * 0.5f)
                    CollisionDetector.testBoxPlane(pos, half, 0f)
                }
                ColliderShape.PLANE -> null
            }

            if (contact != null && contact.penetration > 0f) {
                // Separate out of ground
                val correctedPos = pos + contact.normal * contact.penetration
                var vel = rb.velocity

                // Reflect velocity along normal with restitution
                val velDotNorm = vel.dot(contact.normal)
                if (velDotNorm < 0f) {
                    val normalVel = contact.normal * velDotNorm
                    val tangentVel = vel - normalVel
                    vel = tangentVel * (1f - rb.friction) - normalVel * rb.restitution

                    // Ground friction stops micro-bounces
                    if (abs(vel.y) < 0.15f) {
                        vel = Vec3(vel.x * 0.9f, 0f, vel.z * 0.9f)
                    }
                }

                updated[i] = entity.copy(
                    transform = entity.transform.copy(position = correctedPos),
                    rigidBody = rb.copy(velocity = vel)
                )
            }
        }

        // 3. Resolve Entity vs Entity collisions
        for (i in 0 until updated.size) {
            val a = updated[i]
            val rbA = a.rigidBody ?: continue

            for (j in (i + 1) until updated.size) {
                val b = updated[j]
                val rbB = b.rigidBody ?: continue

                if (rbA.isStatic && rbB.isStatic) continue

                val contact = checkEntityCollision(a, b) ?: continue
                collisionEvents.add(CollisionEvent(a, b, contact))

                // Separate bodies based on static flags and mass
                val totalMass = (if (rbA.isStatic) 0f else rbA.mass) + (if (rbB.isStatic) 0f else rbB.mass)
                if (totalMass <= 0f) continue

                val normal = contact.normal
                val pen = contact.penetration

                val invMassA = if (rbA.isStatic) 0f else 1f / rbA.mass
                val invMassB = if (rbB.isStatic) 0f else 1f / rbB.mass
                val invMassSum = invMassA + invMassB
                if (invMassSum <= 0f) continue

                // Positional separation
                val sepA = if (rbA.isStatic) Vec3.ZERO else -normal * (pen * (invMassA / invMassSum))
                val sepB = if (rbB.isStatic) Vec3.ZERO else normal * (pen * (invMassB / invMassSum))

                // Relative velocity
                val relVel = rbB.velocity - rbA.velocity
                val velAlongNorm = relVel.dot(normal)

                // Do not resolve if velocities are separating
                if (velAlongNorm > 0f) {
                    updated[i] = a.copy(transform = a.transform.copy(position = a.transform.position + sepA))
                    updated[j] = b.copy(transform = b.transform.copy(position = b.transform.position + sepB))
                    continue
                }

                val e = (rbA.restitution + rbB.restitution) * 0.5f
                val jImpulse = -(1f + e) * velAlongNorm / invMassSum
                val impulse = normal * jImpulse

                val newVelA = if (rbA.isStatic) rbA.velocity else rbA.velocity - impulse * invMassA
                val newVelB = if (rbB.isStatic) rbB.velocity else rbB.velocity + impulse * invMassB

                updated[i] = a.copy(
                    transform = a.transform.copy(position = a.transform.position + sepA),
                    rigidBody = rbA.copy(velocity = newVelA)
                )
                updated[j] = b.copy(
                    transform = b.transform.copy(position = b.transform.position + sepB),
                    rigidBody = rbB.copy(velocity = newVelB)
                )
            }
        }

        return updated
    }

    private fun checkEntityCollision(a: Entity, b: Entity): ContactPoint? {
        val rbA = a.rigidBody ?: return null
        val rbB = b.rigidBody ?: return null

        val posA = a.transform.position
        val posB = b.transform.position
        val scaleA = a.transform.scale
        val scaleB = b.transform.scale

        return when {
            rbA.colliderShape == ColliderShape.SPHERE && rbB.colliderShape == ColliderShape.SPHERE -> {
                val rA = scaleA.x * 0.5f
                val rB = scaleB.x * 0.5f
                CollisionDetector.testSphereSphere(posA, rA, posB, rB)
            }
            rbA.colliderShape == ColliderShape.SPHERE && rbB.colliderShape == ColliderShape.BOX -> {
                val rA = scaleA.x * 0.5f
                val halfB = scaleB * 0.5f
                CollisionDetector.testSphereBox(posA, rA, posB, halfB)
            }
            rbA.colliderShape == ColliderShape.BOX && rbB.colliderShape == ColliderShape.SPHERE -> {
                val halfA = scaleA * 0.5f
                val rB = scaleB.x * 0.5f
                val cp = CollisionDetector.testSphereBox(posB, rB, posA, halfA)
                cp?.let { ContactPoint(it.point, -it.normal, it.penetration) }
            }
            rbA.colliderShape == ColliderShape.BOX && rbB.colliderShape == ColliderShape.BOX -> {
                val halfA = scaleA * 0.5f
                val halfB = scaleB * 0.5f
                CollisionDetector.testBoxBox(posA, halfA, posB, halfB)
            }
            else -> null
        }
    }
}
