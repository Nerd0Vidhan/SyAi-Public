import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CuteAnimatedWaterTank() {
    var glassCount by remember { mutableIntStateOf(0) }
    val animatedLevel by animateFloatAsState(
        targetValue = (glassCount / 8f).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "WaterLevel"
    )

    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val wobble by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Wobble"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Blue, Color.White)
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Decorations
        BackgroundDecorations()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val potShape = GenericShape { size, _ ->
                val width = size.width
                val height = size.height

                moveTo(width * 0.35f, 0f)  // narrower neck
                lineTo(width * 0.65f, 0f)  // top rim
                lineTo(width * 0.88f, height * 0.5f)

                // Right-side curve
                cubicTo(
                    width * 0.95f, height * 0.75f,
                    width * 0.75f, height * 0.95f,
                    width * 0.5f, height
                )

                // Left-side curve
                cubicTo(
                    width * 0.25f, height * 0.95f,
                    width * 0.05f, height * 0.75f,
                    width * 0.12f, height * 0.5f
                )

                close()
            }


            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = wobble.dp)
                    .size(width = 200.dp, height = 400.dp)
                    .clip(potShape)
                    .border(3.dp, Color.Black, potShape)
                    .background(Color(0xFFDDFFFF))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val waveHeight = 20f
                    val waterTop = height * (1 - animatedLevel)
                    val waveLength = width / 1.5f

                    val path = Path().apply {
                        moveTo(0f, waterTop)
                        for (x in 0..width.toInt()) {
                            val angle = (x.toFloat() / waveLength * 2f * PI + waveOffset).toFloat()
                            val y = waterTop + sin(angle) * waveHeight
                            lineTo(x.toFloat(), y)
                        }
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }

                    drawPath(path, color = Color.Cyan.copy(alpha = 0.8f))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedFace(isFull = glassCount == 8, pulse = pulse)
                    Text(
                        text = "${glassCount} / 8 Glasses",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .background(Color.Magenta.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = if (glassCount == 8) "🎉 Great job! You're fully hydrated! 🥳" else "Tap to drink water 💧",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        if (glassCount < 8) glassCount++
                    }
                )
            }
        }
    }
}

@Composable
fun AnimatedFace(isFull: Boolean, pulse: Float) {
    val eyeColor = if (isFull) Color.Red else Color.Black
    val eyeSize = if (isFull) 12.dp else 8.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        FaceEye(eyeColor, eyeSize, pulse)
        Text(
            text = if (isFull) "😍" else "😊",
            fontSize = if (isFull) 36.sp else 28.sp,
            modifier = Modifier.scale(pulse)
        )
        FaceEye(eyeColor, eyeSize, pulse)
    }
}

@Composable
fun FaceEye(color: Color, size: Dp, scale: Float) {
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .background(color, shape = RoundedCornerShape(50))
    )
}

data class Bubble(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)

data class Cloud(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float
)

@Composable
fun BackgroundDecorations() {
    val infiniteTransition = rememberInfiniteTransition()

    // Generate stable bubbles
    val bubbles = remember {
        List(15) {
            Bubble(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 8f + 4f,
                speed = Random.nextFloat() * 0.0015f + 0.0008f,
                alpha = Random.nextFloat() * 0.4f + 0.3f
            )
        }
    }

    // Animate bubble vertical positions
    val animatedBubbleYs = bubbles.mapIndexed { index, bubble ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (5000 / bubble.speed).toInt()),
                repeatMode = RepeatMode.Restart
            ),
            label = "bubbleAnim$index"
        )
    }

    // Generate stable clouds
    val clouds = remember {
        List(5) {
            Cloud(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.2f,
                size = Random.nextFloat() * 60f + 40f,
                speed = Random.nextFloat() * 0.0005f + 0.0003f
            )
        }
    }

    // Animate cloud horizontal positions
    val animatedCloudXs = clouds.mapIndexed { index, cloud ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (10000 / cloud.speed).toInt()),
                repeatMode = RepeatMode.Restart
            ),
            label = "cloudAnim$index"
        )
    }

    // Animate sparkles opacity (twinkling)
    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val widthPx = size.width
        val heightPx = size.height

        // Draw bubbles
        bubbles.forEachIndexed { index, bubble ->
            val animValue = animatedBubbleYs[index].value
            val bubbleX = bubble.x * widthPx
            val bubbleY = ((bubble.y - animValue + 1f) % 1f) * heightPx

            drawCircle(
                color = Color.White.copy(alpha = bubble.alpha),
                radius = bubble.size,
                center = Offset(bubbleX, bubbleY)
            )
        }

        // Draw clouds as soft circles grouped
        clouds.forEachIndexed { index, cloud ->
            val animValue = animatedCloudXs[index].value
            val cloudX = ((cloud.x + animValue) % 1f) * widthPx
            val cloudY = cloud.y * heightPx

            // Simple cloud shape: three overlapping circles
            val radius = cloud.size / 3f

            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = radius,
                center = Offset(cloudX, cloudY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = radius * 0.9f,
                center = Offset(cloudX + radius * 0.9f, cloudY + radius * 0.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.8f,
                center = Offset(cloudX - radius * 0.9f, cloudY + radius * 0.2f)
            )
        }

        // Draw sparkle stars randomly around top area
        val sparklePositions = listOf(
            Offset(widthPx * 0.15f, heightPx * 0.1f),
            Offset(widthPx * 0.35f, heightPx * 0.05f),
            Offset(widthPx * 0.55f, heightPx * 0.12f),
            Offset(widthPx * 0.75f, heightPx * 0.08f),
            Offset(widthPx * 0.85f, heightPx * 0.15f)
        )

        sparklePositions.forEach { pos ->
            drawCircle(
                color = Color.White.copy(alpha = sparkleAlpha),
                radius = 4f,
                center = pos
            )
            // tiny cross lines for sparkle
            drawLine(
                color = Color.White.copy(alpha = sparkleAlpha),
                start = pos.copy(x = pos.x - 6f),
                end = pos.copy(x = pos.x + 6f),
                strokeWidth = 1.2f
            )
            drawLine(
                color = Color.White.copy(alpha = sparkleAlpha),
                start = pos.copy(y = pos.y - 6f),
                end = pos.copy(y = pos.y + 6f),
                strokeWidth = 1.2f
            )
        }
    }
}