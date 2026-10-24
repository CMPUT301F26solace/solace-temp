/**
 * MainActivity.kt
 * 
 * Purpose: This is the main entry point for the Solace Campsite application.
 * It sets up the Compose activity, navigation graph, and the bottom navigation bar.
 * 
 * Needs Change/Issues: 
 * - The navigation graph contains hardcoded routes which should ideally be extracted into a separate Navigation constants file or a type-safe navigation graph.
 * - The greeting logic and role switching logic are tightly coupled to the UI layer; could be moved to a ViewModel.
 */
package com.example.solace_temp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.solace_temp.data.FirestoreAppRepository
import com.example.solace_temp.ui.navigation.BottomNavItem
import com.example.solace_temp.ui.navigation.getBottomNavItemsForRole
import com.example.solace_temp.ui.screens.*
import com.example.solace_temp.ui.theme.Solace_tempTheme
import kotlinx.coroutines.delay

/**
 * Main entry point of the Solace application.
 * Extends [ComponentActivity] to host Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is starting.
     * Sets the content of the activity to the [SolaceCampsiteApp] composable.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Solace_tempTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SolaceCampsiteApp()
                }
            }
        }
    }
}

/**
 * The root composable function for the Solace Campsite application.
 * Contains the [Scaffold], bottom navigation bar, and [NavHost] for routing.
 * Manages shared state such as the [FirestoreAppRepository] and current user profile.
 */
@Composable
fun SolaceCampsiteApp() {
    val context = LocalContext.current
    val repository = remember { FirestoreAppRepository(context) }
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
            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        
                        // Icon scale animation
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.2f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "scale_anim"
                        )

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { 
                                Icon(
                                    item.icon, 
                                    contentDescription = item.label,
                                    modifier = Modifier.scale(scale)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                ) 
                            },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                    OwnerCampsitesScreen(
                        repository = repository,
                        onSelectCampsite = { id -> navController.navigate("campsite_details/$id") },
                        onCreateNew = { navController.navigate(BottomNavItem.OwnerCreate.route) }
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

            // Dynamic Floating Greeting Overlay
            var showGreeting by remember { mutableStateOf(false) }

            LaunchedEffect(profile.name) {
                if (profile.name.isNotBlank()) {
                    showGreeting = true
                    delay(10000)
                    showGreeting = false
                }
            }

            AnimatedVisibility(
                visible = showGreeting,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(600)
                ) + fadeIn(tween(600)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(600)
                ) + fadeOut(tween(600)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val greeting = if (profile.name.isBlank() || profile.name == "New Explorer") {
                        "Welcome to Solace Campsite \uD83C\uDFD5\uFE0F"
                    } else {
                        "Welcome back, ${profile.name} \uD83D\uDC4B"
                    }
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}