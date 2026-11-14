package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ActiveDraft
import com.example.domain.DraftExercise
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.LoggedExercise
import com.example.domain.RpeRating
import com.example.domain.WorkoutMath
import com.example.ui.components.BorderedCard
import com.example.ui.components.MuscleFigureBox
import com.example.ui.components.SectionLabel
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberDatePickerLauncher
import com.example.ui.theme.LocalWorkoutPalette

@Composable
fun LiveSessionScreen(
    draft: ActiveDraft,
    bodyWeightKg: Double,
    allExercises: List<ExerciseDefinition>,
    suggestedExercises: List<Pair<ExerciseDefinition, Int>>,
    lastSessionOf: (String) -> Pair<String, LoggedExercise>?,
    personalRecordKg: (String) -> Double,
    onOpenPrevious: () -> Unit,
    onOpenLookup: () -> Unit,
    onOpenAddPicker: () -> Unit,
    onAddSuggested: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onAddSet: (String) -> Unit,
    onRemoveLastSet: (String) -> Unit,
    onUpdateReps: (String, Int, String) -> Unit,
    onUpdateWeight: (String, Int, String) -> Unit,
    onToggleSetDone: (String, Int) -> Unit,
    onSelectRpe: (String, RpeRating) -> Unit,
    onUpdateExerciseNote: (String, String) -> Unit,
    onUpdateSessionNote: (String) -> Unit,
    onUpdateDate: (String) -> Unit = {},
    onUpdateManualDuration: (String) -> Unit = {},
    onDiscard: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val isEditing = draft.editingSessionIds.isNotEmpty()
    var quickSearch by remember { mutableStateOf("") }

    val totalVol = WorkoutMath.draftVolume(draft, allExercises)
    val totalSets = WorkoutMath.draftSetCount(draft)
    val timerStr = WorkoutMath.formatTimer(draft.elapsedSeconds)
    val estKcal = WorkoutMath.estimateDraftCalories(draft, bodyWeightKg, allExercises)

    val draftNames = draft.exercises.map { it.name.lowercase() }.toSet()
    val filteredSuggested = suggestedExercises
        .filter { it.first.name.lowercase() !in draftNames }
        .take(5)

    val searchMatches = remember(quickSearch, allExercises, draftNames) {
        val q = quickSearch.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else allExercises.filter { def ->
            def.name.lowercase() !in draftNames &&
                "${def.name} ${def.equipment} ${def.groups.joinToString(" ")} ${def.keywords}"
                    .lowercase()
                    .contains(q)
        }.take(10)
    }

    val openDatePicker = rememberDatePickerLauncher(draft.dateIso) { picked ->
        onUpdateDate(picked)
    }

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
            if (isEditing) {
                item {
                    BorderedCard(
                        backgroundColor = palette.brandSoft,
                        borderColor = palette.brandBd
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SectionLabel("Editing workout in history")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.surface,
                                    border = BorderStroke(1.dp, palette.borderStrong),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(onClick = openDatePicker)
                                        .testTag("edit_session_date_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarToday,
                                            contentDescription = "Change date",
                                            tint = palette.brand,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = WorkoutMath.formatShort(draft.dateIso),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.text
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = (draft.manualDurationMinutes ?: 0).let { if (it > 0) it.toString() else "" },
                                    onValueChange = onUpdateManualDuration,
                                    label = { Text("Duration (min)") },
                                    placeholder = { Text("Optional") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                        .testTag("edit_session_duration_input")
                                )
                            }
                        }
                    }
                }
            }

            // Top Previous / All exercises bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.brandBd),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenPrevious)
                            .testTag("session_previous_workouts_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = palette.brandInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Previous workouts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.brandInk
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = palette.surface,
                        border = BorderStroke(1.dp, palette.borderStrong),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenLookup)
                            .testTag("session_lookup_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = palette.text2,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Browse / AI",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.text2
                            )
                        }
                    }
                }
            }

            // Live KPI Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiveStatTile("Volume", WorkoutMath.formatVolume(totalVol), Modifier.weight(1f))
                    LiveStatTile("Sets", "$totalSets", Modifier.weight(1f))
                    LiveStatTile(
                        label = if (isEditing) "Est. kcal" else "Elapsed",
                        value = if (isEditing) "~$estKcal" else timerStr,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Immediate Inline Search + Usual Exercises
            item {
                BorderedCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quickSearch,
                            onValueChange = { quickSearch = it },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search exercises",
                                    tint = palette.faint
                                )
                            },
                            trailingIcon = {
                                if (quickSearch.isNotEmpty()) {
                                    IconButton(onClick = { quickSearch = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear search",
                                            tint = palette.faint
                                        )
                                    }
                                }
                            },
                            placeholder = { Text("Search ${allExercises.size} exercises to add…") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.appBg,
                                unfocusedContainerColor = palette.appBg,
                                focusedBorderColor = palette.brand,
                                unfocusedBorderColor = palette.borderStrong,
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("session_inline_search_input")
                        )

                        if (quickSearch.isNotBlank()) {
                            SectionLabel("${searchMatches.size} match${if (searchMatches.size == 1) "" else "es"}")
                            if (searchMatches.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "No matching exercises found.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.muted
                                    )
                                    TextButton(
                                        onClick = onOpenAddPicker,
                                        modifier = Modifier.heightIn(min = 44.dp)
                                    ) {
                                        Text("Open full picker or create custom", color = palette.brand)
                                    }
                                }
                            } else {
                                searchMatches.forEach { def ->
                                    QuickAddExerciseRow(
                                        def = def,
                                        last = lastSessionOf(def.name),
                                        allExercises = allExercises,
                                        onAdd = {
                                            onAddSuggested(def.name)
                                            quickSearch = ""
                                        }
                                    )
                                }
                            }
                        } else if (filteredSuggested.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                SectionLabel(
                                    "Usual for ${draft.groups.joinToString(" + ").ifEmpty { "today" }}"
                                )
                                Text(
                                    text = "Ranked by how often you train them. Type above to search any exercise.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.muted
                                )
                            }
                            filteredSuggested.forEach { (def, _) ->
                                QuickAddExerciseRow(
                                    def = def,
                                    last = lastSessionOf(def.name),
                                    allExercises = allExercises,
                                    onAdd = { onAddSuggested(def.name) }
                                )
                            }
                        }
                    }
                }
            }

            // Logged Exercise Cards
            items(draft.exercises, key = { it.localId }) { exercise ->
                val def = ExerciseCatalog.findByName(exercise.name, allExercises)
                val last = lastSessionOf(exercise.name)
                val prKg = personalRecordKg(exercise.name)

                LiveExerciseCard(
                    exercise = exercise,
                    definition = def,
                    allExercises = allExercises,
                    lastSession = last,
                    historicalPrKg = prKg,
                    onRemove = { onRemoveExercise(exercise.localId) },
                    onAddSet = { onAddSet(exercise.localId) },
                    onRemoveLastSet = { onRemoveLastSet(exercise.localId) },
                    onUpdateReps = { idx, valStr -> onUpdateReps(exercise.localId, idx, valStr) },
                    onUpdateWeight = { idx, valStr -> onUpdateWeight(exercise.localId, idx, valStr) },
                    onToggleDone = { idx -> onToggleSetDone(exercise.localId, idx) },
                    onSelectRpe = { rpe -> onSelectRpe(exercise.localId, rpe) },
                    onUpdateNote = { note -> onUpdateExerciseNote(exercise.localId, note) }
                )
            }

            // Add Exercise Picker Button (custom / AI / full filter)
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.brandBd),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenAddPicker)
                        .testTag("session_add_exercise_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add exercise",
                            tint = palette.brand,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "More exercises / Describe to AI",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.brand
                        )
                    }
                }
            }

            // Session Notes
            item {
                BorderedCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionLabel("Session notes · optional")
                        OutlinedTextField(
                            value = draft.sessionNote,
                            onValueChange = onUpdateSessionNote,
                            placeholder = { Text("Slept 6h, gym crowded, dropped the last set…") },
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = palette.appBg,
                                unfocusedContainerColor = palette.appBg,
                                focusedBorderColor = palette.brand,
                                unfocusedBorderColor = palette.border,
                                focusedTextColor = palette.text,
                                unfocusedTextColor = palette.text
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("session_notes_input")
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(6.dp)) }
        }

        // Sticky Bottom Bar
        Surface(
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (totalVol > 0.0) {
                            "${WorkoutMath.formatVolume(totalVol)} · $totalSets sets"
                        } else {
                            "$totalSets sets"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text
                    )
                    Text(
                        text = if (isEditing) "~$estKcal kcal est." else "~$estKcal kcal · $timerStr",
                        fontSize = 12.sp,
                        color = palette.muted
                    )
                }
                TextButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("session_discard_button")
                ) {
                    Text(if (isEditing) "Cancel" else "Discard", color = palette.faint)
                }
                Button(
                    onClick = onFinish,
                    enabled = draft.exercises.isNotEmpty(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.brand,
                        disabledContainerColor = palette.border,
                        contentColor = Color.White,
                        disabledContentColor = palette.faint
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("session_finish_button")
                ) {
                    Text(
                        text = if (isEditing) "Save changes" else "Finish",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddExerciseRow(
    def: ExerciseDefinition,
    last: Pair<String, LoggedExercise>?,
    allExercises: List<ExerciseDefinition>,
    onAdd: () -> Unit
) {
    val palette = LocalWorkoutPalette.current
    val meta = if (last != null) {
        "${def.equipment} · ${WorkoutMath.formatAgo(last.first)} · ${WorkoutMath.formatSetSummary(last.second, allExercises)}"
    } else {
        "${def.equipment} · ${def.groups.take(2).joinToString(", ")}"
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = palette.appBg,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onAdd)
            .testTag("session_suggest_${def.name.lowercase().replace(' ', '_')}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MuscleFigureBox(
                regions = def.regions,
                width = 34.dp,
                height = 44.dp,
                containerColor = palette.surface
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = def.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = meta,
                    fontSize = 12.sp,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add ${def.name}",
                tint = palette.brand,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LiveStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    BorderedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            SectionLabel(label)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = palette.text,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveExerciseCard(
    exercise: DraftExercise,
    definition: ExerciseDefinition?,
    allExercises: List<ExerciseDefinition>,
    lastSession: Pair<String, LoggedExercise>?,
    historicalPrKg: Double,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveLastSet: () -> Unit,
    onUpdateReps: (Int, String) -> Unit,
    onUpdateWeight: (Int, String) -> Unit,
    onToggleDone: (Int) -> Unit,
    onSelectRpe: (RpeRating) -> Unit,
    onUpdateNote: (String) -> Unit
) {
    val palette = LocalWorkoutPalette.current
    val primaryGroup = definition?.groups?.firstOrNull() ?: "Push"
    val groupColor = palette.groupColor(primaryGroup)
    val isCardio = ExerciseCatalog.isCardioExercise(exercise.name, allExercises)
    val isBodyweight = !isCardio && definition?.isBodyweight == true
    val hasPrInDraft = !isCardio && exercise.sets.any {
        historicalPrKg > 0.0 && (it.weightText.toDoubleOrNull() ?: 0.0) > historicalPrKg
    }

    BorderedCard {
        Column {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                MuscleFigureBox(
                    regions = definition?.regions ?: listOf("chest"),
                    width = 46.dp,
                    height = 60.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = palette.text
                    )
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusBadge(
                            label = exercise.equipment.uppercase(),
                            bg = palette.subtle,
                            fg = palette.text2
                        )
                        StatusBadge(
                            label = primaryGroup.uppercase(),
                            bg = groupColor.bg,
                            fg = groupColor.fg
                        )
                        if (hasPrInDraft) {
                            StatusBadge(
                                label = "PR",
                                bg = palette.orangeSoft,
                                fg = palette.orangeInk
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = if (lastSession != null) {
                            "Last time · ${WorkoutMath.formatShort(lastSession.first)} · ${WorkoutMath.formatSetSummary(lastSession.second, allExercises)}"
                        } else {
                            "First time logging this"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove ${exercise.name}",
                        tint = palette.faint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.subtle)
            )

            // Sets Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionLabel("Set", Modifier.width(30.dp))
                SectionLabel(if (isCardio) "Time (min)" else "Reps", Modifier.weight(1f))
                SectionLabel(
                    text = when {
                        isCardio -> "Distance (km)"
                        isBodyweight -> "Load"
                        else -> "Weight kg"
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(48.dp))
            }

            // Sets Rows
            exercise.sets.forEachIndexed { idx, set ->
                val isPrSet = !isCardio && historicalPrKg > 0.0 && (set.weightText.toDoubleOrNull() ?: 0.0) > historicalPrKg
                val fieldBg = if (set.isCompleted) palette.greenTint else palette.surface
                val fieldBd = if (set.isCompleted) palette.greenBd else palette.borderStrong

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${idx + 1}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (set.isCompleted) palette.greenInk else palette.faint,
                        modifier = Modifier.width(30.dp)
                    )

                    NumberCellInput(
                        value = set.repsText,
                        onValueChange = { onUpdateReps(idx, it) },
                        keyboardType = KeyboardType.Number,
                        bg = fieldBg,
                        border = fieldBd,
                        textColor = palette.text,
                        modifier = Modifier.weight(1f)
                    )

                    if (isBodyweight) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.appBg)
                                .border(1.dp, palette.border, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bodyweight",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = palette.faint
                            )
                        }
                    } else {
                        NumberCellInput(
                            value = set.weightText,
                            onValueChange = { onUpdateWeight(idx, it) },
                            keyboardType = KeyboardType.Decimal,
                            bg = fieldBg,
                            border = fieldBd,
                            textColor = palette.text,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (set.isCompleted) palette.green else palette.surface)
                                .border(
                                    1.dp,
                                    if (set.isCompleted) palette.green else palette.borderStrong,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { onToggleDone(idx) }
                                .testTag("set_done_${exercise.name.lowercase().replace(' ', '_')}_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = if (set.isCompleted) "Set completed" else "Mark set complete",
                                tint = if (set.isCompleted) Color.White else palette.borderStrong,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isPrSet) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.orange)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "PR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00164E)
                                )
                            }
                        }
                    }
                }
            }

            // Add Set (green outline) / Remove Set (red outline) Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = palette.greenTint,
                    border = BorderStroke(1.5.dp, palette.green),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onAddSet)
                        .testTag("add_set_${exercise.name.lowercase().replace(' ', '_')}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+ Add set",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.greenInk
                        )
                    }
                }
                val canRemove = exercise.sets.size > 1
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (canRemove) palette.redSoft.copy(alpha = 0.45f) else Color.Transparent,
                    border = BorderStroke(
                        1.5.dp,
                        if (canRemove) palette.redInk else palette.redInk.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = canRemove, onClick = onRemoveLastSet)
                        .testTag("remove_set_${exercise.name.lowercase().replace(' ', '_')}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "Remove set",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (canRemove) palette.redInk else palette.redInk.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Difficulty / RPE Rating (Color-coded Easy=Green, Just right=Blue, Hard=Red) + Overload Advice
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionLabel("How did it feel · optional")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RpeRating.entries.forEach { rating ->
                        val selected = exercise.rpe == rating
                        val (bg, borderColor, fg) = when (rating) {
                            RpeRating.EASY -> if (selected) {
                                Triple(palette.green, palette.green, Color.White)
                            } else {
                                Triple(palette.greenSoft.copy(alpha = 0.55f), palette.green.copy(alpha = 0.65f), palette.greenInk)
                            }
                            RpeRating.RIGHT -> if (selected) {
                                Triple(palette.brand, palette.brand, Color.White)
                            } else {
                                Triple(palette.brandSoft.copy(alpha = 0.55f), palette.brand.copy(alpha = 0.65f), palette.brandInk)
                            }
                            RpeRating.HARD -> if (selected) {
                                Triple(palette.redInk, palette.redInk, Color.White)
                            } else {
                                Triple(palette.redSoft.copy(alpha = 0.55f), palette.redInk.copy(alpha = 0.65f), palette.redInk)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = bg,
                            border = BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSelectRpe(rating) }
                                .testTag("rpe_${rating.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = rating.label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = fg
                                )
                            }
                        }
                    }
                }

                val firstWeight = exercise.sets.firstOrNull()?.weightText?.toDoubleOrNull() ?: 0.0
                val advice = when {
                    isCardio -> null
                    exercise.rpe == RpeRating.EASY && !isBodyweight ->
                        "Rated easy — next time start at ${WorkoutMath.formatKg(firstWeight + 2.5)} kg."
                    exercise.rpe == RpeRating.EASY && isBodyweight ->
                        "Rated easy — aim for +1–2 reps per set next time."
                    exercise.rpe == RpeRating.HARD ->
                        "Rated hard — hold this weight until reps come clean."
                    else -> null
                }
                if (advice != null) {
                    val advBg = if (exercise.rpe == RpeRating.EASY) palette.greenSoft else palette.redSoft
                    val advFg = if (exercise.rpe == RpeRating.EASY) palette.greenInk else palette.redInk
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(advBg)
                            .border(1.dp, advFg.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = advFg,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = advice,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = advFg
                        )
                    }
                }
            }

            // Per-exercise Notes
            OutlinedTextField(
                value = exercise.note,
                onValueChange = onUpdateNote,
                placeholder = { Text("Notes — grip, seat height, form cue…") },
                minLines = 1,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.appBg,
                    unfocusedContainerColor = palette.appBg,
                    focusedBorderColor = palette.brand,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.text,
                    unfocusedTextColor = palette.text
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun NumberCellInput(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    bg: Color,
    border: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(textColor),
            textStyle = TextStyle(
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                fontFeatureSettings = "tnum"
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        )
    }
}
