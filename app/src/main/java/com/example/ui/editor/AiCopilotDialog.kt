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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EngineViewModel

@Composable
fun AiCopilotDialog(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var promptInput by remember { mutableStateOf("") }
    var isThinkingExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "High-Thinking Engine AI Director",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Powered by gemini-3.1-pro-preview • High Reasoning Mode",
                    fontSize = 11.sp,
                    color = Color(0xFF00E5FF)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Quick prompts
        Text("Quick Prompts:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SuggestionChip(
                onClick = {
                    promptInput = "Create an advanced Lua script for double-jumping, coyote time, and wall bouncing."
                    viewModel.askAiCopilot(promptInput, "lua_script")
                },
                label = { Text("Jump Physics Lua", fontSize = 11.sp) }
            )
            SuggestionChip(
                onClick = {
                    promptInput = "Generate a custom GLSL sci-fi forcefield shader with glowing hexagonal scanlines and fresnel edges."
                    viewModel.askAiCopilot(promptInput, "custom_shader")
                },
                label = { Text("Forcefield GLSL", fontSize = 11.sp) }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Ask anything about 3D physics, Lua, or custom shaders...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input"),
                singleLine = false,
                maxLines = 3
            )

            Spacer(Modifier.width(8.dp))

            Button(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        viewModel.askAiCopilot(promptInput, "general")
                    }
                },
                enabled = !isAiLoading && promptInput.isNotBlank(),
                modifier = Modifier.testTag("send_ai_prompt_button")
            ) {
                if (isAiLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Loading state
        if (isAiLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Gemini 3.1 Pro Preview is Thinking...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Performing deep mathematical and architectural reasoning with high thinking level.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Response View
        val response = aiResponse
        if (response != null) {
            // Thinking Box (if present)
            if (!response.thinkingContent.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Thinking Process (High Reasoning)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                            }
                            IconButton(onClick = { isThinkingExpanded = !isThinkingExpanded }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (isThinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }

                        if (isThinkingExpanded) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = response.thinkingContent,
                                fontSize = 11.sp,
                                color = Color(0xFFE0E0E0),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Final Output Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Technical Director Solution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = response.answerContent,
                        fontSize = 12.sp,
                        fontFamily = if (response.answerContent.contains("```")) FontFamily.Monospace else FontFamily.Default,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
