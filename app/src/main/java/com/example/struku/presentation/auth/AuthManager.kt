package com.example.struku.presentation.auth

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.example.struku.util.BiometricHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for handling app authentication
 */
class AuthManager(private val context: Context) {
    
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
        if (!isBiometricAvailable()) {
            // If biometrics not available, just unlock
            _authState.value = AuthState.UNLOCKED
            return
        }
        
        biometricHelper.showBiometricPrompt(
            activity = activity,
            title = "Autentikasi",
            subtitle = "Harap autentikasi untuk mengakses aplikasi Struku",
            description = "Autentikasi diperlukan untuk melindungi data keuangan pribadi Anda",
            onSuccess = {
                _authState.value = AuthState.UNLOCKED
            },
            onError = { errorCode, errorMessage ->
                if (errorCode == BiometricHelper.ERROR_NEGATIVE_BUTTON || 
                    errorCode == BiometricHelper.ERROR_USER_CANCELED) {
                    // User cancelled - keep locked
                    _authState.value = AuthState.LOCKED
                } else {
                    // System error - unblock for usability
                    _authState.value = AuthState.UNLOCKED
                }
            },
            onFailed = {
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

// Missing constant from BiometricHelper
private val BiometricHelper.ERROR_NEGATIVE_BUTTON: Int get() = BiometricPrompt.ERROR_NEGATIVE_BUTTON
private val BiometricHelper.ERROR_USER_CANCELED: Int get() = BiometricPrompt.ERROR_USER_CANCELED
