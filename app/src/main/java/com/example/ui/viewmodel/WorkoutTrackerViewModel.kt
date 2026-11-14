package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
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
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MainUiState(
    val isLoaded: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val measurements: BodyMeasurements = BodyMeasurements(),
    val gymFee: GymFeeRecord = GymFeeRecord(),
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val sessions: List<WorkoutSession> = emptyList(),
    val bodyWeightLog: List<BodyWeightEntry> = emptyList(),
    val customExercises: List<ExerciseDefinition> = emptyList(),
    val allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES,
    val activeDraft: ActiveDraft? = null,
    val aiLookupState: AiLookupState = AiLookupState.Idle,
    val toastMessage: String? = null
)

class WorkoutTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WorkoutDatabase.getInstance(application)
    private val prefsRepo = PreferencesRepository(application)
    private val repository = WorkoutRepository(database.workoutDao(), prefsRepo)
    private val aiRepository = ExerciseAiRepository()

    private val _activeDraft = MutableStateFlow<ActiveDraft?>(null)
    private val _aiState = MutableStateFlow<AiLookupState>(AiLookupState.Idle)
    private val _toast = MutableStateFlow<String?>(null)

    private var timerJob: Job? = null
    private var toastJob: Job? = null

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
            repository.customExercisesFlow
        ) { sessions, bw, custom ->
            Triple(sessions, bw, custom)
        },
        _activeDraft,
        _aiState,
        _toast
    ) { prefsList, dbTriple, draft, aiState, toast ->
        val profile = prefsList[0] as UserProfile
        val measures = prefsList[1] as BodyMeasurements
        val fee = prefsList[2] as GymFeeRecord
        val appearance = prefsList[3] as AppearanceMode
        val (sessions, bw, custom) = dbTriple
        MainUiState(
            isLoaded = true,
            profile = profile,
            measurements = measures,
            gymFee = fee,
            appearanceMode = appearance,
            sessions = sessions,
            bodyWeightLog = bw,
            customExercises = custom,
            allExercises = ExerciseCatalog.BUILT_IN_EXERCISES + custom,
            activeDraft = draft,
            aiLookupState = aiState,
            toastMessage = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(isLoaded = false)
    )

    fun showToast(message: String) {
        toastJob?.cancel()
        _toast.value = message
        toastJob = viewModelScope.launch {
            delay(2_600)
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
        loadSampleData: Boolean
    ) {
        viewModelScope.launch {
            val savedProfile = profile.copy(
                name = profile.name.ifBlank { "Athlete" },
                onboardingComplete = true
            )
            repository.saveProfile(savedProfile)
            if (nextFeeDueIso.isNotBlank()) {
                val currentFee = uiState.value.gymFee
                repository.saveGymFee(currentFee.copy(nextDueIso = nextFeeDueIso))
            }
            if (loadSampleData) {
                repository.seedSampleData()
                showToast("Setup complete · Sample workouts loaded")
            } else {
                if (uiState.value.bodyWeightLog.isEmpty() && savedProfile.bodyWeightKg > 0) {
                    repository.logBodyWeight(savedProfile.bodyWeightKg)
                }
                showToast("You are set up. Tap Create workout to begin.")
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile.copy(onboardingComplete = true))
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

    fun saveGymFee(lastPaidIso: String, nextDueIso: String, amount: Double) {
        viewModelScope.launch {
            repository.saveGymFee(GymFeeRecord(lastPaidIso, nextDueIso, amount))
        }
    }

    fun markFeePaidToday() {
        viewModelScope.launch {
            repository.markFeePaidToday()
            showToast("Fee marked paid for today")
        }
    }

    fun logBodyWeight(weightKg: Double) {
        if (weightKg <= 0.0) {
            showToast("Enter a valid weight")
            return
        }
        viewModelScope.launch {
            repository.logBodyWeight(weightKg)
            showToast("Body weight logged (${WorkoutMath.formatKg(weightKg)} kg)")
        }
    }

    fun saveMeasurements(waistCm: Double, chestCm: Double, armCm: Double) {
        viewModelScope.launch {
            repository.saveMeasurements(waistCm, chestCm, armCm)
        }
    }

    fun saveProgressPhoto(slot: PhotoSlot, uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveProgressPhoto(slot, uri)
            if (path != null) {
                showToast("${slot.label} progress photo saved locally")
            } else {
                showToast("Could not read selected image")
            }
        }
    }

    fun clearProgressPhoto(slot: PhotoSlot) {
        viewModelScope.launch {
            repository.clearProgressPhoto(slot)
            showToast("${slot.label} photo removed")
        }
    }

    fun loadSampleWorkouts() {
        viewModelScope.launch {
            repository.seedSampleData()
            showToast("10 sample workouts loaded")
        }
    }

    fun clearAllWorkouts() {
        viewModelScope.launch {
            repository.clearAllWorkoutsAndWeights()
            showToast("All workout history cleared")
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
            customExercises = st.customExercises
        )
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            runCatching {
                repository.importBackupJson(json)
            }.onSuccess { count ->
                showToast("Imported $count workouts")
            }.onFailure { err ->
                showToast(err.message ?: "That file is not a Workout Tracker backup")
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            showToast("Workout deleted")
        }
    }

    // ── Live Draft & Timer ──

    fun startNewDraft(dateIso: String = WorkoutMath.todayIso(), groups: List<String> = emptyList()) {
        _activeDraft.value = ActiveDraft(
            dateIso = dateIso,
            groups = groups,
            exercises = emptyList(),
            sessionNote = "",
            elapsedSeconds = 0
        )
        startTimerIfNeeded()
    }

    fun beginSessionFromWizard(dateIso: String, groups: List<String>) {
        val current = _activeDraft.value
        _activeDraft.value = if (current == null) {
            ActiveDraft(dateIso = dateIso, groups = groups)
        } else {
            current.copy(dateIso = dateIso, groups = groups)
        }
        startTimerIfNeeded()
    }

    private fun startTimerIfNeeded() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val draft = _activeDraft.value
                if (draft == null) {
                    break
                }
                _activeDraft.value = draft.copy(elapsedSeconds = draft.elapsedSeconds + 1)
            }
        }
    }

    fun addExerciseToDraft(exerciseName: String, explicitEquipment: String? = null) {
        val st = uiState.value
        val current = _activeDraft.value ?: ActiveDraft(
            dateIso = WorkoutMath.todayIso(),
            groups = ExerciseCatalog.findByName(exerciseName, st.customExercises)?.groups?.take(1)
                ?: listOf("Push")
        ).also {
            _activeDraft.value = it
            startTimerIfNeeded()
        }

        if (current.exercises.any { it.name.equals(exerciseName, ignoreCase = true) }) {
            showToast("$exerciseName is already in this workout")
            return
        }

        val def = ExerciseCatalog.findByName(exerciseName, st.customExercises)
        val equipment = explicitEquipment ?: def?.equipment ?: "Machine"
        val isBodyweight = def?.isBodyweight == true
        val last = lastSessionOf(exerciseName)

        val initialSets = if (last != null && last.second.sets.isNotEmpty()) {
            val prevEx = last.second
            val bump = if (prevEx.rpe == RpeRating.EASY && !isBodyweight) 2.5 else 0.0
            prevEx.sets.map { s ->
                val targetKg = if (isBodyweight) 0.0 else (s.weightKg + bump)
                DraftSet(
                    repsText = s.reps.toString(),
                    weightText = if (isBodyweight) "0" else WorkoutMath.formatKg(targetKg),
                    isCompleted = false
                )
            }
        } else {
            listOf(
                DraftSet(
                    repsText = if (isBodyweight) "12" else "10",
                    weightText = if (isBodyweight) "0" else "20",
                    isCompleted = false
                )
            )
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
            repository.addCustomExercise(cleanName, targetGroups, equipment)
            addExerciseToDraft(cleanName, equipment)
        }
    }

    fun addAiSuggestionToDraft(suggestion: AiExerciseSuggestion) {
        val st = uiState.value
        val existing = ExerciseCatalog.findByName(suggestion.exerciseName, st.customExercises)
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
        _activeDraft.update { d ->
            d?.copy(
                exercises = d.exercises.map { ex ->
                    if (ex.localId != localId) ex
                    else {
                        val template = ex.sets.lastOrNull() ?: DraftSet("10", "20", false)
                        ex.copy(sets = ex.sets + template.copy(isCompleted = false))
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

        if (nowDone) {
            val weight = set.weightText.toDoubleOrNull() ?: 0.0
            val prevPr = personalRecordKg(ex.name)
            if (prevPr > 0.0 && weight > prevPr) {
                showToast("Personal record on ${ex.name} · ${WorkoutMath.formatKg(weight)} kg")
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
        timerJob?.cancel()
        _activeDraft.value = null
        showToast("Draft discarded")
    }

    fun finishDraft(onSaved: () -> Unit) {
        val draft = _activeDraft.value ?: return
        if (draft.exercises.isEmpty()) {
            showToast("Add at least one exercise before finishing")
            return
        }
        viewModelScope.launch {
            repository.saveCompletedDraft(draft)
            val vol = WorkoutMath.draftVolume(draft)
            timerJob?.cancel()
            _activeDraft.value = null
            showToast("Workout saved · ${WorkoutMath.formatVolume(vol)} lifted")
            onSaved()
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

        if (ranked.size < 6) {
            for (def in st.allExercises) {
                if (groups.isEmpty() || def.groups.any { g -> groups.any { it.equals(g, ignoreCase = true) } }) {
                    if (ranked.none { it.first.name == def.name }) {
                        ranked.add(def to 0)
                    }
                }
                if (ranked.size >= 8) break
            }
        }
        return ranked
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
