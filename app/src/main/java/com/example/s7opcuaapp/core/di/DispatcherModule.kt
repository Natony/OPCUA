package com.example.s7opcuaapp.core.di

import com.example.s7opcuaapp.core.dispatchers.DispatcherProvider
import com.example.s7opcuaapp.core.dispatchers.StandardDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides coroutine dispatchers
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = StandardDispatcherProvider()
}