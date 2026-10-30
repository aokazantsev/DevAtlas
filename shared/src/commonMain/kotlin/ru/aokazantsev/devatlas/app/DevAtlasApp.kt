package ru.aokazantsev.devatlas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.di.AppGraph
import ru.aokazantsev.devatlas.navigation.Route
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval
import ru.aokazantsev.devatlas.ui.about.AboutScreen
import ru.aokazantsev.devatlas.ui.addtheme.AddThemeScreen
import ru.aokazantsev.devatlas.ui.catalog.ComponentCatalogScreen
import ru.aokazantsev.devatlas.ui.contents.ContentsScreen
import ru.aokazantsev.devatlas.ui.onboarding.OnboardingScreen
import ru.aokazantsev.devatlas.ui.page.PageScreen
import ru.aokazantsev.devatlas.ui.semantics.APP_ROOT_TAG
import ru.aokazantsev.devatlas.ui.semantics.darkTheme
import ru.aokazantsev.devatlas.ui.semantics.surfaceColor
import ru.aokazantsev.devatlas.ui.semantics.wallpaperColors
import ru.aokazantsev.devatlas.ui.settings.SettingsScreen
import ru.aokazantsev.devatlas.ui.splash.SplashScreen
import ru.aokazantsev.devatlas.ui.splash.SplashState
import ru.aokazantsev.devatlas.ui.themes.ManageThemeScreen
import ru.aokazantsev.devatlas.ui.themes.ThemeDetailsScreen
import ru.aokazantsev.devatlas.ui.themes.ThemesScreen

@Composable
fun DevAtlasApp(
    graph: AppGraph,
    onThemeResolved: (mode: ThemeMode, darkTheme: Boolean) -> Unit,
    onBackgroundResolved: (argb: Int) -> Unit,
    onContentReady: () -> Unit,
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val appViewModel = viewModel { graph.appViewModel() }
    val appearance by appViewModel.appearance.collectAsStateWithLifecycle()
    val startupState by appViewModel.startupState.collectAsStateWithLifecycle()
    val startRoute by appViewModel.startRoute.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val systemIsDark = isSystemInDarkTheme()
    val resolvedAppearance = appearance ?: return
    val mode = resolvedAppearance.themeMode
    val darkTheme = mode.isDark(systemIsDark)
    val startupFinished = startupState != SplashState.Checking
    LaunchedEffect(mode, darkTheme) { onThemeResolved(mode, darkTheme) }
    LaunchedEffect(startupFinished) {
        if (startupFinished) onContentReady()
    }
    AtlasTheme(darkTheme = darkTheme, wallpaperColors = resolvedAppearance.wallpaperColors) {
        val background = AtlasTheme.colors.surfaceContainer
        LaunchedEffect(background) { onBackgroundResolved(background.toArgb()) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .semantics {
                    testTag = APP_ROOT_TAG
                    this.darkTheme = darkTheme
                    this.wallpaperColors = resolvedAppearance.wallpaperColors
                    surfaceColor = background.toArgb()
                },
        ) {
            val route = startRoute
            if (startupState == SplashState.Ready && route != null) {
                AppNavHost(graph = graph, navController = navController, startRoute = route)
            } else {
                SplashScreen(
                    state = startupState,
                    onRetry = appViewModel::retry,
                    onRedownload = appViewModel::redownloadDamagedTheme,
                    onChooseAnother = appViewModel::chooseAnotherTheme,
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(graph: AppGraph, navController: NavHostController, startRoute: Route) {
    NavHost(navController = navController, startDestination = startRoute) {
        firstRunDestinations(graph, navController)
        themeDestinations(graph, navController)
        settingsDestinations(graph, navController)
    }
}

private fun NavGraphBuilder.firstRunDestinations(graph: AppGraph, navController: NavHostController) {
    composable<Route.Onboarding> { entry ->
        OnboardingScreen(
            viewModel = viewModel { graph.onboardingViewModel() },
            onNext = { entry.whenResumed { navController.resetTo(Route.AddTheme(onboarding = true)) } },
        )
    }
    composable<Route.AddTheme> { entry ->
        val onboarding = entry.toRoute<Route.AddTheme>().onboarding
        AddThemeScreen(
            viewModel = viewModel { graph.addThemeViewModel() },
            onboarding = onboarding,
            onBack = if (onboarding) null else ({ entry.whenResumed { navController.popBackStack() } }),
            onInstalled = { themeId ->
                if (onboarding) {
                    navController.resetTo(Route.Contents)
                } else {
                    navController.navigate(Route.ThemeDetails(themeId)) { popUpTo<Route.AddTheme> { inclusive = true } }
                }
            },
        )
    }
    composable<Route.Contents> { entry ->
        ContentsScreen(
            viewModel = viewModel { graph.contentsViewModel() },
            onOpenSettings = { entry.whenResumed { navController.navigate(Route.Settings) } },
            onAddTheme = { entry.whenResumed { navController.resetTo(Route.AddTheme(onboarding = true)) } },
        )
    }
    composable<Route.Page> { entry ->
        PageScreen(
            pageId = entry.toRoute<Route.Page>().pageId,
            onBack = { entry.whenResumed { navController.popBackStack() } },
        )
    }
}

private fun NavGraphBuilder.themeDestinations(graph: AppGraph, navController: NavHostController) {
    composable<Route.Themes> { entry ->
        ThemesScreen(
            viewModel = viewModel { graph.themesViewModel() },
            onBack = { entry.whenResumed { navController.popBackStack() } },
            onOpenTheme = { themeId -> entry.whenResumed { navController.navigate(Route.ThemeDetails(themeId)) } },
            onAddTheme = { entry.whenResumed { navController.navigate(Route.AddTheme(onboarding = false)) } },
        )
    }
    composable<Route.ThemeDetails> { entry ->
        val themeId = entry.toRoute<Route.ThemeDetails>().themeId
        ThemeDetailsScreen(
            viewModel = viewModel(key = themeId) { graph.themeDetailsViewModel.create(themeId) },
            onBack = { entry.whenResumed { navController.popBackStack() } },
            onApplied = { navController.resetTo(Route.Contents) },
            onManage = { entry.whenResumed { navController.navigate(Route.ManageTheme(themeId)) } },
        )
    }
    composable<Route.ManageTheme> { entry ->
        val themeId = entry.toRoute<Route.ManageTheme>().themeId
        ManageThemeScreen(
            viewModel = viewModel(key = themeId) { graph.manageThemeViewModel.create(themeId) },
            onBack = { entry.whenResumed { navController.popBackStack() } },
            onRemoved = { removal -> navController.handleRemoval(removal) },
        )
    }
}

private fun NavGraphBuilder.settingsDestinations(graph: AppGraph, navController: NavHostController) {
    composable<Route.Settings> { entry ->
        SettingsScreen(
            viewModel = viewModel { graph.settingsViewModel() },
            showComponentCatalog = graph.appInfo.isDebug,
            onBack = { entry.whenResumed { navController.popBackStack() } },
            onOpenThemes = { entry.whenResumed { navController.navigate(Route.Themes) } },
            onOpenAbout = { entry.whenResumed { navController.navigate(Route.About) } },
            onOpenComponentCatalog = { entry.whenResumed { navController.navigate(Route.ComponentCatalog) } },
        )
    }
    composable<Route.About> { entry ->
        AboutScreen(
            versionName = graph.appInfo.versionName,
            onBack = { entry.whenResumed { navController.popBackStack() } },
        )
    }
    composable<Route.ComponentCatalog> { entry ->
        ComponentCatalogScreen(onBack = { entry.whenResumed { navController.popBackStack() } })
    }
}

private fun NavHostController.resetTo(route: Route) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}

private fun NavHostController.handleRemoval(removal: ThemeRemoval) {
    when (removal) {
        ThemeRemoval.ThemeDeleted -> popBackStack<Route.Themes>(inclusive = false)
        is ThemeRemoval.SwitchedTo -> resetTo(Route.Contents)
        ThemeRemoval.NoThemesLeft -> resetTo(Route.AddTheme(onboarding = true))
        ThemeRemoval.ProgressReset, is ThemeRemoval.Failed -> Unit
    }
}

private fun NavBackStackEntry.whenResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
