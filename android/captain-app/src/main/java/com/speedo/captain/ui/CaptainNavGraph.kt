package com.speedo.captain.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.speedo.captain.viewmodel.CaptainViewModel
import com.speedo.core.theme.SpeedoOrange
import com.speedo.core.theme.SpeedoTextSecondary
import com.speedo.core.theme.SpeedoWhite

sealed class CaptainScreen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : CaptainScreen("dashboard", "Duty", Icons.Default.ElectricRickshaw)
    object KycUpload : CaptainScreen("kyc_upload", "KYC Upload", Icons.Default.UploadFile)
    object KycStatus : CaptainScreen("kyc_status", "KYC Status", Icons.Default.VerifiedUser)
    object ActiveRide : CaptainScreen("active_ride", "Active Trip", Icons.Default.Navigation)
    object Earnings : CaptainScreen("earnings", "Earnings", Icons.Default.AccountBalanceWallet)
    object Notifications : CaptainScreen("notifications", "Alerts", Icons.Default.Notifications)
    object Profile : CaptainScreen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun CaptainMainScaffold(
    viewModel: CaptainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("speedo_captain_prefs", android.content.Context.MODE_PRIVATE) }

    // 0. Over-the-Air Version Check & Update Overlays (Never show if already updated)
    val appUpdate = uiState.appUpdateState
    if (appUpdate.isUpdateAvailable && appUpdate.isForceUpdate && !appUpdate.isDismissed) {
        com.speedo.core.components.ForceUpdateOverlay(
            promptState = appUpdate,
            onDismiss = { viewModel.dismissFlexibleUpdate() }
        )
        return
    }

    if (appUpdate.isUpdateAvailable && !appUpdate.isDismissed) {
        com.speedo.core.components.FlexibleUpdateDialog(
            promptState = appUpdate,
            onDismiss = { viewModel.dismissFlexibleUpdate() }
        )
    }

    var showIntro by remember {
        mutableStateOf(!prefs.getBoolean("intro_seen", false) && !uiState.isLoggedIn)
    }

    // 1. Interactive Feature Intro Screen Slider (if first-time user)
    if (showIntro && !uiState.isLoggedIn) {
        CaptainIntroScreen(
            onFinishIntro = {
                prefs.edit().putBoolean("intro_seen", true).apply()
                showIntro = false
            }
        )
        return
    }

    // 2. If not authenticated, render CaptainAuthScreen directly
    if (!uiState.isLoggedIn) {
        CaptainAuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                // Auth state update in uiState will immediately recompose into MainScaffold
            }
        )
        return
    }

    // 2. Authenticated Captain Main View with Bottom Navigation & Flow
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val unreadCount by viewModel.unreadCount.collectAsState(initial = 0)

    val bottomNavItems = listOf(
        CaptainScreen.Dashboard,
        CaptainScreen.KycStatus,
        CaptainScreen.Earnings,
        CaptainScreen.Notifications,
        CaptainScreen.Profile
    )

    Scaffold(
        bottomBar = {
            if (currentRoute != CaptainScreen.ActiveRide.route && currentRoute != CaptainScreen.KycUpload.route) {
                NavigationBar(
                    containerColor = SpeedoWhite,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route

                        NavigationBarItem(
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (screen == CaptainScreen.Notifications) {
                                            if (appUpdate.isUpdateAvailable) {
                                                Badge(
                                                    containerColor = SpeedoOrange,
                                                    contentColor = SpeedoWhite
                                                ) {
                                                    Text(text = "NEW", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                }
                                            } else if (unreadCount > 0) {
                                                Badge { Text(text = "$unreadCount") }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpeedoOrange,
                                selectedTextColor = SpeedoOrange,
                                unselectedIconColor = SpeedoTextSecondary,
                                unselectedTextColor = SpeedoTextSecondary,
                                indicatorColor = SpeedoWhite
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CaptainScreen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(CaptainScreen.Dashboard.route) {
                CaptainDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToActiveRide = {
                        navController.navigate(CaptainScreen.ActiveRide.route)
                    },
                    onNavigateToKyc = {
                        navController.navigate(CaptainScreen.KycUpload.route)
                    }
                )
            }

            composable(CaptainScreen.KycUpload.route) {
                KycSubmissionScreen(
                    viewModel = viewModel,
                    onNavigateToStatus = {
                        navController.navigate(CaptainScreen.KycStatus.route)
                    }
                )
            }

            composable(CaptainScreen.KycStatus.route) {
                KycStatusScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = {
                        navController.navigate(CaptainScreen.Dashboard.route)
                    },
                    onNavigateToUpload = {
                        navController.navigate(CaptainScreen.KycUpload.route)
                    }
                )
            }

            composable(CaptainScreen.ActiveRide.route) {
                CaptainActiveRideScreen(
                    viewModel = viewModel,
                    onRideFinished = {
                        navController.navigate(CaptainScreen.Dashboard.route) {
                            popUpTo(CaptainScreen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(CaptainScreen.Earnings.route) {
                EarningsScreen(viewModel = viewModel)
            }

            composable(CaptainScreen.Notifications.route) {
                CaptainNotificationsScreen(viewModel = viewModel)
            }

            composable(CaptainScreen.Profile.route) {
                CaptainProfileScreen(viewModel = viewModel)
            }
        }
    }
}
