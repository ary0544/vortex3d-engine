package com.example.engine.lua

data class LuaScriptAsset(
    val id: String,
    val name: String,
    val description: String,
    val content: String
)

object LuaScriptLibrary {
    val defaultScripts = listOf(
        LuaScriptAsset(
            id = "script_orbit",
            name = "Orbit Rotator",
            description = "Rotates entity around Y axis continuously.",
            content = """
                -- Orbit Rotator Script
                function onUpdate(entity, dt)
                    local rot = entity.transform.rotation
                    entity.transform.rotation = rot + Vec3(0, 45 * dt, 0)
                end
            """.trimIndent()
        ),
        LuaScriptAsset(
            id = "script_bounce",
            name = "Vertical Bounce",
            description = "Bounces entity up and down using sine wave.",
            content = """
                -- Vertical Bounce Script
                local timeElapsed = 0
                function onUpdate(entity, dt)
                    timeElapsed = timeElapsed + dt
                    local pos = entity.transform.position
                    pos.y = 1.0 + math.sin(timeElapsed * 3) * 1.5
                    entity.transform.position = pos
                end
            """.trimIndent()
        )
    )

    val templates = defaultScripts
}
