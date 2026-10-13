package com.example.engine.lua

import com.example.engine.math.Vec3
import com.example.engine.model.Entity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LuaInterpreter(
    val entityId: String,
    private val callbacks: EngineCallbacks
) {
    private val variables = mutableMapOf<String, Any>()
    private var isInitialized = false

    init {
        setupStandardLibrary()
    }

    private fun setupStandardLibrary() {
        variables["PI"] = Math.PI.toFloat()
    }

    fun executeInit(script: String, currentEntity: Entity): Entity {
        isInitialized = true
        return executeFunction(script, "init", listOf(), currentEntity)
    }

    fun executeUpdate(script: String, dt: Float, currentEntity: Entity, input: InputState): Entity {
        if (!isInitialized) {
            executeInit(script, currentEntity)
        }
        variables["dt"] = dt
        variables["input_h"] = input.horizontal
        variables["input_v"] = input.vertical
        variables["input_jump"] = input.actionJump
        variables["input_fire"] = input.actionFire
        variables["pos_x"] = currentEntity.transform.position.x
        variables["pos_y"] = currentEntity.transform.position.y
        variables["pos_z"] = currentEntity.transform.position.z
        variables["self_x"] = currentEntity.transform.position.x
        variables["self_y"] = currentEntity.transform.position.y
        variables["self_z"] = currentEntity.transform.position.z
        return executeFunction(script, "update", listOf(dt), currentEntity)
    }

    fun executeOnCollision(script: String, otherEntityName: String, currentEntity: Entity): Entity {
        return executeFunction(script, "on_collision", listOf(otherEntityName), currentEntity)
    }

    private fun executeFunction(
        script: String,
        functionName: String,
        args: List<Any>,
        initialEntity: Entity
    ): Entity {
        var activeEntity = initialEntity
        if (script.isBlank()) return activeEntity

        try {
            val lines = script.lines()
            var inFunction = false
            var blockDepth = 0

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.startsWith("--") || line.isEmpty()) continue

                if (line.startsWith("function $functionName(") || line.startsWith("function $functionName (")) {
                    inFunction = true
                    blockDepth = 1
                    continue
                }

                if (inFunction) {
                    if (line.startsWith("function ") || line.startsWith("if ") || line.startsWith("for ") || line.startsWith("while ")) {
                        blockDepth++
                    }
                    if (line == "end" || line.startsWith("end ") || line.endsWith(" end")) {
                        blockDepth--
                        if (blockDepth <= 0) {
                            inFunction = false
                            break
                        }
                    }

                    activeEntity = executeLine(line, activeEntity, args)
                }
            }
        } catch (e: Exception) {
            callbacks.log("LuaError", "[${initialEntity.name}] in $functionName(): ${e.message}")
        }

        return activeEntity
    }

    private fun executeLine(line: String, currentEntity: Entity, args: List<Any>): Entity {
        var entity = currentEntity
        val clean = line.substringBefore("--").trim()
        if (clean.isEmpty() || clean == "end" || clean.startsWith("end")) return entity

        // Handle entity:rotate(rx, ry, rz)
        if (clean.contains("entity:rotate(")) {
            val params = extractParams(clean, "entity:rotate")
            if (params.size >= 3) {
                val rx = evalFloat(params[0])
                val ry = evalFloat(params[1])
                val rz = evalFloat(params[2])
                val curRot = entity.transform.rotation
                entity = entity.copy(
                    transform = entity.transform.copy(
                        rotation = Vec3(curRot.x + rx, curRot.y + ry, curRot.z + rz)
                    )
                )
            }
        }
        // Handle entity:translate(dx, dy, dz)
        else if (clean.contains("entity:translate(")) {
            val params = extractParams(clean, "entity:translate")
            if (params.size >= 3) {
                val dx = evalFloat(params[0])
                val dy = evalFloat(params[1])
                val dz = evalFloat(params[2])
                val curPos = entity.transform.position
                entity = entity.copy(
                    transform = entity.transform.copy(
                        position = Vec3(curPos.x + dx, curPos.y + dy, curPos.z + dz)
                    )
                )
            }
        }
        // Handle entity:set_position(x, y, z)
        else if (clean.contains("entity:set_position(") || clean.contains("entity:set_pos(")) {
            val prefix = if (clean.contains("entity:set_position(")) "entity:set_position" else "entity:set_pos"
            val params = extractParams(clean, prefix)
            if (params.size >= 3) {
                val x = evalFloat(params[0])
                val y = evalFloat(params[1])
                val z = evalFloat(params[2])
                entity = entity.copy(transform = entity.transform.copy(position = Vec3(x, y, z)))
            }
        }
        // Handle physics:apply_force(fx, fy, fz)
        else if (clean.contains("physics:apply_force(")) {
            val params = extractParams(clean, "physics:apply_force")
            if (params.size >= 3) {
                val fx = evalFloat(params[0])
                val fy = evalFloat(params[1])
                val fz = evalFloat(params[2])
                val rb = entity.rigidBody
                if (rb != null) {
                    val dt = (variables["dt"] as? Float) ?: 0.016f
                    val newVel = rb.velocity + Vec3(fx, fy, fz) * (dt / maxOf(0.1f, rb.mass))
                    entity = entity.copy(rigidBody = rb.copy(velocity = newVel))
                }
            }
        }
        // Handle physics:apply_impulse(ix, iy, iz)
        else if (clean.contains("physics:apply_impulse(")) {
            val params = extractParams(clean, "physics:apply_impulse")
            if (params.size >= 3) {
                val ix = evalFloat(params[0])
                val iy = evalFloat(params[1])
                val iz = evalFloat(params[2])
                val rb = entity.rigidBody
                if (rb != null) {
                    val newVel = rb.velocity + Vec3(ix, iy, iz) * (1f / maxOf(0.1f, rb.mass))
                    entity = entity.copy(rigidBody = rb.copy(velocity = newVel))
                }
            }
        }
        // Handle engine:play_sound(sound)
        else if (clean.contains("engine:play_sound(")) {
            val params = extractParams(clean, "engine:play_sound")
            if (params.isNotEmpty()) {
                val sound = params[0].replace("\"", "").replace("'", "").trim()
                callbacks.playSound(sound)
            }
        }
        // Handle camera:set_target(x, y, z)
        else if (clean.contains("camera:set_target(")) {
            val params = extractParams(clean, "camera:set_target")
            if (params.size >= 3) {
                val tx = evalFloat(params[0])
                val ty = evalFloat(params[1])
                val tz = evalFloat(params[2])
                callbacks.setCameraTarget(Vec3(tx, ty, tz))
            }
        }
        // Handle camera:follow_self(offset_y) or camera:follow_entity()
        else if (clean.contains("camera:follow_self(") || clean.contains("camera:follow_entity(") || clean == "camera:follow_self()" || clean == "camera:follow_entity()") {
            val prefix = if (clean.contains("camera:follow_self")) "camera:follow_self" else "camera:follow_entity"
            val params = extractParams(clean, prefix)
            val offsetY = if (params.isNotEmpty()) evalFloat(params[0]) else 1.2f
            val p = entity.transform.position
            callbacks.setCameraTarget(Vec3(p.x, p.y + offsetY, p.z))
        }
        // Handle camera:set_distance(dist)
        else if (clean.contains("camera:set_distance(")) {
            val params = extractParams(clean, "camera:set_distance")
            if (params.isNotEmpty()) {
                callbacks.setCameraDistance(evalFloat(params[0]))
            }
        }
        // Handle camera:set_orbit(yaw, pitch)
        else if (clean.contains("camera:set_orbit(")) {
            val params = extractParams(clean, "camera:set_orbit")
            if (params.size >= 2) {
                callbacks.setCameraOrbit(evalFloat(params[0]), evalFloat(params[1]))
            }
        }
        // Handle engine:log(msg)
        else if (clean.contains("engine:log(") || clean.contains("print(")) {
            val prefix = if (clean.contains("engine:log(")) "engine:log" else "print"
            val params = extractParams(clean, prefix)
            if (params.isNotEmpty()) {
                val msg = evalString(params[0], args)
                callbacks.log("Lua", msg)
            }
        }
        // Handle variable assignment: local a = 10 or x = x + 1
        else if (clean.contains("=")) {
            val assignLine = if (clean.startsWith("local ")) clean.removePrefix("local ").trim() else clean
            val parts = assignLine.split("=", limit = 2)
            if (parts.size == 2) {
                val varName = parts[0].trim()
                val expr = parts[1].trim()
                variables[varName] = evalExpr(expr)
            }
        }

        return entity
    }

    private fun extractParams(line: String, funcPrefix: String): List<String> {
        val startIdx = line.indexOf("$funcPrefix(")
        if (startIdx < 0) return emptyList()
        val openParen = startIdx + funcPrefix.length
        val closeParen = line.lastIndexOf(')')
        if (closeParen <= openParen) return emptyList()
        val inner = line.substring(openParen + 1, closeParen)
        return inner.split(',').map { it.trim() }
    }

    private fun evalFloat(expr: String): Float {
        val e = expr.trim()
        if (e.startsWith("dt") || e == "dt") return (variables["dt"] as? Float) ?: 0.016f
        if (e.startsWith("input:get_axis(\"Horizontal\")") || e == "h") return (variables["input_h"] as? Float) ?: 0f
        if (e.startsWith("input:get_axis(\"Vertical\")") || e == "v") return (variables["input_v"] as? Float) ?: 0f

        // Variable lookup
        if (variables.containsKey(e)) {
            val v = variables[e]
            if (v is Number) return v.toFloat()
        }

        // Handle simple multiplications like "45.0 * dt" or "h * 15.0"
        if (e.contains("*")) {
            val parts = e.split("*").map { it.trim() }
            if (parts.size == 2) {
                return evalFloat(parts[0]) * evalFloat(parts[1])
            }
        }
        // Handle additions
        if (e.contains("+")) {
            val parts = e.split("+").map { it.trim() }
            if (parts.size == 2) {
                return evalFloat(parts[0]) + evalFloat(parts[1])
            }
        }

        return e.toFloatOrNull() ?: 0f
    }

    private fun evalExpr(expr: String): Any {
        val e = expr.trim()
        if (e == "true") return true
        if (e == "false") return false
        if (e.startsWith("\"") && e.endsWith("\"")) return e.substring(1, e.length - 1)
        if (e.contains("input:is_action_pressed(\"Jump\")")) return (variables["input_jump"] as? Boolean) ?: false
        if (e.contains("input:is_action_pressed(\"Fire\")")) return (variables["input_fire"] as? Boolean) ?: false
        return evalFloat(e)
    }

    private fun evalString(expr: String, args: List<Any>): String {
        val e = expr.trim()
        if (e.contains("..")) {
            val parts = e.split("..").map { it.trim() }
            return parts.joinToString("") { part ->
                if (part.startsWith("\"") && part.endsWith("\"")) {
                    part.substring(1, part.length - 1)
                } else if (part == "other" && args.isNotEmpty()) {
                    args[0].toString()
                } else {
                    variables[part]?.toString() ?: part
                }
            }
        }
        if (e.startsWith("\"") && e.endsWith("\"")) {
            return e.substring(1, e.length - 1)
        }
        return variables[e]?.toString() ?: e
    }
}
