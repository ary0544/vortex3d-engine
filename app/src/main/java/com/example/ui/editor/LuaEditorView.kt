package com.example.ui.editor

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
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
    val currentScript by viewModel.editingScript.collectAsState()
    val allScripts = viewModel.assetManager.scripts.values.toList()

    val script = currentScript ?: allScripts.firstOrNull() ?: return
    var codeContent by remember(script.id) { mutableStateOf(script.content) }
    var scriptDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
            .padding(12.dp)
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
                    modifier = Modifier.testTag("select_script_dropdown")
                ) {
                    Text(script.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                DropdownMenu(
                    expanded = scriptDropdownExpanded,
                    onDismissRequest = { scriptDropdownExpanded = false }
                ) {
                    allScripts.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = {
                                viewModel.editScript(item)
                                scriptDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            val latestError by viewModel.latestScriptError.collectAsState()
            Row {
                if (latestError != null) {
                    Button(
                        onClick = { viewModel.fixCurrentScriptError() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.testTag("one_tap_fix_lua_editor_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("⚡ Fix Error", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                }

                OutlinedButton(
                    onClick = {
                        viewModel.setWorkspaceTab(WorkspaceTab.AI_COPILOT)
                    },
                    modifier = Modifier.testTag("ai_assist_script_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI Prompts", fontSize = 11.sp, color = Color(0xFF00E5FF))
                }

                Spacer(Modifier.width(6.dp))

                Button(
                    onClick = { viewModel.saveScript(script.id, codeContent) },
                    modifier = Modifier.testTag("save_script_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Quick Snippets Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SnippetChip("+ update(dt)") { codeContent += "\nfunction update(dt)\n    \nend\n" }
            SnippetChip("+ apply_force") { codeContent += "\nphysics:apply_force(0, 10.0, 0)\n" }
            SnippetChip("+ apply_impulse") { codeContent += "\nphysics:apply_impulse(0, 8.0, 0)\n" }
            SnippetChip("+ rotate") { codeContent += "\nentity:rotate(0, 45.0 * dt, 0)\n" }
            SnippetChip("+ play_sound") { codeContent += "\nengine:play_sound(\"jump\")\n" }
            SnippetChip("+ on_collision") { codeContent += "\nfunction on_collision(other)\n    engine:log(\"Hit \" .. other)\nend\n" }
        }

        Spacer(Modifier.height(8.dp))

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
