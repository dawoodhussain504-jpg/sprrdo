package com.speedo.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.theme.*
import kotlinx.coroutines.launch

sealed class AdminScreen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : AdminScreen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object KycQueue : AdminScreen("kyc_queue", "KYC Queue", Icons.Default.VerifiedUser)
    object LiveMap : AdminScreen("live_map", "Live Fleet Map", Icons.Default.Map)
    object Rides : AdminScreen("rides", "Ride Monitoring", Icons.Default.DirectionsCar)
    object SupportDesk : AdminScreen("support_desk", "Support Desk", Icons.Default.SupportAgent)
    object Users : AdminScreen("users", "User Moderation", Icons.Default.People)
    object Auth : AdminScreen("auth", "Sign In", Icons.Default.Lock)
}

@Composable
fun AdminMainScaffold(
    viewModel: AdminViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navItems = listOf(
        AdminScreen.Dashboard,
        AdminScreen.KycQueue,
        AdminScreen.LiveMap,
        AdminScreen.Rides,
        AdminScreen.SupportDesk,
        AdminScreen.Users
    )

    if (!uiState.isLoggedIn) {
        AdminLoginScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                navController.navigate(AdminScreen.Dashboard.route) {
                    popUpTo(AdminScreen.Auth.route) { inclusive = true }
                }
            }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SpeedoWhite,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpeedoOrange)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Speedo Admin",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = SpeedoWhite)
                    )
                    Text(
                        text = "Platform Control Center",
                        style = MaterialTheme.typography.bodySmall,
                        color = SpeedoOrangeContainer
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SpeedoOrangeContainer,
                            selectedIconColor = SpeedoOrange,
                            selectedTextColor = SpeedoOrange,
                            unselectedIconColor = SpeedoTextSecondary,
                            unselectedTextColor = SpeedoTextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = "Log Out", tint = SpeedoError) },
                    label = { Text("Log Out", color = SpeedoError, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = AdminScreen.Dashboard.route
        ) {
            composable(AdminScreen.Dashboard.route) {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateToKyc = { navController.navigate(AdminScreen.KycQueue.route) },
                    onNavigateToMap = { navController.navigate(AdminScreen.LiveMap.route) },
                    onNavigateToRides = { navController.navigate(AdminScreen.Rides.route) },
                    onNavigateToSupport = { navController.navigate(AdminScreen.SupportDesk.route) },
                    onNavigateToUsers = { navController.navigate(AdminScreen.Users.route) }
                )
            }

            composable(AdminScreen.KycQueue.route) {
                KycReviewQueueScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(AdminScreen.LiveMap.route) {
                LiveFleetMapScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(AdminScreen.Rides.route) {
                RidesMonitoringScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(AdminScreen.SupportDesk.route) {
                SupportDeskScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(AdminScreen.Users.route) {
                UserManagementScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}
