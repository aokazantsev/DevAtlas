package ru.aokazantsev.devatlas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
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
import ru.aokazantsev.devatlas.ui.about.AboutScreen
import ru.aokazantsev.devatlas.ui.catalog.ComponentCatalogScreen
import ru.aokazantsev.devatlas.ui.contents.ContentsScreen
import ru.aokazantsev.devatlas.ui.onboarding.OnboardingScreen
import ru.aokazantsev.devatlas.ui.page.PageScreen
import ru.aokazantsev.devatlas.ui.settings.SettingsScreen
import ru.aokazantsev.devatlas.ui.splash.SplashScreen
import ru.aokazantsev.devatlas.ui.splash.SplashState

@Composable
fun DevAtlasApp(
    graph: AppGraph,
    onThemeResolved: (mode: ThemeMode, darkTheme: Boolean) -> Unit,
    onContentReady: () -> Unit,
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val appViewModel = viewModel { graph.appViewModel() }
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val startupState by appViewModel.startupState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val systemIsDark = isSystemInDarkTheme()
    val mode = themeMode ?: return
    val darkTheme = mode.isDark(systemIsDark)
    val startupFinished = startupState != SplashState.Checking
    LaunchedEffect(mode, darkTheme) { onThemeResolved(mode, darkTheme) }
    LaunchedEffect(startupFinished) {
        if (startupFinished) onContentReady()
    }
    AtlasTheme(darkTheme = darkTheme) {
        Box(modifier = Modifier.fillMaxSize().background(AtlasTheme.colors.background)) {
            val state = startupState
            if (state == SplashState.Ready) {
                AppNavHost(graph = graph, navController = navController)
            } else {
                SplashScreen(state = state, onRetry = appViewModel::retry)
            }
        }
    }
}

@Composable
private fun AppNavHost(graph: AppGraph, navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Onboarding) {
        composable<Route.Onboarding> { entry ->
            OnboardingScreen(
                onNext = {
                    entry.whenResumed {
                        navController.navigate(Route.Contents) {
                            popUpTo<Route.Onboarding> { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<Route.Contents> { entry ->
            ContentsScreen(
                onOpenPage = { pageId -> entry.whenResumed { navController.navigate(Route.Page(pageId)) } },
                onOpenSettings = { entry.whenResumed { navController.navigate(Route.Settings) } },
            )
        }
        composable<Route.Page> { entry ->
            PageScreen(
                pageId = entry.toRoute<Route.Page>().pageId,
                onBack = { entry.whenResumed { navController.popBackStack() } },
            )
        }
        composable<Route.Settings> { entry ->
            SettingsScreen(
                viewModel = viewModel { graph.settingsViewModel() },
                showComponentCatalog = graph.appInfo.isDebug,
                onBack = { entry.whenResumed { navController.popBackStack() } },
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
}

private fun NavBackStackEntry.whenResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
