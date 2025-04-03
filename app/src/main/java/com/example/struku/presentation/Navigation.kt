package com.example.struku.presentation

// Imports remain the same, adding import for our debug screen
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.ReceiptRepository
import com.example.struku.presentation.analytics.AnalyticsScreen
import com.example.struku.presentation.debug.PreprocessingDebugScreen
import com.example.struku.presentation.receipts.ReceiptDetailScreen
import com.example.struku.presentation.receipts.ReceiptsScreen
import com.example.struku.presentation.scan.ReceiptReviewScreen
import com.example.struku.presentation.scan.ScannerScreen
import com.example.struku.presentation.settings.ExportScreen
import com.example.struku.presentation.settings.ManageBudgetsScreen
import com.example.struku.presentation.settings.ManageCategoriesScreen
import com.example.struku.presentation.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main navigation graph containing bottom tabs and other screens
 * This file only contains navigation graph builders, the actual NavHost is in AppNavHost.kt
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
                navController.navigate(NavRoutes.RECEIPT_ADD)
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
            },
            // Add navigation to debug screen
            onDebugClick = {
                navController.navigate(NavRoutes.DEBUG_PREPROCESSING)
            }
        )
    }
    
    // Debug preprocessing screen
    composable(NavRoutes.DEBUG_PREPROCESSING) {
        PreprocessingDebugScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onCaptureImage = {
                navController.navigate(NavRoutes.SCANNER) {
                    popUpTo(NavRoutes.DEBUG_PREPROCESSING) {
                        saveState = true
                    }
                }
            }
        )
    }
    
    // Scanner screen
    composable(NavRoutes.SCANNER) {
        ScannerScreen(navController)
    }
    
    // Receipt add screen (previously missing)
    composable(NavRoutes.RECEIPT_ADD) {
        val viewModel = hiltViewModel<AddReceiptViewModel>()
        AddReceiptScreen(
            navController = navController,
            receiptRepository = viewModel.repository
        )
    }
    
    // Receipt scan screen
    composable(NavRoutes.RECEIPT_SCAN) {
        // Add your receipt scan screen implementation here
        ScannerScreen(navController)
    }
    
    // Receipt detail screen
    composable(
        route = NavRoutes.RECEIPT_DETAIL,
        arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
    ) { backStackEntry ->
        val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
        ReceiptDetailScreen(receiptId, navController)
    }
    
    // Receipt review screen
    composable(
        route = NavRoutes.RECEIPT_REVIEW,
        arguments = listOf(navArgument("receiptId") { type = NavType.LongType })
    ) { backStackEntry ->
        val receiptId = backStackEntry.arguments?.getLong("receiptId") ?: 0L
        ReceiptReviewScreen(receiptId, navController)
    }
    
    // Settings screens
    composable(NavRoutes.SETTINGS_EXPORT) {
        ExportScreen(navController)
    }
    
    composable(NavRoutes.SETTINGS_CATEGORIES) {
        ManageCategoriesScreen(navController)
    }
    
    composable(NavRoutes.SETTINGS_BUDGETS) {
        ManageBudgetsScreen(navController)
    }
    
    // Analytics detailed screens
    composable(
        route = NavRoutes.MONTHLY_REPORT,
        arguments = listOf(
            navArgument("year") { type = NavType.IntType },
            navArgument("month") { type = NavType.IntType }
        )
    ) { backStackEntry ->
        val year = backStackEntry.arguments?.getInt("year") ?: 0
        val month = backStackEntry.arguments?.getInt("month") ?: 0
        // Use the same analytics screen with different parameters
        AnalyticsScreen(
            onMonthClick = { y, m ->
                navController.navigate(NavRoutes.monthlyReport(y, m)) {
                    popUpTo(NavRoutes.monthlyReport(year, month)) {
                        inclusive = true
                    }
                }
            },
            onCategoryClick = { categoryId ->
                navController.navigate(NavRoutes.categoryReport(categoryId))
            }
        )
    }
    
    composable(
        route = NavRoutes.CATEGORY_REPORT,
        arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
    ) { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
        // For "budgets" category, navigate to the budget management screen
        if (categoryId == "budgets") {
            ManageBudgetsScreen(navController)
        } else {
            // For other categories, show category detail screen
            // This is just a placeholder that will show the category ID
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Detail Kategori: $categoryId",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Kembali")
                    }
                }
            }
        }
    }
}

// The rest of the file remains unchanged
// ...
// AddReceiptScreen and related data classes are kept as is