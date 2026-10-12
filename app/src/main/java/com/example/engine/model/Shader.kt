package com.example.engine.model

import com.example.engine.math.Vec3

enum class ShaderType(val displayName: String, val description: String) {
    PBR_STANDARD("PBR Standard", "Physically-Based Blinn-Phong/Cook-Torrance lighting with roughness & metallic"),
    TOON_CEL("Toon / Cel Shading", "Banded anime-style lighting thresholds with edge silhouette line"),
    HOLOGRAPHIC("Neon Hologram", "Sci-fi scanlines with chromatic aberration and high-intensity rim glow"),
    DISSOLVE("Dissolve Burn", "Noise-driven edge burn dissolution effect with glowing ember rim"),
    WATER_WAVE("Water Caustics", "Real-time sine wave vertex displacement with specular highlights"),
    GLITCH_CYBER("Cyber Glitch", "High-frequency vertex jitter and RGB offset distortion"),
    CUSTOM_GLSL("Custom GLSL", "User-defined programmable vertex and fragment shader pipeline")
}

data class ShaderUniform(
    val name: String,
    val type: UniformType,
    val floatValue: Float = 0f,
    val vec3Value: Vec3 = Vec3.ZERO,
    val colorHex: Long = 0xFFFFFFFF
)

enum class UniformType {
    FLOAT,
    VEC3,
    COLOR
}

data class ShaderProgram(
    val id: String,
    val name: String,
    val type: ShaderType,
    val vertexCode: String,
    val fragmentCode: String,
    val uniforms: Map<String, Float> = emptyMap(),
    val isBuiltIn: Boolean = true
) {
    companion object {
        fun createPbrShader(): ShaderProgram = ShaderProgram(
            id = "shader_pbr",
            name = "PBR Standard",
            type = ShaderType.PBR_STANDARD,
            vertexCode = """
                // Vertex Shader - PBR Standard
                precision highp float;
                attribute vec3 a_Position;
                attribute vec3 a_Normal;
                attribute vec2 a_TexCoord;
                uniform mat4 u_MVPMatrix;
                uniform mat4 u_ModelMatrix;
                varying vec3 v_WorldPos;
                varying vec3 v_Normal;
                varying vec2 v_TexCoord;
                
                void main() {
                    v_WorldPos = (u_ModelMatrix * vec4(a_Position, 1.0)).xyz;
                    v_Normal = normalize((u_ModelMatrix * vec4(a_Normal, 0.0)).xyz);
                    v_TexCoord = a_TexCoord;
                    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
                }
            """.trimIndent(),
            fragmentCode = """
                // Fragment Shader - PBR Standard
                precision mediump float;
                varying vec3 v_WorldPos;
                varying vec3 v_Normal;
                varying vec2 v_TexCoord;
                uniform vec3 u_LightDir;
                uniform vec4 u_AlbedoColor;
                uniform float u_Roughness;
                uniform float u_Metallic;
                uniform vec3 u_CameraPos;
                
                void main() {
                    vec3 N = normalize(v_Normal);
                    vec3 L = normalize(u_LightDir);
                    vec3 V = normalize(u_CameraPos - v_WorldPos);
                    vec3 H = normalize(L + V);
                    
                    float diff = max(dot(N, L), 0.0);
                    float spec = pow(max(dot(N, H), 0.0), (1.0 - u_Roughness) * 64.0 + 1.0) * u_Metallic;
                    
                    vec3 ambient = vec3(0.2) * u_AlbedoColor.rgb;
                    vec3 diffuse = diff * u_AlbedoColor.rgb;
                    vec3 specular = vec3(spec);
                    
                    gl_FragColor = vec4(ambient + diffuse + specular, u_AlbedoColor.a);
                }
            """.trimIndent(),
            uniforms = mapOf("roughness" to 0.4f, "metallic" to 0.6f)
        )

        fun createToonShader(): ShaderProgram = ShaderProgram(
            id = "shader_toon",
            name = "Toon Cel Shading",
            type = ShaderType.TOON_CEL,
            vertexCode = """
                // Vertex Shader - Cel Toon
                attribute vec3 a_Position;
                attribute vec3 a_Normal;
                uniform mat4 u_MVPMatrix;
                uniform mat4 u_ModelMatrix;
                varying vec3 v_Normal;
                varying vec3 v_ViewDir;
                
                void main() {
                    v_Normal = normalize((u_ModelMatrix * vec4(a_Normal, 0.0)).xyz);
                    vec3 worldPos = (u_ModelMatrix * vec4(a_Position, 1.0)).xyz;
                    v_ViewDir = normalize(-worldPos);
                    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
                }
            """.trimIndent(),
            fragmentCode = """
                // Fragment Shader - Cel Toon
                precision mediump float;
                varying vec3 v_Normal;
                varying vec3 v_ViewDir;
                uniform vec3 u_LightDir;
                uniform vec4 u_AlbedoColor;
                uniform float u_Bands;
                
                void main() {
                    float NdotL = dot(v_Normal, normalize(u_LightDir));
                    float bandFactor = max(0.0, floor(NdotL * u_Bands + 0.5) / u_Bands);
                    float rim = 1.0 - max(dot(v_Normal, v_ViewDir), 0.0);
                    float rimStep = step(0.7, rim);
                    
                    vec3 base = u_AlbedoColor.rgb * (bandFactor * 0.7 + 0.3);
                    vec3 finalCol = mix(base, vec3(1.0), rimStep * 0.5);
                    gl_FragColor = vec4(finalCol, 1.0);
                }
            """.trimIndent(),
            uniforms = mapOf("bands" to 3.0f, "rimThreshold" to 0.7f)
        )

        fun createHologramShader(): ShaderProgram = ShaderProgram(
            id = "shader_hologram",
            name = "Cyber Hologram",
            type = ShaderType.HOLOGRAPHIC,
            vertexCode = """
                // Vertex Shader - Cyber Hologram
                attribute vec3 a_Position;
                attribute vec3 a_Normal;
                uniform mat4 u_MVPMatrix;
                uniform float u_Time;
                varying vec3 v_Normal;
                varying float v_Height;
                
                void main() {
                    v_Normal = a_Normal;
                    v_Height = a_Position.y + sin(u_Time * 4.0 + a_Position.x) * 0.05;
                    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
                }
            """.trimIndent(),
            fragmentCode = """
                // Fragment Shader - Cyber Hologram
                precision mediump float;
                varying vec3 v_Normal;
                varying float v_Height;
                uniform float u_Time;
                uniform vec4 u_GlowColor;
                
                void main() {
                    float scanline = sin(v_Height * 40.0 - u_Time * 10.0) * 0.5 + 0.5;
                    float rim = pow(1.0 - abs(v_Normal.z), 2.0);
                    vec3 color = u_GlowColor.rgb * (rim * 0.8 + scanline * 0.4);
                    gl_FragColor = vec4(color, 0.85);
                }
            """.trimIndent(),
            uniforms = mapOf("scanlineFreq" to 40.0f, "speed" to 5.0f)
        )

        fun createWaterShader(): ShaderProgram = ShaderProgram(
            id = "shader_water",
            name = "Water Caustics",
            type = ShaderType.WATER_WAVE,
            vertexCode = """
                // Vertex Shader - Water Wave Displacement
                attribute vec3 a_Position;
                attribute vec3 a_Normal;
                uniform mat4 u_MVPMatrix;
                uniform float u_Time;
                uniform float u_WaveHeight;
                varying vec3 v_Normal;
                
                void main() {
                    vec3 pos = a_Position;
                    pos.y += sin(pos.x * 2.0 + u_Time * 3.0) * u_WaveHeight 
                           + cos(pos.z * 2.0 + u_Time * 2.5) * u_WaveHeight;
                    v_Normal = a_Normal;
                    gl_Position = u_MVPMatrix * vec4(pos, 1.0);
                }
            """.trimIndent(),
            fragmentCode = """
                // Fragment Shader - Water
                precision mediump float;
                varying vec3 v_Normal;
                uniform vec4 u_WaterColor;
                uniform float u_Time;
                
                void main() {
                    float shimmer = sin(u_Time * 6.0) * 0.1 + 0.9;
                    vec3 finalCol = u_WaterColor.rgb * shimmer;
                    gl_FragColor = vec4(finalCol, 0.8);
                }
            """.trimIndent(),
            uniforms = mapOf("waveHeight" to 0.15f, "speed" to 2.0f)
        )
    }
}
