package com.example.data

import com.example.domain.BodyMeasurements
import com.example.domain.BodyWeightEntry
import com.example.domain.ExerciseDefinition
import com.example.domain.GymFeeRecord
import com.example.domain.RpeRating
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class BackupSetDto(
    val reps: Int,
    val kg: Double,
    val completed: Boolean = true
)

@JsonClass(generateAdapter = true)
data class BackupExerciseDto(
    val name: String,
    val equipment: String,
    val sets: List<BackupSetDto>,
    val rpe: String? = null,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class BackupSessionDto(
    val date: String,
    val groups: List<String>,
    val mins: Int,
    val note: String = "",
    val ex: List<BackupExerciseDto>
)

@JsonClass(generateAdapter = true)
data class BackupProfileDto(
    val name: String,
    val age: Int,
    val gender: String,
    val height: Int,
    val weight: Double,
    val bf: Double? = null,
    val level: String = "Intermediate",
    val split: String = "Push"
)

@JsonClass(generateAdapter = true)
data class BackupMeasuresDto(
    val waist: Double,
    val chest: Double,
    val arm: Double
)

@JsonClass(generateAdapter = true)
data class BackupFeesDto(
    val last: String,
    val next: String,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class BackupBodyWeightDto(
    val date: String,
    val kg: Double
)

@JsonClass(generateAdapter = true)
data class BackupCustomExerciseDto(
    val name: String,
    val groups: List<String>,
    val equipment: String,
    val regions: List<String>,
    val keywords: String = "",
    val isBodyweight: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BackupPayloadDto(
    val app: String = "workout-tracker",
    val version: Int = 1,
    val exportedAt: String,
    val profile: BackupProfileDto,
    val measures: BackupMeasuresDto,
    val fees: BackupFeesDto,
    val bodyWeightLog: List<BackupBodyWeightDto>,
    val sessions: List<BackupSessionDto>,
    val customExercises: List<BackupCustomExerciseDto> = emptyList()
)

class BackupManager {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupPayloadDto::class.java).indent("  ")

    fun serialize(
        profile: UserProfile,
        measures: BodyMeasurements,
        fees: GymFeeRecord,
        bodyWeightLog: List<BodyWeightEntry>,
        sessions: List<WorkoutSession>,
        customExercises: List<ExerciseDefinition>
    ): String {
        val payload = BackupPayloadDto(
            exportedAt = WorkoutMath.todayIso(),
            profile = BackupProfileDto(
                name = profile.name,
                age = profile.age,
                gender = profile.gender,
                height = profile.heightCm,
                weight = profile.bodyWeightKg,
                bf = profile.bodyFatPercent,
                level = profile.experienceLevel,
                split = profile.preferredSplit
            ),
            measures = BackupMeasuresDto(
                waist = measures.waistCm,
                chest = measures.chestCm,
                arm = measures.armCm
            ),
            fees = BackupFeesDto(
                last = fees.lastPaidIso,
                next = fees.nextDueIso,
                amount = fees.amount
            ),
            bodyWeightLog = bodyWeightLog.map { BackupBodyWeightDto(it.dateIso, it.weightKg) },
            sessions = sessions.map { s ->
                BackupSessionDto(
                    date = s.dateIso,
                    groups = s.groups,
                    mins = s.durationMinutes,
                    note = s.note,
                    ex = s.exercises.map { ex ->
                        BackupExerciseDto(
                            name = ex.name,
                            equipment = ex.equipment,
                            sets = ex.sets.map { BackupSetDto(it.reps, it.weightKg, it.isCompleted) },
                            rpe = ex.rpe?.name,
                            note = ex.note
                        )
                    }
                )
            },
            customExercises = customExercises.map {
                BackupCustomExerciseDto(
                    name = it.name,
                    groups = it.groups,
                    equipment = it.equipment,
                    regions = it.regions,
                    keywords = it.keywords,
                    isBodyweight = it.isBodyweight
                )
            }
        )
        return adapter.toJson(payload)
    }

    fun deserialize(json: String): BackupPayloadDto {
        val parsed = adapter.fromJson(json)
            ?: throw IllegalArgumentException("Empty or invalid backup file.")
        if (parsed.app != "workout-tracker") {
            throw IllegalArgumentException("This file is not a Workout Tracker backup.")
        }
        return parsed
    }

    fun parseRpe(raw: String?): RpeRating? {
        if (raw.isNullOrBlank()) return null
        return when (raw.trim().lowercase()) {
            "easy" -> RpeRating.EASY
            "right", "just right" -> RpeRating.RIGHT
            "hard" -> RpeRating.HARD
            else -> runCatching { RpeRating.valueOf(raw.trim().uppercase()) }.getOrNull()
        }
    }
}
