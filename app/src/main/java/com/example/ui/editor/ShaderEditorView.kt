package com.example.ui.editor

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
fun ShaderEditorView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val currentShader by viewModel.editingShader.collectAsState()
    val allShaders = viewModel.assetManager.shaders.values.toList()

    val shader = currentShader ?: allShaders.firstOrNull() ?: return
    var vertexCode by remember(shader.id) { mutableStateOf(shader.vertexCode) }
    var fragmentCode by remember(shader.id) { mutableStateOf(shader.fragmentCode) }
    var activeTab by remember { mutableIntStateOf(1) } // 0: Vertex, 1: Fragment
    var shaderDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(
                    onClick = { shaderDropdownExpanded = true },
                    modifier = Modifier.testTag("select_shader_dropdown")
                ) {
                    Text(shader.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                DropdownMenu(
                    expanded = shaderDropdownExpanded,
                    onDismissRequest = { shaderDropdownExpanded = false }
                ) {
                    allShaders.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.name) },
                            onClick = {
                                viewModel.editShader(item)
                                shaderDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Row {
                OutlinedButton(
                    onClick = {
                        viewModel.askAiCopilot(
                            prompt = "Generate an optimized mobile custom GLSL shader with vertex displacement or unique fragment lighting for Vortex3D.",
                            taskType = "custom_shader"
                        )
                        viewModel.setWorkspaceTab(WorkspaceTab.AI_COPILOT)
                    },
                    modifier = Modifier.testTag("ai_shader_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI GLSL", fontSize = 11.sp, color = Color(0xFF00E5FF))
                }

                Spacer(Modifier.width(6.dp))

                Button(
                    onClick = {
                        viewModel.saveShader(
                            shader.copy(
                                vertexCode = vertexCode,
                                fragmentCode = fragmentCode
                            )
                        )
                    },
                    modifier = Modifier.testTag("save_shader_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Apply", fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color(0xFF161B22),
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Vertex Shader (GLSL)", fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Fragment Shader (GLSL)", fontSize = 12.sp) }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Editor
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(8.dp)
        ) {
            val currentCode = if (activeTab == 0) vertexCode else fragmentCode
            TextField(
                value = currentCode,
                onValueChange = {
                    if (activeTab == 0) vertexCode = it else fragmentCode = it
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("glsl_code_input"),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF7EE787),
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
