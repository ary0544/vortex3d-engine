package com.example.ui.editor

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import com.example.engine.model.GlbParser
import com.example.engine.model.GlbMetadata
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.storage.FileItem
import com.example.engine.storage.StorageManager
import com.example.viewmodel.EngineViewModel
import com.example.viewmodel.WorkspaceTab
import java.io.File

@Composable
fun FileManagerView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(StorageManager.hasStoragePermission(context)) }
    var currentDir by remember { mutableStateOf(StorageManager.getDefaultStartDirectory(context)) }
    var fileItems by remember { mutableStateOf(listOf<FileItem>()) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedFileForAction by remember { mutableStateOf<FileItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFullScreenPicker by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showLocationsMenu by remember { mutableStateOf(false) }

    // Android 7-10 permission launcher
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = StorageManager.hasStoragePermission(context)
    }

    // Android 11+ permission launcher
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = StorageManager.hasStoragePermission(context)
    }

    fun refreshFiles() {
        fileItems = StorageManager.listDirectory(currentDir)
    }

    LaunchedEffect(currentDir, hasPermission) {
        refreshFiles()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(6.dp)
    ) {
        // Compact Single-Row Toolbar (Conserves 80%+ vertical screen space)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back to parent folder
                IconButton(
                    onClick = {
                        val parent = currentDir.parentFile
                        if (parent != null && parent.canRead()) {
                            currentDir = parent
                        }
                    },
                    enabled = currentDir.parentFile != null && currentDir.parentFile?.canRead() == true,
                    modifier = Modifier.size(32.dp).testTag("file_manager_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Naik ke Folder Induk",
                        tint = if (currentDir.parentFile != null) Color(0xFF00E5FF) else Color(0xFF6E7681),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Current folder badge / quick jump button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showLocationsMenu = !showLocationsMenu }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = currentDir.name.ifEmpty { "Penyimpanan" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF0F6FC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Lokasi",
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showLocationsMenu,
                        onDismissRequest = { showLocationsMenu = false },
                        modifier = Modifier.background(Color(0xFF161B22))
                    ) {
                        StorageManager.getQuickLocations(context).forEach { (label, dir) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 11.sp, color = Color.White) },
                                onClick = {
                                    currentDir = dir
                                    showLocationsMenu = false
                                }
                            )
                        }
                    }
                }

                // Search & Filter Toggle Button
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.size(30.dp).testTag("toggle_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cari",
                        tint = if (isSearchExpanded || searchQuery.isNotEmpty()) Color(0xFF00E5FF) else Color(0xFF8B949E),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Create folder button
                IconButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier.size(30.dp).testTag("create_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Buat Folder",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Fullscreen Picker Button
                IconButton(
                    onClick = { showFullScreenPicker = true },
                    modifier = Modifier.size(30.dp).testTag("open_fullscreen_picker_banner")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Mode Layar Penuh",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Refresh Button
                IconButton(
                    onClick = { refreshFiles() },
                    modifier = Modifier.size(30.dp).testTag("refresh_files_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Muat Ulang",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Collapsible Search / Quick Path Bar
        AnimatedVisibility(visible = isSearchExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ketik nama file atau ekstensi (.glb)...", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF8B949E), modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("file_search_input")
                    )

                    // Path indicator
                    Text(
                        text = currentDir.absolutePath,
                        fontSize = 10.sp,
                        color = Color(0xFF6E7681),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                    )
                }
            }
        }

        // Compact Storage Permission Request Banner if not granted
        if (!hasPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .testTag("storage_permission_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1C14)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Izin Berkas Diperlukan", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFFB300))
                        Text("Untuk membaca .glb & simpan proyek.", fontSize = 10.sp, color = Color(0xFFD7CCC8))
                    }
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = StorageManager.getManageStorageIntent(context)
                                if (intent != null) manageStorageLauncher.launch(intent)
                            } else {
                                legacyPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("grant_storage_button"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Izinkan", fontSize = 10.sp)
                    }
                }
            }
        }

        // File & Directory List
        val filtered = remember(fileItems, searchQuery) {
            if (searchQuery.isBlank()) fileItems
            else fileItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("file_manager_list")
        ) {
            itemsIndexed(filtered, key = { index, item -> "${item.file.absolutePath}_$index" }) { _, item ->
                FileRow(
                    item = item,
                    onClick = {
                        if (item.isDirectory) {
                            currentDir = item.file
                        } else {
                            selectedFileForAction = item
                        }
                    }
                )
                Spacer(Modifier.height(2.dp))
            }
        }
    }

    // New Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Buat Folder Baru", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nama Folder") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            StorageManager.createFolder(currentDir, newFolderName)
                            newFolderName = ""
                            showCreateFolderDialog = false
                            refreshFiles()
                            viewModel.log("Storage", "Folder dibuat: $newFolderName")
                        }
                    }
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // File Action Sheet Dialog
    // File Action Sheet Dialog with GLB Inspection & Placement Recommendation
    selectedFileForAction?.let { fileItem ->
        val isGlb = fileItem.extension == "glb"
        val glbMeta = remember(fileItem) {
            if (isGlb) GlbParser.inspectFile(fileItem.file) else null
        }

        AlertDialog(
            onDismissRequest = { selectedFileForAction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (fileItem.extension) {
                            "glb" -> Icons.Default.ViewInAr
                            "lua" -> Icons.Default.Code
                            "glsl" -> Icons.Default.ColorLens
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = when (fileItem.extension) {
                            "glb" -> Color(0xFF00E5FF)
                            "lua" -> Color(0xFF4FC3F7)
                            "glsl" -> Color(0xFFFF4081)
                            else -> Color(0xFF8B949E)
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(fileItem.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Ukuran: ${fileItem.sizeString} • Dimodifikasi: ${fileItem.dateString}", fontSize = 10.5.sp, color = Color(0xFF8B949E))
                    Spacer(Modifier.height(8.dp))

                    if (isGlb && glbMeta != null) {
                        // 1. Spesifikasi Geometri GLB
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Spesifikasi Model GLB:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF00E5FF))
                                Spacer(Modifier.height(3.dp))
                                Text("• Model: ${glbMeta.modelName}", fontSize = 10.5.sp, color = Color.White)
                                Text("• Geometri: ${glbMeta.meshCount} Mesh | ${glbMeta.vertexCount} Verts | ${glbMeta.triangleCount} Tris", fontSize = 10.5.sp, color = Color.White)
                                Text("• Shading PBR: Roughness ${String.format(java.util.Locale.US, "%.2f", glbMeta.roughness)} | Metallic ${String.format(java.util.Locale.US, "%.2f", glbMeta.metallic)}", fontSize = 10.5.sp, color = Color(0xFFC9D1D9))
                                Text("• Aset: ${glbMeta.textureCount} Tekstur, ${glbMeta.animationCount} Animasi", fontSize = 10.5.sp, color = Color(0xFF8B949E))
                            }
                        }

                        // 2. Rekomendasi Penempatan Folder
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2538)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("📌 Rekomendasi Penempatan:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF00E676))
                                Spacer(Modifier.height(3.dp))
                                Text("Peran: ${glbMeta.recommendedRole}", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.White)
                                Spacer(Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF161B22))
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = glbMeta.recommendedFolder,
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF58A6FF),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF238636))
                                            .clickable {
                                                viewModel.copyToClipboard(
                                                    context = context,
                                                    text = glbMeta.recommendedFolder,
                                                    label = "Path Folder Rekomendasi"
                                                )
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text("Salin Path", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = glbMeta.recommendationTip,
                                    fontSize = 10.sp,
                                    color = Color(0xFFC9D1D9),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Tombol Salin Info Lengkap untuk AI
                        OutlinedButton(
                            onClick = {
                                val fullInfo = buildString {
                                    appendLine("=== INFORMASI MODEL 3D GLB ===")
                                    appendLine("File: ${glbMeta.fileName} (${glbMeta.fileSizeFormatted})")
                                    appendLine("Model: ${glbMeta.modelName}")
                                    appendLine("Meshes: ${glbMeta.meshCount}, Vertices: ${glbMeta.vertexCount}, Triangles: ${glbMeta.triangleCount}")
                                    appendLine("PBR: Roughness ${glbMeta.roughness}, Metallic ${glbMeta.metallic}, BaseColor ${glbMeta.baseColorHex}")
                                    appendLine("Tekstur: ${glbMeta.textureCount}, Animasi: ${glbMeta.animationCount}")
                                    appendLine("Rekomendasi Peran: ${glbMeta.recommendedRole}")
                                    appendLine("Folder Penempatan: ${glbMeta.recommendedFolder}")
                                    appendLine("Petunjuk: ${glbMeta.recommendationTip}")
                                }
                                viewModel.copyToClipboard(context, fullInfo, "Spesifikasi Lengkap GLB")
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.fillMaxWidth().height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF00E5FF))
                            Spacer(Modifier.width(4.dp))
                            Text("Salin Info Lengkap (untuk AI)", fontSize = 10.sp, color = Color(0xFF00E5FF))
                        }
                    } else if (fileItem.extension == "lua") {
                        Text("Script Game Lua. Buka di editor kode IDE Vortex3D?", fontSize = 12.sp)
                    } else {
                        Text("Format file: ${fileItem.extension}", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                when (fileItem.extension) {
                    "glb" -> {
                        Button(
                            onClick = {
                                viewModel.importAndSpawnGlbFile(fileItem.file)
                                selectedFileForAction = null
                                viewModel.setWorkspaceTab(WorkspaceTab.SCENE_VIEW)
                            },
                            modifier = Modifier.testTag("spawn_glb_dialog_button")
                        ) {
                            Text("Spawn ke Scene 3D", fontSize = 11.sp)
                        }
                    }
                    "lua" -> {
                        Button(
                            onClick = {
                                viewModel.importLuaFile(fileItem.file)
                                selectedFileForAction = null
                                viewModel.setWorkspaceTab(WorkspaceTab.LUA_EDITOR)
                            }
                        ) {
                            Text("Buka di Lua IDE", fontSize = 11.sp)
                        }
                    }
                    else -> {
                        Button(onClick = { selectedFileForAction = null }) {
                            Text("Tutup", fontSize = 11.sp)
                        }
                    }
                }
            },
            dismissButton = {
                if (fileItem.extension == "glb") {
                    OutlinedButton(
                        onClick = {
                            viewModel.importGlbToAssets(fileItem.file)
                            selectedFileForAction = null
                        }
                    ) {
                        Text("Simpan ke Assets Saja", fontSize = 11.sp)
                    }
                } else {
                    OutlinedButton(onClick = { selectedFileForAction = null }) {
                        Text("Batal", fontSize = 11.sp)
                    }
                }
            }
        )
    }

    // Full Screen File Picker Dialog Modal
    if (showFullScreenPicker) {
        FullScreenFilePickerDialog(
            viewModel = viewModel,
            initialDirectory = currentDir,
            onDismiss = {
                showFullScreenPicker = false
                refreshFiles()
            }
        )
    }
}

@Composable
private fun FileRow(
    item: FileItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("file_item_${item.name}"),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDirectory) Color(0xFF161B22) else Color(0xFF0F141C)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (item.isDirectory) Color(0xFF263040) else Color(0xFF1B2230)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    item.isDirectory -> Icons.Default.Folder
                    item.extension == "glb" -> Icons.Default.ViewInAr
                    item.extension == "lua" -> Icons.Default.Code
                    item.extension == "glsl" -> Icons.Default.ColorLens
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    item.isDirectory -> Color(0xFFFFB300)
                    item.extension == "glb" -> Color(0xFF00E5FF)
                    item.extension == "lua" -> Color(0xFF4FC3F7)
                    item.extension == "glsl" -> Color(0xFFFF4081)
                    else -> Color(0xFF8B949E)
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (item.isDirectory) Color(0xFFF0F6FC) else Color(0xFFC9D1D9),
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.sizeString} • ${item.dateString}",
                    fontSize = 10.sp,
                    color = Color(0xFF6E7681)
                )
            }

            if (item.extension == "glb") {
                Text(
                    text = "3D GLB",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            } else if (item.isDirectory) {
                Text(
                    text = "Buka",
                    fontSize = 10.sp,
                    color = Color(0xFFFFB300),
                    modifier = Modifier
                        .background(Color(0xFFFFB300).copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
