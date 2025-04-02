package com.example.struku.presentation.scan

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.struku.R
import com.example.struku.domain.model.Category
import com.example.struku.domain.model.LineItem
import com.example.struku.presentation.NavRoutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    receiptId: Int,
    navController: NavController,
    viewModel: ReceiptReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Menampilkan dialog sukses
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Load receipt data
    LaunchedEffect(receiptId) {
        viewModel.loadReceipt(receiptId)
    }
    
    // Process save completion
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            // Tampilkan dialog sukses
            showSuccessDialog = true
            
            // Tunggu sebentar agar user bisa melihat dialog
            delay(1500)
            
            // Navigate to receipts screen setelah dialog ditampilkan
            navController.navigate(NavRoutes.RECEIPTS) {
                popUpTo(NavRoutes.RECEIPTS) {
                    inclusive = true
                }
            }
        }
    }
    
    // Tampilkan dialog sukses jika proses penyimpanan sudah selesai
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Sukses") },
            text = { Text("Struk berhasil disimpan dan akan ditampilkan di daftar struk") },
            confirmButton = {
                Button(
                    onClick = { 
                        showSuccessDialog = false
                        navController.navigate(NavRoutes.RECEIPTS) {
                            popUpTo(NavRoutes.RECEIPTS) {
                                inclusive = true
                            }
                        }
                    }
                ) {
                    Text("Lihat Daftar Struk")
                }
            }
        )
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Review Struk") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveReceipt() },
                        enabled = !state.isLoading && !state.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.receipt == null -> {
                    Text(
                        text = "Tidak dapat memuat data struk",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    ReceiptReviewContent(
                        state = state,
                        onSaveClick = { viewModel.saveReceipt() },
                        onMerchantNameChange = viewModel::updateMerchantName,
                        onDateChange = viewModel::updateDate,
                        onCategoryChange = viewModel::updateCategory,
                        onItemDescriptionChange = viewModel::updateItemDescription,
                        onItemQuantityChange = viewModel::updateItemQuantity,
                        onItemPriceChange = viewModel::updateItemPrice,
                        onAddItem = viewModel::addLineItem,
                        onRemoveItem = viewModel::removeLineItem
                    )
                }
            }
            
            // Overlay untuk menunjukkan proses penyimpanan sedang berlangsung
            if (state.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Menyimpan struk...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptReviewContent(
    state: ReceiptReviewState,
    onSaveClick: () -> Unit,
    onMerchantNameChange: (String) -> Unit,
    onDateChange: (Date) -> Unit,
    onCategoryChange: (String) -> Unit,
    onItemDescriptionChange: (Int, String) -> Unit,
    onItemQuantityChange: (Int, Int) -> Unit,
    onItemPriceChange: (Int, Double) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    val receipt = state.receipt ?: return
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Merchant name field
        OutlinedTextField(
            value = receipt.merchantName,
            onValueChange = onMerchantNameChange,
            label = { Text(stringResource(id = R.string.receipt_merchant)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date field - simplified, in a real app would show date picker
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = dateFormatter.format(receipt.date),
                onValueChange = { /* Will use date picker */ },
                label = { Text(stringResource(id = R.string.receipt_date)) },
                modifier = Modifier.weight(1f),
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category dropdown
        CategoryDropdown(
            selectedCategory = receipt.category,
            categories = state.availableCategories.map { it.id },
            onCategorySelected = onCategoryChange
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Line items section
        Text(
            text = stringResource(id = R.string.receipt_items),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Line items list
        receipt.items.forEachIndexed { index, item ->
            LineItemRow(
                item = item,
                onDescriptionChange = { onItemDescriptionChange(index, it) },
                onQuantityChange = { onItemQuantityChange(index, it) },
                onPriceChange = { onItemPriceChange(index, it) },
                onRemove = { onRemoveItem(index) }
            )
            
            if (index < receipt.items.size - 1) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        
        // Add item button
        TextButton(
            onClick = onAddItem,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add item")
            Spacer(modifier = Modifier.padding(4.dp))
            Text("Tambah Item")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Total section
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        OutlinedTextField(
            value = currencyFormatter.format(receipt.total),
            onValueChange = { /* read-only, dihitung otomatis */ },
            label = { Text(stringResource(id = R.string.receipt_total)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true
        )
        
        // Informasi tentang total otomatis
        Text(
            text = "Total dihitung otomatis dari total harga semua item",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Error message if any
        if (state.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Save button with enhanced design
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !state.isSaving
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.padding(4.dp))
            Text("Simpan Struk")
        }
        
        // Extra space at bottom for better scrolling
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.receipt_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LineItemRow(
    item: LineItem,
    onDescriptionChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onPriceChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val calculatedItemTotal = item.quantity * item.price
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Description
            OutlinedTextField(
                value = item.description,
                onValueChange = onDescriptionChange,
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity
                OutlinedTextField(
                    value = item.quantity.toString(),
                    onValueChange = { 
                        try {
                            onQuantityChange(it.toInt())
                        } catch (e: NumberFormatException) {
                            // Ignore invalid input
                        }
                    },
                    label = { Text("Jumlah") },
                    modifier = Modifier.weight(0.3f),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.padding(4.dp))
                
                // Price
                OutlinedTextField(
                    value = item.price.toString(),
                    onValueChange = { 
                        try {
                            onPriceChange(it.toDouble())
                        } catch (e: NumberFormatException) {
                            // Ignore invalid input
                        }
                    },
                    label = { Text("Harga") },
                    modifier = Modifier.weight(0.5f),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.padding(4.dp))
                
                // Remove button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Item subtotal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Subtotal: ${currencyFormatter.format(calculatedItemTotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
