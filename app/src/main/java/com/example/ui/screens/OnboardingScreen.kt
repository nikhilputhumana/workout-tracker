package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ExerciseCatalog
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.ui.components.BorderedCard
import com.example.ui.components.rememberDatePickerLauncher
import com.example.ui.theme.LocalWorkoutPalette

@Composable
fun OnboardingScreen(
    initialProfile: UserProfile,
    initialNextFeeIso: String,
    startStep: Int = 1,
    isEditOnly: Boolean = false,
    onComplete: (profile: UserProfile, nextFeeIso: String) -> Unit,
    onImportLog: (profile: UserProfile, nextFeeIso: String) -> Unit = { _, _ -> },
    onCancelEdit: (() -> Unit)? = null
) {
    val palette = LocalWorkoutPalette.current
    var step by remember(startStep) { mutableIntStateOf(startStep) }
    var name by remember { mutableStateOf(initialProfile.name) }
    var ageText by remember { mutableStateOf(initialProfile.age.toString()) }
    var heightText by remember { mutableStateOf(initialProfile.heightCm.toString()) }
    var weightText by remember { mutableStateOf(WorkoutMath.formatKg(initialProfile.bodyWeightKg)) }
    var bfText by remember { mutableStateOf(initialProfile.bodyFatPercent?.let { WorkoutMath.formatKg(it) } ?: "") }
    var gender by remember { mutableStateOf(initialProfile.gender) }
    var level by remember { mutableStateOf(initialProfile.experienceLevel) }
    var split by remember {
        mutableStateOf(
            if (ExerciseCatalog.TRAINING_SPLITS.any { it.name == initialProfile.preferredSplit }) {
                initialProfile.preferredSplit
            } else {
                ExerciseCatalog.TRAINING_SPLITS.first().name
            }
        )
    }
    var nextFeeIso by remember {
        mutableStateOf(initialNextFeeIso.ifBlank { WorkoutMath.isoOffsetMonths(1) })
    }

    val openDatePicker = rememberDatePickerLauncher(nextFeeIso) { picked ->
        nextFeeIso = picked
    }

    fun buildProfile(): UserProfile = UserProfile(
        name = name.trim(),
        age = ageText.toIntOrNull()?.coerceIn(12, 100) ?: 26,
        gender = gender,
        heightCm = heightText.toIntOrNull()?.coerceIn(100, 240) ?: 175,
        bodyWeightKg = weightText.toDoubleOrNull()?.coerceIn(25.0, 300.0) ?: 74.5,
        bodyFatPercent = bfText.toDoubleOrNull()?.coerceIn(3.0, 65.0),
        experienceLevel = level,
        preferredSplit = split,
        weeklyTargetSessions = initialProfile.weeklyTargetSessions,
        onboardingComplete = true
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.appBg)
    ) {
        if (isEditOnly) {
            Surface(
                color = palette.surface,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onCancelEdit?.invoke() },
                        modifier = Modifier.testTag("edit_profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.text
                        )
                    }
                    Column {
                        Text(
                            text = "Edit profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.text
                        )
                        Text(
                            text = "Body metrics & training experience",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (!isEditOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (step >= idx + 1) palette.brand else palette.border)
                        )
                    }
                }
            }

            when (step) {
                1 -> {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.brand),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = "Workout Tracker logo",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Every set, in one place.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.text
                        )
                        Text(
                            text = "Private, offline workout log with JSON backup anytime.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.muted
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeatureRow(
                            icon = Icons.Filled.Schedule,
                            iconBg = palette.brandSoft,
                            iconTint = palette.brandInk,
                            title = "Previous numbers inline",
                            subtitle = "See last session's reps, weight, or distance as you log."
                        )
                        FeatureRow(
                            icon = Icons.Filled.ArrowUpward,
                            iconBg = palette.greenSoft,
                            iconTint = palette.greenInk,
                            title = "Overload prompts",
                            subtitle = "Mark a lift Easy and next session suggests +2.5 kg."
                        )
                        FeatureRow(
                            icon = Icons.Filled.Star,
                            iconBg = palette.orangeSoft,
                            iconTint = palette.orangeInk,
                            title = "Live PR badges",
                            subtitle = "New personal records are highlighted as you train."
                        )
                    }
                }

                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "About you",
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.text
                        )
                        Text(
                            text = "Used for calorie estimates and strength trends. Editable in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface,
                        focusedBorderColor = palette.brand,
                        unfocusedBorderColor = palette.borderStrong,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        placeholder = { Text("Your name") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Age") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_age_input")
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Height (cm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_height_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Body weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_weight_input")
                        )
                        OutlinedTextField(
                            value = bfText,
                            onValueChange = { bfText = it },
                            label = { Text("Body fat % (opt)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_bf_input")
                        )
                    }

                    ChoiceRow(
                        label = "Gender",
                        options = listOf("Male", "Female", "Other"),
                        selected = gender,
                        onSelect = { gender = it }
                    )

                    ChoiceRow(
                        label = "Experience",
                        options = listOf("Beginner", "Intermediate", "Advanced"),
                        selected = level,
                        onSelect = { level = it }
                    )
                }

                3 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Your split",
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.text
                        )
                        Text(
                            text = "Choose how you structure your training week. You can change this or train any muscle group anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExerciseCatalog.TRAINING_SPLITS.forEach { preset ->
                            val selected = split == preset.name
                            BorderedCard(
                                backgroundColor = if (selected) palette.brandSoft else palette.surface,
                                borderColor = if (selected) palette.brand else palette.border,
                                onClick = { split = preset.name },
                                modifier = Modifier.testTag("split_option_${preset.name.lowercase().replace(' ', '_')}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) palette.brandInk else palette.text
                                    )
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.muted
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Gym fee due date",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = palette.text2
                        )
                        BorderedCard(
                            borderColor = palette.borderStrong,
                            onClick = openDatePicker,
                            modifier = Modifier.testTag("onboarding_fee_date_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = "Select fee due date",
                                    tint = palette.muted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = WorkoutMath.formatLong(nextFeeIso),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.text
                                )
                            }
                        }
                    }

                    BorderedCard(
                        modifier = Modifier.testTag("onboarding_import_log_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Import workout log",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.text
                            )
                            Text(
                                text = "Bring in an existing Workout Tracker JSON file to restore your past sessions, sets, weights, and body-weight log immediately.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted
                            )
                            OutlinedButton(
                                onClick = { onImportLog(buildProfile(), nextFeeIso) },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, palette.brandBd),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("onboarding_import_log_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Upload,
                                    contentDescription = "Import workout log",
                                    tint = palette.brandInk,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Import JSON log",
                                    color = palette.brandInk,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = palette.surface,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditOnly) {
                    TextButton(
                        onClick = { onCancelEdit?.invoke() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("edit_profile_cancel_button")
                    ) {
                        Text("Cancel", color = palette.muted)
                    }
                    Button(
                        onClick = { onComplete(buildProfile(), nextFeeIso) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.brand),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("edit_profile_save_button")
                    ) {
                        Text("Save profile", color = Color.White)
                    }
                } else {
                    TextButton(
                        onClick = {
                            if (step > 1) step-- else onComplete(buildProfile(), nextFeeIso)
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("onboarding_skip_or_back_button")
                    ) {
                        Text(if (step > 1) "Back" else "Skip", color = palette.muted)
                    }
                    Button(
                        onClick = {
                            if (step < 3) step++
                            else onComplete(buildProfile(), nextFeeIso)
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.brand),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("onboarding_continue_button")
                    ) {
                        Text(
                            text = if (step == 3) "Start tracking" else "Continue",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    val palette = LocalWorkoutPalette.current
    BorderedCard {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = palette.text
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val palette = LocalWorkoutPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = palette.text2
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                val isSelected = selected == opt
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) palette.brandSoft else palette.surface,
                    border = BorderStroke(1.dp, if (isSelected) palette.brand else palette.borderStrong),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelect(opt) }
                        .testTag("choice_${label.lowercase()}_${opt.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                        Text(
                            text = opt,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) palette.brandInk else palette.text2,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
