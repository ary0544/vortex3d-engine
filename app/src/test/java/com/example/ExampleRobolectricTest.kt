package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.assets.AssetManager
import com.example.engine.lua.EngineCallbacks
import com.example.engine.lua.InputState
import com.example.engine.lua.LuaInterpreter
import com.example.engine.math.Vec3
import com.example.engine.model.ColliderShape
import com.example.engine.model.Entity
import com.example.engine.model.RigidBodyComponent
import com.example.engine.model.TransformComponent
import com.example.engine.physics.CollisionDetector
import com.example.engine.physics.PhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Vortex3D`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vortex3D", appName)
    }

    @Test
    fun `test 3D vector math operations`() {
        val v1 = Vec3(1f, 2f, 3f)
        val v2 = Vec3(4f, 5f, 6f)
        val sum = v1 + v2
        assertEquals(5f, sum.x, 0.001f)
        assertEquals(7f, sum.y, 0.001f)
        assertEquals(9f, sum.z, 0.001f)

        val dot = v1.dot(v2)
        assertEquals(32f, dot, 0.001f)

        val cross = Vec3.RIGHT.cross(Vec3.UP)
        assertEquals(Vec3.FORWARD.z, cross.z, 0.001f)
    }

    @Test
    fun `test physics collision detection`() {
        val sphereA = Vec3(0f, 1f, 0f)
        val sphereB = Vec3(0f, 1.5f, 0f)
        val contact = CollisionDetector.testSphereSphere(sphereA, 0.5f, sphereB, 0.5f)
        assertNotNull(contact)
        assertTrue(contact!!.penetration > 0f)
    }

    @Test
    fun `test embedded Lua interpreter execution`() {
        var logOutput = ""
        val callbacks = object : EngineCallbacks {
            override fun log(tag: String, message: String) {
                logOutput = message
            }
            override fun playSound(soundName: String) {}
            override fun updateEntity(updated: Entity) {}
            override fun getEntity(id: String): Entity? = null
        }

        val entity = Entity(
            id = "test_ent",
            name = "TestOrb",
            transform = TransformComponent(position = Vec3(0f, 0f, 0f))
        )

        val interp = LuaInterpreter(entity.id, callbacks)
        val script = """
            function init()
                engine:log("Initialized!")
            end
            function update(dt)
                entity:translate(0, 1.0, 0)
            end
        """.trimIndent()

        val updated = interp.executeUpdate(script, 0.016f, entity, InputState())
        assertEquals(1.0f, updated.transform.position.y, 0.001f)
        assertEquals("Initialized!", logOutput)
    }

    @Test
    fun `test asset manager loads primitives and shaders`() {
        val assets = AssetManager()
        assertTrue(assets.meshes.containsKey("mesh_cube"))
        assertTrue(assets.meshes.containsKey("mesh_sphere"))
        assertTrue(assets.shaders.containsKey("shader_pbr"))
        assertTrue(assets.shaders.containsKey("shader_toon"))
        assertTrue(assets.shaders.containsKey("shader_hologram"))
    }

    @Test
    fun `test storage manager directories and formatting`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val projectDir = com.example.engine.storage.StorageManager.getAppProjectsDir(context)
        assertTrue(projectDir.exists())

        val formatted1 = com.example.engine.storage.StorageManager.formatFileSize(500)
        assertEquals("500 B", formatted1)

        val formatted2 = com.example.engine.storage.StorageManager.formatFileSize(2048)
        assertTrue(formatted2.contains("KB"))
    }

    @Test
    fun `test Lua if condition skips jump impulse when not pressed`() {
        val callbacks = object : EngineCallbacks {
            override fun log(tag: String, message: String) {}
            override fun playSound(soundName: String) {}
            override fun updateEntity(updated: Entity) {}
            override fun getEntity(id: String): Entity? = null
        }

        val entity = Entity(
            id = "test_jump",
            name = "Hero",
            transform = TransformComponent(position = Vec3(0f, 1f, 0f)),
            rigidBody = RigidBodyComponent(mass = 1.0f, velocity = Vec3.ZERO)
        )

        val interp = LuaInterpreter(entity.id, callbacks)
        val script = """
            function update(dt)
                if input_jump then
                    physics:apply_impulse(0, 9.0, 0)
                end
            end
        """.trimIndent()

        // 1. Without jumping, velocity Y must remain 0 (not added to millions!)
        val stateNoJump = InputState(actionJump = false)
        val result1 = interp.executeUpdate(script, 0.016f, entity, stateNoJump)
        assertEquals(0f, result1.rigidBody?.velocity?.y ?: 0f, 0.001f)

        // 2. With jump active, impulse should be applied
        val stateJump = InputState(actionJump = true)
        val result2 = interp.executeUpdate(script, 0.016f, entity, stateJump)
        assertEquals(9.0f, result2.rigidBody?.velocity?.y ?: 0f, 0.001f)
    }

    @Test
    fun `test Odyssey controller does not shoot camera or entity to millions`() {
        var cameraTarget = Vec3.ZERO
        val callbacks = object : EngineCallbacks {
            override fun log(tag: String, message: String) {}
            override fun playSound(soundName: String) {}
            override fun updateEntity(updated: Entity) {}
            override fun getEntity(id: String): Entity? = null
            override fun setCameraTarget(target: Vec3) {
                cameraTarget = target
            }
        }

        var entity = Entity(
            id = "odyssey_hero",
            name = "OdysseyHero",
            transform = TransformComponent(position = Vec3(0f, 1f, 0f)),
            rigidBody = RigidBodyComponent(mass = 1.0f, velocity = Vec3.ZERO)
        )

        val interp = LuaInterpreter(entity.id, callbacks)
        val odysseyScript = com.example.engine.lua.LuaScriptLibrary.defaultScripts.find { it.id == "script_odyssey" }!!.content

        // Simulate 60 frames without jump
        for (i in 0 until 60) {
            entity = interp.executeUpdate(odysseyScript, 0.016f, entity, InputState(horizontal = 0.5f, vertical = 0.5f, actionJump = false))
        }

        // Camera target Y must stay close to character, nowhere near millions
        assertTrue("Camera target Y must be reasonable: ${cameraTarget.y}", cameraTarget.y in 0f..10f)
        assertTrue("Entity Y must be reasonable: ${entity.transform.position.y}", entity.transform.position.y in 0f..10f)
    }

    @Test
    fun `test is_grounded prevents mid-air repeated jump`() {
        val callbacks = object : EngineCallbacks {
            override fun log(tag: String, message: String) {}
            override fun playSound(soundName: String) {}
            override fun updateEntity(updated: Entity) {}
            override fun getEntity(id: String): Entity? = null
        }

        // Entity in air (y = 5.0f, isGrounded = false)
        val airEntity = Entity(
            id = "air_hero",
            name = "AirHero",
            transform = TransformComponent(position = Vec3(0f, 5.0f, 0f)),
            rigidBody = RigidBodyComponent(mass = 1.0f, velocity = Vec3.ZERO, isGrounded = false)
        )

        val interp = LuaInterpreter(airEntity.id, callbacks)
        val script = """
            function update(dt)
                if input_jump and is_grounded then
                    physics:apply_impulse(0, 8.0, 0)
                end
            end
        """.trimIndent()

        // Jump triggered while in mid-air must be ignored
        val resAir = interp.executeUpdate(script, 0.016f, airEntity, InputState(actionJump = true))
        assertEquals(0f, resAir.rigidBody?.velocity?.y ?: 0f, 0.001f)

        // Entity on ground (isGrounded = true)
        val groundedEntity = airEntity.copy(
            transform = TransformComponent(position = Vec3(0f, 0.5f, 0f)),
            rigidBody = RigidBodyComponent(mass = 1.0f, velocity = Vec3.ZERO, isGrounded = true)
        )
        val resGrounded = interp.executeUpdate(script, 0.016f, groundedEntity, InputState(actionJump = true))
        assertEquals(8.0f, resGrounded.rigidBody?.velocity?.y ?: 0f, 0.001f)
    }

    @Test
    fun `test physics engine grounds falling sphere cleanly`() {
        val engine = com.example.engine.physics.PhysicsEngine()
        val sphere = Entity(
            id = "test_sphere",
            name = "Sphere",
            transform = TransformComponent(position = Vec3(0f, 0.5f, 0f)),
            rigidBody = RigidBodyComponent(mass = 1.0f, velocity = Vec3(0f, -0.1f, 0f), restitution = 0.05f, colliderShape = ColliderShape.SPHERE)
        )

        val updated = engine.step(listOf(sphere), 0.016f)
        val rb = updated[0].rigidBody
        assertNotNull(rb)
        assertTrue("Sphere should be marked as grounded when resting on floor", rb!!.isGrounded)
    }

    @Test
    fun `test formatFullInfoString generates complete GLB specification`() {
        val bbox = com.example.engine.model.BoundingBox3D(
            min = Vec3(-1f, 0f, -1f),
            max = Vec3(1f, 2f, 1f),
            center = Vec3(0f, 1f, 0f),
            size = Vec3(2f, 2f, 2f)
        )
        val rawMeta = com.example.engine.model.GlbMetadata(
            fileName = "warrior.glb",
            fileSizeFormatted = "3.2 MB",
            modelName = "WarriorModel",
            meshCount = 2,
            vertexCount = 1450,
            triangleCount = 2800,
            materialCount = 1,
            textureCount = 1,
            animationCount = 2,
            roughness = 0.5f,
            metallic = 0.2f,
            baseColorHex = "#A0A0A0",
            recommendedRole = "Kostum Karakter TPP (Dynamic Actor)",
            recommendedFolder = "assets/characters/",
            recommendationTip = "Cocok untuk karakter hero utama."
        )
        val anims = listOf(
            com.example.engine.model.GlbAnimationInfo(0, "Idle", 2.0f, 4, 4),
            com.example.engine.model.GlbAnimationInfo(1, "Run", 1.2f, 6, 6)
        )
        val bones = listOf(
            com.example.engine.model.GlbNodeInfo(
                index = 0,
                name = "Hips",
                type = com.example.engine.model.GlbNodeType.BONE,
                children = listOf(
                    com.example.engine.model.GlbNodeInfo(1, "Spine", com.example.engine.model.GlbNodeType.BONE)
                )
            )
        )

        val inspection = com.example.engine.model.GlbInspectionResult(
            mesh = com.example.engine.model.Mesh.createCube("warrior_mesh"),
            material = com.example.engine.model.Material.defaultMaterial(),
            modelName = "WarriorModel",
            boundingBox = bbox,
            suggestedScale = 1.0f,
            rootNodes = bones,
            totalNodesCount = 2,
            boneCount = 2,
            animations = anims,
            rawMetadata = rawMeta
        )

        val text = inspection.formatFullInfoString()
        assertTrue(text.contains("warrior.glb"))
        assertTrue(text.contains("WarriorModel"))
        assertTrue(text.contains("[GEOMETRI & MESH]"))
        assertTrue(text.contains("Bounding Box Size"))
        assertTrue(text.contains("[SKELETON & RIGGING TULANG]"))
        assertTrue(text.contains("Hips"))
        assertTrue(text.contains("[KLIP ANIMASI TERTANAM]"))
        assertTrue(text.contains("Idle"))
        assertTrue(text.contains("Run"))
        assertTrue(text.contains("assets/characters/"))
    }
}

