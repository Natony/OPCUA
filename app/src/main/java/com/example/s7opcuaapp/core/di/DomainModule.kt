package com.example.s7opcuaapp.core.di

import com.example.s7opcuaapp.domain.button.ButtonActionHandler
import com.example.s7opcuaapp.domain.button.ButtonActionHandlerImpl
import com.example.s7opcuaapp.domain.connection.ConnectionManager
import com.example.s7opcuaapp.domain.connection.ConnectionManagerImpl
import com.example.s7opcuaapp.domain.state.PlcStateManager
import com.example.s7opcuaapp.domain.state.PlcStateManagerImpl
import com.example.s7opcuaapp.domain.sync.DataSyncManager
import com.example.s7opcuaapp.domain.sync.DataSyncManagerImpl
import com.example.s7opcuaapp.domain.validation.ButtonValidator
import com.example.s7opcuaapp.domain.validation.ButtonValidatorImpl
import com.example.s7opcuaapp.presentation.coordinator.ControlCoordinator
import com.example.s7opcuaapp.presentation.coordinator.ControlCoordinatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for domain layer dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindConnectionManager(
        impl: ConnectionManagerImpl
    ): ConnectionManager

    @Binds
    @Singleton
    abstract fun bindButtonActionHandler(
        impl: ButtonActionHandlerImpl
    ): ButtonActionHandler

    @Binds
    @Singleton
    abstract fun bindPlcStateManager(
        impl: PlcStateManagerImpl
    ): PlcStateManager

    @Binds
    @Singleton
    abstract fun bindDataSyncManager(
        impl: DataSyncManagerImpl
    ): DataSyncManager

    @Binds
    @Singleton
    abstract fun bindButtonValidator(
        impl: ButtonValidatorImpl
    ): ButtonValidator

    @Binds
    @Singleton
    abstract fun bindControlCoordinator(
        impl: ControlCoordinatorImpl
    ): ControlCoordinator
}