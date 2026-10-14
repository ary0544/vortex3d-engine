package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.ViewInAr
import com.example.viewmodel.ProjectOrientation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.EngineViewModel
import com.example.viewmodel.ProjectItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectHubScreen(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val activeProjectId by viewModel.activeProjectId.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Projects, 1: Templates, 2: Info
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter { it.name.contains(searchQuery, ignoreCase = true) || it.templateName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF478CBF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Grain,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Vortex3D",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFF0F6FC)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Project Hub",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color(0xFF478CBF)
                                )
                            }
                            Text(
                                text = "Godot 4 Mobile Architecture • Project Manager",
                                fontSize = 10.sp,
                                color = Color(0xFF8B949E)
                            )
                        }
                    }
                },
                actions = {
                    // Back to active workspace
                    OutlinedButton(
                        onClick = { viewModel.setInLobby(false) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("lobby_open_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Open 3D Studio", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141419))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF478CBF),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_create_scene")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Scene Project",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF1A1A22))
        ) {
            // Hub Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = Color(0xFF141419),
                contentColor = Color(0xFF478CBF),
                modifier = Modifier.fillMaxWidth().testTag("lobby_tabs")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Saved Projects & Scenes (${projects.size})", fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New Scene Templates", fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Engine Engine Specs", fontSize = 12.sp)
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Projects List View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Search & Action bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("lobby_search_field"),
                                placeholder = { Text("Filter scenes...", fontSize = 12.sp, color = Color(0xFF8B949E)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF8B949E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF478CBF),
                                    unfocusedBorderColor = Color(0xFF2E3440),
                                    focusedContainerColor = Color(0xFF141419),
                                    unfocusedContainerColor = Color(0xFF141419),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF478CBF)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("lobby_new_scene_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Scene", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("lobby_project_list"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = filteredProjects,
                                key = { index, project -> "${project.id}_$index" }
                            ) { _, project ->
                                val isCurrentActive = project.id == activeProjectId
                                ProjectCard(
                                    project = project,
                                    isActive = isCurrentActive,
                                    onOpen = { viewModel.openProject(project) },
                                    onPlay = {
                                        viewModel.openProject(project)
                                        viewModel.setPlayMode(true)
                                    },
                                    onDuplicate = { viewModel.duplicateProject(project) },
                                    onDelete = { viewModel.deleteProject(project.id) },
                                    onChangeOrientation = { nextOrientation ->
                                        viewModel.setProjectOrientation(project.id, nextOrientation)
                                    }
                                )
                            }
                            item(key = "bottom_spacer") {
                                Spacer(Modifier.height(80.dp))
                            }
                        }
                    }
                }
                1 -> {
                    // Templates Grid / List
                    SceneTemplatesView(
                        onCreateFromTemplate = { name, templateKey, orientation ->
                            viewModel.createProject(name, templateKey, orientation)
                        }
                    )
                }
                2 -> {
                    // Engine Specs & Godot Ergonomics Info
                    EngineSpecsView()
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateNewSceneDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, template, orientation ->
                viewModel.createProject(name, template, orientation)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun ProjectCard(
    project: ProjectItem,
    isActive: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onChangeOrientation: (ProjectOrientation) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1E2838) else Color(0xFF161B24)
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) Color(0xFF478CBF) else Color(0xFF30363D)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Row 1: Icon + Project Header & Badges + Quick Run Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail / Icon Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (project.templateName) {
                                "FPS Action Arena", "FPS" -> Color(0xFF6A1B9A)
                                "Cyberpunk Neo World", "CYBER" -> Color(0xFF00695C)
                                "Blank 3D Scene", "BLANK" -> Color(0xFF37474F)
                                else -> Color(0xFF1565C0)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (project.templateName) {
                            "FPS Action Arena", "FPS" -> Icons.Default.SportsEsports
                            "Cyberpunk Neo World", "CYBER" -> Icons.Default.AutoAwesome
                            else -> Icons.Default.ViewInAr
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Project Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFF0F6FC),
                            maxLines = 1
                        )
                        if (isActive) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF478CBF).copy(alpha = 0.25f))
                                    .border(1.dp, Color(0xFF478CBF), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                            }
                        }
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = "${project.entityCount} Nodes • ${project.templateName}",
                        fontSize = 11.sp,
                        color = Color(0xFF8B949E),
                        maxLines = 1
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Primary Run/Play Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00E676).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onPlay() }
                        .testTag("btn_play_${project.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run Scene",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Thin separation line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF21262D))
            )

            Spacer(Modifier.height(8.dp))

            // Row 2: Orientation Toggle Badge + Action Buttons (Edit, Duplicate, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Clickable orientation indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (project.orientation) {
                                ProjectOrientation.LANDSCAPE -> Color(0xFF1565C0).copy(alpha = 0.25f)
                                ProjectOrientation.PORTRAIT -> Color(0xFF6A1B9A).copy(alpha = 0.25f)
                                ProjectOrientation.AUTO_SENSOR -> Color(0xFF00695C).copy(alpha = 0.25f)
                            }
                        )
                        .border(
                            1.dp,
                            when (project.orientation) {
                                ProjectOrientation.LANDSCAPE -> Color(0xFF1E88E5).copy(alpha = 0.4f)
                                ProjectOrientation.PORTRAIT -> Color(0xFF8E24AA).copy(alpha = 0.4f)
                                ProjectOrientation.AUTO_SENSOR -> Color(0xFF00897B).copy(alpha = 0.4f)
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            val next = when (project.orientation) {
                                ProjectOrientation.LANDSCAPE -> ProjectOrientation.PORTRAIT
                                ProjectOrientation.PORTRAIT -> ProjectOrientation.AUTO_SENSOR
                                ProjectOrientation.AUTO_SENSOR -> ProjectOrientation.LANDSCAPE
                            }
                            onChangeOrientation(next)
                        }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (project.orientation) {
                                ProjectOrientation.LANDSCAPE -> Icons.Default.StayCurrentLandscape
                                ProjectOrientation.PORTRAIT -> Icons.Default.StayCurrentPortrait
                                ProjectOrientation.AUTO_SENSOR -> Icons.Default.ScreenRotation
                            },
                            contentDescription = "Toggle Orientation",
                            tint = when (project.orientation) {
                                ProjectOrientation.LANDSCAPE -> Color(0xFF64B5F6)
                                ProjectOrientation.PORTRAIT -> Color(0xFFBA68C8)
                                ProjectOrientation.AUTO_SENSOR -> Color(0xFF4DB6AC)
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${project.orientation.badge} Mode",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (project.orientation) {
                                ProjectOrientation.LANDSCAPE -> Color(0xFF64B5F6)
                                ProjectOrientation.PORTRAIT -> Color(0xFFBA68C8)
                                ProjectOrientation.AUTO_SENSOR -> Color(0xFF4DB6AC)
                            }
                        )
                    }
                }

                // Action Buttons Row (Compact 30dp height, Godot 4 style)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF478CBF)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_edit_${project.id}")
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editor", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SceneTemplatesView(
    onCreateFromTemplate: (String, String, ProjectOrientation) -> Unit
) {
    val templates = listOf(
        TemplateCardData(
            id = "DEFAULT",
            title = "Physics Playground",
            subtitle = "Rigid bodies, bouncing spheres, stacking boxes, ground plane, hero Lua controller.",
            badge = "Recommended",
            icon = Icons.Default.ViewInAr,
            color = Color(0xFF1565C0),
            defaultOrientation = ProjectOrientation.LANDSCAPE
        ),
        TemplateCardData(
            id = "FPS",
            title = "FPS Action Arena",
            subtitle = "Pre-configured arena walls, obstacles, responsive touch player controller, dynamic lighting.",
            badge = "Gameplay",
            icon = Icons.Default.SportsEsports,
            color = Color(0xFF6A1B9A),
            defaultOrientation = ProjectOrientation.LANDSCAPE
        ),
        TemplateCardData(
            id = "CYBER",
            title = "Cyberpunk Neo World",
            subtitle = "Neon spinning quantum torus, reflective floor, dual magenta/cyan point lights, custom shaders.",
            badge = "VFX & Shader",
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFF00695C),
            defaultOrientation = ProjectOrientation.PORTRAIT
        ),
        TemplateCardData(
            id = "BLANK",
            title = "Blank 3D Scene",
            subtitle = "Clean minimalist starting scene with single directional sun and grid floor.",
            badge = "Minimal",
            icon = Icons.Default.Layers,
            color = Color(0xFF37474F),
            defaultOrientation = ProjectOrientation.AUTO_SENSOR
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("templates_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(templates, key = { index, t -> "${t.id}_$index" }) { _, template ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B24)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(template.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(template.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = template.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFF0F6FC)
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(template.color.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(template.badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                }
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF21262D))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(template.defaultOrientation.badge, fontSize = 8.sp, color = Color(0xFF8B949E))
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = template.subtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF8B949E),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onCreateFromTemplate(template.title, template.id, template.defaultOrientation)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF478CBF)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(34.dp).testTag("btn_create_template_${template.id.lowercase()}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Create from Template", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        item(key = "template_bottom_spacer") {
            Spacer(Modifier.height(80.dp))
        }
    }
}

data class TemplateCardData(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val color: Color,
    val defaultOrientation: ProjectOrientation = ProjectOrientation.LANDSCAPE
)

@Composable
fun EngineSpecsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B24)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Godot 4 Mobile Architecture in Vortex3D",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF00E5FF)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• Collapsible Dock Panels: Scene Tree (Left), Inspector & Script (Right), and Console (Bottom) retract with 1 tap, leaving 100% of the screen for the 3D Viewport on touch devices.\n\n" +
                           "• Adaptive Orientations: Seamlessly transitions between 3-Pane Landscape Studio, Compact Portrait view, and expanded Tablet layouts.\n\n" +
                           "• Built-in Storage Access: Direct file manager with zero-flicker browsing for .glb, .obj, .lua, and shaders without external dependencies.\n\n" +
                           "• Real-time Physics & Shaders: 60 FPS Verlet integration, Lua 5.1 virtual machine, Phong illumination & custom animated GLSL shaders.",
                    fontSize = 12.sp,
                    color = Color(0xFFC9D1D9),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun CreateNewSceneDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, ProjectOrientation) -> Unit
) {
    var sceneName by remember { mutableStateOf("My 3D Level") }
    var selectedTemplate by remember { mutableStateOf("DEFAULT") }
    var selectedOrientation by remember { mutableStateOf(ProjectOrientation.LANDSCAPE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create New 3D Scene", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Scene Name", fontSize = 12.sp, color = Color(0xFF8B949E))
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = sceneName,
                    onValueChange = { sceneName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_scene_name_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF478CBF),
                        unfocusedBorderColor = Color(0xFF2E3440),
                        focusedContainerColor = Color(0xFF141419),
                        unfocusedContainerColor = Color(0xFF141419),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(Modifier.height(14.dp))
                Text("Select Template Preset", fontSize = 12.sp, color = Color(0xFF8B949E))
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTemplate == "DEFAULT",
                        onClick = {
                            selectedTemplate = "DEFAULT"
                            selectedOrientation = ProjectOrientation.LANDSCAPE
                        },
                        label = { Text("Playground", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF478CBF),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedTemplate == "FPS",
                        onClick = {
                            selectedTemplate = "FPS"
                            selectedOrientation = ProjectOrientation.LANDSCAPE
                        },
                        label = { Text("FPS Arena", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6A1B9A),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedTemplate == "CYBER",
                        onClick = {
                            selectedTemplate = "CYBER"
                            selectedOrientation = ProjectOrientation.PORTRAIT
                        },
                        label = { Text("Cyberpunk", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00695C),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                Spacer(Modifier.height(4.dp))
                FilterChip(
                    selected = selectedTemplate == "BLANK",
                    onClick = {
                        selectedTemplate = "BLANK"
                        selectedOrientation = ProjectOrientation.AUTO_SENSOR
                    },
                    label = { Text("Blank 3D Scene", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF37474F),
                        selectedLabelColor = Color.White
                    )
                )

                Spacer(Modifier.height(14.dp))
                Text("Target Orientation", fontSize = 12.sp, color = Color(0xFF8B949E))
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedOrientation == ProjectOrientation.LANDSCAPE,
                        onClick = { selectedOrientation = ProjectOrientation.LANDSCAPE },
                        label = { Text("Landscape", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.StayCurrentLandscape, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1565C0),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedOrientation == ProjectOrientation.PORTRAIT,
                        onClick = { selectedOrientation = ProjectOrientation.PORTRAIT },
                        label = { Text("Portrait", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.StayCurrentPortrait, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6A1B9A),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedOrientation == ProjectOrientation.AUTO_SENSOR,
                        onClick = { selectedOrientation = ProjectOrientation.AUTO_SENSOR },
                        label = { Text("Auto", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.ScreenRotation, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00695C),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sceneName.isNotBlank()) {
                        onCreate(sceneName.trim(), selectedTemplate, selectedOrientation)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF478CBF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("dialog_create_confirm")
            ) {
                Text("Create Scene", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8B949E))
            }
        },
        containerColor = Color(0xFF1E1E28),
        shape = RoundedCornerShape(12.dp)
    )
}
