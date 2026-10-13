package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EngineViewModel
import com.example.viewmodel.WorkspaceTab

@Composable
fun LuaEditorView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScript by viewModel.editingScript.collectAsState()
    val allScripts = viewModel.assetManager.scripts.values.toList()
    val scene by viewModel.scene.collectAsState()
    val selectedId by viewModel.selectedEntityId.collectAsState()
    val selectedEntity = scene.findEntity(selectedId ?: "")

    val script = currentScript ?: allScripts.firstOrNull() ?: return
    var codeContent by remember(script.id) { mutableStateOf(script.content) }
    var scriptDropdownExpanded by remember { mutableStateOf(false) }
    var entityPickerExpanded by remember { mutableStateOf(false) }

    val isAttachedToSelected = selectedEntity?.luaScript?.scriptId == script.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
            .padding(8.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(
                    onClick = { scriptDropdownExpanded = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp).testTag("select_script_dropdown")
                ) {
                    Text(script.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                DropdownMenu(
                    expanded = scriptDropdownExpanded,
                    onDismissRequest = { scriptDropdownExpanded = false }
                ) {
                    allScripts.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name, fontSize = 12.sp) },
                            onClick = {
                                viewModel.editScript(item)
                                scriptDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            val latestError by viewModel.latestScriptError.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (latestError != null) {
                    Button(
                        onClick = { viewModel.fixCurrentScriptError() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp).testTag("one_tap_fix_lua_editor_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Fix", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(4.dp))
                }

                // Copy Code to Clipboard Button
                OutlinedButton(
                    onClick = {
                        viewModel.copyToClipboard(
                            context = context,
                            text = codeContent,
                            label = "Kode Lua ${script.name}"
                        )
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp).testTag("copy_script_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Salin", fontSize = 10.sp, color = Color(0xFF00E5FF))
                }

                Spacer(Modifier.width(4.dp))

                Button(
                    onClick = { viewModel.saveScript(script.id, codeContent) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp).testTag("save_script_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Save", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Direct Entity Attachment Ribbon Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF161B22))
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = if (isAttachedToSelected) Color(0xFF00E676) else Color(0xFF00E5FF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))

                if (selectedEntity != null) {
                    Text(
                        text = "Target: ${selectedEntity.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    if (isAttachedToSelected) {
                        Text(
                            text = "(Terpasang)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                } else {
                    Text(
                        text = "Belum ada entitas dipilih",
                        fontSize = 11.sp,
                        color = Color(0xFF8B949E)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedEntity != null) {
                    if (isAttachedToSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF21262D))
                                .clickable { viewModel.detachScriptFromSelectedEntity() }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Lepas", fontSize = 10.sp, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.attachScriptToSelectedEntity(script.id) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp).testTag("attach_script_from_lua_panel"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Text("Attach ke ${selectedEntity.name}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                } else {
                    Box {
                        OutlinedButton(
                            onClick = { entityPickerExpanded = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Pilih Entitas...", fontSize = 10.sp)
                        }

                        DropdownMenu(
                            expanded = entityPickerExpanded,
                            onDismissRequest = { entityPickerExpanded = false }
                        ) {
                            scene.entities.forEach { entity ->
                                DropdownMenuItem(
                                    text = { Text(entity.name, fontSize = 11.sp) },
                                    onClick = {
                                        viewModel.selectEntity(entity.id)
                                        viewModel.attachScriptToEntity(entity.id, script.id)
                                        entityPickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Quick Snippets Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SnippetChip("+ update(dt)") { codeContent += "\nfunction update(dt)\n    \nend\n" }
            SnippetChip("+ apply_force") { codeContent += "\nphysics:apply_force(input_h * 15.0, 0, input_v * 15.0)\n" }
            SnippetChip("+ apply_impulse") { codeContent += "\nphysics:apply_impulse(0, 8.5, 0)\n" }
            SnippetChip("+ rotate") { codeContent += "\nentity:rotate(0, 45.0 * dt, 0)\n" }
            SnippetChip("+ play_sound") { codeContent += "\nengine:play_sound(\"jump\")\n" }
            SnippetChip("+ on_collision") { codeContent += "\nfunction on_collision(other)\n    engine:log(\"Tabrakan dengan: \" .. other)\nend\n" }
        }

        Spacer(Modifier.height(6.dp))

        // Code Editor Field
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(8.dp)
        ) {
            TextField(
                value = codeContent,
                onValueChange = { codeContent = it },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("lua_code_input"),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFFE6EDF3),
                    lineHeight = 18.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF161B22),
                    unfocusedContainerColor = Color(0xFF161B22),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun SnippetChip(label: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21262D)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF58A6FF),
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { onClick() }
        )
    }
}
