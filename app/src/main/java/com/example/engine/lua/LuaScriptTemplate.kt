package com.example.engine.lua

data class LuaScriptAsset(
    val id: String,
    val name: String,
    val description: String,
    val content: String
)

object LuaScriptLibrary {
    val templates = listOf(
        LuaScriptAsset(
            id = "script_hero",
            name = "HeroController.lua",
            description = "Dual-stick player movement with jump impulse and collision detection",
            content = """
                -- Vortex3D Hero Character Controller
                local move_speed = 18.0
                local jump_force = 9.0

                function init()
                    engine:log("Hero controller activated!")
                end

                function update(dt)
                    local h = input:get_axis("Horizontal")
                    local v = input:get_axis("Vertical")
                    
                    if h ~= 0 or v ~= 0 then
                        physics:apply_force(h * 18.0, 0, v * 18.0)
                    end
                    
                    if input:is_action_pressed("Jump") then
                        physics:apply_impulse(0, 9.0, 0)
                        engine:play_sound("jump")
                    end
                end

                function on_collision(other)
                    engine:play_sound("impact")
                    engine:log("Collision with: " .. other)
                end
            """.trimIndent()
        ),
        LuaScriptAsset(
            id = "script_rotator",
            name = "Rotator.lua",
            description = "Continuous 3-axis rotation for pickups, portals, and spinning hazards",
            content = """
                -- Continuous Object Rotator
                function update(dt)
                    entity:rotate(0, 45.0 * dt, 20.0 * dt)
                end
            """.trimIndent()
        ),
        LuaScriptAsset(
            id = "script_bouncing_orb",
            name = "BouncingOrb.lua",
            description = "Elastic bouncing sphere with impulse boosts on tap",
            content = """
                -- Bouncing Orb with sound
                function init()
                    engine:log("Bouncing Orb ready")
                end

                function update(dt)
                    if input:is_action_pressed("Fire") then
                        physics:apply_impulse(0, 7.5, 0)
                        engine:play_sound("laser")
                    end
                end

                function on_collision(other)
                    engine:play_sound("impact")
                end
            """.trimIndent()
        ),
        LuaScriptAsset(
            id = "script_daynight",
            name = "DayNightCycle.lua",
            description = "Simulates rotating sun and dynamic ambient time-of-day progression",
            content = """
                -- Day-Night Cycle for Directional Light
                function update(dt)
                    entity:rotate(10.0 * dt, 5.0 * dt, 0)
                end
            """.trimIndent()
        ),
        LuaScriptAsset(
            id = "script_homing",
            name = "HomingProjectile.lua",
            description = "Accelerates forward with torque alignment towards target",
            content = """
                -- Homing Missile Controller
                function update(dt)
                    entity:translate(0, 0, 5.0 * dt)
                end

                function on_collision(other)
                    engine:play_sound("explosion")
                    engine:log("Projectile exploded against " .. other)
                end
            """.trimIndent()
        )
    )
}
