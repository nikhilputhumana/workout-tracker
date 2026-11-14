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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.components.BorderedCard
import com.example.ui.components.MuscleGroupTag
import com.example.ui.components.StatusBadge
import com.example.ui.components.WorkoutExerciseCardBlock
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
    onEditSession: (WorkoutSession, List<Long>) -> Unit,
    onDeleteSessions: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    var selectedFilter by remember { mutableStateOf("All") }
    var expandedDates by remember(initialExpandedId) {
        val targetDate = initialExpandedId?.let { id ->
            sessions.firstOrNull { it.id == id }?.dateIso
        }
        mutableStateOf(if (targetDate != null) setOf(targetDate) else emptySet())
    }
    LaunchedEffect(initialExpandedId, sessions) {
        if (initialExpandedId != null) {
            val targetDate = sessions.firstOrNull { it.id == initialExpandedId }?.dateIso
            if (targetDate != null && targetDate !in expandedDates) {
                expandedDates = expandedDates + targetDate
            }
        }
    }
    var pendingDeleteIds by remember { mutableStateOf<List<Long>?>(null) }

    val filterChips = listOf("All") + ExerciseCatalog.MUSCLE_GROUPS
    val filteredSessions = remember(selectedFilter, sessions) {
        if (selectedFilter == "All") sessions
        else sessionsMatchingGroups(listOf(selectedFilter))
    }
    val clubbedSessions = remember(filteredSessions) {
        WorkoutMath.clubSessionsByDate(filteredSessions)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedFilter = chip }
                            .testTag("history_filter_${chip.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chip,
                                fontSize = 13.sp,
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
                text = "${clubbedSessions.size} workout${if (clubbedSessions.size == 1) "" else "s"}" +
                    if (selectedFilter == "All") "" else " · $selectedFilter",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = palette.muted
            )
        }

        if (clubbedSessions.isEmpty()) {
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
            items(clubbedSessions, key = { it.session.dateIso }) { club ->
                val session = club.session
                val sourceIds = club.sourceIds
                val isOpen = session.dateIso in expandedDates
                val prCount = countSessionPrs(session, sessions, allExercises)
                val vol = WorkoutMath.sessionVolume(session, allExercises)
                val sets = WorkoutMath.sessionSets(session)
                val kcal = WorkoutMath.estimateSessionCalories(session, bodyWeightKg, allExercises)

                BorderedCard(
                    modifier = Modifier.animateContentSize()
                ) {
                    Column {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedDates = if (isOpen) expandedDates - session.dateIso
                                    else expandedDates + session.dateIso
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                                .testTag("history_session_${session.id}"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                if (session.durationMinutes > 0) {
                                    Text(
                                        text = "${session.durationMinutes} min",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.muted
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                IconButton(
                                    onClick = { onEditSession(session, sourceIds) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("history_edit_icon_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit workout",
                                        tint = palette.brandInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { pendingDeleteIds = sourceIds },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("history_delete_icon_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete workout",
                                        tint = palette.redInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (isOpen) "Collapse" else "Expand",
                                    tint = palette.faint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                session.groups.forEach { MuscleGroupTag(it) }
                                if (sourceIds.size > 1) {
                                    StatusBadge(
                                        label = "${sourceIds.size} clubbed",
                                        bg = palette.brandSoft,
                                        fg = palette.brandInk
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

                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                if (vol > 0.0) {
                                    Text(
                                        text = "${WorkoutMath.formatVolume(vol)} lifted",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.text2
                                    )
                                }
                                Text(
                                    text = "$sets sets",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.text2
                                )
                                if (kcal > 0) {
                                    Text(
                                        text = "~$kcal kcal",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.text2
                                    )
                                }
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
                                WorkoutExerciseCardBlock(
                                    exercise = ex,
                                    allExercises = allExercises,
                                    showSetPills = true,
                                    onClick = { onOpenExerciseDetail(ex.name) }
                                )
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
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onEditSession(session, sourceIds) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("history_edit_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit workout",
                                        tint = palette.brandInk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Edit workout",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.brandInk
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                TextButton(
                                    onClick = { pendingDeleteIds = sourceIds },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("history_delete_${session.id}")
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
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

    if (pendingDeleteIds != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteIds = null },
            title = { Text("Delete workout?") },
            text = { Text("This removes the logged session from your device history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteIds?.let(onDeleteSessions)
                        pendingDeleteIds = null
                    }
                ) {
                    Text("Delete", color = palette.redInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIds = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
