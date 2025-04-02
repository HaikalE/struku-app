package com.example.struku.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.struku.presentation.auth.AuthManager
import com.example.struku.presentation.auth.AuthState
import com.example.struku.presentation.auth.AuthenticationScreen
import com.example.struku.presentation.theme.StrukuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var authManager: AuthManager
    
    // Permission launcher for requesting camera permission
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Camera permission granted")
            // Permission granted, proceed with camera functionality
        } else {
            Log.d("MainActivity", "Camera permission denied")
            // Permission denied, show a message or handle accordingly
        }
    }
    
    // Permission launcher for multiple permissions
    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, isGranted) ->
            Log.d("MainActivity", "Permission: $permission, granted: $isGranted")
        }
    }
    
    companion object {
        // Define our own navigation routes
        private const val AUTH_ROUTE = "auth"
        private const val MAIN_APP_ROUTE = "main_app"
        
        // List of required permissions
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.USE_BIOMETRIC
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check and request permissions
        requestPermissionsIfNeeded()
        
        setContent {
            StrukuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create a top-level NavController
                    val navController = rememberNavController()
                    
                    // Collect the auth state
                    val authState by authManager.authState.collectAsState()
                    
                    // Log the current auth state for debugging
                    LaunchedEffect(authState) {
                        Log.d("MainActivity", "Current auth state: $authState")
                        
                        // Navigate based on auth state
                        when (authState) {
                            AuthState.LOCKED -> navController.navigate(AUTH_ROUTE) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                            AuthState.UNLOCKED -> navController.navigate(MAIN_APP_ROUTE) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    }
                    
                    // Set up top-level navigation
                    NavHost(
                        navController = navController,
                        startDestination = AUTH_ROUTE
                    ) {
                        // Authentication route
                        composable(AUTH_ROUTE) {
                            AuthenticationScreen(
                                onAuthenticate = {
                                    authManager.authenticate(this@MainActivity)
                                }
                            )
                        }
                        
                        // Main app route - this directly embeds StrukuApp which has the bottom navigation
                        composable(MAIN_APP_ROUTE) {
                            StrukuApp()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check and request permissions if not granted
     */
    private fun requestPermissionsIfNeeded() {
        Log.d("MainActivity", "Checking permissions...")
        
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            Log.d("MainActivity", "Requesting permissions: ${permissionsToRequest.joinToString()}")
            multiplePermissionsLauncher.launch(permissionsToRequest)
        } else {
            Log.d("MainActivity", "All permissions already granted")
        }
    }
}