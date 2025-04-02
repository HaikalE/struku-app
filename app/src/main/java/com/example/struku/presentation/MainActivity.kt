package com.example.struku.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.struku.presentation.auth.AuthManager
import com.example.struku.presentation.auth.AuthState
import com.example.struku.presentation.auth.AuthenticationScreen
import com.example.struku.presentation.receipts.ReceiptsScreen
import com.example.struku.presentation.settings.SettingsScreen
import com.example.struku.presentation.theme.StrukuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var authManager: AuthManager
    
    companion object {
        // Define our own navigation routes
        private const val AUTH_ROUTE = "auth"
        private const val MAIN_APP_ROUTE = "main_app"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
}