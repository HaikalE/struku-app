package com.example.struku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.struku.presentation.navigation.Navigation
import com.example.struku.presentation.navigation.Screen
import com.example.struku.ui.theme.StrukuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StrukuTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StrukuApp()
                }
            }
        }
    }
}

/**
 * Main app composable with bottom navigation
 */
@Composable
fun StrukuApp() {
    val navController = rememberNavController()
    
    // Define bottom navigation items
    val bottomNavItems = listOf(
        BottomNavItem(
            route = "home",
            title = "Beranda",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = "scanner",
            title = "Pindai",
            icon = Icons.Default.ReceiptLong
        ),
        BottomNavItem(
            route = Screen.Analytics.route,
            title = "Analitik",
            icon = Icons.Default.PieChart
        ),
        BottomNavItem(
            route = "profile",
            title = "Profil",
            icon = Icons.Default.Settings
        )
    )
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Show bottom navigation only on main screens
            val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }
            
            if (showBottomBar) {
                BottomNavigation(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        
                        BottomNavigationItem(
                            icon = { 
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = item.title,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ) 
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Navigation(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Data class for bottom navigation items
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)
