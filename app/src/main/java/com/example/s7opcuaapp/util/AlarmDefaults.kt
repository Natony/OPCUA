package com.example.s7opcuaapp.util

import com.example.s7opcuaapp.data.model.alarm.AlarmCategory
import com.example.s7opcuaapp.data.model.alarm.AlarmConfig
import com.example.s7opcuaapp.data.model.alarm.AlarmPriority

object AlarmDefaults {

    /**
     * Danh sách cấu hình alarm mặc định cho hệ thống
     * Mã lỗi từ PLC sẽ được map với các config này
     */
    val DEFAULT_ALARM_CONFIGS = listOf(
        // Nhóm lỗi Process (1-10)
        AlarmConfig(
            alarmCode = 1,
            priority = AlarmPriority.LOW,
            category = AlarmCategory.PROCESS,
            message = "Pallet không đúng vị trí",
            description = "Pallet không nằm đúng vị trí cần thiết trên băng tải",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 2,
            priority = AlarmPriority.MEDIUM,
            category = AlarmCategory.PROCESS,
            message = "Chu trình vận hành quá thời gian",
            description = "Thời gian thực hiện chu trình vượt quá giới hạn cho phép",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 3,
            priority = AlarmPriority.MEDIUM,
            category = AlarmCategory.PROCESS,
            message = "Số lượng pallet vượt quá giới hạn",
            description = "Số lượng pallet trong hệ thống vượt quá capacity",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        ),

        // Nhóm lỗi Equipment (11-20)
        AlarmConfig(
            alarmCode = 11,
            priority = AlarmPriority.HIGH,
            category = AlarmCategory.EQUIPMENT,
            message = "Động cơ băng tải quá tải",
            description = "Dòng điện động cơ vượt quá định mức cho phép",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 12,
            priority = AlarmPriority.HIGH,
            category = AlarmCategory.EQUIPMENT,
            message = "Nhiệt độ động cơ cao",
            description = "Nhiệt độ động cơ vượt ngưỡng an toàn",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 13,
            priority = AlarmPriority.MEDIUM,
            category = AlarmCategory.EQUIPMENT,
            message = "Áp suất khí nén thấp",
            description = "Áp suất hệ thống khí nén dưới mức yêu cầu",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),

        // Nhóm lỗi Safety (21-30)
        AlarmConfig(
            alarmCode = 21,
            priority = AlarmPriority.CRITICAL,
            category = AlarmCategory.SAFETY,
            message = "Cửa an toàn mở",
            description = "Cửa bảo vệ khu vực làm việc đang mở khi hệ thống hoạt động",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 22,
            priority = AlarmPriority.EMERGENCY,
            category = AlarmCategory.SAFETY,
            message = "Nút dừng khẩn cấp được kích hoạt",
            description = "Emergency Stop button đã được nhấn",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 23,
            priority = AlarmPriority.HIGH,
            category = AlarmCategory.SAFETY,
            message = "Light curtain bị chặn",
            description = "Rào cản ánh sáng an toàn phát hiện vật cản",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        ),

        // Nhóm lỗi Communication (31-40)
        AlarmConfig(
            alarmCode = 31,
            priority = AlarmPriority.MEDIUM,
            category = AlarmCategory.COMMUNICATION,
            message = "Mất kết nối với sensor vị trí",
            description = "Không nhận được tín hiệu từ sensor detect vị trí pallet",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 32,
            priority = AlarmPriority.HIGH,
            category = AlarmCategory.COMMUNICATION,
            message = "Mất kết nối với PLC phụ",
            description = "Không thể giao tiếp với PLC điều khiển khu vực 2",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 33,
            priority = AlarmPriority.LOW,
            category = AlarmCategory.COMMUNICATION,
            message = "Chất lượng tín hiệu kém",
            description = "Tín hiệu từ encoder có nhiễu",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),

        // Nhóm lỗi System (41-50)
        AlarmConfig(
            alarmCode = 41,
            priority = AlarmPriority.LOW,
            category = AlarmCategory.SYSTEM,
            message = "Bộ nhớ PLC gần đầy",
            description = "Dung lượng bộ nhớ PLC còn lại dưới 10%",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 42,
            priority = AlarmPriority.MEDIUM,
            category = AlarmCategory.SYSTEM,
            message = "Pin backup yếu",
            description = "Pin lưu trữ dữ liệu PLC cần thay thế",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),

        // Nhóm lỗi Maintenance (51-60)
        AlarmConfig(
            alarmCode = 51,
            priority = AlarmPriority.LOW,
            category = AlarmCategory.MAINTENANCE,
            message = "Đến hạn bảo trì định kỳ",
            description = "Hệ thống cần bảo trì theo lịch",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 52,
            priority = AlarmPriority.MEDIUM,
            category = AlarmCategory.MAINTENANCE,
            message = "Bộ lọc cần làm sạch",
            description = "Bộ lọc khí nén cần được làm sạch hoặc thay thế",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),

        // Nhóm lỗi Operator (61-70)
        AlarmConfig(
            alarmCode = 61,
            priority = AlarmPriority.LOW,
            category = AlarmCategory.OPERATOR,
            message = "Thao tác không hợp lệ",
            description = "Người vận hành thực hiện thao tác không đúng quy trình",
            enabled = true,
            soundEnabled = false,
            createdBy = "system"
        ),
        AlarmConfig(
            alarmCode = 62,
            priority = AlarmPriority.MEDIUM,
            category = AlarmCategory.OPERATOR,
            message = "Chưa xác nhận lệnh",
            description = "Đang chờ người vận hành xác nhận để tiếp tục",
            enabled = true,
            soundEnabled = true,
            createdBy = "system"
        )
    )

    /**
     * Get alarm config by code
     */
    fun getConfigByCode(code: Int): AlarmConfig? {
        return DEFAULT_ALARM_CONFIGS.find { it.alarmCode == code }
    }

    /**
     * Get all configs for a specific category
     */
    fun getConfigsByCategory(category: AlarmCategory): List<AlarmConfig> {
        return DEFAULT_ALARM_CONFIGS.filter { it.category == category }
    }

    /**
     * Get all configs for a specific priority
     */
    fun getConfigsByPriority(priority: AlarmPriority): List<AlarmConfig> {
        return DEFAULT_ALARM_CONFIGS.filter { it.priority == priority }
    }
}