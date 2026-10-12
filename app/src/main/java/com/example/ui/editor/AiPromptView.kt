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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.viewmodel.WorkspaceTab

@Composable
fun AiPromptView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val latestError by viewModel.latestScriptError.collectAsState()
    val currentScript by viewModel.editingScript.collectAsState()

    var promptInput by remember { mutableStateOf("") }
    var isThinkingExpanded by remember { mutableStateOf(true) }
    var applySuccessMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "AI Script Assistant & Error Fixer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF0F6FC)
                )
                Text(
                    text = "Perintah Pembuatan Script & Perbaikan Otomatis • High Thinking Mode",
                    fontSize = 11.sp,
                    color = Color(0xFF00E5FF)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 1-Tap Error Fix Banner (if error exists)
        if (latestError != null) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("error_fix_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B181E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Terdeteksi Error pada Script:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFF5252))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = latestError ?: "",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFCDD2)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.fixCurrentScriptError()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth().testTag("one_tap_fix_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("⚡ 1-Tap Perbaiki Error dengan AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Tata Cara & Panduan Perintah Prompt
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Tata Cara Perintah untuk AI:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFFFFB300)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "• Tulis tujuan script secara spesifik (misal: kontrol karakter, fisika partikel, rotasi pickup).\n" +
                            "• Sebutkan hook yang diinginkan: init(), update(dt), on_collision(other).\n" +
                            "• AI akan menyusun script Lua lengkap siap pakai tanpa kode terpotong.",
                    fontSize = 11.sp,
                    color = Color(0xFFC9D1D9),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Quick Command Prompts
        Text("Pilihan Perintah Cepat:", fontSize = 11.sp, color = Color(0xFF8B949E))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SuggestionChip(
                onClick = {
                    promptInput = "Buatkan script lompat ganda (double jump), coyote time, dan gaya dorong fisika."
                    viewModel.askAiCopilot(promptInput, "generate_lua")
                },
                label = { Text("Lompat Ganda Lua", fontSize = 11.sp) }
            )
            SuggestionChip(
                onClick = {
                    promptInput = "Buatkan script koin berputar yang bersuara saat ditabrak dan menambah skor."
                    viewModel.askAiCopilot(promptInput, "generate_lua")
                },
                label = { Text("Koin Pickup SFX", fontSize = 11.sp) }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Prompt Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Ketik perintah script untuk AI (misal: buatkan controller mobil fisika)...", fontSize = 11.sp) },
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
                        viewModel.askAiCopilot(promptInput, "generate_lua")
                    }
                },
                enabled = !isAiLoading && promptInput.isNotBlank(),
                modifier = Modifier.testTag("send_ai_prompt_button")
            ) {
                if (isAiLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Loading Indicator
        if (isAiLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF00E5FF))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Gemini 3.1 Pro Preview sedang Berpikir (High Reasoning)...", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF00E5FF))
                        Text("Menganalisis sintaksis, hook engine, dan kalkulasi fisika.", fontSize = 11.sp, color = Color(0xFF8B949E))
                    }
                }
            }
        }

        // Response View
        val response = aiResponse
        if (response != null) {
            // Collapsible Thinking Box
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
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Proses Berpikir AI (High Thinking)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
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
                Spacer(Modifier.height(8.dp))
            }

            // Generated / Fixed Code Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kode Hasil AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF00E676)
                        )

                        if (!response.extractedCode.isNullOrBlank()) {
                            Button(
                                onClick = {
                                    val codeToApply = response.extractedCode
                                    val scriptToUpdate = currentScript ?: viewModel.assetManager.scripts.values.firstOrNull()
                                    if (scriptToUpdate != null) {
                                        viewModel.saveScript(scriptToUpdate.id, codeToApply)
                                        viewModel.clearScriptError()
                                        applySuccessMessage = "Berhasil diterapkan ke ${scriptToUpdate.name}!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                modifier = Modifier.testTag("apply_code_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(Modifier.width(4.dp))
                                Text("Terapkan ke Script", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (applySuccessMessage != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(applySuccessMessage ?: "", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = response.answerContent,
                        fontSize = 12.sp,
                        fontFamily = if (response.answerContent.contains("function")) FontFamily.Monospace else FontFamily.Default,
                        color = Color(0xFFE6EDF3),
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
