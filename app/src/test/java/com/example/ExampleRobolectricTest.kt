package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ensureFirebaseInitialized
import com.example.domain.ExerciseCatalog
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

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
}
