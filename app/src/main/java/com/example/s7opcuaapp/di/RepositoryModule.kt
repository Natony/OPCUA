package com.example.s7opcuaapp.di

import com.example.s7opcuaapp.data.buffer.PlcDataBuffer
import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.repository.*
import com.example.s7opcuaapp.util.PerformanceMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePerformanceMonitor(): PerformanceMonitor {
        return PerformanceMonitor()
    }

    @Provides
    @Singleton
    fun providePlcDataBuffer(
        performanceMonitor: PerformanceMonitor
    ): PlcDataBuffer {
        return PlcDataBuffer(performanceMonitor)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        database: AppDatabase,
        prefsManager: PrefsManager
    ): UserRepository {
        return UserRepositoryImpl(database, prefsManager)
    }

    @Provides
    @Singleton
    fun provideLogRepository(
        database: AppDatabase
    ): LogRepository {
        return LogRepositoryImpl(database)
    }

    // Remove @Singleton from S7Repository since it depends on device config
    @Provides
    fun provideS7Repository(
        prefsManager: PrefsManager,
        database: AppDatabase,
        dataBuffer: PlcDataBuffer,
        performanceMonitor: PerformanceMonitor
    ): S7Repository {
        val device = prefsManager.getCurrentDevice() ?: DeviceEntity(
            id = "default",
            name = "Default Device",
            ipAddress = "192.168.1.100",
            port = 4840,
            opcUsername = "",
            opcPassword = "",
            useOpcUa = true
        )

        return OptimizedOPCUARepositoryImpl(
            device = device,
            database = database,
            dataBuffer = dataBuffer,
            performanceMonitor = performanceMonitor
        )
    }
}