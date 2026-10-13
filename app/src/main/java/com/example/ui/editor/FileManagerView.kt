package com.example.ui.editor

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
            .padding(8.dp)
    ) {
        // Permission Request Banner if not granted
        if (!hasPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("storage_permission_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1C14)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Izin Penyimpanan Internal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFB300))
                        Text(
                            "Diperlukan untuk membaca file .glb, membuat folder, dan menyimpan proyek.",
                            fontSize = 11.sp,
                            color = Color(0xFFD7CCC8)
                        )
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
                        modifier = Modifier.testTag("grant_storage_button")
                    ) {
                        Text("Izinkan", fontSize = 11.sp)
                    }
                }
            }
        }

        // Header & Quick Bookmarks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "File Manager (ZArchiver Style)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF0F6FC)
            )

            Row {
                IconButton(onClick = { showCreateFolderDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Buat Folder", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { refreshFiles() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Muat Ulang", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Quick Bookmark Locations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StorageManager.getQuickLocations(context).forEach { (label, dir) ->
                SuggestionChip(
                    onClick = { currentDir = dir },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Breadcrumb Path Bar with Up Navigation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val parent = currentDir.parentFile
                        if (parent != null && parent.canRead()) {
                            currentDir = parent
                        }
                    },
                    enabled = currentDir.parentFile != null && currentDir.parentFile?.canRead() == true,
                    modifier = Modifier.size(28.dp).testTag("file_manager_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Naik ke Folder Induk",
                        tint = if (currentDir.parentFile != null) Color(0xFF00E5FF) else Color(0xFF6E7681),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                Text(
                    text = currentDir.absolutePath,
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari file di folder ini...", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("file_search_input")
        )

        Spacer(Modifier.height(6.dp))

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
    selectedFileForAction?.let { fileItem ->
        AlertDialog(
            onDismissRequest = { selectedFileForAction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (fileItem.extension) {
                            "glb" -> Icons.Default.ViewInAr
                            "lua" -> Icons.Default.Code
                            "glsl" -> Icons.Default.ColorLens
                            else -> Icons.Default.InsertDriveFile
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
                    Text("Ukuran: ${fileItem.sizeString}", fontSize = 11.sp, color = Color(0xFF8B949E))
                    Text("Dimodifikasi: ${fileItem.dateString}", fontSize = 11.sp, color = Color(0xFF8B949E))
                    Spacer(Modifier.height(8.dp))

                    when (fileItem.extension) {
                        "glb" -> {
                            Text("Model 3D glTF Binary (.glb). Mau di apakan file GLB ini?", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        "lua" -> {
                            Text("Script Game Lua. Buka di editor kode IDE Vortex3D?", fontSize = 12.sp)
                        }
                        else -> {
                            Text("Format file: ${fileItem.extension}", fontSize = 12.sp)
                        }
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
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    item.isDirectory -> Icons.Default.Folder
                    item.extension == "glb" -> Icons.Default.ViewInAr
                    item.extension == "lua" -> Icons.Default.Code
                    item.extension == "glsl" -> Icons.Default.ColorLens
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    item.isDirectory -> Color(0xFFFFB300)
                    item.extension == "glb" -> Color(0xFF00E5FF)
                    item.extension == "lua" -> Color(0xFF4FC3F7)
                    item.extension == "glsl" -> Color(0xFFFF4081)
                    else -> Color(0xFF8B949E)
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (item.isDirectory) Color(0xFFF0F6FC) else Color(0xFFC9D1D9),
                    maxLines = 1
                )
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
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
