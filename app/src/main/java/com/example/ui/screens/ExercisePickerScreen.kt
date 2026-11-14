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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.AiExerciseSuggestion
import com.example.domain.AiLookupState
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.LoggedExercise
import com.example.domain.WorkoutMath
import com.example.ui.components.BorderedCard
import com.example.ui.components.MuscleFigureBox
import com.example.ui.components.SectionLabel
import com.example.ui.theme.LocalWorkoutPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePickerScreen(
    initialTab: Int = 0,
    draftGroups: List<String>,
    draftExerciseNames: Set<String>,
    allExercises: List<ExerciseDefinition>,
    suggestedExercises: List<Pair<ExerciseDefinition, Int>>,
    aiLookupState: AiLookupState,
    lastSessionOf: (String) -> Pair<String, LoggedExercise>?,
    onAddExercise: (String) -> Unit,
    onAddCustomExercise: (name: String, groups: List<String>, equipment: String) -> Unit,
    onAskGeminiAi: (String) -> Unit,
    onAddAiSuggestion: (AiExerciseSuggestion) -> Unit,
    onResetAiLookup: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    var selectedTab by remember(initialTab) {
        mutableIntStateOf(if (initialTab >= 2) 1 else 0)
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var describeQuery by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Exercises", "Describe it")
    val filterOptions = listOf(
        "All", "Chest", "Back", "Shoulders", "Legs", "Abs", "Cardio",
        "Dumbbell", "Machine", "Bar", "Cable", "Bodyweight"
    )
    val describeExamples = listOf(
        "pull a wide bar down while seated",
        "push a sled with my feet",
        "squeeze pads together with elbows bent"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = palette.surface,
            contentColor = palette.brand,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = palette.brand
                    )
                }
            }
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selectedTab == idx) palette.brand else palette.muted
                        )
                    },
                    modifier = Modifier.testTag("picker_tab_${title.lowercase().replace(' ', '_')}")
                )
            }
        }

        val searchResults = remember(searchQuery, selectedFilter, allExercises) {
            val q = searchQuery.trim().lowercase()
            allExercises.filter { def ->
                val filterMatch = selectedFilter == "All" ||
                    def.groups.any { it.equals(selectedFilter, ignoreCase = true) } ||
                    def.equipment.equals(selectedFilter, ignoreCase = true)
                if (!filterMatch) return@filter false
                if (q.isEmpty()) true
                else {
                    val hay = "${def.name} ${def.equipment} ${def.groups.joinToString(" ")} ${def.keywords}".lowercase()
                    hay.contains(q)
                }
            }
        }

        val offlineMatches = remember(describeQuery, allExercises) {
            ExerciseCatalog.matchOffline(describeQuery, allExercises)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search exercises",
                                        tint = palette.faint
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Clear search",
                                                tint = palette.faint
                                            )
                                        }
                                    }
                                },
                                placeholder = { Text("Search ${allExercises.size} exercises") },
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
                                    .testTag("picker_search_input")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filterOptions.forEach { f ->
                                    val selected = selectedFilter == f
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (selected) palette.brandSoft else palette.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) palette.brand else palette.borderStrong
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 38.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { selectedFilter = f }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = f,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (selected) palette.brandInk else palette.text2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val isSearchingOrFiltering = searchQuery.isNotBlank() || selectedFilter != "All"
                    if (!isSearchingOrFiltering) {
                        item {
                            SectionLabel("Usual for ${draftGroups.joinToString(" + ").ifEmpty { "you" }}")
                        }

                        items(suggestedExercises, key = { "sug_${it.first.name}" }) { (def, freq) ->
                            val last = lastSessionOf(def.name)
                            val why = if (freq > 0) {
                                "You do this most on ${def.groups.firstOrNull() ?: "this"} days · ${freq}×"
                            } else null
                            PickerExerciseRow(
                                def = def,
                                last = last,
                                why = why,
                                allExercises = allExercises,
                                alreadyAdded = draftExerciseNames.any { it.equals(def.name, ignoreCase = true) },
                                onOpenDetail = { onOpenDetail(def.name) },
                                onAdd = { onAddExercise(def.name) }
                            )
                        }
                    } else {
                        item {
                            SectionLabel(
                                "${searchResults.size} match${if (searchResults.size == 1) "" else "es"}"
                            )
                        }

                        if (searchResults.isEmpty()) {
                            item {
                                EmptyMatchCard(
                                    onCustomClick = { showCustomDialog = true }
                                )
                            }
                        } else {
                            items(searchResults, key = { "res_${it.name}" }) { def ->
                                PickerExerciseRow(
                                    def = def,
                                    last = lastSessionOf(def.name),
                                    why = null,
                                    allExercises = allExercises,
                                    alreadyAdded = draftExerciseNames.any { it.equals(def.name, ignoreCase = true) },
                                    onOpenDetail = { onOpenDetail(def.name) },
                                    onAdd = { onAddExercise(def.name) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        BorderedCard {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Not sure what it's called?",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.text
                                    )
                                    Text(
                                        text = "Describe the machine, the movement, or where you feel it. Offline matching runs instantly; tap Ask Gemini AI when online.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.muted
                                    )
                                }

                                OutlinedTextField(
                                    value = describeQuery,
                                    onValueChange = {
                                        describeQuery = it
                                        if (aiLookupState !is AiLookupState.Idle) onResetAiLookup()
                                    },
                                    placeholder = { Text("e.g. sit down, pull a wide bar down to my chest") },
                                    minLines = 3,
                                    maxLines = 4,
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
                                        .testTag("picker_describe_input")
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    describeExamples.forEach { example ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = palette.subtle,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    describeQuery = example
                                                    onResetAiLookup()
                                                }
                                        ) {
                                            Text(
                                                text = "\"${example.take(26)}…\"",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = palette.text2,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(palette.greenMid)
                                        )
                                        Text(
                                            text = "On-device match · works offline",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.muted
                                        )
                                    }

                                    Button(
                                        onClick = { onAskGeminiAi(describeQuery) },
                                        enabled = describeQuery.trim().length >= 3 && aiLookupState !is AiLookupState.Loading,
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = palette.brandSoft,
                                            contentColor = palette.brandInk
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 44.dp)
                                            .testTag("picker_ask_gemini_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Ask Gemini AI",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Gemini AI Result Section
                    when (aiLookupState) {
                        is AiLookupState.Loading -> {
                            item {
                                BorderedCard(
                                    backgroundColor = palette.brandSoft,
                                    borderColor = palette.brandBd
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = palette.brand
                                        )
                                        Text(
                                            text = "Asking Gemini AI to identify the exercise…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = palette.brandInk
                                        )
                                    }
                                }
                            }
                        }
                        is AiLookupState.Error -> {
                            item {
                                BorderedCard(
                                    backgroundColor = palette.redSoft,
                                    borderColor = palette.redInk.copy(alpha = 0.5f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Gemini AI unavailable",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.redInk
                                        )
                                        Text(
                                            text = aiLookupState.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.redInk
                                        )
                                    }
                                }
                            }
                        }
                        is AiLookupState.Success -> {
                            item { SectionLabel("Gemini AI suggestions") }
                            items(aiLookupState.suggestions, key = { "ai_${it.exerciseName}" }) { sug ->
                                val known = ExerciseCatalog.findByName(sug.exerciseName, allExercises)
                                BorderedCard(
                                    borderColor = palette.brandBd,
                                    backgroundColor = palette.surface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        MuscleFigureBox(
                                            regions = known?.regions ?: listOf("chest"),
                                            width = 38.dp,
                                            height = 50.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sug.exerciseName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = palette.text
                                            )
                                            Text(
                                                text = "${sug.equipment} · ${sug.muscleGroups.joinToString(", ")}",
                                                fontSize = 12.sp,
                                                color = palette.muted
                                            )
                                            if (sug.reason.isNotBlank()) {
                                                Text(
                                                    text = sug.reason,
                                                    fontSize = 12.sp,
                                                    color = palette.brandInk
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = palette.brandSoft,
                                            border = BorderStroke(1.dp, palette.brandBd),
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onAddAiSuggestion(sug) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Add,
                                                    contentDescription = "Add ${sug.exerciseName}",
                                                    tint = palette.brandInk
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        AiLookupState.Idle -> Unit
                    }

                    // On-device Offline Matches
                    item {
                        SectionLabel(
                            if (describeQuery.trim().length < 3) "Start typing to match offline"
                            else "On-device matches"
                        )
                    }

                    if (describeQuery.trim().length >= 3 && offlineMatches.isEmpty()) {
                        item {
                            EmptyMatchCard(
                                onCustomClick = { showCustomDialog = true }
                            )
                        }
                    } else {
                        items(offlineMatches, key = { "off_${it.exercise.name}" }) { match ->
                            PickerExerciseRow(
                                def = match.exercise,
                                last = lastSessionOf(match.exercise.name),
                                why = "${match.matchLabel} · ${match.exercise.groups.take(2).joinToString(", ")}",
                                allExercises = allExercises,
                                alreadyAdded = draftExerciseNames.any {
                                    it.equals(match.exercise.name, ignoreCase = true)
                                },
                                onOpenDetail = { onOpenDetail(match.exercise.name) },
                                onAdd = { onAddExercise(match.exercise.name) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomExerciseDialog(
            initialName = (searchQuery.ifBlank { describeQuery }).take(40),
            defaultGroups = draftGroups,
            onDismiss = { showCustomDialog = false },
            onSave = { name, groups, eq ->
                showCustomDialog = false
                onAddCustomExercise(name, groups, eq)
            }
        )
    }
}

@Composable
private fun PickerExerciseRow(
    def: ExerciseDefinition,
    last: Pair<String, LoggedExercise>?,
    why: String?,
    allExercises: List<ExerciseDefinition>,
    alreadyAdded: Boolean,
    onOpenDetail: () -> Unit,
    onAdd: () -> Unit
) {
    val palette = LocalWorkoutPalette.current
    BorderedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MuscleFigureBox(
                regions = def.regions,
                width = 38.dp,
                height = 50.dp
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDetail)
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = def.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (last != null) {
                        "${def.equipment} · ${WorkoutMath.formatAgo(last.first)} · ${WorkoutMath.formatSetSummary(last.second, allExercises)}"
                    } else {
                        "${def.equipment} · not logged yet"
                    },
                    fontSize = 12.sp,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!why.isNullOrBlank()) {
                    Text(
                        text = why,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.greenInk
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (alreadyAdded) palette.subtle else palette.brandSoft,
                border = BorderStroke(1.dp, if (alreadyAdded) palette.border else palette.brandBd),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onAdd)
                    .testTag("picker_add_${def.name.lowercase().replace(' ', '_')}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add ${def.name}",
                        tint = if (alreadyAdded) palette.faint else palette.brandInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMatchCard(
    onCustomClick: () -> Unit
) {
    val palette = LocalWorkoutPalette.current
    BorderedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No match yet.",
                style = MaterialTheme.typography.titleSmall,
                color = palette.text
            )
            Text(
                text = "Name the equipment, or save it under your own name.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted
            )
            OutlinedButton(
                onClick = onCustomClick,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, palette.brandBd),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .heightIn(min = 48.dp)
                    .testTag("picker_add_custom_button")
            ) {
                Text("Add as custom exercise", color = palette.brandInk)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExerciseDialog(
    initialName: String,
    defaultGroups: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, groups: List<String>, equipment: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var equipment by remember { mutableStateOf("Machine") }
    var selectedGroups by remember { mutableStateOf(defaultGroups.ifEmpty { listOf("Push") }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Equipment", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseCatalog.EQUIPMENT_OPTIONS.forEach { eq ->
                        val sel = equipment == eq
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            color = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.clickable { equipment = eq }
                        ) {
                            Text(eq, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                        }
                    }
                }
                Text("Muscle group", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseCatalog.MUSCLE_GROUPS.forEach { grp ->
                        val sel = grp in selectedGroups
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            color = if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.clickable {
                                selectedGroups = if (sel && selectedGroups.size > 1) selectedGroups - grp else selectedGroups + grp
                            }
                        ) {
                            Text(grp, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, selectedGroups.distinct(), equipment) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Add & save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
