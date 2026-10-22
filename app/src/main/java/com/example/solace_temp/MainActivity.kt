package com.example.solace_temp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.solace_temp.data.MockAppRepository
import com.example.solace_temp.ui.navigation.BottomNavItem
import com.example.solace_temp.ui.navigation.getBottomNavItemsForRole
import com.example.solace_temp.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SolaceCampsiteApp()
            }
        }
    }
}

@Composable
fun SolaceCampsiteApp() {
    val repository = MockAppRepository
    val profile by repository.currentProfile.collectAsState()
    val navController = rememberNavController()
    val navItems = getBottomNavItemsForRole(profile.role)

    // Automatically jump to the first tab of the new role when switching roles
    LaunchedEffect(profile.role) {
        navController.navigate(navItems.first().route) {
            popUpTo(0)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.EntrantHome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ENTRANT ROUTES
            composable(BottomNavItem.EntrantHome.route) {
                CampsiteListScreen(
                    title = "Explore Alberta Campsites",
                    repository = repository,
                    onSelectCampsite = { id -> navController.navigate("campsite_details/$id") }
                )
            }
            composable(BottomNavItem.EntrantSearch.route) {
                CampsiteListScreen(
                    title = "Search & Filter Campsites",
                    repository = repository,
                    onSelectCampsite = { id -> navController.navigate("campsite_details/$id") }
                )
            }
            composable(BottomNavItem.EntrantQrScan.route) {
                TeammatePlaceholderScreen("Scan Promotional QR Code", "US 01.12.01")
            }
            composable(BottomNavItem.EntrantNotifications.route) {
                TeammatePlaceholderScreen("Lottery Notifications & Invitations", "US 01.03.01, US 01.04.01")
            }

            // CAMPSITE OWNER ROUTES
            composable(BottomNavItem.OwnerCampsites.route) {
                CampsiteListScreen(
                    title = "My Registered Campsites",
                    repository = repository,
                    onSelectCampsite = { id -> navController.navigate("campsite_details/$id") }
                )
            }
            composable(BottomNavItem.OwnerCreate.route) {
                CreateCampsiteScreen(
                    repository = repository,
                    onCreated = { id -> navController.navigate("campsite_details/$id") }
                )
            }
            composable(BottomNavItem.OwnerEntrants.route) {
                TeammatePlaceholderScreen("Entrant Map & Waiting List", "US 02.09.01, Geolocation")
            }
            composable(BottomNavItem.OwnerLottery.route) {
                TeammatePlaceholderScreen("Lottery Sampling & Draw Control", "US 02.04.01 - US 02.05.01")
            }

            // ADMIN ROUTES
            composable(BottomNavItem.AdminCampsites.route) {
                CampsiteListScreen(
                    title = "Admin: Moderate Campsites",
                    repository = repository,
                    onSelectCampsite = { id -> navController.navigate("campsite_details/$id") }
                )
            }
            composable(BottomNavItem.AdminProfiles.route) {
                TeammatePlaceholderScreen("Admin: Browse & Remove Profiles", "US 03.02.01, US 03.05.01")
            }
            composable(BottomNavItem.AdminLogs.route) {
                TeammatePlaceholderScreen("Admin: Notification & Message Logs", "US 03.08.01, US 03.12.01")
            }

            // SHARED PROFILE & CAMPSITE DETAILS ROUTES
            composable(BottomNavItem.ProfileTab.route) {
                ProfileAndRoleScreen(repository = repository)
            }
            composable("campsite_details/{campsiteId}") { backStackEntry ->
                val campsiteId = backStackEntry.arguments?.getString("campsiteId") ?: ""
                CampsiteDetailsScreen(
                    campsiteId = campsiteId,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}