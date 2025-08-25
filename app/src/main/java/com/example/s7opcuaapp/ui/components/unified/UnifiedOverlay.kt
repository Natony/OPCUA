package com.example.s7opcuaapp.ui.components.unified

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.ui.theme.UiConfig

enum class OverlayType {
    LOADING,
    ERROR,
    CONNECTION,
    OFFLINE,
    TIMEOUT,
    CONNECTION_LOST
}

data class OverlayConfig(
    val type: OverlayType,
    val message: String,
    val showProgress: Boolean = false,
    val progressValue: Int? = null,
    val isIndeterminate: Boolean = true,
    val showRetry: Boolean = false,
    val showSettings: Boolean = false,
    val showOfflineMode: Boolean = false,
    val countdown: Int? = null,
    val onRetry: (() -> Unit)? = null,
    val onSettings: (() -> Unit)? = null,
    val onOffline: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
)

@Composable
fun UnifiedOverlay(
    config: OverlayConfig,
    modifier: Modifier = Modifier
) {
    when (config.type) {
        OverlayType.LOADING -> LoadingOverlay(config, modifier)
        OverlayType.ERROR -> ErrorOverlay(config, modifier)
        OverlayType.CONNECTION -> ConnectionOverlay(config, modifier)
        OverlayType.OFFLINE -> OfflineOverlay(config, modifier)
        OverlayType.TIMEOUT -> TimeoutOverlay(config, modifier)
        OverlayType.CONNECTION_LOST -> ConnectionLostOverlay(config, modifier)
    }
}

@Composable
private fun BaseOverlay(
    alpha: Float = UiConfig.Overlay.DEFAULT_ALPHA,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
            .pointerInput(Unit) {
                awaitEachGesture { /* Block touches */ }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun LoadingOverlay(
    config: OverlayConfig,
    modifier: Modifier = Modifier
) {
    BaseOverlay(alpha = UiConfig.Overlay.LOADING_ALPHA) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (config.isIndeterminate || config.progressValue == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                CircularProgressIndicator(
                    progress = config.progressValue / 100f,
                    modifier = Modifier.size(64.dp)
                )
            }

            Text(
                text = config.message,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )

            config.progressValue?.let {
                Text(
                    text = "$it%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ErrorOverlay(
    config: OverlayConfig,
    modifier: Modifier = Modifier
) {
    BaseOverlay(alpha = UiConfig.Overlay.ERROR_ALPHA) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Text(
                    text = config.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                if (config.showRetry) {
                    Button(onClick = { config.onRetry?.invoke() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionOverlay(
    config: OverlayConfig,
    modifier: Modifier = Modifier
) {
    BaseOverlay(alpha = UiConfig.Overlay.DEFAULT_ALPHA) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (config.showProgress) {
                    CircularProgressIndicator()
                }

                Text(
                    text = config.message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                if (config.showRetry) {
                    Button(onClick = { config.onRetry?.invoke() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineOverlay(
    config: OverlayConfig,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = UiConfig.Overlay.OFFLINE_ALPHA))
            .pointerInput(Unit) {
                awaitEachGesture { /* Block touches */ }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Offline Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = config.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.showRetry) {
                        TextButton(
                            onClick = { config.onRetry?.invoke() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Retry")
                        }
                    }

                    if (config.showSettings) {
                        IconButton(
                            onClick = { config.onSettings?.invoke() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Config",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeoutOverlay(
    config: OverlayConfig,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = { /* Can't dismiss */ },
        icon = {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Connection Failed",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = config.message,
                    style = MaterialTheme.typography.bodyLarge
                )

                config.countdown?.let { countdown ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = countdown / 30f,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Auto-returning to config in ${countdown}s...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (config.showRetry) {
                    Button(onClick = { config.onRetry?.invoke() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry")
                    }
                }

                if (config.showSettings) {
                    OutlinedButton(onClick = { config.onSettings?.invoke() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Config")
                    }
                }
            }
        },
        dismissButton = {
            if (config.showOfflineMode) {
                TextButton(onClick = { config.onOffline?.invoke() }) {
                    Text("Continue Offline")
                }
            }
        }
    )
}

@Composable
private fun ConnectionLostOverlay(
    config: OverlayConfig,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = config.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (config.showRetry) {
                    TextButton(
                        onClick = { config.onRetry?.invoke() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Retry Now")
                    }
                }
            }
        }
    }
}