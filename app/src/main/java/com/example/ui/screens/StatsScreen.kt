package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ExerciseCatalog
import com.example.domain.LoggedExercise
import com.example.domain.WorkoutMath
import com.example.ui.components.BorderedCard
import com.example.ui.components.KpiTile
import com.example.ui.components.LineChartPoint
import com.example.ui.components.SectionLabel
import com.example.ui.components.TrendLineChart
import com.example.ui.components.VolumeBarChart
import com.example.ui.components.VolumeBarPoint
import com.example.ui.theme.LocalWorkoutPalette
import com.example.ui.viewmodel.MainUiState
import java.util.Locale
import kotlin.math.abs

@Composable
fun StatsScreen(
    uiState: MainUiState,
    allSessionsOf: (String) -> List<Pair<String, LoggedExercise>>,
    onLogBodyWeight: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    var newWeightText by remember { mutableStateOf("") }
    val todayIso = WorkoutMath.todayIso()

    val allVol = uiState.sessions.sumOf { WorkoutMath.sessionVolume(it) }
    val allKcal = uiState.sessions.sumOf {
        WorkoutMath.estimateCalories(it.groups, it.durationMinutes, uiState.profile.bodyWeightKg)
    }
    val avgMins = if (uiState.sessions.isEmpty()) 0
    else uiState.sessions.sumOf { it.durationMinutes } / uiState.sessions.size
    val avgSets = if (uiState.sessions.isEmpty()) 0
    else uiState.sessions.sumOf { WorkoutMath.sessionSets(it) } / uiState.sessions.size

    val bwLog = uiState.bodyWeightLog
    val latestBw = bwLog.lastOrNull()?.weightKg ?: uiState.profile.bodyWeightKg
    val bwDelta = if (bwLog.size >= 2) bwLog.last().weightKg - bwLog.first().weightKg else 0.0
    val bwDeltaStr = "${if (bwDelta > 0) "+" else ""}${String.format(Locale.US, "%.1f", bwDelta)} kg"

    val bars = (5 downTo 0).map { weekBack ->
        val endIso = WorkoutMath.isoOffsetDays(-weekBack * 7, todayIso)
        val startIso = WorkoutMath.isoOffsetDays(-6, endIso)
        val vol = uiState.sessions
            .filter { it.dateIso in startIso..endIso }
            .sumOf { WorkoutMath.sessionVolume(it) }
        VolumeBarPoint(
            label = WorkoutMath.shortWeekBucketLabel(startIso),
            topLabel = String.format(Locale.US, "%.1f", vol / 1000.0),
            value = vol.toFloat(),
            isHighlighted = weekBack == 0
        )
    }

    val bwPoints = bwLog.map { entry ->
        LineChartPoint(
            label = WorkoutMath.formatDayMonth(entry.dateIso),
            value = entry.weightKg.toFloat(),
            unit = " kg"
        )
    }

    val trackedLifts = listOf(
        "Back squat",
        "Barbell bench press",
        "Overhead press",
        "Lat pulldown",
        "Barbell curl"
    )
    val strengthRows = trackedLifts.mapNotNull { name ->
        val rows = allSessionsOf(name)
        if (rows.isEmpty()) return@mapNotNull null
        val first = rows.last()
        val latest = rows.first()
        val firstBest = WorkoutMath.bestSet(first.second) ?: return@mapNotNull null
        val latestBest = WorkoutMath.bestSet(latest.second) ?: return@mapNotNull null
        val first1Rm = WorkoutMath.epley1Rm(firstBest.reps, firstBest.weightKg)
        val latest1Rm = WorkoutMath.epley1Rm(latestBest.reps, latestBest.weightKg)
        val pct = if (first1Rm > 0.0) (latest1Rm / first1Rm - 1.0) * 100.0 else 0.0
        Triple(
            name,
            "${WorkoutMath.formatKg(firstBest.weightKg)} kg → ${WorkoutMath.formatKg(latestBest.weightKg)} kg",
            pct
        )
    }

    val fourWeeksAgoIso = WorkoutMath.isoOffsetDays(-28, todayIso)
    val setsByGroup = mutableMapOf<String, Int>()
    uiState.sessions
        .filter { it.dateIso >= fourWeeksAgoIso }
        .forEach { session ->
            session.exercises.forEach { ex ->
                val def = ExerciseCatalog.findByName(ex.name, uiState.allExercises)
                val primary = def?.groups?.firstOrNull() ?: session.groups.firstOrNull() ?: "Other"
                setsByGroup[primary] = (setsByGroup[primary] ?: 0) + ex.sets.size
            }
        }
    val sortedGroups = setsByGroup.entries.sortedByDescending { it.value }
    val maxGroupSets = (sortedGroups.firstOrNull()?.value ?: 1).coerceAtLeast(1)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        label = "Total lifted",
                        value = String.format(Locale.US, "%.1f t", allVol / 1000.0),
                        sub = "across ${uiState.sessions.size} workouts",
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        label = "Calories burnt",
                        value = if (allKcal > 0) "$allKcal" else "—",
                        sub = "estimated, all time",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        label = "Avg session",
                        value = if (avgMins > 0) "$avgMins min" else "—",
                        sub = if (avgSets > 0) "$avgSets sets typical" else "no sessions yet",
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        label = "Body weight",
                        value = "${WorkoutMath.formatKg(latestBw)} kg",
                        sub = if (bwLog.size >= 2) "$bwDeltaStr total" else "current weight",
                        subColor = if (bwDelta < 0) palette.greenInk else palette.muted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            BorderedCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel("Volume lifted · per 7 days")
                        Text(
                            text = "tonnes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.faint
                        )
                    }
                    VolumeBarChart(bars = bars, barMaxHeight = 66.dp)
                }
            }
        }

        item {
            BorderedCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("Body weight")
                        if (bwLog.size >= 2) {
                            Text(
                                text = bwDeltaStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (bwDelta < 0) palette.greenInk else palette.muted
                            )
                        }
                    }

                    if (bwPoints.isNotEmpty()) {
                        TrendLineChart(points = bwPoints, chartHeight = 80.dp)
                    } else {
                        Text(
                            text = "Log your first weigh-in below to start tracking your trend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newWeightText,
                            onValueChange = { newWeightText = it },
                            placeholder = { Text(WorkoutMath.formatKg(latestBw)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.surface,
                                unfocusedContainerColor = palette.surface,
                                focusedBorderColor = palette.brand,
                                unfocusedBorderColor = palette.borderStrong,
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stats_bw_input")
                        )
                        Button(
                            onClick = {
                                val parsed = newWeightText.toDoubleOrNull()
                                if (parsed != null && parsed > 20.0) {
                                    onLogBodyWeight(parsed)
                                    newWeightText = ""
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.brandSoft,
                                contentColor = palette.brandInk
                            ),
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .testTag("stats_log_bw_button")
                        ) {
                            Text("Log today", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        item {
            BorderedCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionLabel("Strength change")
                    Text(
                        text = "Best set now vs your first logged session, by estimated one-rep max.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )

                    if (strengthRows.isEmpty()) {
                        Text(
                            text = "Log core lifts across multiple sessions to compare strength progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.faint
                        )
                    } else {
                        strengthRows.forEach { (name, fromTo, pct) ->
                            val barColor = if (pct > 0) palette.greenMid else palette.borderStrong
                            val textColor = when {
                                pct > 0 -> palette.greenInk
                                pct < 0 -> palette.redInk
                                else -> palette.muted
                            }
                            val fillFraction = (abs(pct).toFloat() * 0.08f).coerceIn(0.05f, 1f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = fromTo,
                                        fontSize = 11.sp,
                                        color = palette.faint
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(74.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.subtle)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fillFraction)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(barColor)
                                    )
                                }
                                Text(
                                    text = "${if (pct >= 0) "+" else ""}${String.format(Locale.US, "%.1f", pct)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(54.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            BorderedCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionLabel("Sets per muscle group · last 4 weeks")
                    if (sortedGroups.isEmpty()) {
                        Text(
                            text = "No sets logged in the last 4 weeks yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    } else {
                        sortedGroups.forEach { (group, count) ->
                            val frac = (count.toFloat() / maxGroupSets.toFloat()).coerceIn(0.05f, 1f)
                            val barColor = palette.groupColor(group).fg
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = group,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.text2,
                                    modifier = Modifier.width(74.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.appBg)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(frac)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(barColor)
                                    )
                                }
                                Text(
                                    text = "$count",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.text,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "Aim for 10–20 hard sets per muscle group per week. ${sortedGroups.last().key} is your lightest right now.",
                            fontSize = 11.sp,
                            color = palette.faint
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(6.dp)) }
    }
}
