package com.example.struku.presentation.receipts

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import com.example.struku.presentation.NavRoutes
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: Int,
    navController: NavController,
    viewModel: ReceiptDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Load receipt data
    androidx.compose.runtime.LaunchedEffect(receiptId) {
        viewModel.loadReceipt(receiptId)
    }
    
    // Handle receipt deletion
    if (state.isDeleted) {
        navController.popBackStack()
        return
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Struk") },
            text = { Text("Apakah Anda yakin ingin menghapus struk ini? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReceipt()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Struk") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Share button
                    IconButton(onClick = { /* TODO: Implement share feature */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    
                    // Edit button
                    IconButton(onClick = {
                        navController.navigate(NavRoutes.receiptReview(receiptId))
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    
                    // Delete button
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
                    ReceiptDetailContent(receipt = state.receipt)
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailContent(receipt: Receipt) {
    val scrollState = rememberScrollState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Merchant information
        Text(
            text = receipt.merchantName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Date
        Text(
            text = "Tanggal: ${dateFormat.format(receipt.date)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Category
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = receipt.category,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Items
        Text(
            text = "Daftar Item",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (receipt.items.isEmpty()) {
            Text(
                text = "Tidak ada item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            receipt.items.forEach { item ->
                LineItemDetail(item = item, currencyFormat = currencyFormat)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        
        // Total
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currencyFormat.format(receipt.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Notes if any
        if (!receipt.notes.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Catatan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = receipt.notes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Extra space at the bottom
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LineItemDetail(
    item: LineItem,
    currencyFormat: NumberFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyLarge
            )
            if (item.quantity > 1 || item.unitPrice != null) {
                val quantityText = "${item.quantity} x ${item.unitPrice?.let { currencyFormat.format(it) } ?: ""}"
                Text(
                    text = quantityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = currencyFormat.format(item.price),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
