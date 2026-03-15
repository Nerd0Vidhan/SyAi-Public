package com.mato.syai.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.mato.syai.R

@Composable
fun ProfilePage(
    profilePicResId: Int,
    initialUsername: String,
    email: String,
    initialGender: String,
    initialAge: Int,
    initialAddress: String,
    initialPinCode: String,
    initialState: String,
    initialCity: String,
    initialCountry: String,
    onSaveClick: (username: String, gender: String, age: Int, address: String, pinCode: String, state: String, city: String, country: String) -> Unit = { _, _, _, _, _, _, _, _ -> }
) {
    var username by remember { mutableStateOf(initialUsername) }
    var gender by remember { mutableStateOf(initialGender) }
    var ageText by remember { mutableStateOf(initialAge.toString()) }
    var address by remember { mutableStateOf(initialAddress) }
    var pinCode by remember { mutableStateOf(initialPinCode) }
    var state by remember { mutableStateOf(initialState) }
    var city by remember { mutableStateOf(initialCity) }
    var country by remember { mutableStateOf(initialCountry) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Profile Image
            Image(
                painter = painterResource(id = profilePicResId),
                contentDescription = "Profile picture of $username",
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Username TextField
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Email label (non-editable)
            Text(
                text = "Email: $email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Editable info fields in a Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("Gender") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { newValue ->
                            // Only allow digits and max 3 chars.
                            if(newValue.all { it.isDigit() } && newValue.length <= 3) {
                                ageText = newValue
                            }
                        },
                        label = { Text("Age") },
//                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { newValue ->
                            // Allow digits only max length 10 (enough for pins)
                            if(newValue.all { it.isDigit() } && newValue.length <= 10) {
                                pinCode = newValue
                            }
                        },
                        label = { Text("Pin Code") },
//                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val ageInt = ageText.toIntOrNull() ?: 0
                    onSaveClick(username, gender, ageInt, address, pinCode, state, city, country)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePagePreview() {
    MaterialTheme {
        ProfilePage(
            profilePicResId = R.drawable.camera,
            initialUsername = "John Doe",
            email = "john.doe@example.com",
            initialGender = "Male",
            initialAge = 29,
            initialAddress = "1234 Elm Street",
            initialPinCode = "90001",
            initialState = "California",
            initialCity = "Los Angeles",
            initialCountry = "USA",
            onSaveClick = { username, gender, age, address, pinCode, state, city, country ->
                // Handle the save action here for testing
            }
        )
    }
}