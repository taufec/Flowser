package com.flowser.app.window

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowStateMachineTest {
    @Test
    fun valid_transitions_match_window_lifecycle() {
        val valid = setOf(
            WindowMode.WINDOWED to WindowMode.MAXIMIZED,
            WindowMode.WINDOWED to WindowMode.MINIMIZED,
            WindowMode.WINDOWED to WindowMode.CLOSED,
            WindowMode.MAXIMIZED to WindowMode.WINDOWED,
            WindowMode.MAXIMIZED to WindowMode.MINIMIZED,
            WindowMode.MAXIMIZED to WindowMode.CLOSED,
            WindowMode.MINIMIZED to WindowMode.WINDOWED,
            WindowMode.MINIMIZED to WindowMode.MAXIMIZED,
            WindowMode.MINIMIZED to WindowMode.CLOSED
        )

        WindowMode.entries.forEach { from ->
            WindowMode.entries.forEach { to ->
                assertEquals(
                    "$from -> $to",
                    from to to in valid,
                    WindowStateMachine.canTransition(from, to)
                )
            }
        }
    }

    @Test
    fun closed_window_cannot_be_reopened_by_transition() {
        assertFalse(WindowStateMachine.canTransition(WindowMode.CLOSED, WindowMode.WINDOWED))
        assertThrowsIllegalArgument {
            WindowStateMachine.transition(state(mode = WindowMode.CLOSED), WindowMode.WINDOWED)
        }
    }

    @Test
    fun minimizing_records_previous_window_mode() {
        val fromWindowed = WindowStateMachine.transition(
            state(mode = WindowMode.WINDOWED),
            WindowMode.MINIMIZED
        )
        val fromMaximized = WindowStateMachine.transition(
            state(mode = WindowMode.MAXIMIZED),
            WindowMode.MINIMIZED
        )

        assertEquals(WindowMode.WINDOWED, fromWindowed.preMinimizeMode)
        assertEquals(WindowMode.MAXIMIZED, fromMaximized.preMinimizeMode)
        assertEquals(WindowMode.MINIMIZED, fromWindowed.mode)
        assertEquals(WindowMode.MINIMIZED, fromMaximized.mode)
    }

    @Test
    fun minimized_window_only_restores_to_recorded_mode() {
        val minimized = state(
            mode = WindowMode.MINIMIZED,
            preMinimizeMode = WindowMode.MAXIMIZED
        )

        val restored = WindowStateMachine.transition(minimized, WindowMode.MAXIMIZED)

        assertEquals(WindowMode.MAXIMIZED, restored.mode)
        assertThrowsIllegalArgument {
            WindowStateMachine.transition(minimized, WindowMode.WINDOWED)
        }
    }

    @Test
    fun maximizing_records_last_normal_geometry() {
        val normal = WindowGeometry(x = 44, y = 55, width = 600, height = 700)
        val maximized = WindowStateMachine.transition(
            state(mode = WindowMode.WINDOWED, geometry = normal),
            WindowMode.MAXIMIZED
        )

        assertEquals(normal, maximized.lastNormalGeometry)
        assertTrue(maximized.mode == WindowMode.MAXIMIZED)
    }

    private fun state(
        mode: WindowMode,
        geometry: WindowGeometry = WindowGeometry(10, 20, 300, 400),
        preMinimizeMode: WindowMode = WindowMode.WINDOWED
    ): BrowserWindowState = BrowserWindowState(
        mode = mode,
        geometry = geometry,
        lastNormalGeometry = WindowGeometry(10, 20, 300, 400),
        preMinimizeMode = preMinimizeMode,
        bubbleX = 0,
        bubbleY = 100,
        currentUrl = "https://example.com",
        desktopMode = false,
        zoomPercent = 100
    )

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
