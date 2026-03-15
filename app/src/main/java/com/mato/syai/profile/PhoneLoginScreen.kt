//package com.mato.syai.profile
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.animateContentSize
//import androidx.compose.animation.core.FastOutSlowInEasing
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material.icons.filled.Visibility
//import androidx.compose.material.icons.filled.VisibilityOff
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.ExperimentalComposeUiApi
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.FocusRequester
//import androidx.compose.ui.focus.focusRequester
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.input.key.Key
//import androidx.compose.ui.input.key.key
//import androidx.compose.ui.input.key.onKeyEvent
//import androidx.compose.ui.platform.LocalSoftwareKeyboardController
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.text.input.VisualTransformation
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextDecoration
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.mato.syai.profile.presentation.LoginUiState
//import com.mato.syai.ui.theme.BrownLight
//import com.mato.syai.ui.theme.FadeBrownLight
//import com.mato.syai.ui.theme.LightPurple
//import com.mato.syai.ui.theme.White
//
//// ... [Keep your main LoginScreen, EmailLoginContent, and imports unchanged] ...
//
//// --- REPLACED: Phone Login Content with Dropping Animation ---
//@Composable
//fun PhoneLoginContent(
//    phone: String, onPhoneChange: (String) -> Unit,
//    otp: String, onOtpChange: (String) -> Unit,
//    verificationId: String?,
//    uiState: LoginUiState,
//    onSendOtpClick: () -> Unit,
//    onVerifyOtpClick: (String, String) -> Unit,
//    onGoogleSignInClick: () -> Unit,
//    onSwitchToEmailMode: () -> Unit
//) {
//    // Local state to control the "Animation" phase, separate from logic state
//    var isOtpVisible by remember { mutableStateOf(false) }
//
//    // Sync local animation state with logic state
//    LaunchedEffect(verificationId) {
//        isOtpVisible = verificationId != null
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .verticalScroll(rememberScrollState())
//            .padding(vertical = 30.dp, horizontal = 24.dp)
//            .animateContentSize(),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        LogoAndHeader()
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // --- PHONE NUMBER FIELD ---
//        // This stays visible but changes appearance when OTP is sent
//        StyledOutlinedTextField(
//            value = phone,
//            onValueChange = onPhoneChange,
//            placeholder = "Phone Number",
//            keyboardType = KeyboardType.Phone,
//            // When OTP is visible, we disable the field
//            enabled = !isOtpVisible,
//            prefix = {
//                Text(
//                    "+91 ",
//                    // Requested: Light Brown color
//                    color = BrownLight,
//                    fontWeight = FontWeight.Bold
//                )
//            },
//            // Requested: Remove counter (suffix is null)
//            suffix = null,
//            // Custom colors for the "Disabled/Fade Brown" look
//            colors = OutlinedTextFieldDefaults.colors(
//                focusedBorderColor = LightPurple,
//                unfocusedBorderColor = BrownLight,
//                focusedTextColor = White,
//                unfocusedTextColor = BrownLight,
//                cursorColor = White,
//                // Disabled state colors
//                disabledBorderColor = FadeBrownLight.copy(alpha = 0.5f),
//                disabledTextColor = FadeBrownLight,
//                disabledPrefixColor = FadeBrownLight
//            )
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        // --- BUTTON / OTP AREA ---
//
//        // We use a Box to overlap the "Send OTP" button and the dropping blocks
//        // if you wanted them to occupy similar space, but here we stack them vertically
//        // based on the flow.
//
//        if (!isOtpVisible) {
//            // Initial State: Show Send Button
//            ActionButton(
//                text = "Send OTP",
//                isLoading = uiState is LoginUiState.Loading,
//                onClick = onSendOtpClick
//            )
//        } else {
//            // OTP SENT STATE
//
//            // 1. The 6 Dropping Blocks
//            OtpDroppingRow(
//                otpValue = otp,
//                onOtpChange = onOtpChange,
//                isVisible = isOtpVisible
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // 2. Edit Number Button (Animated Fade In)
//            Box(modifier = Modifier.fillMaxWidth()) {
//                AnimatedVisibility(
//                    visible = isOtpVisible,
//                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 1500)), // Appears after blocks drop
//                    exit = fadeOut(animationSpec = tween(300)),
//                    modifier = Modifier.align(Alignment.CenterEnd)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .clickable {
//                                // Logic to "Reset"
//                                // We clear verificationId in ViewModel in a real app,
//                                // here we just simulate UI reset via callback if needed,
//                                // or assume parent handles it.
//                                // For visual purposes, we treat "Edit" as cancelling current flow.
//                                // NOTE: In a real app, you might want to keep the ID but allow re-entry.
//                                // For this prompt, we assume we just go back to phone entry.
//                                onOtpChange("") // Clear OTP
//                                // We rely on the parent/ViewModel to clear verificationId to trigger the reverse animation.
//                                // Since we don't have direct access to set verificationId = null here,
//                                // we assume the ViewModel has a 'reset' function or we handle it via a new callback.
//                                // For now, I'll invoke a callback that sets local visibility off immediately for visual feedback
//                                // assuming the parent logic follows suit.
//                                // *Critical*: You need to implement a "clear session" in VM or pass a callback to nullify verificationId.
//                                // I will assume calling onPhoneChange with existing phone triggers a reset or add a specific callback.
//                                // Actually, let's just use a specific callback logic:
//                                // Resetting UI state:
//                                isOtpVisible = false
//                                // NOTE: You should also update the actual ViewModel state here!
//                            },
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "Edit Number",
//                            color = BrownLight,
//                            fontSize = 12.sp,
//                            textDecoration = TextDecoration.Underline
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Icon(
//                            imageVector = Icons.Default.Edit,
//                            contentDescription = "Edit",
//                            tint = BrownLight,
//                            modifier = Modifier.size(14.dp)
//                        )
//                    }
//                }
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // 3. Verify Button
//            ActionButton(
//                text = "Verify OTP",
//                isLoading = uiState is LoginUiState.Loading,
//                onClick = { verificationId?.let { onVerifyOtpClick(it, otp) } }
//            )
//        }
//
//        Spacer(modifier = Modifier.height(20.dp))
//        DividerAndGoogle(onGoogleSignInClick)
//        Spacer(modifier = Modifier.height(20.dp))
//
//        Text(
//            text = "Login with Email",
//            color = Color.White,
//            fontSize = 15.sp,
//            modifier = Modifier.clickable { onSwitchToEmailMode() },
//            textDecoration = TextDecoration.Underline
//        )
//    }
//}
//
//// --- NEW COMPOSABLE: The Staggered Dropping OTP Row ---
//@Composable
//fun OtpDroppingRow(
//    otpValue: String,
//    onOtpChange: (String) -> Unit,
//    isVisible: Boolean
//) {
//    val otpLength = 6
//    val focusRequesters = remember { List(otpLength) { FocusRequester() } }
//    val keyboardController = LocalSoftwareKeyboardController.current
//
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        repeat(otpLength) { index ->
//            // --- ANIMATION LOGIC ---
//            // "When one block drops then only the other will appear"
//            // We stagger the delay.
//            // Total time ~1.5s. 1500ms / 6 = 250ms per block delay.
//
//            val delay = if (isVisible) index * 200 else 0 // No delay on exit
//
//            val translationY by animateFloatAsState(
//                targetValue = if (isVisible) 0f else -50f, // Start from -50 (up near phone field)
//                animationSpec = tween(
//                    durationMillis = 400, // Speed of the individual drop
//                    delayMillis = delay,
//                    easing = FastOutSlowInEasing
//                ),
//                label = "DropTransY"
//            )
//
//            val alpha by animateFloatAsState(
//                targetValue = if (isVisible) 1f else 0f,
//                animationSpec = tween(
//                    durationMillis = 400,
//                    delayMillis = delay
//                ),
//                label = "DropAlpha"
//            )
//
//            // Render the Individual Box
//            OtpDigitInput(
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(horizontal = 4.dp)
//                    .graphicsLayer {
//                        this.translationY = translationY
//                        this.alpha = alpha
//                    },
//                value = if (index < otpValue.length) otpValue[index].toString() else "",
//                onValueChange = { newValue ->
//                    // Logic to construct new OTP string
//                    if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
//                        val sb = StringBuilder(otpValue)
//                        if (index < sb.length) {
//                            if (newValue.isNotEmpty()) sb.setCharAt(index, newValue[0])
//                        } else {
//                            if (newValue.isNotEmpty()) sb.append(newValue)
//                        }
//
//                        // Limit to 6 chars
//                        val finalOtp = sb.toString().take(6)
//                        onOtpChange(finalOtp)
//
//                        // Move Focus Forward
//                        if (newValue.isNotEmpty() && index < otpLength - 1) {
//                            focusRequesters[index + 1].requestFocus()
//                        } else if (index == otpLength - 1 && newValue.isNotEmpty()) {
//                            keyboardController?.hide()
//                        }
//                    }
//                },
//                focusRequester = focusRequesters[index],
//                onBackspace = {
//                    // Logic to Move Focus Backward and delete
//                    if (otpValue.isNotEmpty() && index > 0) {
//                        // Remove last char if we are at the end, or specific char?
//                        // For simple OTP flow, usually backspace at empty index moves back
//                        focusRequesters[index - 1].requestFocus()
//                    }
//                }
//            )
//        }
//    }
//}
//
//// --- NEW COMPOSABLE: Individual OTP Digit Box ---
//@OptIn(ExperimentalComposeUiApi::class)
//@Composable
//fun OtpDigitInput(
//    modifier: Modifier = Modifier,
//    value: String,
//    onValueChange: (String) -> Unit,
//    focusRequester: FocusRequester,
//    onBackspace: () -> Unit
//) {
//    OutlinedTextField(
//        value = value,
//        onValueChange = {
//            if (it.length <= 1) onValueChange(it)
//            // If length is 2 (user typed fast), take the last char
//            else if (it.length == 2) onValueChange(it.last().toString())
//        },
//        modifier = modifier
//            .aspectRatio(0.8f) // Vertical Rectangle shape
//            .focusRequester(focusRequester)
//            .onKeyEvent { keyEvent ->
//                // Handle Backspace to move focus back
//                if (keyEvent.key == Key.Backspace && value.isEmpty()) {
//                    onBackspace()
//                    true
//                } else {
//                    false
//                }
//            },
//        textStyle = TextStyle(
//            color = White,
//            fontSize = 18.sp,
//            textAlign = TextAlign.Center,
//            fontWeight = FontWeight.Bold
//        ),
//        colors = OutlinedTextFieldDefaults.colors(
//            focusedBorderColor = LightPurple,
//            unfocusedBorderColor = BrownLight,
//            cursorColor = Color.Transparent // Hide cursor for cleaner look
//        ),
//        shape = RoundedCornerShape(8.dp),
//        singleLine = true,
//        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//    )
//}
//
//// --- Modified StyledOutlinedTextField to support enabled state & custom colors ---
//// Ensure you update your existing helper or replace it with this one
//@Composable
//fun StyledOutlinedTextField(
//    value: String,
//    onValueChange: (String) -> Unit,
//    placeholder: String,
//    keyboardType: KeyboardType,
//    enabled: Boolean = true, // Added enabled param
//    isPassword: Boolean = false,
//    isVisible: Boolean = false,
//    onVisibilityChange: ((Boolean) -> Unit)? = null,
//    prefix: @Composable (() -> Unit)? = null,
//    suffix: @Composable (() -> Unit)? = null,
//    colors: TextFieldColors? = null // Added colors param
//) {
//    val defaultColors = OutlinedTextFieldDefaults.colors(
//        focusedBorderColor = LightPurple,
//        unfocusedBorderColor = BrownLight,
//        focusedTextColor = White,
//        unfocusedTextColor = BrownLight,
//        cursorColor = White,
//        focusedPrefixColor = White,
//        unfocusedPrefixColor = BrownLight
//    )
//
//    OutlinedTextField(
//        value = value,
//        onValueChange = onValueChange,
//        enabled = enabled,
//        placeholder = { Text(placeholder, color = FadeBrownLight) },
//        colors = colors ?: defaultColors,
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(10.dp),
//        singleLine = true,
//        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
//        prefix = prefix,
//        suffix = suffix,
//        visualTransformation = if (isPassword && !isVisible) PasswordVisualTransformation() else VisualTransformation.None,
//        trailingIcon = if (isPassword && onVisibilityChange != null) {
//            {
//                IconButton(onClick = { onVisibilityChange(!isVisible) }) {
//                    Icon(
//                        imageVector = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
//                        contentDescription = "Toggle Password",
//                        tint = BrownLight
//                    )
//                }
//            }
//        } else null
//    )
//}