package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalWorkoutPalette

private sealed interface FigureShape {
    data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : FigureShape
    data class RoundRect(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val r: Float = 1.5f
    ) : FigureShape
}

private val BASE_BODY_SHAPES = listOf(
    FigureShape.Ellipse(20f, 6.6f, 4f, 4.6f),
    FigureShape.RoundRect(18.6f, 10.4f, 2.8f, 2.6f, 0.8f),
    FigureShape.RoundRect(12.8f, 12.8f, 14.4f, 23f, 4f),
    FigureShape.RoundRect(8.2f, 15.4f, 4f, 20f, 2f),
    FigureShape.RoundRect(27.8f, 15.4f, 4f, 20f, 2f),
    FigureShape.RoundRect(14f, 34.4f, 12f, 4.6f, 2f),
    FigureShape.RoundRect(14.2f, 37.6f, 5.2f, 24f, 2.6f),
    FigureShape.RoundRect(20.6f, 37.6f, 5.2f, 24f, 2.6f)
)

private val REGION_SHAPES: Map<String, List<FigureShape>> = mapOf(
    "shoulders" to listOf(
        FigureShape.Ellipse(12.4f, 17.6f, 3.5f, 3f),
        FigureShape.Ellipse(27.6f, 17.6f, 3.5f, 3f)
    ),
    "chest" to listOf(
        FigureShape.RoundRect(13.2f, 19f, 6.2f, 6f, 1.8f),
        FigureShape.RoundRect(20.6f, 19f, 6.2f, 6f, 1.8f)
    ),
    "back" to listOf(
        FigureShape.RoundRect(13f, 19.5f, 6.2f, 9f, 2f),
        FigureShape.RoundRect(20.8f, 19.5f, 6.2f, 9f, 2f)
    ),
    "abs" to listOf(
        FigureShape.RoundRect(16.4f, 26.5f, 7.2f, 8.5f, 1.6f)
    ),
    "biceps" to listOf(
        FigureShape.Ellipse(10.2f, 24f, 2.3f, 3.4f),
        FigureShape.Ellipse(29.8f, 24f, 2.3f, 3.4f)
    ),
    "triceps" to listOf(
        FigureShape.Ellipse(9.7f, 28.6f, 2.1f, 3f),
        FigureShape.Ellipse(30.3f, 28.6f, 2.1f, 3f)
    ),
    "forearms" to listOf(
        FigureShape.Ellipse(8.7f, 33.4f, 2f, 3.6f),
        FigureShape.Ellipse(31.3f, 33.4f, 2f, 3.6f)
    ),
    "traps" to listOf(
        FigureShape.RoundRect(15f, 14f, 10f, 3.6f, 1.6f)
    ),
    "glutes" to listOf(
        FigureShape.Ellipse(16.6f, 37.6f, 3f, 2.6f),
        FigureShape.Ellipse(23.4f, 37.6f, 3f, 2.6f)
    ),
    "quads" to listOf(
        FigureShape.RoundRect(14f, 39.5f, 5.2f, 11f, 2.4f),
        FigureShape.RoundRect(20.8f, 39.5f, 5.2f, 11f, 2.4f)
    ),
    "hams" to listOf(
        FigureShape.RoundRect(14f, 43f, 5.2f, 9f, 2.4f),
        FigureShape.RoundRect(20.8f, 43f, 5.2f, 9f, 2.4f)
    ),
    "calves" to listOf(
        FigureShape.Ellipse(16.5f, 54f, 2.5f, 4f),
        FigureShape.Ellipse(23.5f, 54f, 2.5f, 4f)
    ),
    "heart" to listOf(
        FigureShape.Ellipse(20f, 22.5f, 3.6f, 3.6f)
    )
)

@Composable
fun MuscleFigureBox(
    regions: List<String>,
    width: Dp = 46.dp,
    height: Dp = 60.dp,
    modifier: Modifier = Modifier,
    containerColor: Color? = null
) {
    val palette = LocalWorkoutPalette.current
    val bg = containerColor ?: palette.appBg
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / 40f
            val scaleY = size.height / 64f
            val scale = minOf(scaleX, scaleY)
            val offsetX = (size.width - 40f * scale) / 2f
            val offsetY = (size.height - 64f * scale) / 2f

            fun drawShape(shape: FigureShape, color: Color) {
                when (shape) {
                    is FigureShape.Ellipse -> {
                        val left = offsetX + (shape.cx - shape.rx) * scale
                        val top = offsetY + (shape.cy - shape.ry) * scale
                        drawOval(
                            color = color,
                            topLeft = Offset(left, top),
                            size = Size(shape.rx * 2f * scale, shape.ry * 2f * scale)
                        )
                    }
                    is FigureShape.RoundRect -> {
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(
                                offsetX + shape.x * scale,
                                offsetY + shape.y * scale
                            ),
                            size = Size(shape.w * scale, shape.h * scale),
                            cornerRadius = CornerRadius(shape.r * scale, shape.r * scale)
                        )
                    }
                }
            }

            BASE_BODY_SHAPES.forEach { drawShape(it, palette.figBase) }
            regions.forEach { region ->
                REGION_SHAPES[region]?.forEach {
                    drawShape(it, palette.figHighlight.copy(alpha = 0.92f))
                }
            }
        }
    }
}
