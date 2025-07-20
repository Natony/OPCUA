package com.example.s7opcuaapp.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.s7opcuaapp.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin = viewModel.isAdmin()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang chủ") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome message
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Xin chào, ${currentUser?.username ?: "User"}!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Vai trò: ${currentUser?.role?.name ?: "N/A"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Menu grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Common items
                item {
                    MenuCard(
                        title = "Điều khiển",
                        icon = Icons.Default.ControlCamera,
                        onClick = { navController.navigate("control") }
                    )
                }

                item {
                    MenuCard(
                        title = "Cảnh báo",
                        icon = Icons.Default.Warning,
                        onClick = { navController.navigate("alarm") }
                    )
                }

                item {
                    MenuCard(
                        title = "Cấu hình thiết bị",
                        icon = Icons.Default.Settings,
                        onClick = { navController.navigate("config_btm") }
                    )
                }

                // Admin only items
                if (isAdmin) {
                    item {
                        MenuCard(
                            title = "Quản lý người dùng",
                            icon = Icons.Default.People,
                            onClick = { navController.navigate("user_manager") }
                        )
                    }

                    item {
                        MenuCard(
                            title = "Lịch sử đăng nhập",
                            icon = Icons.Default.History,
                            onClick = { navController.navigate("login_history") }
                        )
                    }

                    item {
                        MenuCard(
                            title = "Cấu hình khóa",
                            icon = Icons.Default.Lock,
                            onClick = { navController.navigate("status_lock_config") },
                            isNew = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isNew: Boolean = false
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            // New badge
            if (isNew) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text("MỚI")
                }
            }
        }
    }
}