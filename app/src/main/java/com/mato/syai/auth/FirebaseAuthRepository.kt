package com.mato.syai.auth


import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.mato.syai.data.remote.repository.UserRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
            private val userRepository: UserRepository
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String
    ): Result<Unit> {

        return try {

            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            Log.d("AUTH", "Firebase Signup Success")

            val token = authResult.user
                ?.getIdToken(false)
                ?.await()
                ?.token

            Log.d("AUTH", "Token: $token")

            // CREATE USER IN NEON DB
            userRepository.createUser()

            Log.d("API", "Backend user created")

            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("AUTH", "Signup Failed", e)

            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {

        return try {

            val credential = GoogleAuthProvider.getCredential(idToken, null)

            val authResult = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val user = authResult.user

            val email = user?.email
            val uid = user?.uid

            Log.d("AUTH", "Google Sign-In Success")
            Log.d("AUTH", "UID: $uid")
            Log.d("AUTH", "EMAIL: $email")

            // 🔥 SYNC TO NEON DB
            userRepository.createUser()

            Log.d("API", "Google user synced to backend")

            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("AUTH", "Google Sign-In Failed", e)

            Result.failure(e)
        }
    }

    override suspend fun sendOtp(
        phoneNumber: String,
        timeoutSeconds: Long,
        activity: android.app.Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): Result<Unit> {
        return try {
            val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithPhoneCredential(
        verificationId: String,
        otp: String
    ): Result<Unit> {

        return try {

            val credential = PhoneAuthProvider.getCredential(verificationId, otp)

            val authResult = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val user = authResult.user

            val uid = user?.uid
            val phone = user?.phoneNumber

            Log.d("AUTH", "Phone Login Success")
            Log.d("AUTH", "UID: $uid")
            Log.d("AUTH", "PHONE: $phone")

            // 🔥 SYNC TO NEON
            userRepository.createUser()

            Log.d("API", "Phone user synced")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("AUTH", "Phone Login Failed", e)
            Result.failure(e)
        }
    }

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null
    override fun getCurrentUserUid(): String? = firebaseAuth.currentUser?.uid

    override fun logout(context: Context): Result<Unit> {
        FirebaseAuth.getInstance().signOut()

        return try {
            // Also sign out Google if used
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
