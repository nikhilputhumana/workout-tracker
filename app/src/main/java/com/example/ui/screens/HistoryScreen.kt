package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.RpeRating
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.components.BorderedCard
import com.example.ui.components.MuscleFigureBox
import com.example.ui.components.MuscleGroupTag
import com.example.ui.components.StatusBadge
import com.example.ui.components.countSessionPrs
import com.example.ui.theme.LocalWorkoutPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    sessions: List<WorkoutSession>,
    allExercises: List<ExerciseDefinition>,
    bodyWeightKg: Double,
    initialExpandedId: Long? = null,
    sessionsMatchingGroups: (List<String>) -> List<WorkoutSession>,
    onOpenExerciseDetail: (String) -> Unit,
    onDeleteSession: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    var selectedFilter by remember { mutableStateOf("All") }
    var expandedIds by remember(initialExpandedId) {
        mutableStateOf(if (initialExpandedId != null) setOf(initialExpandedId) else emptySet())
    }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    val filterChips = listOf("All") + ExerciseCatalog.MUSCLE_GROUPS
    val filteredSessions = remember(selectedFilter, sessions) {
        if (selectedFilter == "All") sessions
        else sessionsMatchingGroups(listOf(selectedFilter))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterChips.forEach { chip ->
                    val selected = selectedFilter == chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (selected) palette.brandSoft else palette.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) palette.brand else palette.borderStrong
                        ),
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedFilter = chip }
                            .testTag("history_filter_${chip.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chip,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) palette.brandInk else palette.text2
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "${filteredSessions.size} workout${if (filteredSessions.size == 1) "" else "s"}" +
                    if (selectedFilter == "All") "" else " · $selectedFilter",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = palette.muted
            )
        }

        if (filteredSessions.isEmpty()) {
            item {
                BorderedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No workouts recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                    }
                }
            }
        } else {
            items(filteredSessions, key = { it.id }) { session ->
                val isOpen = session.id in expandedIds
                val prCount = countSessionPrs(session, sessions)
                val vol = WorkoutMath.sessionVolume(session)
                val sets = WorkoutMath.sessionSets(session)
                val kcal = WorkoutMath.estimateCalories(
                    session.groups,
                    session.durationMinutes,
                    bodyWeightKg
                )

                BorderedCard(
                    modifier = Modifier.animateContentSize()
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedIds = if (isOpen) expandedIds - session.id
                                    else expandedIds + session.id
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("history_session_${session.id}"),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = WorkoutMath.formatShort(session.dateIso),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.text
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = WorkoutMath.formatAgo(session.dateIso),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.faint
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "${session.durationMinutes} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.muted
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isOpen) "Collapse" else "Expand",
                                    tint = palette.faint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                session.groups.forEach { MuscleGroupTag(it) }
                                if (prCount > 0) {
                                    StatusBadge(
                                        label = "$prCount PR",
                                        bg = palette.orangeSoft,
                                        fg = palette.orangeInk
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "${WorkoutMath.formatVolume(vol)} lifted",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.text2
                                )
                                Text(
                                    text = "$sets sets",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.text2
                                )
                                Text(
                                    text = "~$kcal kcal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.text2
                                )
                            }
                        }

                        if (isOpen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(palette.border)
                            )

                            session.exercises.forEach { ex ->
                                val def = ExerciseCatalog.findByName(ex.name, allExercises)
                                val rpeLabel = when (ex.rpe) {
                                    RpeRating.EASY -> "easy"
                                    RpeRating.RIGHT -> "just right"
                                    RpeRating.HARD -> "hard"
                                    null -> "not rated"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenExerciseDetail(ex.name) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MuscleFigureBox(
                                        regions = def?.regions ?: listOf("chest"),
                                        width = 30.dp,
                                        height = 40.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ex.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${ex.equipment} · $rpeLabel",
                                            fontSize = 11.sp,
                                            color = palette.faint
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = WorkoutMath.formatSetSummary(ex),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFeatureSettings = "tnum"
                                            ),
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.text
                                        )
                                        Text(
                                            text = WorkoutMath.formatVolume(WorkoutMath.exerciseVolume(ex)),
                                            fontSize = 11.sp,
                                            color = palette.faint
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(palette.appBg)
                                )
                            }

                            if (session.note.isNotBlank()) {
                                Text(
                                    text = session.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.muted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.appBg)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.appBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { pendingDeleteId = session.id },
                                    modifier = Modifier.testTag("history_delete_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete workout",
                                        tint = palette.redInk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Delete workout",
                                        fontSize = 12.sp,
                                        color = palette.redInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete workout?") },
            text = { Text("This removes the logged session from your device history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteId?.let(onDeleteSession)
                        pendingDeleteId = null
                    }
                ) {
                    Text("Delete", color = palette.redInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
