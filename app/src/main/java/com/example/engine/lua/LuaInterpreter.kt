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

    private data class BlockFrame(
        var branchTaken: Boolean,
        var isExecuting: Boolean
    )

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
            val blockStack = mutableListOf<BlockFrame>()

            for (rawLine in lines) {
                val line = rawLine.substringBefore("--").trim()
                if (line.isEmpty()) continue

                if (!inFunction) {
                    if (line.startsWith("function $functionName(") ||
                        line.startsWith("function $functionName (") ||
                        line.startsWith("function $functionName\t(")
                    ) {
                        inFunction = true
                        blockStack.clear()
                        blockStack.add(BlockFrame(branchTaken = true, isExecuting = true))
                    }
                    continue
                }

                // If inFunction is true:
                // 1. Check end keyword
                if (line == "end" || line.startsWith("end ") || line.endsWith(" end")) {
                    if (blockStack.size > 1) {
                        blockStack.removeAt(blockStack.lastIndex)
                    } else {
                        // End of current function definition
                        inFunction = false
                        break
                    }
                    continue
                }

                // 2. Check if statement: e.g. "if ... then"
                if (line.startsWith("if ")) {
                    val conditionPart = line.removePrefix("if ").removeSuffix(" then").trim()
                    val parentExecuting = blockStack.all { it.isExecuting }
                    val cond = if (parentExecuting) evalBoolean(conditionPart) else false
                    blockStack.add(BlockFrame(branchTaken = cond, isExecuting = cond))
                    continue
                }

                // 3. Check elseif statement: e.g. "elseif ... then"
                if (line.startsWith("elseif ")) {
                    val conditionPart = line.removePrefix("elseif ").removeSuffix(" then").trim()
                    if (blockStack.size > 1) {
                        val currentFrame = blockStack.last()
                        val parentExecuting = blockStack.dropLast(1).all { it.isExecuting }
                        if (parentExecuting && !currentFrame.branchTaken) {
                            val cond = evalBoolean(conditionPart)
                            currentFrame.branchTaken = cond
                            currentFrame.isExecuting = cond
                        } else {
                            currentFrame.isExecuting = false
                        }
                    }
                    continue
                }

                // 4. Check else statement: e.g. "else"
                if (line == "else" || line.startsWith("else ")) {
                    if (blockStack.size > 1) {
                        val currentFrame = blockStack.last()
                        val parentExecuting = blockStack.dropLast(1).all { it.isExecuting }
                        if (parentExecuting && !currentFrame.branchTaken) {
                            currentFrame.branchTaken = true
                            currentFrame.isExecuting = true
                        } else {
                            currentFrame.isExecuting = false
                        }
                    }
                    continue
                }

                // 5. Normal line execution only if all enclosing blocks are active
                if (blockStack.all { it.isExecuting }) {
                    activeEntity = executeLine(line, activeEntity, args)
                }
            }
        } catch (e: Exception) {
            callbacks.log("LuaError", "[${initialEntity.name}] in $functionName(): ${e.message}")
        }

        return activeEntity
    }

    private fun evalBoolean(condition: String): Boolean {
        val c = condition.trim()
        if (c.isEmpty()) return false
        if (c == "true") return true
        if (c == "false") return false

        // Handle logical OR
        if (c.contains(" or ")) {
            val parts = c.split(" or ")
            return parts.any { evalBoolean(it.trim()) }
        }

        // Handle logical AND
        if (c.contains(" and ")) {
            val parts = c.split(" and ")
            return parts.all { evalBoolean(it.trim()) }
        }

        // Handle logical NOT
        if (c.startsWith("not ")) {
            return !evalBoolean(c.removePrefix("not ").trim())
        }

        // Handle action triggers
        if (c.contains("input:is_action_pressed(\"Jump\")") || c.contains("input:is_action_pressed('Jump')")) {
            return (variables["input_jump"] as? Boolean) ?: false
        }
        if (c.contains("input:is_action_pressed(\"Fire\")") || c.contains("input:is_action_pressed('Fire')")) {
            return (variables["input_fire"] as? Boolean) ?: false
        }
        if (c == "input_jump") {
            return (variables["input_jump"] as? Boolean) ?: false
        }
        if (c == "input_fire") {
            return (variables["input_fire"] as? Boolean) ?: false
        }

        // Handle comparison: ~= or !=
        if (c.contains("~=")) {
            val parts = c.split("~=", limit = 2)
            return abs(evalFloat(parts[0]) - evalFloat(parts[1])) > 0.0001f
        }
        if (c.contains("!=")) {
            val parts = c.split("!=", limit = 2)
            return abs(evalFloat(parts[0]) - evalFloat(parts[1])) > 0.0001f
        }

        // Handle comparison: ==
        if (c.contains("==")) {
            val parts = c.split("==", limit = 2)
            return abs(evalFloat(parts[0]) - evalFloat(parts[1])) <= 0.0001f
        }

        // Handle comparison: >=
        if (c.contains(">=")) {
            val parts = c.split(">=", limit = 2)
            return evalFloat(parts[0]) >= evalFloat(parts[1])
        }

        // Handle comparison: <=
        if (c.contains("<=")) {
            val parts = c.split("<=", limit = 2)
            return evalFloat(parts[0]) <= evalFloat(parts[1])
        }

        // Handle comparison: >
        if (c.contains(">")) {
            val parts = c.split(">", limit = 2)
            return evalFloat(parts[0]) > evalFloat(parts[1])
        }

        // Handle comparison: <
        if (c.contains("<")) {
            val parts = c.split("<", limit = 2)
            return evalFloat(parts[0]) < evalFloat(parts[1])
        }

        // Variable lookup
        if (variables.containsKey(c)) {
            val v = variables[c]
            if (v is Boolean) return v
            if (v is Number) return v.toFloat() != 0f
            return v != null
        }

        // Non-zero number check
        val num = c.toFloatOrNull()
        if (num != null) return num != 0f

        return false
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
                        position = Vec3(
                            (curPos.x + dx).coerceIn(-500f, 500f),
                            (curPos.y + dy).coerceIn(-50f, 300f),
                            (curPos.z + dz).coerceIn(-500f, 500f)
                        )
                    )
                )
            }
        }
        // Handle entity:set_position(x, y, z)
        else if (clean.contains("entity:set_position(") || clean.contains("entity:set_pos(")) {
            val prefix = if (clean.contains("entity:set_position(")) "entity:set_position" else "entity:set_pos"
            val params = extractParams(clean, prefix)
            if (params.size >= 3) {
                val x = evalFloat(params[0]).coerceIn(-500f, 500f)
                val y = evalFloat(params[1]).coerceIn(-50f, 300f)
                val z = evalFloat(params[2]).coerceIn(-500f, 500f)
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
                    val clampedVel = Vec3(
                        newVel.x.coerceIn(-30f, 30f),
                        newVel.y.coerceIn(-30f, 30f),
                        newVel.z.coerceIn(-30f, 30f)
                    )
                    entity = entity.copy(rigidBody = rb.copy(velocity = clampedVel))
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
                    // Safety clamp to prevent rocket escape velocity to infinity
                    val clampedVel = Vec3(
                        newVel.x.coerceIn(-30f, 30f),
                        newVel.y.coerceIn(-30f, 22f),
                        newVel.z.coerceIn(-30f, 30f)
                    )
                    entity = entity.copy(rigidBody = rb.copy(velocity = clampedVel))
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
                val tx = evalFloat(params[0]).coerceIn(-500f, 500f)
                val ty = evalFloat(params[1]).coerceIn(-50f, 300f)
                val tz = evalFloat(params[2]).coerceIn(-500f, 500f)
                callbacks.setCameraTarget(Vec3(tx, ty, tz))
            }
        }
        // Handle camera:follow_self(offset_y) or camera:follow_entity()
        else if (clean.contains("camera:follow_self(") || clean.contains("camera:follow_entity(") || clean == "camera:follow_self()" || clean == "camera:follow_entity()") {
            val prefix = if (clean.contains("camera:follow_self")) "camera:follow_self" else "camera:follow_entity"
            val params = extractParams(clean, prefix)
            val offsetY = if (params.isNotEmpty()) evalFloat(params[0]) else 1.2f
            val p = entity.transform.position
            // Ensure target never blows up into astronomical numbers
            val safeTarget = Vec3(
                p.x.coerceIn(-300f, 300f),
                (p.y + offsetY).coerceIn(-50f, 150f),
                p.z.coerceIn(-300f, 300f)
            )
            callbacks.setCameraTarget(safeTarget)
        }
        // Handle camera:set_distance(dist)
        else if (clean.contains("camera:set_distance(")) {
            val params = extractParams(clean, "camera:set_distance")
            if (params.isNotEmpty()) {
                callbacks.setCameraDistance(evalFloat(params[0]).coerceIn(1.5f, 50f))
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
        if (e.isEmpty()) return 0f
        if (e == "dt") return (variables["dt"] as? Float) ?: 0.016f
        if (e.contains("input:get_axis(\"Horizontal\")") || e == "h" || e == "input_h") return (variables["input_h"] as? Float) ?: 0f
        if (e.contains("input:get_axis(\"Vertical\")") || e == "v" || e == "input_v") return (variables["input_v"] as? Float) ?: 0f

        // Variable lookup
        if (variables.containsKey(e)) {
            val v = variables[e]
            if (v is Number) return v.toFloat()
        }

        // Chained multiplication: e.g. "h * 120.0 * dt"
        if (e.contains("*")) {
            val parts = e.split("*").map { it.trim() }
            var product = 1.0f
            for (p in parts) {
                product *= evalFloat(p)
            }
            return product
        }

        // Addition
        if (e.contains("+")) {
            val parts = e.split("+").map { it.trim() }
            var sum = 0.0f
            for (p in parts) {
                sum += evalFloat(p)
            }
            return sum
        }

        // Subtraction (avoid splitting on leading negative sign)
        if (e.contains("-") && !e.startsWith("-")) {
            val parts = e.split("-").map { it.trim() }
            var diff = evalFloat(parts[0])
            for (i in 1 until parts.size) {
                diff -= evalFloat(parts[i])
            }
            return diff
        }

        // Division
        if (e.contains("/")) {
            val parts = e.split("/").map { it.trim() }
            var result = evalFloat(parts[0])
            for (i in 1 until parts.size) {
                val denom = evalFloat(parts[i])
                result /= if (abs(denom) > 0.0001f) denom else 1.0f
            }
            return result
        }

        return e.toFloatOrNull() ?: 0f
    }

    private fun evalExpr(expr: String): Any {
        val e = expr.trim()
        if (e == "true") return true
        if (e == "false") return false
        if (e.startsWith("\"") && e.endsWith("\"")) return e.substring(1, e.length - 1)
        if (e.contains("input:is_action_pressed(\"Jump\")") || e == "input_jump") return (variables["input_jump"] as? Boolean) ?: false
        if (e.contains("input:is_action_pressed(\"Fire\")") || e == "input_fire") return (variables["input_fire"] as? Boolean) ?: false
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
