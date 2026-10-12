package com.example.engine.model

import com.example.engine.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Vertex(
    val position: Vec3,
    val normal: Vec3 = Vec3.UP,
    val uvU: Float = 0f,
    val uvV: Float = 0f,
    val color: Long = 0xFFFFFFFF
)

data class Triangle(
    val i0: Int,
    val i1: Int,
    val i2: Int
)

enum class PrimitiveType {
    CUBE,
    SPHERE,
    CYLINDER,
    PLANE,
    TORUS,
    CAPSULE,
    TERRAIN,
    CHARACTER
}

data class Mesh(
    val id: String,
    val name: String,
    val vertices: List<Vertex>,
    val triangles: List<Triangle>,
    val primitiveType: PrimitiveType = PrimitiveType.CUBE
) {
    val vertexCount: Int get() = vertices.size
    val triangleCount: Int get() = triangles.size

    companion object {
        fun createCube(id: String = "mesh_cube", size: Float = 1f): Mesh {
            val h = size * 0.5f
            val verts = mutableListOf<Vertex>()
            val tris = mutableListOf<Triangle>()

            fun addFace(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, normal: Vec3) {
                val start = verts.size
                verts.add(Vertex(p0, normal, 0f, 0f))
                verts.add(Vertex(p1, normal, 1f, 0f))
                verts.add(Vertex(p2, normal, 1f, 1f))
                verts.add(Vertex(p3, normal, 0f, 1f))
                tris.add(Triangle(start, start + 1, start + 2))
                tris.add(Triangle(start, start + 2, start + 3))
            }

            // Front
            addFace(Vec3(-h, -h, h), Vec3(h, -h, h), Vec3(h, h, h), Vec3(-h, h, h), Vec3(0f, 0f, 1f))
            // Back
            addFace(Vec3(h, -h, -h), Vec3(-h, -h, -h), Vec3(-h, h, -h), Vec3(h, h, -h), Vec3(0f, 0f, -1f))
            // Top
            addFace(Vec3(-h, h, h), Vec3(h, h, h), Vec3(h, h, -h), Vec3(-h, h, -h), Vec3(0f, 1f, 0f))
            // Bottom
            addFace(Vec3(-h, -h, -h), Vec3(h, -h, -h), Vec3(h, -h, h), Vec3(-h, -h, h), Vec3(0f, -1f, 0f))
            // Right
            addFace(Vec3(h, -h, h), Vec3(h, -h, -h), Vec3(h, h, -h), Vec3(h, h, h), Vec3(1f, 0f, 0f))
            // Left
            addFace(Vec3(-h, -h, -h), Vec3(-h, -h, h), Vec3(-h, h, h), Vec3(-h, h, -h), Vec3(-1f, 0f, 0f))

            return Mesh(id, "Cube", verts, tris, PrimitiveType.CUBE)
        }

        fun createSphere(id: String = "mesh_sphere", radius: Float = 0.5f, rings: Int = 12, sectors: Int = 16): Mesh {
            val verts = mutableListOf<Vertex>()
            val tris = mutableListOf<Triangle>()

            val R = 1f / rings.toFloat()
            val S = 1f / sectors.toFloat()

            for (r in 0..rings) {
                val v = r * R
                val theta = v * PI.toFloat()
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)

                for (s in 0..sectors) {
                    val u = s * S
                    val phi = u * 2f * PI.toFloat()
                    val sinPhi = sin(phi)
                    val cosPhi = cos(phi)

                    val x = cosPhi * sinTheta
                    val y = cosTheta
                    val z = sinPhi * sinTheta

                    val norm = Vec3(x, y, z)
                    val pos = norm * radius
                    verts.add(Vertex(pos, norm, u, v))
                }
            }

            for (r in 0 until rings) {
                for (s in 0 until sectors) {
                    val cur = r * (sectors + 1) + s
                    val next = cur + sectors + 1
                    tris.add(Triangle(cur, next, cur + 1))
                    tris.add(Triangle(cur + 1, next, next + 1))
                }
            }

            return Mesh(id, "Sphere", verts, tris, PrimitiveType.SPHERE)
        }

        fun createPlane(id: String = "mesh_plane", width: Float = 10f, depth: Float = 10f, subdivisions: Int = 4): Mesh {
            val verts = mutableListOf<Vertex>()
            val tris = mutableListOf<Triangle>()

            val hw = width * 0.5f
            val hd = depth * 0.5f
            val stepW = width / subdivisions
            val stepD = depth / subdivisions

            for (z in 0..subdivisions) {
                for (x in 0..subdivisions) {
                    val px = -hw + x * stepW
                    val pz = -hd + z * stepD
                    val u = x.toFloat() / subdivisions
                    val v = z.toFloat() / subdivisions
                    verts.add(Vertex(Vec3(px, 0f, pz), Vec3.UP, u, v))
                }
            }

            val stride = subdivisions + 1
            for (z in 0 until subdivisions) {
                for (x in 0 until subdivisions) {
                    val i0 = z * stride + x
                    val i1 = (z + 1) * stride + x
                    val i2 = i0 + 1
                    val i3 = i1 + 1
                    tris.add(Triangle(i0, i1, i2))
                    tris.add(Triangle(i2, i1, i3))
                }
            }

            return Mesh(id, "Ground Plane", verts, tris, PrimitiveType.PLANE)
        }

        fun createCylinder(id: String = "mesh_cylinder", radius: Float = 0.5f, height: Float = 1f, segments: Int = 16): Mesh {
            val verts = mutableListOf<Vertex>()
            val tris = mutableListOf<Triangle>()
            val hh = height * 0.5f

            // Side vertices
            for (i in 0..segments) {
                val angle = (i.toFloat() / segments) * 2f * PI.toFloat()
                val x = cos(angle) * radius
                val z = sin(angle) * radius
                val norm = Vec3(cos(angle), 0f, sin(angle)).normalized()
                val u = i.toFloat() / segments

                verts.add(Vertex(Vec3(x, hh, z), norm, u, 1f))
                verts.add(Vertex(Vec3(x, -hh, z), norm, u, 0f))
            }

            for (i in 0 until segments) {
                val top1 = i * 2
                val bot1 = i * 2 + 1
                val top2 = (i + 1) * 2
                val bot2 = (i + 1) * 2 + 1
                tris.add(Triangle(top1, bot1, top2))
                tris.add(Triangle(top2, bot1, bot2))
            }

            return Mesh(id, "Cylinder", verts, tris, PrimitiveType.CYLINDER)
        }

        fun createTorus(id: String = "mesh_torus", mainRadius: Float = 0.7f, tubeRadius: Float = 0.25f, radialSegs: Int = 12, tubeSegs: Int = 16): Mesh {
            val verts = mutableListOf<Vertex>()
            val tris = mutableListOf<Triangle>()

            for (j in 0..radialSegs) {
                val v = j.toFloat() / radialSegs
                val uAngle = v * 2f * PI.toFloat()
                for (i in 0..tubeSegs) {
                    val u = i.toFloat() / tubeSegs
                    val vAngle = u * 2f * PI.toFloat()

                    val x = (mainRadius + tubeRadius * cos(vAngle)) * cos(uAngle)
                    val y = tubeRadius * sin(vAngle)
                    val z = (mainRadius + tubeRadius * cos(vAngle)) * sin(uAngle)

                    val cx = mainRadius * cos(uAngle)
                    val cz = mainRadius * sin(uAngle)
                    val norm = (Vec3(x, y, z) - Vec3(cx, 0f, cz)).normalized()

                    verts.add(Vertex(Vec3(x, y, z), norm, u, v))
                }
            }

            for (j in 0 until radialSegs) {
                for (i in 0 until tubeSegs) {
                    val a = (tubeSegs + 1) * j + i
                    val b = (tubeSegs + 1) * (j + 1) + i
                    val c = (tubeSegs + 1) * (j + 1) + i + 1
                    val d = (tubeSegs + 1) * j + i + 1
                    tris.add(Triangle(a, b, d))
                    tris.add(Triangle(b, c, d))
                }
            }

            return Mesh(id, "Torus", verts, tris, PrimitiveType.TORUS)
        }

        fun createProceduralTerrain(id: String = "mesh_terrain", size: Float = 12f, resolution: Int = 10): Mesh {
            val verts = mutableListOf<Vertex>()
            val tris = mutableListOf<Triangle>()
            val half = size * 0.5f
            val step = size / resolution

            for (z in 0..resolution) {
                for (x in 0..resolution) {
                    val px = -half + x * step
                    val pz = -half + z * step
                    // Simple procedural hill function
                    val dist = kotlin.math.sqrt(px * px + pz * pz)
                    val py = (sin(px * 0.8f) * cos(pz * 0.8f) * 0.8f + sin(dist * 0.5f) * 0.5f).coerceAtLeast(0f)
                    val norm = Vec3(-cos(px * 0.8f) * 0.4f, 1f, -cos(pz * 0.8f) * 0.4f).normalized()
                    verts.add(Vertex(Vec3(px, py, pz), norm, x.toFloat() / resolution, z.toFloat() / resolution))
                }
            }

            val stride = resolution + 1
            for (z in 0 until resolution) {
                for (x in 0 until resolution) {
                    val i0 = z * stride + x
                    val i1 = (z + 1) * stride + x
                    val i2 = i0 + 1
                    val i3 = i1 + 1
                    tris.add(Triangle(i0, i1, i2))
                    tris.add(Triangle(i2, i1, i3))
                }
            }

            return Mesh(id, "Hills Terrain", verts, tris, PrimitiveType.TERRAIN)
        }
    }
}
