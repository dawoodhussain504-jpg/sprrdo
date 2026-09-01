package com.speedo.core.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.speedo.core.network.RetrofitClient
import com.speedo.core.socket.SpeedoSocketManager
import com.speedo.core.theme.*
import com.speedo.core.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun SpeedoServerConfigDialog(
    onDismissRequest: () -> Unit,
    onServerChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUrl by remember { mutableStateOf(Constants.getBaseUrl(context)) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val presets = listOf(
        "Railway Cloud (Live)" to "https://web-production-5d826.up.railway.app/api/",
        "Local WiFi (PC IP)" to "http://10.181.215.97:5000/api/",
        "Android Emulator" to "http://10.0.2.2:5000/api/",
        "Localhost (Port 5000)" to "http://localhost:5000/api/"
    )

    fun testConnectivity(urlToTest: String) {
        isTesting = true
        testResult = "Pinging $urlToTest..."
        scope.launch {
            val startTime = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                try {
                    val healthUrl = if (urlToTest.endsWith("/")) "${urlToTest}health" else "$urlToTest/health"
                    val connection = URL(healthUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    connection.requestMethod = "GET"
                    val code = connection.responseCode
                    val elapsed = System.currentTimeMillis() - startTime
                    if (code in 200..299) {
                        Pair(true, "✅ Connected (Status: $code, ${elapsed}ms)")
                    } else {
                        Pair(false, "⚠️ Server returned HTTP $code (${elapsed}ms)")
                    }
                } catch (e: Exception) {
                    Pair(false, "❌ Connection failed: ${e.localizedMessage ?: "Timeout"}")
                }
            }
            isTesting = false
            isSuccess = result.first
            testResult = result.second
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpeedoWhite,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            tint = SpeedoOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Server Endpoint",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SpeedoTextSecondary)
                    }
                }

                Text(
                    text = "Select or enter your backend server URL for seamless login & live tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpeedoTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth(),
                    color = SpeedoTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                presets.forEach { (label, presetUrl) ->
                    val isSelected = currentUrl.trim().equals(presetUrl.trim(), ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SpeedoOrange.copy(alpha = 0.1f) else SpeedoSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) SpeedoOrange else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                currentUrl = presetUrl
                                testConnectivity(presetUrl)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SpeedoOrange else SpeedoTextPrimary
                                    )
                                )
                                Text(
                                    text = presetUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpeedoTextSecondary,
                                    maxLines = 1
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpeedoOrange)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom URL Input
                OutlinedTextField(
                    value = currentUrl,
                    onValueChange = {
                        currentUrl = it
                        testResult = null
                    },
                    label = { Text("API Base URL") },
                    placeholder = { Text("https://your-api.com/api/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (testResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testResult!!,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = if (isSuccess) SpeedoSuccess else SpeedoError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { testConnectivity(currentUrl) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Test Ping")
                        }
                    }

                    Button(
                        onClick = {
                            val formatted = if (currentUrl.endsWith("/")) currentUrl else "$currentUrl/"
                            Constants.setCustomBaseUrl(context, formatted)
                            RetrofitClient.resetService()
                            SpeedoSocketManager.getInstance(context).disconnect()
                            SpeedoSocketManager.getInstance(context).connect()
                            onServerChanged(formatted)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply & Save", color = SpeedoWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
