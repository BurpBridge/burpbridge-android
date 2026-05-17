package com.kompyler.burpbridge.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val CyberDark = Color(0xFF141414)
private val CyberOrange = Color(0xFFFF6633)
private val CyberText = Color(0xFFE0E0E0)
private val CyberTextDim = Color(0xFF808080)

@Composable
fun SplashScreen(
    onNavigateToDashboard: () -> Unit
) {
    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        delay(500)
        showText = true
        delay(2000)
        onNavigateToDashboard()
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (showText) 0.3f else 0f,
        animationSpec = tween(800),
        label = "glow"
    )

    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val blinkAlpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp)
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val outerR = size.minDimension / 2 - 2f
                    val innerR = outerR * 0.6f

                    drawCircle(
                        color = CyberOrange.copy(alpha = glowAlpha * 0.5f),
                        radius = outerR * 1.3f
                    )
                    drawCircle(
                        color = CyberOrange.copy(alpha = pulse * 0.15f),
                        radius = outerR * 1.1f
                    )
                    drawCircle(
                        color = CyberOrange.copy(alpha = 0.2f),
                        radius = outerR,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = CyberOrange.copy(alpha = 0.4f),
                        radius = innerR,
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = CyberOrange.copy(alpha = 0.3f),
                        radius = innerR * 0.5f
                    )
                    for (i in 0..2) {
                        val angle = Math.toRadians((rotation + i * 120).toDouble())
                        val len = outerR * 0.5f + outerR * 0.2f * (i % 2)
                        drawLine(
                            color = CyberOrange.copy(alpha = 0.25f),
                            start = Offset(
                                cx + len * Math.cos(angle).toFloat(),
                                cy + len * Math.sin(angle).toFloat()
                            ),
                            end = Offset(
                                cx + (len + 12f) * Math.cos(angle).toFloat(),
                                cy + (len + 12f) * Math.sin(angle).toFloat()
                            ),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BurpBridge",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CyberText
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (showText) {
                Text(
                    text = "INITIALIZING SYSTEM",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberTextDim,
                    letterSpacing = 2.sp,
                    modifier = Modifier.graphicsLayer {
                        this.alpha = blinkAlpha
                    }
                )
            }
        }
    }
}
