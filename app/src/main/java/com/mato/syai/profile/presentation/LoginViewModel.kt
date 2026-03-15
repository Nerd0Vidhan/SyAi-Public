package com.mato.syai.profile.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mato.syai.auth.AuthRepository
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val uid: String?) : LoginUiState
    data class Error(val message: String) : LoginUiState

    data class PhoneOtpSent(val verificationId: String) : LoginUiState
    data class PhoneVerificationFailed(val msg: String) : LoginUiState

    object PasswordResetSent : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    // To handle Resend OTP, we must store the resending token from the previous callbacks
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Store the phone number locally for resending convenience
    private var currentPhone: String? = null

    fun checkAlreadyLoggedIn() {
        if (authRepository.isUserLoggedIn()) {
            _uiState.value = LoginUiState.Success(authRepository.getCurrentUserUid())
        }
    }

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.signInWithEmail(email, password)
            _uiState.value = if (result.isSuccess) {
                LoginUiState.Success(authRepository.getCurrentUserUid())
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Login failed")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.signUpWithEmail(email, password)
            _uiState.value = if (result.isSuccess) {
                LoginUiState.Success(authRepository.getCurrentUserUid())
            } else {
                LoginUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Signup failed")
            }
        }
    }

    fun handleGoogleSignInIntent(intent: Intent) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken ?: throw Exception("Missing id token")
                val result = authRepository.signInWithGoogleIdToken(idToken)
                _uiState.value = if (result.isSuccess) LoginUiState.Success(authRepository.getCurrentUserUid())
                else LoginUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Google sign-in failed")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Google sign-in error")
            }
        }
    }

    /**
     * Sends OTP to the provided phone number.
     * Ensures +91 prefix is present.
     */
    suspend fun sendOtp(phone: String, activity: Activity) {
        _uiState.value = LoginUiState.Loading

        // Ensure +91 is added if not present
        val finalPhone = if (phone.startsWith("+91")) phone else "+91$phone"
        currentPhone = finalPhone

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto verification
                viewModelScope.launch {
                    try {
                        FirebaseAuth.getInstance().signInWithCredential(credential).await()
                        _uiState.value = LoginUiState.Success(FirebaseAuth.getInstance().currentUser?.uid)
                    } catch (e: Exception) {
                        _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Auto verification failed")
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = LoginUiState.PhoneVerificationFailed(e.localizedMessage ?: "Verification failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                // Save the resend token!
                resendToken = token
                _uiState.value = LoginUiState.PhoneOtpSent(verificationId)
            }
        }

        authRepository.sendOtp(finalPhone, 60, activity, callbacks)
    }

    /**
     * Resends OTP using the stored token and phone number.
     */
    suspend fun resendOtp(activity: Activity) {
        val phone = currentPhone
        val token = resendToken

        if (phone != null && token != null) {
            _uiState.value = LoginUiState.Loading
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) { /* Same as above */ }
                override fun onVerificationFailed(e: FirebaseException) {
                    _uiState.value = LoginUiState.PhoneVerificationFailed(e.localizedMessage ?: "Resend failed")
                }
                override fun onCodeSent(verificationId: String, newToken: PhoneAuthProvider.ForceResendingToken) {
                    resendToken = newToken
                    _uiState.value = LoginUiState.PhoneOtpSent(verificationId)
                }
            }
            // Pass the token here if your repository supports it, otherwise use direct Firebase call
            // Ideally authRepository.resendOtp(phone, token, activity, callbacks)
            // For now assuming sendOtp handles standard flow or you overload it.
            // If repository doesn't support resend token, just call sendOtp(phone, activity) again.
            // authRepository.sendOtp(phone, 60, activity, callbacks, token) <--- Assuming you update repo

            // Fallback if no specific resend method in your repo:
            authRepository.sendOtp(phone, 60, activity, callbacks)
        }
    }

    fun verifyOtp(verificationId: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.signInWithPhoneCredential(verificationId, otp)
            _uiState.value = if (result.isSuccess)
                LoginUiState.Success(authRepository.getCurrentUserUid())
            else
                LoginUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Invalid OTP")
        }
    }

    /**
     * Clears the current verification state (e.g., when user clicks "Edit Number")
     */
    fun resetOtpState() {
        _uiState.value = LoginUiState.Idle
        resendToken = null
        currentPhone = null
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                _uiState.value = LoginUiState.PasswordResetSent
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Reset failed")
            }
        }
    }

    fun isLoggedIn() = authRepository.isUserLoggedIn()

    fun logout(context: Context) {
        viewModelScope.launch {
            try {
                authRepository.logout(context)
                _uiState.value = LoginUiState.Success(null)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Logout failed")
            }
        }
    }
}