package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.AppearanceMode
import com.example.domain.PhotoSlot
import com.example.domain.WorkoutMath
import com.example.ui.components.BorderedCard
import com.example.ui.components.SectionLabel
import com.example.ui.components.StatusBadge
import com.example.ui.components.rememberDatePickerLauncher
import com.example.ui.theme.LocalWorkoutPalette
import com.example.ui.viewmodel.MainUiState
import java.io.File
import java.util.Locale
import kotlin.math.pow

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onEditProfile: () -> Unit,
    onSaveSettingsChanges: (
        lastPaidIso: String,
        nextDueIso: String,
        amount: Double,
        heightCm: Int,
        bodyWeightKg: Double,
        bodyFatPercent: Double?,
        waistCm: Double?,
        chestCm: Double?,
        armCm: Double?,
        weeklyTargetSessions: Int
    ) -> Unit,
    onMarkFeePaidToday: () -> Unit,
    onSelectProgressPhoto: (PhotoSlot, android.net.Uri) -> Unit,
    onClearProgressPhoto: (PhotoSlot) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onClearAllWorkouts: () -> Unit,
    onSetAppearance: (AppearanceMode) -> Unit,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val profile = uiState.profile
    val todayIso = WorkoutMath.todayIso()

    val bwLog = uiState.bodyWeightLog
    val latestBw = bwLog.lastOrNull()?.weightKg ?: profile.bodyWeightKg
    val rawBwDelta = if (bwLog.size >= 2) bwLog.last().weightKg - bwLog.first().weightKg else 0.0
    val bwDelta = if (kotlin.math.abs(rawBwDelta) < 0.05) 0.0 else rawBwDelta

    var lastPaidIso by remember(uiState.gymFee.lastPaidIso) {
        mutableStateOf(uiState.gymFee.lastPaidIso)
    }
    var nextDueIso by remember(uiState.gymFee.nextDueIso) {
        mutableStateOf(uiState.gymFee.nextDueIso)
    }
    var amountText by remember(uiState.gymFee.amount) {
        mutableStateOf(WorkoutMath.formatKg(uiState.gymFee.amount))
    }
    var heightText by remember(profile.heightCm) {
        mutableStateOf(profile.heightCm.toString())
    }
    var weightText by remember(latestBw) {
        mutableStateOf(WorkoutMath.formatKg(latestBw))
    }
    var bfText by remember(profile.bodyFatPercent) {
        mutableStateOf(profile.bodyFatPercent?.let { WorkoutMath.formatKg(it) } ?: "")
    }
    var waistText by remember(uiState.measurements.waistCm) {
        mutableStateOf(uiState.measurements.waistCm?.let { WorkoutMath.formatKg(it) } ?: "")
    }
    var chestText by remember(uiState.measurements.chestCm) {
        mutableStateOf(uiState.measurements.chestCm?.let { WorkoutMath.formatKg(it) } ?: "")
    }
    var armText by remember(uiState.measurements.armCm) {
        mutableStateOf(uiState.measurements.armCm?.let { WorkoutMath.formatKg(it) } ?: "")
    }
    var weeklyTarget by remember(profile.weeklyTargetSessions) {
        mutableIntStateOf(profile.weeklyTargetSessions.coerceIn(1, 7))
    }

    val parsedAmount = amountText.toDoubleOrNull()
    val parsedHeight = heightText.toIntOrNull()
    val parsedWeight = weightText.toDoubleOrNull()
    val parsedBf = if (bfText.isBlank()) null else bfText.toDoubleOrNull()
    val parsedWaist = if (waistText.isBlank()) null else waistText.toDoubleOrNull()
    val parsedChest = if (chestText.isBlank()) null else chestText.toDoubleOrNull()
    val parsedArm = if (armText.isBlank()) null else armText.toDoubleOrNull()

    val amountError = if (parsedAmount == null || parsedAmount <= 0.0 || parsedAmount > 1_000_000.0) "Enter > 0" else null
    val heightError = if (parsedHeight == null || parsedHeight !in 100..240) "100–240" else null
    val weightError = if (parsedWeight == null || parsedWeight !in 25.0..300.0) "25–300" else null
    val bfError = if (bfText.isNotBlank() && (parsedBf == null || parsedBf !in 3.0..65.0)) "3–65%" else null
    val waistError = if (waistText.isNotBlank() && (parsedWaist == null || parsedWaist !in 30.0..250.0)) "30–250" else null
    val chestError = if (chestText.isNotBlank() && (parsedChest == null || parsedChest !in 40.0..250.0)) "40–250" else null
    val armError = if (armText.isNotBlank() && (parsedArm == null || parsedArm !in 15.0..100.0)) "15–100" else null
    val allInputsValid = amountError == null &&
        heightError == null &&
        weightError == null &&
        bfError == null &&
        waistError == null &&
        chestError == null &&
        armError == null

    fun sanitizeDecimal(input: String): String = buildString {
        var dotSeen = false
        for (ch in input) {
            if (ch.isDigit()) append(ch)
            else if (ch == '.' && !dotSeen) {
                dotSeen = true
                append(ch)
            }
        }
    }.take(7)

    val weightChanged = parsedWeight == null || kotlin.math.abs(parsedWeight - latestBw) > 0.001
    val bfChanged = if (parsedBf != null && profile.bodyFatPercent != null) {
        kotlin.math.abs(parsedBf - profile.bodyFatPercent) > 0.001
    } else {
        (bfText.isNotBlank()) != (profile.bodyFatPercent != null) || parsedBf != profile.bodyFatPercent
    }

    val hasUnsavedChanges =
        lastPaidIso != uiState.gymFee.lastPaidIso ||
            nextDueIso != uiState.gymFee.nextDueIso ||
            parsedAmount != uiState.gymFee.amount ||
            parsedHeight != profile.heightCm ||
            weightChanged ||
            bfChanged ||
            parsedWaist != uiState.measurements.waistCm ||
            parsedChest != uiState.measurements.chestCm ||
            parsedArm != uiState.measurements.armCm ||
            weeklyTarget != profile.weeklyTargetSessions.coerceIn(1, 7)

    var activePhotoSlot by remember { mutableStateOf(PhotoSlot.FRONT) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectProgressPhoto(activePhotoSlot, uri)
        }
    }

    val openLastPaidPicker = rememberDatePickerLauncher(lastPaidIso) { picked ->
        lastPaidIso = picked
    }

    val openNextDuePicker = rememberDatePickerLauncher(nextDueIso) { picked ->
        nextDueIso = picked
    }

    val dueIn = WorkoutMath.daysBetween(todayIso, nextDueIso) ?: 10L
    val (feeBg, feeFg, feeBadge) = when {
        dueIn < 0 -> Triple(palette.redSoft, palette.redInk, "Overdue")
        dueIn <= 7 -> Triple(palette.orangeSoft, palette.orangeInk, "Due in ${dueIn}d")
        else -> Triple(palette.greenSoft, palette.greenInk, "Paid")
    }

    val effectiveHeightCm = parsedHeight?.takeIf { it in 100..240 } ?: profile.heightCm
    val effectiveWeightKg = parsedWeight?.takeIf { it in 25.0..300.0 } ?: latestBw
    val bmi = WorkoutMath.calculateBmi(effectiveWeightKg, effectiveHeightCm)
    val bmiCategory = WorkoutMath.bmiCategory(bmi)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.surface,
        unfocusedContainerColor = palette.surface,
        focusedBorderColor = palette.brand,
        unfocusedBorderColor = palette.borderStrong,
        focusedTextColor = palette.text,
        unfocusedTextColor = palette.text
    )

    fun commitSave() {
        val validAmount = parsedAmount ?: return
        val validHeight = parsedHeight ?: return
        val validWeight = parsedWeight ?: return
        if (!allInputsValid) return
        onSaveSettingsChanges(
            lastPaidIso,
            nextDueIso,
            validAmount,
            validHeight,
            validWeight,
            parsedBf,
            parsedWaist,
            parsedChest,
            parsedArm,
            weeklyTarget
        )
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
            // Profile Card
            item {
                BorderedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(palette.brandSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (profile.name.ifBlank { "A" }).take(1).uppercase(),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.brandInk
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name.ifBlank { "Athlete" },
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.text
                            )
                            val profileParts = listOfNotNull(
                                profile.age?.let { "$it yrs" },
                                profile.gender.ifBlank { null },
                                "${profile.heightCm} cm",
                                profile.experienceLevel.ifBlank { null },
                                profile.preferredRepRange.ifBlank { null }?.let { "Reps $it" },
                                profile.preferredSplit.ifBlank { null }
                            )
                            Text(
                                text = profileParts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                                modifier = Modifier.testTag("profile_summary_subtitle")
                            )
                        }
                        OutlinedButton(
                            onClick = onEditProfile,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, palette.borderStrong),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("settings_edit_profile_button")
                        ) {
                            Text("Edit", color = palette.text2)
                        }
                    }
                }
            }

            // Weekly Workout Target Card
            item {
                BorderedCard {
                    Column {
                        SectionLabel(
                            text = "Training goal",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.border)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Target sessions per week",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.text
                                )
                                Text(
                                    text = "Shown on your Home sessions progress counter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.muted
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.appBg,
                                    border = BorderStroke(1.dp, palette.borderStrong),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(enabled = weeklyTarget > 1) {
                                            weeklyTarget = (weeklyTarget - 1).coerceIn(1, 7)
                                        }
                                        .testTag("settings_target_minus")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Remove,
                                            contentDescription = "Decrease weekly target",
                                            tint = if (weeklyTarget > 1) palette.text else palette.faint
                                        )
                                    }
                                }
                                Text(
                                    text = "$weeklyTarget",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.text,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.appBg,
                                    border = BorderStroke(1.dp, palette.borderStrong),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable(enabled = weeklyTarget < 7) {
                                            weeklyTarget = (weeklyTarget + 1).coerceIn(1, 7)
                                        }
                                        .testTag("settings_target_plus")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Increase weekly target",
                                            tint = if (weeklyTarget < 7) palette.text else palette.faint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Gym Fees Card
            item {
                BorderedCard {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionLabel("Gym fees")
                            Spacer(Modifier.weight(1f))
                            StatusBadge(label = feeBadge, bg = feeBg, fg = feeFg)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.border)
                        )
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DateFieldCard(
                                    label = "Last paid",
                                    dateIso = lastPaidIso,
                                    onClick = openLastPaidPicker,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_fee_last_paid_button")
                                )
                                DateFieldCard(
                                    label = "Next due",
                                    dateIso = nextDueIso,
                                    onClick = openNextDuePicker,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_fee_next_due_button")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = sanitizeDecimal(it) },
                                    label = { Text("Amount (₹)") },
                                    isError = amountError != null,
                                    supportingText = amountError?.let { msg -> { Text(msg) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_fee_amount_input")
                                )
                                Button(
                                    onClick = onMarkFeePaidToday,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.brandSoft,
                                        contentColor = palette.brandInk
                                    ),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .heightIn(min = 52.dp)
                                        .testTag("settings_mark_paid_button")
                                ) {
                                    Text("Mark paid today", fontWeight = FontWeight.Medium)
                                }
                            }

                            val summaryAmt = parsedAmount ?: uiState.gymFee.amount
                            Text(
                                text = "Last paid ${WorkoutMath.formatShort(lastPaidIso)} · ₹${WorkoutMath.formatKg(summaryAmt)} · next due ${WorkoutMath.formatShort(nextDueIso)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted
                            )
                        }
                    }
                }
            }

            // Body Tracking & Local Photo Picker Card
            item {
                BorderedCard {
                    Column {
                        SectionLabel(
                            text = "Body tracking",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.border)
                        )
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MiniBodyStat(
                                    label = "Weight",
                                    value = "${WorkoutMath.formatKg(effectiveWeightKg)} kg",
                                    sub = if (bwLog.size >= 2) "${if (bwDelta > 0) "+" else ""}${String.format(Locale.US, "%.1f", bwDelta)} kg" else "current",
                                    subColor = if (bwDelta < 0) palette.greenInk else palette.muted,
                                    modifier = Modifier.weight(1f)
                                )
                                parsedBf?.takeIf { bfError == null }?.let { bf ->
                                    MiniBodyStat(
                                        label = "Body fat",
                                        value = "${WorkoutMath.formatKg(bf)}%",
                                        sub = "optional",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                MiniBodyStat(
                                    label = "BMI",
                                    value = String.format(Locale.US, "%.1f", bmi),
                                    sub = bmiCategory,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_bmi_stat")
                                )
                            }

                            Text(
                                text = "Height, Weight & Fat %",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = palette.text2
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = heightText,
                                    onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
                                    label = { Text("Height (cm)") },
                                    isError = heightError != null,
                                    supportingText = heightError?.let { msg -> { Text(msg) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_height_input")
                                )
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { weightText = sanitizeDecimal(it).take(6) },
                                    label = { Text("Weight (kg)") },
                                    isError = weightError != null,
                                    supportingText = weightError?.let { msg -> { Text(msg) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_weight_input")
                                )
                                OutlinedTextField(
                                    value = bfText,
                                    onValueChange = { bfText = sanitizeDecimal(it).take(5) },
                                    label = { Text("Fat %") },
                                    placeholder = { Text("Optional") },
                                    isError = bfError != null,
                                    supportingText = bfError?.let { msg -> { Text(msg) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_body_fat_input")
                                )
                            }

                            Text(
                                text = "Body measurements, cm (Optional)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = palette.text2
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = waistText,
                                    onValueChange = { waistText = sanitizeDecimal(it) },
                                    label = { Text("Waist") },
                                    placeholder = { Text("Optional") },
                                    isError = waistError != null,
                                    supportingText = waistError?.let { msg -> { Text(msg) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_waist_input")
                                )
                                OutlinedTextField(
                                    value = chestText,
                                    onValueChange = { chestText = sanitizeDecimal(it) },
                                    label = { Text("Chest") },
                                    placeholder = { Text("Optional") },
                                    isError = chestError != null,
                                    supportingText = chestError?.let { msg -> { Text(msg) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_chest_input")
                                )
                                OutlinedTextField(
                                    value = armText,
                                    onValueChange = { armText = sanitizeDecimal(it) },
                                    label = { Text("Arm") },
                                    placeholder = { Text("Optional") },
                                    isError = armError != null,
                                    supportingText = armError?.let { msg -> { Text(msg) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_arm_input")
                                )
                            }

                            Button(
                                onClick = { commitSave() },
                                enabled = hasUnsavedChanges && allInputsValid,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.brand,
                                    disabledContainerColor = palette.subtle,
                                    contentColor = Color.White,
                                    disabledContentColor = palette.faint
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_save_changes_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        !allInputsValid -> "Fix invalid numbers"
                                        hasUnsavedChanges -> "Save changes"
                                        else -> "All profile changes saved"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "Progress photos",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = palette.text2
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PhotoSlot.entries.forEach { slot ->
                                    val (path, photoDate) = when (slot) {
                                        PhotoSlot.FRONT -> uiState.measurements.frontPhotoPath to uiState.measurements.frontPhotoDateIso
                                        PhotoSlot.SIDE -> uiState.measurements.sidePhotoPath to uiState.measurements.sidePhotoDateIso
                                        PhotoSlot.BACK -> uiState.measurements.backPhotoPath to uiState.measurements.backPhotoDateIso
                                    }
                                    ProgressPhotoSlotBox(
                                        label = slot.label,
                                        photoPath = path,
                                        photoDateIso = photoDate,
                                        onPick = {
                                            activePhotoSlot = slot
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        onRemove = { onClearProgressPhoto(slot) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_photo_${slot.name.lowercase()}")
                                    )
                                }
                            }
                            Text(
                                text = "Stored on device only via Android Photo Picker. Never uploaded.",
                                fontSize = 12.sp,
                                color = palette.faint
                            )
                        }
                    }
                }
            }

            // Your Data — Export & Import Card
            item {
                BorderedCard {
                    Column {
                        SectionLabel(
                            text = "Your data",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.border)
                        )
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.tealSoft)
                                    .border(1.dp, palette.tealBd, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = palette.teal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${uiState.sessions.size} workouts · ${uiState.bodyWeightLog.size} weigh-ins · stored on this device",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.text
                                    )
                                    Text(
                                        text = "One JSON file holds everything: profile, workouts, body log, fees. Import replaces current data.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.text2
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onExportJson,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.brand),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_export_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Export", color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = onImportJson,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, palette.brandBd),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_import_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Upload,
                                        contentDescription = null,
                                        tint = palette.brandInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Import", color = palette.brandInk)
                                }
                            }

                            Text(
                                text = "workout-tracker-$todayIso.json",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = palette.faint
                            )

                            OutlinedButton(
                                onClick = { showClearConfirm = true },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, palette.redInk.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_clear_data_button")
                            ) {
                                Text("Clear workout history", fontSize = 13.sp, color = palette.redInk)
                            }
                        }
                    }
                }
            }

            // Appearance Card
            item {
                BorderedCard {
                    Column {
                        SectionLabel(
                            text = "Appearance",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(palette.border)
                        )
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val opts = listOf(
                                    Triple(AppearanceMode.LIGHT, "Light", Icons.Filled.LightMode),
                                    Triple(AppearanceMode.DARK, "Dark", Icons.Filled.DarkMode),
                                    Triple(AppearanceMode.SYSTEM, "System", Icons.Filled.BrightnessAuto)
                                )
                                opts.forEach { (mode, label, icon) ->
                                    val selected = uiState.appearanceMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selected) palette.brandSoft else palette.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) palette.brand else palette.borderStrong
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onSetAppearance(mode) }
                                            .testTag("settings_theme_${mode.name.lowercase()}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (selected) palette.brandInk else palette.text2,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (selected) palette.brandInk else palette.text2
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (palette.isDark) {
                                    "Dark surfaces with the same cobalt blue for actions. Easier on the eyes in a dim gym."
                                } else {
                                    "Light theme. Switch anytime from the icon in the top bar."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted
                            )
                        }
                    }
                }
            }

            // Replay Onboarding Button
            item {
                OutlinedButton(
                    onClick = onReplayOnboarding,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("settings_replay_onboarding_button")
                ) {
                    Text("Replay onboarding", color = palette.muted)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        if (hasUnsavedChanges || !allInputsValid) {
            Surface(
                color = palette.surface,
                border = BorderStroke(1.dp, if (allInputsValid) palette.brandBd else palette.redInk.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (!allInputsValid) "Fix invalid numbers before saving" else "You have unsaved profile changes",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (!allInputsValid) palette.redInk else palette.brandInk,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { commitSave() },
                        enabled = allInputsValid,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.brand),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("settings_sticky_save_button")
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all workout history?") },
            text = { Text("This deletes all saved workouts and weigh-ins on this device. Export a JSON backup first if you want to keep them.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAllWorkouts()
                    }
                ) {
                    Text("Clear", color = palette.redInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DateFieldCard(
    label: String,
    dateIso: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = palette.text2
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Text(
                    text = WorkoutMath.formatShort(dateIso),
                    fontSize = 14.sp,
                    color = palette.text
                )
            }
        }
    }
}

@Composable
private fun MiniBodyStat(
    label: String,
    value: String,
    sub: String,
    subColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(palette.appBg)
            .border(1.dp, palette.border, RoundedCornerShape(4.dp))
            .padding(10.dp)
    ) {
        SectionLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = palette.text
        )
        Text(
            text = sub,
            fontSize = 12.sp,
            color = subColor ?: palette.muted,
            maxLines = 1
        )
    }
}

@Composable
private fun ProgressPhotoSlotBox(
    label: String,
    photoPath: String?,
    photoDateIso: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    Box(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.appBg)
            .border(1.dp, palette.borderStrong, RoundedCornerShape(6.dp))
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        if (photoPath != null && File(photoPath).exists()) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "$label progress photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clickable(onClick = onRemove)
                    .padding(6.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove $label photo",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(vertical = 3.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (!photoDateIso.isNullOrBlank()) {
                    Text(
                        text = WorkoutMath.formatDayMonth(photoDateIso),
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Pick $label photo",
                    tint = palette.faint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = palette.faint
                )
            }
        }
    }
}
