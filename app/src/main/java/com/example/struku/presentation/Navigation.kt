package com.example.struku.presentation

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
import com.example.struku.presentation.receipts.ReceiptDetailScreen
import com.example.struku.presentation.receipts.ReceiptsScreen
import com.example.struku.presentation.scan.ScannerScreen
import com.example.struku.presentation.settings.ExportScreen
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
 * Data class for receipt item state
 */
data class ReceiptItemState(
    val id: Int,
    val name: String,
    val quantity: String,
    val price: String
)

/**
 * Screen for adding a receipt manually
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptScreen(
    navController: NavController,
    receiptRepository: ReceiptRepository
) {
    // State for form fields
    var merchantName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    
    // Saving state
    var isSaving by remember { mutableStateOf(false) }
    var showSavedDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // List of receipt items - using a different approach for state management
    var itemStates by remember { mutableStateOf(listOf<ReceiptItemState>()) }
    
    // Categories (in a real app, this would come from a repository)
    val categories = listOf("Makanan", "Transportasi", "Belanja", "Hiburan", "Pendidikan", "Lainnya")
    
    // NumberFormat for currency
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Calculate total from items
    val calculatedTotal = itemStates.sumOf { 
        val qty = it.quantity.toIntOrNull() ?: 0
        val price = it.price.toDoubleOrNull() ?: 0.0
        qty * price 
    }
    
    // Function to handle save button click
    fun handleSave() {
        if (merchantName.isBlank()) {
            errorMessage = "Nama merchant harus diisi"
            return
        }
        
        if (date.isBlank()) {
            errorMessage = "Tanggal harus diisi"
            return
        }
        
        if (selectedCategory.isBlank()) {
            errorMessage = "Kategori harus dipilih"
            return
        }
        
        if (itemStates.isEmpty()) {
            errorMessage = "Tambahkan minimal satu item"
            return
        }
        
        // Parse date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = try {
            dateFormat.parse(date) ?: Date()
        } catch (e: Exception) {
            errorMessage = "Format tanggal tidak valid (YYYY-MM-DD)"
            return
        }
        
        // Convert items
        val lineItemsList = mutableListOf<LineItem>()
        for (state in itemStates) {
            val quantity = state.quantity.toIntOrNull()
            val price = state.price.toDoubleOrNull()
            
            if (quantity == null || price == null) {
                errorMessage = "Semua item harus diisi dengan benar"
                return
            }
            
            lineItemsList.add(
                LineItem(
                    description = state.name,
                    quantity = quantity,
                    price = price
                )
            )
        }
        
        // Create receipt object
        val receipt = Receipt(
            merchantName = merchantName,
            date = parsedDate,
            category = selectedCategory,
            total = calculatedTotal,
            items = lineItemsList,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        // Save receipt
        isSaving = true
        errorMessage = null
        
        Log.d("AddReceiptScreen", "Saving receipt: $receipt")
        
        // Use coroutine to save receipt
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val receiptId = receiptRepository.saveReceipt(receipt)
                Log.d("AddReceiptScreen", "Receipt saved with ID: $receiptId")
                
                withContext(Dispatchers.Main) {
                    isSaving = false
                    showSavedDialog = true
                }
            } catch (e: Exception) {
                Log.e("AddReceiptScreen", "Error saving receipt", e)
                withContext(Dispatchers.Main) {
                    isSaving = false
                    errorMessage = "Gagal menyimpan struk: ${e.message}"
                }
            }
        }
    }
    
    // Dialog konfirmasi sukses
    if (showSavedDialog) {
        Dialog(onDismissRequest = { /* Dialog tidak bisa ditutup dengan klik di luar */ }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Struk Berhasil Disimpan!",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Struk Anda telah berhasil disimpan ke database. Anda dapat melihat daftar struk di halaman utama.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            showSavedDialog = false
                            navController.navigate(NavRoutes.RECEIPTS) {
                                popUpTo(NavRoutes.RECEIPTS) {
                                    inclusive = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lihat Daftar Struk")
                    }
                }
            }
        }
    }
    
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
                    // Add a new empty item to the list with a unique ID
                    val newId = if (itemStates.isEmpty()) 1 else itemStates.maxByOrNull { it.id }?.id?.plus(1) ?: 1
                    itemStates = itemStates + ReceiptItemState(newId, "", "1", "")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Item")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    singleLine = true,
                    placeholder = { Text("Contoh: Alfamart Jl. Sudirman") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date field
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Tanggal (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Contoh: 2025-04-02") }
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
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        placeholder = { Text("Pilih kategori...") }
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
                if (itemStates.isEmpty()) {
                    Text(
                        "Belum ada item. Klik tombol + untuk menambahkan item.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    itemStates.forEachIndexed { index, item ->
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
                                    onValueChange = { newName ->
                                        // Create a new list with the updated item
                                        val newList = itemStates.toMutableList()
                                        newList[index] = item.copy(name = newName)
                                        itemStates = newList
                                    },
                                    label = { Text("Nama Item") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("Contoh: Indomie Goreng") }
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = item.quantity,
                                        onValueChange = { newQty ->
                                            // Update with only valid numbers
                                            if (newQty.isEmpty() || newQty.all { it.isDigit() }) {
                                                val newList = itemStates.toMutableList()
                                                newList[index] = item.copy(quantity = newQty)
                                                itemStates = newList
                                            }
                                        },
                                        label = { Text("Qty") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        placeholder = { Text("1") }
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    OutlinedTextField(
                                        value = item.price,
                                        onValueChange = { newPrice ->
                                            // Allow empty string, digits and single decimal point
                                            if (newPrice.isEmpty() || 
                                                newPrice.all { it.isDigit() || it == '.' } && 
                                                newPrice.count { it == '.' } <= 1) {
                                                val newList = itemStates.toMutableList()
                                                newList[index] = item.copy(price = newPrice)
                                                itemStates = newList
                                            }
                                        },
                                        label = { Text("Harga") },
                                        modifier = Modifier.weight(2f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        placeholder = { Text("Contoh: 3500") }
                                    )
                                    
                                    IconButton(
                                        onClick = { 
                                            itemStates = itemStates.filterIndexed { i, _ -> i != index }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus Item",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                // Tampilkan subtotal item
                                val itemQty = item.quantity.toIntOrNull() ?: 0
                                val itemPrice = item.price.toDoubleOrNull() ?: 0.0
                                val itemTotal = itemQty * itemPrice
                                
                                if (itemTotal > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Subtotal: ${currencyFormatter.format(itemTotal)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Total section - Tampilkan total yang dihitung otomatis
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Total Struk",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = currencyFormatter.format(calculatedTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Dihitung otomatis dari total semua item",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Tampilkan pesan error jika ada
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                // Contoh data - ditambahkan untuk membantu pengguna
                if (itemStates.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Contoh:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        "Merchant: Alfamart Jl. Sudirman\n" +
                        "Tanggal: 2025-04-02\n" +
                        "Kategori: Belanja\n\n" +
                        "Item 1: Indomie Goreng, Qty: 5, Harga: 3500\n" +
                        "Item 2: Aqua 600ml, Qty: 2, Harga: 4000\n" +
                        "Item 3: Teh Pucuk Harum, Qty: 1, Harga: 5500\n" +
                        "Item 4: Roti Tawar, Qty: 1, Harga: 12500\n\n" +
                        "Total: 40000",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Save button
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { handleSave() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Simpan Struk", style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Overlay untuk proses penyimpanan
            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(0.8f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = "Menyimpan Struk",
                                style = MaterialTheme.typography.titleLarge
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Mohon tunggu, data struk sedang disimpan ke database...",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
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