package com.mato.syai.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mato.syai.R
import com.mato.syai.profile.presentation.LoginUiState
import com.mato.syai.profile.presentation.LoginViewModel
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.PurpleDark


@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSignupMode by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    // On successful login → navigate
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Google Sign-in launcher
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let {
            viewModel.handleGoogleSignInIntent(it)
        }
    }

    // Google Sign-in intent builder
    fun launchGoogleSignIn(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(activity, gso)
        googleLauncher.launch(googleClient.signInIntent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurpleDark)
            .padding(50.dp)
            .focusable(true),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // App logo
        Image(
            painter = painterResource(R.drawable.syai),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email", color = BrownLight) },
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BrownLight, RoundedCornerShape(10.dp)),
            textStyle = TextStyle(fontSize = 20.sp, color = BrownLight),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Password
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

        // Confirm password only if in Sign Up mode
        if (isSignupMode) {
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("Confirm Password", color = BrownLight) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, BrownLight, RoundedCornerShape(10.dp)),
                textStyle = TextStyle(fontSize = 20.sp, color = BrownLight),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!isSignupMode) {
            Text(
                text = "Forgot Password?",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable {
                        if (email.isEmpty()) {
                            Toast.makeText(context, "Enter email first", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.sendPasswordReset(email)
                        }
                    }
                    .padding(top = 8.dp, end = 4.dp),
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Login with Phone Number",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable {
                    navController.navigate("phone_login")
                }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Submit button
        Button(
            onClick = {
                if (!isSignupMode) {
                    viewModel.loginWithEmail(email, password)
                } else {
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.signUpWithEmail(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (isSignupMode) "Sign Up" else "Login", color = Color.White)
        }

        Spacer(modifier = Modifier.height(25.dp))

        Text("__________________________", color = BrownLight, fontSize = 20.sp)

        Spacer(modifier = Modifier.height(25.dp))

        // Google Sign In Button
        Button(
            onClick = { launchGoogleSignIn(context as Activity) },
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

        Spacer(modifier = Modifier.height(20.dp))

        // Switch Login / Signup mode
        Text(
            text = if (isSignupMode) "Already have an account? Login"
            else "Create new account",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.clickable { isSignupMode = !isSignupMode }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // State handling
        when (uiState) {
            is LoginUiState.Loading -> CircularProgressIndicator()

            is LoginUiState.Error ->
                Toast.makeText(
                    context,
                    (uiState as LoginUiState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()

            else -> {}
        }
    }
}
