package com.example.struku.presentation.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Component for selecting test images
 */
@Composable
fun TestImageSelector(
    onImageSelected: (Bitmap) -> Unit,
    onProcessComparisons: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var testImages by remember { mutableStateOf<List<TestImage>>(emptyList()) }
    var selectedImage by remember { mutableStateOf<TestImage?>(null) }
    
    // Load test images
    LaunchedEffect(Unit) {
        testImages = withContext(Dispatchers.IO) {
            loadTestImages(context)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Test Images",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            "Select an image to test preprocessing and OCR",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Test image grid
        if (testImages.isEmpty()) {
            Text(
                "No test images available",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(220.dp) // Fixed height
            ) {
                items(testImages) { image ->
                    TestImageItem(
                        image = image,
                        isSelected = selectedImage == image,
                        onClick = { selectedImage = image }
                    )
                }
            }
        }
        
        // Selected image actions
        selectedImage?.let { image ->
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Selected image preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image preview
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = image.bitmap.asImageBitmap(),
                            contentDescription = image.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Image details and actions
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        image.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        "${image.bitmap.width} x ${image.bitmap.height}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onImageSelected(image.bitmap) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Process")
                        }
                        
                        Button(
                            onClick = { onProcessComparisons(image.bitmap) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compare")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestImageItem(
    image: TestImage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Image(
                    bitmap = image.bitmap.asImageBitmap(),
                    contentDescription = image.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Text(
                text = image.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Model for test images
 */
data class TestImage(
    val name: String,
    val bitmap: Bitmap
)

/**
 * Load test images from assets
 */
private fun loadTestImages(context: Context): List<TestImage> {
    return try {
        val assetManager = context.assets
        val testImageFiles = assetManager.list("test_receipts") ?: return emptyList()
        
        testImageFiles.mapNotNull { fileName ->
            try {
                val inputStream = assetManager.open("test_receipts/$fileName")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                val name = fileName.substringBeforeLast(".")
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { it.capitalize() }
                
                TestImage(name, bitmap)
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun String.capitalize(): String {
    return if (isNotEmpty()) this[0].uppercase() + substring(1) else this
}
