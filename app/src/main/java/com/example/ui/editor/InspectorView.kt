package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Transform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.math.Vec3
import com.example.engine.model.ColliderShape
import com.example.engine.model.Material
import com.example.viewmodel.EngineViewModel
import com.example.viewmodel.WorkspaceTab

@Composable
fun InspectorView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scene by viewModel.scene.collectAsState()
    val selectedId by viewModel.selectedEntityId.collectAsState()
    val allScripts by viewModel.scriptsState.collectAsState()
    val selectedEntity = scene.findEntity(selectedId ?: "")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Entity Inspector",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (selectedEntity == null) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = "No entity selected. Tap an object in the 3D viewport or select from Hierarchy.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            return@Column
        }

        Spacer(Modifier.height(12.dp))

        // Name input
        OutlinedTextField(
            value = selectedEntity.name,
            onValueChange = { newName ->
                viewModel.updateSelectedEntity { it.copy(name = newName) }
            },
            label = { Text("Entity Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("entity_name_input")
        )

        Spacer(Modifier.height(12.dp))

        // 1. Transform Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Transform, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Transform", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(Modifier.height(8.dp))

                // Position X, Y, Z
                Text("Position (X, Y, Z)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AxisStepper(modifier = Modifier.weight(1f), axis = "X", value = selectedEntity.transform.position.x, color = Color(0xFFFF5252)) { delta ->
                        val p = selectedEntity.transform.position
                        viewModel.updateSelectedEntity { it.withPosition(p.copy(x = p.x + delta)) }
                    }
                    AxisStepper(modifier = Modifier.weight(1f), axis = "Y", value = selectedEntity.transform.position.y, color = Color(0xFF69F0AE)) { delta ->
                        val p = selectedEntity.transform.position
                        viewModel.updateSelectedEntity { it.withPosition(p.copy(y = p.y + delta)) }
                    }
                    AxisStepper(modifier = Modifier.weight(1f), axis = "Z", value = selectedEntity.transform.position.z, color = Color(0xFF448AFF)) { delta ->
                        val p = selectedEntity.transform.position
                        viewModel.updateSelectedEntity { it.withPosition(p.copy(z = p.z + delta)) }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Scale
                Text("Scale Uniform", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = selectedEntity.transform.scale.x,
                    onValueChange = { s ->
                        viewModel.updateSelectedEntity { it.withScale(Vec3(s, s, s)) }
                    },
                    valueRange = 0.2f..4.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 2. RigidBody Section
        val rb = selectedEntity.rigidBody
        if (rb != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF00E676))
                            Spacer(Modifier.width(6.dp))
                            Text("RigidBody (Real-time Physics)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Is Static (Immovable):", fontSize = 12.sp)
                        Switch(
                            checked = rb.isStatic,
                            onCheckedChange = { isStat ->
                                viewModel.updateSelectedEntity { it.copy(rigidBody = rb.copy(isStatic = isStat)) }
                            }
                        )
                    }

                    if (!rb.isStatic) {
                        Text("Restitution (Bounciness): ${String.format("%.2f", rb.restitution)}", fontSize = 12.sp)
                        Slider(
                            value = rb.restitution,
                            onValueChange = { b ->
                                viewModel.updateSelectedEntity { it.copy(rigidBody = rb.copy(restitution = b)) }
                            },
                            valueRange = 0f..1f
                        )

                        Text("Mass (kg): ${String.format("%.1f", rb.mass)}", fontSize = 12.sp)
                        Slider(
                            value = rb.mass,
                            onValueChange = { m ->
                                viewModel.updateSelectedEntity { it.copy(rigidBody = rb.copy(mass = m)) }
                            },
                            valueRange = 0.5f..10.0f
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 3. Lua Script Section
        val luaComp = selectedEntity.luaScript
        var showScriptPicker by remember { mutableStateOf(false) }
        var showNewScriptDialog by remember { mutableStateOf(false) }
        var newScriptName by remember { mutableStateOf("") }
        var selectedTemplate by remember { mutableStateOf("hero") }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF4FC3F7))
                        Spacer(Modifier.width(6.dp))
                        Text("Lua Script Runtime", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (luaComp != null) {
                        Switch(
                            checked = luaComp.isEnabled,
                            onCheckedChange = { en ->
                                viewModel.updateSelectedEntity { it.copy(luaScript = luaComp.copy(isEnabled = en)) }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                if (luaComp != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF161B22))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (luaComp.isEnabled) Color(0xFF00E676) else Color(0xFFFF5252))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(luaComp.scriptName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                            Text("ID: ${luaComp.scriptId}", fontSize = 10.sp, color = Color(0xFF8B949E))
                        }

                        // Detach button
                        IconButton(
                            onClick = { viewModel.detachScriptFromSelectedEntity() },
                            modifier = Modifier.size(28.dp).testTag("detach_script_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Lepas Script",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val scriptAsset = viewModel.assetManager.scripts[luaComp.scriptId]
                                    ?: com.example.engine.lua.LuaScriptAsset(
                                        luaComp.scriptId,
                                        luaComp.scriptName,
                                        "Custom",
                                        luaComp.scriptContent
                                    )
                                viewModel.editScript(scriptAsset)
                            },
                            modifier = Modifier.weight(1f).testTag("open_lua_ide_button")
                        ) {
                            Text("Edit di Lua IDE", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showScriptPicker = true },
                            modifier = Modifier.weight(1f).testTag("change_script_button")
                        ) {
                            Text("Ganti Script", fontSize = 11.sp)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showScriptPicker = true },
                            modifier = Modifier.fillMaxWidth().testTag("attach_lua_script_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+ Attach Script Lua ke Entitas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showScriptPicker,
                            onDismissRequest = { showScriptPicker = false }
                        ) {
                            Text(
                                text = "Pilih Script untuk Dipasang:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color(0xFF00E5FF)
                            )

                            allScripts.forEach { scriptAsset ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(scriptAsset.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Text(scriptAsset.description, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                        }
                                    },
                                    onClick = {
                                        viewModel.attachScriptToSelectedEntity(scriptAsset.id)
                                        showScriptPicker = false
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Buat Script Baru...", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showScriptPicker = false
                                    newScriptName = "${selectedEntity.name}_Script"
                                    showNewScriptDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showNewScriptDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showNewScriptDialog = false },
                title = { Text("Buat & Pasang Script Lua", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = newScriptName,
                            onValueChange = { newScriptName = it },
                            label = { Text("Nama Script") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Pilih Template Dasar:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        listOf(
                            "odyssey" to "⚔️ Assassin Odyssey (Analog & Camera Follow)",
                            "hero" to "🎮 TPP Hero / Character Controller",
                            "orbit" to "🔄 Orbit / Rotasi Objek 3D",
                            "terrain" to "⛰️ Permukaan Tanah GLB / Collider",
                            "custom" to "📝 Blank Script Kosong"
                        ).forEach { (type, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTemplate = type }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = selectedTemplate == type,
                                    onClick = { selectedTemplate = type }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createAndAttachScript(
                                name = if (newScriptName.isBlank()) "entity_script" else newScriptName,
                                templateType = selectedTemplate
                            )
                            showNewScriptDialog = false
                        }
                    ) {
                        Text("Buat & Attach", fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showNewScriptDialog = false }) {
                        Text("Batal", fontSize = 12.sp)
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // 4. Material & Custom Shader Section
        val mr = selectedEntity.meshRenderer
        if (mr != null) {
            val material = viewModel.assetManager.materials[mr.materialId] ?: Material.defaultMaterial()
            val shader = viewModel.assetManager.shaders[material.shaderProgramId]

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFFF4081))
                        Spacer(Modifier.width(6.dp))
                        Text("Material & Custom Shader", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(6.dp))

                    Text("Shader: ${shader?.name ?: "PBR Standard"}", fontSize = 12.sp, color = Color(0xFF00E5FF))

                    Spacer(Modifier.height(6.dp))

                    Text("Roughness: ${String.format("%.2f", material.roughness)}", fontSize = 11.sp)
                    Slider(
                        value = material.roughness,
                        onValueChange = { r ->
                            val updatedMat = material.copy(roughness = r)
                            viewModel.assetManager.materials[material.id] = updatedMat
                            viewModel.updateSelectedEntity { it }
                        },
                        valueRange = 0.05f..1.0f
                    )

                    Text("Metallic: ${String.format("%.2f", material.metallic)}", fontSize = 11.sp)
                    Slider(
                        value = material.metallic,
                        onValueChange = { m ->
                            val updatedMat = material.copy(metallic = m)
                            viewModel.assetManager.materials[material.id] = updatedMat
                            viewModel.updateSelectedEntity { it }
                        },
                        valueRange = 0f..1f
                    )

                    val mesh = viewModel.assetManager.meshes[mr.meshId]
                    if (mesh != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Mesh: ${mesh.name} (${mesh.vertexCount} v, ${mesh.triangleCount} tris)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = {
                                    val meshInfo = buildString {
                                        appendLine("=== INFORMASI MESH ENTITAS 3D ===")
                                        appendLine("Entitas: ${selectedEntity.name} (ID: ${selectedEntity.id})")
                                        appendLine("Mesh ID: ${mr.meshId} (${mesh.name})")
                                        appendLine("Vertices: ${mesh.vertexCount}, Triangles: ${mesh.triangleCount}")
                                        appendLine("Posisi: (${selectedEntity.transform.position.x}, ${selectedEntity.transform.position.y}, ${selectedEntity.transform.position.z})")
                                        appendLine("Skala: (${selectedEntity.transform.scale.x}, ${selectedEntity.transform.scale.y}, ${selectedEntity.transform.scale.z})")
                                        appendLine("Material: ${material.name} (Roughness: ${material.roughness}, Metallic: ${material.metallic})")
                                    }
                                    viewModel.copyToClipboard(context, meshInfo, "Info Mesh 3D")
                                },
                                modifier = Modifier.size(24.dp).testTag("copy_inspector_mesh_info_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Salin Info Model", modifier = Modifier.size(14.dp), tint = Color(0xFF00E5FF))
                            }
                        }
                    }

                    if (shader != null) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.editShader(shader) },
                            modifier = Modifier.fillMaxWidth().testTag("open_shader_studio_button")
                        ) {
                            Text("Edit in Shader Studio", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AxisStepper(
    modifier: Modifier = Modifier,
    axis: String,
    value: Float,
    color: Color,
    onStep: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(axis, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.width(2.dp))
                Text(String.format("%.1f", value), fontSize = 10.sp, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF21262D))
                        .clickable { onStep(-0.5f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .size(19.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF21262D))
                        .clickable { onStep(0.5f) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
