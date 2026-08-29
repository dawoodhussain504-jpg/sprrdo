package com.speedo.rider.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTextField
import com.speedo.core.theme.*
import com.speedo.rider.viewmodel.RiderViewModel

@Composable
fun RiderAuthScreen(
    viewModel: RiderViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSignUp by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("rider@speedo.com") }
    var password by remember { mutableStateOf("Rider@123") }
    var phone by remember { mutableStateOf("+919988776655") }
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpeedoWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Speedo Brand Header
            com.speedo.core.components.SpeedoAppIconBadge(sizeDp = 72)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Speedo",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SpeedoOrange,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = if (isSignUp) "Create your rider account to start booking" else "Book bike, auto & cab rides in seconds",
                style = MaterialTheme.typography.bodyMedium,
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            if (isSignUp) {
                SpeedoTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    placeholder = "e.g. Sneha Patel",
                    leadingIcon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(14.dp))

                SpeedoTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    placeholder = "+91 9876543210",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            SpeedoTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "rider@speedo.com",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(14.dp))

            SpeedoTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
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

            // Error Message
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SpeedoErrorContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
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
                text = if (isSignUp) "Create Rider Account" else "Sign In as Rider",
                isLoading = uiState.isAuthLoading,
                onClick = {
                    if (isSignUp) {
                        viewModel.register(name, email, password, phone) { success ->
                            if (success) onAuthSuccess()
                        }
                    } else {
                        viewModel.login(email, password) { success ->
                            if (success) onAuthSuccess()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isSignUp) "Already have an account? " else "New to Speedo? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpeedoTextSecondary
                )
                Text(
                    text = if (isSignUp) "Sign In" else "Register Now",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SpeedoOrange
                    ),
                    modifier = Modifier.clickable {
                        isSignUp = !isSignUp
                        viewModel.clearMessages()
                    }
                )
            }
        }
    }
}
