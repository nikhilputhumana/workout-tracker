package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val groupsCsv: String,
    val durationMinutes: Int,
    val note: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "logged_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class LoggedExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val orderIndex: Int,
    val name: String,
    val equipment: String,
    val rpeName: String?,
    val note: String
)

@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = LoggedExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["loggedExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("loggedExerciseId")]
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loggedExerciseId: Long,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Double,
    val isCompleted: Boolean
)

@Entity(tableName = "body_weight_entries")
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val weightKg: Double
)

@Entity(tableName = "custom_exercises")
data class CustomExerciseEntity(
    @PrimaryKey val name: String,
    val groupsCsv: String,
    val equipment: String,
    val regionsCsv: String,
    val keywords: String,
    val isBodyweight: Boolean
)

data class ExerciseWithSets(
    @Embedded val exercise: LoggedExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "loggedExerciseId"
    )
    val sets: List<LoggedSetEntity>
)

data class SessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = LoggedExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exercises: List<ExerciseWithSets>
)
