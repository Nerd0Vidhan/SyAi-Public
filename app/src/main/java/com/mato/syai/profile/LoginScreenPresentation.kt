package com.mato.syai.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.R
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.White
import com.mato.syai.ui.theme.PurpleDark

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreen() {
    var number by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize().background(PurpleDark)
            .padding(50.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(R.drawable.syai), contentDescription = "")

        Spacer(modifier = Modifier.height(90.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = (RoundedCornerShape(10.dp))
        ) {
            OutlinedTextField(
                value = number,
                onValueChange = {number = it},
                placeholder = { Text("Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrownLight),
                textStyle = TextStyle(fontSize = 30.sp),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = (RoundedCornerShape(10.dp))
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = {password = it},
                placeholder = { Text("Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrownLight),
                textStyle = TextStyle(fontSize = 30.sp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        IconButton(onClick = {}, modifier = Modifier.size(width=60.dp, height = 50.dp).background(color = LightPurple,  shape = RoundedCornerShape(10.dp))) {
            Image(painterResource(R.drawable.arrow_fwd), contentDescription = "chunnu", modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Forget Password",
            color = BrownLight,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { /* Handle forget password */ }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "________________________________",
            color = BrownLight,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
//                .clickable { /* Handle forget password */ }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(){
            IconButton(onClick = {  }, modifier = Modifier.size(30.dp).background(White, shape = RoundedCornerShape(50.dp))) {
                Icon(
                    painter = painterResource(id = R.drawable.google),
//                    imageVector = FontAwesomeIcons.Google,
                    contentDescription = "Dashboard",
                    tint = Color.Unspecified,
//                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.size(20.dp).background(White, shape = RoundedCornerShape(50.dp))
            )
        }

            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Facebook, // replace with desired Dashboard icon
                    contentDescription = "Dashboard",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Text(
            text = "Don't have account/Sign Up",
            color = BrownLight,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { /* Handle sign up */ }
        )

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .width(200.dp)
                .align(Alignment.CenterHorizontally)
        )

    }

}
