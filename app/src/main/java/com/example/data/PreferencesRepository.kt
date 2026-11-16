package com.example.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AppearanceMode
import com.example.domain.BodyMeasurements
import com.example.domain.GymFeeRecord
import com.example.domain.PhotoSlot
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_tracker_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("profile_name")
        val AGE = intPreferencesKey("profile_age")
        val GENDER = stringPreferencesKey("profile_gender")
        val HEIGHT_CM = intPreferencesKey("profile_height_cm")
        val WEIGHT_KG = doublePreferencesKey("profile_weight_kg")
        val BODY_FAT = doublePreferencesKey("profile_body_fat")
        val HAS_BODY_FAT = booleanPreferencesKey("profile_has_body_fat")
        val EXPERIENCE = stringPreferencesKey("profile_experience")
        val SPLIT = stringPreferencesKey("profile_split")
        val REP_RANGE = stringPreferencesKey("profile_rep_range")
        val WEEKLY_TARGET = intPreferencesKey("profile_weekly_target")
        val ONBOARDED = booleanPreferencesKey("onboarding_complete")

        val WAIST_CM = doublePreferencesKey("measure_waist_cm")
        val CHEST_CM = doublePreferencesKey("measure_chest_cm")
        val ARM_CM = doublePreferencesKey("measure_arm_cm")
        val PHOTO_FRONT = stringPreferencesKey("photo_front_path")
        val PHOTO_FRONT_DATE = stringPreferencesKey("photo_front_date")
        val PHOTO_SIDE = stringPreferencesKey("photo_side_path")
        val PHOTO_SIDE_DATE = stringPreferencesKey("photo_side_date")
        val PHOTO_BACK = stringPreferencesKey("photo_back_path")
        val PHOTO_BACK_DATE = stringPreferencesKey("photo_back_date")

        val FEE_LAST_PAID = stringPreferencesKey("fee_last_paid")
        val FEE_NEXT_DUE = stringPreferencesKey("fee_next_due")
        val FEE_AMOUNT = doublePreferencesKey("fee_amount")

        val APPEARANCE = stringPreferencesKey("appearance_mode")
    }

    val profileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val hasBf = prefs[Keys.HAS_BODY_FAT] ?: false
        UserProfile(
            name = prefs[Keys.NAME] ?: "",
            age = prefs[Keys.AGE],
            gender = prefs[Keys.GENDER] ?: "",
            heightCm = prefs[Keys.HEIGHT_CM] ?: 175,
            bodyWeightKg = prefs[Keys.WEIGHT_KG] ?: 74.5,
            bodyFatPercent = if (hasBf) prefs[Keys.BODY_FAT] else null,
            experienceLevel = prefs[Keys.EXPERIENCE] ?: "",
            preferredSplit = prefs[Keys.SPLIT] ?: "Push / Pull / Legs (PPL)",
            preferredRepRange = prefs[Keys.REP_RANGE]?.takeIf { it.isNotBlank() } ?: "15-12-10-8",
            weeklyTargetSessions = (prefs[Keys.WEEKLY_TARGET] ?: 4).coerceIn(1, 7),
            onboardingComplete = prefs[Keys.ONBOARDED] ?: false
        )
    }

    val measurementsFlow: Flow<BodyMeasurements> = context.dataStore.data.map { prefs ->
        fun resolvePhoto(
            pathKey: Preferences.Key<String>,
            dateKey: Preferences.Key<String>
        ): Pair<String?, String?> {
            val path = prefs[pathKey]?.takeIf { File(it).exists() } ?: return null to null
            val stored = prefs[dateKey]?.takeIf { WorkoutMath.isValidIsoDate(it) }
            val resolvedDate = stored ?: WorkoutMath.isoFromEpochMillis(File(path).lastModified())
            return path to resolvedDate
        }
        val (frontPath, frontDate) = resolvePhoto(Keys.PHOTO_FRONT, Keys.PHOTO_FRONT_DATE)
        val (sidePath, sideDate) = resolvePhoto(Keys.PHOTO_SIDE, Keys.PHOTO_SIDE_DATE)
        val (backPath, backDate) = resolvePhoto(Keys.PHOTO_BACK, Keys.PHOTO_BACK_DATE)
        BodyMeasurements(
            waistCm = prefs[Keys.WAIST_CM],
            chestCm = prefs[Keys.CHEST_CM],
            armCm = prefs[Keys.ARM_CM],
            frontPhotoPath = frontPath,
            frontPhotoDateIso = frontDate,
            sidePhotoPath = sidePath,
            sidePhotoDateIso = sideDate,
            backPhotoPath = backPath,
            backPhotoDateIso = backDate
        )
    }

    val gymFeeFlow: Flow<GymFeeRecord> = context.dataStore.data.map { prefs ->
        val todayIso = WorkoutMath.todayIso()
        val defaultLast = WorkoutMath.isoOffsetDays(-20, todayIso)
        val defaultNext = WorkoutMath.isoOffsetDays(10, todayIso)
        GymFeeRecord(
            lastPaidIso = prefs[Keys.FEE_LAST_PAID] ?: defaultLast,
            nextDueIso = prefs[Keys.FEE_NEXT_DUE] ?: defaultNext,
            amount = prefs[Keys.FEE_AMOUNT] ?: 1400.0
        )
    }

    val appearanceFlow: Flow<AppearanceMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.APPEARANCE] ?: AppearanceMode.SYSTEM.name
        runCatching { AppearanceMode.valueOf(raw) }.getOrDefault(AppearanceMode.SYSTEM)
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name.trim()
            if (profile.age != null) {
                prefs[Keys.AGE] = profile.age
            } else {
                prefs.remove(Keys.AGE)
            }
            if (profile.gender.isNotBlank()) {
                prefs[Keys.GENDER] = profile.gender.trim()
            } else {
                prefs.remove(Keys.GENDER)
            }
            prefs[Keys.HEIGHT_CM] = profile.heightCm
            prefs[Keys.WEIGHT_KG] = profile.bodyWeightKg
            if (profile.bodyFatPercent != null) {
                prefs[Keys.HAS_BODY_FAT] = true
                prefs[Keys.BODY_FAT] = profile.bodyFatPercent
            } else {
                prefs[Keys.HAS_BODY_FAT] = false
                prefs.remove(Keys.BODY_FAT)
            }
            if (profile.experienceLevel.isNotBlank()) {
                prefs[Keys.EXPERIENCE] = profile.experienceLevel.trim()
            } else {
                prefs.remove(Keys.EXPERIENCE)
            }
            prefs[Keys.SPLIT] = profile.preferredSplit
            prefs[Keys.REP_RANGE] = profile.preferredRepRange.trim().ifBlank { "15-12-10-8" }
            prefs[Keys.WEEKLY_TARGET] = profile.weeklyTargetSessions.coerceIn(1, 7)
            prefs[Keys.ONBOARDED] = profile.onboardingComplete
        }
    }

    suspend fun updateBodyWeight(weightKg: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WEIGHT_KG] = weightKg
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDED] = complete
        }
    }

    suspend fun saveMeasurements(waistCm: Double?, chestCm: Double?, armCm: Double?) {
        context.dataStore.edit { prefs ->
            if (waistCm != null) prefs[Keys.WAIST_CM] = waistCm else prefs.remove(Keys.WAIST_CM)
            if (chestCm != null) prefs[Keys.CHEST_CM] = chestCm else prefs.remove(Keys.CHEST_CM)
            if (armCm != null) prefs[Keys.ARM_CM] = armCm else prefs.remove(Keys.ARM_CM)
        }
    }

    suspend fun saveGymFee(record: GymFeeRecord) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FEE_LAST_PAID] = record.lastPaidIso
            prefs[Keys.FEE_NEXT_DUE] = record.nextDueIso
            prefs[Keys.FEE_AMOUNT] = record.amount
        }
    }

    suspend fun markFeePaidToday() {
        val todayIso = WorkoutMath.todayIso()
        val nextMonthIso = WorkoutMath.isoOffsetMonths(1, todayIso)
        context.dataStore.edit { prefs ->
            prefs[Keys.FEE_LAST_PAID] = todayIso
            prefs[Keys.FEE_NEXT_DUE] = nextMonthIso
        }
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APPEARANCE] = mode.name
        }
    }

    suspend fun saveProgressPhoto(slot: PhotoSlot, sourceUri: Uri): String? =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "progress_photos").apply { mkdirs() }
            val dest = File(dir, "${slot.name.lowercase()}_${System.currentTimeMillis()}.jpg")

            val copied = runCatching {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    true
                } ?: false
            }.getOrDefault(false)

            if (!copied || !dest.exists() || dest.length() == 0L) {
                dest.delete()
                return@withContext null
            }

            dir.listFiles()
                ?.filter { it.name.startsWith("${slot.name.lowercase()}_") && it.absolutePath != dest.absolutePath }
                ?.forEach { it.delete() }

            val savedIso = WorkoutMath.todayIso()
            context.dataStore.edit { prefs ->
                when (slot) {
                    PhotoSlot.FRONT -> {
                        prefs[Keys.PHOTO_FRONT] = dest.absolutePath
                        prefs[Keys.PHOTO_FRONT_DATE] = savedIso
                    }
                    PhotoSlot.SIDE -> {
                        prefs[Keys.PHOTO_SIDE] = dest.absolutePath
                        prefs[Keys.PHOTO_SIDE_DATE] = savedIso
                    }
                    PhotoSlot.BACK -> {
                        prefs[Keys.PHOTO_BACK] = dest.absolutePath
                        prefs[Keys.PHOTO_BACK_DATE] = savedIso
                    }
                }
            }
            dest.absolutePath
        }

    suspend fun clearProgressPhoto(slot: PhotoSlot) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "progress_photos")
        dir.listFiles()?.filter { it.name.startsWith("${slot.name.lowercase()}_") }
            ?.forEach { it.delete() }
        context.dataStore.edit { prefs ->
            when (slot) {
                PhotoSlot.FRONT -> {
                    prefs.remove(Keys.PHOTO_FRONT)
                    prefs.remove(Keys.PHOTO_FRONT_DATE)
                }
                PhotoSlot.SIDE -> {
                    prefs.remove(Keys.PHOTO_SIDE)
                    prefs.remove(Keys.PHOTO_SIDE_DATE)
                }
                PhotoSlot.BACK -> {
                    prefs.remove(Keys.PHOTO_BACK)
                    prefs.remove(Keys.PHOTO_BACK_DATE)
                }
            }
        }
    }

    suspend fun restoreFromBackup(
        profile: UserProfile,
        waistCm: Double?,
        chestCm: Double?,
        armCm: Double?,
        feeRecord: GymFeeRecord
    ) {
        saveProfile(profile.copy(onboardingComplete = true))
        saveMeasurements(waistCm, chestCm, armCm)
        saveGymFee(feeRecord.copy(
            lastPaidIso = feeRecord.lastPaidIso.ifBlank { WorkoutMath.todayIso() },
            nextDueIso = feeRecord.nextDueIso.ifBlank { WorkoutMath.isoOffsetMonths(1) }
        ))
    }
}
