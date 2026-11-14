package com.example

import com.example.data.BackupManager
import com.example.domain.BodyMeasurements
import com.example.domain.BodyWeightEntry
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.GymFeeRecord
import com.example.domain.LoggedExercise
import com.example.domain.LoggedSet
import com.example.domain.RpeRating
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun workoutMath_dateAndVolumeCalculations() {
    assertEquals(10L, WorkoutMath.daysBetween("2025-05-01", "2025-05-11"))
    assertEquals("2025-05-11", WorkoutMath.isoOffsetDays(10, "2025-05-01"))
    assertEquals("2025-06-01", WorkoutMath.isoOffsetMonths(1, "2025-05-01"))
    assertEquals(Triple(2025, 5, 11), WorkoutMath.yearMonthDay("2025-05-11"))
    assertEquals("2025-05-09", WorkoutMath.isoOf(2025, 5, 9))
    assertEquals("yesterday", WorkoutMath.formatAgo("2025-05-10", "2025-05-11"))

    val ex = LoggedExercise(
      id = 1L,
      name = "Barbell bench press",
      equipment = "Bar",
      sets = listOf(LoggedSet(reps = 8, weightKg = 70.0), LoggedSet(reps = 8, weightKg = 70.0))
    )
    assertEquals(1120.0, WorkoutMath.exerciseVolume(ex), 0.01)
    assertEquals("1.1 t", WorkoutMath.formatVolume(1120.0))
    assertTrue(WorkoutMath.estimateCalories(listOf("Push"), 60, 75.0) > 350)
  }

  @Test
  fun offlineExerciseMatcher_identifiesDescribedMovements() {
    val matches = ExerciseCatalog.matchOffline(
      "lying on a flat bench pushing dumbbells up for chest",
      ExerciseCatalog.BUILT_IN_EXERCISES
    )
    assertTrue(matches.isNotEmpty())
    assertTrue(matches.any { it.exercise.name.contains("bench", ignoreCase = true) || it.exercise.name.contains("dumbbell", ignoreCase = true) })
  }

  @Test
  fun backupManager_roundTripPreservesData() {
    val backupManager = BackupManager()
    val profile = UserProfile(
      name = "Alex",
      age = 27,
      gender = "Male",
      heightCm = 178,
      bodyWeightKg = 75.0,
      bodyFatPercent = 14.5,
      experienceLevel = "Intermediate",
      preferredSplit = "Push",
      onboardingComplete = true
    )
    val measures = BodyMeasurements(waistCm = 80.0, chestCm = 104.0, armCm = 36.0)
    val fees = GymFeeRecord(lastPaidIso = "2025-05-01", nextDueIso = "2025-06-01", amount = 1400.0)
    val bw = listOf(BodyWeightEntry(1L, "2025-05-01", 75.0))
    val sessions = listOf(
      WorkoutSession(
        id = 1L,
        dateIso = "2025-05-10",
        groups = listOf("Push", "Chest"),
        durationMinutes = 55,
        note = "Felt strong",
        exercises = listOf(
          LoggedExercise(
            id = 1L,
            name = "Barbell bench press",
            equipment = "Bar",
            sets = listOf(LoggedSet(reps = 8, weightKg = 70.0, isCompleted = true)),
            rpe = RpeRating.RIGHT,
            note = "Controlled"
          )
        )
      )
    )
    val custom = listOf(
      ExerciseDefinition(
        name = "Landmine press",
        groups = listOf("Push", "Shoulders"),
        equipment = "Bar",
        regions = listOf("shoulders", "chest"),
        keywords = "angled barbell press",
        isCustom = true
      )
    )

    val json = backupManager.serialize(profile, measures, fees, bw, sessions, custom)
    val restored = backupManager.deserialize(json)

    assertEquals("workout-tracker", restored.app)
    assertEquals("Alex", restored.profile?.name)
    assertEquals(1, restored.sessions.size)
    assertEquals("Barbell bench press", restored.sessions.first().ex.first().name)
    assertEquals("RIGHT", restored.sessions.first().ex.first().rpe)
    assertEquals(1, restored.customExercises.size)
    assertEquals("Landmine press", restored.customExercises.first().name)

    val workoutsOnly = backupManager.deserialize(
      """{"app":"workout-tracker","version":1,"sessions":[{"date":"2026-01-24","groups":["Push"],"mins":50,"ex":[]}]}"""
    )
    assertEquals(null, workoutsOnly.profile)
    assertEquals(1, workoutsOnly.sessions.size)

    val withWeeklyTarget = backupManager.serialize(
      profile.copy(weeklyTargetSessions = 6),
      measures,
      fees,
      bw,
      sessions,
      custom
    )
    assertEquals(6, backupManager.deserialize(withWeeklyTarget).profile?.weeklyTarget)

    val withoutWeeklyTarget = backupManager.deserialize(
      """{"app":"workout-tracker","version":1,"profile":{"name":"Sam","age":25,"gender":"Male","height":175,"weight":74.5},"sessions":[]}"""
    )
    assertEquals(4, withoutWeeklyTarget.profile?.weeklyTarget)
  }

  @Test
  fun trainingSplits_and_cardioAndClubbingBehavior() {
    assertTrue(ExerciseCatalog.TRAINING_SPLITS.any { it.name == "I will choose this later" })
    assertTrue(ExerciseCatalog.BUILT_IN_EXERCISES.any { it.name == "Run" && "Cardio" in it.groups })
    assertTrue(ExerciseCatalog.BUILT_IN_EXERCISES.any { it.name == "Walk" && "Cardio" in it.groups })

    val runEx = LoggedExercise(
      id = 10L,
      name = "Run",
      equipment = "Bodyweight",
      sets = listOf(LoggedSet(reps = 30, weightKg = 5.0))
    )
    assertEquals("30 min · 5 km", WorkoutMath.formatSetSummary(runEx))
    assertEquals(0.0, WorkoutMath.exerciseVolume(runEx), 0.01)
    assertTrue(WorkoutMath.estimateCardioCalories("Run", 30, 5.0, 75.0) > 300.0)
    assertTrue(
      WorkoutMath.estimateCardioCalories("Run", 30, 5.0, 75.0) >
        WorkoutMath.estimateCardioCalories("Walk", 30, 3.0, 75.0)
    )

    val pushOnly = LoggedExercise(id = 1L, name = "Custom Push", equipment = "Bar", sets = listOf(LoggedSet(reps = 10, weightKg = 40.0)))
    assertEquals(
      "Chest",
      WorkoutMath.primaryMuscleGroupForStats(
        pushOnly,
        WorkoutSession(dateIso = "2026-02-01", groups = listOf("Push"), durationMinutes = 0, exercises = listOf(pushOnly)),
        emptyList()
      )
    )
    val pullOnly = LoggedExercise(id = 2L, name = "Custom Pull", equipment = "Cable", sets = listOf(LoggedSet(reps = 10, weightKg = 40.0)))
    assertEquals(
      "Back",
      WorkoutMath.primaryMuscleGroupForStats(
        pullOnly,
        WorkoutSession(dateIso = "2026-02-01", groups = listOf("Pull"), durationMinutes = 0, exercises = listOf(pullOnly)),
        emptyList()
      )
    )

    val s1 = WorkoutSession(
      id = 1L,
      dateIso = "2026-03-01",
      groups = listOf("Push", "Chest"),
      durationMinutes = 0,
      exercises = listOf(LoggedExercise(1L, "Barbell bench press", "Bar", listOf(LoggedSet(reps = 8, weightKg = 60.0))))
    )
    val s2 = WorkoutSession(
      id = 2L,
      dateIso = "2026-03-01",
      groups = listOf("Cardio"),
      durationMinutes = 0,
      exercises = listOf(runEx)
    )
    val clubbed = WorkoutMath.clubSessionsByDate(listOf(s2, s1))
    assertEquals(1, clubbed.size)
    assertEquals(listOf(2L, 1L), clubbed.first().sourceIds)
    assertEquals(2, clubbed.first().session.exercises.size)
    assertEquals(0, clubbed.first().session.durationMinutes)
  }
}
