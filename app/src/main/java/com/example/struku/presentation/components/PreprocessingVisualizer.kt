package com.example.struku.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * A component for visualizing image preprocessing steps
 */
@Composable
fun PreprocessingVisualizer(
    image: Bitmap?,
    currentStep: ProcessingStep,
    onStepSelected: (ProcessingStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Display the current image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = "Preprocessing visualization",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("No image to display")
            }
        }
        
        // Step selector
        Text(
            "Processing Step: ${currentStep.name}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(120.dp)
        ) {
            items(ProcessingStep.values()) { step ->
                StepButton(
                    step = step,
                    isSelected = step == currentStep,
                    onClick = { onStepSelected(step) }
                )
            }
        }
    }
}

@Composable
private fun StepButton(
    step: ProcessingStep,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Enum representing the different preprocessing steps
 */
enum class ProcessingStep {
    ORIGINAL,
    GRAYSCALE,
    NOISE_REDUCTION,
    CONTRAST,
    THRESHOLD,
    DESKEW,
    FINAL;
    
    val displayName: String
        get() = when (this) {
            ORIGINAL -> "Original"
            GRAYSCALE -> "Grayscale"
            NOISE_REDUCTION -> "Noise Reduction"
            CONTRAST -> "Contrast"
            THRESHOLD -> "Threshold"
            DESKEW -> "Deskew"
            FINAL -> "Final Result"
        }
}