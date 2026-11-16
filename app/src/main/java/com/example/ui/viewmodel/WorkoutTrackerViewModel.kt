package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ExerciseAiRepository
import com.example.data.PreferencesRepository
import com.example.data.WorkoutDatabase
import com.example.data.WorkoutRepository
import com.example.domain.ActiveDraft
import com.example.domain.AiExerciseSuggestion
import com.example.domain.AiLookupState
import com.example.domain.AppearanceMode
import com.example.domain.BodyMeasurementEntry
import com.example.domain.BodyMeasurements
import com.example.domain.BodyWeightEntry
import com.example.domain.DraftExercise
import com.example.domain.DraftSet
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.GymFeeRecord
import com.example.domain.LoggedExercise
import com.example.domain.PhotoSlot
import com.example.domain.RpeRating
import com.example.domain.SplitPreset
import com.example.domain.ToastBanner
import com.example.domain.ToastTone
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

data class WizardSelectionState(
    val dateIso: String = WorkoutMath.todayIso(),
    val groups: List<String> = emptyList()
)

data class MainUiState(
    val isLoaded: Boolean = false,
    val showLaunchSplash: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val measurements: BodyMeasurements = BodyMeasurements(),
    val gymFee: GymFeeRecord = GymFeeRecord(),
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val sessions: List<WorkoutSession> = emptyList(),
    val bodyWeightLog: List<BodyWeightEntry> = emptyList(),
    val measurementLog: List<BodyMeasurementEntry> = emptyList(),
    val customExercises: List<ExerciseDefinition> = emptyList(),
    val allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES,
    val activeDraft: ActiveDraft? = null,
    val wizardState: WizardSelectionState = WizardSelectionState(),
    val aiLookupState: AiLookupState = AiLookupState.Idle,
    val toastBanner: ToastBanner? = null
)

class WorkoutTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WorkoutDatabase.getInstance(application)
    private val prefsRepo = PreferencesRepository(application)
    private val repository = WorkoutRepository(database.workoutDao(), prefsRepo)
    private val aiRepository = ExerciseAiRepository(application)

    private val _activeDraft = MutableStateFlow<ActiveDraft?>(null)
    private val _wizardState = MutableStateFlow(WizardSelectionState())
    private val _aiState = MutableStateFlow<AiLookupState>(AiLookupState.Idle)
    private val _toast = MutableStateFlow<ToastBanner?>(null)
    private val _showLaunchSplash = MutableStateFlow(true)
    private val _bundledExercises = MutableStateFlow<List<ExerciseDefinition>>(emptyList())

    private var timerJob: Job? = null
    private var toastJob: Job? = null
    private var timerAnchorElapsedMs: Long? = null
    private var timerBaseSeconds: Int = 0

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val builtInNames = ExerciseCatalog.BUILT_IN_EXERCISES
                    .map { it.name.lowercase(Locale.US) }
                    .toSet()
                ExerciseCatalog.loadAssetExercises(application)
                    .filter { it.name.lowercase(Locale.US) !in builtInNames }
            }
            _bundledExercises.value = loaded
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        combine(
            repository.profileFlow,
            repository.measurementsFlow,
            repository.gymFeeFlow,
            repository.appearanceFlow
        ) { profile, measures, fee, appearance ->
            listOf(profile, measures, fee, appearance)
        },
        combine(
            repository.sessionsFlow,
            repository.bodyWeightFlow,
            repository.measurementLogFlow,
            repository.customExercisesFlow,
            _bundledExercises
        ) { sessions, bw, measureLog, custom, bundled ->
            listOf(sessions, bw, measureLog, custom, bundled)
        },
        combine(_activeDraft, _wizardState) { draft, wizard -> draft to wizard },
        _aiState,
        combine(_toast, _showLaunchSplash) { toast, splash -> toast to splash }
    ) { prefsList, dbList, draftWizard, aiState, toastSplash ->
        val profile = prefsList[0] as UserProfile
        val measures = prefsList[1] as BodyMeasurements
        val fee = prefsList[2] as GymFeeRecord
        val appearance = prefsList[3] as AppearanceMode
        @Suppress("UNCHECKED_CAST")
        val sessions = dbList[0] as List<WorkoutSession>
        @Suppress("UNCHECKED_CAST")
        val bw = dbList[1] as List<BodyWeightEntry>
        @Suppress("UNCHECKED_CAST")
        val measureLog = dbList[2] as List<BodyMeasurementEntry>
        @Suppress("UNCHECKED_CAST")
        val custom = dbList[3] as List<ExerciseDefinition>
        @Suppress("UNCHECKED_CAST")
        val bundled = dbList[4] as List<ExerciseDefinition>
        val combinedExercises = (ExerciseCatalog.BUILT_IN_EXERCISES + bundled + custom)
            .distinctBy { it.name.lowercase(Locale.US) }
        MainUiState(
            isLoaded = true,
            showLaunchSplash = toastSplash.second,
            profile = profile,
            measurements = measures,
            gymFee = fee,
            appearanceMode = appearance,
            sessions = sessions,
            bodyWeightLog = bw,
            measurementLog = measureLog,
            customExercises = custom,
            allExercises = combinedExercises,
            activeDraft = draftWizard.first,
            wizardState = draftWizard.second,
            aiLookupState = aiState,
            toastBanner = toastSplash.first
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(isLoaded = false, showLaunchSplash = true)
    )

    fun dismissLaunchSplash() {
        _showLaunchSplash.value = false
    }

    fun showToast(message: String, tone: ToastTone = ToastTone.SUCCESS) {
        toastJob?.cancel()
        _toast.value = ToastBanner(message, tone)
        toastJob = viewModelScope.launch {
            delay(3_200)
            _toast.value = null
        }
    }

    fun clearToast() {
        toastJob?.cancel()
        _toast.value = null
    }

    // ── Onboarding & Profile ──

    fun completeOnboarding(
        profile: UserProfile,
        nextFeeDueIso: String,
        waistCm: Double? = null,
        chestCm: Double? = null,
        armCm: Double? = null
    ) {
        viewModelScope.launch {
            val savedProfile = profile.copy(
                name = profile.name.trim(),
                preferredRepRange = profile.preferredRepRange.trim().ifBlank { "15-12-10-8" },
                onboardingComplete = true
            )
            repository.saveProfile(savedProfile)
            repository.saveMeasurements(
                waistCm = waistCm,
                chestCm = chestCm,
                armCm = armCm,
                recordHistory = true,
                heightCm = savedProfile.heightCm,
                bodyFatPercent = savedProfile.bodyFatPercent
            )
            if (nextFeeDueIso.isNotBlank()) {
                val currentFee = uiState.value.gymFee
                repository.saveGymFee(currentFee.copy(nextDueIso = nextFeeDueIso))
            }
            if (uiState.value.bodyWeightLog.isEmpty() && savedProfile.bodyWeightKg > 0) {
                repository.logBodyWeight(savedProfile.bodyWeightKg)
            }
            showToast("You are set up! Tap Create workout to begin.")
        }
    }

    fun importBackupDuringOnboarding(
        json: String,
        pendingProfile: UserProfile,
        nextFeeDueIso: String,
        waistCm: Double? = null,
        chestCm: Double? = null,
        armCm: Double? = null
    ) {
        viewModelScope.launch {
            runCatching {
                val result = repository.importBackupJson(json)
                if (!result.includedProfile) {
                    val savedProfile = pendingProfile.copy(
                        name = pendingProfile.name.trim(),
                        preferredRepRange = pendingProfile.preferredRepRange.trim().ifBlank { "15-12-10-8" },
                        onboardingComplete = true
                    )
                    repository.saveProfile(savedProfile)
                    repository.saveMeasurements(
                        waistCm = waistCm,
                        chestCm = chestCm,
                        armCm = armCm,
                        recordHistory = true,
                        heightCm = savedProfile.heightCm,
                        bodyFatPercent = savedProfile.bodyFatPercent
                    )
                    if (nextFeeDueIso.isNotBlank()) {
                        val currentFee = uiState.value.gymFee
                        repository.saveGymFee(currentFee.copy(nextDueIso = nextFeeDueIso))
                    }
                }
                result
            }.onSuccess { result ->
                showToast("Imported ${result.sessionCount} workouts successfully")
            }.onFailure { err ->
                showToast(err.message ?: "That file is not a Workout Tracker backup", ToastTone.WARNING)
            }
        }
    }

    fun updateProfile(
        profile: UserProfile,
        waistCm: Double? = uiState.value.measurements.waistCm,
        chestCm: Double? = uiState.value.measurements.chestCm,
        armCm: Double? = uiState.value.measurements.armCm
    ) {
        viewModelScope.launch {
            val prevProfile = uiState.value.profile
            val prevMeasures = uiState.value.measurements
            val currentLatestBw = uiState.value.bodyWeightLog.lastOrNull()?.weightKg ?: prevProfile.bodyWeightKg
            val savedProfile = profile.copy(
                name = profile.name.trim(),
                preferredRepRange = profile.preferredRepRange.trim().ifBlank { "15-12-10-8" },
                onboardingComplete = true
            )
            repository.saveProfile(savedProfile)

            if (kotlin.math.abs(savedProfile.bodyWeightKg - currentLatestBw) > 0.001 || uiState.value.bodyWeightLog.isEmpty()) {
                repository.logBodyWeight(savedProfile.bodyWeightKg)
            }

            val heightChanged = savedProfile.heightCm != prevProfile.heightCm
            val bfChanged = savedProfile.bodyFatPercent != prevProfile.bodyFatPercent
            val measuresChanged = waistCm != prevMeasures.waistCm || chestCm != prevMeasures.chestCm || armCm != prevMeasures.armCm

            if (measuresChanged) {
                repository.saveMeasurements(waistCm, chestCm, armCm, recordHistory = false)
            }
            if (measuresChanged || heightChanged || bfChanged) {
                repository.recordMeasurementSnapshot(
                    waistCm = if (measuresChanged) waistCm else null,
                    chestCm = if (measuresChanged) chestCm else null,
                    armCm = if (measuresChanged) armCm else null,
                    heightCm = if (heightChanged) savedProfile.heightCm else null,
                    bodyFatPercent = if (bfChanged) savedProfile.bodyFatPercent else null
                )
            }
            showToast("Profile updated")
        }
    }

    fun replayOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingComplete(false)
        }
    }

    // ── Appearance, Fees, Body & Data ──

    fun setAppearanceMode(mode: AppearanceMode) {
        viewModelScope.launch {
            repository.setAppearanceMode(mode)
        }
    }

    fun saveSettingsChanges(
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
    ) {
        viewModelScope.launch {
            val prevProfile = uiState.value.profile
            val prevMeasures = uiState.value.measurements
            val currentLatestBw = uiState.value.bodyWeightLog.lastOrNull()?.weightKg ?: prevProfile.bodyWeightKg

            repository.saveGymFee(GymFeeRecord(lastPaidIso, nextDueIso, amount))

            val heightChanged = heightCm != prevProfile.heightCm
            val weightChanged = kotlin.math.abs(bodyWeightKg - currentLatestBw) > 0.001 || uiState.value.bodyWeightLog.isEmpty()
            val bfChanged = bodyFatPercent != prevProfile.bodyFatPercent
            val measuresChanged = waistCm != prevMeasures.waistCm || chestCm != prevMeasures.chestCm || armCm != prevMeasures.armCm

            val updatedProfile = prevProfile.copy(
                heightCm = heightCm.coerceIn(100, 240),
                bodyWeightKg = bodyWeightKg.coerceIn(25.0, 300.0),
                bodyFatPercent = bodyFatPercent?.coerceIn(3.0, 65.0),
                weeklyTargetSessions = weeklyTargetSessions.coerceIn(1, 7)
            )
            repository.saveProfile(updatedProfile)

            if (weightChanged) {
                repository.logBodyWeight(updatedProfile.bodyWeightKg)
            }
            repository.saveMeasurements(waistCm, chestCm, armCm, recordHistory = false)
            if (measuresChanged || heightChanged || bfChanged) {
                repository.recordMeasurementSnapshot(
                    waistCm = if (measuresChanged) waistCm else null,
                    chestCm = if (measuresChanged) chestCm else null,
                    armCm = if (measuresChanged) armCm else null,
                    heightCm = if (heightChanged) updatedProfile.heightCm else null,
                    bodyFatPercent = if (bfChanged) updatedProfile.bodyFatPercent else null
                )
            }
            showToast("Profile saved successfully")
        }
    }

    fun markFeePaidToday() {
        viewModelScope.launch {
            repository.markFeePaidToday()
            showToast("Gym fee marked paid for today")
        }
    }

    fun logBodyWeight(weightKg: Double) {
        if (weightKg <= 0.0) {
            showToast("Enter a valid weight", ToastTone.WARNING)
            return
        }
        viewModelScope.launch {
            repository.logBodyWeight(weightKg)
            showToast("Body weight logged (${WorkoutMath.formatKg(weightKg)} kg)")
        }
    }

    fun saveProgressPhoto(slot: PhotoSlot, uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveProgressPhoto(slot, uri)
            if (path != null) {
                showToast("${slot.label} progress photo saved locally")
            } else {
                showToast("Could not read selected image", ToastTone.WARNING)
            }
        }
    }

    fun clearProgressPhoto(slot: PhotoSlot) {
        viewModelScope.launch {
            repository.clearProgressPhoto(slot)
            showToast("${slot.label} photo removed", ToastTone.INFO)
        }
    }

    fun clearAllWorkouts() {
        viewModelScope.launch {
            repository.clearAllWorkoutsAndWeights()
            showToast("All workout history cleared", ToastTone.WARNING)
        }
    }

    fun createBackupJson(): String {
        val st = uiState.value
        return repository.exportBackupJson(
            profile = st.profile,
            measures = st.measurements,
            fees = st.gymFee,
            bodyWeights = st.bodyWeightLog,
            sessions = st.sessions,
            customExercises = st.customExercises,
            measurementLog = st.measurementLog
        )
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            runCatching {
                repository.importBackupJson(json)
            }.onSuccess { result ->
                showToast("Imported ${result.sessionCount} workouts successfully")
            }.onFailure { err ->
                showToast(err.message ?: "That file is not a Workout Tracker backup", ToastTone.WARNING)
            }
        }
    }

    fun deleteSessions(sessionIds: List<Long>) {
        viewModelScope.launch {
            repository.deleteSessions(sessionIds)
            showToast("Workout deleted", ToastTone.WARNING)
        }
    }

    // ── Wizard Selection State ──

    fun setWizardDate(dateIso: String) {
        _wizardState.update { it.copy(dateIso = dateIso) }
    }

    fun selectWizardPreset(groups: List<String>) {
        _wizardState.update { current ->
            val alreadySelected = groups.all { it in current.groups }
            current.copy(groups = if (alreadySelected) emptyList() else groups)
        }
    }

    fun toggleWizardGroup(group: String) {
        _wizardState.update { current ->
            val updated = if (group in current.groups) {
                current.groups - group
            } else {
                current.groups + group
            }
            current.copy(groups = updated)
        }
    }

    fun resetWizard() {
        _wizardState.value = WizardSelectionState(dateIso = WorkoutMath.todayIso(), groups = emptyList())
    }

    // ── Live Draft, Edit History & Timer ──

    fun startNewDraft(dateIso: String = WorkoutMath.todayIso(), groups: List<String> = emptyList()) {
        timerJob?.cancel()
        timerAnchorElapsedMs = null
        timerBaseSeconds = 0
        resetWizard()
        _activeDraft.value = ActiveDraft(
            dateIso = dateIso,
            groups = groups,
            exercises = emptyList(),
            sessionNote = "",
            elapsedSeconds = 0,
            editingSessionIds = emptyList(),
            manualDurationMinutes = null
        )
        startTimerIfNeeded()
    }

    fun beginSessionFromWizard(dateIso: String, groups: List<String>) {
        val current = _activeDraft.value
        if (current == null || current.editingSessionIds.isNotEmpty()) {
            timerJob?.cancel()
            timerAnchorElapsedMs = null
            timerBaseSeconds = 0
            _activeDraft.value = ActiveDraft(dateIso = dateIso, groups = groups)
        } else {
            _activeDraft.update { it?.copy(dateIso = dateIso, groups = groups) }
        }
        resetWizard()
        startTimerIfNeeded()
    }

    fun startEditingSession(session: WorkoutSession, sourceIds: List<Long>) {
        timerJob?.cancel()
        timerAnchorElapsedMs = null
        timerBaseSeconds = 0
        val draftExercises = session.exercises.map { ex ->
            DraftExercise(
                localId = UUID.randomUUID().toString(),
                name = ex.name,
                equipment = ex.equipment,
                sets = ex.sets.map { st ->
                    DraftSet(
                        repsText = st.reps.toString(),
                        weightText = if (st.weightKg == 0.0) "0" else WorkoutMath.formatKg(st.weightKg),
                        isCompleted = true
                    )
                }.ifEmpty { listOf(DraftSet("10", "0", true)) },
                rpe = ex.rpe,
                note = ex.note
            )
        }
        _activeDraft.value = ActiveDraft(
            dateIso = session.dateIso,
            groups = session.groups,
            exercises = draftExercises,
            sessionNote = session.note,
            elapsedSeconds = session.durationMinutes * 60,
            editingSessionIds = sourceIds.ifEmpty { listOf(session.id) },
            manualDurationMinutes = session.durationMinutes.takeIf { it > 0 }
        )
    }

    fun updateDraftDate(dateIso: String) {
        _activeDraft.update { it?.copy(dateIso = dateIso) }
    }

    fun updateDraftManualDuration(minsText: String) {
        val digits = minsText.filter { it.isDigit() }.take(3)
        val parsed = if (digits.isEmpty()) null else digits.toIntOrNull()?.coerceIn(0, 600)
        _activeDraft.update { d ->
            if (d == null) null
            else d.copy(
                manualDurationMinutes = parsed,
                elapsedSeconds = (parsed ?: 0) * 60
            )
        }
    }

    private fun currentLiveElapsedSeconds(fallbackSeconds: Int): Int {
        val anchor = timerAnchorElapsedMs ?: return fallbackSeconds
        return computeElapsedSeconds(timerBaseSeconds, anchor, SystemClock.elapsedRealtime())
    }

    private fun startTimerIfNeeded() {
        val initial = _activeDraft.value ?: return
        if (initial.editingSessionIds.isNotEmpty()) return
        if (timerAnchorElapsedMs == null) {
            timerBaseSeconds = initial.elapsedSeconds
            timerAnchorElapsedMs = SystemClock.elapsedRealtime()
        }
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val anchor = timerAnchorElapsedMs ?: break
                val liveSeconds = computeElapsedSeconds(timerBaseSeconds, anchor, SystemClock.elapsedRealtime())
                var stillRunning = false
                _activeDraft.update { d ->
                    if (d == null || d.editingSessionIds.isNotEmpty()) {
                        d
                    } else {
                        stillRunning = true
                        if (d.elapsedSeconds == liveSeconds) d else d.copy(elapsedSeconds = liveSeconds)
                    }
                }
                if (!stillRunning) break
            }
        }
    }

    fun addExerciseToDraft(exerciseName: String, explicitEquipment: String? = null) {
        val st = uiState.value
        val def = ExerciseCatalog.findByName(exerciseName, st.allExercises)
        val current = _activeDraft.value ?: ActiveDraft(
            dateIso = WorkoutMath.todayIso(),
            groups = def?.groups?.take(1) ?: listOf("Push")
        ).also {
            _activeDraft.value = it
            startTimerIfNeeded()
        }

        if (current.exercises.any { it.name.equals(exerciseName, ignoreCase = true) }) {
            showToast("$exerciseName is already in this workout", ToastTone.WARNING)
            return
        }

        val equipment = explicitEquipment ?: def?.equipment ?: "Machine"
        val isCardio = ExerciseCatalog.isCardioExercise(exerciseName, st.allExercises)
        val isBodyweight = !isCardio && def?.isBodyweight == true
        val last = lastSessionOf(exerciseName)

        val initialSets = if (last != null && last.second.sets.isNotEmpty()) {
            val prevEx = last.second
            val bump = if (!isCardio && prevEx.rpe == RpeRating.EASY && !isBodyweight) 2.5 else 0.0
            prevEx.sets.map { s ->
                val targetVal = if (isBodyweight) 0.0 else (s.weightKg + bump)
                DraftSet(
                    repsText = s.reps.toString(),
                    weightText = if (isBodyweight) "0" else WorkoutMath.formatKg(targetVal),
                    isCompleted = false
                )
            }
        } else if (isCardio) {
            val isWalk = exerciseName.contains("walk", ignoreCase = true)
            listOf(
                DraftSet(
                    repsText = if (isWalk) "30" else "20",
                    weightText = if (isWalk) "2.5" else "3",
                    isCompleted = false
                )
            )
        } else {
            val defaultWeight = if (isBodyweight) "0" else "20"
            initialRepsForRange(st.profile.preferredRepRange, count = 3).map { reps ->
                DraftSet(
                    repsText = reps.toString(),
                    weightText = defaultWeight,
                    isCompleted = false
                )
            }
        }

        val newExercise = DraftExercise(
            localId = UUID.randomUUID().toString(),
            name = exerciseName,
            equipment = equipment,
            sets = initialSets
        )
        _activeDraft.update { d ->
            d?.copy(exercises = d.exercises + newExercise)
        }
        showToast("$exerciseName added")
    }

    fun addCustomExerciseAndAddToDraft(
        name: String,
        groups: List<String>,
        equipment: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val targetGroups = groups.ifEmpty {
            _activeDraft.value?.groups?.ifEmpty { listOf("Push") } ?: listOf("Push")
        }
        viewModelScope.launch {
            repository.addCustomExercise(
                name = cleanName,
                groups = targetGroups,
                equipment = equipment,
                allExercises = uiState.value.allExercises
            )
            addExerciseToDraft(cleanName, equipment)
        }
    }

    fun addAiSuggestionToDraft(suggestion: AiExerciseSuggestion) {
        val st = uiState.value
        val existing = ExerciseCatalog.findByName(suggestion.exerciseName, st.allExercises)
        if (existing != null) {
            addExerciseToDraft(existing.name, existing.equipment)
        } else {
            addCustomExerciseAndAddToDraft(
                name = suggestion.exerciseName,
                groups = suggestion.muscleGroups,
                equipment = suggestion.equipment
            )
        }
    }

    fun removeExerciseFromDraft(localId: String) {
        _activeDraft.update { d ->
            d?.copy(exercises = d.exercises.filterNot { it.localId == localId })
        }
    }

    fun addSetToExercise(localId: String) {
        val st = uiState.value
        val allEx = st.allExercises
        val preferredRange = st.profile.preferredRepRange
        _activeDraft.update { d ->
            d?.copy(
                exercises = d.exercises.map { ex ->
                    if (ex.localId != localId) ex
                    else {
                        val isCardio = ExerciseCatalog.isCardioExercise(ex.name, allEx)
                        val template = ex.sets.lastOrNull() ?: DraftSet("10", "20", false)
                        val nextReps = if (isCardio) {
                            template.repsText
                        } else {
                            nextRepForSetCount(ex.sets, preferredRange) ?: template.repsText
                        }
                        ex.copy(sets = ex.sets + template.copy(repsText = nextReps, isCompleted = false))
                    }
                }
            )
        }
    }

    fun removeLastSetFromExercise(localId: String) {
        _activeDraft.update { d ->
            d?.copy(
                exercises = d.exercises.map { ex ->
                    if (ex.localId != localId || ex.sets.size <= 1) ex
                    else ex.copy(sets = ex.sets.dropLast(1))
                }
            )
        }
    }

    fun updateSetReps(localId: String, setIndex: Int, reps: String) {
        val filtered = reps.filter { it.isDigit() }.take(4)
        updateSet(localId, setIndex) { it.copy(repsText = filtered) }
    }

    fun updateSetWeight(localId: String, setIndex: Int, weight: String) {
        val cleaned = buildString {
            var dotSeen = false
            for (ch in weight) {
                if (ch.isDigit()) append(ch)
                else if (ch == '.' && !dotSeen) {
                    dotSeen = true
                    append(ch)
                }
            }
        }.take(6)
        updateSet(localId, setIndex) { it.copy(weightText = cleaned) }
    }

    fun toggleSetDone(localId: String, setIndex: Int) {
        val draft = _activeDraft.value ?: return
        val ex = draft.exercises.firstOrNull { it.localId == localId } ?: return
        val set = ex.sets.getOrNull(setIndex) ?: return
        val nowDone = !set.isCompleted
        updateSet(localId, setIndex) { it.copy(isCompleted = nowDone) }

        val isCardio = ExerciseCatalog.isCardioExercise(ex.name, uiState.value.allExercises)
        if (nowDone && !isCardio) {
            val weight = set.weightText.toDoubleOrNull() ?: 0.0
            val prevPr = personalRecordKg(ex.name)
            if (prevPr > 0.0 && weight > prevPr) {
                showToast("Personal record on ${ex.name} · ${WorkoutMath.formatKg(weight)} kg!")
            }
        }
    }

    private fun updateSet(localId: String, setIndex: Int, transform: (DraftSet) -> DraftSet) {
        _activeDraft.update { d ->
            d?.copy(
                exercises = d.exercises.map { ex ->
                    if (ex.localId != localId) ex
                    else ex.copy(
                        sets = ex.sets.mapIndexed { idx, s ->
                            if (idx == setIndex) transform(s) else s
                        }
                    )
                }
            )
        }
    }

    fun setExerciseRpe(localId: String, rpe: RpeRating) {
        _activeDraft.update { d ->
            d?.copy(
                exercises = d.exercises.map { ex ->
                    if (ex.localId != localId) ex
                    else ex.copy(rpe = if (ex.rpe == rpe) null else rpe)
                }
            )
        }
    }

    fun updateExerciseNote(localId: String, note: String) {
        _activeDraft.update { d ->
            d?.copy(
                exercises = d.exercises.map { ex ->
                    if (ex.localId != localId) ex else ex.copy(note = note)
                }
            )
        }
    }

    fun updateSessionNote(note: String) {
        _activeDraft.update { d -> d?.copy(sessionNote = note) }
    }

    fun discardDraft() {
        val wasEditing = _activeDraft.value?.editingSessionIds?.isNotEmpty() == true
        timerJob?.cancel()
        timerAnchorElapsedMs = null
        timerBaseSeconds = 0
        resetWizard()
        _activeDraft.value = null
        showToast(if (wasEditing) "Edit cancelled" else "Draft discarded", ToastTone.INFO)
    }

    fun finishDraft(onSaved: (wasEditing: Boolean) -> Unit) {
        val rawDraft = _activeDraft.value ?: return
        if (rawDraft.exercises.isEmpty()) {
            showToast("Add at least one exercise before saving", ToastTone.WARNING)
            return
        }
        val wasEditing = rawDraft.editingSessionIds.isNotEmpty()
        val tickedSetsCount = WorkoutMath.draftSetCount(rawDraft, onlyCompleted = !wasEditing)
        if (!wasEditing && tickedSetsCount == 0) {
            showToast("Tap the tick button on at least one completed set before finishing", ToastTone.WARNING)
            return
        }
        val draft = if (wasEditing) {
            rawDraft
        } else {
            rawDraft.copy(elapsedSeconds = currentLiveElapsedSeconds(rawDraft.elapsedSeconds))
        }
        viewModelScope.launch {
            val allEx = uiState.value.allExercises
            val vol = WorkoutMath.draftVolume(draft, allEx, onlyCompleted = !wasEditing)
            val kcal = WorkoutMath.estimateDraftCalories(
                draft = draft,
                bodyWeightKg = uiState.value.profile.bodyWeightKg,
                allExercises = allEx,
                onlyCompleted = !wasEditing
            )
            repository.saveCompletedDraft(draft)
            timerJob?.cancel()
            timerAnchorElapsedMs = null
            timerBaseSeconds = 0
            resetWizard()
            _activeDraft.value = null
            val msg = when {
                wasEditing -> "Workout updated successfully"
                vol > 0.0 -> "Workout saved · ${WorkoutMath.formatVolume(vol)} lifted"
                else -> "Workout saved · ~$kcal kcal est."
            }
            showToast(msg)
            onSaved(wasEditing)
        }
    }

    // ── Gemini AI Exercise Assist ──

    fun askGeminiForExercise(description: String) {
        viewModelScope.launch {
            _aiState.value = AiLookupState.Loading
            val result = aiRepository.identifyExercise(description, uiState.value.allExercises)
            _aiState.value = result.fold(
                onSuccess = { AiLookupState.Success(it) },
                onFailure = { AiLookupState.Error(it.message ?: "AI lookup failed") }
            )
        }
    }

    fun resetAiLookup() {
        _aiState.value = AiLookupState.Idle
    }

    // ── Analytics & History Queries ──

    fun lastSessionOf(exerciseName: String): Pair<String, LoggedExercise>? {
        val sorted = uiState.value.sessions.sortedByDescending { it.dateIso }
        for (s in sorted) {
            val found = s.exercises.firstOrNull { it.name.equals(exerciseName, ignoreCase = true) }
            if (found != null) return s.dateIso to found
        }
        return null
    }

    fun allSessionsOf(exerciseName: String): List<Pair<String, LoggedExercise>> {
        return uiState.value.sessions
            .sortedByDescending { it.dateIso }
            .mapNotNull { s ->
                val found = s.exercises.firstOrNull { it.name.equals(exerciseName, ignoreCase = true) }
                if (found != null) s.dateIso to found else null
            }
    }

    fun personalRecordKg(exerciseName: String): Double {
        if (ExerciseCatalog.isCardioExercise(exerciseName, uiState.value.allExercises)) return 0.0
        return allSessionsOf(exerciseName)
            .flatMap { it.second.sets }
            .maxOfOrNull { it.weightKg } ?: 0.0
    }

    fun sessionsMatchingGroups(groups: List<String>): List<WorkoutSession> {
        val sessions = uiState.value.sessions
        if (groups.isEmpty()) return sessions
        val allEx = uiState.value.allExercises
        return sessions.filter { s ->
            s.groups.any { g -> groups.any { it.equals(g, ignoreCase = true) } } ||
                s.exercises.any { ex ->
                    val def = ExerciseCatalog.findByName(ex.name, allEx)
                    def != null && def.groups.any { g -> groups.any { it.equals(g, ignoreCase = true) } }
                }
        }
    }

    fun suggestedExercisesForGroups(groups: List<String>): List<Pair<ExerciseDefinition, Int>> {
        val st = uiState.value
        val counts = mutableMapOf<String, Int>()
        for (s in st.sessions) {
            for (ex in s.exercises) {
                val def = ExerciseCatalog.findByName(ex.name, st.allExercises) ?: continue
                if (groups.isEmpty() || def.groups.any { g -> groups.any { it.equals(g, ignoreCase = true) } }) {
                    counts[def.name] = (counts[def.name] ?: 0) + 1
                }
            }
        }
        val ranked = counts.entries
            .sortedByDescending { it.value }
            .mapNotNull { entry ->
                ExerciseCatalog.findByName(entry.key, st.allExercises)?.let { it to entry.value }
            }
            .toMutableList()

        if (ranked.size < 12) {
            for (def in st.allExercises) {
                if (groups.isEmpty() || def.groups.any { g -> groups.any { it.equals(g, ignoreCase = true) } }) {
                    if (ranked.none { it.first.name == def.name }) {
                        ranked.add(def to 0)
                    }
                }
                if (ranked.size >= 12) break
            }
        }
        return ranked
    }

    companion object {
        internal fun computeElapsedSeconds(
            baseSeconds: Int,
            anchorElapsedRealtimeMs: Long,
            nowElapsedRealtimeMs: Long
        ): Int {
            val deltaSeconds = ((nowElapsedRealtimeMs - anchorElapsedRealtimeMs).coerceAtLeast(0L) / 1_000L).toInt()
            return (baseSeconds + deltaSeconds).coerceAtLeast(0)
        }

        internal fun initialRepsForRange(preferredRepRange: String, count: Int = 3): List<Int> {
            val parsed = ExerciseCatalog.parseRepRange(preferredRepRange)
            return List(count) { idx ->
                parsed.getOrElse(idx) { parsed.last() }
            }
        }

        internal fun nextRepForSetCount(existingSets: List<DraftSet>, preferredRepRange: String): String? {
            val parsed = ExerciseCatalog.parseRepRange(preferredRepRange)
            val nextIdx = existingSets.size
            return if (nextIdx < parsed.size) parsed[nextIdx].toString() else null
        }

        internal fun nextPyramidReps(existingSets: List<DraftSet>): String? {
            val reps = existingSets.map { it.repsText.trim() }
            return when {
                reps == listOf("15") -> "12"
                reps == listOf("15", "12") -> "10"
                reps == listOf("15", "12", "10") -> "8"
                reps.size == 3 && reps.last() == "10" && reps[1] == "12" -> "8"
                else -> null
            }
        }
    }

    fun suggestedNextSplit(): Pair<SplitPreset, Long?> {
        val today = WorkoutMath.todayIso()
        var bestPreset = ExerciseCatalog.SPLIT_PRESETS.first()
        var longestGap = -1L
        var bestResultGap: Long? = null

        for (preset in ExerciseCatalog.SPLIT_PRESETS) {
            val matching = sessionsMatchingGroups(preset.groups)
            val gap = if (matching.isEmpty()) 999L
            else (WorkoutMath.daysBetween(matching.first().dateIso, today) ?: 0L)
            if (gap > longestGap) {
                longestGap = gap
                bestPreset = preset
                bestResultGap = if (matching.isEmpty()) null else gap
            }
        }
        return bestPreset to bestResultGap
    }
}
