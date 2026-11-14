package com.example.ai

import android.util.Log
import com.example.domain.AiExerciseSuggestion
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ExerciseAiRepo"

class ExerciseAiRepository {

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "suggestions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "exerciseName" to Schema.string(description = "Standard gym exercise name"),
                        "equipment" to Schema.string(description = "Primary equipment: Bar, Dumbbell, Machine, Cable, or Bodyweight"),
                        "muscleGroups" to Schema.array(
                            items = Schema.string(),
                            description = "Targeted muscle groups such as Chest, Back, Shoulders, Biceps, Triceps, Legs, Abs, Cardio"
                        ),
                        "reason" to Schema.string(description = "Short 1-sentence reason why this exercise fits the user's description")
                    )
                ),
                description = "Top 1 to 3 matching gym exercises"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@ExerciseAiRepository.responseSchema
            }
        )
    }

    suspend fun identifyExercise(
        userDescription: String,
        knownExercises: List<ExerciseDefinition>
    ): Result<List<AiExerciseSuggestion>> {
        val trimmed = userDescription.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Describe the machine or movement first."))
        }

        val catalogNames = knownExercises.joinToString(", ") { "${it.name} (${it.equipment})" }
        val prompt = """
            You are a knowledgeable strength & conditioning coach helping a lifter identify a gym exercise they described.
            Prefer matching exact names from this catalog whenever appropriate, or provide the standard gym name if it is not in the catalog:
            Catalog: $catalogNames

            Lifter's description: "$trimmed"

            Return 1 to 3 best matching exercises ordered from closest match to alternative.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
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
                        ExerciseCatalog.findByName(name)?.groups ?: listOf("Push")
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
