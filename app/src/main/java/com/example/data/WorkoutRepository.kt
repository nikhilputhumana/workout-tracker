package com.example.data

import android.net.Uri
import com.example.domain.ActiveDraft
import com.example.domain.AppearanceMode
import com.example.domain.BodyMeasurements
import com.example.domain.BodyWeightEntry
import com.example.domain.ExerciseCatalog
import com.example.domain.ExerciseDefinition
import com.example.domain.GymFeeRecord
import com.example.domain.LoggedExercise
import com.example.domain.LoggedSet
import com.example.domain.PhotoSlot
import com.example.domain.RpeRating
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.domain.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ImportResult(
    val sessionCount: Int,
    val includedProfile: Boolean
)

class WorkoutRepository(
    private val dao: WorkoutDao,
    private val prefs: PreferencesRepository,
    private val backupManager: BackupManager = BackupManager()
) {
    val sessionsFlow: Flow<List<WorkoutSession>> = dao.observeAllSessions().map { list ->
        list.map { item ->
            WorkoutSession(
                id = item.session.id,
                dateIso = item.session.dateIso,
                groups = item.session.groupsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                durationMinutes = item.session.durationMinutes,
                note = item.session.note,
                exercises = item.exercises
                    .sortedBy { it.exercise.orderIndex }
                    .map { exWithSets ->
                        LoggedExercise(
                            id = exWithSets.exercise.id,
                            name = exWithSets.exercise.name,
                            equipment = exWithSets.exercise.equipment,
                            sets = exWithSets.sets
                                .sortedBy { it.setIndex }
                                .map { s ->
                                    LoggedSet(
                                        id = s.id,
                                        reps = s.reps,
                                        weightKg = s.weightKg,
                                        isCompleted = s.isCompleted
                                    )
                                },
                            rpe = exWithSets.exercise.rpeName?.let {
                                runCatching { RpeRating.valueOf(it) }.getOrNull()
                            },
                            note = exWithSets.exercise.note
                        )
                    }
            )
        }
    }

    val bodyWeightFlow: Flow<List<BodyWeightEntry>> = dao.observeBodyWeightEntries().map { entries ->
        entries.map { BodyWeightEntry(it.id, it.dateIso, it.weightKg) }
    }

    val customExercisesFlow: Flow<List<ExerciseDefinition>> = dao.observeCustomExercises().map { list ->
        list.map { entity ->
            ExerciseDefinition(
                name = entity.name,
                groups = entity.groupsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                equipment = entity.equipment,
                regions = entity.regionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                keywords = entity.keywords,
                isBodyweight = entity.isBodyweight,
                isCustom = true
            )
        }
    }

    val profileFlow: Flow<UserProfile> = prefs.profileFlow
    val measurementsFlow: Flow<BodyMeasurements> = prefs.measurementsFlow
    val gymFeeFlow: Flow<GymFeeRecord> = prefs.gymFeeFlow
    val appearanceFlow: Flow<AppearanceMode> = prefs.appearanceFlow

    suspend fun saveProfile(profile: UserProfile) {
        prefs.saveProfile(profile)
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        prefs.setOnboardingComplete(complete)
    }

    suspend fun logBodyWeight(weightKg: Double, dateIso: String = WorkoutMath.todayIso()) {
        dao.insertBodyWeight(BodyWeightEntity(dateIso = dateIso, weightKg = weightKg))
        prefs.updateBodyWeight(weightKg)
    }

    suspend fun saveMeasurements(waistCm: Double, chestCm: Double, armCm: Double) {
        prefs.saveMeasurements(waistCm, chestCm, armCm)
    }

    suspend fun saveGymFee(record: GymFeeRecord) {
        prefs.saveGymFee(record)
    }

    suspend fun markFeePaidToday() {
        prefs.markFeePaidToday()
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        prefs.setAppearanceMode(mode)
    }

    suspend fun saveProgressPhoto(slot: PhotoSlot, uri: Uri): String? =
        prefs.saveProgressPhoto(slot, uri)

    suspend fun clearProgressPhoto(slot: PhotoSlot) {
        prefs.clearProgressPhoto(slot)
    }

    suspend fun addCustomExercise(
        name: String,
        groups: List<String>,
        equipment: String,
        isBodyweight: Boolean = equipment.equals("Bodyweight", ignoreCase = true),
        allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val existing = ExerciseCatalog.findByName(cleanName, allExercises)
        if (existing != null) return
        val regions = inferRegions(groups)
        dao.insertCustomExercise(
            CustomExerciseEntity(
                name = cleanName,
                groupsCsv = groups.ifEmpty { listOf("Push") }.joinToString(","),
                equipment = equipment.ifBlank { "Custom" },
                regionsCsv = regions.joinToString(","),
                keywords = "$cleanName $equipment ${groups.joinToString(" ")}",
                isBodyweight = isBodyweight
            )
        )
    }

    private fun inferRegions(groups: List<String>): List<String> = buildSet {
        groups.forEach { g ->
            when (g.lowercase()) {
                "push", "chest" -> addAll(listOf("chest", "triceps"))
                "pull", "back" -> addAll(listOf("back", "biceps"))
                "shoulders" -> add("shoulders")
                "biceps" -> add("biceps")
                "triceps" -> add("triceps")
                "legs" -> addAll(listOf("quads", "glutes"))
                "abs" -> add("abs")
                "cardio" -> add("heart")
            }
        }
    }.toList().ifEmpty { listOf("chest") }

    suspend fun saveCompletedDraft(draft: ActiveDraft): Long {
        val newDurationMins = draft.manualDurationMinutes?.coerceAtLeast(0)
            ?: (draft.elapsedSeconds / 60).coerceAtLeast(0)

        val newExPairs = draft.exercises.mapIndexed { idx, ex ->
            val entity = LoggedExerciseEntity(
                sessionId = 0,
                orderIndex = idx,
                name = ex.name,
                equipment = ex.equipment,
                rpeName = ex.rpe?.name,
                note = ex.note.trim()
            )
            val setEntities = ex.sets.mapIndexed { sIdx, s ->
                LoggedSetEntity(
                    loggedExerciseId = 0,
                    setIndex = sIdx,
                    reps = s.repsText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    weightKg = s.weightText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                    isCompleted = s.isCompleted
                )
            }
            entity to setEntities
        }

        if (draft.editingSessionIds.isNotEmpty()) {
            val editSet = draft.editingSessionIds.toSet()
            val otherSameDateSessions = dao.getSessionsByDate(draft.dateIso)
                .filter { it.session.id !in editSet }
            if (otherSameDateSessions.isEmpty()) {
                val sessionEntity = WorkoutSessionEntity(
                    dateIso = draft.dateIso,
                    groupsCsv = draft.groups.joinToString(","),
                    durationMinutes = newDurationMins,
                    note = draft.sessionNote.trim()
                )
                return dao.replaceSessionsByIds(draft.editingSessionIds, sessionEntity, newExPairs)
            } else {
                val existingGroups = otherSameDateSessions.flatMap {
                    it.session.groupsCsv.split(",").map { g -> g.trim() }.filter { g -> g.isNotEmpty() }
                }
                val mergedGroups = (existingGroups + draft.groups).distinct()
                val mergedDuration = otherSameDateSessions.sumOf { it.session.durationMinutes.coerceAtLeast(0) } + newDurationMins
                val mergedNote = (otherSameDateSessions.map { it.session.note.trim() } + draft.sessionNote.trim())
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .joinToString(" · ")
                val existingExPairs = otherSameDateSessions.flatMap { sw ->
                    sw.exercises.sortedBy { it.exercise.orderIndex }.map { exWithSets ->
                        val exEntity = LoggedExerciseEntity(
                            sessionId = 0,
                            orderIndex = 0,
                            name = exWithSets.exercise.name,
                            equipment = exWithSets.exercise.equipment,
                            rpeName = exWithSets.exercise.rpeName,
                            note = exWithSets.exercise.note
                        )
                        val setEntities = exWithSets.sets.sortedBy { it.setIndex }.map { st ->
                            LoggedSetEntity(
                                loggedExerciseId = 0,
                                setIndex = st.setIndex,
                                reps = st.reps,
                                weightKg = st.weightKg,
                                isCompleted = st.isCompleted
                            )
                        }
                        exEntity to setEntities
                    }
                }
                val mergedSession = WorkoutSessionEntity(
                    dateIso = draft.dateIso,
                    groupsCsv = mergedGroups.joinToString(","),
                    durationMinutes = mergedDuration,
                    note = mergedNote
                )
                return dao.replaceSessionsByIds(
                    idsToDelete = (draft.editingSessionIds + otherSameDateSessions.map { it.session.id }).distinct(),
                    session = mergedSession,
                    exercisesWithSets = existingExPairs + newExPairs
                )
            }
        }

        val sameDateSessions = dao.getSessionsByDate(draft.dateIso)
        if (sameDateSessions.isNotEmpty()) {
            val existingGroups = sameDateSessions.flatMap {
                it.session.groupsCsv.split(",").map { g -> g.trim() }.filter { g -> g.isNotEmpty() }
            }
            val mergedGroups = (existingGroups + draft.groups).distinct()
            val mergedDuration = sameDateSessions.sumOf { it.session.durationMinutes.coerceAtLeast(0) } + newDurationMins
            val mergedNote = (sameDateSessions.map { it.session.note.trim() } + draft.sessionNote.trim())
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(" · ")

            val existingExPairs = sameDateSessions.flatMap { sw ->
                sw.exercises.sortedBy { it.exercise.orderIndex }.map { exWithSets ->
                    val exEntity = LoggedExerciseEntity(
                        sessionId = 0,
                        orderIndex = 0,
                        name = exWithSets.exercise.name,
                        equipment = exWithSets.exercise.equipment,
                        rpeName = exWithSets.exercise.rpeName,
                        note = exWithSets.exercise.note
                    )
                    val setEntities = exWithSets.sets.sortedBy { it.setIndex }.map { st ->
                        LoggedSetEntity(
                            loggedExerciseId = 0,
                            setIndex = st.setIndex,
                            reps = st.reps,
                            weightKg = st.weightKg,
                            isCompleted = st.isCompleted
                        )
                    }
                    exEntity to setEntities
                }
            }

            val mergedSession = WorkoutSessionEntity(
                dateIso = draft.dateIso,
                groupsCsv = mergedGroups.joinToString(","),
                durationMinutes = mergedDuration,
                note = mergedNote
            )
            return dao.replaceSessionsByIds(
                idsToDelete = sameDateSessions.map { it.session.id },
                session = mergedSession,
                exercisesWithSets = existingExPairs + newExPairs
            )
        }

        val sessionEntity = WorkoutSessionEntity(
            dateIso = draft.dateIso,
            groupsCsv = draft.groups.joinToString(","),
            durationMinutes = newDurationMins,
            note = draft.sessionNote.trim()
        )
        return dao.insertFullSession(sessionEntity, newExPairs)
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteSessionById(sessionId)
    }

    suspend fun deleteSessions(sessionIds: List<Long>) {
        if (sessionIds.isNotEmpty()) {
            dao.deleteSessionsByIds(sessionIds)
        }
    }

    suspend fun seedSampleData() {
        val sessions = SampleDataSeeder.buildSampleSessions()
        val bw = SampleDataSeeder.buildSampleBodyWeights()
        dao.replaceAllData(sessions, bw, emptyList())
    }

    suspend fun clearAllWorkoutsAndWeights() {
        dao.replaceAllData(emptyList(), emptyList(), emptyList())
    }

    fun exportBackupJson(
        profile: UserProfile,
        measures: BodyMeasurements,
        fees: GymFeeRecord,
        bodyWeights: List<BodyWeightEntry>,
        sessions: List<WorkoutSession>,
        customExercises: List<ExerciseDefinition>
    ): String = backupManager.serialize(profile, measures, fees, bodyWeights, sessions, customExercises)

    suspend fun importBackupJson(json: String): ImportResult {
        val payload = backupManager.deserialize(json)
        val fallbackDate = WorkoutMath.todayIso()
        val sessionEntities = payload.sessions.map { s ->
            val safeDate = s.date.trim().takeIf { WorkoutMath.isValidIsoDate(it) } ?: fallbackDate
            val entity = WorkoutSessionEntity(
                dateIso = safeDate,
                groupsCsv = s.groups.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(","),
                durationMinutes = s.mins.coerceIn(0, 1440),
                note = s.note.trim()
            )
            val exList = s.ex.mapIndexedNotNull { idx, ex ->
                val exName = ex.name.trim()
                if (exName.isEmpty()) return@mapIndexedNotNull null
                val exEntity = LoggedExerciseEntity(
                    sessionId = 0,
                    orderIndex = idx,
                    name = exName,
                    equipment = ex.equipment.trim().ifEmpty { "Machine" },
                    rpeName = backupManager.parseRpe(ex.rpe)?.name,
                    note = ex.note.trim()
                )
                val sets = ex.sets.mapIndexed { sIdx, st ->
                    LoggedSetEntity(
                        loggedExerciseId = 0,
                        setIndex = sIdx,
                        reps = st.reps.coerceIn(0, 9999),
                        weightKg = st.kg.takeIf { it.isFinite() }?.coerceIn(0.0, 2000.0) ?: 0.0,
                        isCompleted = st.completed
                    )
                }
                exEntity to sets
            }
            entity to exList
        }
        val bwEntities = payload.bodyWeightLog.mapNotNull { entry ->
            val safeKg = entry.kg.takeIf { it.isFinite() && it in 20.0..350.0 } ?: return@mapNotNull null
            val safeDate = entry.date.trim().takeIf { WorkoutMath.isValidIsoDate(it) } ?: fallbackDate
            BodyWeightEntity(dateIso = safeDate, weightKg = safeKg)
        }
        val customEntities = payload.customExercises.mapNotNull { item ->
            val cleanName = item.name.trim()
            if (cleanName.isEmpty()) return@mapNotNull null
            CustomExerciseEntity(
                name = cleanName,
                groupsCsv = item.groups.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Push") }.joinToString(","),
                equipment = item.equipment.trim().ifEmpty { "Custom" },
                regionsCsv = item.regions.map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("chest") }.joinToString(","),
                keywords = item.keywords.trim(),
                isBodyweight = item.isBodyweight
            )
        }

        val profile = payload.profile
        if (profile != null || payload.bodyWeightLog.isNotEmpty()) {
            dao.replaceAllData(sessionEntities, bwEntities, customEntities)
        } else {
            dao.replaceSessionsAndCustomExercises(sessionEntities, customEntities)
        }

        if (profile != null && payload.measures != null && payload.fees != null) {
            prefs.restoreFromBackup(
                profile = UserProfile(
                    name = profile.name.trim(),
                    age = profile.age.coerceIn(12, 100),
                    gender = profile.gender.trim().ifEmpty { "Male" },
                    heightCm = profile.height.coerceIn(100, 240),
                    bodyWeightKg = profile.weight.takeIf { it.isFinite() }?.coerceIn(25.0, 300.0) ?: 74.5,
                    bodyFatPercent = profile.bf?.takeIf { it.isFinite() }?.coerceIn(3.0, 65.0),
                    experienceLevel = profile.level.trim().ifEmpty { "Intermediate" },
                    preferredSplit = profile.split.trim().ifEmpty { "Push / Pull / Legs (PPL)" },
                    weeklyTargetSessions = profile.weeklyTarget.coerceIn(1, 7),
                    onboardingComplete = true
                ),
                waistCm = payload.measures.waist.takeIf { it.isFinite() }?.coerceIn(30.0, 250.0) ?: 82.0,
                chestCm = payload.measures.chest.takeIf { it.isFinite() }?.coerceIn(40.0, 250.0) ?: 102.0,
                armCm = payload.measures.arm.takeIf { it.isFinite() }?.coerceIn(15.0, 100.0) ?: 35.0,
                feeRecord = GymFeeRecord(
                    lastPaidIso = payload.fees.last.trim().takeIf { WorkoutMath.isValidIsoDate(it) } ?: fallbackDate,
                    nextDueIso = payload.fees.next.trim().takeIf { WorkoutMath.isValidIsoDate(it) } ?: WorkoutMath.isoOffsetMonths(1, fallbackDate),
                    amount = payload.fees.amount.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 1400.0
                )
            )
        } else if (profile != null) {
            prefs.saveProfile(
                UserProfile(
                    name = profile.name.trim(),
                    age = profile.age.coerceIn(12, 100),
                    gender = profile.gender.trim().ifEmpty { "Male" },
                    heightCm = profile.height.coerceIn(100, 240),
                    bodyWeightKg = profile.weight.takeIf { it.isFinite() }?.coerceIn(25.0, 300.0) ?: 74.5,
                    bodyFatPercent = profile.bf?.takeIf { it.isFinite() }?.coerceIn(3.0, 65.0),
                    experienceLevel = profile.level.trim().ifEmpty { "Intermediate" },
                    preferredSplit = profile.split.trim().ifEmpty { "Push / Pull / Legs (PPL)" },
                    weeklyTargetSessions = profile.weeklyTarget.coerceIn(1, 7),
                    onboardingComplete = true
                )
            )
        } else {
            prefs.setOnboardingComplete(true)
        }
        return ImportResult(sessionCount = sessionEntities.size, includedProfile = profile != null)
    }
}
