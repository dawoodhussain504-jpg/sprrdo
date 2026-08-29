package com.speedo.rider.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.speedo.core.theme.SpeedoOrange
import com.speedo.core.theme.SpeedoTextSecondary
import com.speedo.core.theme.SpeedoWhite
import com.speedo.rider.viewmodel.RiderViewModel

sealed class RiderScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : RiderScreen("home", "Book Ride", Icons.AutoMirrored.Filled.DirectionsBike)
    object ActiveRide : RiderScreen("active_ride", "Active Ride", Icons.Default.Navigation)
    object History : RiderScreen("history", "Trips", Icons.Default.History)
    object Notifications : RiderScreen("notifications", "Alerts", Icons.Default.Notifications)
    object Profile : RiderScreen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun RiderMainScaffold(
    viewModel: RiderViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("speedo_rider_prefs", android.content.Context.MODE_PRIVATE) }

    var showIntro by remember {
        mutableStateOf(!prefs.getBoolean("intro_seen", false) && !uiState.isLoggedIn)
    }

    // 1. Interactive Feature Intro Screen Slider (if first-time user)
    if (showIntro && !uiState.isLoggedIn) {
        RiderIntroScreen(
            onFinishIntro = {
                prefs.edit().putBoolean("intro_seen", true).apply()
                showIntro = false
            }
        )
        return
    }

    // 2. If not authenticated, render RiderAuthScreen
    if (!uiState.isLoggedIn) {
        RiderAuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                // Auth state update in uiState will immediately recompose into MainScaffold
            }
        )
        return
    }

    // 2. Authenticated Rider Main View with Bottom Navigation & Flow
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val unreadCount by viewModel.unreadCount.collectAsState(initial = 0)

    val bottomNavItems = listOf(
        RiderScreen.Home,
        RiderScreen.History,
        RiderScreen.Notifications,
        RiderScreen.Profile
    )

    Scaffold(
        bottomBar = {
            if (currentRoute != RiderScreen.ActiveRide.route) {
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
                                        if (screen == RiderScreen.Notifications && unreadCount > 0) {
                                            Badge { Text(text = "$unreadCount") }
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
            startDestination = RiderScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(RiderScreen.Home.route) {
                RiderHomeScreen(
                    viewModel = viewModel,
                    onNavigateToActiveRide = {
                        navController.navigate(RiderScreen.ActiveRide.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(RiderScreen.ActiveRide.route) {
                ActiveRideScreen(
                    viewModel = viewModel,
                    onRideCompleted = {
                        navController.navigate(RiderScreen.Home.route) {
                            popUpTo(RiderScreen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(RiderScreen.History.route) {
                RiderHistoryScreen(viewModel = viewModel)
            }

            composable(RiderScreen.Notifications.route) {
                RiderNotificationsScreen(viewModel = viewModel)
            }

            composable(RiderScreen.Profile.route) {
                RiderProfileScreen(
                    viewModel = viewModel,
                    onLogout = {
                        viewModel.logout()
                    }
                )
            }
        }
    }
}
