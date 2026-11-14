package com.example.domain

enum class AppearanceMode {
    SYSTEM, LIGHT, DARK
}

enum class RpeRating(val label: String) {
    EASY("Easy"),
    RIGHT("Just right"),
    HARD("Hard")
}

enum class PhotoSlot(val label: String, val fileName: String) {
    FRONT("Front", "progress_front.jpg"),
    SIDE("Side", "progress_side.jpg"),
    BACK("Back", "progress_back.jpg")
}

data class UserProfile(
    val name: String = "",
    val age: Int = 25,
    val gender: String = "Male",
    val heightCm: Int = 175,
    val bodyWeightKg: Double = 74.5,
    val bodyFatPercent: Double? = null,
    val experienceLevel: String = "Intermediate",
    val preferredSplit: String = "Push / Pull / Legs (PPL)",
    val weeklyTargetSessions: Int = 4,
    val onboardingComplete: Boolean = false
)

data class BodyMeasurements(
    val waistCm: Double = 82.0,
    val chestCm: Double = 102.0,
    val armCm: Double = 35.0,
    val frontPhotoPath: String? = null,
    val sidePhotoPath: String? = null,
    val backPhotoPath: String? = null
)

data class GymFeeRecord(
    val lastPaidIso: String = "",
    val nextDueIso: String = "",
    val amount: Double = 1400.0
)

data class LoggedSet(
    val id: Long = 0,
    val reps: Int,
    val weightKg: Double,
    val isCompleted: Boolean = true
)

data class LoggedExercise(
    val id: Long = 0,
    val name: String,
    val equipment: String,
    val sets: List<LoggedSet>,
    val rpe: RpeRating? = null,
    val note: String = ""
)

data class WorkoutSession(
    val id: Long = 0,
    val dateIso: String,
    val groups: List<String>,
    val durationMinutes: Int,
    val note: String = "",
    val exercises: List<LoggedExercise>
)

data class BodyWeightEntry(
    val id: Long = 0,
    val dateIso: String,
    val weightKg: Double
)

data class ExerciseDefinition(
    val name: String,
    val groups: List<String>,
    val equipment: String,
    val regions: List<String>,
    val keywords: String,
    val isBodyweight: Boolean = false,
    val isCustom: Boolean = false,
    val category: String = "strength",
    val instructions: List<String> = emptyList()
)

data class SplitPreset(
    val name: String,
    val groups: List<String>,
    val description: String
)

data class DraftSet(
    val repsText: String,
    val weightText: String,
    val isCompleted: Boolean = false
)

data class DraftExercise(
    val localId: String,
    val name: String,
    val equipment: String,
    val sets: List<DraftSet>,
    val rpe: RpeRating? = null,
    val note: String = ""
)

data class ActiveDraft(
    val dateIso: String,
    val groups: List<String>,
    val exercises: List<DraftExercise> = emptyList(),
    val sessionNote: String = "",
    val elapsedSeconds: Int = 0,
    val editingSessionIds: List<Long> = emptyList(),
    val manualDurationMinutes: Int? = null
)

enum class ToastTone {
    SUCCESS, WARNING, INFO
}

data class ToastBanner(
    val message: String,
    val tone: ToastTone = ToastTone.SUCCESS
)

data class OfflineExerciseMatch(
    val exercise: ExerciseDefinition,
    val score: Int,
    val matchLabel: String
)

data class AiExerciseSuggestion(
    val exerciseName: String,
    val equipment: String,
    val muscleGroups: List<String>,
    val reason: String
)

sealed interface AiLookupState {
    data object Idle : AiLookupState
    data object Loading : AiLookupState
    data class Success(val suggestions: List<AiExerciseSuggestion>) : AiLookupState
    data class Error(val message: String) : AiLookupState
}
