package com.example.struku.presentation.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
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
import javax.inject.Inject

/**
 * View model for managing categories
 */
@HiltViewModel
class CategoryViewModel @Inject constructor() : ViewModel() {
    
    private val _state = MutableStateFlow(CategoryState())
    val state: StateFlow<CategoryState> = _state.asStateFlow()
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            // In a real application, this would come from a repository
            val defaultCategories = Category.DEFAULT_CATEGORIES.toMutableList()
            _state.value = CategoryState(
                categories = defaultCategories,
                isLoading = false
            )
        }
    }
    
    fun toggleCategoryVisibility(categoryId: String) {
        _state.value = _state.value.copy(
            categories = _state.value.categories.map { 
                if (it.id == categoryId) it.copy(visible = !it.visible) else it 
            }
        )
    }
    
    fun addCategory(category: Category) {
        _state.value = _state.value.copy(
            categories = _state.value.categories + category
        )
    }
    
    fun updateCategory(updatedCategory: Category) {
        _state.value = _state.value.copy(
            categories = _state.value.categories.map { 
                if (it.id == updatedCategory.id) updatedCategory else it 
            }
        )
    }
    
    fun deleteCategory(categoryId: String) {
        _state.value = _state.value.copy(
            categories = _state.value.categories.filter { it.id != categoryId }
        )
    }
    
    fun reorderCategories(fromIndex: Int, toIndex: Int) {
        val mutableList = _state.value.categories.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
        _state.value = _state.value.copy(categories = mutableList)
    }
}

/**
 * State holder for the category management screen
 */
data class CategoryState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Screen for managing expense categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    onBackPressed: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Kategori") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah Kategori",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
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
            } else if (state.categories.isEmpty()) {
                EmptyCategoriesState()
            } else {
                Text(
                    text = "Personalisasi kategori pengeluaran Anda",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories) { category ->
                        CategoryListItem(
                            category = category,
                            onToggleVisibility = { viewModel.toggleCategoryVisibility(category.id) },
                            onEdit = { categoryToEdit = category },
                            onDelete = { 
                                categoryToDelete = category
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
            
            // Error display
            if (state.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Add/Edit Category Dialog
        if (showAddDialog || categoryToEdit != null) {
            CategoryDialog(
                category = categoryToEdit,
                onDismiss = { 
                    showAddDialog = false
                    categoryToEdit = null
                },
                onSave = { newCategory ->
                    if (categoryToEdit != null) {
                        viewModel.updateCategory(newCategory)
                    } else {
                        viewModel.addCategory(newCategory)
                    }
                    showAddDialog = false
                    categoryToEdit = null
                }
            )
        }
        
        // Delete Confirmation Dialog
        if (showDeleteConfirmation && categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmation = false
                    categoryToDelete = null
                },
                title = { Text("Hapus Kategori") },
                text = { 
                    Text("Apakah Anda yakin ingin menghapus kategori '${categoryToDelete?.name}'? Semua transaksi dalam kategori ini akan dipindahkan ke kategori 'Lainnya'.") 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            categoryToDelete?.let { viewModel.deleteCategory(it.id) }
                            showDeleteConfirmation = false
                            categoryToDelete = null
                        }
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDeleteConfirmation = false
                            categoryToDelete = null
                        }
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

/**
 * Dialog for adding or editing a category
 */
@Composable
fun CategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    val isEditing = category != null
    val title = if (isEditing) "Edit Kategori" else "Tambah Kategori"
    
    // Default color if creating a new category
    val initialColor = category?.color ?: 0xFF5C6BC0.toInt()
    
    var name by remember { mutableStateOf(category?.name ?: "") }
    var color by remember { mutableStateOf(initialColor) }
    var iconName by remember { mutableStateOf(category?.iconName ?: "category") }
    var showColorPicker by remember { mutableStateOf(false) }
    
    val isValid = name.isNotBlank()
    
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category preview
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .clickable { showColorPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    // In a real app, you'd display an icon here based on iconName
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "Ketuk lingkaran untuk mengubah warna",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Simple color picker (in a real app, you'd have a more sophisticated color picker)
                AnimatedVisibility(
                    visible = showColorPicker,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "Pilih Warna Kategori",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val colorOptions = listOf(
                            0xFF5C6BC0.toInt(), // Indigo
                            0xFFEC407A.toInt(), // Pink
                            0xFF66BB6A.toInt(), // Green
                            0xFFFFA726.toInt(), // Orange
                            0xFF42A5F5.toInt(), // Blue
                            0xFFEF5350.toInt(), // Red
                            0xFF8D6E63.toInt(), // Brown
                            0xFF26A69A.toInt(), // Teal
                            0xFF9575CD.toInt(), // Deep Purple
                            0xFFD4E157.toInt(), // Lime
                            0xFF29B6F6.toInt(), // Light Blue
                            0xFFFFEE58.toInt()  // Yellow
                        )
                        
                        // Color grid
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (i in 0 until 4) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (j in 0 until 3) {
                                        val index = i * 3 + j
                                        if (index < colorOptions.size) {
                                            val colorOption = colorOptions[index]
                                            ColorOption(
                                                color = colorOption,
                                                isSelected = color == colorOption,
                                                onClick = { 
                                                    color = colorOption
                                                    showColorPicker = false
                                                }
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }
                            }
                        }
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
                            val newCategory = if (isEditing && category != null) {
                                category.copy(
                                    name = name,
                                    color = color,
                                    iconName = iconName
                                )
                            } else {
                                Category(
                                    id = "custom_${System.currentTimeMillis()}",
                                    name = name,
                                    color = color,
                                    iconName = iconName,
                                    visible = true
                                )
                            }
                            onSave(newCategory)
                        },
                        enabled = isValid
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border color animation"
    )
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor.value,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

/**
 * List item for a category in the management screen
 */
@Composable
fun CategoryListItem(
    category: Category,
    onToggleVisibility: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val categoryColor = Color(category.color)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    .background(categoryColor),
                contentAlignment = Alignment.Center
            ) {
                // Show first letter of category name
                Text(
                    text = category.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Category name
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            // Visibility toggle
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    if (category.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (category.visible) "Sembunyikan" else "Tampilkan",
                    tint = if (category.visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            // More options menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opsi Lainnya"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            onEdit()
                            showMenu = false
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("Hapus") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Empty state when no categories are available
 */
@Composable
fun EmptyCategoriesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PieChart,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Belum Ada Kategori",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tambahkan kategori pengeluaran Anda untuk mempermudah pelacakan keuangan",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Text("Tambah Kategori")
        }
    }
}