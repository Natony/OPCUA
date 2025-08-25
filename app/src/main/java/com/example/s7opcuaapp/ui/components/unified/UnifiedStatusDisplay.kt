package com.example.s7opcuaapp.ui.components.unified

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s7opcuaapp.ui.theme.UiConfig

enum class StatusDisplayType {
    BOOLEAN,      // On/Off status
    MULTI_STATE,  // Multiple states with icons
    NUMERIC,      // Numeric value display
    TEXT,         // Text status
    BATTERY,      // Battery level
    INLINE_NUMBER // Inline number with underline
}

data class StatusDisplayConfig(
    val type: StatusDisplayType,
    val label: String? = null,
    val value: Any,
    val icons: List<Int>? = null,
    val statuses: List<String>? = null,
    val thresholds: List<Int>? = null,
    val showIndicator: Boolean = false,
    val isCompact: Boolean = false
)

@Composable
fun UnifiedStatusDisplay(
    config: StatusDisplayConfig,
    modifier: Modifier = Modifier
) {
    when (config.type) {
        StatusDisplayType.BOOLEAN -> BooleanStatus(config, modifier)
        StatusDisplayType.MULTI_STATE -> MultiStateStatus(config, modifier)
        StatusDisplayType.NUMERIC -> NumericStatus(config, modifier)
        StatusDisplayType.TEXT -> TextStatus(config, modifier)
        StatusDisplayType.BATTERY -> BatteryStatus(config, modifier)
        StatusDisplayType.INLINE_NUMBER -> InlineNumberStatus(config, modifier)
    }
}

@Composable
private fun BooleanStatus(
    config: StatusDisplayConfig,
    modifier: Modifier = Modifier
) {
    val value = config.value as? Boolean ?: false
    val icons = config.icons ?: emptyList()

    if (config.isCompact) {
        Icon(
            painter = painterResource(
                id = if (value) icons[0] else icons.getOrNull(1) ?: icons[0]
            ),
            contentDescription = null,
            modifier = modifier.size(UiConfig.Status.ICON_SIZE),
            tint = Color.Unspecified
        )
    } else {
        Box(
            modifier = modifier
                .size(156.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (value) icons[0] else icons.getOrNull(1) ?: icons[0]
                ),
                contentDescription = null,
                modifier = Modifier.size(156.dp),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun MultiStateStatus(
    config: StatusDisplayConfig,
    modifier: Modifier = Modifier
) {
    val intValue = config.value as? Int ?: 0
    val icons = config.icons ?: emptyList()
    val iconRes = icons.getOrElse(intValue) { icons.lastOrNull() ?: 0 }
    val isActive = intValue != 0

    if (config.isCompact) {
        Box(
            modifier = modifier.size(UiConfig.Buttons.SMALL_SIZE),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(UiConfig.Buttons.SMALL_SIZE),
                tint = Color.Unspecified
            )
        }
    } else {
        val bg = if (isActive)
            MaterialTheme.colorScheme.primary.copy(alpha = UiConfig.Status.ACTIVE_ALPHA)
        else MaterialTheme.colorScheme.surface

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isActive) 4.dp else 1.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isActive)
                                MaterialTheme.colorScheme.primary.copy(alpha = UiConfig.Status.ACTIVE_ALPHA)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    config.label?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                            color = if (isActive)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = intValue.toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (config.showIndicator) {
                    Box(
                        modifier = Modifier
                            .size(UiConfig.Status.INDICATOR_SIZE)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isActive)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericStatus(
    config: StatusDisplayConfig,
    modifier: Modifier = Modifier
) {
    val value = config.value as? Int ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        config.label?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString(),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun TextStatus(
    config: StatusDisplayConfig,
    modifier: Modifier = Modifier
) {
    val intValue = config.value as? Int ?: 0
    val statuses = config.statuses ?: emptyList()
    val idx = intValue.coerceIn(0, statuses.size - 1)

    Column(
        modifier = modifier
            .wrapContentHeight()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        config.label?.let {
            if (it.isNotEmpty()) {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statuses.getOrNull(idx) ?: "",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BatteryStatus(
    config: StatusDisplayConfig,
    modifier: Modifier = Modifier
) {
    val level = (config.value as? Int ?: 0).coerceIn(0, 100)
    val thresholds = config.thresholds ?: listOf(20, 80)
    val icons = config.icons ?: emptyList()

    val idx = when {
        icons.size != thresholds.size + 1 -> 0
        else -> {
            thresholds.indexOfFirst { level < it }.let {
                if (it == -1) thresholds.size else it
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icons.getOrNull(idx) ?: 0),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = Color.Unspecified
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$level%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InlineNumberStatus(
    config: StatusDisplayConfig,
    modifier: Modifier = Modifier
) {
    val value = config.value.toString()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.Gray
                )
            }
        }
    }
}