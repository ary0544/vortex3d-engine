package com.example.engine.model

import com.example.engine.math.Vec3

data class Scene(
    val id: String = "scene_default",
    val name: String = "Main Scene",
    val entities: List<Entity> = emptyList(),
    val ambientColor: Long = 0xFF1A2238,
    val skyColor: Long = 0xFF0D1117,
    val lightDirection: Vec3 = Vec3(0.5f, 1.0f, 0.3f).normalized(),
    val lightColor: Long = 0xFFFFF8E7,
    val gravity: Vec3 = Vec3(0f, -9.81f, 0f),
    val physicsSubsteps: Int = 2
) {
    fun findEntity(id: String): Entity? = entities.firstOrNull { it.id == id }
    fun findEntityByName(name: String): Entity? = entities.firstOrNull { it.name == name }

    companion object {
        fun createDefaultPhysicsPlayground(): Scene {
            val entities = listOf(
                // Ground Plane
                Entity(
                    id = "ent_ground",
                    name = "Ground",
                    transform = TransformComponent(
                        position = Vec3(0f, 0f, 0f),
                        rotation = Vec3.ZERO,
                        scale = Vec3(1f, 1f, 1f)
                    ),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_plane", materialId = "mat_default"),
                    rigidBody = RigidBodyComponent(isStatic = true, restitution = 0.5f, friction = 0.4f, colliderShape = ColliderShape.PLANE)
                ),
                // Hero / Dynamic Player Sphere with Lua Script
                Entity(
                    id = "ent_hero_orb",
                    name = "Player Orb",
                    transform = TransformComponent(
                        position = Vec3(0f, 4.5f, 0f),
                        rotation = Vec3.ZERO,
                        scale = Vec3(1f, 1f, 1f)
                    ),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_sphere", materialId = "mat_gold"),
                    rigidBody = RigidBodyComponent(mass = 2.0f, isStatic = false, restitution = 0.75f, friction = 0.3f, colliderShape = ColliderShape.SPHERE),
                    luaScript = LuaScriptComponent(
                        scriptId = "script_hero",
                        scriptName = "HeroController.lua",
                        scriptContent = """
                            -- Player Orb Controller
                            function init()
                                entity:set_color(1.0, 0.85, 0.2, 1.0)
                                engine:log("Hero Orb Initialized!")
                            end

                            function update(dt)
                                -- Read virtual joystick input
                                local h = input:get_axis("Horizontal")
                                local v = input:get_axis("Vertical")
                                if h ~= 0 or v ~= 0 then
                                    physics:apply_force(h * 15.0, 0, v * 15.0)
                                end
                                
                                if input:is_action_pressed("Jump") then
                                    physics:apply_impulse(0, 8.0, 0)
                                    engine:play_sound("jump")
                                end
                            end

                            function on_collision(other)
                                engine:play_sound("impact")
                                engine:log("Impact with " .. other)
                            end
                        """.trimIndent()
                    )
                ),
                // Glowing Cyber Hologram Torus
                Entity(
                    id = "ent_torus_hologram",
                    name = "Cyber Portal",
                    transform = TransformComponent(
                        position = Vec3(-2.5f, 2.0f, -1.0f),
                        rotation = Vec3(45f, 30f, 0f),
                        scale = Vec3(1.2f, 1.2f, 1.2f)
                    ),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_torus", materialId = "mat_hologram"),
                    luaScript = LuaScriptComponent(
                        scriptId = "script_spin",
                        scriptName = "Rotator.lua",
                        scriptContent = """
                            function update(dt)
                                entity:rotate(0, 45.0 * dt, 25.0 * dt)
                            end
                        """.trimIndent()
                    )
                ),
                // Physics Tower Cube 1
                Entity(
                    id = "ent_box_1",
                    name = "Crate Alpha",
                    transform = TransformComponent(
                        position = Vec3(2.0f, 1.0f, 0f),
                        rotation = Vec3.ZERO,
                        scale = Vec3(0.9f, 0.9f, 0.9f)
                    ),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_cube", materialId = "mat_ruby"),
                    rigidBody = RigidBodyComponent(mass = 1.0f, isStatic = false, restitution = 0.4f, colliderShape = ColliderShape.BOX)
                ),
                // Physics Tower Cube 2 (Stacked)
                Entity(
                    id = "ent_box_2",
                    name = "Crate Beta",
                    transform = TransformComponent(
                        position = Vec3(2.0f, 2.2f, 0f),
                        rotation = Vec3(0f, 15f, 0f),
                        scale = Vec3(0.9f, 0.9f, 0.9f)
                    ),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_cube", materialId = "mat_emerald"),
                    rigidBody = RigidBodyComponent(mass = 1.0f, isStatic = false, restitution = 0.4f, colliderShape = ColliderShape.BOX)
                ),
                // Water Caustics Pond
                Entity(
                    id = "ent_water_pool",
                    name = "Water Pool",
                    transform = TransformComponent(
                        position = Vec3(0f, 0.1f, -3.5f),
                        rotation = Vec3.ZERO,
                        scale = Vec3(0.5f, 1f, 0.5f)
                    ),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_plane", materialId = "mat_water")
                ),
                // Directional Sun Light
                Entity(
                    id = "ent_sun",
                    name = "Sun Light",
                    transform = TransformComponent(position = Vec3(5f, 10f, 5f)),
                    light = LightComponent(type = LightType.DIRECTIONAL, color = 0xFFFFF5D6, intensity = 1.2f)
                )
            )

            return Scene(
                id = "scene_playground",
                name = "Physics & Lua Playground",
                entities = entities
            )
        }

        fun createBlank3DScene(name: String = "Blank 3D Scene"): Scene {
            val entities = listOf(
                Entity(
                    id = "ent_ground",
                    name = "Grid Floor",
                    transform = TransformComponent(
                        position = Vec3(0f, 0f, 0f),
                        rotation = Vec3.ZERO,
                        scale = Vec3(1f, 1f, 1f)
                    ),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_plane", materialId = "mat_default"),
                    rigidBody = RigidBodyComponent(isStatic = true, restitution = 0.5f, friction = 0.4f, colliderShape = ColliderShape.PLANE)
                ),
                Entity(
                    id = "ent_sun",
                    name = "Directional Light",
                    transform = TransformComponent(position = Vec3(4f, 8f, 4f)),
                    light = LightComponent(type = LightType.DIRECTIONAL, color = 0xFFFFF5D6, intensity = 1.0f)
                )
            )
            return Scene(
                id = "scene_${System.currentTimeMillis()}",
                name = name,
                entities = entities
            )
        }

        fun createFpsArena(name: String = "FPS Action Arena"): Scene {
            val entities = listOf(
                Entity(
                    id = "ent_arena_floor",
                    name = "Arena Floor",
                    transform = TransformComponent(position = Vec3(0f, 0f, 0f), scale = Vec3(1.5f, 1f, 1.5f)),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_plane", materialId = "mat_default"),
                    rigidBody = RigidBodyComponent(isStatic = true, colliderShape = ColliderShape.PLANE)
                ),
                Entity(
                    id = "ent_player",
                    name = "FPS Player Controller",
                    transform = TransformComponent(position = Vec3(0f, 1.5f, 0f)),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_sphere", materialId = "mat_gold"),
                    rigidBody = RigidBodyComponent(mass = 1.0f, isStatic = false, colliderShape = ColliderShape.SPHERE),
                    luaScript = LuaScriptComponent(
                        scriptId = "script_fps",
                        scriptName = "PlayerFPS.lua",
                        scriptContent = """
                            function update(dt)
                                local h = input:get_axis("Horizontal")
                                local v = input:get_axis("Vertical")
                                if h ~= 0 or v ~= 0 then
                                    physics:apply_force(h * 20.0, 0, v * 20.0)
                                end
                                if input:is_action_pressed("Jump") then
                                    physics:apply_impulse(0, 10.0, 0)
                                    engine:play_sound("jump")
                                end
                            end
                        """.trimIndent()
                    )
                ),
                Entity(
                    id = "ent_pillar_1",
                    name = "Obstacle Pillar A",
                    transform = TransformComponent(position = Vec3(-3f, 1.5f, -2f), scale = Vec3(0.8f, 2f, 0.8f)),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_cube", materialId = "mat_ruby"),
                    rigidBody = RigidBodyComponent(isStatic = true, colliderShape = ColliderShape.BOX)
                ),
                Entity(
                    id = "ent_pillar_2",
                    name = "Obstacle Pillar B",
                    transform = TransformComponent(position = Vec3(3f, 1.5f, 2f), scale = Vec3(0.8f, 2f, 0.8f)),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_cube", materialId = "mat_emerald"),
                    rigidBody = RigidBodyComponent(isStatic = true, colliderShape = ColliderShape.BOX)
                ),
                Entity(
                    id = "ent_target_orb",
                    name = "Target Sphere",
                    transform = TransformComponent(position = Vec3(0f, 3f, 4f), scale = Vec3(0.7f, 0.7f, 0.7f)),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_sphere", materialId = "mat_hologram"),
                    rigidBody = RigidBodyComponent(mass = 0.5f, isStatic = false, restitution = 0.9f, colliderShape = ColliderShape.SPHERE)
                ),
                Entity(
                    id = "ent_arena_light",
                    name = "Arena Sun",
                    transform = TransformComponent(position = Vec3(5f, 10f, 5f)),
                    light = LightComponent(type = LightType.DIRECTIONAL, color = 0xFFFFE0B2, intensity = 1.3f)
                )
            )
            return Scene(
                id = "scene_fps_${System.currentTimeMillis()}",
                name = name,
                entities = entities
            )
        }

        fun createCyberpunkGlow(name: String = "Cyberpunk Neo World"): Scene {
            val entities = listOf(
                Entity(
                    id = "ent_cyber_ground",
                    name = "Reflective Ground",
                    transform = TransformComponent(position = Vec3(0f, 0f, 0f), scale = Vec3(2f, 1f, 2f)),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_plane", materialId = "mat_water"),
                    rigidBody = RigidBodyComponent(isStatic = true, colliderShape = ColliderShape.PLANE)
                ),
                Entity(
                    id = "ent_neon_torus",
                    name = "Quantum Gate Torus",
                    transform = TransformComponent(position = Vec3(0f, 3f, 0f), scale = Vec3(2f, 2f, 2f)),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_torus", materialId = "mat_hologram"),
                    luaScript = LuaScriptComponent(
                        scriptId = "script_gate_spin",
                        scriptName = "GateSpin.lua",
                        scriptContent = """
                            function update(dt)
                                entity:rotate(20 * dt, 40 * dt, 10 * dt)
                            end
                        """.trimIndent()
                    )
                ),
                Entity(
                    id = "ent_cyber_orb",
                    name = "Energy Core",
                    transform = TransformComponent(position = Vec3(0f, 3f, 0f), scale = Vec3(0.8f, 0.8f, 0.8f)),
                    meshRenderer = MeshRendererComponent(meshId = "mesh_sphere", materialId = "mat_gold")
                ),
                Entity(
                    id = "ent_light_blue",
                    name = "Cyber Light Blue",
                    transform = TransformComponent(position = Vec3(-5f, 6f, 0f)),
                    light = LightComponent(type = LightType.POINT, color = 0xFF00E5FF, intensity = 2.0f)
                ),
                Entity(
                    id = "ent_light_pink",
                    name = "Cyber Light Magenta",
                    transform = TransformComponent(position = Vec3(5f, 6f, 0f)),
                    light = LightComponent(type = LightType.POINT, color = 0xFFFF4081, intensity = 2.0f)
                )
            )
            return Scene(
                id = "scene_cyber_${System.currentTimeMillis()}",
                name = name,
                skyColor = 0xFF050510,
                ambientColor = 0xFF120024,
                entities = entities
            )
        }
    }
}
