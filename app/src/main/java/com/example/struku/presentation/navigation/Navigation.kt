package com.example.struku.presentation.navigation

import androidx.compose.runtime.Composable
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
fun Navigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Analytics.route
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
        
        // Add other screens here as needed
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
