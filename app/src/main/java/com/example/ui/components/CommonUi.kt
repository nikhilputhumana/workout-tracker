package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.theme.LocalWorkoutPalette

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = palette.faint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun BorderedCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalWorkoutPalette.current
    val shape = RoundedCornerShape(8.dp)
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Surface(
        modifier = baseModifier,
        shape = shape,
        color = backgroundColor ?: palette.surface,
        border = BorderStroke(1.dp, borderColor ?: palette.border)
    ) {
        Column(content = content)
    }
}

@Composable
fun MuscleGroupTag(
    group: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val pair = palette.groupColor(group)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(pair.bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = group,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = pair.fg
        )
    }
}

@Composable
fun StatusBadge(
    label: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg
        )
    }
}

@Composable
fun KpiTile(
    label: String,
    value: String,
    sub: String,
    subColor: Color? = null,
    trailingHeader: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    BorderedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(label, modifier = Modifier.weight(1f))
                if (trailingHeader != null) {
                    Spacer(Modifier.width(6.dp))
                    trailingHeader()
                }
            }
            Text(
                text = value.ifEmpty { " " },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFeatureSettings = "tnum"
                ),
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = subColor ?: palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionSummaryCard(
    session: WorkoutSession,
    bodyWeightKg: Double,
    prCount: Int,
    allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val vol = WorkoutMath.sessionVolume(session, allExercises)
    val sets = WorkoutMath.sessionSets(session)
    val kcal = WorkoutMath.estimateSessionCalories(session, bodyWeightKg, allExercises)

    BorderedCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
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
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                session.groups.forEach { g -> MuscleGroupTag(g) }
                if (prCount > 0) {
                    StatusBadge(
                        label = "$prCount PR",
                        bg = palette.orangeSoft,
                        fg = palette.orangeInk
                    )
                }
            }

            Text(
                text = session.exercises.joinToString(" · ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.subtle)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
    }
}

@Composable
fun rememberDatePickerLauncher(
    initialIso: String,
    onDateSelected: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    return {
        val (year, month1, day) = WorkoutMath.yearMonthDay(initialIso)
        DatePickerDialog(
            context,
            { _, pickedYear, pickedMonth0, pickedDay ->
                onDateSelected(WorkoutMath.isoOf(pickedYear, pickedMonth0 + 1, pickedDay))
            },
            year,
            month1 - 1,
            day
        ).show()
    }
}

fun countSessionPrs(session: WorkoutSession, allSessions: List<WorkoutSession>): Int {
    var prs = 0
    for (ex in session.exercises) {
        val best = WorkoutMath.bestSet(ex) ?: continue
        if (best.weightKg <= 0.0) continue
        val priorMax = allSessions
            .filter { it.dateIso < session.dateIso }
            .flatMap { it.exercises }
            .filter { it.name.equals(ex.name, ignoreCase = true) }
            .flatMap { it.sets }
            .maxOfOrNull { it.weightKg } ?: 0.0
        if (priorMax > 0.0 && best.weightKg > priorMax) {
            prs++
        }
    }
    return prs
}

fun regionsForGroups(groups: List<String>): List<String> = buildSet {
    groups.forEach { g ->
        when (g.lowercase()) {
            "legs" -> addAll(listOf("quads", "glutes", "calves"))
            "push", "chest" -> addAll(listOf("chest", "shoulders", "triceps"))
            "pull", "back" -> addAll(listOf("back", "biceps"))
            "shoulders" -> addAll(listOf("shoulders", "traps"))
            "abs" -> add("abs")
            "biceps" -> add("biceps")
            "triceps" -> add("triceps")
            "cardio" -> add("heart")
        }
    }
}.toList()
