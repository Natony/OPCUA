package com.example.s7opcuaapp.ui.state

import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages UI state with loading, error, and success states
 */
@Singleton
class UiStateManager @Inject constructor() {

    sealed class UiState<out T> {
        object Loading : UiState<Nothing>()
        data class Success<T>(val data: T) : UiState<T>()
        data class Error(val exception: Throwable) : UiState<Nothing>()
        object Empty : UiState<Nothing>()
    }

    /**
     * Convert Flow to UI state
     */
    fun <T> Flow<T>.asUiState(): Flow<UiState<T>> = map<T, UiState<T>> {
        UiState.Success(it)
    }.onStart {
        emit(UiState.Loading)
    }.catch {
        emit(UiState.Error(it))
    }

    /**
     * Convert Result to UI state
     */
    fun <T> Result<T>.toUiState(): UiState<T> = fold(
        onSuccess = { UiState.Success(it) },
        onFailure = { UiState.Error(it) }
    )

    /**
     * Handle UI state in composable
     */
    inline fun <T> UiState<T>.handle(
        onLoading: () -> Unit = {},
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {},
        onEmpty: () -> Unit = {}
    ) {
        when (this) {
            is UiState.Loading -> onLoading()
            is UiState.Success -> onSuccess(data)
            is UiState.Error -> onError(exception)
            is UiState.Empty -> onEmpty()
        }
    }
}