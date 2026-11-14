package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object ExerciseCatalog {

    val MUSCLE_GROUPS = listOf(
        "Push", "Pull", "Chest", "Back", "Shoulders",
        "Biceps", "Triceps", "Legs", "Abs", "Cardio"
    )

    val EQUIPMENT_OPTIONS = listOf(
        "Bar", "Dumbbell", "Machine", "Cable", "Bodyweight"
    )

    val TRAINING_SPLITS = listOf(
        SplitPreset(
            name = "Push / Pull / Legs (PPL)",
            groups = listOf("Push", "Pull", "Legs"),
            description = "Push (chest, shoulders, triceps), Pull (back, biceps), Legs · 3–6 days/week"
        ),
        SplitPreset(
            name = "Upper / Lower",
            groups = listOf("Chest", "Back", "Shoulders", "Legs"),
            description = "Alternate upper-body and lower-body strength days · 4 days/week"
        ),
        SplitPreset(
            name = "Bro Split",
            groups = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs"),
            description = "One focus per day: Chest, Back, Shoulders, Arms, Legs · 5 days/week"
        ),
        SplitPreset(
            name = "Full Body",
            groups = listOf("Chest", "Back", "Legs", "Shoulders"),
            description = "Train all major muscle groups each workout · 2–3 days/week"
        ),
        SplitPreset(
            name = "I will choose this later",
            groups = emptyList(),
            description = "Skip split setup for now — pick any muscle groups when starting a workout"
        )
    )

    val SPLIT_PRESETS = listOf(
        SplitPreset("Push", listOf("Push", "Chest", "Triceps"), "Chest, front & side delts, triceps"),
        SplitPreset("Pull", listOf("Pull", "Back", "Biceps"), "Back, rear delts, biceps"),
        SplitPreset("Legs", listOf("Legs"), "Quads, hamstrings, glutes, calves"),
        SplitPreset("Shoulders + abs", listOf("Shoulders", "Abs"), "Delts, traps, core"),
        SplitPreset("Upper body", listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps"), "Upper push & pull compounds"),
        SplitPreset("Full body", listOf("Chest", "Back", "Legs", "Shoulders"), "Squat, press, hinge & row")
    )

    private fun ex(
        name: String,
        groups: String,
        equipment: String,
        regions: List<String>,
        keywords: String,
        bw: Boolean = false
    ) = ExerciseDefinition(
        name = name,
        groups = groups.split(","),
        equipment = equipment,
        regions = regions,
        keywords = keywords,
        isBodyweight = bw
    )

    val BUILT_IN_EXERCISES: List<ExerciseDefinition> = listOf(
        ex("Barbell bench press", "Push,Chest", "Bar", listOf("chest", "shoulders", "triceps"), "lie flat bench push barbell off chest press"),
        ex("Incline dumbbell press", "Push,Chest", "Dumbbell", listOf("chest", "shoulders"), "bench angled up push dumbbells overhead upper chest"),
        ex("Machine chest press", "Push,Chest", "Machine", listOf("chest", "triceps"), "sit push handles forward away from chest machine"),
        ex("Cable fly", "Push,Chest", "Cable", listOf("chest"), "stand pull two cables together in front hug arc chest stretch"),
        ex("Pec deck", "Push,Chest", "Machine", listOf("chest"), "sit squeeze pads together elbows bent chest machine"),
        ex("Push-up", "Push,Chest", "Bodyweight", listOf("chest", "triceps"), "floor hands push body up bodyweight", bw = true),
        ex("Chest dip", "Push,Chest", "Bodyweight", listOf("chest", "triceps"), "parallel bars lower body lean forward dips", bw = true),

        ex("Lat pulldown", "Pull,Back", "Machine", listOf("back", "biceps"), "sit pull wide bar down to chest overhead cable lats"),
        ex("Seated cable row", "Pull,Back", "Cable", listOf("back", "biceps"), "sit pull handle to stomach horizontal row"),
        ex("Barbell row", "Pull,Back", "Bar", listOf("back", "biceps"), "bend over pull barbell to waist hinge"),
        ex("One-arm dumbbell row", "Pull,Back", "Dumbbell", listOf("back", "biceps"), "knee on bench pull dumbbell up one side"),
        ex("T-bar row", "Pull,Back", "Machine", listOf("back", "biceps"), "chest pad lean pull handles machine row"),
        ex("Straight-arm pulldown", "Pull,Back", "Cable", listOf("back"), "stand push bar down straight arms rope lats"),
        ex("Pull-up", "Pull,Back", "Bodyweight", listOf("back", "biceps"), "hang from bar pull chin over bar", bw = true),
        ex("Chin-up", "Pull,Back,Biceps", "Bodyweight", listOf("back", "biceps"), "hang underhand grip pull up", bw = true),
        ex("Face pull", "Pull,Shoulders", "Cable", listOf("shoulders", "traps"), "pull rope to face elbows high rear delt"),

        ex("Overhead press", "Shoulders,Push", "Bar", listOf("shoulders", "triceps"), "stand press barbell overhead from shoulders"),
        ex("Dumbbell shoulder press", "Shoulders,Push", "Dumbbell", listOf("shoulders", "triceps"), "seated press dumbbells overhead"),
        ex("Lateral raise", "Shoulders", "Dumbbell", listOf("shoulders"), "raise arms out to the sides shoulder width side delt"),
        ex("Cable lateral raise", "Shoulders", "Cable", listOf("shoulders"), "one arm across body raise out to side cable"),
        ex("Rear-delt fly", "Shoulders", "Machine", listOf("shoulders", "traps"), "sit chest against pad open arms backwards reverse fly"),
        ex("Front raise", "Shoulders", "Dumbbell", listOf("shoulders"), "raise dumbbell straight in front to eye level"),
        ex("Upright row", "Shoulders,Back", "Bar", listOf("shoulders", "traps"), "pull bar up along body elbows high"),
        ex("Barbell shrug", "Shoulders,Back", "Bar", listOf("traps"), "hold bar lift shoulders up traps shrug"),

        ex("Barbell curl", "Biceps,Pull", "Bar", listOf("biceps"), "stand curl barbell up bend elbows biceps"),
        ex("Dumbbell curl", "Biceps,Pull", "Dumbbell", listOf("biceps"), "curl dumbbells up alternating biceps"),
        ex("Hammer curl", "Biceps,Pull", "Dumbbell", listOf("biceps", "forearms"), "curl neutral grip thumbs up biceps forearm"),
        ex("Preacher curl", "Biceps", "Machine", listOf("biceps"), "arms on angled pad curl bar biceps isolated"),
        ex("Cable curl", "Biceps", "Cable", listOf("biceps"), "low pulley curl bar up biceps"),

        ex("Triceps pushdown", "Triceps,Push", "Cable", listOf("triceps"), "push rope or bar down elbows tucked triceps"),
        ex("Overhead cable extension", "Triceps", "Cable", listOf("triceps"), "face away press rope overhead behind head extension"),
        ex("Skull crusher", "Triceps", "Bar", listOf("triceps"), "lie down lower bar to forehead extend elbows"),
        ex("Close-grip bench press", "Triceps,Push,Chest", "Bar", listOf("triceps", "chest"), "bench narrow grip press triceps"),
        ex("Triceps dip", "Triceps", "Bodyweight", listOf("triceps"), "bench behind hands dip body down", bw = true),

        ex("Back squat", "Legs", "Bar", listOf("quads", "glutes"), "bar on shoulders squat down stand up legs"),
        ex("Front squat", "Legs", "Bar", listOf("quads"), "bar on front shoulders squat quads"),
        ex("Leg press", "Legs", "Machine", listOf("quads", "glutes"), "sit push sled away with feet machine legs"),
        ex("Hack squat", "Legs", "Machine", listOf("quads"), "angled machine shoulder pads squat"),
        ex("Romanian deadlift", "Legs,Back", "Bar", listOf("hams", "glutes"), "hinge hips lower bar down legs straight hamstring stretch"),
        ex("Leg extension", "Legs", "Machine", listOf("quads"), "sit straighten knees pad on shins quads"),
        ex("Leg curl", "Legs", "Machine", listOf("hams"), "lie or sit curl heels back hamstrings"),
        ex("Walking lunge", "Legs", "Dumbbell", listOf("quads", "glutes"), "step forward lunge alternate legs walking"),
        ex("Bulgarian split squat", "Legs", "Dumbbell", listOf("quads", "glutes"), "back foot on bench single leg squat"),
        ex("Hip thrust", "Legs", "Bar", listOf("glutes"), "shoulders on bench drive hips up glutes"),
        ex("Standing calf raise", "Legs", "Machine", listOf("calves"), "push up onto toes calves"),
        ex("Seated calf raise", "Legs", "Machine", listOf("calves"), "seated knees bent push toes down calves"),

        ex("Cable crunch", "Abs", "Cable", listOf("abs"), "kneel pull rope crunch abs down"),
        ex("Hanging leg raise", "Abs", "Bodyweight", listOf("abs"), "hang from bar lift legs up abs", bw = true),
        ex("Plank", "Abs", "Bodyweight", listOf("abs"), "hold forearms on floor body straight core", bw = true),
        ex("Ab wheel rollout", "Abs", "Bodyweight", listOf("abs"), "roll wheel forward out and back core", bw = true),
        ex("Russian twist", "Abs", "Bodyweight", listOf("abs"), "sit twist side to side core", bw = true),

        ex("Run", "Cardio", "Bodyweight", listOf("heart", "quads", "calves"), "normal run outdoor jog running track road cardio", bw = false),
        ex("Walk", "Cardio", "Bodyweight", listOf("heart", "quads", "calves"), "normal walk walking brisk stroll outdoor cardio", bw = false),
        ex("Treadmill run", "Cardio", "Machine", listOf("heart", "quads"), "running machine belt treadmill run jog cardio", bw = false),
        ex("Stationary bike", "Cardio", "Machine", listOf("heart", "quads"), "cycling bike pedal cardio", bw = false),
        ex("Rowing machine", "Cardio", "Machine", listOf("heart", "back"), "rowing erg pull handle cardio", bw = false),
        ex("Stair climber", "Cardio", "Machine", listOf("heart", "glutes"), "stepper climb stairs cardio", bw = false)
    )

    fun findByName(name: String, custom: List<ExerciseDefinition> = emptyList()): ExerciseDefinition? {
        return (BUILT_IN_EXERCISES + custom).firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun isCardioExercise(name: String, allExercises: List<ExerciseDefinition> = BUILT_IN_EXERCISES): Boolean {
        val def = findByName(name, allExercises.filter { it.isCustom })
        if (def != null) return def.groups.any { it.equals("Cardio", ignoreCase = true) }
        val lower = name.lowercase(Locale.US)
        return lower.contains("run") || lower.contains("walk") || lower.contains("jog") ||
            lower.contains("treadmill") || lower.contains("cycling") || lower.contains("cardio") ||
            lower.contains("stair")
    }

    fun matchOffline(description: String, allExercises: List<ExerciseDefinition>): List<OfflineExerciseMatch> {
        val tokens = description.lowercase(Locale.US)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 }
        if (tokens.isEmpty()) return emptyList()

        val scored = allExercises.mapNotNull { def ->
            val haystack = (
                def.name + " " + def.equipment + " " + def.keywords + " " +
                    def.groups.joinToString(" ") + " " + def.regions.joinToString(" ")
                ).lowercase(Locale.US)
            var score = 0
            for (t in tokens) {
                if (haystack.contains(t)) {
                    score += if (def.name.lowercase(Locale.US).contains(t)) 2 else 1
                }
            }
            if (score > 0) def to score else null
        }.sortedByDescending { it.second }.take(5)

        return scored.mapIndexed { idx, (def, score) ->
            val label = when (idx) {
                0 -> "Closest match"
                1 -> "Also possible"
                else -> "Related"
            }
            OfflineExerciseMatch(def, score, label)
        }
    }
}

data class ClubbedSession(
    val session: WorkoutSession,
    val sourceIds: List<Long>
)

object WorkoutMath {
    private val utcTz: TimeZone = TimeZone.getTimeZone("UTC")

    private fun isoFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = utcTz
        }

    private fun localIsoFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    private fun patternFormat(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = utcTz }

    fun todayIso(): String = localIsoFormat().format(Date())

    private fun parseUtcCalendar(iso: String): Calendar? {
        val date = runCatching { isoFormat().parse(iso.trim()) }.getOrNull() ?: return null
        return Calendar.getInstance(utcTz, Locale.US).apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun isoOffsetDays(days: Int, baseIso: String = todayIso()): String {
        val cal = parseUtcCalendar(baseIso) ?: Calendar.getInstance(utcTz, Locale.US)
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoFormat().format(cal.time)
    }

    fun isoOffsetMonths(months: Int, baseIso: String = todayIso()): String {
        val cal = parseUtcCalendar(baseIso) ?: Calendar.getInstance(utcTz, Locale.US)
        cal.add(Calendar.MONTH, months)
        return isoFormat().format(cal.time)
    }

    fun isoOf(year: Int, month1Based: Int, dayOfMonth: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month1Based, dayOfMonth)

    fun yearMonthDay(iso: String): Triple<Int, Int, Int> {
        val cal = parseUtcCalendar(iso) ?: parseUtcCalendar(todayIso())!!
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun shortWeekBucketLabel(iso: String): String {
        val cal = parseUtcCalendar(iso) ?: return iso
        val monthInitial = patternFormat("MMM").format(cal.time).take(1)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return "$monthInitial$day"
    }

    fun formatShort(iso: String): String =
        parseUtcCalendar(iso)?.let { patternFormat("EEE, MMM d").format(it.time) } ?: iso

    fun formatLong(iso: String): String =
        parseUtcCalendar(iso)?.let { patternFormat("EEEE, d MMM yyyy").format(it.time) } ?: iso

    fun formatDayMonth(iso: String): String =
        parseUtcCalendar(iso)?.let { patternFormat("MMM d").format(it.time) } ?: iso

    fun daysBetween(fromIso: String, toIso: String): Long? {
        val a = parseUtcCalendar(fromIso) ?: return null
        val b = parseUtcCalendar(toIso) ?: return null
        val diffMs = b.timeInMillis - a.timeInMillis
        return diffMs / 86_400_000L
    }

    fun formatAgo(dateIso: String, todayIso: String = todayIso()): String {
        val diff = daysBetween(dateIso, todayIso) ?: return dateIso
        return when {
            diff == 0L -> "today"
            diff == 1L -> "yesterday"
            diff < 0L -> "in ${-diff}d"
            diff < 7L -> "$diff days ago"
            diff < 14L -> "last week"
            else -> "${diff / 7} weeks ago"
        }
    }

    fun exerciseVolume(
        ex: LoggedExercise,
        allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES
    ): Double {
        if (ExerciseCatalog.isCardioExercise(ex.name, allExercises)) return 0.0
        return ex.sets.sumOf { it.reps * it.weightKg }
    }

    fun sessionVolume(
        session: WorkoutSession,
        allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES
    ): Double = session.exercises.sumOf { exerciseVolume(it, allExercises) }

    fun sessionSets(session: WorkoutSession): Int =
        session.exercises.sumOf { it.sets.size }

    fun draftVolume(
        draft: ActiveDraft,
        allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES
    ): Double =
        draft.exercises.sumOf { ex ->
            if (ExerciseCatalog.isCardioExercise(ex.name, allExercises)) 0.0
            else ex.sets.sumOf { st ->
                val r = st.repsText.toIntOrNull() ?: 0
                val w = st.weightText.toDoubleOrNull() ?: 0.0
                r * w
            }
        }

    fun draftSetCount(draft: ActiveDraft): Int =
        draft.exercises.sumOf { it.sets.size }

    fun formatVolume(kg: Double): String =
        if (kg >= 1000.0) String.format(Locale.US, "%.1f t", kg / 1000.0)
        else "${kg.roundToInt()} kg"

    fun formatKg(kg: Double): String =
        if (kg % 1.0 == 0.0) "${kg.toInt()}" else String.format(Locale.US, "%.1f", kg)

    fun estimateCardioCalories(
        exerciseName: String,
        minutes: Int,
        distanceKm: Double,
        bodyWeightKg: Double
    ): Double {
        val weight = if (bodyWeightKg > 20.0) bodyWeightKg else 74.5
        val lower = exerciseName.lowercase(Locale.US)
        val isWalk = lower.contains("walk")
        val isRun = lower.contains("run") || lower.contains("jog")

        if (distanceKm > 0.0) {
            val speedKmh = if (minutes > 0) distanceKm / (minutes / 60.0) else if (isWalk) 5.0 else 9.0
            val factor = when {
                isWalk || (!isRun && speedKmh < 6.4) -> 0.75
                else -> 1.036
            }
            return factor * weight * distanceKm
        }
        if (minutes > 0) {
            val met = when {
                isWalk -> 3.8
                isRun -> 8.5
                else -> 7.0
            }
            return met * 3.5 * weight / 200.0 * minutes
        }
        return 0.0
    }

    fun estimateSessionCalories(
        session: WorkoutSession,
        bodyWeightKg: Double,
        allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES
    ): Int {
        val weight = if (bodyWeightKg > 20.0) bodyWeightKg else 74.5
        var cardioKcal = 0.0
        var cardioMinutes = 0
        var strengthSets = 0

        for (ex in session.exercises) {
            if (ExerciseCatalog.isCardioExercise(ex.name, allExercises)) {
                for (s in ex.sets) {
                    cardioMinutes += s.reps.coerceAtLeast(0)
                    cardioKcal += estimateCardioCalories(ex.name, s.reps, s.weightKg, weight)
                }
            } else {
                strengthSets += ex.sets.size
            }
        }

        val strengthKcal = if (strengthSets > 0) {
            val remainingMins = (session.durationMinutes - cardioMinutes).coerceAtLeast(0)
            if (remainingMins > 0) {
                5.0 * 3.5 * weight / 200.0 * remainingMins
            } else {
                // Estimate from working sets when session duration was not timed
                strengthSets * 4.0 * (weight / 70.0)
            }
        } else if (cardioKcal <= 0.0 && session.durationMinutes > 0) {
            estimateCalories(session.groups, session.durationMinutes, weight).toDouble()
        } else {
            0.0
        }

        return (cardioKcal + strengthKcal).roundToInt()
    }

    fun estimateDraftCalories(
        draft: ActiveDraft,
        bodyWeightKg: Double,
        allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES
    ): Int {
        val syntheticSession = WorkoutSession(
            dateIso = draft.dateIso,
            groups = draft.groups,
            durationMinutes = draft.manualDurationMinutes ?: (draft.elapsedSeconds / 60),
            note = draft.sessionNote,
            exercises = draft.exercises.map { ex ->
                LoggedExercise(
                    name = ex.name,
                    equipment = ex.equipment,
                    sets = ex.sets.map { s ->
                        LoggedSet(
                            reps = s.repsText.toIntOrNull() ?: 0,
                            weightKg = s.weightText.toDoubleOrNull() ?: 0.0,
                            isCompleted = s.isCompleted
                        )
                    },
                    rpe = ex.rpe,
                    note = ex.note
                )
            }
        )
        return estimateSessionCalories(syntheticSession, bodyWeightKg, allExercises)
    }

    fun estimateCalories(groups: List<String>, durationMins: Int, bodyWeightKg: Double): Int {
        if (durationMins <= 0) return 0
        val met = if (groups.any { it.equals("Cardio", ignoreCase = true) }) 7.5 else 5.0
        val weight = if (bodyWeightKg > 20.0) bodyWeightKg else 74.5
        return (met * 3.5 * weight / 200.0 * durationMins).roundToInt()
    }

    fun epley1Rm(reps: Int, kg: Double): Double =
        if (kg <= 0.0) 0.0 else kg * (1.0 + reps / 30.0)

    fun bestSet(ex: LoggedExercise): LoggedSet? =
        ex.sets.maxByOrNull { epley1Rm(it.reps, it.weightKg) }

    fun formatSetSummary(
        ex: LoggedExercise,
        allExercises: List<ExerciseDefinition> = ExerciseCatalog.BUILT_IN_EXERCISES
    ): String {
        val isCardio = ExerciseCatalog.isCardioExercise(ex.name, allExercises)
        if (isCardio) {
            return ex.sets.joinToString(", ") { s ->
                val minsPart = if (s.reps > 0) "${s.reps} min" else ""
                val distPart = if (s.weightKg > 0.0) "${formatKg(s.weightKg)} km" else ""
                listOf(minsPart, distPart).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { "logged" }
            }
        }
        val grouped = linkedMapOf<Pair<Int, Double>, Int>()
        for (s in ex.sets) {
            val key = s.reps to s.weightKg
            grouped[key] = (grouped[key] ?: 0) + 1
        }
        return grouped.entries.joinToString(", ") { (key, count) ->
            val (reps, kg) = key
            val wLabel = if (kg > 0.0) " @ ${formatKg(kg)} kg" else " bw"
            "${count}×$reps$wLabel"
        }
    }

    fun primaryMuscleGroupForStats(
        ex: LoggedExercise,
        session: WorkoutSession,
        allExercises: List<ExerciseDefinition>
    ): String {
        val def = ExerciseCatalog.findByName(ex.name, allExercises)
        val raw = def?.groups?.firstOrNull {
            !it.equals("Push", ignoreCase = true) && !it.equals("Pull", ignoreCase = true)
        } ?: def?.groups?.firstOrNull()
        ?: session.groups.firstOrNull {
            !it.equals("Push", ignoreCase = true) && !it.equals("Pull", ignoreCase = true)
        }
        ?: session.groups.firstOrNull()
        ?: "Other"

        return when {
            raw.equals("Push", ignoreCase = true) -> "Chest"
            raw.equals("Pull", ignoreCase = true) -> "Back"
            else -> raw
        }
    }

    fun clubSessionsByDate(sessions: List<WorkoutSession>): List<ClubbedSession> {
        if (sessions.isEmpty()) return emptyList()
        val byDate = linkedMapOf<String, MutableList<WorkoutSession>>()
        sessions.sortedByDescending { it.dateIso }.forEach { s ->
            byDate.getOrPut(s.dateIso) { mutableListOf() }.add(s)
        }
        return byDate.values.map { dayList ->
            if (dayList.size == 1) {
                val single = dayList.first()
                ClubbedSession(single, listOf(single.id))
            } else {
                val mergedGroups = dayList.flatMap { it.groups }.distinct()
                val mergedMins = dayList.sumOf { it.durationMinutes.coerceAtLeast(0) }
                val mergedNotes = dayList.map { it.note.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(" · ")
                val mergedExercises = dayList.asReversed().flatMap { it.exercises }
                val primaryId = dayList.first().id
                ClubbedSession(
                    session = WorkoutSession(
                        id = primaryId,
                        dateIso = dayList.first().dateIso,
                        groups = mergedGroups,
                        durationMinutes = mergedMins,
                        note = mergedNotes,
                        exercises = mergedExercises
                    ),
                    sourceIds = dayList.map { it.id }
                )
            }
        }
    }

    fun formatTimer(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }
}
