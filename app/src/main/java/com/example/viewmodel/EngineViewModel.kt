package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.ai.AiAssistantResponse
import com.example.engine.ai.GeminiEngineAssistant
import com.example.engine.assets.AssetManager
import com.example.engine.assets.AudioSynthesizer
import com.example.engine.lua.EngineCallbacks
import com.example.engine.lua.InputState
import com.example.engine.lua.LuaInterpreter
import com.example.engine.lua.LuaScriptAsset
import com.example.engine.math.MathUtils
import com.example.engine.math.Vec3
import com.example.engine.model.ColliderShape
import com.example.engine.model.Entity
import com.example.engine.model.Material
import com.example.engine.model.MeshRendererComponent
import com.example.engine.model.RigidBodyComponent
import com.example.engine.model.Scene
import com.example.engine.model.ShaderProgram
import com.example.engine.model.TransformComponent
import com.example.engine.physics.PhysicsEngine
import com.example.engine.renderer.Camera3D
import com.example.engine.renderer.RenderStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.engine.model.GlbParser
import java.io.File
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import com.example.engine.model.LuaScriptComponent

enum class WorkspaceTab {
    SCENE_VIEW,
    HIERARCHY,
    INSPECTOR,
    ASSETS,
    FILE_MANAGER,
    LUA_EDITOR,
    SHADER_EDITOR,
    AI_COPILOT,
    DOCS_TUTORIALS
}

enum class LeftDockTab {
    SCENE_TREE,
    FILE_SYSTEM
}

enum class RightDockTab {
    INSPECTOR,
    LUA_SCRIPT,
    SHADERS,
    AI_COPILOT,
    DOCS
}

enum class BottomDockTab {
    CONSOLE,
    STATS,
    AI_COPILOT,
    FILES
}

enum class ProjectOrientation(val title: String, val badge: String) {
    LANDSCAPE("Landscape (Horizontal)", "Landscape"),
    PORTRAIT("Portrait (Vertical)", "Portrait"),
    AUTO_SENSOR("Auto-Sensor (Gyro)", "Sensor")
}

data class ProjectItem(
    val id: String,
    val name: String,
    val templateName: String,
    val entityCount: Int,
    val lastModified: String,
    val scene: Scene,
    val orientation: ProjectOrientation = ProjectOrientation.LANDSCAPE
)

object LogIdCounter {
    private val counter = java.util.concurrent.atomic.AtomicLong(1)
    fun nextId(): Long = counter.getAndIncrement()
}

data class ConsoleLogItem(
    val id: Long = LogIdCounter.nextId(),
    val tag: String,
    val message: String,
    val timestamp: String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
)

data class CameraSettings(
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val orbitSensitivity: Float = 1.0f,
    val panSensitivity: Float = 1.0f,
    val zoomSensitivity: Float = 1.0f
)

class EngineViewModel : ViewModel(), EngineCallbacks {

    val camera = Camera3D()
    val physicsEngine = PhysicsEngine()
    val assetManager = AssetManager()

    private val _scene = MutableStateFlow(Scene.createDefaultPhysicsPlayground())
    val scene: StateFlow<Scene> = _scene.asStateFlow()

    private val _sceneName = MutableStateFlow(_scene.value.name)
    val sceneName: StateFlow<String> = _sceneName.asStateFlow()

    private var initialSceneBackup = _scene.value

    private val _selectedEntityId = MutableStateFlow<String?>("ent_hero_orb")
    val selectedEntityId: StateFlow<String?> = _selectedEntityId.asStateFlow()

    private val _workspaceTab = MutableStateFlow(WorkspaceTab.SCENE_VIEW)
    val workspaceTab: StateFlow<WorkspaceTab> = _workspaceTab.asStateFlow()

    // Godot 4 Mobile Collapsible Dock states
    private val _isLeftDockOpen = MutableStateFlow(true)
    val isLeftDockOpen: StateFlow<Boolean> = _isLeftDockOpen.asStateFlow()

    private val _isRightDockOpen = MutableStateFlow(true)
    val isRightDockOpen: StateFlow<Boolean> = _isRightDockOpen.asStateFlow()

    private val _isBottomDockOpen = MutableStateFlow(false)
    val isBottomDockOpen: StateFlow<Boolean> = _isBottomDockOpen.asStateFlow()

    private val _leftDockTab = MutableStateFlow(LeftDockTab.SCENE_TREE)
    val leftDockTab: StateFlow<LeftDockTab> = _leftDockTab.asStateFlow()

    private val _rightDockTab = MutableStateFlow(RightDockTab.INSPECTOR)
    val rightDockTab: StateFlow<RightDockTab> = _rightDockTab.asStateFlow()

    private val _bottomDockTab = MutableStateFlow(BottomDockTab.CONSOLE)
    val bottomDockTab: StateFlow<BottomDockTab> = _bottomDockTab.asStateFlow()

    // Project Manager / Lobby state - defaults to true so Lobby is the main launch screen
    private val _isInLobby = MutableStateFlow(true)
    val isInLobby: StateFlow<Boolean> = _isInLobby.asStateFlow()

    private val _activeProjectId = MutableStateFlow("proj_playground")
    val activeProjectId: StateFlow<String> = _activeProjectId.asStateFlow()

    private val _projects = MutableStateFlow<List<ProjectItem>>(listOf(
        ProjectItem(
            id = "proj_playground",
            name = "Physics Playground",
            templateName = "Physics Sandbox",
            entityCount = 6,
            lastModified = "Just now",
            scene = Scene.createDefaultPhysicsPlayground(),
            orientation = ProjectOrientation.LANDSCAPE
        ),
        ProjectItem(
            id = "proj_fps_arena",
            name = "Cyber FPS Arena",
            templateName = "FPS Action Arena",
            entityCount = 6,
            lastModified = "Today",
            scene = Scene.createFpsArena(),
            orientation = ProjectOrientation.LANDSCAPE
        ),
        ProjectItem(
            id = "proj_cyberpunk",
            name = "Neon Cyberpunk Gate",
            templateName = "Cyberpunk Neo World",
            entityCount = 5,
            lastModified = "Yesterday",
            scene = Scene.createCyberpunkGlow(),
            orientation = ProjectOrientation.PORTRAIT
        ),
        ProjectItem(
            id = "proj_blank",
            name = "New Blank World",
            templateName = "Blank 3D Scene",
            entityCount = 2,
            lastModified = "Yesterday",
            scene = Scene.createBlank3DScene(),
            orientation = ProjectOrientation.AUTO_SENSOR
        )
    ))
    val projects: StateFlow<List<ProjectItem>> = _projects.asStateFlow()

    private val _isPlayMode = MutableStateFlow(false)
    val isPlayMode: StateFlow<Boolean> = _isPlayMode.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<ConsoleLogItem>>(listOf(
        ConsoleLogItem(tag = "Engine", message = "Vortex3D Engine v1.0 Initialized."),
        ConsoleLogItem(tag = "Physics", message = "Rigid Body solver ready (Gravity: -9.81 m/s²)."),
        ConsoleLogItem(tag = "Lua", message = "Lua Scripting Virtual Machine loaded.")
    ))
    val consoleLogs: StateFlow<List<ConsoleLogItem>> = _consoleLogs.asStateFlow()

    private val _editingScript = MutableStateFlow<LuaScriptAsset?>(null)
    val editingScript: StateFlow<LuaScriptAsset?> = _editingScript.asStateFlow()

    private val _scriptsState = MutableStateFlow<List<LuaScriptAsset>>(emptyList())
    val scriptsState: StateFlow<List<LuaScriptAsset>> = _scriptsState.asStateFlow()

    fun refreshScriptsState() {
        _scriptsState.value = assetManager.scripts.values.toList()
    }

    fun syncSceneScriptsToAssetManager(sceneObj: Scene) {
        var changed = false
        for (entity in sceneObj.entities) {
            val comp = entity.luaScript ?: continue
            if (comp.scriptId.isNotBlank() && !assetManager.scripts.containsKey(comp.scriptId)) {
                assetManager.scripts[comp.scriptId] = LuaScriptAsset(
                    id = comp.scriptId,
                    name = comp.scriptName,
                    description = "Skrip untuk ${entity.name}",
                    content = comp.scriptContent
                )
                changed = true
            }
        }
        if (changed || _scriptsState.value.isEmpty()) {
            refreshScriptsState()
        }
    }

    fun syncEditingScriptForSelectedEntity(entityId: String?) {
        if (entityId == null) {
            _editingScript.value = null
            return
        }
        val entity = _scene.value.findEntity(entityId)
        val comp = entity?.luaScript
        if (comp != null && comp.scriptId.isNotBlank()) {
            val asset = assetManager.scripts[comp.scriptId] ?: LuaScriptAsset(
                id = comp.scriptId,
                name = comp.scriptName,
                description = "Skrip untuk ${entity.name}",
                content = comp.scriptContent
            ).also {
                assetManager.scripts[comp.scriptId] = it
                refreshScriptsState()
            }
            _editingScript.value = asset
        } else {
            _editingScript.value = null
        }
    }

    private val _editingShader = MutableStateFlow<ShaderProgram?>(null)
    val editingShader: StateFlow<ShaderProgram?> = _editingShader.asStateFlow()

    private val _renderStats = MutableStateFlow(RenderStats())
    val renderStats: StateFlow<RenderStats> = _renderStats.asStateFlow()

    private val _cameraRevision = MutableStateFlow(0L)
    val cameraRevision: StateFlow<Long> = _cameraRevision.asStateFlow()

    private val _cameraSettings = MutableStateFlow(CameraSettings())
    val cameraSettings: StateFlow<CameraSettings> = _cameraSettings.asStateFlow()

    private val _isWireframeMode = MutableStateFlow(false)
    val isWireframeMode: StateFlow<Boolean> = _isWireframeMode.asStateFlow()

    // AI Copilot state
    private val _aiResponse = MutableStateFlow<AiAssistantResponse?>(null)
    val aiResponse: StateFlow<AiAssistantResponse?> = _aiResponse.asStateFlow()

    private val _latestScriptError = MutableStateFlow<String?>(null)
    val latestScriptError: StateFlow<String?> = _latestScriptError.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    val inputState = InputState()
    private val luaInterpreters = mutableMapOf<String, LuaInterpreter>()
    private var simulationJob: Job? = null
    var elapsedTimeSeconds = 0f

    init {
        syncSceneScriptsToAssetManager(_scene.value)
        syncEditingScriptForSelectedEntity(_selectedEntityId.value)
        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.Default) {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val dt = (now - lastTime) / 1000f
                lastTime = now

                if (_isSimulating.value || _isPlayMode.value) {
                    elapsedTimeSeconds += dt
                    stepSimulation(dt)
                }
                delay(16) // ~60 FPS
            }
        }
    }

    private fun stepSimulation(dt: Float) {
        val currentEntities = _scene.value.entities
        // 1. Run Lua Scripts on Entities
        val scriptUpdated = currentEntities.map { entity ->
            val scriptComp = entity.luaScript
            if (scriptComp != null && scriptComp.isEnabled && scriptComp.scriptContent.isNotBlank()) {
                val interp = luaInterpreters.getOrPut(entity.id) {
                    LuaInterpreter(entity.id, this)
                }
                interp.executeUpdate(scriptComp.scriptContent, dt, entity, inputState)
            } else {
                entity
            }
        }

        // 2. Step Physics simulation
        val physicsUpdated = physicsEngine.step(scriptUpdated, dt)

        // 3. Handle collision events for Lua scripts
        val events = physicsEngine.pollCollisionEvents()
        var collisionUpdated = physicsUpdated
        for (event in events) {
            val scriptA = event.entityA.luaScript
            if (scriptA != null && scriptA.isEnabled) {
                val interp = luaInterpreters.getOrPut(event.entityA.id) {
                    LuaInterpreter(event.entityA.id, this)
                }
                val res = interp.executeOnCollision(scriptA.scriptContent, event.entityB.name, event.entityA)
                collisionUpdated = collisionUpdated.map { if (it.id == res.id) res else it }
            }
        }

        _scene.value = _scene.value.copy(entities = collisionUpdated)

        // Reset pulsa input satu frame (Jump / Fire) agar tidak tertekan selamanya
        if (inputState.actionJump) {
            inputState.actionJump = false
        }
        if (inputState.actionFire) {
            inputState.actionFire = false
        }
    }

    fun toggleSimulation() {
        if (!_isSimulating.value && !_isPlayMode.value) {
            initialSceneBackup = _scene.value
        }
        _isSimulating.value = !_isSimulating.value
        log("Engine", if (_isSimulating.value) "Simulation Started" else "Simulation Paused")
    }

    fun resetSimulation() {
        _isSimulating.value = false
        inputState.actionJump = false
        inputState.actionFire = false
        _scene.value = initialSceneBackup
        luaInterpreters.clear()
        camera.reset()
        log("Engine", "Simulation Reset to initial state")
    }

    fun setPlayMode(enabled: Boolean) {
        if (enabled) {
            initialSceneBackup = _scene.value
            _isPlayMode.value = true
            _isSimulating.value = true
            log("Engine", "Entered Game Play Mode")
        } else {
            _isPlayMode.value = false
            _isSimulating.value = false
            inputState.actionJump = false
            inputState.actionFire = false
            _scene.value = initialSceneBackup
            luaInterpreters.clear()
            log("Engine", "Exited Game Play Mode")
        }
    }

    fun selectEntity(id: String?) {
        _selectedEntityId.value = id
        syncEditingScriptForSelectedEntity(id)
    }

    fun setWorkspaceTab(tab: WorkspaceTab) {
        _workspaceTab.value = tab
    }

    fun toggleWireframe() {
        _isWireframeMode.value = !_isWireframeMode.value
    }

    private var lastStatsUpdateTime = 0L

    fun updateRenderStats(stats: RenderStats) {
        val now = System.currentTimeMillis()
        if (now - lastStatsUpdateTime >= 350L) {
            lastStatsUpdateTime = now
            _renderStats.value = stats
        }
    }

    fun orbitCamera(deltaYaw: Float, deltaPitch: Float) {
        camera.orbit(deltaYaw, deltaPitch)
        _cameraRevision.value++
    }

    fun zoomCamera(deltaDist: Float) {
        camera.zoom(deltaDist)
        _cameraRevision.value++
    }

    fun panCamera(deltaX: Float, deltaY: Float) {
        camera.pan(deltaX, deltaY)
        _cameraRevision.value++
    }

    fun setInvertX(invert: Boolean) {
        _cameraSettings.value = _cameraSettings.value.copy(invertX = invert)
    }

    fun setInvertY(invert: Boolean) {
        _cameraSettings.value = _cameraSettings.value.copy(invertY = invert)
    }

    fun setOrbitSensitivity(sensitivity: Float) {
        _cameraSettings.value = _cameraSettings.value.copy(orbitSensitivity = sensitivity.coerceIn(0.2f, 3.0f))
    }

    fun setPanSensitivity(sensitivity: Float) {
        _cameraSettings.value = _cameraSettings.value.copy(panSensitivity = sensitivity.coerceIn(0.2f, 3.0f))
    }

    fun setZoomSensitivity(sensitivity: Float) {
        _cameraSettings.value = _cameraSettings.value.copy(zoomSensitivity = sensitivity.coerceIn(0.2f, 3.0f))
    }

    fun resetCamera() {
        camera.reset()
        _cameraRevision.value++
    }

    fun setCameraFrontView() {
        camera.setFrontView()
        _cameraRevision.value++
    }

    fun setCameraTopView() {
        camera.setTopView()
        _cameraRevision.value++
    }

    fun setCameraSideView() {
        camera.setSideView()
        _cameraRevision.value++
    }

    fun setCameraIsometricView() {
        camera.setIsometricView()
        _cameraRevision.value++
    }

    // Entity manipulations
    fun updateSelectedEntity(transform: (Entity) -> Entity) {
        val id = _selectedEntityId.value ?: return
        _scene.value = _scene.value.copy(
            entities = _scene.value.entities.map {
                if (it.id == id) transform(it) else it
            }
        )
    }

    fun addPrimitive(shape: String) {
        val newPos = Vec3(0f, 3.5f, 0f)
        val entity = when (shape.lowercase()) {
            "cube" -> Entity(
                id = "ent_cube_${LogIdCounter.nextId()}",
                name = "Cube",
                transform = TransformComponent(position = newPos),
                meshRenderer = MeshRendererComponent("mesh_cube", "mat_ruby"),
                rigidBody = RigidBodyComponent(mass = 1.0f, colliderShape = ColliderShape.BOX)
            )
            "sphere" -> Entity(
                id = "ent_sphere_${LogIdCounter.nextId()}",
                name = "Sphere",
                transform = TransformComponent(position = newPos),
                meshRenderer = MeshRendererComponent("mesh_sphere", "mat_gold"),
                rigidBody = RigidBodyComponent(mass = 1.5f, restitution = 0.8f, colliderShape = ColliderShape.SPHERE)
            )
            "cylinder" -> Entity(
                id = "ent_cyl_${LogIdCounter.nextId()}",
                name = "Cylinder",
                transform = TransformComponent(position = newPos),
                meshRenderer = MeshRendererComponent("mesh_cylinder", "mat_emerald"),
                rigidBody = RigidBodyComponent(mass = 1.2f, colliderShape = ColliderShape.BOX)
            )
            "torus" -> Entity(
                id = "ent_torus_${LogIdCounter.nextId()}",
                name = "Torus",
                transform = TransformComponent(position = newPos),
                meshRenderer = MeshRendererComponent("mesh_torus", "mat_hologram")
            )
            else -> Entity(
                id = "ent_custom_${LogIdCounter.nextId()}",
                name = "Object",
                transform = TransformComponent(position = newPos),
                meshRenderer = MeshRendererComponent("mesh_cube", "mat_default")
            )
        }

        _scene.value = _scene.value.copy(entities = _scene.value.entities + entity)
        _selectedEntityId.value = entity.id
        log("Scene", "Added entity: ${entity.name}")
    }

    fun deleteSelectedEntity() {
        val id = _selectedEntityId.value ?: return
        _scene.value = _scene.value.copy(entities = _scene.value.entities.filterNot { it.id == id })
        _selectedEntityId.value = null
        log("Scene", "Deleted entity")
    }

    fun duplicateSelectedEntity() {
        val id = _selectedEntityId.value ?: return
        val original = _scene.value.findEntity(id) ?: return
        val copy = original.copy(
            id = "ent_dup_${LogIdCounter.nextId()}",
            name = "${original.name} (Copy)",
            transform = original.transform.copy(position = original.transform.position + Vec3(1f, 0f, 0f))
        )
        _scene.value = _scene.value.copy(entities = _scene.value.entities + copy)
        _selectedEntityId.value = copy.id
        log("Scene", "Duplicated entity: ${copy.name}")
    }

    fun editScript(script: LuaScriptAsset) {
        _editingScript.value = script
        _workspaceTab.value = WorkspaceTab.LUA_EDITOR
        _rightDockTab.value = RightDockTab.LUA_SCRIPT
        _isRightDockOpen.value = true
    }

    fun saveScript(id: String, content: String) {
        val existing = assetManager.scripts[id] ?: LuaScriptAsset(
            id = id,
            name = _editingScript.value?.name ?: "script",
            description = "Custom script",
            content = content
        )
        val updated = existing.copy(content = content)
        assetManager.scripts[id] = updated
        _editingScript.value = updated
        refreshScriptsState()

        // Update all entities in scene using this script
        _scene.value = _scene.value.copy(
            entities = _scene.value.entities.map { entity ->
                if (entity.luaScript?.scriptId == id) {
                    entity.copy(luaScript = entity.luaScript.copy(scriptContent = content))
                } else entity
            }
        )
        log("Lua", "Saved script: ${updated.name}")
    }

    fun attachScriptToEntity(entityId: String, scriptId: String) {
        val script = assetManager.scripts[scriptId] ?: return
        val entity = _scene.value.findEntity(entityId) ?: return
        val updatedComp = LuaScriptComponent(
            scriptId = script.id,
            scriptName = script.name,
            scriptContent = script.content,
            isEnabled = true
        )
        _scene.value = _scene.value.copy(
            entities = _scene.value.entities.map {
                if (it.id == entityId) it.copy(luaScript = updatedComp) else it
            }
        )
        if (_selectedEntityId.value == entityId) {
            _editingScript.value = script
        }
        log("Lua", "✓ Script '${script.name}' terpasang ke '${entity.name}'")
    }

    fun attachScriptToSelectedEntity(scriptId: String) {
        val sel = _selectedEntityId.value
        if (sel != null) {
            attachScriptToEntity(sel, scriptId)
        } else {
            log("Lua", "Pilih entitas terlebih dahulu untuk memasang script")
        }
    }

    fun detachScriptFromSelectedEntity() {
        val sel = _selectedEntityId.value ?: return
        val entity = _scene.value.findEntity(sel) ?: return
        _scene.value = _scene.value.copy(
            entities = _scene.value.entities.map {
                if (it.id == sel) it.copy(luaScript = null) else it
            }
        )
        _editingScript.value = null
        log("Lua", "Script dilepas dari entitas '${entity.name}'")
    }

    fun createAndAttachScript(name: String, templateType: String = "custom") {
        val sel = _selectedEntityId.value ?: return
        val scriptId = "script_${LogIdCounter.nextId()}"
        val content = when (templateType) {
            "odyssey" -> com.example.engine.lua.LuaScriptLibrary.defaultScripts.find { it.id == "script_odyssey" }?.content
                ?: "-- Controller\nfunction update(dt)\nend\n"
            "hero" -> com.example.engine.lua.LuaScriptLibrary.defaultScripts.find { it.id == "script_hero" }?.content
                ?: "-- Controller\nfunction update(dt)\nend\n"
            "orbit" -> com.example.engine.lua.LuaScriptLibrary.defaultScripts.find { it.id == "script_orbit" }?.content
                ?: "-- Orbit\nfunction update(dt)\n    entity:rotate(0, 45.0 * dt, 0)\nend\n"
            "terrain" -> com.example.engine.lua.LuaScriptLibrary.defaultScripts.find { it.id == "script_terrain" }?.content
                ?: "-- Terrain\nfunction on_collision(other)\nend\n"
            else -> "-- Script: $name\nfunction init()\n    engine:log(\"$name diinisialisasi\")\nend\n\nfunction update(dt)\n    -- Logika frame\nend\n\nfunction on_collision(other)\n    engine:log(\"Tabrakan dengan: \" .. other)\nend\n"
        }
        val asset = LuaScriptAsset(id = scriptId, name = name, description = "Dibuat untuk entitas", content = content)
        assetManager.scripts[scriptId] = asset
        refreshScriptsState()
        attachScriptToSelectedEntity(scriptId)
        editScript(asset)
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Teks") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "$label disalin ke clipboard", Toast.LENGTH_SHORT).show()
            log("Clipboard", "✓ $label disalin ke clipboard (${text.take(35)}...)")
        } catch (e: Exception) {
            log("Clipboard", "Gagal menyalin: ${e.message}")
        }
    }

    fun getFormattedLogs(): String {
        return _consoleLogs.value.joinToString("\n") { "[${it.timestamp}] [${it.tag}] ${it.message}" }
    }

    fun clearConsoleLogs() {
        _consoleLogs.value = emptyList()
        log("Console", "Console log dibersihkan.")
    }

    fun editShader(shader: ShaderProgram) {
        _editingShader.value = shader
        _workspaceTab.value = WorkspaceTab.SHADER_EDITOR
        _rightDockTab.value = RightDockTab.SHADERS
        _isRightDockOpen.value = true
    }

    fun saveShader(shader: ShaderProgram) {
        assetManager.shaders[shader.id] = shader
        _editingShader.value = shader
        log("Shader", "Saved shader: ${shader.name}")
    }

    // AI Copilot with gemini-3.1-pro-preview High Thinking
    fun askAiCopilot(prompt: String, taskType: String = "general") {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = null
            log("AI", "Consulting High-Thinking AI Director for: $taskType...")

            val context = buildString {
                appendLine("Active Entities in Scene (${_scene.value.entities.size}):")
                _scene.value.entities.forEach {
                    appendLine("- ${it.name} [Pos: ${it.transform.position}, Mesh: ${it.meshRenderer?.meshId}, RigidBody: ${it.rigidBody != null}]")
                }
                val selected = _scene.value.findEntity(_selectedEntityId.value ?: "")
                if (selected != null) {
                    appendLine("Currently Selected Entity: ${selected.name}")
                    appendLine("Script attached: ${selected.luaScript?.scriptName ?: "None"}")
                }
            }

            val result = GeminiEngineAssistant.queryGenerateScript(prompt, context, taskType)
            _aiResponse.value = result
            _isAiLoading.value = false

            if (result.isSuccess) {
                log("AI", "Respon AI diterima dengan sukses.")
            } else {
                log("AI", "Gagal memproses prompt: ${result.errorMessage}")
            }
        }
    }

    fun fixCurrentScriptError() {
        val err = _latestScriptError.value ?: return
        val currentScriptObj = _editingScript.value ?: assetManager.scripts.values.firstOrNull() ?: return
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = null
            setWorkspaceTab(WorkspaceTab.AI_COPILOT)
            log("AI", "Memperbaiki error script otomatis dengan Gemini 3.1 Pro Preview...")
            val context = "Scene: ${_scene.value.name}, Entities: ${_scene.value.entities.size}"
            val res = GeminiEngineAssistant.queryFixError(currentScriptObj.content, err, context)
            _aiResponse.value = res
            _isAiLoading.value = false
            if (res.isSuccess) {
                log("AI", "Solusi perbaikan error berhasil ditemukan!")
            }
        }
    }

    fun clearScriptError() {
        _latestScriptError.value = null
    }

    private val _inspectingGlbResult = MutableStateFlow<com.example.engine.model.GlbInspectionResult?>(null)
    val inspectingGlbResult: StateFlow<com.example.engine.model.GlbInspectionResult?> = _inspectingGlbResult.asStateFlow()

    fun openGlbInspection(file: File) {
        try {
            val inspection = GlbParser.inspectAndParseGlbFile(file)
            _inspectingGlbResult.value = inspection
            log("Asset", "Membuka dialog inspeksi 3D GLB: ${file.name} (${inspection.mesh.vertexCount} verts, ${inspection.boneCount} tulang, ${inspection.animations.size} animasi)")
        } catch (e: Exception) {
            log("AssetError", "Gagal membaca file GLB: ${e.message}")
        }
    }

    fun closeGlbInspection() {
        _inspectingGlbResult.value = null
    }

    fun spawnGlbFromInspection(
        inspection: com.example.engine.model.GlbInspectionResult,
        colliderShape: ColliderShape = ColliderShape.BOX,
        customScale: Float = inspection.suggestedScale,
        role: String = "Dynamic Actor"
    ) {
        try {
            assetManager.meshes[inspection.mesh.id] = inspection.mesh
            assetManager.materials[inspection.material.id] = inspection.material

            val effectiveScale = if (customScale > 0.0001f) customScale else 1.0f
            val halfHeight = (inspection.boundingBox.size.y * effectiveScale) * 0.5f
            val spawnPos = Vec3(0f, maxOf(halfHeight, 0.05f), 0f)

            val isStatic = role.contains("Static") || role.contains("Terrain") || role.contains("Ground")
            val entity = Entity(
                id = "ent_glb_${LogIdCounter.nextId()}",
                name = inspection.modelName,
                transform = TransformComponent(
                    position = spawnPos,
                    rotation = Vec3.ZERO,
                    scale = Vec3(effectiveScale, effectiveScale, effectiveScale)
                ),
                meshRenderer = MeshRendererComponent(
                    meshId = inspection.mesh.id,
                    materialId = inspection.material.id
                ),
                rigidBody = RigidBodyComponent(
                    mass = if (isStatic) 0f else 1.0f,
                    isStatic = isStatic,
                    colliderShape = colliderShape
                )
            )

            _scene.value = _scene.value.copy(entities = _scene.value.entities + entity)
            _selectedEntityId.value = entity.id

            // Otomatis fokuskan dan sesuaikan jarak kamera ke model baru
            camera.target = spawnPos
            val modelRadius = (inspection.boundingBox.size.length() * effectiveScale * 0.5f).coerceAtLeast(1.2f)
            camera.distance = (modelRadius * 2.4f).coerceIn(2.5f, 50.0f)
            camera.pitchDegrees = 20.0f
            camera.yawDegrees = 35.0f

            setWorkspaceTab(WorkspaceTab.SCENE_VIEW)
            _inspectingGlbResult.value = null
            log("Asset", "Berhasil spawn model GLB '${inspection.modelName}'! Kamera langsung difokuskan ke model.")
        } catch (e: Exception) {
            log("AssetError", "Gagal men-spawn GLB: ${e.message}")
        }
    }

    fun spawnMeshAsset(mesh: com.example.engine.model.Mesh) {
        assetManager.meshes[mesh.id] = mesh
        val entity = Entity(
            id = "ent_mesh_${LogIdCounter.nextId()}",
            name = mesh.name,
            transform = TransformComponent(position = Vec3(0f, 1.5f, 0f)),
            meshRenderer = MeshRendererComponent(meshId = mesh.id, materialId = "mat_default"),
            rigidBody = RigidBodyComponent(mass = 1.0f, colliderShape = ColliderShape.BOX)
        )
        _scene.value = _scene.value.copy(entities = _scene.value.entities + entity)
        _selectedEntityId.value = entity.id
        camera.target = entity.transform.position
        setWorkspaceTab(WorkspaceTab.SCENE_VIEW)
        log("Asset", "Spawned mesh asset: ${mesh.name}")
    }

    fun importAndSpawnGlbFile(file: File) {
        try {
            val inspection = GlbParser.inspectAndParseGlbFile(file)
            spawnGlbFromInspection(inspection)
        } catch (e: Exception) {
            log("AssetError", "Gagal memproses file GLB: ${e.message}")
        }
    }

    fun importGlbToAssets(file: File) {
        try {
            val inspection = GlbParser.inspectAndParseGlbFile(file)
            assetManager.meshes[inspection.mesh.id] = inspection.mesh
            assetManager.materials[inspection.material.id] = inspection.material
            log("Asset", "Model GLB disimpan di Asset Browser: ${inspection.modelName} (${inspection.mesh.vertexCount} verts)")
        } catch (e: Exception) {
            log("AssetError", "Gagal memproses file GLB: ${e.message}")
        }
    }

    fun importLuaFile(file: File) {
        try {
            val content = file.readText()
            val asset = LuaScriptAsset(
                id = "script_${LogIdCounter.nextId()}",
                name = file.name,
                description = "Diimpor dari ${file.name}",
                content = content
            )
            assetManager.scripts[asset.id] = asset
            editScript(asset)
            log("Lua", "Script Lua berhasil dibuka dari file: ${file.name}")
        } catch (e: Exception) {
            log("LuaError", "Gagal membaca script: ${e.message}")
        }
    }

    // EngineCallbacks implementation
    override fun log(tag: String, message: String) {
        _consoleLogs.value = (_consoleLogs.value + ConsoleLogItem(tag = tag, message = message)).takeLast(100)
        if (tag == "LuaError") {
            _latestScriptError.value = message
        }
    }

    override fun playSound(soundName: String) {
        AudioSynthesizer.play(soundName)
    }

    override fun updateEntity(updated: Entity) {
        _scene.value = _scene.value.copy(
            entities = _scene.value.entities.map { if (it.id == updated.id) updated else it }
        )
    }

    override fun getEntity(id: String): Entity? = _scene.value.findEntity(id)

    override fun setCameraTarget(target: Vec3) {
        camera.target = target
    }

    override fun setCameraDistance(distance: Float) {
        camera.distance = distance.coerceIn(1.5f, 50.0f)
    }

    override fun setCameraOrbit(yaw: Float, pitch: Float) {
        camera.yawDegrees = yaw
        camera.pitchDegrees = pitch.coerceIn(-85f, 85f)
    }

    // Raycast selection helper
    fun pickEntityAtScreen(screenX: Float, screenY: Float, width: Float, height: Float) {
        val ray = MathUtils.screenPointToRay(
            screenX = screenX,
            screenY = screenY,
            viewportWidth = width,
            viewportHeight = height,
            cameraPos = camera.getEyePosition(),
            cameraTarget = camera.target,
            fovYDegrees = camera.fovDegrees
        )

        var closestHitDist = Float.MAX_VALUE
        var hitEntityId: String? = null

        for (entity in _scene.value.entities) {
            if (!entity.isEnabled) continue
            val pos = entity.transform.position
            val scale = entity.transform.scale
            val radius = maxOf(scale.x, scale.y, scale.z) * 0.7f

            val hit = ray.intersectSphere(pos, radius)
            if (hit != null && hit < closestHitDist) {
                closestHitDist = hit
                hitEntityId = entity.id
            }
        }

        if (hitEntityId != null) {
            selectEntity(hitEntityId)
            playSound("click")
        }
    }

    // Godot 4 Dock & Project Manager Controls
    fun setInLobby(inLobby: Boolean) {
        _isInLobby.value = inLobby
        if (inLobby) {
            log("Lobby", "Opened Project Hub & Scene Manager")
        } else {
            log("Engine", "Active Scene: ${_scene.value.name}")
        }
    }

    fun toggleLeftDock() {
        _isLeftDockOpen.value = !_isLeftDockOpen.value
    }

    fun toggleRightDock() {
        _isRightDockOpen.value = !_isRightDockOpen.value
    }

    fun toggleBottomDock() {
        _isBottomDockOpen.value = !_isBottomDockOpen.value
    }

    fun setLeftDockTab(tab: LeftDockTab) {
        _leftDockTab.value = tab
        _isLeftDockOpen.value = true
    }

    fun setRightDockTab(tab: RightDockTab) {
        _rightDockTab.value = tab
        _isRightDockOpen.value = true
    }

    fun setBottomDockTab(tab: BottomDockTab) {
        _bottomDockTab.value = tab
        _isBottomDockOpen.value = true
    }

    fun openProject(project: ProjectItem) {
        _activeProjectId.value = project.id
        _scene.value = project.scene
        _sceneName.value = project.scene.name
        initialSceneBackup = project.scene
        val firstEntityId = project.scene.entities.firstOrNull()?.id
        _selectedEntityId.value = firstEntityId
        syncSceneScriptsToAssetManager(project.scene)
        syncEditingScriptForSelectedEntity(firstEntityId)
        _isInLobby.value = false
        log("Project", "Loaded Scene: ${project.name} (${project.templateName})")
    }

    fun createProject(name: String, templateType: String, orientation: ProjectOrientation = ProjectOrientation.LANDSCAPE) {
        val newScene = when (templateType) {
            "FPS" -> Scene.createFpsArena(name)
            "CYBER" -> Scene.createCyberpunkGlow(name)
            "BLANK" -> Scene.createBlank3DScene(name)
            else -> Scene.createDefaultPhysicsPlayground().copy(
                id = "scene_${System.currentTimeMillis()}",
                name = name
            )
        }
        val newProject = ProjectItem(
            id = "proj_${System.currentTimeMillis()}",
            name = name,
            templateName = templateType,
            entityCount = newScene.entities.size,
            lastModified = "Just now",
            scene = newScene,
            orientation = orientation
        )
        _projects.value = listOf(newProject) + _projects.value
        openProject(newProject)
        log("Project", "Created new project scene: $name (${orientation.badge})")
    }

    fun setProjectOrientation(projectId: String, orientation: ProjectOrientation) {
        val list = _projects.value.map {
            if (it.id == projectId) it.copy(orientation = orientation) else it
        }
        _projects.value = list
        log("Project", "Changed project orientation to: ${orientation.title}")
    }

    fun duplicateProject(project: ProjectItem) {
        val dup = project.copy(
            id = "proj_${System.currentTimeMillis()}",
            name = "${project.name} (Copy)",
            lastModified = "Just now",
            orientation = project.orientation
        )
        _projects.value = listOf(dup) + _projects.value
        log("Project", "Duplicated project: ${project.name}")
    }

    fun deleteProject(projectId: String) {
        _projects.value = _projects.value.filter { it.id != projectId }
        log("Project", "Deleted project $projectId")
    }

    fun saveCurrentScene() {
        val curr = _scene.value
        val list = _projects.value.toMutableList()
        val idx = list.indexOfFirst { it.id == _activeProjectId.value }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                entityCount = curr.entities.size,
                lastModified = "Just now",
                scene = curr
            )
        } else {
            list.add(
                ProjectItem(
                    id = _activeProjectId.value,
                    name = curr.name,
                    templateName = "Custom Scene",
                    entityCount = curr.entities.size,
                    lastModified = "Just now",
                    scene = curr
                )
            )
        }
        _projects.value = list
        log("Project", "Saved scene: ${curr.name} (${curr.entities.size} nodes)")
    }
}
