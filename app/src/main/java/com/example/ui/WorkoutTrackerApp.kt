package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.AppearanceMode
import com.example.domain.ExerciseCatalog
import com.example.domain.ToastBanner
import com.example.domain.ToastTone
import com.example.domain.UserProfile
import com.example.domain.WorkoutMath
import com.example.ui.screens.ExerciseDetailScreen
import com.example.ui.screens.ExerciseLibraryScreen
import com.example.ui.screens.ExercisePickerScreen
import com.example.ui.screens.FoodPreviewScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveSessionScreen
import com.example.ui.screens.NewWorkoutScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PreviousWorkoutsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.LocalWorkoutPalette
import com.example.ui.theme.WorkoutTrackerTheme
import com.example.ui.viewmodel.WorkoutTrackerViewModel
import kotlinx.coroutines.delay

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val LIBRARY = "library"
    const val STATS = "stats"
    const val FOOD = "food"
    const val SETTINGS = "settings"
    const val NEW_WORKOUT = "new_workout"
    const val LIVE_SESSION = "live_session"
    const val EXERCISE_PICKER = "exercise_picker/{tab}"
    const val PREVIOUS_WORKOUTS = "previous_workouts"
    const val EXERCISE_DETAIL = "exercise_detail/{name}"
    const val EDIT_PROFILE = "edit_profile"

    fun picker(tab: Int = 0) = "exercise_picker/$tab"
    fun detail(name: String) = "exercise_detail/${Uri.encode(name)}"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MAIN_TABS = listOf(
    BottomNavTab(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavTab(Routes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
    BottomNavTab(Routes.LIBRARY, "Exercises", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    BottomNavTab(Routes.STATS, "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavTab(Routes.FOOD, "Food", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    BottomNavTab(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun WorkoutTrackerApp(
    viewModel: WorkoutTrackerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingOnboardingImport by remember { mutableStateOf<Pair<UserProfile, String>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = viewModel.createBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Cannot open destination file")
            }.onSuccess {
                viewModel.showToast("Exported ${uiState.sessions.size} workouts")
            }.onFailure {
                viewModel.showToast(
                    "Export failed: ${it.localizedMessage ?: "error"}",
                    ToastTone.WARNING
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val onboardingArgs = pendingOnboardingImport
        pendingOnboardingImport = null
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: error("Cannot read selected file")
            }.onSuccess { json ->
                if (onboardingArgs != null) {
                    viewModel.importBackupDuringOnboarding(
                        json = json,
                        pendingProfile = onboardingArgs.first,
                        nextFeeDueIso = onboardingArgs.second
                    )
                } else {
                    viewModel.importBackupJson(json)
                }
            }.onFailure {
                viewModel.showToast("Import failed", ToastTone.WARNING)
            }
        }
    }

    LaunchedEffect(uiState.showLaunchSplash) {
        if (uiState.showLaunchSplash) {
            delay(1050)
            viewModel.dismissLaunchSplash()
        }
    }

    WorkoutTrackerTheme(appearanceMode = uiState.appearanceMode) {
        val palette = LocalWorkoutPalette.current

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !uiState.isLoaded -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(palette.appBg),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = palette.brand)
                    }
                }

                !uiState.profile.onboardingComplete -> {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        containerColor = palette.appBg
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            OnboardingScreen(
                                initialProfile = uiState.profile,
                                initialNextFeeIso = uiState.gymFee.nextDueIso,
                                startStep = 1,
                                isEditOnly = false,
                                onComplete = { profile, nextDue ->
                                    viewModel.completeOnboarding(profile, nextDue)
                                },
                                onImportLog = { profile, nextDue ->
                                    pendingOnboardingImport = profile to nextDue
                                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                }
                            )
                            FloatingBannerHost(
                                banner = uiState.toastBanner,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                            )
                        }
                    }
                }

                else -> {
                    val navController = rememberNavController()
                    val backEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backEntry?.destination?.route ?: Routes.HOME
                    var historyExpandId by remember { mutableStateOf<Long?>(null) }
                    var lastWorkoutRoute by remember { mutableStateOf<String?>(null) }
                    var pausedToActualHome by remember { mutableStateOf(false) }

                    val topLevelRoutes = remember { MAIN_TABS.map { it.route }.toSet() }
                    val isTopLevel = currentRoute in topLevelRoutes
                    val isWorkoutFlowRoute = currentRoute == Routes.NEW_WORKOUT ||
                        currentRoute == Routes.LIVE_SESSION ||
                        currentRoute.startsWith("exercise_picker") ||
                        currentRoute == Routes.PREVIOUS_WORKOUTS
                    val showNavBar = currentRoute != Routes.EDIT_PROFILE

                    LaunchedEffect(currentRoute, backEntry) {
                        if (isWorkoutFlowRoute) {
                            lastWorkoutRoute = when {
                                currentRoute.startsWith("exercise_picker") -> {
                                    val tab = backEntry?.arguments?.getInt("tab") ?: 0
                                    Routes.picker(tab)
                                }
                                else -> currentRoute
                            }
                            pausedToActualHome = false
                        }
                    }

                    LaunchedEffect(uiState.activeDraft) {
                        if (uiState.activeDraft == null && !isWorkoutFlowRoute) {
                            lastWorkoutRoute = null
                            pausedToActualHome = false
                        }
                    }

                    val (headerTitle, headerSubtitle) = when {
                        currentRoute == Routes.HOME ->
                            "Workout Tracker" to "Hi ${uiState.profile.name.ifBlank { "Athlete" }} · ${WorkoutMath.clubSessionsByDate(uiState.sessions).size} workouts logged"
                        currentRoute == Routes.HISTORY ->
                            "History" to "${WorkoutMath.clubSessionsByDate(uiState.sessions).size} workouts on this device"
                        currentRoute == Routes.LIBRARY ->
                            "Exercises" to "${uiState.allExercises.size} in the library · your numbers attached"
                        currentRoute == Routes.STATS ->
                            "Stats" to "Volume, strength and body weight"
                        currentRoute == Routes.FOOD ->
                            "Food & calories" to "Nutrition tracking · coming soon"
                        currentRoute == Routes.SETTINGS ->
                            "Settings" to "Profile, fees, body, data"
                        currentRoute == Routes.NEW_WORKOUT ->
                            "New workout" to "Step 1 of 2 · pick muscle groups"
                        currentRoute == Routes.LIVE_SESSION -> {
                            val draft = uiState.activeDraft
                            val dateLabel = WorkoutMath.formatShort(draft?.dateIso ?: WorkoutMath.todayIso())
                            if (draft?.editingSessionIds?.isNotEmpty() == true) {
                                val title = "Edit · ${draft.groups.joinToString(" + ").ifEmpty { "Workout" }}"
                                title to dateLabel
                            } else {
                                val title = draft?.groups?.joinToString(" + ")?.ifEmpty { "Session" } ?: "Session"
                                val timer = WorkoutMath.formatTimer(draft?.elapsedSeconds ?: 0)
                                title to "$dateLabel · $timer elapsed"
                            }
                        }
                        currentRoute.startsWith("exercise_picker") ->
                            "Add exercise" to (uiState.activeDraft?.groups?.joinToString(" + ") ?: "")
                        currentRoute == Routes.PREVIOUS_WORKOUTS ->
                            "Previous workouts" to (uiState.activeDraft?.groups?.joinToString(" + ")?.ifEmpty { "All groups" } ?: "All groups")
                        currentRoute.startsWith("exercise_detail") -> {
                            val exName = backEntry?.arguments?.getString("name")?.let(Uri::decode) ?: "Exercise"
                            val def = ExerciseCatalog.findByName(exName, uiState.allExercises)
                            exName to (def?.let { "${it.equipment} · ${it.groups.joinToString(", ")}" } ?: "Custom exercise")
                        }
                        currentRoute == Routes.EDIT_PROFILE ->
                            "Edit profile" to "Update your metrics"
                        else -> "Workout Tracker" to ""
                    }

                    fun navigateTopTab(route: String) {
                        if (route != Routes.HISTORY) historyExpandId = null
                        if (route == Routes.HOME) {
                            when {
                                isWorkoutFlowRoute -> {
                                    // 2nd Home tap (already creating/documenting a workout) -> go to actual Home page
                                    if (uiState.activeDraft == null) {
                                        lastWorkoutRoute = null
                                    }
                                    pausedToActualHome = true
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                                currentRoute != Routes.HOME -> {
                                    // 1st Home tap from another tab -> resume active workout screen if any
                                    val resumeTarget = if (!pausedToActualHome) {
                                        lastWorkoutRoute ?: if (uiState.activeDraft != null) Routes.LIVE_SESSION else null
                                    } else {
                                        null
                                    }
                                    if (resumeTarget != null) {
                                        navController.navigate(resumeTarget) {
                                            launchSingleTop = true
                                        }
                                    } else {
                                        pausedToActualHome = false
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                else -> Unit
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useRail = maxWidth >= 600.dp

                        Scaffold(
                            contentWindowInsets = WindowInsets.safeDrawing,
                            containerColor = palette.appBg,
                            topBar = {
                                if (currentRoute != Routes.EDIT_PROFILE) {
                                    Surface(
                                        color = palette.surface,
                                        border = BorderStroke(1.dp, palette.border)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .windowInsetsPadding(
                                                    WindowInsets.safeDrawing.only(
                                                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                                                    )
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (!isTopLevel) {
                                                IconButton(
                                                    onClick = { navController.popBackStack() },
                                                    modifier = Modifier.testTag("topbar_back_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Back",
                                                        tint = palette.text
                                                    )
                                                }
                                            } else if (currentRoute == Routes.HOME) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(palette.brand),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.FitnessCenter,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = headerTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = palette.text,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = headerSubtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (currentRoute == Routes.LIVE_SESSION) palette.brand else palette.muted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val nextMode = if (palette.isDark) AppearanceMode.LIGHT else AppearanceMode.DARK
                                                    viewModel.setAppearanceMode(nextMode)
                                                },
                                                modifier = Modifier.testTag("topbar_theme_toggle")
                                            ) {
                                                Icon(
                                                    imageVector = if (palette.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                                    contentDescription = "Toggle theme",
                                                    tint = palette.text
                                                )
                                            }

                                            if (currentRoute == Routes.HOME) {
                                                IconButton(
                                                    onClick = { navigateTopTab(Routes.SETTINGS) },
                                                    modifier = Modifier.testTag("topbar_settings_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Settings,
                                                        contentDescription = "Open settings",
                                                        tint = palette.text
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                if (showNavBar && !useRail) {
                                    NavigationBar(
                                        containerColor = palette.surface,
                                        tonalElevation = 0.dp
                                    ) {
                                        MAIN_TABS.forEach { tab ->
                                            val selected = currentRoute == tab.route ||
                                                (tab.route == Routes.HOME && isWorkoutFlowRoute)
                                            val showActiveDot = tab.route == Routes.HOME &&
                                                (uiState.activeDraft != null || lastWorkoutRoute != null)
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { navigateTopTab(tab.route) },
                                                icon = {
                                                    Box {
                                                        Icon(
                                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                            contentDescription = tab.label,
                                                            modifier = Modifier.size(21.dp)
                                                        )
                                                        if (showActiveDot) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .size(7.dp)
                                                                    .clip(CircleShape)
                                                                    .background(palette.greenMid)
                                                            )
                                                        }
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = tab.label,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Clip
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = palette.brand,
                                                    selectedTextColor = palette.brand,
                                                    indicatorColor = palette.brandSoft,
                                                    unselectedIconColor = palette.faint,
                                                    unselectedTextColor = palette.faint
                                                ),
                                                modifier = Modifier.testTag("bottom_nav_${tab.route}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    if (showNavBar && useRail) {
                                        NavigationRail(
                                            containerColor = palette.surface
                                        ) {
                                            Spacer(Modifier.height(8.dp))
                                            MAIN_TABS.forEach { tab ->
                                                val selected = currentRoute == tab.route ||
                                                    (tab.route == Routes.HOME && isWorkoutFlowRoute)
                                                val showActiveDot = tab.route == Routes.HOME &&
                                                    (uiState.activeDraft != null || lastWorkoutRoute != null)
                                                NavigationRailItem(
                                                    selected = selected,
                                                    onClick = { navigateTopTab(tab.route) },
                                                    icon = {
                                                        Box {
                                                            Icon(
                                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                                contentDescription = tab.label
                                                            )
                                                            if (showActiveDot) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.TopEnd)
                                                                        .size(7.dp)
                                                                        .clip(CircleShape)
                                                                        .background(palette.greenMid)
                                                                )
                                                            }
                                                        }
                                                    },
                                                    label = { Text(tab.label, fontSize = 12.sp) },
                                                    colors = NavigationRailItemDefaults.colors(
                                                        selectedIconColor = palette.brand,
                                                        selectedTextColor = palette.brand,
                                                        indicatorColor = palette.brandSoft,
                                                        unselectedIconColor = palette.faint,
                                                        unselectedTextColor = palette.faint
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        NavHost(
                                            navController = navController,
                                            startDestination = Routes.HOME,
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .widthIn(max = 640.dp)
                                                .fillMaxWidth()
                                        ) {
                                            composable(Routes.HOME) {
                                                HomeScreen(
                                                    uiState = uiState,
                                                    suggestedSplit = viewModel.suggestedNextSplit(),
                                                    onCreateWorkout = {
                                                        if (uiState.activeDraft != null) {
                                                            viewModel.showToast(
                                                                "Finish or discard your workout in progress first",
                                                                ToastTone.WARNING
                                                            )
                                                        } else {
                                                            pausedToActualHome = false
                                                            navController.navigate(Routes.NEW_WORKOUT)
                                                        }
                                                    },
                                                    onResumeDraft = {
                                                        pausedToActualHome = false
                                                        val target = lastWorkoutRoute ?: Routes.LIVE_SESSION
                                                        navController.navigate(target) {
                                                            launchSingleTop = true
                                                        }
                                                    },
                                                    onStartSuggestedSplit = { preset ->
                                                        if (uiState.activeDraft != null) {
                                                            viewModel.showToast(
                                                                "Finish or discard your workout in progress first",
                                                                ToastTone.WARNING
                                                            )
                                                        } else {
                                                            pausedToActualHome = false
                                                            viewModel.startNewDraft(WorkoutMath.todayIso(), preset.groups)
                                                            navController.navigate(Routes.LIVE_SESSION)
                                                        }
                                                    },
                                                    onOpenStats = { navigateTopTab(Routes.STATS) },
                                                    onOpenFees = { navigateTopTab(Routes.SETTINGS) },
                                                    onOpenHistory = { sessionId ->
                                                        historyExpandId = sessionId
                                                        navigateTopTab(Routes.HISTORY)
                                                    }
                                                )
                                            }

                                            composable(Routes.NEW_WORKOUT) {
                                                NewWorkoutScreen(
                                                    allSessions = uiState.sessions,
                                                    sessionsForGroups = { viewModel.sessionsMatchingGroups(it) },
                                                    onContinue = { dateIso, groups ->
                                                        viewModel.beginSessionFromWizard(dateIso, groups)
                                                        navController.navigate(Routes.LIVE_SESSION) {
                                                            popUpTo(Routes.NEW_WORKOUT) { inclusive = true }
                                                        }
                                                    }
                                                )
                                            }

                                            composable(Routes.LIVE_SESSION) {
                                                val draft = uiState.activeDraft
                                                if (draft == null) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("No active workout", color = palette.muted)
                                                    }
                                                } else {
                                                    LiveSessionScreen(
                                                        draft = draft,
                                                        bodyWeightKg = uiState.profile.bodyWeightKg,
                                                        allExercises = uiState.allExercises,
                                                        suggestedExercises = viewModel.suggestedExercisesForGroups(draft.groups),
                                                        lastSessionOf = { viewModel.lastSessionOf(it) },
                                                        personalRecordKg = { viewModel.personalRecordKg(it) },
                                                        onOpenPrevious = { navController.navigate(Routes.PREVIOUS_WORKOUTS) },
                                                        onOpenLookup = { navController.navigate(Routes.picker(0)) },
                                                        onOpenAddPicker = { navController.navigate(Routes.picker(0)) },
                                                        onAddSuggested = { viewModel.addExerciseToDraft(it) },
                                                        onRemoveExercise = { viewModel.removeExerciseFromDraft(it) },
                                                        onAddSet = { viewModel.addSetToExercise(it) },
                                                        onRemoveLastSet = { viewModel.removeLastSetFromExercise(it) },
                                                        onUpdateReps = { id, idx, r -> viewModel.updateSetReps(id, idx, r) },
                                                        onUpdateWeight = { id, idx, w -> viewModel.updateSetWeight(id, idx, w) },
                                                        onToggleSetDone = { id, idx -> viewModel.toggleSetDone(id, idx) },
                                                        onSelectRpe = { id, rpe -> viewModel.setExerciseRpe(id, rpe) },
                                                        onUpdateExerciseNote = { id, n -> viewModel.updateExerciseNote(id, n) },
                                                        onUpdateSessionNote = { viewModel.updateSessionNote(it) },
                                                        onUpdateDate = { viewModel.updateDraftDate(it) },
                                                        onUpdateManualDuration = { viewModel.updateDraftManualDuration(it) },
                                                        onDiscard = {
                                                            val wasEditing = draft.editingSessionIds.isNotEmpty()
                                                            lastWorkoutRoute = null
                                                            pausedToActualHome = false
                                                            viewModel.discardDraft()
                                                            if (wasEditing) navController.popBackStack()
                                                            else navController.popBackStack(Routes.HOME, inclusive = false)
                                                        },
                                                        onFinish = {
                                                            viewModel.finishDraft { wasEditing ->
                                                                lastWorkoutRoute = null
                                                                pausedToActualHome = false
                                                                if (wasEditing) {
                                                                    navController.popBackStack()
                                                                } else {
                                                                    navController.popBackStack(Routes.HOME, inclusive = false)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }

                                            composable(
                                                route = Routes.EXERCISE_PICKER,
                                                arguments = listOf(navArgument("tab") { type = NavType.IntType })
                                            ) { entry ->
                                                val tab = entry.arguments?.getInt("tab") ?: 0
                                                val draft = uiState.activeDraft
                                                val groups = draft?.groups ?: emptyList()
                                                val draftNames = draft?.exercises?.map { it.name }?.toSet() ?: emptySet()

                                                ExercisePickerScreen(
                                                    initialTab = tab,
                                                    draftGroups = groups,
                                                    draftExerciseNames = draftNames,
                                                    allExercises = uiState.allExercises,
                                                    suggestedExercises = viewModel.suggestedExercisesForGroups(groups),
                                                    aiLookupState = uiState.aiLookupState,
                                                    lastSessionOf = { viewModel.lastSessionOf(it) },
                                                    onAddExercise = { name ->
                                                        viewModel.addExerciseToDraft(name)
                                                        navController.popBackStack()
                                                    },
                                                    onAddCustomExercise = { name, grps, eq ->
                                                        viewModel.addCustomExerciseAndAddToDraft(name, grps, eq)
                                                        navController.popBackStack()
                                                    },
                                                    onAskGeminiAi = { desc -> viewModel.askGeminiForExercise(desc) },
                                                    onAddAiSuggestion = { sug ->
                                                        viewModel.addAiSuggestionToDraft(sug)
                                                        navController.popBackStack()
                                                    },
                                                    onResetAiLookup = { viewModel.resetAiLookup() },
                                                    onOpenDetail = { name -> navController.navigate(Routes.detail(name)) }
                                                )
                                            }

                                            composable(Routes.PREVIOUS_WORKOUTS) {
                                                val draft = uiState.activeDraft
                                                val groups = draft?.groups ?: emptyList()
                                                val draftNames = draft?.exercises?.map { it.name }?.toSet() ?: emptySet()
                                                PreviousWorkoutsScreen(
                                                    draftGroups = groups,
                                                    draftExerciseNames = draftNames,
                                                    allExercises = uiState.allExercises,
                                                    sessionsMatchingGroups = { viewModel.sessionsMatchingGroups(it) },
                                                    onAddExerciseToDraft = { name ->
                                                        viewModel.addExerciseToDraft(name)
                                                        navController.popBackStack()
                                                    }
                                                )
                                            }

                                            composable(Routes.HISTORY) {
                                                HistoryScreen(
                                                    sessions = uiState.sessions,
                                                    allExercises = uiState.allExercises,
                                                    bodyWeightKg = uiState.profile.bodyWeightKg,
                                                    initialExpandedId = historyExpandId,
                                                    sessionsMatchingGroups = { viewModel.sessionsMatchingGroups(it) },
                                                    onOpenExerciseDetail = { name ->
                                                        navController.navigate(Routes.detail(name))
                                                    },
                                                    onEditSession = { session, sourceIds ->
                                                        pausedToActualHome = false
                                                        viewModel.startEditingSession(session, sourceIds)
                                                        navController.navigate(Routes.LIVE_SESSION)
                                                    },
                                                    onDeleteSessions = { ids -> viewModel.deleteSessions(ids) }
                                                )
                                            }

                                            composable(Routes.LIBRARY) {
                                                ExerciseLibraryScreen(
                                                    allExercises = uiState.allExercises,
                                                    lastSessionOf = { viewModel.lastSessionOf(it) },
                                                    personalRecordKg = { viewModel.personalRecordKg(it) },
                                                    allSessionsOf = { viewModel.allSessionsOf(it) },
                                                    onOpenDetail = { name -> navController.navigate(Routes.detail(name)) }
                                                )
                                            }

                                            composable(
                                                route = Routes.EXERCISE_DETAIL,
                                                arguments = listOf(navArgument("name") { type = NavType.StringType })
                                            ) { entry ->
                                                val name = entry.arguments?.getString("name")?.let(Uri::decode) ?: "Back squat"
                                                ExerciseDetailScreen(
                                                    exerciseName = name,
                                                    allExercises = uiState.allExercises,
                                                    historyRows = viewModel.allSessionsOf(name),
                                                    personalRecordKg = viewModel.personalRecordKg(name),
                                                    hasActiveDraft = uiState.activeDraft != null,
                                                    onAddOrStartWithExercise = { exName, exGroups ->
                                                        if (uiState.activeDraft == null) {
                                                            viewModel.startNewDraft(WorkoutMath.todayIso(), exGroups)
                                                        }
                                                        viewModel.addExerciseToDraft(exName)
                                                        navController.navigate(Routes.LIVE_SESSION) {
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                )
                                            }

                                            composable(Routes.STATS) {
                                                StatsScreen(
                                                    uiState = uiState,
                                                    allSessionsOf = { viewModel.allSessionsOf(it) },
                                                    onLogBodyWeight = { viewModel.logBodyWeight(it) }
                                                )
                                            }

                                            composable(Routes.FOOD) {
                                                FoodPreviewScreen(bodyWeightKg = uiState.profile.bodyWeightKg)
                                            }

                                            composable(Routes.SETTINGS) {
                                                SettingsScreen(
                                                    uiState = uiState,
                                                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                                                    onSaveSettingsChanges = { last, next, amt, w, c, a, target ->
                                                        viewModel.saveSettingsChanges(last, next, amt, w, c, a, target)
                                                    },
                                                    onMarkFeePaidToday = { viewModel.markFeePaidToday() },
                                                    onSelectProgressPhoto = { slot, uri ->
                                                        viewModel.saveProgressPhoto(slot, uri)
                                                    },
                                                    onClearProgressPhoto = { slot ->
                                                        viewModel.clearProgressPhoto(slot)
                                                    },
                                                    onExportJson = {
                                                        exportLauncher.launch("workout-tracker-${WorkoutMath.todayIso()}.json")
                                                    },
                                                    onImportJson = {
                                                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                                    },
                                                    onClearAllWorkouts = { viewModel.clearAllWorkouts() },
                                                    onSetAppearance = { viewModel.setAppearanceMode(it) },
                                                    onReplayOnboarding = { viewModel.replayOnboarding() }
                                                )
                                            }

                                            composable(Routes.EDIT_PROFILE) {
                                                OnboardingScreen(
                                                    initialProfile = uiState.profile,
                                                    initialNextFeeIso = uiState.gymFee.nextDueIso,
                                                    startStep = 2,
                                                    isEditOnly = true,
                                                    onComplete = { updated, _ ->
                                                        viewModel.updateProfile(updated)
                                                        navController.popBackStack()
                                                    },
                                                    onCancelEdit = { navController.popBackStack() }
                                                )
                                            }
                                        }
                                    }
                                }

                                FloatingBannerHost(
                                    banner = uiState.toastBanner,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 14.dp, vertical = 14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Cold-Launch Splash Animation (< 1.5s, never replays on background return)
            AnimatedVisibility(
                visible = uiState.showLaunchSplash,
                exit = fadeOut(animationSpec = tween(220))
            ) {
                LaunchSplashOverlay()
            }
        }
    }
}

@Composable
private fun FloatingBannerHost(
    banner: ToastBanner?,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val tone = banner?.tone ?: ToastTone.SUCCESS
        val (bg, borderColor, fg, icon) = when (tone) {
            ToastTone.WARNING -> Quad(
                palette.orangeSoft,
                palette.orange,
                palette.orangeInk,
                Icons.Filled.WarningAmber
            )
            ToastTone.INFO -> Quad(
                palette.brandSoft,
                palette.brandBd,
                palette.brandInk,
                Icons.Filled.Info
            )
            ToastTone.SUCCESS -> Quad(
                palette.toastBg,
                palette.greenMid.copy(alpha = 0.6f),
                palette.toastFg,
                Icons.Filled.CheckCircle
            )
        }
        val iconTint = if (tone == ToastTone.SUCCESS) palette.greenMid else fg

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = bg,
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .testTag("floating_toast_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = banner?.message ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = fg
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun LaunchSplashOverlay() {
    val palette = LocalWorkoutPalette.current
    val scale = remember { Animatable(0.76f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(260))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.appBg)
            .testTag("launch_splash_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.brand),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = "Workout Tracker",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.text
            )
        }
    }
}
