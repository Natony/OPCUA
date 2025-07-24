// app/src/main/java/com/example/s7opcuaapp/ui/components/UserDialogs.kt

package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.data.model.UserRole
import com.example.s7opcuaapp.util.PasswordUtils
import androidx.compose.ui.graphics.Color // Thêm import này

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUserDialog(
    isEdit: Boolean,
    username: String,
    password: String,
    confirmPassword: String,
    role: UserRole,
    passwordStrength: PasswordUtils.PasswordStrength?,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Chỉnh sửa người dùng" else "Thêm người dùng")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Tên đăng nhập") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEdit
                )

                if (!isEdit) {
                    // Password field với helper text
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Column {
                                Text(
                                    "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ và số",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                passwordStrength?.let { strength ->
                                    Text(
                                        text = when (strength) {
                                            PasswordUtils.PasswordStrength.WEAK -> "Mật khẩu yếu"
                                            PasswordUtils.PasswordStrength.MEDIUM -> "Mật khẩu trung bình"
                                            PasswordUtils.PasswordStrength.STRONG -> "Mật khẩu mạnh"
                                            else -> "Mật khẩu rất mạnh" // Dùng else để cover mọi trường hợp
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (strength) {
                                            PasswordUtils.PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
                                            PasswordUtils.PasswordStrength.MEDIUM -> Color(0xFFFF9800)
                                            PasswordUtils.PasswordStrength.STRONG -> Color(0xFF4CAF50)
                                            else -> Color(0xFF2196F3) // Màu xanh dương cho rất mạnh
                                        }
                                    )
                                }
                            }
                        },
                        isError = password.isNotEmpty() && !PasswordUtils.isValidPassword(password)
                    )

                    // Confirm password field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Xác nhận mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                        supportingText = {
                            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                Text(
                                    "Mật khẩu không khớp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }

                // Role selector
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (role) {
                            UserRole.ADMIN -> "Admin"
                            UserRole.OPERATOR -> "Operator"
                            UserRole.VIEWER -> "Viewer"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vai trò") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Admin") },
                            onClick = {
                                onRoleChange(UserRole.ADMIN)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Operator") },
                            onClick = {
                                onRoleChange(UserRole.OPERATOR)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Viewer") },
                            onClick = {
                                onRoleChange(UserRole.VIEWER)
                                expanded = false
                            }
                        )
                    }
                }

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    username: String,
    newPassword: String,
    confirmPassword: String,
    passwordStrength: PasswordUtils.PasswordStrength?,
    errorMessage: String?,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Đổi mật khẩu cho $username")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("Mật khẩu mới") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Column {
                            Text(
                                "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ và số",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            passwordStrength?.let { strength ->
                                Text(
                                    text = when (strength) {
                                        PasswordUtils.PasswordStrength.WEAK -> "Mật khẩu yếu"
                                        PasswordUtils.PasswordStrength.MEDIUM -> "Mật khẩu trung bình"
                                        PasswordUtils.PasswordStrength.STRONG -> "Mật khẩu mạnh"
                                        else -> "Mật khẩu rất mạnh" // Dùng else để cover mọi trường hợp
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (strength) {
                                        PasswordUtils.PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
                                        PasswordUtils.PasswordStrength.MEDIUM -> Color(0xFFFF9800)
                                        PasswordUtils.PasswordStrength.STRONG -> Color(0xFF4CAF50)
                                        else -> Color(0xFF2196F3) // Màu xanh dương cho rất mạnh
                                    }
                                )
                            }
                        }
                    },
                    isError = newPassword.isNotEmpty() && !PasswordUtils.isValidPassword(newPassword)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Xác nhận mật khẩu") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                            Text(
                                "Mật khẩu không khớp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    username: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Xác nhận xóa")
        },
        text = {
            Text("Bạn có chắc chắn muốn xóa người dùng '$username'?")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Xóa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}