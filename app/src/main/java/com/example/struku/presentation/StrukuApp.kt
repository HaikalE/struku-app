package com.example.struku.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.struku.R

/**
 * Navigation items for bottom navigation
 */
sealed class BottomNavItem(val route: String, val icon: @Composable () -> Unit, val titleRes: Int) {
    object Receipts : BottomNavItem(
        NavRoutes.RECEIPTS,
        { Icon(Icons.Filled.Receipt, contentDescription = null) },
        R.string.screen_receipts
    )
    
    object Analytics : BottomNavItem(
        NavRoutes.ANALYTICS,
        { Icon(Icons.Filled.Analytics, contentDescription = null) },
        R.string.screen_analytics
    )
    
    object Settings : BottomNavItem(
        NavRoutes.SETTINGS,
        { Icon(Icons.Filled.Settings, contentDescription = null) },
        R.string.screen_settings
    )
}

/**
 * Main app composable containing the navigation structure
 */
@Composable
fun StrukuApp() {
    val navController = rememberNavController()
    val navItems = listOf(BottomNavItem.Receipts, BottomNavItem.Analytics, BottomNavItem.Settings)
    
    // Get current navigation state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Only show bottom bar for main tabs
    val showBottomBar = currentDestination?.hierarchy?.any { destination ->
        navItems.any { it.route == destination.route }
    } ?: false
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = { item.icon() },
                            label = { Text(stringResource(id = item.titleRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
