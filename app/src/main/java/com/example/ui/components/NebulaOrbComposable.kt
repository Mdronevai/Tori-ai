package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ToriAmberGlow
import com.example.ui.theme.ToriCyan
import com.example.ui.theme.ToriElectricPink
import com.example.ui.theme.ToriNeonViolet
import com.example.ui.theme.ToriRubyGlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Iridescent Holographic Nebula Orb inspired by Dola AI.
 * Renders intertwined dynamic fluid ribbon waves, ambient radial bloom, and breathing pulse.
 */
@Composable
fun NebulaOrbComposable(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    isActive: Boolean = true,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NebulaOrbTransition")

    // Breathing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    // Wave phase progression
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbPhase"
    )

    // Rotation angle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val radius = (size.toPx() / 2.3f) * scale

            // 1. Radiant Ambient Glow Bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ToriElectricPink.copy(alpha = 0.35f),
                        ToriNeonViolet.copy(alpha = 0.25f),
                        ToriAmberGlow.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.55f
                ),
                radius = radius * 1.55f,
                center = center
            )

            // 2. Inner Nebula Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1F1135),
                        Color(0xFF100926),
                        Color(0xFF070414),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.95f
                ),
                radius = radius * 0.95f,
                center = center
            )

            // 3. Draw Multi-Layer Intertwined Holographic Fluid Ribbons
            drawFluidRibbonStrands(center, radius, phase, rotation)

            // 4. Highlight Shimmer Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ToriElectricPink.copy(alpha = 0.7f),
                        ToriAmberGlow.copy(alpha = 0.8f),
                        ToriNeonViolet.copy(alpha = 0.9f),
                        ToriCyan.copy(alpha = 0.7f),
                        ToriElectricPink.copy(alpha = 0.7f)
                    ),
                    center = center
                ),
                radius = radius * 0.96f,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }
    }
}

private fun DrawScope.drawFluidRibbonStrands(
    center: Offset,
    radius: Float,
    phase: Float,
    rotation: Float
) {
    val strandCount = 18
    val radRot = Math.toRadians(rotation.toDouble()).toFloat()

    val colors = listOf(
        ToriElectricPink,
        ToriRubyGlow,
        ToriNeonViolet,
        ToriAmberGlow,
        ToriCyan,
        Color(0xFFC084FC)
    )

    for (i in 0 until strandCount) {
        val strandOffset = (i * (Math.PI * 2 / strandCount)).toFloat()
        val path = Path()
        val points = 36
        val color = colors[i % colors.size]

        var first = true
        for (p in 0..points) {
            val angle = (p.toFloat() / points) * (Math.PI * 2).toFloat()
            val wave = sin((angle * 3f + phase + strandOffset).toDouble()).toFloat() * (radius * 0.22f)
            val wave2 = cos((angle * 2f - phase * 0.7f).toDouble()).toFloat() * (radius * 0.12f)
            val currentRadius = radius * 0.78f + wave + wave2

            val totalAngle = angle + radRot
            val x = center.x + currentRadius * cos(totalAngle.toDouble()).toFloat()
            val y = center.y + currentRadius * sin(totalAngle.toDouble()).toFloat()

            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        drawPath(
            path = path,
            color = color.copy(alpha = 0.45f),
            style = Stroke(width = 2.2f),
            blendMode = BlendMode.Plus
        )
    }
}
