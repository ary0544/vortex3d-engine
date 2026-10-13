package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun ConsoleLogView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onToggleExpand: (() -> Unit)? = null
) {
    val logs by viewModel.consoleLogs.collectAsState()
    val lastLog = logs.lastOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C)),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onToggleExpand != null) { onToggleExpand?.invoke() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Console", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF00E5FF))
                Spacer(Modifier.width(8.dp))

                if (!isExpanded && lastLog != null) {
                    Text(
                        text = "${lastLog.tag}: ${lastLog.message}",
                        fontSize = 10.sp,
                        color = Color(0xFF8B949E),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                    Text("${logs.size} logs", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Spacer(Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .testTag("console_logs_list"),
                    reverseLayout = true
                ) {
                itemsIndexed(logs.reversed(), key = { index, log -> "${log.id}_$index" }) { _, log ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
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
                        if (log.tag == "LuaError") {
                            Text(
                                text = "⚡ Perbaiki",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252),
                                modifier = Modifier
                                    .padding(start = 6.dp)
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
