package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.viewmodel.EngineViewModel

@Composable
fun AssetBrowserView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var showFullScreenPicker by remember { mutableStateOf(false) }
    val categories = listOf("3D Meshes", "Prefabs", "Materials", "Lua Scripts", "Audio SFX", "Shaders")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Asset Manager",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Button(
                onClick = { showFullScreenPicker = true },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(30.dp).testTag("import_glb_file_manager_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("Impor .GLB", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(6.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth().height(32.dp)
        ) {
            categories.forEachIndexed { index, title ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = { Text(title, fontSize = 11.sp) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when (selectedCategoryIndex) {
            0 -> MeshesTab(viewModel)
            1 -> PrefabsTab(viewModel)
            2 -> MaterialsTab(viewModel)
            3 -> LuaScriptsTab(viewModel)
            4 -> AudioTab(viewModel)
            5 -> ShadersTab(viewModel)
        }
    }

    if (showFullScreenPicker) {
        FullScreenFilePickerDialog(
            viewModel = viewModel,
            onDismiss = { showFullScreenPicker = false }
        )
    }
}

@Composable
private fun MeshesTab(viewModel: EngineViewModel) {
    val meshes = viewModel.assetManager.meshes.values.toList()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(meshes, key = { index, mesh -> "${mesh.id}_$index" }) { _, mesh ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(mesh.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${mesh.vertexCount} Verts • ${mesh.triangleCount} Tris", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = { viewModel.spawnMeshAsset(mesh) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Spawn", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefabsTab(viewModel: EngineViewModel) {
    val prefabs = viewModel.assetManager.prefabs
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(prefabs, key = { index, prefab -> "${prefab.id}_$index" }) { _, prefab ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Extension, contentDescription = null, tint = Color(0xFF00E676))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(prefab.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Pre-configured ECS Entity with Physics", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = {
                            val ent = prefab.createEntity(Vec3(0f, 4f, 0f))
                            viewModel.updateSelectedEntity { ent }
                            viewModel.selectEntity(ent.id)
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Spawn", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialsTab(viewModel: EngineViewModel) {
    val materials = viewModel.assetManager.materials.values.toList()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(materials, key = { index, mat -> "${mat.id}_$index" }) { _, mat ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color(mat.albedoColor))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(mat.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Roughness: ${mat.roughness} • Metallic: ${mat.metallic}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.updateSelectedEntity {
                                if (it.meshRenderer != null) it.copy(meshRenderer = it.meshRenderer.copy(materialId = mat.id)) else it
                            }
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Apply to Selected", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LuaScriptsTab(viewModel: EngineViewModel) {
    val scripts = viewModel.assetManager.scripts.values.toList()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(scripts, key = { index, script -> "${script.id}_$index" }) { _, script ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF4FC3F7))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(script.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(script.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { viewModel.attachScriptToSelectedEntity(script.id) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Attach", fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = { viewModel.editScript(script) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("IDE", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTab(viewModel: EngineViewModel) {
    val sounds = listOf("jump", "impact", "laser", "explosion", "coin")
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(sounds, key = { index, sfx -> "${sfx}_$index" }) { _, sfx ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFFFFD54F))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(sfx.replaceFirstChar { it.uppercase() } + " SFX", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Real-time procedural audio synthesis", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.playSound(sfx) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Test SFX", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShadersTab(viewModel: EngineViewModel) {
    val shaders = viewModel.assetManager.shaders.values.toList()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(shaders, key = { index, shader -> "${shader.id}_$index" }) { _, shader ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color(0xFF00E5FF))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(shader.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(shader.type.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = { viewModel.editShader(shader) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Edit GLSL", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
