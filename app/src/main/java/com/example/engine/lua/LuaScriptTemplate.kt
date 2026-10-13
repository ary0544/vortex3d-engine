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
            id = "script_odyssey",
            name = "Assassin Odyssey TPP Controller",
            description = "Kontrol analog 360°, rotasi arah jalan, lompatan fisika, dan kamera smooth follow ala Assassin's Creed.",
            content = """
                -- Assassin's Creed Odyssey Style TPP Controller
                -- Kontrol analog halus dengan kamera TPP mengikuti karakter
                function init()
                    engine:log("Assassin Odyssey Controller aktif!")
                    camera:set_distance(6.5)
                end

                function update(dt)
                    -- 1. Baca nilai joystick analog (-1.0 s/d 1.0)
                    local h = input_h
                    local v = input_v

                    -- 2. Terapkan gaya jika analog digerakkan
                    if h ~= 0 or v ~= 0 then
                        -- Mendorong karakter ke arah analog
                        physics:apply_force(h * 20.0, 0, v * 20.0)

                        -- Menghadapkan karakter ke arah pergerakan
                        entity:rotate(0, h * 120.0 * dt, 0)
                    end

                    -- 3. Lompatan dengan tombol Jump (hanya saat menyentuh pijakan)
                    if input_jump and is_grounded then
                        physics:apply_impulse(0, 8.5, 0)
                        engine:play_sound("jump")
                    end

                    -- 4. Kamera Assassin Odyssey Follow:
                    -- Kamera otomatis mengunci fokus ke karakter dengan offset ketinggian bahu (+1.3)
                    camera:follow_self(1.3)
                end

                function on_collision(other)
                    engine:log("Karakter menyentuh: " .. other)
                    engine:play_sound("impact")
                end
            """.trimIndent()
        ),
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

                    -- Lompat saat tombol jump ditekan (hanya saat menyentuh pijakan)
                    if input_jump and is_grounded then
                        physics:apply_impulse(0, 8.0, 0)
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
                    -- idle
                end

                function on_collision(other)
                    if is_grounded then
                        physics:apply_impulse(0, 7.5, 0)
                        engine:play_sound("bounce")
                    end
                end
            """.trimIndent()
        )
    )

    val templates = defaultScripts
}

