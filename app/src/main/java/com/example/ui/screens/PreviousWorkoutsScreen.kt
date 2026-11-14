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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.RpeRating
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.components.BorderedCard
import com.example.ui.theme.LocalWorkoutPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviousWorkoutsScreen(
    draftGroups: List<String>,
    draftExerciseNames: Set<String>,
    allExercises: List<ExerciseDefinition>,
    sessionsMatchingGroups: (List<String>) -> List<WorkoutSession>,
    onAddExerciseToDraft: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val chips = listOf<String?>(null) + (draftGroups.ifEmpty { ExerciseCatalog.MUSCLE_GROUPS.take(6) })
    val targetGroups = if (selectedFilter != null) listOf(selectedFilter!!) else draftGroups
    val sessions = remember(selectedFilter, draftGroups, sessionsMatchingGroups) {
        sessionsMatchingGroups(targetGroups).take(8)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BorderedCard(
                backgroundColor = palette.brandSoft,
                borderColor = palette.brandBd
            ) {
                Text(
                    text = if (selectedFilter != null) {
                        "Every $selectedFilter session on record. Tap Add to pull an exercise into today with your last weight."
                    } else {
                        "Everything you have done for ${draftGroups.joinToString(" + ").ifEmpty { "any group" }}, newest first. Tap Add to bring it into today."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.brandInk,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { grp ->
                    val isSelected = selectedFilter == grp
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) palette.brandSoft else palette.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) palette.brand else palette.borderStrong
                        ),
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { selectedFilter = grp }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = grp ?: "All selected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) palette.brandInk else palette.text2
                            )
                        }
                    }
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                BorderedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No previous sessions for this muscle group yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
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
                                text = WorkoutMath.formatShort(session.dateIso),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.text
                            )
                            Spacer(Modifier.padding(4.dp))
                            Text(
                                text = WorkoutMath.formatAgo(session.dateIso),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.faint
                            )
                            Spacer(Modifier.weight(1f))
                            val vol = WorkoutMath.sessionVolume(session, allExercises)
                            val durPart = if (session.durationMinutes > 0) "${session.durationMinutes} min" else ""
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

                        session.exercises.forEach { ex ->
                            val alreadyInDraft = draftExerciseNames.any {
                                it.equals(ex.name, ignoreCase = true)
                            }
                            val rpeColor = when (ex.rpe) {
                                RpeRating.EASY -> palette.greenInk
                                RpeRating.HARD -> palette.redInk
                                RpeRating.RIGHT -> palette.brandInk
                                null -> palette.faint
                            }
                            val rpeText = when (ex.rpe) {
                                RpeRating.EASY -> "Felt easy"
                                RpeRating.HARD -> "Felt hard"
                                RpeRating.RIGHT -> "Just right"
                                null -> "—"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                        text = ex.equipment,
                                        fontSize = 12.sp,
                                        color = palette.faint
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = WorkoutMath.formatSetSummary(ex, allExercises),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFeatureSettings = "tnum"
                                        ),
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.text
                                    )
                                    Text(
                                        text = rpeText,
                                        fontSize = 12.sp,
                                        color = rpeColor
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = palette.subtle,
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onAddExerciseToDraft(ex.name) }
                                        .testTag("prev_add_${ex.name.lowercase().replace(' ', '_')}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp)
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
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
