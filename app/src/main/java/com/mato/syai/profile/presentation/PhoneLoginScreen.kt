package com.mato.syai.profile.presentation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mato.syai.ui.theme.PurpleDark
import com.mato.syai.profile.presentation.LoginUiState
import com.mato.syai.profile.presentation.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun PhoneLoginScreen(navController: NavHostController, viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    // handle uiState
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.PhoneOtpSent -> {
                verificationId = (uiState as LoginUiState.PhoneOtpSent).verificationId
            }
            is LoginUiState.Success -> {
                navController.navigate("home") {
                    popUpTo("phone_login") { inclusive = true }
                }
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

    Column(
        Modifier
            .fillMaxSize()
            .background(PurpleDark)
            .padding(40.dp),
        verticalArrangement = Arrangement.Center
    ) {

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            placeholder = { Text("Phone Number (+91...)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = {
            coroutineScope.launch {
                viewModel.sendOtp(phone, context as Activity)
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Send OTP")
        }

        verificationId?.let {
            Spacer(Modifier.height(30.dp))

            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                placeholder = { Text("Enter OTP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(onClick = {
                viewModel.verifyOtp(it, otp)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Verify OTP")
            }
        }

        if (uiState is LoginUiState.Loading)
            CircularProgressIndicator()
    }
}
