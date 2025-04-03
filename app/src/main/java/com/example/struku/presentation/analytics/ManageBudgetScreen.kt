package com.example.struku.presentation.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * View model for managing budget
 */
@HiltViewModel
class BudgetViewModel @Inject constructor() : ViewModel() {
    
    private val _state = MutableStateFlow(BudgetState())
    val state: StateFlow<BudgetState> = _state.asStateFlow()
    
    init {
        loadBudgetData()
    }
    
    private fun loadBudgetData() {
        viewModelScope.launch {
            // In a real application, this would be loaded from a repository
            val totalBudget = 5000000.0 // 5 juta rupiah
            
            val categoryBudgets = mutableMapOf<String, Double>()
            val expenses = mutableMapOf<String, Double>()
            
            // Sample data for each category
            Category.DEFAULT_CATEGORIES.forEach { category ->
                // Random budget between 500k and 1.5M
                val budget = (500000..1500000).random().toDouble()
                categoryBudgets[category.id] = budget
                
                // Random expense between 20% and 110% of budget
                val percentUsed = (20..110).random() / 100.0
                expenses[category.id] = budget * percentUsed
            }
            
            _state.value = BudgetState(
                totalBudget = totalBudget,
                budgetByCategory = categoryBudgets,
                expensesByCategory = expenses,
                isLoading = false
            )
        }
    }
    
    fun updateTotalBudget(amount: Double) {
        _state.value = _state.value.copy(totalBudget = amount)
    }
    
    fun updateCategoryBudget(categoryId: String, amount: Double) {
        val newBudgetMap = _state.value.budgetByCategory.toMutableMap()
        newBudgetMap[categoryId] = amount
        _state.value = _state.value.copy(budgetByCategory = newBudgetMap)
    }
    
    fun resetAllBudgets() {
        val emptyBudgets = _state.value.budgetByCategory.keys.associateWith { 0.0 }
        _state.value = _state.value.copy(
            totalBudget = 0.0,
            budgetByCategory = emptyBudgets
        )
    }
    
    fun redistributeBudgetsEvenly() {
        val categories = _state.value.budgetByCategory.keys
        if (categories.isNotEmpty() && _state.value.totalBudget > 0) {
            val amountPerCategory = _state.value.totalBudget / categories.size
            val evenBudgets = categories.associateWith { amountPerCategory }
            _state.value = _state.value.copy(budgetByCategory = evenBudgets)
        }
    }
}

/**
 * State class for budget management
 */
data class BudgetState(
    val totalBudget: Double = 0.0,
    val budgetByCategory: Map<String, Double> = emptyMap(),
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Screen for managing budget
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBudgetScreen(
    onBackPressed: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showTotalBudgetDialog by remember { mutableStateOf(false) }
    var showCategoryBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    
    // Format for currency
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Anggaran") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Budget distribution options
                    var showBudgetMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        IconButton(onClick = { showBudgetMenu = true }) {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Opsi Anggaran",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showBudgetMenu,
                            onDismissRequest = { showBudgetMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Distribusi Merata") },
                                onClick = {
                                    viewModel.redistributeBudgetsEvenly()
                                    showBudgetMenu = false
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Reset Semua Anggaran") },
                                onClick = {
                                    showResetConfirmation = true
                                    showBudgetMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTotalBudgetDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                text = { Text("Ubah Anggaran Total") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Total Budget Card
                TotalBudgetCard(
                    totalBudget = state.totalBudget,
                    totalExpenses = state.expensesByCategory.values.sum(),
                    onEditClick = { showTotalBudgetDialog = true }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Category Budget List
                Text(
                    text = "Anggaran per Kategori",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (state.budgetByCategory.isEmpty()) {
                    EmptyBudgetState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            Category.DEFAULT_CATEGORIES.filter { category ->
                                state.budgetByCategory.containsKey(category.id)
                            }
                        ) { category ->
                            val budget = state.budgetByCategory[category.id] ?: 0.0
                            val expense = state.expensesByCategory[category.id] ?: 0.0
                            
                            CategoryBudgetItem(
                                category = category,
                                budget = budget,
                                expense = expense,
                                onClick = {
                                    selectedCategory = category
                                    showCategoryBudgetDialog = true
                                }
                            )
                        }
                    }
                }
                
                // Error message if any
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Dialogs
        if (showTotalBudgetDialog) {
            TotalBudgetDialog(
                currentAmount = state.totalBudget,
                onDismiss = { showTotalBudgetDialog = false },
                onSave = { amount ->
                    viewModel.updateTotalBudget(amount)
                    showTotalBudgetDialog = false
                }
            )
        }
        
        if (showCategoryBudgetDialog && selectedCategory != null) {
            val categoryId = selectedCategory!!.id
            val currentBudget = state.budgetByCategory[categoryId] ?: 0.0
            
            CategoryBudgetDialog(
                category = selectedCategory!!,
                currentAmount = currentBudget,
                maxAmount = state.totalBudget,
                onDismiss = {
                    showCategoryBudgetDialog = false
                    selectedCategory = null
                },
                onSave = { amount ->
                    viewModel.updateCategoryBudget(categoryId, amount)
                    showCategoryBudgetDialog = false
                    selectedCategory = null
                }
            )
        }
        
        if (showResetConfirmation) {
            AlertDialog(
                onDismissRequest = { showResetConfirmation = false },
                title = { Text("Reset Anggaran") },
                text = { Text("Apakah Anda yakin ingin menghapus semua anggaran yang telah ditetapkan?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetAllBudgets()
                            showResetConfirmation = false
                        }
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmation = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

/**
 * Dialog for editing total budget
 */
@Composable
fun TotalBudgetDialog(
    currentAmount: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountInput by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }
    var hasError by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Tetapkan Anggaran Total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { 
                        amountInput = it.filter { char -> char.isDigit() }
                        hasError = false
                    },
                    label = { Text("Jumlah Anggaran (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasError,
                    supportingText = {
                        if (hasError) {
                            Text("Masukkan jumlah anggaran yang valid")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Anggaran total akan didistribusikan ke setiap kategori pengeluaran",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val amountValue = amountInput.toDoubleOrNull()
                            if (amountValue != null && amountValue >= 0) {
                                onSave(amountValue)
                            } else {
                                hasError = true
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for editing category budget
 */
@Composable
fun CategoryBudgetDialog(
    category: Category,
    currentAmount: Double,
    maxAmount: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountInput by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }
    var hasError by remember { mutableStateOf(false) }
    val categoryColor = Color(category.color)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category color indicator
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(categoryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "Anggaran untuk ${category.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { 
                        amountInput = it.filter { char -> char.isDigit() }
                        hasError = false
                    },
                    label = { Text("Jumlah Anggaran (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasError,
                    supportingText = {
                        if (hasError) {
                            Text("Masukkan jumlah anggaran yang valid")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (maxAmount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Anggaran total: ${NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(maxAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val amountValue = amountInput.toDoubleOrNull()
                            if (amountValue != null && amountValue >= 0) {
                                onSave(amountValue)
                            } else {
                                hasError = true
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

/**
 * Card showing total budget information
 */
@Composable
fun TotalBudgetCard(
    totalBudget: Double,
    totalExpenses: Double,
    onEditClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val progressPercent = if (totalBudget > 0) (totalExpenses / totalBudget).coerceIn(0.0, 1.0) else 0.0
    val isOverBudget = totalExpenses > totalBudget
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anggaran Bulanan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Anggaran",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Anggaran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = currencyFormat.format(totalBudget),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Pengeluaran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        text = currencyFormat.format(totalExpenses),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress indicator
            AnimatedVisibility(visible = totalBudget > 0) {
                Column {
                    LinearProgressIndicator(
                        progress = progressPercent.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        strokeCap = StrokeCap.Round
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format("%.1f%%", progressPercent * 100),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        if (isOverBudget) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Text(
                                    text = "Melebihi anggaran",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text(
                                text = "Tersisa: ${currencyFormat.format(totalBudget - totalExpenses)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Show message if no budget is set
            if (totalBudget <= 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Tetapkan anggaran bulanan Anda untuk melacak pengeluaran dengan lebih efektif",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Tetapkan Anggaran")
                }
            }
        }
    }
}

/**
 * List item for a category budget
 */
@Composable
fun CategoryBudgetItem(
    category: Category,
    budget: Double,
    expense: Double,
    onClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val categoryColor = Color(category.color)
    val progress = if (budget > 0) (expense / budget).coerceIn(0.0, 1.0) else 0.0
    val isOverBudget = expense > budget
    
    var expanded by remember { mutableStateOf(false) }
    val progressAnimation by animateFloatAsState(
        targetValue = progress.toFloat(),
        label = "progress animation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category color indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (budget > 0) {
                        Text(
                            text = currencyFormat.format(budget),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            text = "Belum ada anggaran",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (budget > 0 && expense > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isOverBudget) "Melebihi" else "Terpakai",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = String.format("%.1f%%", progress * 100),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Sembunyikan detail" else "Tampilkan detail"
                    )
                }
            }
            
            if (budget > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = progressAnimation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else categoryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
            
            // Expanded details
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Anggaran",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = currencyFormat.format(budget),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Pengeluaran",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = currencyFormat.format(expense),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOverBudget) "Terlampaui:" else "Tersisa:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = currencyFormat.format(if (isOverBudget) expense - budget else budget - expense),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onClick,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = categoryColor.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ubah Anggaran")
                    }
                }
            }
        }
    }
}

/**
 * Empty state when no budgets are set
 */
@Composable
fun EmptyBudgetState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Belum Ada Anggaran yang Ditetapkan",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tetapkan anggaran untuk setiap kategori pengeluaran Anda untuk melacak keuangan dengan lebih efektif",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { /* This would be handled by the FloatingActionButton */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tetapkan Anggaran")
        }
    }
}