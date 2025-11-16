// profile/presentation/LoginViewModel.kt
package com.mato.syai.profile.presentation

import android.app.Activity
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

    // Called by UI when Google intent result returns (pass the raw Intent)
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

    suspend fun sendOtp(phone: String, activity: Activity) {
        _uiState.value = LoginUiState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto verification 💥
                viewModelScope.launch {
                    try {
                        FirebaseAuth.getInstance().signInWithCredential(credential).await()
                        _uiState.value = LoginUiState.Success(
                            FirebaseAuth.getInstance().currentUser?.uid
                        )
                    } catch (e: Exception) {
                        _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Auto verification failed")
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = LoginUiState.PhoneVerificationFailed(e.localizedMessage ?: "Verification failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _uiState.value = LoginUiState.PhoneOtpSent(verificationId)
            }
        }

        authRepository.sendOtp(phone,120, activity, callbacks)
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



    // For phone OTP you will create callbacks in UI and call repository.sendOtp(...)
    fun isLoggedIn() = authRepository.isUserLoggedIn()
}
