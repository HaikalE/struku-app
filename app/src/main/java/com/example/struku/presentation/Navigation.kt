package com.example.struku.presentation

// Imports remain the same, adding import for our debug screen
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.mutableStateListOf
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
    
    // Receipt add screen
    composable(NavRoutes.RECEIPT_ADD) {
        val viewModel = hiltViewModel<AddReceiptViewModel>()
        AddReceiptScreen(
            navController = navController,
            viewModel = viewModel
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
    
    // Receipt edit screen (redirects to review screen)
    composable(
        route = NavRoutes.RECEIPT_EDIT,
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

/**
 * AddReceiptScreen implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptScreen(
    navController: NavController,
    viewModel: AddReceiptViewModel
) {
    var merchantName by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Item input state
    var itemName by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf("1") }
    var itemPrice by remember { mutableStateOf("") }
    
    // List of items
    val items = remember { mutableStateListOf<LineItem>() }
    
    // Calculate total from items
    val totalAmount = items.sumOf { it.price * it.quantity }
    val formattedTotal = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalAmount)
    
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val todayDateStr = dateFormat.format(Date())
    
    val categories = listOf("Makanan", "Transportasi", "Belanja", "Hiburan", "Lainnya")
    var categoryExpanded by remember { mutableStateOf(false) }
    
    var showAddItemForm by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Struk") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = merchantName,
                onValueChange = { merchantName = it },
                label = { Text("Nama Pedagang") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = dateString.ifEmpty { todayDateStr },
                onValueChange = { dateString = it },
                label = { Text("Tanggal (DD/MM/YYYY)") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                )
                
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.exposedDropdownSize()
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Item Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tambah Item",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(
                            onClick = { showAddItemForm = !showAddItemForm }
                        ) {
                            Icon(
                                if (showAddItemForm) Icons.Default.Clear else Icons.Default.Add,
                                contentDescription = if (showAddItemForm) "Tutup Form" else "Tambah Item"
                            )
                        }
                    }
                    
                    if (showAddItemForm) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Nama Item") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = itemQuantity,
                                onValueChange = { itemQuantity = it },
                                label = { Text("Jumlah") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.3f),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = itemPrice,
                                onValueChange = { itemPrice = it },
                                label = { Text("Harga") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.7f),
                                singleLine = true
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                val quantity = itemQuantity.toDoubleOrNull() ?: 1.0
                                val price = itemPrice.toDoubleOrNull() ?: 0.0
                                
                                if (itemName.isNotBlank() && price > 0) {
                                    val newItem = LineItem(
                                        name = itemName,
                                        quantity = quantity,
                                        price = price,
                                        unitPrice = price
                                    )
                                    
                                    items.add(newItem)
                                    
                                    // Reset input fields
                                    itemName = ""
                                    itemQuantity = "1"
                                    itemPrice = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = itemName.isNotBlank() && itemPrice.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TAMBAH ITEM")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Item List
            if (items.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Daftar Item",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        items.forEachIndexed { index, item ->
                            if (index > 0) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Text(
                                        text = "${item.getFormattedQuantity()} x ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(item.unitPrice ?: item.price)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                Text(
                                    text = item.getFormattedTotal(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                IconButton(
                                    onClick = { items.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = formattedTotal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (merchantName.isNotBlank() && items.isNotEmpty()) {
                        isLoading = true
                        
                        val date = try {
                            dateFormat.parse(dateString.ifEmpty { todayDateStr }) ?: Date()
                        } catch (e: Exception) {
                            Date()
                        }
                        
                        // Create new receipt
                        val receipt = Receipt(
                            merchantName = merchantName,
                            date = date,
                            total = totalAmount,
                            category = category,
                            items = items.toList()
                        )
                        
                        // Use IO context for DB operation
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val id = withContext(Dispatchers.IO) {
                                    viewModel.repository.saveReceipt(receipt)
                                }
                                
                                // Navigate back
                                navController.popBackStack()
                            } catch (e: Exception) {
                                Log.e("AddReceiptScreen", "Error saving receipt", e)
                                // Show error (in a real app, add proper error handling)
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = merchantName.isNotBlank() && items.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan")
                }
            }
        }
    }
}
