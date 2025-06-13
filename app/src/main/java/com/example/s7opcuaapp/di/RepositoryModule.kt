package com.example.s7opcuaapp.di

import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.repository.OPCUARepositoryImpl
import com.example.s7opcuaapp.data.repository.S7Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Singleton
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {

    /**
     * Tuỳ chỉnh: Lấy DeviceEntity hiện tại từ PrefsManager,
     * nếu useOpcUa = true thì trả về instance OPCUARepositoryImpl(device).
     * Bạn có thể bổ sung thêm repository dạng khác nếu cần.
     */
    @Provides
    @ViewModelScoped
    fun provideS7Repository(prefsManager: PrefsManager): S7Repository {
        val device: DeviceEntity = prefsManager.getCurrentDevice()
            ?: DeviceEntity(
                id = "default",
                name = "PLC Default",
                ipAddress = "192.168.1.12",
                useOpcUa = true
            )
        return OPCUARepositoryImpl(device)
    }
}
