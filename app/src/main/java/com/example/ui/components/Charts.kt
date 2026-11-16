package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalWorkoutPalette

data class VolumeBarPoint(
    val label: String,
    val topLabel: String,
    val value: Float,
    val isHighlighted: Boolean
)

data class LineChartPoint(
    val label: String,
    val value: Float,
    val unit: String = ""
)

@Composable
fun VolumeBarChart(
    bars: List<VolumeBarPoint>,
    barMaxHeight: Dp = 62.dp,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val maxVal = (bars.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            val ratio = (bar.value / maxVal).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (bar.value > 0f) bar.topLabel else "",
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (bar.isHighlighted) palette.figHighlight else palette.faint,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barMaxHeight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val heightFraction = if (bar.value <= 0f) 0.04f else ratio.coerceAtLeast(0.06f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(heightFraction)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (bar.isHighlighted) palette.brand else palette.chartTrack)
                    )
                }
                Text(
                    text = bar.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = palette.faint,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TrendLineChart(
    points: List<LineChartPoint>,
    chartHeight: Dp = 82.dp,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    if (points.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .padding(horizontal = 2.dp)
        ) {
            val w = size.width
            val h = size.height
            val minVal = points.minOf { it.value }
            val maxVal = points.maxOf { it.value }
            val span = (maxVal - minVal).takeIf { it > 0.001f } ?: 1f

            fun xAt(i: Int): Float =
                if (points.size == 1) w / 2f
                else 6f + i * (w - 12f) / (points.size - 1)

            fun yAt(v: Float): Float =
                h - 8f - ((v - minVal + span * 0.15f) / (span * 1.3f)) * (h - 18f)

            val linePath = Path()
            val areaPath = Path()
            points.forEachIndexed { idx, pt ->
                val x = xAt(idx)
                val y = yAt(pt.value)
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    areaPath.moveTo(x, h)
                    areaPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    areaPath.lineTo(x, y)
                }
            }
            areaPath.lineTo(xAt(points.lastIndex), h)
            areaPath.close()

            drawPath(path = areaPath, color = palette.chartArea)
            drawPath(
                path = linePath,
                color = palette.figHighlight,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            points.forEachIndexed { idx, pt ->
                val center = Offset(xAt(idx), yAt(pt.value))
                val isLast = idx == points.lastIndex
                val radius = if (isLast) 4.dp.toPx() else 2.5.dp.toPx()
                drawCircle(
                    color = if (isLast) palette.figHighlight else palette.surface,
                    radius = radius,
                    center = center
                )
                if (!isLast) {
                    drawCircle(
                        color = palette.figHighlight,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = palette.faint
            )
            val latest = points.last()
            val valText = if (latest.value % 1f == 0f) "${latest.value.toInt()}" else String.format(java.util.Locale.US, "%.1f", latest.value)
            Text(
                text = "$valText${latest.unit}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text
            )
            Text(
                text = points.last().label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = palette.faint
            )
        }
    }
}

data class MultiLineSeries(
    val name: String,
    val color: Color,
    val values: List<Float?>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiLineChart(
    dateLabels: List<String>,
    series: List<MultiLineSeries>,
    unit: String = " cm",
    chartHeight: Dp = 104.dp,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val activeSeries = series.filter { s -> s.values.any { it != null } }
    if (dateLabels.isEmpty() || activeSeries.isEmpty()) return

    val allValues = activeSeries.flatMap { it.values.filterNotNull() }
    if (allValues.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .padding(horizontal = 4.dp)
        ) {
            val w = size.width
            val h = size.height
            val minVal = allValues.minOrNull() ?: 0f
            val maxVal = allValues.maxOrNull() ?: 1f
            val span = (maxVal - minVal).takeIf { it > 0.001f } ?: 1f

            fun xAt(i: Int): Float =
                if (dateLabels.size <= 1) w / 2f
                else 8f + i * (w - 16f) / (dateLabels.size - 1)

            fun yAt(v: Float): Float =
                h - 10f - ((v - minVal + span * 0.15f) / (span * 1.3f)) * (h - 20f)

            // Subtle reference guide lines
            listOf(0.18f, 0.5f, 0.82f).forEach { frac ->
                val y = h * frac
                drawLine(
                    color = palette.border,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            activeSeries.forEach { lineSeries ->
                val indexedPoints = lineSeries.values.mapIndexedNotNull { idx, value ->
                    if (idx < dateLabels.size && value != null) idx to value else null
                }
                if (indexedPoints.size >= 2) {
                    val path = Path()
                    indexedPoints.forEachIndexed { ptIdx, (xIdx, value) ->
                        val x = xAt(xIdx)
                        val y = yAt(value)
                        if (ptIdx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = lineSeries.color,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                indexedPoints.forEachIndexed { ptIdx, (xIdx, value) ->
                    val center = Offset(xAt(xIdx), yAt(value))
                    val isLast = ptIdx == indexedPoints.lastIndex
                    val radius = if (isLast) 4.dp.toPx() else 2.5.dp.toPx()
                    drawCircle(
                        color = if (isLast) lineSeries.color else palette.surface,
                        radius = radius,
                        center = center
                    )
                    if (!isLast) {
                        drawCircle(
                            color = lineSeries.color,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateLabels.first(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = palette.faint
            )
            if (dateLabels.size > 1) {
                Text(
                    text = dateLabels.last(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.faint
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            activeSeries.forEach { s ->
                val latest = s.values.filterNotNull().lastOrNull()
                if (latest != null) {
                    val valText = if (latest % 1f == 0f) {
                        "${latest.toInt()}"
                    } else {
                        String.format(java.util.Locale.US, "%.1f", latest)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(s.color)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "${s.name} $valText$unit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.text2
                        )
                    }
                }
            }
        }
    }
}
