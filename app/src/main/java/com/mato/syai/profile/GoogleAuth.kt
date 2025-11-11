package com.mato.syai.profile

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.GoogleAuthProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.mato.syai.R

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
//    private val context = application.applicationContext

    fun firebaseAuthWithGoogle(idToken: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "Google sign-in failed")
                }
            }
    }
}

@Preview(showBackground = true)
@Composable
fun kkhh(){
    val fakeNavController = rememberNavController()
    Login(navController = fakeNavController)
//    Login()
}


@Composable
fun Login(navController: NavHostController, viewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GOOGLE_SIGN_IN", "Launcher callback triggered with result: $result")

        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GOOGLE_SIGN_IN", "Google account received: ${account.email}")

                val idToken = account.idToken
                Log.d("ID_TOKEN", idToken ?: "ID Token is null")

                if (idToken != null) {
                    viewModel.firebaseAuthWithGoogle(
                        idToken,
                        onSuccess = {
                            Log.d("FIREBASE_AUTH", "Firebase authentication successful")
                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                            navController.navigate("home")
                        },
                        onFailure = {
                            Log.e("FIREBASE_AUTH", "Firebase authentication failed: $it")
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Log.e("GOOGLE_SIGN_IN", "ID Token is null")
                    Toast.makeText(context, "Failed to get ID token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("GOOGLE_SIGN_IN", "Google sign-in failed: ${e.statusCode} ${e.message}", e)
                Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("GOOGLE_SIGN_IN", "Sign-in was cancelled or no data returned")
            Toast.makeText(context, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id)) // from google-services.json
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            Log.d("GOOGLE_SIGN_IN", "Launching Google sign-in intent")
            launcher.launch(googleSignInClient.signInIntent)
        },
        colors = ButtonDefaults.buttonColors(containerColor = White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.google),
            contentDescription = "Google Icon",
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Login with Google", color = Color.Black)
    }
}
