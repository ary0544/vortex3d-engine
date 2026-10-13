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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EngineViewModel
import com.example.viewmodel.WorkspaceTab

@Composable
fun DocumentationView(
    viewModel: EngineViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    val sections = listOf("Engine Architecture", "Lua API", "3D Physics", "GLSL Shaders", "Tutorials")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Vortex3D Engine Documentation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(6.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedSection,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth().height(32.dp)
        ) {
            sections.forEachIndexed { index, name ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
                    text = { Text(name, fontSize = 11.sp) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedSection) {
                0 -> ArchitectureDoc()
                1 -> LuaApiDoc()
                2 -> PhysicsDoc()
                3 -> ShaderDoc()
                4 -> TutorialsDoc(viewModel)
            }
        }
    }
}

@Composable
private fun ArchitectureDoc() {
    DocCard(title = "Engine Design & ECS Architecture") {
        Text(
            text = "Vortex3D is an ultra-lightweight mobile 3D game engine built specifically for indie developers. It features:\n\n" +
                    "• Entity Component System (ECS): Entities contain Transform, MeshRenderer, RigidBody, LuaScript, and Light components.\n" +
                    "• 60 FPS Software Rasterizer: Highly optimized perspective projection, backface culling, and depth sorting.\n" +
                    "• Integrated Lua VM: Real-time script execution with zero build waiting time.\n" +
                    "• Zero Dependencies Physics: Fast semi-implicit Euler integration with restitution and friction.",
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun LuaApiDoc() {
    DocCard(title = "Entity & Transform API") {
        CodeBlock("""
entity:get_position() -> returns (x, y, z)
entity:set_position(x, y, z) -> sets position
entity:rotate(pitch, yaw, roll) -> continuous Euler rotation
entity:translate(dx, dy, dz) -> local movement
entity:set_color(r, g, b, a) -> alters material tint
        """.trimIndent())
    }

    Spacer(Modifier.height(8.dp))

    DocCard(title = "Real-Time Physics API") {
        CodeBlock("""
physics:apply_force(fx, fy, fz) -> applies continuous force
physics:apply_impulse(ix, iy, iz) -> instantaneous velocity burst
physics:set_velocity(vx, vy, vz) -> overrides velocity
physics:get_velocity() -> returns current velocity
        """.trimIndent())
    }

    Spacer(Modifier.height(8.dp))

    DocCard(title = "Input & Engine Audio API") {
        CodeBlock("""
input:get_axis("Horizontal") -> [-1.0 .. 1.0] virtual joystick
input:get_axis("Vertical") -> [-1.0 .. 1.0] virtual joystick
input:is_action_pressed("Jump") -> boolean
engine:play_sound("jump" | "impact" | "laser" | "explosion" | "coin")
engine:log(message) -> outputs to engine console
        """.trimIndent())
    }
}

@Composable
private fun PhysicsDoc() {
    DocCard(title = "Rigid Body Solver & Colliders") {
        Text(
            text = "The physics simulator models rigid bodies using impulse resolution:\n\n" +
                    "• Box Collider: Axis-Aligned and Oriented Bounding Box collision check.\n" +
                    "• Sphere Collider: Fast radial distance tests.\n" +
                    "• Ground Plane: Dynamic restitution bounce (v\' = -e * v) with surface friction damping.\n" +
                    "• Mass & Inertia: Automatic mass-weighted velocity distribution upon impact.",
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ShaderDoc() {
    DocCard(title = "Custom Shaders & GLSL Pipeline") {
        Text(
            text = "Vortex3D provides mobile GLSL ES shader pipelines:\n\n" +
                    "• PBR Standard: Physically Based Blinn-Phong/Cook-Torrance model with roughness and metallic reflections.\n" +
                    "• Toon / Cel Shading: Discretized diffuse light quantization with rim lighting.\n" +
                    "• Cyber Hologram: High-frequency sine wave scanline generator and fresnel glowing edges.\n" +
                    "• Water Caustics: Real-time vertex displacement with specular highlights.",
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun TutorialsDoc(viewModel: EngineViewModel) {
    DocCard(title = "Tutorial 1: Physics Playground Sandbox") {
        Text(
            text = "Experience stacked destructible crates, a dynamic player orb, and a rotating holographic portal.",
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = {
                viewModel.resetSimulation()
                viewModel.setWorkspaceTab(WorkspaceTab.SCENE_VIEW)
            }
        ) {
            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Open Scene in 3D Viewport", fontSize = 12.sp)
        }
    }
}

@Composable
private fun DocCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = code,
            color = Color(0xFF7EE787),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(8.dp),
            lineHeight = 16.sp
        )
    }
}
