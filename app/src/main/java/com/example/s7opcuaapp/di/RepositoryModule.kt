package com.example.s7opcuaapp.di

import com.example.s7opcuaapp.data.repository.LogRepository
import com.example.s7opcuaapp.data.repository.LogRepositoryImpl
import com.example.s7opcuaapp.data.repository.OPCUARepositoryImpl
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.data.repository.UserRepository
import com.example.s7opcuaapp.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindLogRepository(
        logRepositoryImpl: LogRepositoryImpl
    ): LogRepository

    @Binds
    @Singleton
    abstract fun bindS7Repository(
        impl: OPCUARepositoryImpl
    ): S7Repository

}