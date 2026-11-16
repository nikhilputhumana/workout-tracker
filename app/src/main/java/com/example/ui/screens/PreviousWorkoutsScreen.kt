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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.LoggedExercise
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.components.BorderedCard
import com.example.ui.components.WorkoutExerciseCardBlock
import com.example.ui.theme.LocalWorkoutPalette

private data class FilteredDaySessions(
    val dateIso: String,
    val durationMinutes: Int,
    val note: String,
    val exercises: List<LoggedExercise>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviousWorkoutsScreen(
    draftGroups: List<String>,
    draftExerciseNames: Set<String>,
    allExercises: List<ExerciseDefinition>,
    allSessions: List<WorkoutSession>,
    onAddExerciseToDraft: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var otherGroupFilter by remember { mutableStateOf<String?>(null) }

    val primaryHeading = remember(draftGroups) {
        draftGroups.joinToString(" + ").ifEmpty { "All workouts" }
    }

    val clubbedSessions = remember(allSessions) {
        WorkoutMath.clubSessionsByDate(allSessions)
    }

    fun matchesGroups(ex: LoggedExercise, sessionGroups: List<String>, targetGroups: List<String>): Boolean {
        if (targetGroups.isEmpty()) return true
        val def = ExerciseCatalog.findByName(ex.name, allExercises)
        return if (def != null) {
            def.groups.any { g -> targetGroups.any { it.equals(g, ignoreCase = true) } }
        } else {
            sessionGroups.any { g -> targetGroups.any { it.equals(g, ignoreCase = true) } }
        }
    }

    // Tab 0: every single day containing exercises for the active muscle group(s), newest to oldest
    val primaryDayCards = remember(clubbedSessions, draftGroups, allExercises) {
        clubbedSessions.mapNotNull { club ->
            val session = club.session
            val matchingExercises = session.exercises.filter { ex ->
                matchesGroups(ex, session.groups, draftGroups)
            }
            if (matchingExercises.isEmpty()) null
            else FilteredDaySessions(
                dateIso = session.dateIso,
                durationMinutes = session.durationMinutes,
                note = session.note,
                exercises = matchingExercises
            )
        }
    }

    // Tab 1: all other exercises outside the active muscle group(s), newest to oldest
    val otherAvailableGroups = remember(draftGroups) {
        ExerciseCatalog.MUSCLE_GROUPS.filter { grp ->
            draftGroups.none { it.equals(grp, ignoreCase = true) }
        }
    }

    val otherDayCards = remember(clubbedSessions, draftGroups, otherGroupFilter, allExercises) {
        clubbedSessions.mapNotNull { club ->
            val session = club.session
            val nonMatchingExercises = session.exercises.filter { ex ->
                val inDraft = if (draftGroups.isEmpty()) false else matchesGroups(ex, session.groups, draftGroups)
                if (inDraft) return@filter false
                if (otherGroupFilter == null) true
                else matchesGroups(ex, session.groups, listOf(otherGroupFilter!!))
            }
            if (nonMatchingExercises.isEmpty()) null
            else FilteredDaySessions(
                dateIso = session.dateIso,
                durationMinutes = session.durationMinutes,
                note = session.note,
                exercises = nonMatchingExercises
            )
        }
    }

    val displayedCards = if (selectedTab == 0) primaryDayCards else otherDayCards

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = palette.surface,
            contentColor = palette.brand,
            indicator = { positions ->
                if (selectedTab < positions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                        color = palette.brand
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = primaryHeading,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedTab == 0) palette.brandInk else palette.muted
                    )
                },
                modifier = Modifier.testTag("prev_tab_primary")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Other workouts",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedTab == 1) palette.brandInk else palette.muted
                    )
                },
                modifier = Modifier.testTag("prev_tab_other")
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BorderedCard(
                    backgroundColor = palette.brandSoft,
                    borderColor = palette.brandBd
                ) {
                    Text(
                        text = if (selectedTab == 0) {
                            "Every $primaryHeading workout you logged, from newest to oldest (${primaryDayCards.size} day${if (primaryDayCards.size == 1) "" else "s"}). Tap Add to pull an exercise into today."
                        } else {
                            "All other workouts outside $primaryHeading (${otherDayCards.size} day${if (otherDayCards.size == 1) "" else "s"}). Tap Add to pull an exercise into today."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.brandInk,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            }

            if (selectedTab == 1 && otherAvailableGroups.isNotEmpty()) {
                item {
                    val chips = listOf<String?>(null) + otherAvailableGroups
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chips.forEach { grp ->
                            val isSelected = otherGroupFilter == grp
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) palette.brandSoft else palette.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) palette.brand else palette.borderStrong
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { otherGroupFilter = grp }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = grp ?: "All other",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) palette.brandInk else palette.text2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (displayedCards.isEmpty()) {
                item {
                    BorderedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (selectedTab == 0) {
                                    "No previous $primaryHeading workouts recorded yet."
                                } else {
                                    "No other workouts recorded yet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.muted
                            )
                        }
                    }
                }
            } else {
                items(displayedCards, key = { "${selectedTab}_${it.dateIso}" }) { day ->
                    val tempSession = WorkoutSession(
                        id = 0L,
                        dateIso = day.dateIso,
                        groups = draftGroups,
                        durationMinutes = day.durationMinutes,
                        note = day.note,
                        exercises = day.exercises
                    )
                    BorderedCard {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.appBg)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = WorkoutMath.formatShort(day.dateIso),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.text
                                )
                                Spacer(Modifier.padding(4.dp))
                                Text(
                                    text = WorkoutMath.formatAgo(day.dateIso),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.faint
                                )
                                Spacer(Modifier.weight(1f))
                                val vol = WorkoutMath.sessionVolume(tempSession, allExercises)
                                val durPart = if (day.durationMinutes > 0) "${day.durationMinutes} min" else ""
                                val metaLabel = listOf(
                                    if (vol > 0.0) WorkoutMath.formatVolume(vol) else "",
                                    durPart
                                ).filter { it.isNotEmpty() }.joinToString(" · ")
                                if (metaLabel.isNotEmpty()) {
                                    Text(
                                        text = metaLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.text2
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(palette.border)
                            )

                            day.exercises.forEach { ex ->
                                val alreadyInDraft = draftExerciseNames.any {
                                    it.equals(ex.name, ignoreCase = true)
                                }
                                WorkoutExerciseCardBlock(
                                    exercise = ex,
                                    allExercises = allExercises,
                                    showSetPills = true,
                                    trailingHeaderAction = {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = palette.subtle,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { onAddExerciseToDraft(ex.name) }
                                                .testTag("prev_add_${ex.name.lowercase().replace(' ', '_')}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = if (alreadyInDraft) "Added" else "Add",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (alreadyInDraft) palette.faint else palette.brandInk
                                                )
                                            }
                                        }
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(palette.appBg)
                                )
                            }

                            if (day.note.isNotBlank()) {
                                Text(
                                    text = day.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.muted,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.appBg)
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
