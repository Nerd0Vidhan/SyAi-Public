package com.mato.syai.core.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.core.themes.LightPurple
import com.mato.syai.core.themes.White

@Composable
fun MainButton(
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LightPurple,
            contentColor = White
        )
    ) {
        Text(
            text = "Click Me",
            style = TextStyle(fontSize = 28.sp)
        )
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun MainButtonPreview() {
    val context = LocalContext.current
    MainButton{
        MainToast(context, "Hehe")
    }
}
