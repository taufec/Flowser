package com.flowser.app.window

object WindowStateMachine {
    fun canTransition(from: WindowMode, to: WindowMode): Boolean = when (from) {
        WindowMode.WINDOWED -> to == WindowMode.MAXIMIZED ||
            to == WindowMode.MINIMIZED ||
            to == WindowMode.CLOSED

        WindowMode.MAXIMIZED -> to == WindowMode.WINDOWED ||
            to == WindowMode.MINIMIZED ||
            to == WindowMode.CLOSED

        WindowMode.MINIMIZED -> to == WindowMode.WINDOWED ||
            to == WindowMode.MAXIMIZED ||
            to == WindowMode.CLOSED

        WindowMode.CLOSED -> false
    }

    fun transition(state: BrowserWindowState, to: WindowMode): BrowserWindowState {
        require(canTransition(state.mode, to)) {
            "Invalid window transition: ${state.mode} -> $to"
        }

        if (
            state.mode == WindowMode.MINIMIZED &&
            to != WindowMode.CLOSED &&
            to != state.preMinimizeMode
        ) {
            throw IllegalArgumentException(
                "Minimized window must restore to ${state.preMinimizeMode}, not $to"
            )
        }

        return when (to) {
            WindowMode.MINIMIZED -> state.copy(
                mode = WindowMode.MINIMIZED,
                preMinimizeMode = state.mode
            )

            WindowMode.MAXIMIZED -> state.copy(
                mode = WindowMode.MAXIMIZED,
                lastNormalGeometry = if (state.mode == WindowMode.WINDOWED) {
                    state.geometry
                } else {
                    state.lastNormalGeometry
                }
            )

            WindowMode.WINDOWED -> state.copy(
                mode = WindowMode.WINDOWED,
                geometry = state.lastNormalGeometry
            )

            WindowMode.CLOSED -> state.copy(mode = WindowMode.CLOSED)
        }
    }
}
