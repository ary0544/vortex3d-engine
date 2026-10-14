package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.editor.AiPromptView
import com.example.ui.editor.AssetBrowserView
import com.example.ui.editor.ConsoleLogView
import com.example.ui.editor.DocumentationView
import com.example.ui.editor.FileManagerView
import com.example.ui.editor.HierarchyView
import com.example.ui.editor.InspectorView
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Home
import com.example.ui.editor.ProjectHubScreen
import com.example.ui.editor.LandscapeStudioScreen
import com.example.ui.editor.GlbImportPreviewDialog
import com.example.ui.editor.LuaEditorView
import com.example.ui.editor.PlayModeOverlay
import com.example.ui.editor.ShaderEditorView
import com.example.ui.editor.Viewport3D
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EngineViewModel
import com.example.viewmodel.WorkspaceTab

import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import com.example.viewmodel.ProjectOrientation

class MainActivity : ComponentActivity() {

    private fun hideSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            MyApplicationTheme {
                val viewModel: EngineViewModel = viewModel()
                val isPlayMode by viewModel.isPlayMode.collectAsState()
                val isInLobby by viewModel.isInLobby.collectAsState()
                val activeTab by viewModel.workspaceTab.collectAsState()
                val projects by viewModel.projects.collectAsState()
                val activeProjectId by viewModel.activeProjectId.collectAsState()

                val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

                // Immersive System Bars Auto-Hide immediately when keyboard closes
                LaunchedEffect(imeBottom) {
                    if (imeBottom == 0) {
                        hideSystemBars()
                    }
                }

                val inspectingGlbResult by viewModel.inspectingGlbResult.collectAsState()

                BackHandler(enabled = inspectingGlbResult != null || isPlayMode || !isInLobby) {
                    if (inspectingGlbResult != null) {
                        viewModel.closeGlbInspection()
                    } else if (isPlayMode) {
                        viewModel.setPlayMode(false)
                    } else if (!isInLobby) {
                        viewModel.setInLobby(true)
                    }
                }

                if (isPlayMode) {
                    PlayModeOverlay(viewModel = viewModel)
                } else if (isInLobby) {
                    ProjectHubScreen(viewModel = viewModel)
                } else {
                    LandscapeStudioScreen(viewModel = viewModel)
                }

                // Global GLB Import & Preview Dialog
                inspectingGlbResult?.let { inspection ->
                    GlbImportPreviewDialog(
                        inspection = inspection,
                        viewModel = viewModel,
                        onDismiss = { viewModel.closeGlbInspection() }
                    )
                }
            }
        }
    }
}

data class NavTabItem(
    val tab: WorkspaceTab,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudioScreen(
    viewModel: EngineViewModel
) {
    val activeTab by viewModel.workspaceTab.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val isWireframe by viewModel.isWireframeMode.collectAsState()
    val scene by viewModel.scene.collectAsState()
    var isConsoleExpanded by remember { mutableStateOf(false) }

    val tabs = listOf(
        NavTabItem(WorkspaceTab.SCENE_VIEW, "3D Viewport", Icons.Default.ViewInAr),
        NavTabItem(WorkspaceTab.HIERARCHY, "Hierarchy", Icons.Default.Layers),
        NavTabItem(WorkspaceTab.INSPECTOR, "Inspector", Icons.Default.Tune),
        NavTabItem(WorkspaceTab.ASSETS, "Assets", Icons.Default.Folder),
        NavTabItem(WorkspaceTab.FILE_MANAGER, "File Manager", Icons.Default.FolderOpen),
        NavTabItem(WorkspaceTab.LUA_EDITOR, "Lua IDE", Icons.Default.Code),
        NavTabItem(WorkspaceTab.SHADER_EDITOR, "Shaders", Icons.Default.Palette),
        NavTabItem(WorkspaceTab.AI_COPILOT, "AI Script & Fix", Icons.Default.Build),
        NavTabItem(WorkspaceTab.DOCS_TUTORIALS, "Docs & Guides", Icons.Default.Book)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Godot 4-style Hub Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF478CBF))
                                .clickable { viewModel.setInLobby(true) }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("portrait_lobby_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Project Hub",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "Hub",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "${stringResource(R.string.app_name)} • ${scene.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFF0F6FC),
                                maxLines = 1
                            )
                            Text(
                                text = "Godot 4 Mobile • 3D Lua Engine",
                                fontSize = 10.sp,
                                color = Color(0xFF8B949E)
                            )
                        }
                    }
                },
                actions = {
                    // Play Simulation Toggle
                    IconButton(
                        onClick = { viewModel.toggleSimulation() },
                        modifier = Modifier.size(32.dp).testTag("appbar_sim_button")
                    ) {
                        Icon(
                            imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Simulation",
                            tint = if (isSimulating) Color(0xFFFFB300) else Color(0xFF00E676),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Reset Simulation
                    IconButton(
                        onClick = { viewModel.resetSimulation() },
                        modifier = Modifier.size(32.dp).testTag("appbar_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Wireframe Toggle
                    IconButton(
                        onClick = { viewModel.toggleWireframe() },
                        modifier = Modifier.size(32.dp).testTag("appbar_wireframe_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Wireframe",
                            tint = if (isWireframe) Color(0xFF00E5FF) else Color(0xFF8B949E),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Fullscreen Game Play Mode Button
                    IconButton(
                        onClick = { viewModel.setPlayMode(true) },
                        modifier = Modifier.size(32.dp).testTag("appbar_play_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Play Game",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0D1117))
        ) {
            // Workspace Navigation Tabs (Sleek 34dp)
            val selectedIndex = tabs.indexOfFirst { it.tab == activeTab }.coerceAtLeast(0)
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                edgePadding = 6.dp,
                containerColor = Color(0xFF161B22),
                contentColor = Color(0xFF00E5FF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("workspace_tab_row")
            ) {
                tabs.forEachIndexed { index, item ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { viewModel.setWorkspaceTab(item.tab) },
                        modifier = Modifier.testTag("tab_${item.tab.name.lowercase()}"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selectedIndex == index) Color(0xFF00E5FF) else Color(0xFF8B949E)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedIndex == index) Color(0xFF00E5FF) else Color(0xFF8B949E)
                                )
                            }
                        }
                    )
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    WorkspaceTab.SCENE_VIEW -> Viewport3D(viewModel = viewModel)
                    WorkspaceTab.HIERARCHY -> HierarchyView(viewModel = viewModel)
                    WorkspaceTab.INSPECTOR -> InspectorView(viewModel = viewModel)
                    WorkspaceTab.ASSETS -> AssetBrowserView(viewModel = viewModel)
                    WorkspaceTab.FILE_MANAGER -> FileManagerView(viewModel = viewModel)
                    WorkspaceTab.LUA_EDITOR -> LuaEditorView(viewModel = viewModel)
                    WorkspaceTab.SHADER_EDITOR -> ShaderEditorView(viewModel = viewModel)
                    WorkspaceTab.AI_COPILOT -> AiPromptView(viewModel = viewModel)
                    WorkspaceTab.DOCS_TUTORIALS -> DocumentationView(viewModel = viewModel)
                }
            }

            // Bottom Console Drawer (Collapsible)
            ConsoleLogView(
                viewModel = viewModel,
                isExpanded = isConsoleExpanded,
                onToggleExpand = { isConsoleExpanded = !isConsoleExpanded }
            )
        }
    }
}
