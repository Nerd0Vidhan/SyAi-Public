package com.mato.syai.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.mato.syai.R
import com.mato.syai.profile.presentation.LoginUiState
import com.mato.syai.profile.presentation.LoginViewModel
// Ensure these theme imports exist in your project, or replace with Color.White etc.
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.FadeBrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.White
import com.mato.syai.utils.DeepSpaceWaveBackground
import com.mato.syai.utils.GlassEffect

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

    // Navigation Effect
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Google Sign-In Logic
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { viewModel.handleGoogleSignInIntent(it) }
    }

    fun launchGoogleSignIn(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(activity, gso)
        googleLauncher.launch(googleClient.signInIntent)
    }

    // --- UI Structure ---
    // We wrap the entire screen in our custom background.
    DeepSpaceWaveBackground {

        // 1. The Centering Container
        // This Box takes up the whole screen space over the background
        // and forces its content (the Glass Card) to the center.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding() // Handle status bars here
                .imePadding(),       // Handle keyboard pushing up
            contentAlignment = Alignment.Center
        ) {

            // 2. The Glass Card
            GlassEffect(
                modifier = Modifier
                    .padding(16.dp) // External margin so it doesn't touch screen edges
                    .widthIn(max = 480.dp) // Limits width on tablets/landscape
                    .wrapContentHeight(), // Card shrinks to fit content
                cornerRadius = 24.dp,
                glassTintColor = Color(0x11000000).copy(alpha = 0.55f)
            ) {

                // 3. The Form Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth() // Fill the width of the CARD, not screen
                        .verticalScroll(rememberScrollState()) // Allow scrolling if screen is short
                        .padding(vertical = 30.dp, horizontal = 24.dp), // Internal padding inside glass
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo
                    Image(
                        painter = painterResource(R.drawable.syai),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(100.dp) // Slightly smaller for better fit
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email", color = FadeBrownLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightPurple,
                            unfocusedBorderColor = BrownLight,
                            focusedTextColor = White,
                            unfocusedTextColor = BrownLight,
                            cursorColor = White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password", color = FadeBrownLight) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightPurple,
                            unfocusedBorderColor = BrownLight,
                            focusedTextColor = White,
                            unfocusedTextColor = BrownLight,
                            cursorColor = White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    // Confirm Password (Sign Up Mode)
                    AnimatedVisibility(visible = isSignupMode) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                placeholder = { Text("Confirm Password", color = FadeBrownLight) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LightPurple,
                                    unfocusedBorderColor = BrownLight,
                                    focusedTextColor = White,
                                    unfocusedTextColor = BrownLight,
                                    cursorColor = White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Links Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phone Login",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { navController.navigate("phone_login") },
                            fontStyle = FontStyle.Italic,
                            textDecoration = TextDecoration.Underline
                        )

                        if (!isSignupMode) {
                            Text(
                                text = "Forgot Password?",
                                color = Color.White,
                                modifier = Modifier.clickable {
                                    if (email.isEmpty()) {
                                        Toast.makeText(context, "Enter email first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.sendPasswordReset(email)
                                    }
                                },
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login/Signup Button
                    Button(
                        onClick = {
                            if (!isSignupMode) {
                                viewModel.loginWithEmail(email, password)
                            } else {
                                if (password != confirmPassword) {
                                    Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.signUpWithEmail(email, password)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightPurple
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState is LoginUiState.Loading) {
                            CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isSignupMode) "Sign Up" else "Login",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BrownLight)
                        Text(" OR ", color = BrownLight, fontSize = 14.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BrownLight)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Google Button
                    Button(
                        onClick = { launchGoogleSignIn(context as Activity) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.google),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Sign in with Google", color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Toggle Mode
                    Text(
                        text = if (isSignupMode) "Already have an account? Login" else "Create new account",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { isSignupMode = !isSignupMode }
                    )

                    // Show Error Toast
                    if (uiState is LoginUiState.Error) {
                        LaunchedEffect(uiState) {
                            Toast.makeText(context, (uiState as LoginUiState.Error).message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}