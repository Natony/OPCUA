package com.example.s7opcuaapp.ui.theme

import androidx.compose.ui.unit.dp

object UiConfig {
    // Button configurations
    object Buttons {
        val LARGE_SIZE = 108.dp
        val STANDARD_SIZE = 78.dp
        val COMPACT_SIZE = 56.dp
        val SMALL_SIZE = 36.dp
        val ICON_PADDING = 4.dp

        const val DISABLED_ALPHA = 0.3f
        const val PROCESSING_ALPHA = 0.7f
        const val PRESSED_ALPHA = 0.8f
        const val AUTO_MODE_ALPHA = 0.5f
    }

    // Status display configurations
    object Status {
        val ICON_SIZE = 36.dp
        val CARD_ELEVATION = 2.dp
        val INDICATOR_SIZE = 12.dp
        const val ACTIVE_ALPHA = 0.1f
    }

    // Overlay configurations
    object Overlay {
        const val DEFAULT_ALPHA = 0.7f
        const val ERROR_ALPHA = 0.8f
        const val OFFLINE_ALPHA = 0.5f
        const val LOADING_ALPHA = 0.7f
    }

    // Common spacing
    object Spacing {
        val TINY = 2.dp
        val SMALL = 4.dp
        val MEDIUM = 8.dp
        val LARGE = 12.dp
        val EXTRA_LARGE = 16.dp
    }
}