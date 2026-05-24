package com.mato.syai.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.mato.syai.R
import com.mato.syai.profile.presentation.LoginUiState
import com.mato.syai.profile.presentation.LoginViewModel
import com.mato.syai.utils.GlassEffect
import com.mato.syai.utils.animatedBackground.DeepSpaceWaveBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- Logic States ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Password Visibility States
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Phone Logic States
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }

    // Mode States
    var isSignupMode by remember { mutableStateOf(false) }
    var isPhoneLoginMode by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    // --- Animation State for Flip ---
    val rotation by animateFloatAsState(
        targetValue = if (isPhoneLoginMode) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "FlipAnimation"
    )

    // Handle UI States / Navigation
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is LoginUiState.PhoneOtpSent -> {
                verificationId = (uiState as LoginUiState.PhoneOtpSent).verificationId
                Toast.makeText(context, "OTP Sent", Toast.LENGTH_SHORT).show()
            }
            is LoginUiState.Error -> {
                Toast.makeText(context, (uiState as LoginUiState.Error).message, Toast.LENGTH_SHORT).show()
            }
            is LoginUiState.PhoneVerificationFailed -> {
                Toast.makeText(context, (uiState as LoginUiState.PhoneVerificationFailed).msg, Toast.LENGTH_SHORT).show()
            }
            else -> {}
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

    DeepSpaceWaveBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            GlassEffect(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 480.dp)
                    .wrapContentHeight()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
                cornerRadius = 24.dp,
                glassTintColor = Color(0x11000000).copy(alpha = 0.55f)
            ) {

                if (rotation <= 90f) {
                    EmailLoginContent(
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        isSignupMode = isSignupMode,
                        isPasswordVisible = isPasswordVisible,
                        onPasswordVisibilityChange = { isPasswordVisible = it },
                        isConfirmPasswordVisible = isConfirmPasswordVisible,
                        onConfirmPasswordVisibilityChange = { isConfirmPasswordVisible = it },
                        uiState = uiState,
                        onLoginClick = { viewModel.loginWithEmail(email, password) },
                        onSignUpClick = { viewModel.signUpWithEmail(email, password) },
                        onGoogleSignInClick = { launchGoogleSignIn(context as Activity) },
                        onSwitchToPhoneMode = { isPhoneLoginMode = true },
                        onForgotPasswordClick = { viewModel.sendPasswordReset(email) },
                        onToggleSignupMode = { isSignupMode = !isSignupMode },
                        context = context
                    )
                } else {
                    Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                        PhoneLoginContent(
                            phone = phone,
                            onPhoneChange = {
                                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                    phone = it
                                }
                            },
                            otp = otp,
                            onOtpChange = { otp = it },
                            verificationId = verificationId,
                            uiState = uiState,
                            onSendOtpClick = {
                                if (phone.length == 10) {
                                    coroutineScope.launch {
                                        viewModel.sendOtp("+91$phone", context as Activity)
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter valid 10 digit number", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onVerifyOtpClick = { vid, code ->
                                viewModel.verifyOtp(vid, code)
                            },
                            onGoogleSignInClick = { launchGoogleSignIn(context as Activity) },
                            onSwitchToEmailMode = { isPhoneLoginMode = false }
                        )
                    }
                }
            }
        }
    }
}

// --- Composable: Front Side (Email) ---
@Composable
fun EmailLoginContent(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    isSignupMode: Boolean,
    isPasswordVisible: Boolean, onPasswordVisibilityChange: (Boolean) -> Unit,
    isConfirmPasswordVisible: Boolean, onConfirmPasswordVisibilityChange: (Boolean) -> Unit,
    uiState: LoginUiState,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSwitchToPhoneMode: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onToggleSignupMode: () -> Unit,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 30.dp, horizontal = 24.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LogoAndHeader()

        Spacer(modifier = Modifier.height(32.dp))

        // Email
        StyledOutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Email",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password with Eye Icon
        StyledOutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Password",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isVisible = isPasswordVisible,
            onVisibilityChange = onPasswordVisibilityChange
        )

        // Confirm Password (Sign Up)
        AnimatedVisibility(visible = isSignupMode) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                StyledOutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    placeholder = "Confirm Password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    isVisible = isConfirmPasswordVisible,
                    onVisibilityChange = onConfirmPasswordVisibilityChange
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Phone Login",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onSwitchToPhoneMode() },
                fontStyle = FontStyle.Italic,
                textDecoration = TextDecoration.Underline
            )

            if (!isSignupMode) {
                Text(
                    text = "Forgot Password?",
                    color = Color.White,
                    modifier = Modifier.clickable {
                        if (email.isEmpty()) Toast.makeText(context, "Enter email first", Toast.LENGTH_SHORT).show()
                        else onForgotPasswordClick()
                    },
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    textDecoration = TextDecoration.Underline
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Button
        ActionButton(
            text = if (isSignupMode) "Sign Up" else "Login",
            isLoading = uiState is LoginUiState.Loading,
            onClick = {
                if (!isSignupMode) onLoginClick()
                else if (password != confirmPassword) Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                else onSignUpClick()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        DividerAndGoogle(onGoogleSignInClick)
        Spacer(modifier = Modifier.height(20.dp))

        // Toggle Text
        Text(
            text = if (isSignupMode) "Already have an account? Login" else "Create new account",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.clickable { onToggleSignupMode() }
        )
    }
}

@Composable
fun LogoAndHeader() {
    Image(
        painter = painterResource(R.drawable.syai),
        contentDescription = "App Logo",
        modifier = Modifier.size(100.dp)
    )
}

@Composable
fun StyledOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    isVisible: Boolean = false,
    onVisibilityChange: ((Boolean) -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.secondary,
            focusedTextColor = MaterialTheme.colorScheme.tertiary,
            unfocusedTextColor = MaterialTheme.colorScheme.secondary,
            cursorColor =  MaterialTheme.colorScheme.tertiary,
            focusedPrefixColor =  MaterialTheme.colorScheme.tertiary,
            unfocusedPrefixColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        prefix = prefix,
        suffix = suffix,
        visualTransformation = if (isPassword && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword && onVisibilityChange != null) {
            {
                IconButton(onClick = { onVisibilityChange(!isVisible) }) {
                    Icon(
                        imageVector = if (isVisible) Icons.Filled.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = "Toggle Password",
                        tint = if (isVisible) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else null
    )
}

@Composable
fun ActionButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
        } else {
            Text(text = text, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun DividerAndGoogle(onGoogleClick: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
            Text(" OR ", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onGoogleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Sign in with Google", color = MaterialTheme.colorScheme.onTertiary)
        }
    }
}

@Composable
fun PhoneLoginContent(
    phone: String, onPhoneChange: (String) -> Unit,
    otp: String, onOtpChange: (String) -> Unit,
    verificationId: String?,
    uiState: LoginUiState,
    onSendOtpClick: () -> Unit,
    onVerifyOtpClick: (String, String) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSwitchToEmailMode: () -> Unit,
    onResendOtpClick: () -> Unit = {},
    onEditNumberClick: () -> Unit = {}
) {
    // Controls the logical state
    var isUiInOtpMode by remember { mutableStateOf(false) }
    // Controls the exit animation lifecycle
    var isAnimatingExit by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Timer Logic
    var ticks by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(isUiInOtpMode) {
        if (isUiInOtpMode) {
            ticks = 30
            canResend = false
            while (ticks > 0) {
                delay(1000)
                ticks--
            }
            canResend = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 30.dp, horizontal = 24.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LogoAndHeader()

        Spacer(modifier = Modifier.height(32.dp))

        // --- CUSTOM PHONE FIELD WITH FLOATING EDIT BADGE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp) // Extra padding for the floating badge
        ) {
            val borderColor = if (isUiInOtpMode || isAnimatingExit) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary
            val borderShape = RoundedCornerShape(12.dp)

            // 1. The TextField (With Transparent Border)
            // We wrap it in a Box to apply the THICK custom border (3.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 2.dp, color = borderColor, shape = borderShape)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) onPhoneChange(it) },
                    placeholder = { Text("Phone Number", color = MaterialTheme.colorScheme.outline) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !isUiInOtpMode,
                    singleLine = true,
                    prefix = { Text("+91 ", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) },
                    // Set default borders to Transparent so our Box border shows
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.tertiary,
                        unfocusedTextColor = MaterialTheme.colorScheme.secondary,
                        disabledTextColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.onPrimary,
                        disabledPrefixColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = borderShape
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isUiInOtpMode || isAnimatingExit,
                enter = expandVertically(
                    expandFrom = Alignment.CenterVertically,
                    animationSpec = tween(300)
                ) + fadeIn(tween(300)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.CenterVertically,
                    animationSpec = tween(200)
                ) + fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (-12).dp, x = (-16).dp) // Position on the border
            ) {
                Surface(
                    onClick = {
                        isUiInOtpMode = false
                        isAnimatingExit = true
                    },
                    shape = RoundedCornerShape(50), // Pill shape
                    // Dark background to "Mask" the border line behind it
                    color = Color(0xFF1E1033),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {

            // Send OTP Button
            androidx.compose.animation.AnimatedVisibility(
                visible = !isUiInOtpMode && !isAnimatingExit,
                exit = fadeOut(animationSpec = tween(200)),
                enter = fadeIn(animationSpec = tween(300, delayMillis = 300))
            ) {
                ActionButton(
                    text = "Send OTP",
                    isLoading = uiState is LoginUiState.Loading,
                    onClick = {
                        if (phone.length == 10) {
                            isUiInOtpMode = true
                            onSendOtpClick()
                        } else {
                            Toast.makeText(context, "Enter 10 digit number", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                // OTP Blocks
                if (isUiInOtpMode || isAnimatingExit) {
                    OtpDroppingRow(
                        otpValue = otp,
                        onOtpChange = onOtpChange,
                        isVisible = isUiInOtpMode, // False triggers exit animation
                        onExitFinished = {
                            isAnimatingExit = false
                            onOtpChange("") // Clear data
                            onEditNumberClick()
                        }
                    )
                }

                // Verify Button & Timer
                androidx.compose.animation.AnimatedVisibility(
                    visible = isUiInOtpMode,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 800)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))

                        ActionButton(
                            text = "Verify OTP",
                            isLoading = uiState is LoginUiState.Loading,
                            onClick = { verificationId?.let { onVerifyOtpClick(it, otp) } }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!canResend) {
                            Text(
                                text = "Resend in ${ticks}s",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Text(
                                text = "Resend OTP",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 14.sp,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                    .clickable {
                                        onResendOtpClick()
                                        ticks = 30
                                        canResend = false
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        DividerAndGoogle(onGoogleSignInClick)
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Login with Email",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.clickable { onSwitchToEmailMode() },
            textDecoration = TextDecoration.Underline
        )
    }
}
@Composable
fun OtpDroppingRow(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    isVisible: Boolean,
    onExitFinished: () -> Unit = {} // New Callback
) {
    val otpLength = 6

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(otpLength) { index ->
                val translationY = remember { Animatable(-150f) }
                val alpha = remember { Animatable(0f) }

                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        // --- ENTER: Drop Down ---
                        delay(index * 100L) // 100ms stagger
                        launch {
                            alpha.animateTo(1f, tween(100))
                        }
                        launch {
                            translationY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    } else {
                        // --- EXIT: Recoil Up ---
                        // Stagger: Right blocks leave first (index 5, 4, 3...)
                        val staggerDelay = (otpLength - 1 - index) * 75L
                        delay(staggerDelay)

                        launch {
                            // Go back up to -150f
                            translationY.animateTo(
                                targetValue = -150f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy, // Keep it bouncy!
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                        launch {
                            delay(100) // Small delay so we see it start moving before fading
                            alpha.animateTo(0f, tween(200))

                            // If this is the LAST block to animate (index 0 because we reverse stagger), trigger callback
                            if (index == 0) {
                                onExitFinished()
                            }
                        }
                    }
                }

                OtpVisualBox(
                    char = if (index < otpValue.length) otpValue[index].toString() else "",
                    isActive = index == otpValue.length && isVisible,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            this.translationY = translationY.value
                            this.alpha = alpha.value
                        }
                )
            }
        }

        // Invisible input layer (only active when visible)
        if (isVisible) {
            BasicTextField(
                value = otpValue,
                onValueChange = {
                    if (it.length <= otpLength && it.all { char -> char.isDigit() }) {
                        onOtpChange(it)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = { },
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0f)
            )
        }
    }
}
// --- NEW COMPOSABLE: Visual-Only Box (No Logic, Just UI) ---
@Composable
fun OtpVisualBox(
    char: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .aspectRatio(0.8f)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            style = TextStyle(
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        // Blinking Cursor
        if (isActive && char.isEmpty()) {
            val infiniteTransition = rememberInfiniteTransition(label = "cursor")
            val cursorAlpha by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ), label = "cursor"
            )
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = cursorAlpha))
            )
        }
    }
}