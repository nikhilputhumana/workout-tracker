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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
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
    onSaveGymFee: (lastPaidIso: String, nextDueIso: String, amount: Double) -> Unit,
    onMarkFeePaidToday: () -> Unit,
    onSaveMeasurements: (waistCm: Double, chestCm: Double, armCm: Double) -> Unit,
    onSelectProgressPhoto: (PhotoSlot, android.net.Uri) -> Unit,
    onClearProgressPhoto: (PhotoSlot) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onLoadSampleWorkouts: () -> Unit,
    onClearAllWorkouts: () -> Unit,
    onSetAppearance: (AppearanceMode) -> Unit,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val profile = uiState.profile
    val todayIso = WorkoutMath.todayIso()

    var amountText by remember(uiState.gymFee.amount) {
        mutableStateOf(WorkoutMath.formatKg(uiState.gymFee.amount))
    }
    var waistText by remember(uiState.measurements.waistCm) {
        mutableStateOf(WorkoutMath.formatKg(uiState.measurements.waistCm))
    }
    var chestText by remember(uiState.measurements.chestCm) {
        mutableStateOf(WorkoutMath.formatKg(uiState.measurements.chestCm))
    }
    var armText by remember(uiState.measurements.armCm) {
        mutableStateOf(WorkoutMath.formatKg(uiState.measurements.armCm))
    }

    var activePhotoSlot by remember { mutableStateOf(PhotoSlot.FRONT) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectProgressPhoto(activePhotoSlot, uri)
        }
    }

    val openLastPaidPicker = rememberDatePickerLauncher(uiState.gymFee.lastPaidIso) { picked ->
        onSaveGymFee(
            picked,
            uiState.gymFee.nextDueIso,
            amountText.toDoubleOrNull() ?: uiState.gymFee.amount
        )
    }

    val openNextDuePicker = rememberDatePickerLauncher(uiState.gymFee.nextDueIso) { picked ->
        onSaveGymFee(
            uiState.gymFee.lastPaidIso,
            picked,
            amountText.toDoubleOrNull() ?: uiState.gymFee.amount
        )
    }

    val dueIn = WorkoutMath.daysBetween(todayIso, uiState.gymFee.nextDueIso) ?: 10L
    val (feeBg, feeFg, feeBadge) = when {
        dueIn < 0 -> Triple(palette.redSoft, palette.redInk, "Overdue")
        dueIn <= 7 -> Triple(palette.orangeSoft, palette.orangeInk, "Due in ${dueIn}d")
        else -> Triple(palette.greenSoft, palette.greenInk, "Paid")
    }

    val bwLog = uiState.bodyWeightLog
    val latestBw = bwLog.lastOrNull()?.weightKg ?: profile.bodyWeightKg
    val bwDelta = if (bwLog.size >= 2) bwLog.last().weightKg - bwLog.first().weightKg else 0.0
    val heightM = (profile.heightCm / 100.0).coerceAtLeast(1.0)
    val bmi = latestBw / heightM.pow(2.0)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.surface,
        unfocusedContainerColor = palette.surface,
        focusedBorderColor = palette.brand,
        unfocusedBorderColor = palette.borderStrong,
        focusedTextColor = palette.text,
        unfocusedTextColor = palette.text
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(palette.brandSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (profile.name.ifBlank { "A" }).take(1).uppercase(),
                            fontSize = 16.sp,
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
                        Text(
                            text = "${profile.age} · ${profile.gender} · ${profile.heightCm} cm · ${profile.experienceLevel} · ${profile.preferredSplit} split",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.muted
                        )
                    }
                    OutlinedButton(
                        onClick = onEditProfile,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, palette.borderStrong),
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("settings_edit_profile_button")
                    ) {
                        Text("Edit", color = palette.text2)
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
                                dateIso = uiState.gymFee.lastPaidIso,
                                onClick = openLastPaidPicker,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_fee_last_paid_button")
                            )
                            DateFieldCard(
                                label = "Next due",
                                dateIso = uiState.gymFee.nextDueIso,
                                onClick = openNextDuePicker,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_fee_next_due_button")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = {
                                    amountText = it
                                    val amt = it.toDoubleOrNull()
                                    if (amt != null) {
                                        onSaveGymFee(
                                            uiState.gymFee.lastPaidIso,
                                            uiState.gymFee.nextDueIso,
                                            amt
                                        )
                                    }
                                },
                                label = { Text("Amount (₹)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                    .heightIn(min = 52.dp)
                                    .testTag("settings_mark_paid_button")
                            ) {
                                Text("Mark paid today", fontWeight = FontWeight.Medium)
                            }
                        }

                        Text(
                            text = "Last paid ${WorkoutMath.formatShort(uiState.gymFee.lastPaidIso)} · ₹${WorkoutMath.formatKg(uiState.gymFee.amount)} · next due ${WorkoutMath.formatShort(uiState.gymFee.nextDueIso)}",
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
                                value = "${WorkoutMath.formatKg(latestBw)} kg",
                                sub = if (bwLog.size >= 2) "${if (bwDelta > 0) "+" else ""}${String.format(Locale.US, "%.1f", bwDelta)} kg" else "logged",
                                subColor = if (bwDelta < 0) palette.greenInk else palette.muted,
                                modifier = Modifier.weight(1f)
                            )
                            MiniBodyStat(
                                label = "Body fat",
                                value = profile.bodyFatPercent?.let { "${WorkoutMath.formatKg(it)}%" } ?: "—",
                                sub = "optional",
                                modifier = Modifier.weight(1f)
                            )
                            MiniBodyStat(
                                label = "BMI",
                                value = String.format(Locale.US, "%.1f", bmi),
                                sub = "height & weight",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Measurements (cm)",
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
                                onValueChange = {
                                    waistText = it
                                    val w = it.toDoubleOrNull() ?: uiState.measurements.waistCm
                                    onSaveMeasurements(w, chestText.toDoubleOrNull() ?: uiState.measurements.chestCm, armText.toDoubleOrNull() ?: uiState.measurements.armCm)
                                },
                                label = { Text("Waist") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = chestText,
                                onValueChange = {
                                    chestText = it
                                    val c = it.toDoubleOrNull() ?: uiState.measurements.chestCm
                                    onSaveMeasurements(waistText.toDoubleOrNull() ?: uiState.measurements.waistCm, c, armText.toDoubleOrNull() ?: uiState.measurements.armCm)
                                },
                                label = { Text("Chest") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = armText,
                                onValueChange = {
                                    armText = it
                                    val a = it.toDoubleOrNull() ?: uiState.measurements.armCm
                                    onSaveMeasurements(waistText.toDoubleOrNull() ?: uiState.measurements.waistCm, chestText.toDoubleOrNull() ?: uiState.measurements.chestCm, a)
                                },
                                label = { Text("Arm") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = fieldColors,
                                modifier = Modifier.weight(1f)
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
                                val path = when (slot) {
                                    PhotoSlot.FRONT -> uiState.measurements.frontPhotoPath
                                    PhotoSlot.SIDE -> uiState.measurements.sidePhotoPath
                                    PhotoSlot.BACK -> uiState.measurements.backPhotoPath
                                }
                                ProgressPhotoSlotBox(
                                    label = slot.label,
                                    photoPath = path,
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
                            fontSize = 11.sp,
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
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.tealSoft)
                                .border(1.dp, palette.tealBd, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = palette.teal,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "${uiState.sessions.size} workouts · ${uiState.bodyWeightLog.size} weigh-ins · stored on this device",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
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
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = palette.faint
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onLoadSampleWorkouts,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, palette.borderStrong),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("settings_load_sample_button")
                            ) {
                                Text("Load sample data", fontSize = 12.sp, color = palette.text2)
                            }
                            OutlinedButton(
                                onClick = { showClearConfirm = true },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, palette.redInk.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .testTag("settings_clear_data_button")
                            ) {
                                Text("Clear history", fontSize = 12.sp, color = palette.redInk)
                            }
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
                                        .heightIn(min = 44.dp)
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
                                            fontSize = 13.sp,
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
                    .heightIn(min = 46.dp)
                    .testTag("settings_replay_onboarding_button")
            ) {
                Text("Replay onboarding", color = palette.muted)
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = palette.text2
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Text(
                    text = WorkoutMath.formatShort(dateIso),
                    fontSize = 13.sp,
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
            fontSize = 11.sp,
            color = subColor ?: palette.muted,
            maxLines = 1
        )
    }
}

@Composable
private fun ProgressPhotoSlotBox(
    label: String,
    photoPath: String?,
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
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove $label photo",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
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
