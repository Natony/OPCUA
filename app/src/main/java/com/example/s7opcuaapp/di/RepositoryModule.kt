package com.example.s7opcuaapp.di

import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.repository.LogRepository
import com.example.s7opcuaapp.data.repository.LogRepositoryImpl
import com.example.s7opcuaapp.data.repository.OPCUARepositoryImpl
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.data.repository.UserRepository
import com.example.s7opcuaapp.data.repository.UserRepositoryImpl
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

    @Provides
    @Singleton
    fun provideS7Repository(
        prefsManager: PrefsManager,
        database: AppDatabase
    ): S7Repository {
        // Get current device or create a default one
        val device = prefsManager.getCurrentDevice() ?: DeviceEntity(
            id = "default",
            name = "Default Device",
            ipAddress = "192.168.1.100",
            port = 4840,
            opcUsername = "",
            opcPassword = "",
            useOpcUa = true
        )

        return OPCUARepositoryImpl(device, database)
    }
}