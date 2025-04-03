package com.example.struku.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.ReceiptRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for manual receipt entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptScreen(
    navController: NavController,
    receiptRepository: ReceiptRepository
) {
    var merchantName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Item input state
    var itemName by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf("1") }
    var itemPrice by remember { mutableStateOf("") }
    
    // List of items
    val items = remember { mutableStateListOf<LineItem>() }
    
    // Calculate total from items
    val totalAmount = items.sumOf { it.price * it.quantity }
    val formattedTotal = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalAmount)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Struk Manual") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Save receipt
                        if (merchantName.isNotBlank()) {
                            val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date) ?: Date()
                            val receipt = Receipt(
                                id = 0, // Auto-generated
                                merchantName = merchantName,
                                date = parsedDate,
                                total = totalAmount,
                                category = category,
                                notes = notes,
                                imageUri = null,
                                items = items.toList()
                            )
                            
                            CoroutineScope(Dispatchers.IO).launch {
                                val id = receiptRepository.insertReceipt(receipt)
                                // Navigate back after save
                                launch(Dispatchers.Main) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = merchantName,
                onValueChange = { merchantName = it },
                label = { Text("Nama Toko") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Tanggal (DD/MM/YYYY)") },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Kategori") },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Item section
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
                        text = "Tambah Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Nama Item") },
                        modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.weight(0.3f)
                        )
                        
                        OutlinedTextField(
                            value = itemPrice,
                            onValueChange = { itemPrice = it },
                            label = { Text("Harga Satuan") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Items list
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
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        items.forEachIndexed { index, item ->
                            if (index > 0) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    // Save receipt
                    if (merchantName.isNotBlank()) {
                        val parsedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date) ?: Date()
                        val receipt = Receipt(
                            id = 0, // Auto-generated
                            merchantName = merchantName,
                            date = parsedDate,
                            total = totalAmount,
                            category = category,
                            notes = notes,
                            imageUri = null,
                            items = items.toList()
                        )
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            val id = receiptRepository.insertReceipt(receipt)
                            // Navigate back after save
                            launch(Dispatchers.Main) {
                                navController.popBackStack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = merchantName.isNotBlank() && items.isNotEmpty()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("SIMPAN")
            }
        }
    }
}