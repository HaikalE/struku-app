package com.example.struku.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.struku.presentation.analytics.AnalyticsScreen
import com.example.struku.presentation.analytics.ManageBudgetScreen
import com.example.struku.presentation.analytics.ManageCategoriesScreen

/**
 * Main navigation configuration for the app
 */
@Composable
fun Navigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Analytics.route,
        modifier = modifier
    ) {
        // Analytics screen
        composable(route = Screen.Analytics.route) {
            AnalyticsScreen(
                onMonthClick = { year, month ->
                    // Navigate to month detail if needed
                },
                onCategoryClick = { categoryId ->
                    when (categoryId) {
                        "budgets" -> navController.navigate(Screen.ManageBudget.route)
                        else -> {
                            // Navigate to category detail if needed
                        }
                    }
                },
                onManageBudgetClick = {
                    navController.navigate(Screen.ManageBudget.route)
                },
                onManageCategoriesClick = {
                    navController.navigate(Screen.ManageCategories.route)
                }
            )
        }
        
        // Manage budget screen
        composable(route = Screen.ManageBudget.route) {
            ManageBudgetScreen(
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        // Manage categories screen
        composable(route = Screen.ManageCategories.route) {
            ManageCategoriesScreen(
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        // Placeholder screens for other main tabs
        composable(route = "home") {
            // Home screen placeholder
            // HomeScreen()
        }
        
        composable(route = "scanner") {
            // Scanner screen placeholder
            // ScannerScreen()
        }
        
        composable(route = "profile") {
            // Profile screen placeholder
            // ProfileScreen()
        }
    }
}

/**
 * Sealed class representing all available screens in the app
 */
sealed class Screen(val route: String) {
    object Analytics : Screen("analytics")
    object ManageBudget : Screen("manage_budget")
    object ManageCategories : Screen("manage_categories")
    
    // Add other screens here as needed
    // object Home : Screen("home")
    // object Scanner : Screen("scanner")
    // object Transactions : Screen("transactions")
    // object Profile : Screen("profile")
}