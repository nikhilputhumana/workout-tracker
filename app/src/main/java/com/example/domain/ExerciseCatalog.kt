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

    val SPLIT_PRESETS = listOf(
        SplitPreset("Push", listOf("Push", "Chest", "Triceps"), "Chest, shoulders, triceps"),
        SplitPreset("Pull", listOf("Pull", "Back", "Biceps"), "Back, biceps, rear delts"),
        SplitPreset("Shoulders + abs", listOf("Shoulders", "Abs"), "Delts, traps, core"),
        SplitPreset("Legs", listOf("Legs"), "Quads, hams, glutes, calves")
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

        ex("Treadmill run", "Cardio", "Machine", listOf("heart", "quads"), "running machine belt run walk cardio", bw = true),
        ex("Stationary bike", "Cardio", "Machine", listOf("heart", "quads"), "cycling bike pedal cardio", bw = true),
        ex("Rowing machine", "Cardio", "Machine", listOf("heart", "back"), "rowing erg pull handle cardio", bw = true),
        ex("Stair climber", "Cardio", "Machine", listOf("heart", "glutes"), "stepper climb stairs cardio", bw = true)
    )

    fun findByName(name: String, custom: List<ExerciseDefinition> = emptyList()): ExerciseDefinition? {
        return (BUILT_IN_EXERCISES + custom).firstOrNull { it.name.equals(name, ignoreCase = true) }
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

    fun exerciseVolume(ex: LoggedExercise): Double =
        ex.sets.sumOf { it.reps * it.weightKg }

    fun sessionVolume(session: WorkoutSession): Double =
        session.exercises.sumOf { exerciseVolume(it) }

    fun sessionSets(session: WorkoutSession): Int =
        session.exercises.sumOf { it.sets.size }

    fun draftVolume(draft: ActiveDraft): Double =
        draft.exercises.sumOf { ex ->
            ex.sets.sumOf { st ->
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

    fun estimateCalories(groups: List<String>, durationMins: Int, bodyWeightKg: Double): Int {
        val met = if (groups.any { it.equals("Cardio", ignoreCase = true) }) 7.5 else 5.0
        val weight = if (bodyWeightKg > 20.0) bodyWeightKg else 74.5
        val mins = durationMins.coerceAtLeast(1)
        return (met * 3.5 * weight / 200.0 * mins).roundToInt()
    }

    fun epley1Rm(reps: Int, kg: Double): Double =
        if (kg <= 0.0) 0.0 else kg * (1.0 + reps / 30.0)

    fun bestSet(ex: LoggedExercise): LoggedSet? =
        ex.sets.maxByOrNull { epley1Rm(it.reps, it.weightKg) }

    fun formatSetSummary(ex: LoggedExercise): String {
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

    fun formatTimer(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }
}
