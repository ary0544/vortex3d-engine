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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.engine.model.Entity
import com.example.viewmodel.EngineViewModel

@Composable
fun HierarchyView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val scene by viewModel.scene.collectAsState()
    val selectedId by viewModel.selectedEntityId.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredEntities = remember(scene.entities, searchQuery) {
        if (searchQuery.isBlank()) scene.entities
        else scene.entities.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        // Top action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scene Hierarchy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Box {
                Button(
                    onClick = { showAddMenu = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp).testTag("add_entity_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Entity", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Add 3D", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Cube (RigidBody)", fontSize = 12.sp) },
                        onClick = { viewModel.addPrimitive("cube"); showAddMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Sphere (Bouncing)", fontSize = 12.sp) },
                        onClick = { viewModel.addPrimitive("sphere"); showAddMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Cylinder", fontSize = 12.sp) },
                        onClick = { viewModel.addPrimitive("cylinder"); showAddMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Torus (Holo Portal)", fontSize = 12.sp) },
                        onClick = { viewModel.addPrimitive("torus"); showAddMenu = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search nodes...", fontSize = 11.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_entity_input")
        )

        Spacer(Modifier.height(8.dp))

        // Entity List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(filteredEntities, key = { index, entity -> "${entity.id}_$index" }) { _, entity ->
                EntityRow(
                    entity = entity,
                    isSelected = entity.id == selectedId,
                    onSelect = { viewModel.selectEntity(entity.id) },
                    onToggleVisibility = {
                        viewModel.updateSelectedEntity { it.copy(isEnabled = !it.isEnabled) }
                    },
                    onDuplicate = { viewModel.duplicateSelectedEntity() },
                    onDelete = { viewModel.deleteSelectedEntity() }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun EntityRow(
    entity: Entity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("entity_item_${entity.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon according to entity components
            Icon(
                imageVector = when {
                    entity.light != null -> Icons.Default.Lightbulb
                    entity.luaScript != null -> Icons.Default.PlayArrow
                    entity.rigidBody != null -> Icons.Default.Speed
                    else -> Icons.Default.ViewInAr
                },
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
                Row {
                    if (entity.rigidBody != null) {
                        Text(
                            text = if (entity.rigidBody.isStatic) "Static" else "Physics",
                            fontSize = 10.sp,
                            color = Color(0xFF00E676),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    if (entity.luaScript != null) {
                        Text(
                            text = "Lua",
                            fontSize = 10.sp,
                            color = Color(0xFF4FC3F7)
                        )
                    }
                }
            }

            if (isSelected) {
                IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
