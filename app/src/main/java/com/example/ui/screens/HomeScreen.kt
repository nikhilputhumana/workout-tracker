package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.SplitPreset
import com.example.domain.WorkoutMath
import com.example.ui.components.BorderedCard
import com.example.ui.components.KpiTile
import com.example.ui.components.MuscleFigureBox
import com.example.ui.components.MuscleGroupTag
import com.example.ui.components.SectionLabel
import com.example.ui.components.SessionSummaryCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.VolumeBarChart
import com.example.ui.components.VolumeBarPoint
import com.example.ui.components.countSessionPrs
import com.example.ui.components.regionsForGroups
import com.example.ui.theme.LocalWorkoutPalette
import com.example.ui.viewmodel.MainUiState
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    suggestedSplit: Pair<SplitPreset, Long?>,
    onCreateWorkout: () -> Unit,
    onResumeDraft: () -> Unit,
    onStartSuggestedSplit: (SplitPreset) -> Unit,
    onOpenStats: () -> Unit,
    onOpenFees: () -> Unit,
    onOpenHistory: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val todayIso = WorkoutMath.todayIso()
    val clubbedSessions = remember(uiState.sessions) {
        WorkoutMath.clubSessionsByDate(uiState.sessions)
    }

    // Rolling 7-day volume bars (6 weeks), memoized across timer ticks
    val bars = remember(clubbedSessions, uiState.allExercises, todayIso) {
        (5 downTo 0).map { weekBack ->
            val endIso = WorkoutMath.isoOffsetDays(-weekBack * 7, todayIso)
            val startIso = WorkoutMath.isoOffsetDays(-6, endIso)
            val vol = clubbedSessions
                .map { it.session }
                .filter { it.dateIso in startIso..endIso }
                .sumOf { WorkoutMath.sessionVolume(it, uiState.allExercises) }
            VolumeBarPoint(
                label = WorkoutMath.shortWeekBucketLabel(startIso),
                topLabel = String.format(Locale.US, "%.1f", vol / 1000.0),
                value = vol.toFloat(),
                isHighlighted = weekBack == 0
            )
        }
    }

    val win7Start = remember(todayIso) { WorkoutMath.isoOffsetDays(-6, todayIso) }
    val inLast7 = remember(clubbedSessions, win7Start) {
        clubbedSessions.map { it.session }.filter { it.dateIso >= win7Start }
    }
    val weeklyTarget = uiState.profile.weeklyTargetSessions.coerceIn(1, 7)
    val thisWeekVol = bars.last().value.toDouble()
    val prevWeekVol = bars.getOrNull(bars.lastIndex - 1)?.value?.toDouble() ?: 0.0
    val volDiff = thisWeekVol - prevWeekVol
    val wkKcal = remember(inLast7, uiState.profile.bodyWeightKg, uiState.allExercises) {
        inLast7.sumOf {
            WorkoutMath.estimateSessionCalories(it, uiState.profile.bodyWeightKg, uiState.allExercises)
        }
    }

    val latestSession = clubbedSessions.firstOrNull()?.session
    val lastGapDays = latestSession?.let { WorkoutMath.daysBetween(it.dateIso, todayIso) }

    val dueIn = WorkoutMath.daysBetween(todayIso, uiState.gymFee.nextDueIso) ?: 10L
    val (feeBg, feeFg, feeBadge) = when {
        dueIn < 0 -> Triple(palette.redSoft, palette.redInk, "Overdue")
        dueIn <= 7 -> Triple(palette.orangeSoft, palette.orangeInk, "Due in ${dueIn}d")
        else -> Triple(palette.greenSoft, palette.greenInk, "Paid")
    }

    val (preset, gapDays) = suggestedSplit

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val draft = uiState.activeDraft
        val hasOngoingWorkout = draft != null
        if (draft != null) {
            item {
                BorderedCard(
                    backgroundColor = palette.brandSoft,
                    borderColor = palette.brandBd,
                    onClick = onResumeDraft,
                    modifier = Modifier.testTag("home_resume_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(palette.brand)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (draft.editingSessionIds.isNotEmpty()) "Editing history workout" else "Workout in progress",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.brandInk
                            )
                            Text(
                                text = "${draft.groups.joinToString(" + ").ifEmpty { "Session" }} · ${draft.exercises.size} exercises · ${WorkoutMath.formatTimer(draft.elapsedSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.brandHover
                            )
                        }
                        Text(
                            text = "Resume",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.brand
                        )
                    }
                }
            }
        }

        // Primary Create Workout CTA (blocked while a workout is already in progress)
        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (hasOngoingWorkout) palette.subtle else palette.brand,
                border = if (hasOngoingWorkout) BorderStroke(1.dp, palette.border) else null,
                shadowElevation = if (hasOngoingWorkout) 0.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !hasOngoingWorkout, onClick = onCreateWorkout)
                    .testTag("home_create_workout_button")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (hasOngoingWorkout) palette.border
                                else Color.White.copy(alpha = 0.18f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create workout",
                            tint = if (hasOngoingWorkout) palette.faint else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasOngoingWorkout) "Workout already active" else "Create workout",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (hasOngoingWorkout) palette.muted else Color.White
                        )
                        Text(
                            text = if (hasOngoingWorkout) {
                                "Finish or discard your active workout before starting a new one."
                            } else {
                                WorkoutMath.formatLong(todayIso)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasOngoingWorkout) palette.faint else Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (hasOngoingWorkout) palette.faint else Color.White
                    )
                }
            }
        }

        // Suggested Next Card
        item {
            BorderedCard(modifier = Modifier.testTag("home_suggested_next_card")) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("Suggested next")
                        Text(
                            text = if (gapDays == null) "Not logged yet" else "$gapDays days since last",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.faint
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MuscleFigureBox(
                            regions = regionsForGroups(preset.groups),
                            width = 46.dp,
                            height = 60.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                preset.groups.forEach { MuscleGroupTag(it) }
                            }
                            Text(
                                text = if (gapDays == null) {
                                    "Recommended starting point — no sessions recorded for ${preset.name.lowercase()} yet."
                                } else {
                                    "Longest gap in your rotation — last ${preset.name.lowercase()} day was $gapDays days ago."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.text2
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasOngoingWorkout) palette.subtle else palette.brandSoft,
                            border = BorderStroke(
                                1.dp,
                                if (hasOngoingWorkout) palette.border else palette.brandBd
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !hasOngoingWorkout) { onStartSuggestedSplit(preset) }
                                .testTag("home_start_suggested_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (hasOngoingWorkout) "Workout in progress" else "Start ${preset.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (hasOngoingWorkout) palette.faint else palette.brandInk
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasOngoingWorkout) palette.subtle else palette.surface,
                            border = BorderStroke(
                                1.dp,
                                if (hasOngoingWorkout) palette.border else palette.borderStrong
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !hasOngoingWorkout, onClick = onCreateWorkout)
                                .testTag("home_choose_groups_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Choose",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (hasOngoingWorkout) palette.faint else palette.text2
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2x2 KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        label = "Last 7 days",
                        value = WorkoutMath.formatVolume(thisWeekVol),
                        sub = "${if (volDiff >= 0) "▲" else "▼"} ${WorkoutMath.formatVolume(abs(volDiff))} vs prev 7",
                        subColor = if (volDiff >= 0) palette.greenInk else palette.redInk,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        label = "Sessions",
                        value = "${inLast7.size} / $weeklyTarget",
                        sub = "last 7 days · target $weeklyTarget",
                        subColor = if (inLast7.size >= weeklyTarget) palette.greenInk else palette.muted,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        label = "Calories burnt",
                        value = if (wkKcal > 0) "$wkKcal" else "—",
                        sub = "kcal, est. last 7 days",
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        label = "Last workout",
                        value = when (lastGapDays) {
                            null -> "—"
                            0L -> "today"
                            else -> "${lastGapDays}d ago"
                        },
                        sub = latestSession?.groups?.joinToString(" + ") ?: "nothing logged",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Volume Bar Chart Card
        item {
            BorderedCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("Volume lifted · per 7 days")
                        TextButton(
                            onClick = onOpenStats,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("home_stats_link")
                        ) {
                            Text(
                                text = "Stats",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.brand
                            )
                        }
                    }
                    VolumeBarChart(bars = bars, barMaxHeight = 58.dp)
                }
            }
        }

        // Gym Fees Strip
        item {
            BorderedCard(
                onClick = onOpenFees,
                modifier = Modifier.testTag("home_gym_fees_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(feeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = "Gym fees",
                            tint = feeFg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gym fees",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = palette.text
                        )
                        Text(
                            text = "Last paid ${WorkoutMath.formatShort(uiState.gymFee.lastPaidIso)} · ₹${WorkoutMath.formatKg(uiState.gymFee.amount)} · next due ${WorkoutMath.formatShort(uiState.gymFee.nextDueIso)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }
                    StatusBadge(label = feeBadge, bg = feeBg, fg = feeFg)
                }
            }
        }

        // Recent Workouts Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Recent workouts")
                if (clubbedSessions.isNotEmpty()) {
                    TextButton(
                        onClick = { onOpenHistory(null) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("home_view_all_history_button")
                    ) {
                        Text(
                            text = "View all",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.brand
                        )
                    }
                }
            }
        }

        if (clubbedSessions.isEmpty()) {
            item {
                BorderedCard(modifier = Modifier.testTag("home_empty_state_card")) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = palette.faint,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No workouts logged yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.text
                        )
                        Text(
                            text = "Tap Create workout above to log today's session, or import an existing JSON backup from Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }
                }
            }
        } else {
            items(clubbedSessions.take(3), key = { it.session.dateIso }) { club ->
                val session = club.session
                SessionSummaryCard(
                    session = session,
                    bodyWeightKg = uiState.profile.bodyWeightKg,
                    prCount = countSessionPrs(session, uiState.sessions, uiState.allExercises),
                    allExercises = uiState.allExercises,
                    onClick = { onOpenHistory(session.id) },
                    modifier = Modifier.testTag("home_recent_session_${session.id}")
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
