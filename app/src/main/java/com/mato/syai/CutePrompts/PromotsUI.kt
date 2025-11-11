package com.mato.syai.CutePrompts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.R
//import com.mato.syai.ui.theme.Golden
import kotlinx.coroutines.launch

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun PromptUI() {
    val cardCount = 9
    val clickedCardIndex = remember { mutableStateOf<Int?>(null) }
    val showScratch = remember { mutableStateOf(false) }

    val cuteFlirtPrompts = listOf(
        "If I had a star for every time you crossed my mind, I'd have a galaxy 🌌",
        "Are you made of copper and tellurium? Because you're Cu-Te 😉",
        "Quick! Tell me something cute... or just say hi 😚",
        "You're my favorite notification 💬💖",
        "Do you believe in love at first chat? 💌",
        "You must be tired... you’ve been running through my mind all day 🏃‍♂️💭",
        "Can I keep you in my pocket like a lucky charm? 🍀",
        "If kisses were snowflakes, I’d send you a blizzard ❄️😘",
        "You + Me = 💯 Cute Combo",
        "Let’s skip to the part where we get ice cream together 🍦💕",
        "Just checking... do you always smile this much when we talk? 😊",
        "You’re the kind of person who makes hearts skip a beat 💓",
        "If I could rearrange the alphabet, I'd put U and I together 🔤💘",
        "Did it hurt when you fell from the top of my chat list? 😂",
        "What’s your favorite pizza topping? Asking for our future date 🍕😉",
        "You talking to me is my favorite notification sound 🔔🥰",
        "You’re like a dictionary — you add meaning to my life",
        "Can I borrow a smile? I seem to have lost mine thinking of you",
        "You're the WiFi to my heart — always connecting",
        "Are you a campfire? Because you're hot and I want s'more 🔥🍫"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Text("Let's see what SyAi wants to tell you!",
            color = Color.Black,
            style = TextStyle(fontSize = 30.sp, fontStyle = FontStyle.Italic),
            modifier = Modifier.padding(16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize()
        ) {
            items(cardCount) { index ->
                if (clickedCardIndex.value != index) {
                    FlipCard(
                        frontImage = R.drawable.card_default,
                        modifier = Modifier
                            .height(250.dp)
                            .padding(vertical = 10.dp),
                        onClick = {
                            clickedCardIndex.value = index
                        }
                    )
                }
            }
        }

        // Show the animated card at center if clicked
        clickedCardIndex.value?.let { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)), // optional dim background
                contentAlignment = Alignment.Center
            ) {
                CenterFlippingCard(
                    frontImage = R.drawable.card_default,
                    onFlipComplete = {
                        showScratch.value = true
                    }
                )
            }
        }

        if (showScratch.value) {
            ScratchCard(
                message = cuteFlirtPrompts.random(),
                onClose = {
                    clickedCardIndex.value = null
                    showScratch.value = false
                }
            )
        }
    }
}

@Composable
fun FlipCard(
    frontImage: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = frontImage),
            contentDescription = "Card",
            modifier = Modifier.size(180.dp)
        )
    }
}

@Composable
fun CenterFlippingCard(
    frontImage: Int,
    onFlipComplete: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            offsetY.animateTo(-100f, tween(300))
        }
        scope.launch {
            scale.animateTo(1.5f, tween(300))
        }
        scope.launch {
            rotation.animateTo(1080f, tween(800, easing = LinearEasing))
            onFlipComplete()
        }
    }

    Image(
        painter = painterResource(id = frontImage),
        contentDescription = "Center Card",
        modifier = Modifier
            .graphicsLayer {
                rotationY = rotation.value % 360f
                scaleX = scale.value
                scaleY = scale.value
                translationY = offsetY.value
                cameraDistance = 12 * density
            }
            .size(180.dp)
    )
}

@Composable
fun ScratchCard(message: String, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClose, modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.card_text),
                contentDescription = "Close"
            )
        }
            Text(
                text = message,
                color = Color.Yellow,
                style = TextStyle(fontSize = 30.sp),
                modifier = Modifier.padding(80.dp)
            )
    }
}
