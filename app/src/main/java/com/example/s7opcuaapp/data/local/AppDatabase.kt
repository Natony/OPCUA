// app/src/main/java/com/example/s7opcuaapp/data/local/AppDatabase.kt
package com.example.s7opcuaapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.s7opcuaapp.data.model.*
import com.example.s7opcuaapp.data.local.dao.*
import com.example.s7opcuaapp.data.model.alarm.Alarm
import com.example.s7opcuaapp.data.model.alarm.AlarmConfig

@Database(
    entities = [
        User::class,
        LoginHistory::class,
        DeviceAccessLog::class,
        DeviceEntity::class,
        Alarm::class,           // Add this
        AlarmConfig::class      // Add this
    ],
    version = 2,  // Increment version
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun loginHistoryDao(): LoginHistoryDao
    abstract fun deviceAccessLogDao(): DeviceAccessLogDao
    abstract fun deviceDao(): DeviceDao
    abstract fun alarmDao(): AlarmDao           // Add this
    abstract fun alarmConfigDao(): AlarmConfigDao  // Add this

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "s7opcua_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}