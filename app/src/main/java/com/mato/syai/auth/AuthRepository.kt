package com.mato.syai.auth

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>
    suspend fun signInWithPhoneCredential(verificationId: String, otp: String): Result<Unit>
    suspend fun sendOtp(phoneNumber: String, timeoutSeconds: Long, activity: android.app.Activity, callbacks: com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks): Result<Unit>
    fun isUserLoggedIn(): Boolean
    fun getCurrentUserUid(): String?
}
