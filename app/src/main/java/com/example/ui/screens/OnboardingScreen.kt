package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.BodyMeasurements
import com.example.domain.ExerciseCatalog
import com.example.domain.PhotoSlot
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.ui.components.BorderedCard
import com.example.ui.components.rememberDatePickerLauncher
import com.example.ui.theme.LocalWorkoutPalette
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialProfile: UserProfile,
    initialMeasurements: BodyMeasurements = BodyMeasurements(),
    initialNextFeeIso: String,
    startStep: Int = 1,
    isEditOnly: Boolean = false,
    onComplete: (
        profile: UserProfile,
        nextFeeIso: String,
        waistCm: Double?,
        chestCm: Double?,
        armCm: Double?
    ) -> Unit,
    onImportLog: (
        profile: UserProfile,
        nextFeeIso: String,
        waistCm: Double?,
        chestCm: Double?,
        armCm: Double?
    ) -> Unit = { _, _, _, _, _ -> },
    onSelectProgressPhoto: ((PhotoSlot, Uri) -> Unit)? = null,
    onClearProgressPhoto: ((PhotoSlot) -> Unit)? = null,
    onCancelEdit: (() -> Unit)? = null
) {
    val palette = LocalWorkoutPalette.current
    var step by remember(startStep) { mutableIntStateOf(startStep) }
    var name by remember { mutableStateOf(initialProfile.name) }
    var ageText by remember { mutableStateOf(initialProfile.age?.toString() ?: "") }
    var heightText by remember { mutableStateOf(initialProfile.heightCm.toString()) }
    var weightText by remember { mutableStateOf(WorkoutMath.formatKg(initialProfile.bodyWeightKg)) }
    var bfText by remember { mutableStateOf(initialProfile.bodyFatPercent?.let { WorkoutMath.formatKg(it) } ?: "") }
    var gender by remember { mutableStateOf(initialProfile.gender) }
    var level by remember { mutableStateOf(initialProfile.experienceLevel) }
    var repRangeField by remember {
        val initRange = initialProfile.preferredRepRange.ifBlank { "15-12-10-8" }
        mutableStateOf(TextFieldValue(initRange, TextRange(initRange.length)))
    }
    var waistText by remember { mutableStateOf(initialMeasurements.waistCm?.let { WorkoutMath.formatKg(it) } ?: "") }
    var chestText by remember { mutableStateOf(initialMeasurements.chestCm?.let { WorkoutMath.formatKg(it) } ?: "") }
    var armText by remember { mutableStateOf(initialMeasurements.armCm?.let { WorkoutMath.formatKg(it) } ?: "") }
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

    var activePhotoSlot by remember { mutableStateOf(PhotoSlot.FRONT) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectProgressPhoto?.invoke(activePhotoSlot, uri)
        }
    }

    BackHandler(enabled = !isEditOnly && step > 1) {
        step--
    }

    val openDatePicker = rememberDatePickerLauncher(nextFeeIso) { picked ->
        nextFeeIso = picked
    }

    val nameError = if (name.trim().isEmpty()) "Required · enter your name" else null
    val heightInt = heightText.toIntOrNull()
    val heightError = if (heightInt == null || heightInt !in 100..240) "Required · 100–240" else null
    val weightDouble = weightText.toDoubleOrNull()
    val weightError = if (weightDouble == null || weightDouble !in 25.0..300.0) "Required · 25–300 kg" else null

    val ageInt = ageText.toIntOrNull()
    val ageError = if (ageInt == null || ageInt !in 12..100) "Required · 12–100" else null
    val bfDouble = bfText.toDoubleOrNull()
    val bfError = if (bfText.isNotBlank() && (bfDouble == null || bfDouble !in 3.0..65.0)) "Enter 3–65%" else null

    val waistDouble = waistText.toDoubleOrNull()
    val waistError = if (waistText.isNotBlank() && (waistDouble == null || waistDouble !in 30.0..250.0)) "30–250" else null
    val chestDouble = chestText.toDoubleOrNull()
    val chestError = if (chestText.isNotBlank() && (chestDouble == null || chestDouble !in 40.0..250.0)) "40–250" else null
    val armDouble = armText.toDoubleOrNull()
    val armError = if (armText.isNotBlank() && (armDouble == null || armDouble !in 15.0..100.0)) "15–100" else null

    val metricsValid = nameError == null && heightError == null && weightError == null &&
        ageError == null && bfError == null &&
        waistError == null && chestError == null && armError == null

    fun sanitizeDecimal(input: String): String = buildString {
        var dotSeen = false
        for (ch in input) {
            if (ch.isDigit()) append(ch)
            else if (ch == '.' && !dotSeen) {
                dotSeen = true
                append(ch)
            }
        }
    }.take(6)

    fun sanitizeRepRange(input: String): String = buildString {
        for (ch in input) {
            val mapped = if (ch == '\n' || ch == '\r') '-' else ch
            if (mapped.isDigit()) {
                append(mapped)
            } else if (mapped == '-' && isNotEmpty() && last() != '-') {
                append('-')
            }
        }
    }.take(24)

    fun insertHyphenAtRepCursor() {
        val text = repRangeField.text
        val start = repRangeField.selection.start.coerceIn(0, text.length)
        val end = repRangeField.selection.end.coerceIn(0, text.length)
        val raw = text.substring(0, start) + "-" + text.substring(end)
        val sanitized = sanitizeRepRange(raw)
        val cursor = (start + 1).coerceAtMost(sanitized.length)
        repRangeField = TextFieldValue(sanitized, TextRange(cursor))
    }

    fun buildProfile(): UserProfile = UserProfile(
        name = name.trim(),
        age = ageInt?.coerceIn(12, 100) ?: initialProfile.age?.coerceIn(12, 100),
        gender = gender.trim(),
        heightCm = heightInt?.coerceIn(100, 240) ?: initialProfile.heightCm.coerceIn(100, 240),
        bodyWeightKg = weightDouble?.coerceIn(25.0, 300.0) ?: initialProfile.bodyWeightKg.coerceIn(25.0, 300.0),
        bodyFatPercent = if (bfText.isBlank()) null else bfDouble?.coerceIn(3.0, 65.0),
        experienceLevel = level.trim(),
        preferredSplit = split,
        preferredRepRange = repRangeField.text.trim('-').ifBlank { "15-12-10-8" },
        weeklyTargetSessions = initialProfile.weeklyTargetSessions,
        onboardingComplete = true
    )

    fun parsedWaist(): Double? = if (waistText.isBlank()) null else waistDouble?.coerceIn(30.0, 250.0)
    fun parsedChest(): Double? = if (chestText.isBlank()) null else chestDouble?.coerceIn(40.0, 250.0)
    fun parsedArm(): Double? = if (armText.isBlank()) null else armDouble?.coerceIn(15.0, 100.0)

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
                            text = "Body metrics, measurements & rep range",
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
                            text = "Name, age, height, and body weight are required. Everything else is optional and can be updated later in Profile.",
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
                        label = { Text("Name *") },
                        placeholder = { Text("Your name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Height (cm) *") },
                            isError = heightError != null,
                            supportingText = heightError?.let { msg -> { Text(msg) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_height_input")
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = sanitizeDecimal(it) },
                            label = { Text("Body weight (kg) *") },
                            isError = weightError != null,
                            supportingText = weightError?.let { msg -> { Text(msg) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_weight_input")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Age *") },
                            placeholder = { Text("e.g. 26") },
                            isError = ageError != null,
                            supportingText = ageError?.let { msg -> { Text(msg) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_age_input")
                        )
                        OutlinedTextField(
                            value = bfText,
                            onValueChange = { bfText = sanitizeDecimal(it) },
                            label = { Text("Body fat % (optional)") },
                            placeholder = { Text("e.g. 15") },
                            isError = bfError != null,
                            supportingText = bfError?.let { msg -> { Text(msg) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = fieldColors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("profile_bf_input")
                        )
                    }

                    OptionalChoiceRow(
                        label = "Gender (optional · tap again to clear)",
                        options = listOf("Male", "Female", "Other"),
                        selected = gender,
                        onSelect = { opt -> gender = if (gender == opt) "" else opt }
                    )

                    OptionalChoiceRow(
                        label = "Experience (optional · tap again to clear)",
                        options = listOf("Beginner", "Intermediate", "Advanced"),
                        selected = level,
                        onSelect = { opt -> level = if (level == opt) "" else opt }
                    )

                    // Preferred Rep Range (digits + '-' only; Enter inserts '-')
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Preferred rep range",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = palette.text2
                        )
                        OutlinedTextField(
                            value = repRangeField,
                            onValueChange = { incoming ->
                                val sanitized = sanitizeRepRange(incoming.text)
                                val cursor = incoming.selection.end.coerceIn(0, sanitized.length)
                                repRangeField = TextFieldValue(sanitized, TextRange(cursor))
                            },
                            placeholder = { Text("15-12-10-8") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { insertHyphenAtRepCursor() },
                                onNext = { insertHyphenAtRepCursor() },
                                onGo = { insertHyphenAtRepCursor() },
                                onSend = { insertHyphenAtRepCursor() }
                            ),
                            singleLine = false,
                            maxLines = 1,
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        insertHyphenAtRepCursor()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .testTag("onboarding_rep_range_input")
                        )
                    }

                    // Body Measurements (Optional)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    .testTag("onboarding_waist_input")
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
                                    .testTag("onboarding_chest_input")
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
                                    .testTag("onboarding_arm_input")
                            )
                        }
                    }

                    // Progress Photos (optional)
                    if (onSelectProgressPhoto != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Progress photos (optional · stored on device)",
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
                                        PhotoSlot.FRONT -> initialMeasurements.frontPhotoPath
                                        PhotoSlot.SIDE -> initialMeasurements.sidePhotoPath
                                        PhotoSlot.BACK -> initialMeasurements.backPhotoPath
                                    }
                                    OnboardingPhotoSlotBox(
                                        label = slot.label,
                                        photoPath = path,
                                        onPick = {
                                            activePhotoSlot = slot
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        onRemove = { onClearProgressPhoto?.invoke(slot) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("onboarding_photo_${slot.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
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
                                onClick = {
                                    onImportLog(buildProfile(), nextFeeIso, parsedWaist(), parsedChest(), parsedArm())
                                },
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
                        onClick = {
                            onComplete(buildProfile(), nextFeeIso, parsedWaist(), parsedChest(), parsedArm())
                        },
                        enabled = metricsValid,
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
                    if (step > 1 || metricsValid) {
                        TextButton(
                            onClick = {
                                if (step > 1) step--
                                else onComplete(buildProfile(), nextFeeIso, parsedWaist(), parsedChest(), parsedArm())
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("onboarding_skip_or_back_button")
                        ) {
                            Text(if (step > 1) "Back" else "Skip", color = palette.muted)
                        }
                    }
                    Button(
                        onClick = {
                            if (step < 3) step++
                            else onComplete(buildProfile(), nextFeeIso, parsedWaist(), parsedChest(), parsedArm())
                        },
                        enabled = step != 2 || metricsValid,
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
private fun OptionalChoiceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val palette = LocalWorkoutPalette.current
    val tagKey = label.substringBefore(" ").lowercase()
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
                        .testTag("choice_${tagKey}_${opt.lowercase()}")
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

@Composable
private fun OnboardingPhotoSlotBox(
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
            .background(palette.surface)
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
                    contentDescription = "Add $label photo",
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
