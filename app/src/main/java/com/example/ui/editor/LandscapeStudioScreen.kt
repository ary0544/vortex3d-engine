package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.EngineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandscapeStudioScreen(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val isSimulating by viewModel.isSimulating.collectAsState()
    val isWireframe by viewModel.isWireframeMode.collectAsState()
    val scene by viewModel.scene.collectAsState()
    val isLeftDockOpen by viewModel.isLeftDockOpen.collectAsState()
    val isRightDockOpen by viewModel.isRightDockOpen.collectAsState()
    val isBottomDockOpen by viewModel.isBottomDockOpen.collectAsState()

    var selectedLeftTab by remember { mutableIntStateOf(0) } // 0: Scene Tree, 1: Assets, 2: Files
    var selectedRightTab by remember { mutableIntStateOf(0) } // 0: Inspector, 1: Lua, 2: Shaders, 3: AI Fix, 4: Docs

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
    ) {
        // Godot 4 Mobile Compact Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lobby / Project Hub Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF478CBF))
                            .clickable { viewModel.setInLobby(true) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("landscape_lobby_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Project Hub",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Hub",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Active Scene Name Badge & Save
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF161B22))
                            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(6.dp))
                            .clickable { viewModel.saveCurrentScene() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = scene.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF0F6FC),
                            maxLines = 1
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Scene",
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Dock Toggles Toolbar
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF161B22))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Dock Toggle
                        IconButton(
                            onClick = { viewModel.toggleLeftDock() },
                            modifier = Modifier.size(28.dp).testTag("toggle_left_dock")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Toggle Left Dock",
                                tint = if (isLeftDockOpen) Color(0xFF00E5FF) else Color(0xFF6E7681),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Bottom Dock Toggle
                        IconButton(
                            onClick = { viewModel.toggleBottomDock() },
                            modifier = Modifier.size(28.dp).testTag("toggle_bottom_dock")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Toggle Console Dock",
                                tint = if (isBottomDockOpen) Color(0xFF00E5FF) else Color(0xFF6E7681),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Right Dock Toggle
                        IconButton(
                            onClick = { viewModel.toggleRightDock() },
                            modifier = Modifier.size(28.dp).testTag("toggle_right_dock")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Toggle Inspector Dock",
                                tint = if (isRightDockOpen) Color(0xFF00E5FF) else Color(0xFF6E7681),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            actions = {
                // Simulation Run/Pause
                IconButton(
                    onClick = { viewModel.toggleSimulation() },
                    modifier = Modifier.size(32.dp).testTag("landscape_sim_button")
                ) {
                    Icon(
                        imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Simulation",
                        tint = if (isSimulating) Color(0xFFFFB300) else Color(0xFF00E676),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Reset Simulation
                IconButton(
                    onClick = { viewModel.resetSimulation() },
                    modifier = Modifier.size(32.dp).testTag("landscape_reset_button")
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
                    modifier = Modifier.size(32.dp).testTag("landscape_wireframe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Wireframe",
                        tint = if (isWireframe) Color(0xFF00E5FF) else Color(0xFF8B949E),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Fullscreen Play Game Mode
                IconButton(
                    onClick = { viewModel.setPlayMode(true) },
                    modifier = Modifier.size(32.dp).testTag("landscape_play_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Full Game",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1117))
        )

        // Main Studio Workspace Row (Left Collapsible Dock + Center 3D Viewport + Right Collapsible Dock)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 1. LEFT COLLAPSIBLE DOCK: Scene Tree & Assets
            AnimatedVisibility(
                visible = isLeftDockOpen,
                enter = expandHorizontally(animationSpec = tween(180)) + fadeIn(),
                exit = shrinkHorizontally(animationSpec = tween(180)) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .width(225.dp)
                        .fillMaxHeight()
                        .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Left Dock Header Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E2633))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val leftTabs = listOf(
                                Icons.Default.Layers to "Scene",
                                Icons.Default.Folder to "Assets",
                                Icons.Default.FolderOpen to "Files"
                            )
                            leftTabs.forEachIndexed { idx, (icon, title) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (selectedLeftTab == idx) Color(0xFF2D3748) else Color.Transparent)
                                        .clickable { selectedLeftTab = idx }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = if (selectedLeftTab == idx) Color(0xFF00E5FF) else Color(0xFF8B949E),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = title,
                                            fontSize = 10.sp,
                                            fontWeight = if (selectedLeftTab == idx) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedLeftTab == idx) Color(0xFF00E5FF) else Color(0xFF8B949E)
                                        )
                                    }
                                }
                            }

                            // Collapse handle
                            IconButton(
                                onClick = { viewModel.toggleLeftDock() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Collapse Left",
                                    tint = Color(0xFF8B949E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Left Dock Body
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (selectedLeftTab) {
                                0 -> HierarchyView(viewModel = viewModel)
                                1 -> AssetBrowserView(viewModel = viewModel)
                                2 -> FileManagerView(viewModel = viewModel)
                            }
                        }
                    }
                }
            }

            // Left Edge Handle if collapsed
            if (!isLeftDockOpen) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF161B22).copy(alpha = 0.6f))
                        .clickable { viewModel.toggleLeftDock() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Expand Left Dock",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(2.dp))

            // 2. CENTER PANE: Interactive 3D Viewport
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(top = 2.dp, bottom = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Viewport3D(viewModel = viewModel)
            }

            Spacer(Modifier.width(2.dp))

            // Right Edge Handle if collapsed
            if (!isRightDockOpen) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF161B22).copy(alpha = 0.6f))
                        .clickable { viewModel.toggleRightDock() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Expand Right Dock",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 3. RIGHT COLLAPSIBLE DOCK: Inspector, Lua, Shaders, AI Copilot, Docs
            AnimatedVisibility(
                visible = isRightDockOpen,
                enter = expandHorizontally(animationSpec = tween(180)) + fadeIn(),
                exit = shrinkHorizontally(animationSpec = tween(180)) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .width(270.dp)
                        .fillMaxHeight()
                        .padding(end = 4.dp, top = 2.dp, bottom = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Right Sidebar Sub-Tab Bar
                        val rightTabs = listOf(
                            Icons.Default.Tune to "Inspector",
                            Icons.Default.Code to "Lua",
                            Icons.Default.Palette to "Shaders",
                            Icons.Default.Build to "AI Fix",
                            Icons.Default.Book to "Docs"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E2633))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Collapse button
                            IconButton(
                                onClick = { viewModel.toggleRightDock() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Collapse Right",
                                    tint = Color(0xFF8B949E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            ScrollableTabRow(
                                selectedTabIndex = selectedRightTab,
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF00E5FF),
                                edgePadding = 2.dp,
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                rightTabs.forEachIndexed { idx, (icon, title) ->
                                    Tab(
                                        selected = selectedRightTab == idx,
                                        onClick = { selectedRightTab = idx },
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = title,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = if (selectedRightTab == idx) Color(0xFF00E5FF) else Color(0xFF8B949E)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = title,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (selectedRightTab == idx) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedRightTab == idx) Color(0xFF00E5FF) else Color(0xFF8B949E)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (selectedRightTab) {
                                0 -> InspectorView(viewModel = viewModel)
                                1 -> LuaEditorView(viewModel = viewModel)
                                2 -> ShaderEditorView(viewModel = viewModel)
                                3 -> AiPromptView(viewModel = viewModel)
                                4 -> DocumentationView(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }

        // 4. BOTTOM EXPANDABLE DRAWER: Console Log
        if (isBottomDockOpen) {
            ConsoleLogView(
                viewModel = viewModel,
                modifier = Modifier.height(95.dp),
                isExpanded = true,
                onToggleExpand = { viewModel.toggleBottomDock() }
            )
        }
    }
}
