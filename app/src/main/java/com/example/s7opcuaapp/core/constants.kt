package com.example.s7opcuaapp.core.constants

/**
 * PLC-related constants
 */
object PlcConstants {

    // Node ID ranges
    const val BOOL_NODE_START = 3
    const val BOOL_NODE_END = 17
    const val INT_NODE_START = 18
    const val INT_NODE_END = 45

    // Button indices
    object Buttons {
        // Manual movement buttons
        const val MANUAL_FORWARD = 0
        const val MANUAL_REVERSE = 1
        const val MANUAL_UP = 2
        const val MANUAL_DOWN = 3

        // Common buttons
        const val POWER = 4
        const val BUZZER = 5
        const val EMERGENCY_STOP = 10
        const val FIFO_LIFO = 11
        const val DIRECTION = 13
        const val COUNT_PALLET = 14

        // Auto buttons
        const val AUTO_1 = 6
        const val AUTO_2 = 7
        const val AUTO_3 = 8
        const val AUTO_4 = 9

        // Special indices
        const val SEND_ALL = 999
    }

    // Integer indices
    object Integers {
        const val STATUS = 0
        const val BATTERY = 1
        const val COUNT = 2
        const val PALLETS_IN = 3
        const val PALLETS_OUT = 4

        // Coordinates
        const val START_X = 5
        const val START_Y = 6
        const val START_Z = 7
        const val END_X = 8
        const val END_Y = 9
        const val END_Z = 10
        const val ACTUAL_X = 11
        const val ACTUAL_Y = 12
        const val ACTUAL_Z = 13

        const val FUNCTION_CODE = 14

        // Position states
        const val POS_START = 15
        const val POS_END = 27
    }

    // UI offsets
    const val INT_BUTTON_OFFSET = 200

    // Timing
    const val MIN_BUTTON_ACTION_INTERVAL = 100L
    const val CONNECTION_TIMEOUT = 15000L
    const val RETRY_DELAY = 2000L
}