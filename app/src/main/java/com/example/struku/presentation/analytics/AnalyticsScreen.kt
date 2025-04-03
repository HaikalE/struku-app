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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.struku.domain.model.Category
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * Screen for displaying analytics and reports with enhanced visualization
 */
@Composable
fun AnalyticsScreen(
    onMonthClick: (year: Int, month: Int) -> Unit,
    onCategoryClick: (categoryId: String) -> Unit,
    onManageBudgetClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    var selectedVisualizationType by remember { mutableStateOf(VisualizationType.OVERVIEW) }
    
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
        
        // Visualization type filter
        VisualizationTypeFilter(
            selectedType = selectedVisualizationType,
            onTypeSelected = { selectedVisualizationType = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Expense overview card
        when (selectedVisualizationType) {
            VisualizationType.OVERVIEW -> ExpenseOverviewCard(state = state)
            VisualizationType.PIE_CHART -> ExpensePieChartCard(state = state)
            VisualizationType.TREND -> ExpenseTrendCard(state = state)
        }
        
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
        
        // Add buttons for managing budget and categories
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onManageBudgetClick() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kelola Anggaran")
            }
            
            Button(
                onClick = { onManageCategoriesClick() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PieChart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kelola Kategori")
            }
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
fun VisualizationTypeFilter(
    selectedType: VisualizationType,
    onTypeSelected: (VisualizationType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedType == VisualizationType.OVERVIEW,
            onClick = { onTypeSelected(VisualizationType.OVERVIEW) },
            label = { Text("Ikhtisar") },
            leadingIcon = {
                if (selectedType == VisualizationType.OVERVIEW) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
        
        FilterChip(
            selected = selectedType == VisualizationType.PIE_CHART,
            onClick = { onTypeSelected(VisualizationType.PIE_CHART) },
            label = { Text("Grafik Pie") },
            leadingIcon = {
                if (selectedType == VisualizationType.PIE_CHART) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
        
        FilterChip(
            selected = selectedType == VisualizationType.TREND,
            onClick = { onTypeSelected(VisualizationType.TREND) },
            label = { Text("Tren") },
            leadingIcon = {
                if (selectedType == VisualizationType.TREND) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
    }
}

@Composable
fun ExpensePieChartCard(state: AnalyticsState) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Calculate total expenses
    val totalExpenses = state.expensesByCategory.values.sum()
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Distribusi Pengeluaran",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Display pie chart only if we have expenses
                if (totalExpenses > 0) {
                    PieChart(
                        data = state.expensesByCategory,
                        availableCategories = Category.DEFAULT_CATEGORIES
                    )
                } else {
                    // Show empty state
                    Text(
                        text = "Belum ada data pengeluaran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend for the pie chart
            Column {
                state.expensesByCategory.entries.sortedByDescending { it.value }.forEach { (categoryId, amount) ->
                    val category = Category.DEFAULT_CATEGORIES.find { it.id == categoryId }
                        ?: Category("other", "Lainnya", 0xFF607D8B, "more_horiz", true)
                    
                    val percentage = if (totalExpenses > 0) amount / totalExpenses * 100 else 0.0
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(category.color))
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = String.format("%.1f%%", percentage),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = currencyFormat.format(amount),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    data: Map<String, Double>,
    availableCategories: List<Category>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    
    Canvas(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp)
    ) {
        if (total <= 0) return@Canvas
        
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        
        // Draw pie slices
        var startAngle = 0f
        
        data.forEach { (categoryId, value) ->
            val category = availableCategories.find { it.id == categoryId }
                ?: Category("other", "Lainnya", 0xFF607D8B, "more_horiz", true)
            
            val sweepAngle = (value / total * 360).toFloat()
            
            // Draw slice
            drawArc(
                color = Color(category.color),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            // Draw border around slice
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
        
        // Draw a hole in the center to make it a donut chart
        drawCircle(
            color = MaterialTheme.colorScheme.surface,
            radius = radius * 0.6f,
            center = center
        )
    }
}

@Composable
fun ExpenseTrendCard(state: AnalyticsState) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    
    // Sample trend data (would be fetched from actual trends in a real implementation)
    val previousMonthExpenses = state.expensesByCategory.values.sum() * 0.9 // Example previous month data
    val monthBeforeExpenses = state.expensesByCategory.values.sum() * 0.85 // Example month before previous
    
    val currentTotal = state.expensesByCategory.values.sum()
    
    // Calculate trend percentage
    val trendPercentage = if (previousMonthExpenses > 0) 
        ((currentTotal - previousMonthExpenses) / previousMonthExpenses) * 100 
    else 
        0.0
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
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
                Text(
                    text = "Tren Pengeluaran",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                // Trend indicator
                val trendColor = when {
                    trendPercentage > 5 -> MaterialTheme.colorScheme.error
                    trendPercentage < -5 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondary
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = trendColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (trendPercentage >= 0) Icons.Default.TrendingUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = String.format("%+.1f%%", trendPercentage),
                            style = MaterialTheme.typography.bodySmall,
                            color = trendColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Determine max value for scaling
                val maxValue = maxOf(monthBeforeExpenses, previousMonthExpenses, currentTotal)
                
                // Month before previous
                TrendBar(
                    value = monthBeforeExpenses,
                    maxValue = maxValue,
                    label = getMonthName(state.month - 2),
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                // Previous month
                TrendBar(
                    value = previousMonthExpenses,
                    maxValue = maxValue,
                    label = getMonthName(state.month - 1),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                // Current month
                TrendBar(
                    value = currentTotal,
                    maxValue = maxValue,
                    label = getMonthName(state.month),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Insights or suggestions based on trends
            val message = when {
                trendPercentage > 10 -> "Pengeluaran bulan ini meningkat signifikan. Pertimbangkan untuk mengurangi beberapa pengeluaran non-esensial."
                trendPercentage > 5 -> "Pengeluaran bulan ini sedikit meningkat dibanding bulan lalu."
                trendPercentage < -10 -> "Selamat! Pengeluaran bulan ini menurun drastis. Tetap jaga pola pengeluaran yang baik ini."
                trendPercentage < -5 -> "Pengeluaran bulan ini menurun dibanding bulan lalu. Kerja bagus!"
                else -> "Pengeluaran bulan ini relatif stabil dibanding bulan lalu."
            }
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TrendBar(
    value: Double,
    maxValue: Double,
    label: String,
    color: Color,
    modifier: Modifier = Modifier.width(60.dp)
) {
    val heightPercentage = if (maxValue > 0) min(1f, (value / maxValue).toFloat()) else 0f
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Value text
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        Text(
            text = currencyFormat.format(value).split(",")[0], // Show only the main part of currency
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(80.dp.times(heightPercentage))
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(color)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Month label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp
        )
    }
}

private fun getMonthName(month: Int): String {
    val adjustedMonth = ((month - 1) % 12 + 12) % 12 + 1 // Handle negative months
    
    return when (adjustedMonth) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "Mei"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Agu"
        9 -> "Sep"
        10 -> "Okt"
        11 -> "Nov"
        12 -> "Des"
        else -> ""
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
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
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
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val progressText = String.format("%.1f%%", overallProgress * 100)
                Text(
                    text = if (isOverBudget) "Melebihi anggaran! ($progressText)" else "$progressText dari anggaran",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                if (isOverBudget) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Anda melebihi anggaran sebesar ${currencyFormat.format(totalExpenses - totalBudget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Text(
                    text = "Belum ada anggaran yang ditetapkan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { /* Handle navigation to budget settings */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Tetapkan Anggaran")
                }
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
        state.expensesByCategory.entries.sortedByDescending { it.value }.forEach { (categoryId, amount) ->
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
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
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

enum class VisualizationType {
    OVERVIEW,
    PIE_CHART,
    TREND
}