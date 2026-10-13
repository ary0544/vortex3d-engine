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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.engine.model.GlbParser
import java.io.File

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

class EngineViewModel : ViewModel(), EngineCallbacks {

    val camera = Camera3D()
    val physicsEngine = PhysicsEngine()
    val assetManager = AssetManager()

    private val _scene = MutableStateFlow(Scene.createDefaultPhysicsPlayground())
    val scene: StateFlow<Scene> = _scene.asStateFlow()

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

    private val _editingShader = MutableStateFlow<ShaderProgram?>(null)
    val editingShader: StateFlow<ShaderProgram?> = _editingShader.asStateFlow()

    private val _renderStats = MutableStateFlow(RenderStats())
    val renderStats: StateFlow<RenderStats> = _renderStats.asStateFlow()

    private val _cameraRevision = MutableStateFlow(0L)
    val cameraRevision: StateFlow<Long> = _cameraRevision.asStateFlow()

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
        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
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
        _scene.value = initialSceneBackup
        luaInterpreters.clear()
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
            _scene.value = initialSceneBackup
            luaInterpreters.clear()
            log("Engine", "Exited Game Play Mode")
        }
    }

    fun selectEntity(id: String?) {
        _selectedEntityId.value = id
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
        if (now - lastStatsUpdateTime >= 1000L) {
            lastStatsUpdateTime = now
            if (_renderStats.value.trianglesDrawn != stats.trianglesDrawn ||
                _renderStats.value.drawCalls != stats.drawCalls) {
                _renderStats.value = stats
            }
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
    }

    fun saveScript(id: String, content: String) {
        val existing = assetManager.scripts[id]
        if (existing != null) {
            val updated = existing.copy(content = content)
            assetManager.scripts[id] = updated
            _editingScript.value = updated
            // Update entity script if active
            val sel = _selectedEntityId.value
            if (sel != null) {
                updateSelectedEntity {
                    if (it.luaScript?.scriptId == id) {
                        it.copy(luaScript = it.luaScript.copy(scriptContent = content))
                    } else it
                }
            }
            log("Lua", "Saved script: ${updated.name}")
        }
    }

    fun editShader(shader: ShaderProgram) {
        _editingShader.value = shader
        _workspaceTab.value = WorkspaceTab.SHADER_EDITOR
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

    fun importAndSpawnGlbFile(file: File) {
        try {
            val result = GlbParser.parse(file.inputStream(), file.nameWithoutExtension)
            assetManager.meshes[result.mesh.id] = result.mesh
            assetManager.materials[result.material.id] = result.material

            val entity = Entity(
                id = "ent_glb_${LogIdCounter.nextId()}",
                name = result.name,
                transform = TransformComponent(position = Vec3(0f, 3.5f, 0f)),
                meshRenderer = MeshRendererComponent(meshId = result.mesh.id, materialId = result.material.id),
                rigidBody = RigidBodyComponent(mass = 1.0f, colliderShape = ColliderShape.BOX)
            )
            _scene.value = _scene.value.copy(entities = _scene.value.entities + entity)
            _selectedEntityId.value = entity.id
            log("Asset", "Berhasil mengimpor & spawn model GLB: ${result.name} (${result.vertexCount} verts, ${result.triangleCount} tris)")
        } catch (e: Exception) {
            log("AssetError", "Gagal memproses file GLB: ${e.message}")
        }
    }

    fun importGlbToAssets(file: File) {
        try {
            val result = GlbParser.parse(file.inputStream(), file.nameWithoutExtension)
            assetManager.meshes[result.mesh.id] = result.mesh
            assetManager.materials[result.material.id] = result.material
            log("Asset", "Model GLB disimpan di Asset Browser: ${result.name}")
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
        initialSceneBackup = project.scene
        _selectedEntityId.value = project.scene.entities.firstOrNull()?.id
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
