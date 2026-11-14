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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ExerciseCatalog
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.components.BorderedCard
import com.example.ui.components.SectionLabel
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberDatePickerLauncher
import com.example.ui.theme.LocalWorkoutPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewWorkoutScreen(
    allSessions: List<WorkoutSession>,
    sessionsForGroups: (List<String>) -> List<WorkoutSession>,
    initialDateIso: String = WorkoutMath.todayIso(),
    onContinue: (dateIso: String, groups: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val todayIso = WorkoutMath.todayIso()
    var dateIso by remember { mutableStateOf(initialDateIso) }
    var selectedGroups by remember { mutableStateOf<List<String>>(emptyList()) }

    val openDatePicker = rememberDatePickerLauncher(dateIso) { picked ->
        dateIso = picked
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BorderedCard(
                onClick = openDatePicker,
                modifier = Modifier.testTag("wizard_date_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Change workout date",
                        tint = palette.muted,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Workout date")
                        Text(
                            text = WorkoutMath.formatLong(dateIso),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = palette.text
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, palette.borderStrong),
                        color = palette.surface
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = palette.brandInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Step 1 of 2")
                Text(
                    text = "What are you training today?",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text
                )
                Text(
                    text = "Pick one or more. Suggestions and history are filtered to what you choose.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Your split")
                ExerciseCatalog.SPLIT_PRESETS.forEach { preset ->
                    val isSelected = preset.groups.all { it in selectedGroups }
                    val matching = sessionsForGroups(preset.groups)
                    val lastSession = matching.maxByOrNull { it.dateIso }
                    val daysAgo = lastSession?.let { WorkoutMath.daysBetween(it.dateIso, todayIso) }
                    val isDue = daysAgo != null && daysAgo >= 4L

                    BorderedCard(
                        backgroundColor = if (isSelected) palette.brandSoft else palette.surface,
                        borderColor = if (isSelected) palette.brand else palette.border,
                        onClick = {
                            selectedGroups = if (isSelected) emptyList() else preset.groups
                        },
                        modifier = Modifier.testTag("wizard_preset_${preset.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) palette.brand else palette.surface)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) palette.brand else palette.borderStrong,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${preset.name} day",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.text
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.muted
                                )
                            }
                            StatusBadge(
                                label = lastSession?.let { WorkoutMath.formatAgo(it.dateIso, todayIso) } ?: "never",
                                bg = if (isDue) palette.orangeSoft else palette.subtle,
                                fg = if (isDue) palette.orangeInk else palette.muted
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Or pick muscle groups")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExerciseCatalog.MUSCLE_GROUPS.forEach { group ->
                        val isSelected = group in selectedGroups
                        val gpColor = palette.groupColor(group)
                        val matching = sessionsForGroups(listOf(group))
                        val daysAgo = matching.maxByOrNull { it.dateIso }
                            ?.let { WorkoutMath.daysBetween(it.dateIso, todayIso) }
                        val tag = if (daysAgo == null) "· new" else "· ${daysAgo}d"

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) gpColor.bg else palette.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) gpColor.fg else palette.borderStrong
                            ),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedGroups = if (isSelected) {
                                        selectedGroups - group
                                    } else {
                                        selectedGroups + group
                                    }
                                }
                                .testTag("wizard_group_${group.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = gpColor.fg,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = group,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) gpColor.fg else palette.text2
                                )
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = (if (isSelected) gpColor.fg else palette.text2).copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Surface(
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (selectedGroups.isEmpty()) "Select at least one group"
                    else selectedGroups.joinToString(" + "),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = palette.muted,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onContinue(dateIso, selectedGroups) },
                    enabled = selectedGroups.isNotEmpty(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.brand,
                        disabledContainerColor = palette.border,
                        contentColor = Color.White,
                        disabledContentColor = palette.faint
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("wizard_continue_button")
                ) {
                    Text(
                        text = "Continue",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
