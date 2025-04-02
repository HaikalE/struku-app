package com.example.struku.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.struku.presentation.analytics.AnalyticsScreen
import com.example.struku.presentation.receipts.ReceiptsScreen
import com.example.struku.presentation.settings.SettingsScreen

/**
 * Main navigation host for the app
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.RECEIPTS,
        modifier = modifier
    ) {
        // Receipts tab screens
        composable(NavRoutes.RECEIPTS) {
            ReceiptsScreen(
                onReceiptClick = { receiptId ->
                    navController.navigate(NavRoutes.receiptDetail(receiptId))
                },
                onAddClick = {
                    navController.navigate(NavRoutes.RECEIPT_ADD)
                },
                onScanClick = {
                    navController.navigate(NavRoutes.RECEIPT_SCAN)
                }
            )
        }
        
        // Analytics tab screens
        composable(NavRoutes.ANALYTICS) {
            AnalyticsScreen(
                onMonthClick = { year, month ->
                    navController.navigate(NavRoutes.monthlyReport(year, month))
                },
                onCategoryClick = { categoryId ->
                    navController.navigate(NavRoutes.categoryReport(categoryId))
                }
            )
        }
        
        // Settings tab screens
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onExportClick = {
                    navController.navigate(NavRoutes.SETTINGS_EXPORT)
                },
                onCategoriesClick = {
                    navController.navigate(NavRoutes.SETTINGS_CATEGORIES)
                },
                onBudgetsClick = {
                    navController.navigate(NavRoutes.SETTINGS_BUDGETS)
                }
            )
        }
        
        // Detail screens with route arguments
        composable(
            route = NavRoutes.RECEIPT_DETAIL,
            arguments = listOf(navArgument("receiptId") { type = NavType.IntType })
        ) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.getInt("receiptId") ?: 0
            // ReceiptDetailScreen(receiptId = receiptId, onBackClick = { navController.popBackStack() })
        }
        
        // Add more screen routes here
    }
}