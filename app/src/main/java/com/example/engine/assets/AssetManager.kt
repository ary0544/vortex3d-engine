package com.example.engine.assets

import com.example.engine.lua.LuaScriptAsset
import com.example.engine.lua.LuaScriptLibrary
import com.example.engine.model.ColliderShape
import com.example.engine.model.Entity
import com.example.engine.model.Material
import com.example.engine.model.Mesh
import com.example.engine.model.MeshRendererComponent
import com.example.engine.model.RigidBodyComponent
import com.example.engine.model.ShaderProgram
import com.example.engine.model.TransformComponent
import com.example.engine.math.Vec3

data class PrefabAsset(
    val id: String,
    val name: String,
    val icon: String,
    val createEntity: (pos: Vec3) -> Entity
)

class AssetManager {
    val meshes = mutableMapOf<String, Mesh>()
    val materials = mutableMapOf<String, Material>()
    val shaders = mutableMapOf<String, ShaderProgram>()
    val scripts = mutableMapOf<String, LuaScriptAsset>()
    val prefabs = mutableListOf<PrefabAsset>()

    init {
        loadDefaultAssets()
    }

    private fun loadDefaultAssets() {
        // Register primitive meshes
        val cube = Mesh.createCube()
        val sphere = Mesh.createSphere()
        val cylinder = Mesh.createCylinder()
        val torus = Mesh.createTorus()
        val plane = Mesh.createPlane()
        val terrain = Mesh.createProceduralTerrain()

        meshes[cube.id] = cube
        meshes[sphere.id] = sphere
        meshes[cylinder.id] = cylinder
        meshes[torus.id] = torus
        meshes[plane.id] = plane
        meshes[terrain.id] = terrain

        // Register shaders
        val pbr = ShaderProgram.createPbrShader()
        val toon = ShaderProgram.createToonShader()
        val holo = ShaderProgram.createHologramShader()
        val water = ShaderProgram.createWaterShader()

        shaders[pbr.id] = pbr
        shaders[toon.id] = toon
        shaders[holo.id] = holo
        shaders[water.id] = water

        // Register materials
        val matDef = Material.defaultMaterial()
        val matGold = Material.createPbrGold()
        val matHolo = Material.createCyberHologram()
        val matRuby = Material.createToonRuby()
        val matWater = Material.createWaterMaterial()
        val matEmerald = Material.createNeonEmerald()

        materials[matDef.id] = matDef
        materials[matGold.id] = matGold
        materials[matHolo.id] = matHolo
        materials[matRuby.id] = matRuby
        materials[matWater.id] = matWater
        materials[matEmerald.id] = matEmerald

        // Register default Lua scripts
        LuaScriptLibrary.templates.forEach { script ->
            scripts[script.id] = script
        }

        // Register Prefabs
        prefabs.add(PrefabAsset(
            id = "prefab_cube",
            name = "Physics Cube",
            icon = "Cube",
            createEntity = { pos ->
                Entity(
                    id = "ent_cube_${java.util.UUID.randomUUID().toString().take(8)}",
                    name = "Dynamic Cube",
                    transform = TransformComponent(position = pos),
                    meshRenderer = MeshRendererComponent("mesh_cube", "mat_ruby"),
                    rigidBody = RigidBodyComponent(mass = 1.0f, restitution = 0.5f, colliderShape = ColliderShape.BOX)
                )
            }
        ))

        prefabs.add(PrefabAsset(
            id = "prefab_sphere",
            name = "Physics Sphere",
            icon = "Sphere",
            createEntity = { pos ->
                Entity(
                    id = "ent_sphere_${java.util.UUID.randomUUID().toString().take(8)}",
                    name = "Bouncing Sphere",
                    transform = TransformComponent(position = pos),
                    meshRenderer = MeshRendererComponent("mesh_sphere", "mat_gold"),
                    rigidBody = RigidBodyComponent(mass = 1.5f, restitution = 0.8f, colliderShape = ColliderShape.SPHERE)
                )
            }
        ))

        prefabs.add(PrefabAsset(
            id = "prefab_portal",
            name = "Holo Portal",
            icon = "Torus",
            createEntity = { pos ->
                Entity(
                    id = "ent_portal_${java.util.UUID.randomUUID().toString().take(8)}",
                    name = "Cyber Portal",
                    transform = TransformComponent(position = pos, rotation = Vec3(45f, 0f, 0f)),
                    meshRenderer = MeshRendererComponent("mesh_torus", "mat_hologram")
                )
            }
        ))

        prefabs.add(PrefabAsset(
            id = "prefab_terrain",
            name = "Landscape Hills",
            icon = "Terrain",
            createEntity = { pos ->
                Entity(
                    id = "ent_terrain_${java.util.UUID.randomUUID().toString().take(8)}",
                    name = "Procedural Terrain",
                    transform = TransformComponent(position = pos),
                    meshRenderer = MeshRendererComponent("mesh_terrain", "mat_emerald"),
                    rigidBody = RigidBodyComponent(isStatic = true, restitution = 0.2f, colliderShape = ColliderShape.PLANE)
                )
            }
        ))
    }
}
