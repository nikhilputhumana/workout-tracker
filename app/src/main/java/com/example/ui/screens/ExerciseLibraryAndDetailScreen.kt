package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.LoggedExercise
import com.example.domain.RpeRating
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.components.BorderedCard
import com.example.ui.components.KpiTile
import com.example.ui.components.LineChartPoint
import com.example.ui.components.MuscleFigureBox
import com.example.ui.components.MuscleGroupTag
import com.example.ui.components.SectionLabel
import com.example.ui.components.StatusBadge
import com.example.ui.components.TrendLineChart
import com.example.ui.theme.LocalWorkoutPalette
import java.util.Locale

private data class ExerciseSummaryStats(
    val lastSession: Pair<String, LoggedExercise>,
    val personalRecordKg: Double
)

@Composable
fun ExerciseLibraryScreen(
    allExercises: List<ExerciseDefinition>,
    sessions: List<WorkoutSession>,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }

    val filterChips = listOf(
        "All", "Logged by me", "Chest", "Back", "Shoulders",
        "Legs", "Biceps", "Triceps", "Abs", "Cardio"
    )

    val statsByExercise = remember(sessions, allExercises) {
        val cardioNames = allExercises
            .filter { ExerciseCatalog.isCardioExercise(it.name, allExercises) }
            .map { it.name.lowercase(Locale.US) }
            .toSet()
        val lastMap = HashMap<String, Pair<String, LoggedExercise>>()
        val prMap = HashMap<String, Double>()
        for (session in sessions.sortedByDescending { it.dateIso }) {
            for (ex in session.exercises) {
                val key = ex.name.lowercase(Locale.US)
                if (key !in lastMap) {
                    lastMap[key] = session.dateIso to ex
                }
                if (key !in cardioNames) {
                    val maxW = ex.sets.maxOfOrNull { it.weightKg } ?: 0.0
                    val prev = prMap[key] ?: 0.0
                    if (maxW > prev) prMap[key] = maxW
                }
            }
        }
        lastMap.mapValues { (key, lastPair) ->
            ExerciseSummaryStats(
                lastSession = lastPair,
                personalRecordKg = prMap[key] ?: 0.0
            )
        }
    }

    val itemsList = remember(query, filter, allExercises, statsByExercise) {
        val q = query.trim().lowercase()
        allExercises.filter { def ->
            val fMatch = when (filter) {
                "All" -> true
                "Logged by me" -> def.name.lowercase(Locale.US) in statsByExercise
                else -> def.groups.any { it.equals(filter, ignoreCase = true) }
            }
            if (!fMatch) return@filter false
            if (q.isEmpty()) true
            else "${def.name} ${def.equipment} ${def.keywords}".lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search exercise name",
                        tint = palette.faint
                    )
                },
                placeholder = { Text("Enter an exercise name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface,
                    focusedBorderColor = palette.brand,
                    unfocusedBorderColor = palette.borderStrong,
                    focusedTextColor = palette.text,
                    unfocusedTextColor = palette.text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input")
            )
        }

        item {
            Text(
                text = "Every exercise shows when you last did it and what you lifted.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterChips.forEach { chip ->
                    val selected = filter == chip
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
                            .clickable { filter = chip }
                            .testTag("library_filter_${chip.lowercase().replace(' ', '_')}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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

        items(itemsList, key = { it.name }) { def ->
            val stats = statsByExercise[def.name.lowercase(Locale.US)]
            val last = stats?.lastSession
            val pr = stats?.personalRecordKg ?: 0.0
            val isCardio = ExerciseCatalog.isCardioExercise(def.name, allExercises)
            val lastBest = last?.second?.let { WorkoutMath.bestSet(it) }
            val primaryMetric = if (isCardio) {
                when {
                    pr > 0.0 -> "${WorkoutMath.formatKg(pr)} km"
                    lastBest != null && lastBest.reps > 0 -> "${lastBest.reps} min"
                    else -> "—"
                }
            } else {
                if (pr > 0.0) "${WorkoutMath.formatKg(pr)} kg" else "—"
            }
            BorderedCard(
                onClick = { onOpenDetail(def.name) },
                modifier = Modifier.testTag("library_item_${def.name.lowercase().replace(' ', '_')}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MuscleFigureBox(
                        regions = def.regions,
                        width = 38.dp,
                        height = 50.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = def.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${def.equipment} · ${def.groups.take(2).joinToString(", ")}",
                            fontSize = 12.sp,
                            color = palette.muted
                        )
                        val statusText = if (last != null) {
                            val prPart = if (primaryMetric != "—") "PR $primaryMetric · " else ""
                            "${prPart}Last done ${WorkoutMath.formatAgo(last.first)}"
                        } else {
                            "Not logged yet"
                        }
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            fontWeight = if (last != null) FontWeight.Medium else FontWeight.Normal,
                            color = if (last != null) palette.brandInk else palette.faint
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    exerciseName: String,
    allExercises: List<ExerciseDefinition>,
    historyRows: List<Pair<String, LoggedExercise>>,
    personalRecordKg: Double,
    hasActiveDraft: Boolean,
    onAddOrStartWithExercise: (String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val def = ExerciseCatalog.findByName(exerciseName, allExercises) ?: ExerciseDefinition(
        name = exerciseName,
        groups = listOf("Push"),
        equipment = "Custom",
        regions = listOf("chest"),
        keywords = "Custom exercise you added"
    )
    val isCardio = ExerciseCatalog.isCardioExercise(def.name, allExercises)
    val latest = historyRows.firstOrNull()
    val earliest = historyRows.lastOrNull()
    val latestBest = latest?.second?.let { WorkoutMath.bestSet(it) }
    val earliestBest = earliest?.second?.let { WorkoutMath.bestSet(it) }
    val gain1Rm = if (!isCardio && latestBest != null && earliestBest != null && historyRows.size > 1) {
        WorkoutMath.epley1Rm(latestBest.reps, latestBest.weightKg) -
            WorkoutMath.epley1Rm(earliestBest.reps, earliestBest.weightKg)
    } else 0.0

    val chartPoints = historyRows.reversed().mapNotNull { (dateIso, ex) ->
        val best = WorkoutMath.bestSet(ex) ?: return@mapNotNull null
        if (isCardio) {
            when {
                best.weightKg > 0.0 -> LineChartPoint(
                    label = WorkoutMath.formatDayMonth(dateIso),
                    value = best.weightKg.toFloat(),
                    unit = " km"
                )
                best.reps > 0 -> LineChartPoint(
                    label = WorkoutMath.formatDayMonth(dateIso),
                    value = best.reps.toFloat(),
                    unit = " min"
                )
                else -> null
            }
        } else {
            LineChartPoint(
                label = WorkoutMath.formatDayMonth(dateIso),
                value = best.weightKg.toFloat(),
                unit = " kg"
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BorderedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MuscleFigureBox(
                        regions = def.regions,
                        width = 78.dp,
                        height = 104.dp
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = def.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.text
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            StatusBadge(
                                label = def.equipment.uppercase(),
                                bg = palette.subtle,
                                fg = palette.text2
                            )
                            def.groups.take(2).forEach { MuscleGroupTag(it) }
                        }
                        if (def.instructions.isEmpty() && def.keywords.isNotBlank()) {
                            Text(
                                text = def.keywords.replaceFirstChar { it.uppercase() } + ".",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted
                            )
                        }
                    }
                }
            }
        }

        if (def.instructions.isNotEmpty()) {
            item {
                BorderedCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionLabel("How to perform")
                        def.instructions.forEachIndexed { idx, step ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.brandInk
                                )
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.text2
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        label = "Last done",
                        value = latest?.let { WorkoutMath.formatDayMonth(it.first) } ?: "—",
                        sub = latest?.let { WorkoutMath.formatAgo(it.first) } ?: "no record",
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        label = if (isCardio) "Last cardio" else "Last weight",
                        value = latestBest?.let {
                            if (isCardio) {
                                when {
                                    it.weightKg > 0.0 -> "${WorkoutMath.formatKg(it.weightKg)} km"
                                    it.reps > 0 -> "${it.reps} min"
                                    else -> "Logged"
                                }
                            } else {
                                if (it.weightKg > 0.0) "${WorkoutMath.formatKg(it.weightKg)} kg" else "Bodyweight"
                            }
                        } ?: "—",
                        sub = latest?.let { WorkoutMath.formatSetSummary(it.second, allExercises) } ?: "",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiTile(
                        label = if (isCardio) "Best distance" else "Best weight",
                        value = if (isCardio) {
                            if (personalRecordKg > 0.0) "${WorkoutMath.formatKg(personalRecordKg)} km" else "—"
                        } else {
                            if (personalRecordKg > 0.0) "${WorkoutMath.formatKg(personalRecordKg)} kg" else "—"
                        },
                        sub = "personal record",
                        subColor = palette.orangeInk,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        label = "Times done",
                        value = "${historyRows.size}×",
                        sub = if (gain1Rm != 0.0) {
                            "${if (gain1Rm > 0) "+" else ""}${String.format(Locale.US, "%.1f", gain1Rm)} kg est. 1RM"
                        } else "—",
                        subColor = if (gain1Rm > 0) palette.greenInk else palette.muted,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (historyRows.isEmpty()) {
            item {
                BorderedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "You haven't logged this yet.",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.text
                        )
                        Text(
                            text = "Add it to a workout and it starts building history.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }
                }
            }
        } else {
            if (chartPoints.isNotEmpty()) {
                item {
                    BorderedCard {
                        Column(modifier = Modifier.padding(14.dp)) {
                            SectionLabel(
                                text = if (isCardio) "Distance / duration over time" else "Top-set weight over time",
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            TrendLineChart(points = chartPoints, chartHeight = 80.dp)
                        }
                    }
                }
            }

            item {
                BorderedCard {
                    Column {
                        SectionLabel(
                            text = "Every time you did it",
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.appBg)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.border)
                        )
                        historyRows.forEach { (dateIso, ex) ->
                            val rpeColor = when (ex.rpe) {
                                RpeRating.EASY -> palette.greenInk
                                RpeRating.RIGHT -> palette.brandInk
                                RpeRating.HARD -> palette.redInk
                                null -> palette.faint
                            }
                            val vol = WorkoutMath.exerciseVolume(ex, allExercises)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${WorkoutMath.formatDayMonth(dateIso)} · ${WorkoutMath.formatAgo(dateIso)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.text
                                    )
                                    Text(
                                        text = ex.rpe?.label ?: "—",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = rpeColor
                                    )
                                }
                                Text(
                                    text = WorkoutMath.formatSetSummary(ex, allExercises) +
                                        if (vol > 0.0) " · ${WorkoutMath.formatVolume(vol)} total" else "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = palette.text2
                                )
                                if (ex.note.isNotBlank()) {
                                    Text(
                                        text = "Notes: ${ex.note}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.muted
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
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onAddOrStartWithExercise(def.name, def.groups.take(1)) },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.brand),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("detail_add_exercise_button")
            ) {
                Text(
                    text = if (hasActiveDraft) "Add to today's workout" else "Start a workout with this",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
