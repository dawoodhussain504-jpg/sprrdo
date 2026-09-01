package com.speedo.admin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoServerConfigDialog
import com.speedo.core.components.SpeedoTextField
import com.speedo.core.theme.*

@Composable
fun AdminLoginScreen(
    viewModel: AdminViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showServerDialog by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("admin@speedo.com") }
    var password by remember { mutableStateOf("Admin@123") }
    var passwordVisible by remember { mutableStateOf(false) }

    if (showServerDialog) {
        SpeedoServerConfigDialog(
            onDismissRequest = { showServerDialog = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpeedoWhite
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { showServerDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Dns, contentDescription = "Server Settings", tint = SpeedoTextSecondary)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                com.speedo.core.components.SpeedoAppIconBadge(sizeDp = 72)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Authority",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SpeedoOrange,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = "Platform KYC moderation & live operations",
                style = MaterialTheme.typography.bodyMedium,
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            SpeedoTextField(
                value = email,
                onValueChange = { email = it },
                label = "Admin Email",
                placeholder = "admin@speedo.com",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(14.dp))

            SpeedoTextField(
                value = password,
                onValueChange = { password = it },
                label = "Admin Password",
                placeholder = "••••••••",
                leadingIcon = Icons.Default.Lock,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = SpeedoTextTertiary
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpeedoErrorContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = SpeedoError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SpeedoPrimaryButton(
                text = "Sign In as Administrator",
                isLoading = uiState.isLoading,
                onClick = {
                    viewModel.login(email, password) { success ->
                        if (success) onLoginSuccess()
                    }
                }
            )
        }
    }
}
}
