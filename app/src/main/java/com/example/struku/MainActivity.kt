package com.example.struku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.struku.presentation.auth.AuthManager
import com.example.struku.presentation.auth.AuthState
import com.example.struku.presentation.auth.AuthenticationScreen
import com.example.struku.presentation.theme.StrukuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            StrukuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Observe authentication state
                    val authState = authManager.authState.collectAsState().value
                    
                    when (authState) {
                        AuthState.LOCKED -> {
                            AuthenticationScreen { authManager.authenticate(this) }
                        }
                        AuthState.UNLOCKED -> {
                            // Temporary placeholder for authenticated content
                            // Will be replaced with actual app content
                            MainContent()
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // If biometric login is enabled in settings, show authentication screen
        val biometricEnabled = /* TODO: get from settings */ true
        if (biometricEnabled) {
            authManager.lock() // Lock on resume to enforce auth
        }
    }
}