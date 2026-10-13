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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
    val scene by viewModel.scene.collectAsState()
    val selectedId by viewModel.selectedEntityId.collectAsState()
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
                    Text("Script: ${luaComp.scriptName}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
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
                        modifier = Modifier.fillMaxWidth().testTag("open_lua_ide_button")
                    ) {
                        Text("Edit in Lua Code IDE", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            val defaultScript = viewModel.assetManager.scripts["script_hero"]
                            if (defaultScript != null) {
                                viewModel.updateSelectedEntity {
                                    it.copy(
                                        luaScript = com.example.engine.model.LuaScriptComponent(
                                            scriptId = defaultScript.id,
                                            scriptName = defaultScript.name,
                                            scriptContent = defaultScript.content
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Attach Lua Script", fontSize = 12.sp)
                    }
                }
            }
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

                    if (shader != null) {
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
