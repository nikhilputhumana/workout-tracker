package com.example.data

import com.example.domain.ExerciseCatalog
import com.example.domain.RpeRating
import com.example.domain.WorkoutMath

object SampleDataSeeder {

    private fun sets(count: Int, reps: Int, kg: Double): List<LoggedSetEntity> =
        List(count) { idx ->
            LoggedSetEntity(
                loggedExerciseId = 0,
                setIndex = idx,
                reps = reps,
                weightKg = kg,
                isCompleted = true
            )
        }

    private fun ex(
        name: String,
        setList: List<LoggedSetEntity>,
        rpe: RpeRating?,
        note: String = ""
    ): Pair<LoggedExerciseEntity, List<LoggedSetEntity>> {
        val def = ExerciseCatalog.findByName(name)
        return LoggedExerciseEntity(
            sessionId = 0,
            orderIndex = 0,
            name = name,
            equipment = def?.equipment ?: "Machine",
            rpeName = rpe?.name,
            note = note
        ) to setList
    }

    fun buildSampleSessions(): List<Pair<WorkoutSessionEntity, List<Pair<LoggedExerciseEntity, List<LoggedSetEntity>>>>> {
        val todayIso = WorkoutMath.todayIso()
        fun daysAgo(d: Int) = WorkoutMath.isoOffsetDays(-d, todayIso)

        return listOf(
            WorkoutSessionEntity(
                dateIso = daysAgo(2),
                groupsCsv = "Legs",
                durationMinutes = 64,
                note = "Knees felt good. Squat rep 4 was a grind."
            ) to listOf(
                ex(
                    "Back squat",
                    listOf(
                        LoggedSetEntity(loggedExerciseId = 0, setIndex = 0, reps = 8, weightKg = 80.0, isCompleted = true),
                        LoggedSetEntity(loggedExerciseId = 0, setIndex = 1, reps = 8, weightKg = 80.0, isCompleted = true),
                        LoggedSetEntity(loggedExerciseId = 0, setIndex = 2, reps = 7, weightKg = 80.0, isCompleted = true),
                        LoggedSetEntity(loggedExerciseId = 0, setIndex = 3, reps = 6, weightKg = 80.0, isCompleted = true)
                    ),
                    RpeRating.HARD
                ),
                ex("Leg press", sets(3, 12, 140.0), RpeRating.RIGHT),
                ex("Romanian deadlift", sets(3, 10, 70.0), RpeRating.RIGHT),
                ex("Leg extension", sets(3, 15, 45.0), RpeRating.EASY),
                ex("Standing calf raise", sets(4, 15, 60.0), RpeRating.EASY)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(3),
                groupsCsv = "Shoulders,Abs",
                durationMinutes = 52,
                note = ""
            ) to listOf(
                ex("Overhead press", sets(4, 8, 40.0), RpeRating.HARD),
                ex("Lateral raise", sets(4, 12, 10.0), RpeRating.RIGHT),
                ex("Face pull", sets(3, 15, 25.0), RpeRating.EASY),
                ex("Cable crunch", sets(3, 15, 30.0), RpeRating.RIGHT),
                ex("Hanging leg raise", sets(3, 12, 0.0), RpeRating.HARD)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(4),
                groupsCsv = "Pull",
                durationMinutes = 58,
                note = "Straps for the last two row sets."
            ) to listOf(
                ex("Lat pulldown", sets(4, 10, 55.0), RpeRating.RIGHT),
                ex("Seated cable row", sets(4, 10, 60.0), RpeRating.RIGHT),
                ex("One-arm dumbbell row", sets(3, 10, 30.0), RpeRating.EASY),
                ex("Barbell curl", sets(3, 10, 30.0), RpeRating.HARD),
                ex("Hammer curl", sets(3, 12, 14.0), RpeRating.RIGHT)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(5),
                groupsCsv = "Push",
                durationMinutes = 55,
                note = ""
            ) to listOf(
                ex("Barbell bench press", sets(4, 8, 70.0), RpeRating.RIGHT),
                ex("Incline dumbbell press", sets(3, 10, 26.0), RpeRating.HARD),
                ex("Cable fly", sets(3, 15, 15.0), RpeRating.EASY),
                ex("Triceps pushdown", sets(3, 12, 30.0), RpeRating.RIGHT)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(7),
                groupsCsv = "Legs",
                durationMinutes = 60,
                note = ""
            ) to listOf(
                ex("Back squat", sets(4, 8, 77.5), RpeRating.RIGHT),
                ex("Leg press", sets(3, 12, 135.0), RpeRating.RIGHT),
                ex("Romanian deadlift", sets(3, 10, 65.0), RpeRating.EASY),
                ex("Leg curl", sets(3, 12, 40.0), RpeRating.RIGHT),
                ex("Standing calf raise", sets(4, 15, 55.0), RpeRating.EASY)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(9),
                groupsCsv = "Shoulders,Abs",
                durationMinutes = 50,
                note = ""
            ) to listOf(
                ex("Overhead press", sets(4, 8, 37.5), RpeRating.RIGHT),
                ex("Dumbbell shoulder press", sets(3, 10, 20.0), RpeRating.HARD),
                ex("Lateral raise", sets(4, 12, 9.0), RpeRating.RIGHT),
                ex("Cable crunch", sets(3, 15, 27.5), RpeRating.RIGHT),
                ex("Plank", sets(3, 60, 0.0), RpeRating.RIGHT)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(10),
                groupsCsv = "Pull",
                durationMinutes = 56,
                note = ""
            ) to listOf(
                ex("Lat pulldown", sets(4, 10, 52.5), RpeRating.RIGHT),
                ex("Barbell row", sets(4, 10, 55.0), RpeRating.HARD),
                ex("Straight-arm pulldown", sets(3, 12, 25.0), RpeRating.EASY),
                ex("Barbell curl", sets(3, 10, 27.5), RpeRating.RIGHT),
                ex("Preacher curl", sets(3, 12, 20.0), RpeRating.RIGHT)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(11),
                groupsCsv = "Push",
                durationMinutes = 54,
                note = "Shoulder twinge on the last incline set."
            ) to listOf(
                ex("Barbell bench press", sets(4, 8, 67.5), RpeRating.RIGHT),
                ex("Machine chest press", sets(3, 10, 50.0), RpeRating.RIGHT),
                ex("Cable fly", sets(3, 15, 14.0), RpeRating.EASY),
                ex("Overhead cable extension", sets(3, 12, 22.5), RpeRating.HARD)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(13),
                groupsCsv = "Legs",
                durationMinutes = 58,
                note = ""
            ) to listOf(
                ex("Back squat", sets(4, 8, 75.0), RpeRating.RIGHT),
                ex("Leg press", sets(3, 12, 130.0), RpeRating.EASY),
                ex("Walking lunge", sets(3, 12, 16.0), RpeRating.HARD),
                ex("Leg extension", sets(3, 15, 42.5), RpeRating.RIGHT)
            ),
            WorkoutSessionEntity(
                dateIso = daysAgo(16),
                groupsCsv = "Pull,Cardio",
                durationMinutes = 62,
                note = ""
            ) to listOf(
                ex("Lat pulldown", sets(4, 10, 50.0), RpeRating.RIGHT),
                ex("Seated cable row", sets(4, 10, 57.5), RpeRating.RIGHT),
                ex("Dumbbell curl", sets(3, 12, 12.0), RpeRating.RIGHT),
                ex("Treadmill run", listOf(LoggedSetEntity(loggedExerciseId = 0, setIndex = 0, reps = 20, weightKg = 0.0, isCompleted = true)), RpeRating.EASY)
            )
        )
    }

    fun buildSampleBodyWeights(): List<BodyWeightEntity> {
        val todayIso = WorkoutMath.todayIso()
        val offsets = listOf(56 to 76.8, 49 to 76.4, 42 to 76.0, 35 to 75.6, 28 to 75.4, 21 to 75.1, 14 to 74.8, 7 to 74.6, 1 to 74.5)
        return offsets.map { (days, kg) ->
            BodyWeightEntity(dateIso = WorkoutMath.isoOffsetDays(-days, todayIso), weightKg = kg)
        }
    }
}
