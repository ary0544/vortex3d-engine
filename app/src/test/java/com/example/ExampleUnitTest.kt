package com.example

import com.example.engine.renderer.Camera3D
import com.example.viewmodel.CameraSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCameraDefaultsAndReset() {
        val camera = Camera3D()
        assertEquals(8.0f, camera.distance, 0.001f)
        assertEquals(30.0f, camera.pitchDegrees, 0.001f)
        assertEquals(45.0f, camera.yawDegrees, 0.001f)

        camera.orbit(15f, 10f)
        assertEquals(40.0f, camera.pitchDegrees, 0.001f)
        assertEquals(60.0f, camera.yawDegrees, 0.001f)

        camera.zoom(2.0f)
        assertEquals(10.0f, camera.distance, 0.001f)

        camera.reset()
        assertEquals(8.0f, camera.distance, 0.001f)
        assertEquals(30.0f, camera.pitchDegrees, 0.001f)
        assertEquals(45.0f, camera.yawDegrees, 0.001f)
    }

    @Test
    fun testCameraPresets() {
        val camera = Camera3D()
        camera.setFrontView()
        assertEquals(0.0f, camera.pitchDegrees, 0.001f)
        assertEquals(0.0f, camera.yawDegrees, 0.001f)

        camera.setTopView()
        assertEquals(85.0f, camera.pitchDegrees, 0.001f)
        assertEquals(0.0f, camera.yawDegrees, 0.001f)

        camera.setSideView()
        assertEquals(0.0f, camera.pitchDegrees, 0.001f)
        assertEquals(90.0f, camera.yawDegrees, 0.001f)

        camera.setIsometricView()
        assertEquals(30.0f, camera.pitchDegrees, 0.001f)
        assertEquals(45.0f, camera.yawDegrees, 0.001f)
    }

    @Test
    fun testCameraSettingsDefaults() {
        val settings = CameraSettings()
        assertFalse(settings.invertX)
        assertFalse(settings.invertY)
        assertEquals(1.0f, settings.orbitSensitivity, 0.001f)
        assertEquals(1.0f, settings.panSensitivity, 0.001f)
        assertEquals(1.0f, settings.zoomSensitivity, 0.001f)

        val updated = settings.copy(invertX = true, invertY = true, orbitSensitivity = 1.5f)
        assertTrue(updated.invertX)
        assertTrue(updated.invertY)
        assertEquals(1.5f, updated.orbitSensitivity, 0.001f)
    }
}
