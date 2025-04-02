package com.example.struku.presentation.analytics

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.struku.R
import com.example.struku.domain.model.Category
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.screen_analytics)) }
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
                state.error != null -> {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                state.expensesByCategory.isEmpty() -> {
                    EmptyAnalyticsMessage(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    AnalyticsContent(state = state, onMonthYearChanged = viewModel::changeMonth)
                }
            }
        }
    }
}

@Composable
fun AnalyticsContent(
    state: AnalyticsState,
    onMonthYearChanged: (Int, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Calculate total spending for this month
    val totalSpending = state.expensesByCategory.values.sum()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Month selector
        MonthYearSelector(
            currentMonth = state.month,
            currentYear = state.year,
            onMonthYearChanged = onMonthYearChanged
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Monthly spending card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.analytics_monthly_spending),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = currencyFormat.format(totalSpending),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Category breakdown
        Text(
            text = stringResource(id = R.string.analytics_category_breakdown),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pie chart for category breakdown
        PieChart(
            data = state.expensesByCategory,
            colors = getCategoryColors(state.expensesByCategory.keys.toList()),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Category list with percentages
        state.expensesByCategory.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
            val percentage = if (totalSpending > 0) (amount / totalSpending * 100) else 0.0
            CategoryItem(
                category = category,
                amount = amount,
                percentage = percentage,
                totalSpending = totalSpending,
                currencyFormat = currencyFormat
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Budget status if available
        if (state.budgetByCategory.any { it.value != null }) {
            Text(
                text = stringResource(id = R.string.analytics_budget_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            state.budgetByCategory.entries
                .filter { it.value != null }
                .forEach { (category, budget) ->
                    budget?.let {
                        val spent = state.expensesByCategory[category] ?: 0.0
                        val percentage = if (budget > 0) (spent / budget * 100) else 0.0
                        BudgetItem(
                            category = category,
                            spent = spent,
                            budget = budget,
                            percentage = percentage,
                            currencyFormat = currencyFormat
                        )
                    }
                }
        }
        
        // Extra space at bottom
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MonthYearSelector(
    currentMonth: Int,
    currentYear: Int,
    onMonthYearChanged: (Int, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val currentRealMonth = calendar.get(Calendar.MONTH) + 1
    val currentRealYear = calendar.get(Calendar.YEAR)
    
    // Generate list of last 12 months
    val months = mutableListOf<Pair<Int, Int>>() // month, year
    val cal = Calendar.getInstance()
    cal.set(Calendar.MONTH, currentRealMonth - 1)
    cal.set(Calendar.YEAR, currentRealYear)
    
    // Add current month and 11 previous months
    repeat(12) {
        months.add(Pair(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)))
        cal.add(Calendar.MONTH, -1)
    }
    
    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val cal2 = Calendar.getInstance()
    cal2.set(Calendar.MONTH, currentMonth - 1)
    cal2.set(Calendar.YEAR, currentYear)
    val currentMonthText = dateFormat.format(cal2.time)
    
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentMonthText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select month",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            months.forEach { (month, year) ->
                cal2.set(Calendar.MONTH, month - 1)
                cal2.set(Calendar.YEAR, year)
                val monthText = dateFormat.format(cal2.time)
                
                DropdownMenuItem(
                    text = { Text(monthText) },
                    onClick = {
                        onMonthYearChanged(month, year)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PieChart(
    data: Map<String, Double>,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val total = data.values.sum()
    val angles = data.mapValues { (_, value) -> 360 * (value / total) }
    
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        
        var startAngle = 270f // Start from top (270 degrees)
        
        data.forEach { (category, value) ->
            val sweepAngle = angles[category]?.toFloat() ?: 0f
            val color = colors[category] ?: Color.Gray
            
            // Draw pie segment
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            // Draw outline
            drawArc(
                color = Color.White,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                style = Stroke(width = 2f),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryItem(
    category: String,
    amount: Double,
    percentage: Double,
    totalSpending: Double,
    currencyFormat: NumberFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category color dot
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(getCategoryColor(category))
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Category name
        Text(
            text = category,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        // Percentage
        Text(
            text = String.format("%.1f%%", percentage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Amount
        Text(
            text = currencyFormat.format(amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
    
    // Proportional divider
    Box(
        modifier = Modifier
            .fillMaxWidth(percentage.toFloat() / 100)
            .height(2.dp)
            .background(getCategoryColor(category))
    )
}

@Composable
fun BudgetItem(
    category: String,
    spent: Double,
    budget: Double,
    percentage: Double,
    currencyFormat: NumberFormat
) {
    val isOverBudget = spent > budget
    val progressColor = if (isOverBudget) {
        MaterialTheme.colorScheme.error
    } else if (percentage > 80) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = "${currencyFormat.format(spent)} / ${currencyFormat.format(budget)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = (percentage / 100).coerceAtMost(1f).toFloat(),
            modifier = Modifier.fillMaxWidth(),
            color = progressColor
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Status text
        if (isOverBudget) {
            Text(
                text = "Melebihi anggaran ${currencyFormat.format(spent - budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.End)
            )
        } else {
            Text(
                text = "Tersisa ${currencyFormat.format(budget - spent)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
    
    Divider()
}

@Composable
fun EmptyAnalyticsMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Belum ada data pengeluaran",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tambahkan beberapa struk untuk melihat analitik pengeluaran Anda",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Helper function to get color for a category
 */
fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "groceries", "bahan makanan" -> Color(0xFF4CAF50)
        "dining", "makan di luar" -> Color(0xFFFF9800)
        "transport", "transportasi" -> Color(0xFF2196F3)
        "shopping", "belanja" -> Color(0xFFE91E63)
        "utilities", "tagihan" -> Color(0xFF9C27B0)
        "health", "kesehatan" -> Color(0xFFF44336)
        "entertainment", "hiburan" -> Color(0xFFFF5722)
        else -> Color(0xFF607D8B)
    }
}

/**
 * Helper function to get colors for multiple categories
 */
fun getCategoryColors(categories: List<String>): Map<String, Color> {
    return categories.associateWith { getCategoryColor(it) }
}
