package com.mato.syai.profile

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.mato.syai.R
import com.mato.syai.profile.presentation.LoginState
import com.mato.syai.profile.presentation.LoginViewModel
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.PurpleDark


@Composable
fun LoginScreen(navController: NavHostController, viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginState by viewModel.loginState.collectAsState()

    // Auto navigate if already logged in
    LaunchedEffect(Unit) {
        if (viewModel.isLoggedIn()) navController.navigate("home") { popUpTo("login") { inclusive = true } }
    }

    // Handle Google Sign-In
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result
            val idToken = account.idToken!!
            viewModel.loginWithEmail(account.email ?: "", "")
            navController.navigate("home")
        } catch (e: Exception) {
            Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail() // Request email address
        .requestIdToken(context.getString(R.string.default_web_client_id)) // If you need an ID token for backend authentication
        .build()

    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
    val authResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Handle successful sign-in (e.g., get ID token, user details)
                Log.d("GoogleSignIn", "Signed in as: ${account.displayName}")
            } catch (e: ApiException) {
                // Handle sign-in failure
                Log.e("GoogleSignIn", "Sign-in failed: ${e.statusCode}")
            }
        } else {
            // Handle cases where the sign-in flow was cancelled or failed
            Log.d("GoogleSignIn", "Sign-in cancelled or failed.")
        }
    }

    fun launchGoogleSignIn(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        Log.d("currentUser", "launchGoogleSignIn: ${FirebaseAuth.getInstance().currentUser}")
        launcher.launch(googleSignInClient.signInIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleDark)
            .padding(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(R.drawable.syai), contentDescription = "")

        Spacer(modifier = Modifier.height(60.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("E-mail", color = BrownLight) },
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BrownLight, RoundedCornerShape(10.dp)),
            textStyle = TextStyle(fontSize = 20.sp, color = BrownLight),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password", color = BrownLight) },
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BrownLight, RoundedCornerShape(10.dp)),
            textStyle = TextStyle(fontSize = 20.sp, color = BrownLight),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Text(
            text = "Forgot Password?",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    Toast.makeText(context, "Reset password link sent!", Toast.LENGTH_SHORT).show()
                }
                .padding(top = 8.dp, end = 4.dp),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    viewModel.loginWithEmail(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login", color = Color.White)
        }

        Spacer(modifier = Modifier.height(25.dp))

        Text("__________________________", color = BrownLight, fontSize = 20.sp)

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {
                val signInIntent = googleSignInClient!!.signInIntent
                authResultLauncher.launch(signInIntent)
//                launchGoogleSignIn(context as Activity)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Sign in with Google", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(30.dp))

        when (loginState) {
            is LoginState.Loading -> CircularProgressIndicator()
            is LoginState.Success -> {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is LoginState.Error -> Toast.makeText(context, "Error: ${(loginState as LoginState.Error).message}", Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }
}
