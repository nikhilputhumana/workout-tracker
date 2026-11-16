package com.example

import com.example.ai.ExerciseAiRepository
import com.example.data.BackupManager
import com.example.domain.BodyMeasurements
import com.example.domain.BodyWeightEntry
import com.example.domain.DraftSet
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.GymFeeRecord
import com.example.domain.LoggedExercise
import com.example.domain.LoggedSet
import com.example.domain.RpeRating
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.components.countSessionPrs
import com.example.ui.viewmodel.WorkoutTrackerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    assertEquals("today", WorkoutMath.formatAgo("2025-05-11", "2025-05-11"))
    assertEquals("1 day ago", WorkoutMath.formatAgo("2025-05-10", "2025-05-11"))
    assertEquals("7 days ago", WorkoutMath.formatAgo("2025-05-04", "2025-05-11"))
    assertEquals("21 days ago", WorkoutMath.formatAgo("2025-04-20", "2025-05-11"))

    val ex = LoggedExercise(
      id = 1L,
      name = "Barbell bench press",
      equipment = "Bar",
      sets = listOf(LoggedSet(reps = 8, weightKg = 70.0), LoggedSet(reps = 8, weightKg = 70.0))
    )
    assertEquals(1120.0, WorkoutMath.exerciseVolume(ex), 0.01)
    assertEquals("1.1 t", WorkoutMath.formatVolume(1120.0))
    assertEquals("2 sets × 8 reps · 70 kg", WorkoutMath.formatSetSummary(ex))
    assertTrue(WorkoutMath.estimateCalories(listOf("Push"), 60, 75.0) > 350)

    val pyramidEx = LoggedExercise(
      id = 2L,
      name = "Lat pulldown",
      equipment = "Machine",
      sets = listOf(
        LoggedSet(reps = 15, weightKg = 45.0),
        LoggedSet(reps = 12, weightKg = 45.0),
        LoggedSet(reps = 10, weightKg = 45.0)
      )
    )
    assertEquals("15 / 12 / 10 reps · 45 kg", WorkoutMath.formatSetSummary(pyramidEx))
    assertEquals(listOf("Set 1: 15 reps · 45 kg", "Set 2: 12 reps · 45 kg", "Set 3: 10 reps · 45 kg"), WorkoutMath.formatSetPills(pyramidEx))

    assertEquals("12", WorkoutTrackerViewModel.nextPyramidReps(listOf(DraftSet("15", "20"))))
    assertEquals("10", WorkoutTrackerViewModel.nextPyramidReps(listOf(DraftSet("15", "20"), DraftSet("12", "20"))))
    assertEquals("8", WorkoutTrackerViewModel.nextPyramidReps(listOf(DraftSet("15", "20"), DraftSet("12", "20"), DraftSet("10", "20"))))
    assertNull(WorkoutTrackerViewModel.nextPyramidReps(listOf(DraftSet("21", "10"), DraftSet("21", "10"), DraftSet("21", "10"))))
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

  @Test
  fun workoutMath_isValidIsoDate_validatesStrictCalendarDates() {
    assertTrue(WorkoutMath.isValidIsoDate("2026-02-28"))
    assertTrue(WorkoutMath.isValidIsoDate("2024-02-29"))
    assertFalse(WorkoutMath.isValidIsoDate("2025-02-29"))
    assertFalse(WorkoutMath.isValidIsoDate("2026-02-30"))
    assertFalse(WorkoutMath.isValidIsoDate("2026-13-01"))
    assertFalse(WorkoutMath.isValidIsoDate("not-a-date"))
    assertFalse(WorkoutMath.isValidIsoDate(""))
  }

  @Test
  fun countSessionPrs_excludesCardio_andComparesTrueMaxWeightKg() {
    val priorSession = WorkoutSession(
      id = 1L,
      dateIso = "2026-03-01",
      groups = listOf("Push", "Cardio"),
      durationMinutes = 50,
      exercises = listOf(
        LoggedExercise(1L, "Barbell bench press", "Bar", listOf(LoggedSet(reps = 10, weightKg = 60.0))),
        LoggedExercise(2L, "Run", "Bodyweight", listOf(LoggedSet(reps = 20, weightKg = 3.0)))
      )
    )
    val newSession = WorkoutSession(
      id = 2L,
      dateIso = "2026-03-08",
      groups = listOf("Push", "Cardio"),
      durationMinutes = 55,
      exercises = listOf(
        // Heavy single beats prior 60 kg even though 1RM (65*(1+1/30)=67.17) < 60*(1+10/30)=80.0
        LoggedExercise(3L, "Barbell bench press", "Bar", listOf(LoggedSet(reps = 1, weightKg = 65.0))),
        // Cardio with higher distance must NOT count as strength PR
        LoggedExercise(4L, "Run", "Bodyweight", listOf(LoggedSet(reps = 35, weightKg = 6.5)))
      )
    )

    val prs = countSessionPrs(
      session = newSession,
      allSessions = listOf(priorSession, newSession),
      allExercises = ExerciseCatalog.BUILT_IN_EXERCISES
    )
    assertEquals(1, prs)
  }

  @Test
  fun computeElapsedSeconds_isMonotonicAcrossBackgroundGaps() {
    assertEquals(125, WorkoutTrackerViewModel.computeElapsedSeconds(baseSeconds = 5, anchorElapsedRealtimeMs = 10_000L, nowElapsedRealtimeMs = 130_000L))
    assertEquals(5, WorkoutTrackerViewModel.computeElapsedSeconds(baseSeconds = 5, anchorElapsedRealtimeMs = 10_000L, nowElapsedRealtimeMs = 9_000L))
  }

  @Test
  fun exerciseAiRepository_sanitizeDescription_capsAndNormalizesInput() {
    val noisy = "\n\t  \"Chest\" press\r\nmachine  " + "x".repeat(400)
    val sanitized = ExerciseAiRepository.sanitizeDescription(noisy)
    assertEquals(300, sanitized.length)
    assertFalse(sanitized.contains('"'))
    assertFalse(sanitized.contains('\n'))
    assertFalse(sanitized.contains('\r'))
    assertTrue(sanitized.startsWith("'Chest' press machine"))
  }

  @Test
  fun repRangeParsing_and_tickOnlyDraftMath() {
    assertEquals(listOf(15, 12, 10, 8), ExerciseCatalog.parseRepRange("15-12-10-8"))
    assertEquals(listOf(12, 10, 8, 6), ExerciseCatalog.parseRepRange("12, 10 / 8 - 6"))
    assertEquals(listOf(15, 12, 10, 8), ExerciseCatalog.parseRepRange("abc"))

    assertEquals(listOf(15, 12, 10), WorkoutTrackerViewModel.initialRepsForRange("15-12-10-8", count = 3))
    assertEquals("8", WorkoutTrackerViewModel.nextRepForSetCount(listOf(DraftSet("15", "20"), DraftSet("12", "20"), DraftSet("10", "20")), "15-12-10-8"))
    assertEquals("6", WorkoutTrackerViewModel.nextRepForSetCount(listOf(DraftSet("12", "20"), DraftSet("10", "20"), DraftSet("8", "20")), "12-10-8-6"))

    val draft = com.example.domain.ActiveDraft(
      dateIso = "2026-05-01",
      groups = listOf("Push"),
      exercises = listOf(
        com.example.domain.DraftExercise(
          localId = "1",
          name = "Barbell bench press",
          equipment = "Bar",
          sets = listOf(
            DraftSet("15", "40", isCompleted = true),
            DraftSet("12", "50", isCompleted = false),
            DraftSet("10", "60", isCompleted = false)
          )
        )
      )
    )
    assertEquals(1, WorkoutMath.draftSetCount(draft, onlyCompleted = true))
    assertEquals(600.0, WorkoutMath.draftVolume(draft, ExerciseCatalog.BUILT_IN_EXERCISES, onlyCompleted = true), 0.01)
    assertEquals(3, WorkoutMath.draftSetCount(draft, onlyCompleted = false))
  }

  @Test
  fun backupManager_handlesNullableOptionalProfileAndMeasurementLogs() {
    val backupManager = BackupManager()
    val profile = UserProfile(
      name = "",
      age = null,
      gender = "",
      heightCm = 172,
      bodyWeightKg = 68.5,
      bodyFatPercent = null,
      experienceLevel = "",
      preferredSplit = "Push Pull Legs",
      preferredRepRange = "12-10-8-6",
      onboardingComplete = true
    )
    val measures = BodyMeasurements(waistCm = null, chestCm = 98.0, armCm = null)
    val measureLogs = listOf(
      com.example.domain.BodyMeasurementEntry(
        id = 1L,
        dateIso = "2026-05-01",
        waistCm = null,
        chestCm = 98.0,
        armCm = null,
        heightCm = 172,
        bodyFatPercent = 16.5
      )
    )
    val json = backupManager.serialize(
      profile = profile,
      measures = measures,
      fees = GymFeeRecord(),
      bodyWeightLog = emptyList(),
      sessions = emptyList(),
      customExercises = emptyList(),
      measurementLog = measureLogs
    )
    val restored = backupManager.deserialize(json)
    assertNull(restored.profile?.age)
    assertEquals("", restored.profile?.gender)
    assertEquals("12-10-8-6", restored.profile?.repRange)
    assertNull(restored.measures?.waist)
    assertEquals(98.0, restored.measures?.chest ?: 0.0, 0.01)
    assertEquals(1, restored.measurementLog.size)
    assertEquals(172, restored.measurementLog.first().height)
    assertEquals(16.5, restored.measurementLog.first().bf ?: 0.0, 0.01)
  }

  @Test
  fun workoutMath_bmiAndEpochDateCalculations() {
    assertEquals(20.0, WorkoutMath.calculateBmi(80.0, 200), 0.01)
    assertEquals("Underweight", WorkoutMath.bmiCategory(17.5))
    assertEquals("Normal", WorkoutMath.bmiCategory(22.0))
    assertEquals("Overweight", WorkoutMath.bmiCategory(27.0))
    assertEquals("Obese", WorkoutMath.bmiCategory(32.0))
    assertTrue(WorkoutMath.isValidIsoDate(WorkoutMath.isoFromEpochMillis(1_746_000_000_000L)))
  }
}
