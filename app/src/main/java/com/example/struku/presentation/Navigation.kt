package com.example.struku.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.struku.presentation.analytics.AnalyticsScreen
import com.example.struku.presentation.receipts.ReceiptDetailScreen
import com.example.struku.presentation.receipts.ReceiptsScreen
import com.example.struku.presentation.scan.ReceiptReviewScreen
import com.example.struku.presentation.scan.ScannerScreen
import com.example.struku.presentation.settings.SettingsScreen

/**
 * Main navigation routes
 */
object NavRoutes {
    const val RECEIPTS = "receipts"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    
    const val SCANNER = "scanner"
    const val RECEIPT_REVIEW = "receipt_review/{receiptId}"
    const val RECEIPT_DETAIL = "receipt_detail/{receiptId}"
    
    // Helper functions for navigation with parameters
    fun receiptReview(receiptId: Int) = "receipt_review/$receiptId"
    fun receiptDetail(receiptId: Int) = "receipt_detail/$receiptId"
}

/**
 * Main navigation host for the app
 */
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.RECEIPTS
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
        ReceiptsScreen(navController)
    }
    
    composable(NavRoutes.ANALYTICS) {
        AnalyticsScreen()
    }
    
    composable(NavRoutes.SETTINGS) {
        SettingsScreen()
    }
    
    // Scanner screen
    composable(NavRoutes.SCANNER) {
        ScannerScreen(navController)
    }
    
    // Receipt review screen
    composable(
        route = NavRoutes.RECEIPT_REVIEW,
        arguments = listOf(navArgument("receiptId") { type = NavType.IntType })
    ) { backStackEntry ->
        val receiptId = backStackEntry.arguments?.getInt("receiptId") ?: 0
        ReceiptReviewScreen(receiptId, navController)
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
