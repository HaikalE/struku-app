package com.example.struku.presentation.analytics

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.struku.domain.model.Category
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Screen for displaying analytics and reports
 */
@Composable
fun AnalyticsScreen(
    onMonthClick: (year: Int, month: Int) -> Unit,
    onCategoryClick: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Month selector
        MonthYearSelector(
            currentMonth = state.month,
            currentYear = state.year,
            onMonthYearSelected = { month, year ->
                viewModel.changeMonth(month, year)
                onMonthClick(year, month)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Expense overview card
        ExpenseOverviewCard(state = state)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Category breakdown
        Text(
            text = "Breakdown Pengeluaran",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.expensesByCategory.isEmpty()) {
            EmptyAnalyticsState()
        } else {
            CategoryBreakdownList(
                state = state,
                onCategoryClick = onCategoryClick
            )
        }
        
        // Add a button to manage budgets
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onCategoryClick("budgets") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kelola Anggaran Bulanan")
        }
        
        // Error message if any
        if (state.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Space at the bottom for better scrolling
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MonthYearSelector(
    currentMonth: Int,
    currentYear: Int,
    onMonthYearSelected: (month: Int, year: Int) -> Unit
) {
    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    
    var showMonthDropdown by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Laporan Bulanan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous month button
                IconButton(
                    onClick = {
                        val (newMonth, newYear) = if (currentMonth == 1) {
                            Pair(12, currentYear - 1)
                        } else {
                            Pair(currentMonth - 1, currentYear)
                        }
                        onMonthYearSelected(newMonth, newYear)
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
                }
                
                // Current month/year display with dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { showMonthDropdown = true }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${monthNames[currentMonth - 1]} $currentYear",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Month")
                    }
                    
                    DropdownMenu(
                        expanded = showMonthDropdown,
                        onDismissRequest = { showMonthDropdown = false }
                    ) {
                        // Current year
                        monthNames.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text("$name $currentYear") },
                                onClick = {
                                    onMonthYearSelected(index + 1, currentYear)
                                    showMonthDropdown = false
                                }
                            )
                        }
                        
                        // Previous year
                        monthNames.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text("$name ${currentYear - 1}") },
                                onClick = {
                                    onMonthYearSelected(index + 1, currentYear - 1)
                                    showMonthDropdown = false
                                }
                            )
                        }
                    }
                }
                
                // Next month button
                IconButton(
                    onClick = {
                        val (newMonth, newYear) = if (currentMonth == 12) {
                            Pair(1, currentYear + 1)
                        } else {
                            Pair(currentMonth + 1, currentYear)
                        }
                        onMonthYearSelected(newMonth, newYear)
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }
        }
    }
}

@Composable
fun ExpenseOverviewCard(state: AnalyticsState) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Calculate total expenses and budget
    val totalExpenses = state.expensesByCategory.values.sum()
    val totalBudget = state.budgetByCategory.values.filterNotNull().sum()
    
    // Calculate overall progress
    val overallProgress = if (totalBudget > 0) (totalExpenses / totalBudget).coerceIn(0.0, 1.0) else 0.0
    val isOverBudget = totalExpenses > totalBudget
    
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
                text = "Total Pengeluaran",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = currencyFormat.format(totalExpenses),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (totalBudget > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isOverBudget) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Over Budget",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Text(
                        text = "Anggaran: ${currencyFormat.format(totalBudget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = overallProgress.toFloat(),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val progressText = String.format("%.1f%%", overallProgress * 100)
                Text(
                    text = if (isOverBudget) "Melebihi anggaran! ($progressText)" else "$progressText dari anggaran",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "Belum ada anggaran yang ditetapkan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownList(
    state: AnalyticsState,
    onCategoryClick: (categoryId: String) -> Unit
) {
    Column {
        state.expensesByCategory.forEach { (categoryId, amount) ->
            val category = Category.DEFAULT_CATEGORIES.find { it.id == categoryId } 
                           ?: Category("other", "Lainnya", 0xFF607D8B, "more_horiz", true)
            
            CategoryExpenseItem(
                category = category,
                expenseAmount = amount,
                budgetAmount = state.budgetByCategory[categoryId],
                percentUsed = state.percentByCategory[categoryId],
                onClick = { onCategoryClick(categoryId) }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CategoryExpenseItem(
    category: Category,
    expenseAmount: Double,
    budgetAmount: Double?,
    percentUsed: Double?,
    onClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val progress = percentUsed?.div(100)?.coerceIn(0.0, 1.0) ?: 0.0
    val categoryColor = Color(category.color)
    val isOverBudget = budgetAmount != null && expenseAmount > budgetAmount
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Here you would use an icon based on category.iconName
                // For simplicity, we're using a colored circle
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = currencyFormat.format(expenseAmount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (budgetAmount != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = progress.toFloat(),
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else categoryColor,
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = String.format("%.1f%%", percentUsed ?: 0.0),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    if (isOverBudget) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Melebihi anggaran ${currencyFormat.format(budgetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        text = "Belum ada anggaran",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAnalyticsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Get primary color from theme before Canvas call
            val primaryColor = MaterialTheme.colorScheme.primary
            
            // Simple doughnut chart illustration
            Canvas(modifier = Modifier.size(80.dp)) {
                val radius = size.minDimension / 3
                
                // Draw empty doughnut
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = radius / 2)
                )
                
                // Draw a small segment - using captured color instead of MaterialTheme.colorScheme inside Canvas
                drawArc(
                    color = primaryColor.copy(alpha = 0.7f),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = radius / 2)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Belum ada data pengeluaran",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tambahkan struk belanja untuk melihat analisis pengeluaran Anda",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}