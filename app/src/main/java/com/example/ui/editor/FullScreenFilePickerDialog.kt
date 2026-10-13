package com.example.ui.editor

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.storage.FileItem
import com.example.engine.storage.StorageManager
import com.example.viewmodel.EngineViewModel
import com.example.viewmodel.WorkspaceTab
import java.io.File

enum class FileCategoryFilter(val label: String, val extensions: List<String>) {
    ALL("Semua", emptyList()),
    MODELS_3D("Model 3D (.glb)", listOf("glb", "gltf", "obj")),
    SCRIPTS("Script (.lua)", listOf("lua", "kt")),
    SHADERS("Shader (.glsl)", listOf("glsl", "frag", "vert")),
    TEXTURES("Gambar (.png, .jpg)", listOf("png", "jpg", "jpeg", "webp")),
    FOLDERS("Folder Saja", emptyList())
}

@Composable
fun FullScreenFilePickerDialog(
    viewModel: EngineViewModel,
    initialDirectory: File? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(StorageManager.hasStoragePermission(context)) }
    var currentDir by remember {
        mutableStateOf(initialDirectory ?: StorageManager.getDefaultStartDirectory(context))
    }
    var fileItems by remember { mutableStateOf(listOf<FileItem>()) }
    var selectedItem by remember { mutableStateOf<FileItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FileCategoryFilter.ALL) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var showLocationsDropdown by remember { mutableStateOf(false) }

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
        selectedItem = null
    }

    // Filter files based on search query and category
    val filteredFiles = remember(fileItems, searchQuery, selectedCategory) {
        fileItems.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.name.contains(searchQuery, ignoreCase = true)
            }
            val matchesCategory = when (selectedCategory) {
                FileCategoryFilter.ALL -> true
                FileCategoryFilter.FOLDERS -> item.isDirectory
                else -> {
                    if (item.isDirectory) true
                    else selectedCategory.extensions.contains(item.extension.lowercase())
                }
            }
            matchesSearch && matchesCategory
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("fullscreen_file_picker_dialog"),
            color = Color(0xFF0A0E14)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Compact Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131822))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_file_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup File Manager",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Up to parent directory
                    IconButton(
                        onClick = {
                            val parent = currentDir.parentFile
                            if (parent != null && parent.canRead()) {
                                currentDir = parent
                            }
                        },
                        enabled = currentDir.parentFile != null && currentDir.parentFile?.canRead() == true,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("file_picker_up_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Folder Induk",
                            tint = if (currentDir.parentFile != null) Color(0xFF00E5FF) else Color(0xFF6E7681),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Folder title & quick location jumper
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showLocationsDropdown = !showLocationsDropdown }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
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
                                text = currentDir.name.ifEmpty { "Penyimpanan Utama" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF0F6FC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "(${filteredFiles.size})",
                                fontSize = 10.sp,
                                color = Color(0xFF00E5FF)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Pilih Lokasi",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showLocationsDropdown,
                            onDismissRequest = { showLocationsDropdown = false },
                            modifier = Modifier.background(Color(0xFF161B22))
                        ) {
                            StorageManager.getQuickLocations(context).forEach { (label, dir) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 11.sp, color = Color.White) },
                                    onClick = {
                                        currentDir = dir
                                        showLocationsDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Search & Category Filter Toggle Button
                    IconButton(
                        onClick = { isFilterExpanded = !isFilterExpanded },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("toggle_file_picker_filter")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari & Filter",
                            tint = if (isFilterExpanded || searchQuery.isNotEmpty() || selectedCategory != FileCategoryFilter.ALL) Color(0xFF00E5FF) else Color(0xFF8B949E),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Create Folder Button
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("create_folder_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Buat Folder Baru",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { refreshFiles() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("refresh_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat Ulang Berkas",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF21262D), thickness = 1.dp)

                // Collapsible Search & Filter Area
                AnimatedVisibility(visible = isFilterExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF101520))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari file atau folder...", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color(0xFF8B949E), modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF30363D),
                                focusedContainerColor = Color(0xFF161B22),
                                unfocusedContainerColor = Color(0xFF161B22),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFC9D1D9)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("file_picker_search_input")
                        )

                        Spacer(Modifier.height(4.dp))

                        // Filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FileCategoryFilter.entries.forEach { cat ->
                                val selected = selectedCategory == cat
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.label, fontSize = 10.sp) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(10.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFF00E5FF),
                                        containerColor = Color(0xFF161D28),
                                        labelColor = Color(0xFF8B949E)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (selected) Color(0xFF00E5FF) else Color(0xFF30363D),
                                        enabled = true,
                                        selected = selected
                                    )
                                )
                            }
                        }
                    }
                }

                // Storage Permission Warning Banner (compact)
                if (!hasPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1808)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Akses Berkas Diperlukan", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFFFB300))
                                Text("Berikan izin agar dapat memilih model 3D .glb.", fontSize = 10.sp, color = Color(0xFFE0E0E0))
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Izinkan", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Wide, comfortable file list area (Takes remaining height)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    if (filteredFiles.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color(0xFF30363D),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Folder ini kosong atau tidak ada berkas yang cocok", color = Color(0xFF8B949E), fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("fullscreen_file_list"),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            itemsIndexed(
                                items = filteredFiles,
                                key = { index, item -> "${item.file.absolutePath}_$index" }
                            ) { _, item ->
                                val isSelected = selectedItem?.file?.absolutePath == item.file.absolutePath

                                FullScreenFileCard(
                                    item = item,
                                    isSelected = isSelected,
                                    onSelect = {
                                        if (item.isDirectory) {
                                            currentDir = item.file
                                        } else {
                                            selectedItem = item
                                        }
                                    },
                                    onQuickAction = {
                                        if (item.extension.lowercase() == "glb") {
                                            viewModel.importAndSpawnGlbFile(item.file)
                                            viewModel.setWorkspaceTab(WorkspaceTab.SCENE_VIEW)
                                            onDismiss()
                                        } else if (item.extension.lowercase() == "lua") {
                                            viewModel.importLuaFile(item.file)
                                            viewModel.setWorkspaceTab(WorkspaceTab.LUA_EDITOR)
                                            onDismiss()
                                        } else if (item.isDirectory) {
                                            currentDir = item.file
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Floating Action Confirmation Sheet when a file is selected
                    selectedItem?.let { fileItem ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .testTag("file_action_bottom_sheet"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getFileIcon(fileItem),
                                    contentDescription = null,
                                    tint = getFileIconColor(fileItem),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileItem.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${fileItem.sizeString} • ${fileItem.extension.uppercase()}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8B949E)
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                if (fileItem.extension.lowercase() == "glb") {
                                    Button(
                                        onClick = {
                                            viewModel.importAndSpawnGlbFile(fileItem.file)
                                            viewModel.setWorkspaceTab(WorkspaceTab.SCENE_VIEW)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("action_spawn_3d_btn")
                                    ) {
                                        Text("Spawn ke 3D", color = Color(0xFF0A0E14), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                } else if (fileItem.extension.lowercase() == "lua") {
                                    Button(
                                        onClick = {
                                            viewModel.importLuaFile(fileItem.file)
                                            viewModel.setWorkspaceTab(WorkspaceTab.LUA_EDITOR)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("Buka di Script IDE", color = Color(0xFF0A0E14), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { selectedItem = null },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("Tutup", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create New Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Buat Folder Baru", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nama Folder") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        focusedLabelColor = Color(0xFF00E5FF)
                    )
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Buat", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun FullScreenFileCard(
    item: FileItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onQuickAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable { onSelect() }
            .testTag("fs_file_card_${item.name}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1F2B3E) else if (item.isDirectory) Color(0xFF141922) else Color(0xFF0F141D)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF21262D)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Large Icon container
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getFileIconColor(item).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileIcon(item),
                    contentDescription = null,
                    tint = getFileIconColor(item),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // File Information (Wide, easy to read)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF00E5FF) else if (item.isDirectory) Color(0xFFF0F6FC) else Color(0xFFC9D1D9),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.sizeString,
                        fontSize = 11.sp,
                        color = Color(0xFF8B949E)
                    )
                    Text(" • ", fontSize = 11.sp, color = Color(0xFF484F58))
                    Text(
                        text = item.dateString,
                        fontSize = 11.sp,
                        color = Color(0xFF6E7681)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Action Badge or Button
            if (item.isDirectory) {
                Text(
                    text = "Buka",
                    fontSize = 11.sp,
                    color = Color(0xFFFFB300),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            } else if (item.extension.lowercase() == "glb") {
                Button(
                    onClick = onQuickAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Spawn 3D", color = Color(0xFF0A0E14), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else if (item.extension.lowercase() == "lua") {
                Button(
                    onClick = onQuickAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Editor", color = Color(0xFF0A0E14), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else {
                Text(
                    text = item.extension.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B949E),
                    modifier = Modifier
                        .background(Color(0xFF21262D), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private fun getFileIcon(item: FileItem): ImageVector {
    return when {
        item.isDirectory -> Icons.Default.Folder
        item.extension.lowercase() in listOf("glb", "gltf", "obj") -> Icons.Default.ViewInAr
        item.extension.lowercase() in listOf("lua", "kt") -> Icons.Default.Code
        item.extension.lowercase() in listOf("glsl", "frag", "vert") -> Icons.Default.ColorLens
        item.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") -> Icons.Default.Image
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun getFileIconColor(item: FileItem): Color {
    return when {
        item.isDirectory -> Color(0xFFFFB300)
        item.extension.lowercase() in listOf("glb", "gltf", "obj") -> Color(0xFF00E5FF)
        item.extension.lowercase() in listOf("lua", "kt") -> Color(0xFF4FC3F7)
        item.extension.lowercase() in listOf("glsl", "frag", "vert") -> Color(0xFFFF4081)
        item.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") -> Color(0xFF69F0AE)
        else -> Color(0xFF8B949E)
    }
}
