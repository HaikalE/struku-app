package com.example.struku.presentation.auth

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.example.struku.util.BiometricHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling app authentication
 */
@Singleton
class AuthManager @Inject constructor(private val context: Context) {
    
    private val biometricHelper = BiometricHelper(context)
    
    private val _authState = MutableStateFlow(AuthState.LOCKED)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAvailable(): Boolean {
        return biometricHelper.canAuthenticate()
    }
    
    /**
     * Authenticate the user with biometrics
     */
    fun authenticate(activity: FragmentActivity) {
        Log.d("AuthManager", "Starting authentication process")
        
        if (!isBiometricAvailable()) {
            // If biometrics not available, just unlock
            Log.d("AuthManager", "Biometrics not available, auto-unlocking")
            _authState.value = AuthState.UNLOCKED
            return
        }
        
        biometricHelper.showBiometricPrompt(
            activity = activity,
            title = "Autentikasi",
            subtitle = "Harap autentikasi untuk mengakses aplikasi Struku",
            description = "Autentikasi diperlukan untuk melindungi data keuangan pribadi Anda",
            onSuccess = {
                Log.d("AuthManager", "Authentication successful, unlocking app")
                _authState.value = AuthState.UNLOCKED
            },
            onError = { errorCode, errorMessage ->
                Log.d("AuthManager", "Authentication error: $errorCode - $errorMessage")
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || 
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    // User cancelled - keep locked
                    _authState.value = AuthState.LOCKED
                } else {
                    // System error - for development purposes, we'll unlock to allow testing
                    // In production, you might want to keep this locked depending on the error
                    _authState.value = AuthState.UNLOCKED
                }
            },
            onFailed = {
                Log.d("AuthManager", "Authentication failed")
                // Keep trying
                _authState.value = AuthState.LOCKED
            }
        )
    }
    
    /**
     * Lock the app again
     */
    fun lock() {
        _authState.value = AuthState.LOCKED
    }
}

enum class AuthState {
    LOCKED,
    UNLOCKED
}