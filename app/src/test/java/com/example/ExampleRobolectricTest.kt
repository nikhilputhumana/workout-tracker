package com.example

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ExerciseAiRepository
import com.example.ai.ensureFirebaseInitialized
import com.example.data.PreferencesRepository
import com.example.data.WorkoutDatabase
import com.example.data.WorkoutRepository
import com.example.domain.ActiveDraft
import com.example.domain.AppearanceMode
import com.example.domain.DraftExercise
import com.example.domain.DraftSet
import com.example.domain.ExerciseCatalog
import com.example.domain.LoggedExercise
import com.example.domain.LoggedSet
import com.example.domain.PhotoSlot
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import com.example.ui.WorkoutTrackerApp
import com.example.ui.screens.ExerciseLibraryScreen
import com.example.ui.screens.FinishSummaryScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveSessionScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PreviousWorkoutsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.WorkoutTrackerTheme
import com.example.ui.viewmodel.MainUiState
import com.example.ui.viewmodel.WorkoutTrackerViewModel
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Workout Tracker", appName)
  }

  @Test
  fun `bundled offline exercise library loads 800 plus exercises with instructions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val loaded = ExerciseCatalog.loadAssetExercises(context)
    assertTrue("Expected > 800 bundled exercises, got ${loaded.size}", loaded.size > 800)
    assertTrue(loaded.any { it.instructions.isNotEmpty() })
  }

  @Test
  fun `firebase app initializes cleanly with bundled google services`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val initialized = ensureFirebaseInitialized(context)
    assertTrue("Expected FirebaseApp to initialize cleanly", initialized)
    assertTrue(FirebaseApp.getApps(context).isNotEmpty())
  }

  @Test
  fun `ai json parser handles partial fields safely without throwing`() {
    val json = """
      {
        "suggestions": [
          {"exerciseName": "Barbell bench press"},
          {"exerciseName": ""},
          {"exerciseName": "Mystery Cable Pull", "equipment": "", "muscleGroups": ["Back"]}
        ]
      }
    """.trimIndent()
    val parsed = ExerciseAiRepository.parseSuggestionsJson(json, ExerciseCatalog.BUILT_IN_EXERCISES)
    assertEquals(2, parsed.size)
    assertEquals("Barbell bench press", parsed[0].exerciseName)
    assertTrue("Push" in parsed[0].muscleGroups)
    assertEquals("Mystery Cable Pull", parsed[1].exerciseName)
    assertEquals("Machine", parsed[1].equipment)
    assertEquals(listOf("Back"), parsed[1].muscleGroups)
  }

  @Test
  fun `preferences repository preserves existing progress photo when invalid uri fails`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = PreferencesRepository(context)
    val tempSource = File(context.cacheDir, "valid_photo.jpg").apply {
      writeBytes(byteArrayOf(1, 2, 3, 4))
    }
    try {
      val savedPath = prefs.saveProgressPhoto(PhotoSlot.FRONT, Uri.fromFile(tempSource))
      assertNotNull(savedPath)
      assertTrue(File(savedPath!!).exists())

      val missingFile = File(context.cacheDir, "does_not_exist.jpg")
      val failedPath = prefs.saveProgressPhoto(PhotoSlot.FRONT, Uri.fromFile(missingFile))
      assertNull(failedPath)
      assertTrue("Existing photo should not be deleted when replacement copy fails", File(savedPath).exists())
      assertEquals(savedPath, prefs.measurementsFlow.first().frontPhotoPath)
    } finally {
      prefs.clearProgressPhoto(PhotoSlot.FRONT)
      tempSource.delete()
    }
  }

  @Test
  fun `repository clubs edited session when date changes and blocks bundled duplicates`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val prefs = PreferencesRepository(context)
      val repo = WorkoutRepository(db.workoutDao(), prefs)

      val s1Id = repo.saveCompletedDraft(
        ActiveDraft(
          dateIso = "2026-04-01",
          groups = listOf("Push"),
          exercises = listOf(
            DraftExercise("1", "Barbell bench press", "Bar", listOf(DraftSet("10", "60", true)))
          ),
          manualDurationMinutes = null,
          elapsedSeconds = 0
        )
      )
      val s2Id = repo.saveCompletedDraft(
        ActiveDraft(
          dateIso = "2026-04-02",
          groups = listOf("Pull"),
          exercises = listOf(
            DraftExercise("2", "Lat pulldown", "Machine", listOf(DraftSet("12", "45", true)))
          ),
          manualDurationMinutes = 30,
          elapsedSeconds = 1800
        )
      )

      var all = repo.sessionsFlow.first()
      assertEquals(2, all.size)
      assertEquals(0, all.first { it.id == s1Id }.durationMinutes)

      // Move s2 onto 2026-04-01 while clearing duration -> clubs with s1 and preserves cleared duration
      repo.saveCompletedDraft(
        ActiveDraft(
          dateIso = "2026-04-01",
          groups = listOf("Pull"),
          exercises = listOf(
            DraftExercise("2", "Lat pulldown", "Machine", listOf(DraftSet("12", "45", true)))
          ),
          editingSessionIds = listOf(s2Id),
          manualDurationMinutes = null,
          elapsedSeconds = 0
        )
      )

      all = repo.sessionsFlow.first()
      assertEquals(1, all.size)
      assertEquals("2026-04-01", all.first().dateIso)
      assertEquals(2, all.first().exercises.size)
      assertEquals(0, all.first().durationMinutes)

      // Verify custom exercise rejects bundled catalog duplicate
      val bundled = ExerciseCatalog.loadAssetExercises(context)
      val allKnown = (ExerciseCatalog.BUILT_IN_EXERCISES + bundled).distinctBy { it.name.lowercase() }
      val targetName = bundled.first().name
      repo.addCustomExercise(targetName.lowercase(), listOf("Chest"), "Bar", allExercises = allKnown)
      assertTrue(repo.customExercisesFlow.first().isEmpty())

      // Verify import clamps bad dates & out-of-range numbers cleanly
      val badImportJson = """
        {
          "app": "workout-tracker",
          "version": 1,
          "profile": {"name": "Alex", "age": 999, "gender": "Male", "height": 50, "weight": 800.0, "weeklyTarget": 12},
          "sessions": [
            {
              "date": "2026-02-30",
              "groups": ["Push"],
              "mins": -10,
              "ex": [{"name": "Barbell bench press", "equipment": "Bar", "sets": [{"reps": -3, "kg": 99999.0}]}]
            }
          ]
        }
      """.trimIndent()
      val imported = repo.importBackupJson(badImportJson)
      assertEquals(1, imported.sessionCount)
      val restoredProfile = repo.profileFlow.first()
      assertEquals(100, restoredProfile.age)
      assertEquals(100, restoredProfile.heightCm)
      assertEquals(300.0, restoredProfile.bodyWeightKg, 0.01)
      assertEquals(7, restoredProfile.weeklyTargetSessions)

      val restoredSessions = repo.sessionsFlow.first()
      assertTrue(WorkoutMath.isValidIsoDate(restoredSessions.first().dateIso))
      assertEquals(0, restoredSessions.first().durationMinutes)
      assertEquals(0, restoredSessions.first().exercises.first().sets.first().reps)
      assertEquals(2000.0, restoredSessions.first().exercises.first().sets.first().weightKg, 0.01)
    } finally {
      db.close()
    }
  }

  @Test
  fun `exercise library logged by me filter updates reactively when sessions update`() {
    val sessionsState = mutableStateOf<List<WorkoutSession>>(emptyList())
    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        ExerciseLibraryScreen(
          allExercises = ExerciseCatalog.BUILT_IN_EXERCISES,
          sessions = sessionsState.value,
          onOpenDetail = {}
        )
      }
    }

    composeRule.onNodeWithTag("library_filter_logged_by_me").performClick()
    composeRule.onNodeWithText("Barbell bench press").assertDoesNotExist()

    sessionsState.value = listOf(
      WorkoutSession(
        id = 1L,
        dateIso = "2026-04-01",
        groups = listOf("Push"),
        durationMinutes = 40,
        exercises = listOf(
          LoggedExercise(1L, "Barbell bench press", "Bar", listOf(LoggedSet(reps = 8, weightKg = 70.0, isCompleted = true)))
        )
      )
    )
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Barbell bench press").assertIsDisplayed()
  }

  @Test
  fun `onboarding screen validates numeric metrics and blocks invalid submission`() {
    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        OnboardingScreen(
          initialProfile = UserProfile(),
          initialNextFeeIso = "2026-05-01",
          startStep = 2,
          isEditOnly = false,
          onComplete = { _, _, _, _, _ -> }
        )
      }
    }

    // Name and Age are mandatory so Continue is disabled until both are filled
    composeRule.onNodeWithTag("onboarding_continue_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("profile_name_input").performTextInput("Alex")
    composeRule.onNodeWithTag("onboarding_continue_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("profile_age_input").performTextInput("26")
    composeRule.onNodeWithTag("onboarding_continue_button").assertIsEnabled()

    composeRule.onNodeWithTag("onboarding_rep_range_input").performScrollTo()
    composeRule.onNodeWithTag("onboarding_rep_range_input").assertIsDisplayed()
    composeRule.onNodeWithText("15-12-10-8").assertIsDisplayed()
    composeRule.onNodeWithTag("onboarding_rep_range_input").performTextClearance()
    composeRule.onNodeWithTag("onboarding_rep_range_input").performTextInput("12.a \n10\n8")
    composeRule.onNodeWithText("12-10-8").assertIsDisplayed()

    composeRule.onNodeWithTag("profile_age_input").performScrollTo()
    composeRule.onNodeWithTag("profile_age_input").performTextClearance()
    composeRule.onNodeWithText("Required · 12–100").assertIsDisplayed()
    composeRule.onNodeWithTag("onboarding_continue_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("profile_age_input").performTextInput("5")
    composeRule.onNodeWithTag("onboarding_continue_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("profile_age_input").performTextClearance()
    composeRule.onNodeWithTag("profile_age_input").performTextInput("26")
    composeRule.onNodeWithTag("onboarding_continue_button").assertIsEnabled()

    // Required height cannot be cleared
    composeRule.onNodeWithTag("profile_height_input").performScrollTo()
    composeRule.onNodeWithTag("profile_height_input").performTextClearance()
    composeRule.onNodeWithTag("onboarding_continue_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("profile_height_input").performTextInput("176")
    composeRule.onNodeWithTag("onboarding_continue_button").assertIsEnabled()
  }

  @Test
  fun `settings screen shows inline error and updates height weight fat percent with live bmi`() {
    var savedHeight: Int? = null
    var savedWeight: Double? = null
    var savedBf: Double? = null
    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        SettingsScreen(
          uiState = MainUiState(isLoaded = true),
          onEditProfile = {},
          onSaveSettingsChanges = { _, _, _, h, bw, bf, _, _, _, _ ->
            savedHeight = h
            savedWeight = bw
            savedBf = bf
          },
          onMarkFeePaidToday = {},
          onSelectProgressPhoto = { _, _ -> },
          onClearProgressPhoto = {},
          onExportJson = {},
          onImportJson = {},
          onClearAllWorkouts = {},
          onSetAppearance = {},
          onReplayOnboarding = {}
        )
      }
    }

    composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("settings_height_input"))
    composeRule.onNodeWithTag("settings_height_input").performTextClearance()
    composeRule.onNodeWithTag("settings_height_input").performTextInput("200")
    composeRule.onNodeWithTag("settings_weight_input").performTextClearance()
    composeRule.onNodeWithTag("settings_weight_input").performTextInput("80")
    composeRule.onNodeWithTag("settings_body_fat_input").performTextInput("14.5")
    composeRule.onNodeWithText("20.0").assertIsDisplayed()

    composeRule.onNodeWithTag("settings_sticky_save_button").assertIsEnabled().performClick()
    assertEquals(200, savedHeight)
    assertEquals(80.0, savedWeight ?: 0.0, 0.01)
    assertEquals(14.5, savedBf ?: 0.0, 0.01)

    composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("settings_waist_input"))
    composeRule.onNodeWithTag("settings_waist_input").performScrollTo()
    composeRule.onNodeWithTag("settings_waist_input").performTextClearance()
    composeRule.onNodeWithTag("settings_waist_input").performTextInput("5")
    composeRule.onNodeWithText("30–250").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_sticky_save_button").assertIsNotEnabled()
  }

  @Test
  fun `stats screen validates body weight input and toggles volume and calorie modes`() {
    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        StatsScreen(
          uiState = MainUiState(
            isLoaded = true,
            sessions = listOf(
              WorkoutSession(
                id = 1L,
                dateIso = WorkoutMath.todayIso(),
                groups = listOf("Push"),
                durationMinutes = 45,
                exercises = listOf(
                  LoggedExercise(1L, "Barbell bench press", "Bar", listOf(LoggedSet(reps = 10, weightKg = 60.0)))
                )
              )
            )
          ),
          allSessionsOf = { emptyList() },
          onLogBodyWeight = {}
        )
      }
    }

    composeRule.onNodeWithTag("stats_volume_toggle").performClick()
    composeRule.onNodeWithText("Last lifted", ignoreCase = true).assertIsDisplayed()

    composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("stats_bw_input"))
    composeRule.onNodeWithTag("stats_bw_input").performScrollTo()
    composeRule.onNodeWithTag("stats_bw_input").performTextInput("5")
    composeRule.onNodeWithTag("stats_log_bw_button").performClick()
    composeRule.onNodeWithText("Enter 20–350 kg").assertIsDisplayed()
  }

  @Test
  fun `history screen preserves expanded state when sessions list updates`() {
    val s1 = WorkoutSession(
      id = 1L,
      dateIso = "2026-04-01",
      groups = listOf("Push"),
      durationMinutes = 40,
      exercises = listOf(LoggedExercise(1L, "Barbell bench press", "Bar", listOf(LoggedSet(reps = 8, weightKg = 60.0))))
    )
    val s2 = WorkoutSession(
      id = 2L,
      dateIso = "2026-04-02",
      groups = listOf("Pull"),
      durationMinutes = 45,
      exercises = listOf(LoggedExercise(2L, "Lat pulldown", "Machine", listOf(LoggedSet(reps = 10, weightKg = 45.0))))
    )
    val sessionsState = mutableStateOf(listOf(s2, s1))

    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        HistoryScreen(
          sessions = sessionsState.value,
          allExercises = ExerciseCatalog.BUILT_IN_EXERCISES,
          bodyWeightKg = 74.5,
          sessionsMatchingGroups = { sessionsState.value },
          onOpenExerciseDetail = {},
          onEditSession = { _, _ -> },
          onDeleteSessions = {}
        )
      }
    }

    composeRule.onNodeWithTag("history_session_2").performClick()
    composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("history_edit_2"))
    composeRule.onNodeWithTag("history_edit_2").assertIsDisplayed()

    // Deleting s1 removes it from sessions list while s2 stays expanded
    sessionsState.value = listOf(s2)
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("history_edit_2").assertIsDisplayed()
  }

  @Test
  fun `repository saves only ticked sets and tracks body measurement history`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val prefs = PreferencesRepository(context)
      val repo = WorkoutRepository(db.workoutDao(), prefs)

      repo.saveCompletedDraft(
        ActiveDraft(
          dateIso = "2026-05-05",
          groups = listOf("Push"),
          exercises = listOf(
            DraftExercise(
              localId = "1",
              name = "Barbell bench press",
              equipment = "Bar",
              sets = listOf(
                DraftSet("15", "40", isCompleted = true),
                DraftSet("12", "50", isCompleted = false)
              )
            ),
            DraftExercise(
              localId = "2",
              name = "Incline dumbbell press",
              equipment = "Dumbbell",
              sets = listOf(DraftSet("15", "20", isCompleted = false))
            )
          )
        )
      )

      val sessions = repo.sessionsFlow.first()
      assertEquals(1, sessions.size)
      assertEquals(1, sessions.first().exercises.size)
      assertEquals("Barbell bench press", sessions.first().exercises.first().name)
      assertEquals(1, sessions.first().exercises.first().sets.size)
      assertEquals(15, sessions.first().exercises.first().sets.first().reps)

      repo.saveMeasurements(waistCm = 81.5, chestCm = 102.0, armCm = null, recordHistory = true)
      val mLog = repo.measurementLogFlow.first()
      assertEquals(1, mLog.size)
      assertEquals(81.5, mLog.first().waistCm ?: 0.0, 0.01)
      assertEquals(102.0, mLog.first().chestCm ?: 0.0, 0.01)
      assertNull(mLog.first().armCm)
    } finally {
      db.close()
    }
  }

  @Test
  fun `stats screen displays body fat height combined measurements chart and dated progress photos`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempPhoto = File(context.cacheDir, "test_front_stats.jpg").apply {
      writeBytes(byteArrayOf(1, 2, 3, 4))
    }
    try {
      composeRule.setContent {
        WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
          StatsScreen(
            uiState = MainUiState(
              isLoaded = true,
              profile = UserProfile(heightCm = 178, bodyWeightKg = 74.5, bodyFatPercent = 14.2),
              measurements = com.example.domain.BodyMeasurements(
                waistCm = 80.0,
                chestCm = 101.0,
                armCm = 36.5,
                frontPhotoPath = tempPhoto.absolutePath,
                frontPhotoDateIso = "2026-05-02"
              ),
              measurementLog = listOf(
                com.example.domain.BodyMeasurementEntry(1L, "2026-04-01", 82.0, 100.0, 35.0, heightCm = 177, bodyFatPercent = 15.5),
                com.example.domain.BodyMeasurementEntry(2L, "2026-05-01", 80.0, 101.0, 36.5, heightCm = 178, bodyFatPercent = 14.2)
              )
            ),
            allSessionsOf = { emptyList() },
            onLogBodyWeight = {}
          )
        }
      }

      composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("stats_body_fat_card"))
      composeRule.onNodeWithTag("stats_body_fat_card").assertIsDisplayed()
      composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("stats_height_card"))
      composeRule.onNodeWithTag("stats_height_card").assertIsDisplayed()
      composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("stats_body_measurements_card"))
      composeRule.onNodeWithTag("stats_body_measurements_card").assertIsDisplayed()
      composeRule.onNodeWithTag("stats_measurements_multichart").assertIsDisplayed()
      composeRule.onNodeWithTag("stats_measure_pill_waist").assertIsDisplayed()
      composeRule.onNodeWithTag("stats_measure_pill_chest").assertIsDisplayed()
      composeRule.onNodeWithTag("stats_measure_pill_arm").assertIsDisplayed()
      composeRule.onNodeWithText("+1.0 cm").assertIsDisplayed()
      composeRule.onNodeWithText("+1.5 cm").assertIsDisplayed()
      composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("stats_progress_photos_card"))
      composeRule.onNodeWithTag("stats_progress_photos_card").assertIsDisplayed()
      composeRule.onNodeWithTag("stats_photo_front").assertIsDisplayed()
      composeRule.onNodeWithText(WorkoutMath.formatShort("2026-05-02")).assertIsDisplayed()
    } finally {
      tempPhoto.delete()
    }
  }

  @Test
  fun `profile screen omits unset optional body fat summary and leaves empty measurements blank`() {
    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        SettingsScreen(
          uiState = MainUiState(
            isLoaded = true,
            profile = UserProfile(heightCm = 175, bodyWeightKg = 74.5, onboardingComplete = true),
            measurements = com.example.domain.BodyMeasurements(waistCm = null, chestCm = null, armCm = null)
          ),
          onEditProfile = {},
          onSaveSettingsChanges = { _, _, _, _, _, _, _, _, _, _ -> },
          onMarkFeePaidToday = {},
          onSelectProgressPhoto = { _, _ -> },
          onClearProgressPhoto = {},
          onExportJson = {},
          onImportJson = {},
          onClearAllWorkouts = {},
          onSetAppearance = {},
          onReplayOnboarding = {}
        )
      }
    }

    composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("settings_height_input"))
    composeRule.onNodeWithText("Body fat", ignoreCase = true).assertDoesNotExist()
    composeRule.onNodeWithTag("settings_height_input").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_weight_input").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_body_fat_input").assertIsDisplayed()
    composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("settings_save_changes_button"))
    composeRule.onNodeWithTag("settings_save_changes_button").performScrollTo()
    composeRule.onNodeWithText("All profile changes saved").assertIsDisplayed()
  }

  @Test
  fun `previous workouts separates active group into first tab without 8 session cap and other workouts in second tab`() {
    val sessions = (1..10).map { i ->
      val day = i.toString().padStart(2, '0')
      WorkoutSession(
        id = i.toLong(),
        dateIso = "2026-04-$day",
        groups = listOf("Legs", "Push"),
        durationMinutes = 50,
        exercises = listOf(
          LoggedExercise(i * 10L + 1, "Back squat", "Bar", listOf(LoggedSet(reps = 10, weightKg = 80.0))),
          LoggedExercise(i * 10L + 2, "Barbell bench press", "Bar", listOf(LoggedSet(reps = 10, weightKg = 60.0)))
        )
      )
    }

    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        PreviousWorkoutsScreen(
          draftGroups = listOf("Legs"),
          draftExerciseNames = emptySet(),
          allExercises = ExerciseCatalog.BUILT_IN_EXERCISES,
          allSessions = sessions,
          onAddExerciseToDraft = {}
        )
      }
    }

    composeRule.onNodeWithTag("prev_tab_primary").assertIsDisplayed()
    composeRule.onNodeWithText("10 days", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("Barbell bench press").assertDoesNotExist()

    composeRule.onNodeWithTag("prev_tab_other").performClick()
    composeRule.onNodeWithText("Barbell bench press", substring = true).assertIsDisplayed()
  }

  @Test
  fun `live session shows tick reminder banner and finish summary provides go back and finish and save`() {
    var finishClicked = false
    val draft = ActiveDraft(
      dateIso = "2026-05-05",
      groups = listOf("Push"),
      exercises = listOf(
        DraftExercise(
          localId = "1",
          name = "Barbell bench press",
          equipment = "Bar",
          sets = listOf(
            DraftSet("15", "40", isCompleted = false),
            DraftSet("12", "40", isCompleted = false)
          )
        )
      )
    )
    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        LiveSessionScreen(
          draft = draft,
          bodyWeightKg = 75.0,
          preferredRepRange = "15-12-10-8",
          allExercises = ExerciseCatalog.BUILT_IN_EXERCISES,
          suggestedExercises = emptyList(),
          lastSessionOf = { null },
          personalRecordKg = { 0.0 },
          onOpenPrevious = {},
          onOpenLookup = {},
          onOpenAddPicker = {},
          onAddSuggested = {},
          onRemoveExercise = {},
          onAddSet = {},
          onRemoveLastSet = {},
          onUpdateReps = { _, _, _ -> },
          onUpdateWeight = { _, _, _ -> },
          onToggleSetDone = { _, _ -> },
          onSelectRpe = { _, _ -> },
          onUpdateExerciseNote = { _, _ -> },
          onUpdateSessionNote = {},
          onDiscard = {},
          onFinish = { finishClicked = true }
        )
      }
    }

    composeRule.onNodeWithTag("live_session_tick_reminder_banner").assertIsDisplayed()
    composeRule.onNodeWithText("0 sets").assertIsDisplayed()
    composeRule.onNodeWithTag("session_finish_button").performClick()
    assertTrue(finishClicked)
  }

  @Test
  fun `finish summary disables save with zero ticked sets and enables save when ticked`() {
    val untickedDraft = ActiveDraft(
      dateIso = "2026-05-05",
      groups = listOf("Push"),
      exercises = listOf(
        DraftExercise("1", "Barbell bench press", "Bar", listOf(DraftSet("15", "40", isCompleted = false)))
      )
    )
    val draftState = mutableStateOf(untickedDraft)
    var saved = false
    composeRule.setContent {
      WorkoutTrackerTheme(appearanceMode = AppearanceMode.LIGHT) {
        FinishSummaryScreen(
          draft = draftState.value,
          allSessions = emptyList(),
          allExercises = ExerciseCatalog.BUILT_IN_EXERCISES,
          bodyWeightKg = 75.0,
          onGoBack = {},
          onFinishAndSave = { saved = true }
        )
      }
    }

    composeRule.onNodeWithTag("finish_summary_empty_warning").assertIsDisplayed()
    composeRule.onNodeWithTag("finish_summary_go_back_button").assertIsEnabled()
    composeRule.onNodeWithTag("finish_summary_save_button").assertIsNotEnabled()

    draftState.value = untickedDraft.copy(
      exercises = listOf(
        DraftExercise("1", "Barbell bench press", "Bar", listOf(DraftSet("15", "40", isCompleted = true)))
      )
    )
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("finish_summary_save_button").assertIsEnabled().performClick()
    assertTrue(saved)
  }

  @Test
  fun `resume from home after previous workouts and history returns cleanly to live session`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = WorkoutTrackerViewModel(app)
    vm.dismissLaunchSplash()
    vm.completeOnboarding(
      profile = UserProfile(heightCm = 175, bodyWeightKg = 74.5, onboardingComplete = true),
      nextFeeDueIso = WorkoutMath.isoOffsetMonths(1)
    )
    vm.startNewDraft(WorkoutMath.todayIso(), listOf("Legs"))

    composeRule.setContent {
      WorkoutTrackerApp(viewModel = vm)
    }

    composeRule.waitForIdle()
    // Home shows Profile tab & Resume banner
    composeRule.onNodeWithTag("bottom_nav_settings").assertIsDisplayed()
    composeRule.onNodeWithText("Profile").assertIsDisplayed()
    composeRule.onNodeWithTag("home_resume_banner").performClick()
    composeRule.waitForIdle()

    // In Live Session -> open Previous workouts
    composeRule.onNodeWithTag("session_previous_workouts_button").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("prev_tab_primary").assertIsDisplayed()

    // Switch to History tab -> then Home tab -> Resume must open Live Session, NOT trap on Previous workouts
    composeRule.onNodeWithTag("bottom_nav_history").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("bottom_nav_home").performClick()
    composeRule.waitForIdle()

    // Verify we aren't stranded: navigate to Home if 1st tap resumed subscreen, and check Resume returns to Live Session
    if (vm.uiState.value.activeDraft != null) {
      composeRule.onNodeWithTag("bottom_nav_home").performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("home_resume_banner").performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("live_session_tick_reminder_banner").assertIsDisplayed()
      composeRule.onNodeWithTag("session_finish_button").assertIsDisplayed()
    }
  }
}
