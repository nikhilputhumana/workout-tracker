package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY dateIso DESC, id DESC")
    fun observeAllSessions(): Flow<List<SessionWithExercises>>

    @Query("SELECT * FROM body_weight_entries ORDER BY dateIso ASC, id ASC")
    fun observeBodyWeightEntries(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM custom_exercises ORDER BY name ASC")
    fun observeCustomExercises(): Flow<List<CustomExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: LoggedExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<LoggedSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeight(entry: BodyWeightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomExercise(exercise: CustomExerciseEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM body_weight_entries")
    suspend fun clearBodyWeightEntries()

    @Query("DELETE FROM custom_exercises")
    suspend fun clearCustomExercises()

    @Transaction
    suspend fun insertFullSession(
        session: WorkoutSessionEntity,
        exercisesWithSets: List<Pair<LoggedExerciseEntity, List<LoggedSetEntity>>>
    ): Long {
        val sessionId = insertSession(session)
        exercisesWithSets.forEachIndexed { index, (ex, sets) ->
            val exId = insertExercise(ex.copy(sessionId = sessionId, orderIndex = index))
            val boundSets = sets.mapIndexed { sIdx, s ->
                s.copy(loggedExerciseId = exId, setIndex = sIdx)
            }
            insertSets(boundSets)
        }
        return sessionId
    }

    @Transaction
    suspend fun replaceAllData(
        sessions: List<Pair<WorkoutSessionEntity, List<Pair<LoggedExerciseEntity, List<LoggedSetEntity>>>>>,
        bodyWeights: List<BodyWeightEntity>,
        customExercises: List<CustomExerciseEntity>
    ) {
        clearSessions()
        clearBodyWeightEntries()
        clearCustomExercises()
        customExercises.forEach { insertCustomExercise(it) }
        bodyWeights.forEach { insertBodyWeight(it) }
        sessions.forEach { (session, exList) ->
            insertFullSession(session, exList)
        }
    }

    @Transaction
    suspend fun replaceSessionsAndCustomExercises(
        sessions: List<Pair<WorkoutSessionEntity, List<Pair<LoggedExerciseEntity, List<LoggedSetEntity>>>>>,
        customExercises: List<CustomExerciseEntity>
    ) {
        clearSessions()
        clearCustomExercises()
        customExercises.forEach { insertCustomExercise(it) }
        sessions.forEach { (session, exList) ->
            insertFullSession(session, exList)
        }
    }
}
