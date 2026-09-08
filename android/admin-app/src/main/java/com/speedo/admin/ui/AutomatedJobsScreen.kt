package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.model.CronJobItem
import com.speedo.core.theme.*

@Composable
fun AutomatedJobsScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val cronStatus = uiState.cronStatus
    val triggeringMap = uiState.isTriggeringCronJob

    LaunchedEffect(Unit) {
        viewModel.fetchCronStatus()
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Automated Cron Jobs",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchCronStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SpeedoTextPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SpeedoBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO STATUS & IST CLOCK BANNER
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SpeedoWhite),
                    border = BorderStroke(1.dp, SpeedoOrange.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SpeedoOrangeContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Cron Engine",
                                        tint = SpeedoOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Speedo Automation Engine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SpeedoTextPrimary
                                    )
                                    Text(
                                        text = "Timezone: Asia/Kolkata (IST)",
                                        fontSize = 12.sp,
                                        color = SpeedoTextSecondary
                                    )
                                }
                            }

                            // Active routines chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = SpeedoSuccessContainer
                            ) {
                                Text(
                                    text = "● 6 Active",
                                    color = SpeedoSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SpeedoDivider)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Current Server IST Time Display
                        val istDisplay = cronStatus?.currentIstTime?.ifEmpty { null } ?: "Syncing with cloud server..."
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Current IST: $istDisplay",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SpeedoTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Statistics Pills Row
                        val totalJobs = cronStatus?.totalJobs ?: 6
                        val totalRuns = cronStatus?.jobs?.sumOf { it.totalRuns } ?: 0
                        val totalNotifs = cronStatus?.jobs?.sumOf { it.totalNotificationsSent } ?: 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatPill(
                                label = "Total Jobs",
                                value = "$totalJobs",
                                icon = Icons.Default.FormatListNumbered,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Executions",
                                value = "$totalRuns",
                                icon = Icons.Default.PlayArrow,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Sent Notifs",
                                value = "$totalNotifs",
                                icon = Icons.Default.NotificationsActive,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Trigger All Quick Button
                        val isTriggeringAll = triggeringMap["all"] == true
                        Button(
                            onClick = { viewModel.triggerCronRoutine("all") },
                            enabled = !isTriggeringAll,
                            colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTriggeringAll) {
                                CircularProgressIndicator(
                                    color = SpeedoWhite,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Executing All Routines...", color = SpeedoWhite)
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = SpeedoWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Trigger All 6 Routines Now", fontWeight = FontWeight.Bold, color = SpeedoWhite)
                            }
                        }
                    }
                }
            }

            // 2. ROUTINES SECTION HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Automated Routines",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpeedoTextPrimary
                    )
                    Text(
                        text = "${cronStatus?.jobs?.size ?: 6} routines",
                        fontSize = 12.sp,
                        color = SpeedoTextSecondary
                    )
                }
            }

            // 3. JOB CARDS
            val jobs = cronStatus?.jobs ?: emptyList()
            if (jobs.isEmpty() && uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SpeedoOrange)
                    }
                }
            } else if (jobs.isEmpty()) {
                // Fallback display if not loaded yet
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SpeedoWhite)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = SpeedoTextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading schedules from cloud...", color = SpeedoTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(jobs) { job ->
                    CronJobCard(
                        job = job,
                        isRunning = triggeringMap[job.key] == true,
                        onRunClick = { viewModel.triggerCronRoutine(job.key) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SpeedoSurfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SpeedoTextPrimary)
            }
            Text(text = label, fontSize = 11.sp, color = SpeedoTextSecondary)
        }
    }
}

@Composable
fun CronJobCard(
    job: CronJobItem,
    isRunning: Boolean,
    onRunClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SpeedoWhite),
        border = BorderStroke(1.dp, SpeedoCardBorder),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpeedoTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SpeedoSurfaceVariant
                    ) {
                        Text(
                            text = job.key,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SpeedoTextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Status Badge
                val (statusBg, statusFg, statusText) = when (job.lastStatus) {
                    "success" -> Triple(SpeedoSuccessContainer, SpeedoSuccess, "● Success")
                    "running" -> Triple(SpeedoOrangeContainer, SpeedoOrange, "⚡ Running")
                    "failed" -> Triple(SpeedoErrorContainer, SpeedoError, "✕ Failed")
                    else -> Triple(SpeedoSurfaceVariant, SpeedoTextSecondary, "○ Scheduled")
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        color = statusFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Schedule Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = SpeedoOrange, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${job.schedule} (${job.timezone})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SpeedoOrange
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = job.description,
                fontSize = 13.sp,
                color = SpeedoTextSecondary,
                lineHeight = 18.sp
            )

            // Last message if present
            val lastMsg = job.lastMessage
            if (!lastMsg.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpeedoSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastMsg,
                        fontSize = 11.sp,
                        color = SpeedoTextPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SpeedoCardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Run stats & Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Runs: ${job.totalRuns} | Dispatched: ${job.totalNotificationsSent}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SpeedoTextPrimary
                    )
                    val lastRunStr = job.lastRunAt?.take(19)?.replace("T", " ") ?: "Not run yet"
                    Text(
                        text = "Last: $lastRunStr",
                        fontSize = 11.sp,
                        color = SpeedoTextTertiary
                    )
                }

                // Run Now Button
                OutlinedButton(
                    onClick = onRunClick,
                    enabled = !isRunning,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SpeedoOrange),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpeedoOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            color = SpeedoOrange,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Running...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
