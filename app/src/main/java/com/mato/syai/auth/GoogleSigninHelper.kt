package com.mato.syai.auth

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

object GoogleSignInHelper {
    private lateinit var googleSignInClient: GoogleSignInClient

    fun init(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(com.mato.syai.R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)
        return googleSignInClient
    }

    fun handleSignInResult(data: Intent?, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(Exception::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { result ->
                    if (result.isSuccessful) onSuccess()
                    else onError(result.exception ?: Exception("Sign-in failed"))
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
}

fun loginWithEmail(email: String, password: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
    FirebaseAuth.getInstance()
        .signInWithEmailAndPassword(email, password)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

fun signUpWithEmail(email: String, password: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
    FirebaseAuth.getInstance()
        .createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError(it) }
}

fun sendOtp(phoneNumber: String, activity: Activity, callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
    val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(callbacks)
        .build()
    PhoneAuthProvider.verifyPhoneNumber(options)
}

