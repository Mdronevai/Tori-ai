package com.example.feature.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Immersive Full-Screen Live Voice Mode Screen inspired by Dola AI.
 * Displays dynamic fluid undulating audio waveforms, live timer, ambient radiant aura, and ripple mic controls.
 */
@Composable
fun LiveVoiceModeScreen(
    onDismiss: () -> Unit,
    onMicTrigger: () -> Unit,
    lastTranscribedText: String = "",
    isListening: Boolean = true
) {
    var isPaused by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }

    // Live duration timer
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(1000)
            secondsElapsed++
        }
    }

    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

    // Waveform phase animation
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceModeTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Concentric ripple rings around mic
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // Bottom Radiant Glow Bloom
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .align(Alignment.BottomCenter)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ToriElectricPink.copy(alpha = 0.55f),
                        ToriRubyGlow.copy(alpha = 0.45f),
                        ToriNeonViolet.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 0.78f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width / 2f, size.height * 0.78f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("voice_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x331F1F2E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isListening && !isPaused) ToriCyan else Color.Gray)
                        )
                        Text(
                            text = if (isPaused) "PAUSED" else "LIVE SPEECH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onMicTrigger,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .testTag("voice_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Listener",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Upper Fluid Ribbon Waveform Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawFluidAudioWaveform(wavePhase, if (isPaused) 0.1f else 1.0f)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Session Duration Timer
            Text(
                text = timerText,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Speech Subtitle / Transcript Preview
            Text(
                text = when {
                    isPaused -> "Voice session paused. Tap play to resume."
                    lastTranscribedText.isNotBlank() -> "\"$lastTranscribedText\""
                    else -> "Listening in Bangla or English...\nSpeak your command, password query, or task."
                },
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Controls Row: Pause | Radiant Mic Orb with Concentric Rings | End
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Pause / Resume Button
                IconButton(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .testTag("voice_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Central Radiant Pulsing Mic Button with Concentric Rings
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated Concentric Ripple Ring
                    if (isListening && !isPaused) {
                        Canvas(modifier = Modifier.size(110.dp)) {
                            drawCircle(
                                color = ToriElectricPink.copy(alpha = rippleAlpha),
                                radius = (size.minDimension / 2.4f) * rippleScale,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // Main Gradient Mic Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        ToriElectricPink,
                                        ToriMagenta,
                                        ToriNeonViolet
                                    )
                                )
                            )
                            .shadow(18.dp, CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                            .clickable(onClick = onMicTrigger)
                            .testTag("voice_mic_orb_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speak",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // 3. Exit / Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        .testTag("voice_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Mode",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFluidAudioWaveform(phase: Float, intensity: Float) {
    val width = size.width
    val height = size.height
    val centerY = height / 2f

    val colors = listOf(
        Color(0xFFE879F9).copy(alpha = 0.9f),
        Color(0xFFA855F7).copy(alpha = 0.8f),
        Color(0xFF38BDF8).copy(alpha = 0.7f),
        Color(0xFFFFFFFF).copy(alpha = 0.85f)
    )

    for (c in colors.indices) {
        val path = Path()
        val strandOffset = c * 0.85f
        val points = 80
        var first = true

        for (i in 0..points) {
            val progress = i.toFloat() / points
            val x = progress * width
            val envelope = sin(progress * Math.PI).toFloat() // zero at ends, peaks in middle

            val freq1 = 3.5f
            val freq2 = 2.0f
            val wave = sin((progress * freq1 * 2 * Math.PI + phase + strandOffset).toDouble()).toFloat() * (26.dp.toPx() * intensity)
            val wave2 = sin((progress * freq2 * 2 * Math.PI - phase * 0.8f + strandOffset).toDouble()).toFloat() * (14.dp.toPx() * intensity)

            val y = centerY + (wave + wave2) * envelope

            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = colors[c],
            style = Stroke(width = if (c == 3) 2.dp.toPx() else 1.5.dp.toPx())
        )
    }
}
