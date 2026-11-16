package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.domain.ActiveDraft
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.LoggedExercise
import com.example.domain.LoggedSet
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.components.BorderedCard
import com.example.ui.components.KpiTile
import com.example.ui.components.MuscleGroupTag
import com.example.ui.components.SectionLabel
import com.example.ui.components.StatusBadge
import com.example.ui.components.WorkoutExerciseCardBlock
import com.example.ui.components.countSessionPrs
import com.example.ui.theme.LocalWorkoutPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinishSummaryScreen(
    draft: ActiveDraft,
    allSessions: List<WorkoutSession>,
    allExercises: List<ExerciseDefinition>,
    bodyWeightKg: Double,
    onGoBack: () -> Unit,
    onFinishAndSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val isEditing = draft.editingSessionIds.isNotEmpty()
    val editingSet = draft.editingSessionIds.toSet()

    // Build preview of the exercises/sets that will actually be recorded from this draft
    val recordedDraftExercises = remember(draft, isEditing) {
        draft.exercises.mapIndexedNotNull { idx, ex ->
            val eligibleSets = if (isEditing) {
                ex.sets
            } else {
                ex.sets.filter { it.isCompleted }
            }
            if (eligibleSets.isEmpty()) return@mapIndexedNotNull null
            val loggedSets = eligibleSets.map { s ->
                val isCardio = ExerciseCatalog.isCardioExercise(ex.name, allExercises)
                val reps = s.repsText.toIntOrNull()?.coerceIn(0, 600) ?: 0
                val weight = if (!isCardio && ExerciseCatalog.findByName(ex.name, allExercises)?.isBodyweight == true) {
                    0.0
                } else {
                    s.weightText.toDoubleOrNull()?.coerceIn(0.0, 2000.0) ?: 0.0
                }
                LoggedSet(reps = reps, weightKg = weight, isCompleted = true)
            }
            LoggedExercise(
                id = idx.toLong() + 1L,
                name = ex.name,
                equipment = ex.equipment,
                sets = loggedSets,
                rpe = ex.rpe,
                note = ex.note.trim()
            )
        }
    }

    // Also inspect any workouts already saved earlier on the same calendar date
    val earlierSameDaySessions = remember(allSessions, draft.dateIso, editingSet) {
        allSessions.filter { s ->
            s.dateIso == draft.dateIso && s.id !in editingSet
        }
    }
    val earlierExercises = remember(earlierSameDaySessions) {
        earlierSameDaySessions.flatMap { it.exercises }
    }

    val allDayExercises = remember(recordedDraftExercises, earlierExercises) {
        recordedDraftExercises + earlierExercises
    }
    val durationMins = if (isEditing) {
        draft.manualDurationMinutes ?: 0
    } else {
        (draft.elapsedSeconds / 60).coerceAtLeast(if (recordedDraftExercises.isNotEmpty()) 1 else 0)
    }
    val draftPreviewSession = remember(draft, recordedDraftExercises, durationMins) {
        WorkoutSession(
            id = 0L,
            dateIso = draft.dateIso,
            groups = draft.groups,
            durationMinutes = durationMins,
            note = draft.sessionNote,
            exercises = recordedDraftExercises
        )
    }
    val combinedDaySession = remember(draft, allDayExercises, durationMins, earlierSameDaySessions) {
        WorkoutSession(
            id = 0L,
            dateIso = draft.dateIso,
            groups = (draft.groups + earlierSameDaySessions.flatMap { it.groups }).distinct(),
            durationMinutes = durationMins + earlierSameDaySessions.sumOf { it.durationMinutes },
            note = draft.sessionNote,
            exercises = allDayExercises
        )
    }

    val totalVol = remember(combinedDaySession, allExercises) {
        WorkoutMath.sessionVolume(combinedDaySession, allExercises)
    }
    val totalSets = remember(combinedDaySession) {
        WorkoutMath.sessionSets(combinedDaySession)
    }
    val totalKcal = remember(combinedDaySession, bodyWeightKg, allExercises) {
        WorkoutMath.estimateSessionCalories(combinedDaySession, bodyWeightKg, allExercises)
    }
    val prCount = remember(draftPreviewSession, allSessions, allExercises) {
        countSessionPrs(draftPreviewSession, allSessions, allExercises)
    }
    val untickedCount = remember(draft, isEditing) {
        if (isEditing) 0 else draft.exercises.sumOf { ex -> ex.sets.count { !it.isCompleted } }
    }
    val canSave = recordedDraftExercises.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Day Header Card
            item {
                BorderedCard(
                    backgroundColor = palette.brandSoft,
                    borderColor = palette.brandBd
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.brand),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isEditing) "Review workout edits" else "Today's workout summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.brandInk
                                )
                                Text(
                                    text = WorkoutMath.formatLong(draft.dateIso),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.brandHover
                                )
                            }
                            if (prCount > 0) {
                                StatusBadge(
                                    label = "$prCount PR",
                                    bg = palette.orangeSoft,
                                    fg = palette.orangeInk
                                )
                            }
                        }

                        if (combinedDaySession.groups.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                combinedDaySession.groups.forEach { grp ->
                                    MuscleGroupTag(grp)
                                }
                            }
                        }
                    }
                }
            }

            // Warning if no completed sets recorded yet or some sets left unticked
            if (!canSave) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.orangeSoft)
                            .border(1.dp, palette.orange.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("finish_summary_empty_warning"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Warning",
                            tint = palette.orangeInk,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "No completed sets recorded yet. Go back and tap the tick (✓) button on the right of each set you performed.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = palette.orangeInk
                        )
                    }
                }
            } else if (untickedCount > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.tealSoft)
                            .border(1.dp, palette.tealBd, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = palette.teal,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "$untickedCount unticked set${if (untickedCount == 1) "" else "s"} will not be saved. Go back if you want to tick them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.text2
                        )
                    }
                }
            }

            // KPI Summary Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiTile(
                            label = "Exercises",
                            value = "${allDayExercises.size}",
                            sub = "$totalSets completed sets",
                            modifier = Modifier.weight(1f)
                        )
                        KpiTile(
                            label = "Volume lifted",
                            value = if (totalVol > 0.0) WorkoutMath.formatVolume(totalVol) else "—",
                            sub = if (durationMins > 0) "$durationMins min total" else "session total",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiTile(
                            label = "Est. calories",
                            value = if (totalKcal > 0) "~$totalKcal" else "—",
                            sub = "kcal estimate",
                            modifier = Modifier.weight(1f)
                        )
                        KpiTile(
                            label = "Personal records",
                            value = "$prCount",
                            sub = if (prCount > 0) "new best today" else "matched or building",
                            subColor = if (prCount > 0) palette.orangeInk else palette.muted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Workouts Performed in This Session
            if (recordedDraftExercises.isNotEmpty()) {
                item {
                    BorderedCard(modifier = Modifier.testTag("finish_summary_exercises_card")) {
                        Column {
                            SectionLabel(
                                text = "Recorded in this workout (${recordedDraftExercises.size})",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(palette.border)
                            )
                            recordedDraftExercises.forEachIndexed { index, ex ->
                                WorkoutExerciseCardBlock(
                                    exercise = ex,
                                    allExercises = allExercises,
                                    showSetPills = true
                                )
                                if (index < recordedDraftExercises.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(palette.appBg)
                                    )
                                }
                            }
                            if (draft.sessionNote.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(palette.border)
                                )
                                Text(
                                    text = "Session note: ${draft.sessionNote.trim()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.muted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.appBg)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Workouts Already Saved Earlier Today (so user sees full day summary)
            if (earlierExercises.isNotEmpty()) {
                item {
                    BorderedCard {
                        Column {
                            SectionLabel(
                                text = "Earlier today · already saved (${earlierExercises.size})",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(palette.border)
                            )
                            earlierExercises.forEachIndexed { index, ex ->
                                WorkoutExerciseCardBlock(
                                    exercise = ex,
                                    allExercises = allExercises,
                                    showSetPills = true
                                )
                                if (index < earlierExercises.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(palette.appBg)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(6.dp)) }
        }

        // Bottom 2 Options: 1st Go back (add/edit), 2nd Finish and Save
        Surface(
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onGoBack,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.borderStrong),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("finish_summary_go_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = palette.text2,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Go back",
                        color = palette.text,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onFinishAndSave,
                    enabled = canSave,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.brand,
                        disabledContainerColor = palette.subtle,
                        contentColor = Color.White,
                        disabledContentColor = palette.faint
                    ),
                    modifier = Modifier
                        .weight(1.35f)
                        .heightIn(min = 50.dp)
                        .testTag("finish_summary_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isEditing) "Save changes" else "Finish and Save",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
