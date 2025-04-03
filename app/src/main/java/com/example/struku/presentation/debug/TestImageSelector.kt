package com.example.struku.presentation.debug

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/**
 * Component for selecting test images for preprocessing
 */
@Composable
fun TestImageSelector(
    onImageSelected: (Bitmap) -> Unit,
    onProcessComparisons: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var testImages by remember { mutableStateOf<List<TestImage>>(emptyList()) }
    var selectedImage by remember { mutableStateOf<TestImage?>(null) }
    
    // Load test images from assets
    LaunchedEffect(Unit) {
        testImages = loadTestImages(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Test Images",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (testImages.isEmpty()) {
            // Display loading or empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading test images...")
            }
        } else {
            // Display image selection
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(testImages) { image ->
                    TestImageThumbnail(
                        image = image,
                        isSelected = image == selectedImage,
                        onClick = { 
                            selectedImage = image
                            onImageSelected(image.bitmap)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Selected image preview with actions
        selectedImage?.let { selected ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Selected: ${selected.name}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Image preview
                    Image(
                        bitmap = selected.bitmap.asImageBitmap(),
                        contentDescription = selected.name,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(selected.bitmap.width.toFloat() / selected.bitmap.height),
                        contentScale = ContentScale.Fit
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Process button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { 
                                onImageSelected(selected.bitmap)
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Process")
                        }
                        
                        Button(
                            onClick = {
                                onProcessComparisons(selected.bitmap)
                            }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compare Settings")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thumbnail for a test image
 */
@Composable
fun TestImageThumbnail(
    image: TestImage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        border = if (isSelected) {
            Modifier.border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
        } else {
            Modifier
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            // Image preview
            Image(
                bitmap = image.bitmap.asImageBitmap(),
                contentDescription = image.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Image name
            Text(
                text = image.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

/**
 * Data class for a test image
 */
data class TestImage(
    val name: String,
    val bitmap: Bitmap
)

/**
 * Load test images from assets
 */
private suspend fun loadTestImages(context: Context): List<TestImage> = withContext(Dispatchers.IO) {
    val images = mutableListOf<TestImage>()
    val assetManager = context.assets
    
    try {
        // Try to load from receipts folder
        val imageFiles = assetManager.list("receipts") ?: emptyArray()
        
        for (file in imageFiles) {
            if (file.endsWith(".jpg") || file.endsWith(".png") || file.endsWith(".jpeg")) {
                val bitmap = loadBitmapFromAssets(assetManager, "receipts/$file")
                bitmap?.let {
                    images.add(TestImage(file, it))
                }
            }
        }
        
        // If no receipt images found, use some dummy images for testing
        if (images.isEmpty()) {
            // Create dummy images
            val colors = listOf(0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF)
            for (i in 1..5) {
                val bitmap = Bitmap.createBitmap(300, 500, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(colors[i % colors.size].toInt())
                
                images.add(TestImage("Test Image $i", bitmap))
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        
        // Create dummy images in case of error
        val colors = listOf(0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF)
        for (i in 1..5) {
            val bitmap = Bitmap.createBitmap(300, 500, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(colors[i % colors.size].toInt())
            
            images.add(TestImage("Test Image $i", bitmap))
        }
    }
    
    images
}

/**
 * Load bitmap from assets
 */
private fun loadBitmapFromAssets(assetManager: AssetManager, path: String): Bitmap? {
    return try {
        val inputStream: InputStream = assetManager.open(path)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}