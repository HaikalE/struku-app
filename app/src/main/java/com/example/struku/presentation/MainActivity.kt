package com.example.struku.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.struku.presentation.auth.AuthManager
import com.example.struku.presentation.auth.AuthState
import com.example.struku.presentation.auth.AuthenticationScreen
import com.example.struku.presentation.auth.AuthenticatedApp
import com.example.struku.presentation.theme.StrukuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
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
                    // Collect the auth state
                    val authState by authManager.authState.collectAsState()
                    
                    // Show appropriate screen based on auth state
                    when (authState) {
                        AuthState.LOCKED -> {
                            AuthenticationScreen(
                                onAuthenticate = {
                                    authManager.authenticate(this)
                                }
                            )
                        }
                        AuthState.UNLOCKED -> {
                            AuthenticatedApp()
                        }
                    }
                }
            }
        }
    }
}