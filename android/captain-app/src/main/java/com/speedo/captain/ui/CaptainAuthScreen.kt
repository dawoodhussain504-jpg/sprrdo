package com.speedo.captain.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.core.components.SpeedoPrimaryButton
import com.speedo.core.components.SpeedoTextField
import com.speedo.core.theme.*
import com.speedo.captain.viewmodel.CaptainViewModel

@Composable
fun CaptainAuthScreen(
    viewModel: CaptainViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSignUp by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("captain@speedo.com") }
    var password by remember { mutableStateOf("Captain@123") }
    var phone by remember { mutableStateOf("+919876543210") }
    var vehicleType by remember { mutableStateOf("bike") }
    var vehicleNumber by remember { mutableStateOf("KA-01-EQ-9876") }
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
            com.speedo.core.components.SpeedoAppIconBadge(sizeDp = 72)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Captian",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SpeedoOrange,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = if (isSignUp) "Register vehicle and start earning" else "Drive with Speedo & earn daily",
                style = MaterialTheme.typography.bodyMedium,
                color = SpeedoTextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (isSignUp) {
                SpeedoTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Captain Full Name",
                    placeholder = "Rajesh Kumar",
                    leadingIcon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))

                SpeedoTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Mobile Number",
                    placeholder = "+91 9876543210",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Vehicle Type Segment
                Text(
                    text = "Select Vehicle Type",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("bike" to "Speedo Moto", "auto" to "Speedo Toto", "cab" to "Speedo 4").forEach { (key, label) ->
                        val selected = vehicleType == key
                        Button(
                            onClick = { vehicleType = key },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) SpeedoOrange else SpeedoSurfaceVariant,
                                contentColor = if (selected) SpeedoWhite else SpeedoTextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                SpeedoTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it.uppercase() },
                    label = "Vehicle Registration No.",
                    placeholder = "KA-01-EQ-9876",
                    leadingIcon = Icons.Default.Pin
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            SpeedoTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "captain@speedo.com",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            SpeedoPrimaryButton(
                text = if (isSignUp) "Register Captain" else "Sign In as Captain",
                isLoading = uiState.isLoading,
                onClick = {
                    if (isSignUp) {
                        viewModel.register(name, email, password, phone, vehicleType, vehicleNumber) { success ->
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
                    text = if (isSignUp) "Already a registered captain? " else "Want to drive with Speedo? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpeedoTextSecondary
                )
                Text(
                    text = if (isSignUp) "Sign In" else "Sign Up",
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
