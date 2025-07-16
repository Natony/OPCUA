package com.example.s7opcuaapp.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for button locking behavior
 * Supports:
 * - Group locking (when one button in group is active, lock others in same group)
 * - Cross locking (when button X is active, lock specific other buttons)
 * - Custom rules
 */
@Singleton
class ButtonLockConfig @Inject constructor() {

    // Button types for easier management
    enum class ButtonType {
        BOOL_MANUAL,    // Manual mode bool buttons (0-3)
        BOOL_AUTO,      // Auto mode bool buttons (6-9)
        INT_AUTO,       // Auto mode int buttons (3-4 with offset)
        BOOL_COMMON,    // Common bool buttons (4,5,10,11,12,13,14)
        INT_COMMON      // Common int buttons
    }

    // Button groups - buttons in same group are mutually exclusive
    private val buttonGroups = mapOf(
        "manual_movement" to setOf(0, 1, 2, 3),  // Manual forward/reverse/up/down
        "auto_pallets" to setOf(6, 7, 8, 9),     // Auto mode pallet operations
        "auto_int_pallets" to setOf(203, 204),   // Int buttons 3,4 with offset
        "power_emergency" to setOf(4, 10),       // Power and Emergency stop
        "mode_switch" to setOf(11),              // FIFO/LIFO mode
        "direction" to setOf(13),                // Direction A/B
        "count_pallet" to setOf(14)              // Count pallet
    )

    private val crossLockRules = mapOf(
        // When any manual movement is active, lock auto operations
        0 to setOf(6, 7, 8, 9, 203, 204),
        1 to setOf(6, 7, 8, 9, 203, 204),
        2 to setOf(6, 7, 8, 9, 203, 204),
        3 to setOf(6, 7, 8, 9, 203, 204),
        14 to setOf(0, 1, 2, 3, 203, 204),

        // When any auto operation is active, lock manual movements
        6 to setOf(0, 1, 2, 3, 14, 203, 204),
        7 to setOf(0, 1, 2, 3, 14, 203, 204),
        8 to setOf(0, 1, 2, 3, 14, 203, 204),
        9 to setOf(0, 1, 2, 3, 14, 203, 204),

        203 to setOf(0, 1, 2, 3, 6, 7, 8, 9, 14),
        204 to setOf(0, 1, 2, 3, 6, 7, 8, 9, 14),

        // When emergency stop is active, lock all operations
        10 to setOf(0, 1, 2, 3, 6, 7, 8, 9, 14, 202, 203, 204)
    )

    // Priority rules - these buttons can interrupt others
    private val priorityButtons = setOf(10) // Emergency stop has highest priority

    // Immediate lock on click - these buttons lock immediately without waiting for PLC
    private val immediateLockButtons = setOf(0, 1, 2, 3, 6, 7, 8, 9, 10, 14, 203, 204)

    /**
     * Get all buttons that should be locked when given buttons are active or busy
     * @param activeButtons Set of currently active button indices
     * @param busyButtons Set of buttons currently processing
     * @return Set of button indices that should be locked
     */
    fun getLockedButtons(activeButtons: Set<Int>, busyButtons: Set<Int>): Set<Int> {
        val locked = mutableSetOf<Int>()
        val activeOrBusy = activeButtons + busyButtons

        // Check each active/busy button
        for (buttonIndex in activeOrBusy) {
            // Skip if this is a priority button that's just busy (not active)
            if (buttonIndex in priorityButtons && buttonIndex !in activeButtons) {
                continue
            }

            // Add group locks
            buttonGroups.forEach { (_, group) ->
                if (buttonIndex in group) {
                    // Lock all other buttons in the same group
                    locked.addAll(group - buttonIndex)
                }
            }

            // Add cross-lock rules
            crossLockRules[buttonIndex]?.let { toLock ->
                // Thêm dòng này để đảm bảo không tự khóa chính mình
                locked.addAll(toLock - buttonIndex)
            }
        }

        // Remove priority buttons from locked set if they're not already active
        if (activeButtons.isNotEmpty()) {
            locked.removeAll(priorityButtons - activeButtons)
        }

        // Thêm: Đảm bảo không có button nào tự khóa chính nó
        locked.removeAll(activeOrBusy)

        return locked
    }
    /**
     * Check if a button should lock immediately on click
     */
    fun shouldLockImmediately(buttonIndex: Int): Boolean {
        return buttonIndex in immediateLockButtons
    }

    /**
     * Check if button A can interrupt button B
     */
    fun canInterrupt(buttonA: Int, buttonB: Int): Boolean {
        return buttonA in priorityButtons && buttonB !in priorityButtons
    }

    /**
     * Get button type for display/logging purposes
     */
    fun getButtonType(index: Int): ButtonType {
        return when (index) {
            in 0..3 -> ButtonType.BOOL_MANUAL
            in 6..9 -> ButtonType.BOOL_AUTO
            in 203..204 -> ButtonType.INT_AUTO
            4, 5, 10, 11, 12, 13, 14 -> ButtonType.BOOL_COMMON
            else -> ButtonType.INT_COMMON
        }
    }

    /**
     * Get button group name
     */
    fun getButtonGroup(index: Int): String? {
        return buttonGroups.entries.find { (_, group) -> index in group }?.key
    }

    // Configuration methods for runtime adjustment

    fun addButtonGroup(groupName: String, buttons: Set<Int>) {
        buttonGroups.toMutableMap()[groupName] = buttons
    }

    fun addCrossLockRule(triggerButton: Int, lockedButtons: Set<Int>) {
        crossLockRules.toMutableMap()[triggerButton] = lockedButtons
    }

    fun setPriorityButton(buttonIndex: Int, isPriority: Boolean) {
        if (isPriority) {
            (priorityButtons as MutableSet).add(buttonIndex)
        } else {
            (priorityButtons as MutableSet).remove(buttonIndex)
        }
    }

    fun setImmediateLock(buttonIndex: Int, isImmediate: Boolean) {
        if (isImmediate) {
            (immediateLockButtons as MutableSet).add(buttonIndex)
        } else {
            (immediateLockButtons as MutableSet).remove(buttonIndex)
        }
    }
}