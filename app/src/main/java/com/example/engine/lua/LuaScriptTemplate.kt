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
            id = "script_hero",
            name = "TPP Hero Controller",
            description = "Kontrol pergerakan karakter TPP, lompatan fisika, dan audio aksi.",
            content = """
                -- TPP Character Movement & Jump
                function init()
                    engine:log("Karakter TPP siap dikendalikan")
                end

                function update(dt)
                    -- Gerakan horizontal & vertikal dari joystick
                    if input_h ~= 0 or input_v ~= 0 then
                        physics:apply_force(input_h * 15.0, 0, input_v * 15.0)
                        entity:rotate(0, input_h * 90.0 * dt, 0)
                    end

                    -- Lompat saat tombol jump ditekan
                    if input_jump then
                        physics:apply_impulse(0, 8.5, 0)
                        engine:play_sound("jump")
                    end
                end

                function on_collision(other)
                    engine:log("Karakter mendarat di: " .. other)
                end
            """.trimIndent()
        ),
        LuaScriptAsset(
            id = "script_orbit",
            name = "Orbit Rotator",
            description = "Memutar entitas objek secara kontinu pada sumbu Y.",
            content = """
                -- Orbit Rotator Script
                function update(dt)
                    entity:rotate(0, 45.0 * dt, 0)
                end
            """.trimIndent()
        ),
        LuaScriptAsset(
            id = "script_terrain",
            name = "Permukaan Tanah GLB",
            description = "Logika pijakan dan deteksi tabrakan lantai/terrain open-world.",
            content = """
                -- Permukaan Kostum GLB Ground Script
                function init()
                    engine:log("Permukaan dunia GLB aktif")
                end

                function on_collision(other)
                    engine:log("Permukaan menopang entitas: " .. other)
                end
            """.trimIndent()
        ),
        LuaScriptAsset(
            id = "script_bounce",
            name = "Vertical Bounce",
            description = "Memberikan gaya pegas atau dorongan vertikal berulang.",
            content = """
                -- Vertical Bounce Script
                function update(dt)
                    physics:apply_force(0, 12.0 * dt, 0)
                end

                function on_collision(other)
                    physics:apply_impulse(0, 10.0, 0)
                    engine:play_sound("bounce")
                end
            """.trimIndent()
        )
    )

    val templates = defaultScripts
}

