package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EngineViewModel

@Composable
fun ConsoleLogView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val logs by viewModel.consoleLogs.collectAsState()
    val lastLog = logs.lastOrNull()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "LUA" -> logs.filter { it.tag.contains("Lua", ignoreCase = true) }
            "PHYSICS" -> logs.filter { it.tag.equals("Physics", ignoreCase = true) }
            "ERROR" -> logs.filter { it.tag.contains("Error", ignoreCase = true) }
            "ENGINE" -> logs.filter { it.tag.equals("Engine", ignoreCase = true) || it.tag.equals("Scene", ignoreCase = true) }
            else -> logs
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C)),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onToggleExpand != null) { onToggleExpand?.invoke() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Console & Action Log",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF00E5FF)
                )
                Spacer(Modifier.width(8.dp))

                if (!isExpanded && lastLog != null) {
                    Text(
                        text = "[${lastLog.tag}] ${lastLog.message}",
                        fontSize = 10.sp,
                        color = Color(0xFF8B949E),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = "(${filteredLogs.size}/${logs.size})",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))

                    // Salin Semua Tombol
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF21262D))
                            .clickable {
                                viewModel.copyToClipboard(
                                    context = context,
                                    text = viewModel.getFormattedLogs(),
                                    label = "Semua Log Konsol"
                                )
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("copy_all_logs_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin Semua",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Salin Semua",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Bersihkan Tombol
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF21262D))
                            .clickable { viewModel.clearConsoleLogs() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("clear_logs_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Bersihkan",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "Hapus",
                                fontSize = 9.5.sp,
                                color = Color(0xFF8B949E)
                            )
                        }
                    }
                }

                if (onToggleExpand != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF8B949E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                // Filter Tags Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filters = listOf(
                        "ALL" to "Semua",
                        "LUA" to "Lua Script",
                        "PHYSICS" to "Fisika",
                        "ENGINE" to "Aksi Engine",
                        "ERROR" to "Error"
                    )
                    filters.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF161B22))
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF8B949E)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .testTag("console_logs_list"),
                    reverseLayout = true
                ) {
                    itemsIndexed(filteredLogs.reversed(), key = { index, log -> "${log.id}_$index" }) { _, log ->
                        val lineText = "[${log.timestamp}] [${log.tag}] ${log.message}"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[${log.timestamp}]",
                                fontSize = 10.sp,
                                color = Color(0xFF6E7681),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "${log.tag}: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (log.tag) {
                                    "Lua" -> Color(0xFF58A6FF)
                                    "LuaError" -> Color(0xFFFF5252)
                                    "Physics" -> Color(0xFF00E676)
                                    "AI" -> Color(0xFFFFD54F)
                                    "Clipboard" -> Color(0xFF00E5FF)
                                    else -> Color(0xFF8B949E)
                                },
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = log.message,
                                fontSize = 10.sp,
                                color = Color(0xFFC9D1D9),
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            // Quick Copy single row button
                            IconButton(
                                onClick = {
                                    viewModel.copyToClipboard(
                                        context = context,
                                        text = lineText,
                                        label = "Baris Log"
                                    )
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Baris",
                                    tint = Color(0xFF6E7681),
                                    modifier = Modifier.size(10.dp)
                                )
                            }

                            if (log.tag == "LuaError") {
                                Text(
                                    text = "⚡ Perbaiki",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252),
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .clickable { viewModel.fixCurrentScriptError() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
