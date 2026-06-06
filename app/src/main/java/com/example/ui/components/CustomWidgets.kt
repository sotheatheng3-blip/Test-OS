package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadialGauge(
    value: Float,
    max: Float,
    title: String,
    unit: String,
    modifier: Modifier = Modifier,
    gaugeColor: Color = BentoPurpleAccent
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "gauge"
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val sweepAngle = 240f
            val startAngle = 150f
            val strokeWidth = 12.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.width - strokeWidth * 2) / 2
            
            // Background track
            drawArc(
                color = BentoGreySurface,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // Value sweep
            val ratio = (animatedValue / max).coerceIn(0f, 1f)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(gaugeColor.copy(alpha = 0.4f), gaugeColor),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle * ratio,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%.1f", animatedValue),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoTextDark
                )
            )
            Text(
                text = "$unit ($title)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BentoTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun TelemetryWaveGraph(
    values: List<Float>,
    max: Float,
    modifier: Modifier = Modifier,
    lineColor: Color = BentoPurpleAccent
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val pointCount = values.size
        val xStep = width / (pointCount - 1).coerceAtLeast(1)

        val points = values.mapIndexed { idx, valItem ->
            val x = idx * xStep
            val y = height - (valItem / max).coerceAtLeast(0f).coerceIn(0f, 1f) * height
            Offset(x, y)
        }

        // Draw background gradient
        val fillBrush = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
            startY = 0f,
            endY = height
        )
        
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(width, height)
            close()
        }
        drawPath(path = fillPath, brush = fillBrush)

        // Draw line connection path
        val strokePath = androidx.compose.ui.graphics.Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
        }
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun RadarScannerEffect(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
    radarColor: Color = BentoPurpleAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepRotation"
    )

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulse"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, delayMillis = 1500, easing = EaseOutExpo),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarPulse2"
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // Static concentric grid
            for (circleRadius in listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)) {
                drawCircle(
                    color = radarColor.copy(alpha = 0.15f),
                    radius = maxRadius * circleRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Crosshair lines
            drawLine(
                color = radarColor.copy(alpha = 0.15f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = radarColor.copy(alpha = 0.15f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.dp.toPx()
            )

            if (isScanning) {
                // Expanding rings
                drawCircle(
                    color = radarColor.copy(alpha = 0.35f * (1f - pulseScale1)),
                    radius = maxRadius * pulseScale1,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                drawCircle(
                    color = radarColor.copy(alpha = 0.35f * (1f - pulseScale2)),
                    radius = maxRadius * pulseScale2,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Rotating radar sweep beam
                val angleRad = Math.toRadians(rotationAngle.toDouble())
                val endX = center.x + maxRadius * cos(angleRad).toFloat()
                val endY = center.y + maxRadius * sin(angleRad).toFloat()

                // Glow swept fan
                val sweepBrush = Brush.sweepGradient(
                    colors = listOf(
                        radarColor.copy(alpha = 0.3f),
                        radarColor.copy(alpha = 0.0f)
                    ),
                    center = center
                )
                drawCircle(
                    brush = sweepBrush,
                    radius = maxRadius,
                    center = center
                )

                drawLine(
                    color = radarColor,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
