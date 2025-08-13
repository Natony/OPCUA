// app/src/main/java/com/example/s7opcuaapp/data/model/Alarm.kt
package com.example.s7opcuaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "alarms",
    indices = [
        Index(value = ["alarmCode"]),
        Index(value = ["priority"]),
        Index(value = ["state"]),
        Index(value = ["timestamp"])
    ]
)
data class Alarm(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val alarmCode: Int,           // Mã lỗi từ PLC (1, 2, 3...)
    val priority: AlarmPriority,   // Mức độ ưu tiên
    val category: AlarmCategory,   // Loại alarm
    val message: String,           // Nội dung alarm
    val description: String = "",  // Mô tả chi tiết
    val state: AlarmState,         // Trạng thái hiện tại
    val timestamp: Long = System.currentTimeMillis(),
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val clearedAt: Long? = null,
    val deviceId: String,
    val tagName: String = "",      // Tag/Node gây ra alarm
    val value: String = "",         // Giá trị khi alarm xảy ra
    val setpoint: String = "",     // Ngưỡng alarm
    val suppressed: Boolean = false, // Tạm ẩn alarm
    val shelved: Boolean = false,   // Tạm hoãn alarm
    val shelvedUntil: Long? = null
)

enum class AlarmPriority(val level: Int, val color: Long) {
    LOW(1, 0xFF4CAF50),        // Green - Thông tin
    MEDIUM(2, 0xFFFFC107),     // Amber - Cảnh báo
    HIGH(3, 0xFFFF9800),       // Orange - Lỗi
    CRITICAL(4, 0xFFF44336),   // Red - Nguy hiểm
    EMERGENCY(5, 0xFF9C27B0)   // Purple - Khẩn cấp
}

enum class AlarmCategory {
    PROCESS,        // Lỗi quy trình
    EQUIPMENT,      // Lỗi thiết bị
    SAFETY,         // An toàn
    COMMUNICATION,  // Kết nối
    SYSTEM,         // Hệ thống
    MAINTENANCE,    // Bảo trì
    OPERATOR        // Vận hành
}

enum class AlarmState {
    ACTIVE,         // Đang active
    ACKNOWLEDGED,   // Đã xác nhận nhưng chưa clear
    CLEARED,        // Đã clear nhưng chưa xác nhận
    NORMAL          // Đã xử lý xong (cleared & acknowledged)
}