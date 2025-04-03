package com.example.struku.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Warna yang khas dengan tema aplikasi keuangan dan pembacaan struk
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E88E5),          // Biru yang profesional
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF004881),
    secondary = Color(0xFF00A978),        // Hijau untuk positif/pendapatan
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1F0DE),
    onSecondaryContainer = Color(0xFF00563B),
    tertiary = Color(0xFFE53935),         // Merah untuk negatif/pengeluaran
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF410002),
    error = Color(0xFFB00020),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1D1B20),
    surface = Color.White,
    onSurface = Color(0xFF1D1B20),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497A),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFF4CD99F),
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF005237),
    onSecondaryContainer = Color(0xFFBDF2D6),
    tertiary = Color(0xFFFF8A85),
    onTertiary = Color(0xFF690002),
    tertiaryContainer = Color(0xFF93000A),
    onTertiaryContainer = Color(0xFFFFDAD6),
    error = Color(0xFFCF6679),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE5E1E6),
    surface = Color(0xFF1D1D1D),
    onSurface = Color(0xFFE5E1E6),
)

@Composable
fun StrukuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}