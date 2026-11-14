package com.example.ai

import android.content.Context
import android.util.Log
import com.example.domain.AiExerciseSuggestion
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "ExerciseAiRepo"

class ExerciseAiRepository(private val context: Context) {

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "suggestions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "exerciseName" to Schema.string(description = "Standard gym exercise name"),
                        "equipment" to Schema.string(description = "Primary equipment: Bar, Dumbbell, Machine, Cable, Kettlebell, Band, or Bodyweight"),
                        "muscleGroups" to Schema.array(
                            items = Schema.string(),
                            description = "Targeted muscle groups such as Chest, Back, Shoulders, Biceps, Triceps, Legs, Abs, Cardio"
                        ),
                        "reason" to Schema.string(description = "Short 1-sentence reason under 15 words why this matches")
                    )
                ),
                description = "Top 1 to 3 matching gym exercises"
            )
        )
    )

    @Volatile
    private var cachedModel: GenerativeModel? = null

    private fun getOrCreateModel(): GenerativeModel? {
        if (!ensureFirebaseInitialized(context)) {
            return null
        }
        configureAppCheckIfNeeded()
        return cachedModel ?: synchronized(this) {
            cachedModel ?: Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = this@ExerciseAiRepository.responseSchema
                }
            ).also { cachedModel = it }
        }
    }

    private fun configureAppCheckIfNeeded() {
        runCatching {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                configureAppCheck(context, null)
            }
        }
    }

    private fun compactCatalogCandidates(
        userDescription: String,
        knownExercises: List<ExerciseDefinition>
    ): String {
        val tokens = userDescription.lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 }
        val scored = if (tokens.isEmpty()) {
            ExerciseCatalog.BUILT_IN_EXERCISES.take(35)
        } else {
            val hits = knownExercises.mapNotNull { def ->
                val hay = "${def.name} ${def.equipment} ${def.groups.joinToString(" ")} ${def.keywords}".lowercase(Locale.US)
                var score = 0
                for (t in tokens) {
                    if (hay.contains(t)) score += if (def.name.lowercase(Locale.US).contains(t)) 3 else 1
                }
                if (score > 0) def to score else null
            }.sortedByDescending { it.second }.map { it.first }.take(35)
            (hits + ExerciseCatalog.BUILT_IN_EXERCISES).distinctBy { it.name.lowercase(Locale.US) }.take(45)
        }
        return scored.joinToString(", ") { "${it.name} (${it.equipment})" }
    }

    suspend fun identifyExercise(
        userDescription: String,
        knownExercises: List<ExerciseDefinition>
    ): Result<List<AiExerciseSuggestion>> {
        val trimmed = userDescription.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Describe the machine or movement first."))
        }

        val activeModel = getOrCreateModel()
            ?: return Result.failure(
                IllegalStateException(
                    "Firebase is not configured in this APK (missing google-services.json). " +
                        "Offline matches below still work."
                )
            )

        val catalogNames = compactCatalogCandidates(trimmed, knownExercises)
        val prompt = """
            Identify the gym exercise described by the lifter.
            Use exact names from Candidates when relevant, or provide the standard gym name:
            Candidates: $catalogNames

            Description: "$trimmed"

            Return 1 to 3 best matches ordered closest first. Keep each reason brief.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                activeModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty AI response.")
            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("suggestions")
            val parsed = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val name = item.getString("exerciseName").trim()
                    val equipment = item.optString("equipment", "Machine").trim()
                    val reason = item.optString("reason", "").trim()
                    val groupsArr = item.optJSONArray("muscleGroups")
                    val groups = buildList {
                        if (groupsArr != null) {
                            for (j in 0 until groupsArr.length()) {
                                val g = groupsArr.getString(j).trim()
                                if (g.isNotEmpty()) add(g)
                            }
                        }
                    }.ifEmpty {
                        ExerciseCatalog.findByName(name, knownExercises)?.groups ?: listOf("Push")
                    }
                    if (name.isNotEmpty()) {
                        add(
                            AiExerciseSuggestion(
                                exerciseName = name,
                                equipment = equipment,
                                muscleGroups = groups,
                                reason = reason
                            )
                        )
                    }
                }
            }
            if (parsed.isEmpty()) {
                Result.failure(IllegalStateException("No matching exercises returned."))
            } else {
                Result.success(parsed)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Exercise identification timed out", e)
            Result.failure(Exception("AI request timed out. You can still pick from offline matches."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI exercise identification failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI identification failed. Check connection."))
        }
    }
}
