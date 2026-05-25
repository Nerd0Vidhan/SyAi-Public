package com.mato.syai.auth

//import UserRepository
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.mato.syai.auth.SessionManager.firebaseToken
import com.mato.syai.data.remote.repository.UserRepository
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

object SessionManager {
    var firebaseToken: String? = null
}

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository
) : AuthRepository {

    // ─────────────────────────────────────────────────────────────
    // EMAIL LOGIN
    // ─────────────────────────────────────────────────────────────
    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<Unit> {

        return try {

            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()

            val token = authResult.user
                ?.getIdToken(false)
                ?.await()
                ?.token

            firebaseToken = token

            Log.d("AUTH", "Email Login Success")
            Log.d("AUTH", "TOKEN: $token")

            userRepository.createUser()

            Log.d("API", "Email user synced to backend")

            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("AUTH", "Email Login Failed", e)

            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // EMAIL SIGNUP
    // ─────────────────────────────────────────────────────────────
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

            firebaseToken = token

            Log.d("AUTH", "TOKEN: $token")

            // Sync to backend
            userRepository.createUser()

            Log.d("API", "Backend user created")

            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("AUTH", "Signup Failed", e)

            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GOOGLE LOGIN
    // ─────────────────────────────────────────────────────────────
    override suspend fun signInWithGoogleIdToken(
        idToken: String
    ): Result<Unit> {
        return try {
            val credential =
                GoogleAuthProvider.getCredential(
                    idToken,
                    null
                )

            val authResult = firebaseAuth.signInWithCredential(credential).await()

            val user = authResult.user
                ?: return Result.failure(
                    Exception("Google user null")
                )

            val token = user.getIdToken(false).await().token

            SessionManager.firebaseToken = token

            // Backend sync
            userRepository.createUser()

            Log.d("AUTH", "Google Login Success")

            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("AUTH", "Google Login Failed", e)

            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SEND OTP
    // ─────────────────────────────────────────────────────────────
    override suspend fun sendOtp(
        phoneNumber: String,
        timeoutSeconds: Long,
        activity: Activity,
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

            Log.e("AUTH", "OTP Send Failed", e)

            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PHONE LOGIN
    // ─────────────────────────────────────────────────────────────
    override suspend fun signInWithPhoneCredential(
        verificationId: String,
        otp: String
    ): Result<Unit> {

        return try {

            val credential =
                PhoneAuthProvider.getCredential(
                    verificationId,
                    otp
                )

            val authResult = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val user = authResult.user
                ?: return Result.failure(
                    Exception("Phone auth user is null")
                )

            val uid = user.uid
            val phone = user.phoneNumber

            Log.d("AUTH", "Phone Login Success")
            Log.d("AUTH", "UID: $uid")
            Log.d("AUTH", "PHONE: $phone")

            val firebaseToken = user
                .getIdToken(false)
                .await()
                .token

            SessionManager.firebaseToken = firebaseToken

            Log.d("AUTH", "TOKEN: $firebaseToken")

            // Sync to backend
            userRepository.createUser(
//                uid = TODO(),
//                email = TODO(),
//                firebaseToken = TODO()
            )

            Log.d("API", "Phone user synced")
9
            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("AUTH", "Phone Login Failed", e)

            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // USER STATE
    // ─────────────────────────────────────────────────────────────
    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }

    // ─────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────
    override fun logout(context: Context): Result<Unit> {

        return try {

            firebaseAuth.signOut()

            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN
                ).build()
            ).signOut()

            // Clear token
            firebaseToken = null

            Log.d("AUTH", "Logout Success")

            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("AUTH", "Logout Failed", e)

            Result.failure(e)
        }
    }
}