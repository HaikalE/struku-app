package com.example.struku.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.struku.presentation.analytics.AnalyticsScreen
import com.example.struku.presentation.receipts.ReceiptDetailScreen
import com.example.struku.presentation.receipts.ReceiptsScreen
import com.example.struku.presentation.scan.ScannerScreen
import com.example.struku.presentation.settings.ExportScreen
import com.example.struku.presentation.settings.SettingsScreen
import java.text.NumberFormat
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
            }
        )
    }
    
    // Scanner screen
    composable(NavRoutes.SCANNER) {
        ScannerScreen(navController)
    }
    
    // Receipt add screen (previously missing)
    composable(NavRoutes.RECEIPT_ADD) {
        AddReceiptScreen(navController)
    }
    
    // Receipt scan screen
    composable(NavRoutes.RECEIPT_SCAN) {
        // Add your receipt scan screen implementation here
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
    
    // Settings screens
    composable(NavRoutes.SETTINGS_EXPORT) {
        ExportScreen(navController)
    }
    
    composable(NavRoutes.SETTINGS_CATEGORIES) {
        SettingsCategoriesScreen(navController)
    }
    
    composable(NavRoutes.SETTINGS_BUDGETS) {
        SettingsBudgetsScreen(navController)
    }
}

/**
 * Data class for receipt items
 */
data class ReceiptItem(
    val id: Int,
    var name: String,
    var quantity: Int,
    var price: Double
)

/**
 * Screen for adding a receipt manually
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptScreen(navController: NavController) {
    // State for form fields
    var merchantName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    
    // List of receipt items
    val items = remember { mutableStateListOf<ReceiptItem>() }
    
    // Categories (in a real app, this would come from a repository)
    val categories = listOf("Makanan", "Transportasi", "Belanja", "Hiburan", "Pendidikan", "Lainnya")
    
    // NumberFormat for currency
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Struk Manual") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Add a new empty item to the list
                    val newId = if (items.isEmpty()) 1 else items.maxOf { it.id } + 1
                    items.add(ReceiptItem(newId, "", 1, 0.0))
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Merchant name field
            OutlinedTextField(
                value = merchantName,
                onValueChange = { merchantName = it },
                label = { Text("Nama Merchant") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date field
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Tanggal (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                isExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Items section
            Text(
                "Daftar Item",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display item list
            if (items.isEmpty()) {
                Text(
                    "Belum ada item. Klik tombol + untuk menambahkan item.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                items.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { item.name = it },
                                label = { Text("Nama Item") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = item.quantity.toString(),
                                    onValueChange = { 
                                        item.quantity = it.toIntOrNull() ?: 1
                                    },
                                    label = { Text("Qty") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                OutlinedTextField(
                                    value = if (item.price == 0.0) "" else item.price.toString(),
                                    onValueChange = { 
                                        item.price = it.toDoubleOrNull() ?: 0.0
                                    },
                                    label = { Text("Harga") },
                                    modifier = Modifier.weight(2f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true
                                )
                                
                                IconButton(
                                    onClick = { items.remove(item) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus Item",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total amount
            OutlinedTextField(
                value = totalAmount,
                onValueChange = { totalAmount = it },
                label = { Text("Total (Rp)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Calculate total from items
            val calculatedTotal = items.sumOf { it.price * it.quantity }
            if (calculatedTotal > 0) {
                Text(
                    "Total dari item: ${currencyFormatter.format(calculatedTotal)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Save button
            Button(
                onClick = {
                    // Here we would save the receipt
                    // For now, just go back
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Struk")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Temporary screen for categories settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCategoriesScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kategori") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Pengaturan Kategori")
        }
    }
}

/**
 * Temporary screen for budget settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBudgetsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anggaran") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Pengaturan Anggaran")
        }
    }
}