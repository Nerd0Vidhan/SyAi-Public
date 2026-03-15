package com.mato.syai.core.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class Card(
    val id: Int,
    val title: String,
    val color: Color
)

@Composable
fun SwappableCardsGrid() {
    var cards by remember {
        mutableStateOf(List(12) { index ->
            Card(
                id = index,
                title = "Card ${index + 1}",
                color = when (index % 6) {
                    0 -> Color(0xFF1976D2) // Blue
                    1 -> Color(0xFF9C27B0) // Purple
                    2 -> Color(0xFF388E3C) // Green
                    3 -> Color(0xFFF57C00) // Orange
                    4 -> Color(0xFF00796B) // Teal
                    else -> Color(0xFF7B1FA2) // Deep Purple
                }
            )
        })
    }

    var selectedCardId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Swappable Cards Grid",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cards.size) { index ->
                SwappableCard(
                    card = cards[index],
                    isSelected = selectedCardId == cards[index].id,
                    onClick = {
                        if (selectedCardId == null) {
                            selectedCardId = cards[index].id
                        } else {
                            // Swap cards
                            val firstIndex = cards.indexOfFirst { it.id == selectedCardId }
                            val secondIndex = index
                            cards = cards.toMutableList().apply {
                                val temp = this[firstIndex]
                                this[firstIndex] = this[secondIndex]
                                this[secondIndex] = temp
                            }
                            selectedCardId = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SwappableCard(
    card: Card,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = card.color)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isSelected) "Select another card" else "Tap to swap",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground =
    true)
@Composable
fun SwappableCardsGridPreview() {
    MaterialTheme {
        SwappableCardsGrid()
    }}