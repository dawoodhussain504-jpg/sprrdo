package com.speedo.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    object KycQueue : AdminScreen("kyc_queue", "KYC Queue & OCR", Icons.Default.VerifiedUser)
    object SurgeEngine : AdminScreen("surge_engine", "Surge & Geofencing", Icons.Default.Bolt)
    object SosCenter : AdminScreen("sos_center", "Emergency SOS Center", Icons.Default.Shield)
    object Broadcasts : AdminScreen("broadcasts", "City Broadcasts", Icons.Default.Campaign)
    object Destinations : AdminScreen("destinations", "Popular Destinations", Icons.Default.Place)
    object LiveMap : AdminScreen("live_map", "Live Fleet Map", Icons.Default.Map)
    object Rides : AdminScreen("rides", "Ride Monitoring", Icons.Default.DirectionsCar)
    object Users : AdminScreen("users", "User Moderation", Icons.Default.People)
    object DeletionRequests : AdminScreen("deletion_requests", "Account Deletions", Icons.Default.DeleteForever)
    object AppVersions : AdminScreen("app_versions", "App Version & OTA", Icons.Default.RocketLaunch)
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
        AdminScreen.SurgeEngine,
        AdminScreen.SosCenter,
        AdminScreen.Broadcasts,
        AdminScreen.Destinations,
        AdminScreen.LiveMap,
        AdminScreen.Rides,
        AdminScreen.Users,
        AdminScreen.DeletionRequests,
        AdminScreen.AppVersions
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
                    val isSosBadge = item == AdminScreen.SosCenter && uiState.activeSosCount > 0
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.title,
                                tint = if (isSosBadge) SpeedoError else if (isSelected) SpeedoOrange else SpeedoTextSecondary
                            )
                        },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.title,
                                    fontWeight = if (isSelected || isSosBadge) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSosBadge) SpeedoError else if (isSelected) SpeedoOrange else SpeedoTextPrimary
                                )
                                if (isSosBadge) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SpeedoError
                                    ) {
                                        Text(
                                            text = "${uiState.activeSosCount}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = SpeedoWhite
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != item.route) {
                                if (item.route == AdminScreen.Dashboard.route) {
                                    navController.popBackStack(AdminScreen.Dashboard.route, false)
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(AdminScreen.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
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
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = SpeedoError) },
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
                    onNavigateToSurge = { navController.navigate(AdminScreen.SurgeEngine.route) },
                    onNavigateToSos = { navController.navigate(AdminScreen.SosCenter.route) },
                    onNavigateToBroadcasts = { navController.navigate(AdminScreen.Broadcasts.route) },
                onNavigateToDestinations = { navController.navigate(AdminScreen.Destinations.route) },
                    onNavigateToMap = { navController.navigate(AdminScreen.LiveMap.route) },
                    onNavigateToRides = { navController.navigate(AdminScreen.Rides.route) },
                    onNavigateToUsers = { navController.navigate(AdminScreen.Users.route) },
                    onNavigateToDeletions = { navController.navigate(AdminScreen.DeletionRequests.route) },
                    onNavigateToVersions = { navController.navigate(AdminScreen.AppVersions.route) }
                )
            }

            composable(AdminScreen.KycQueue.route) {
                KycReviewQueueScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(AdminScreen.SurgeEngine.route) {
                GeofenceSurgeEngineScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(AdminScreen.SosCenter.route) {
                SosEmergencyCenterScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

                    composable(AdminScreen.Destinations.route) {
            ManagePopularDestinationsScreen(
                viewModel = viewModel,
                onMenuClick = { scope.launch { drawerState.open() } }
            )
        }
        composable(AdminScreen.Broadcasts.route) {
                CityBroadcastScreen(
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

            composable(AdminScreen.Users.route) {
                UserManagementScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(AdminScreen.DeletionRequests.route) {
                AccountDeletionRequestsScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            composable(AdminScreen.AppVersions.route) {
                ManageAppVersionsScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}
