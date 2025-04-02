package com.example.struku.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.struku.presentation.analytics.AnalyticsScreen
import com.example.struku.presentation.receipts.ReceiptDetailScreen
import com.example.struku.presentation.receipts.ReceiptsScreen
import com.example.struku.presentation.scan.ScannerScreen
import com.example.struku.presentation.settings.SettingsScreen

// NavRoutes is already defined in NavRoutes.kt so no need to redefine it here

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
        mainGraph(navController)
    }
}

/**
 * Main navigation graph containing bottom tabs and other screens
 */
fun NavGraphBuilder.mainGraph(navController: NavController) {
    // Bottom navigation screens
    composable(NavRoutes.RECEIPTS) {
        ReceiptsScreen(
            onReceiptClick = { receiptId ->
                navController.navigate(NavRoutes.receiptDetail(receiptId))
            },
            onAddClick = {
                // Navigate to manual receipt entry screen
                // navController.navigate(NavRoutes.RECEIPT_ADD)
            },
            onScanClick = {
                navController.navigate(NavRoutes.SCANNER)
            }
        )
    }
    
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
    
    // Scanner screen
    composable(NavRoutes.SCANNER) {
        ScannerScreen(navController)
    }
    
    // Receipt detail screen
    composable(
        route = NavRoutes.RECEIPT_DETAIL,
        arguments = listOf(navArgument("receiptId") { type = NavType.IntType })
    ) { backStackEntry ->
        val receiptId = backStackEntry.arguments?.getInt("receiptId") ?: 0
        ReceiptDetailScreen(receiptId, navController)
    }
}