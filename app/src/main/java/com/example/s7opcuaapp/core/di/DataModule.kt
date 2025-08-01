package com.example.s7opcuaapp.core.di

import com.example.s7opcuaapp.data.opcua.OpcUaConnectionPool
import com.example.s7opcuaapp.data.opcua.OpcUaSubscriptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideOpcUaConnectionPool(): OpcUaConnectionPool = OpcUaConnectionPool()

    @Provides
    @Singleton
    fun provideOpcUaSubscriptionManager(): OpcUaSubscriptionManager = OpcUaSubscriptionManager()
}