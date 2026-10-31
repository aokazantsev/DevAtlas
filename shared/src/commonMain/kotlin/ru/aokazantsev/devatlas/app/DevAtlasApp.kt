package ru.aokazantsev.devatlas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.di.AppGraph
import ru.aokazantsev.devatlas.navigation.Route
import ru.aokazantsev.devatlas.onboarding.domain.model.Consent
import ru.aokazantsev.devatlas.settings.domain.model.ThemeMode
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval
import ru.aokazantsev.devatlas.ui.about.AboutScreen
import ru.aokazantsev.devatlas.ui.addtheme.AddThemeScreen
import ru.aokazantsev.devatlas.ui.bookmarks.BookmarksScreen
import ru.aokazantsev.devatlas.ui.catalog.ComponentCatalogScreen
import ru.aokazantsev.devatlas.ui.contents.ContentsScreen
import ru.aokazantsev.devatlas.ui.downloads.DownloadsScreen
import ru.aokazantsev.devatlas.ui.links.LinkActions
import ru.aokazantsev.devatlas.ui.links.LinksHost
import ru.aokazantsev.devatlas.ui.links.LinksViewModel
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
import ru.aokazantsev.devatlas.ui.update.UpdateNoticeHost
import ru.aokazantsev.devatlas.ui.update.UpdateScreen
import ru.aokazantsev.devatlas.ui.web.WebScreen

@Composable
fun DevAtlasApp(
    graph: AppGraph,
    onThemeResolved: (mode: ThemeMode, darkTheme: Boolean) -> Unit,
    onBackgroundResolved: (argb: Int) -> Unit,
    onContentReady: () -> Unit,
) {
    val appViewModel = viewModel { graph.appViewModel() }
    val appearance by appViewModel.appearance.collectAsStateWithLifecycle()
    val startupState by appViewModel.startupState.collectAsStateWithLifecycle()
    val startStack by appViewModel.startStack.collectAsStateWithLifecycle()
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
            val stack = startStack
            if (startupState == SplashState.Ready && stack != null) {
                val links = viewModel { graph.linksViewModel() }
                AppNavHost(graph = graph, navController = navController, startStack = stack, links = links)
                LinksHost(links)
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
private fun AppNavHost(graph: AppGraph, navController: NavHostController, startStack: List<Route>, links: LinksViewModel) {
    var stackOpened by rememberSaveable { mutableStateOf(false) }
    NavHost(navController = navController, startDestination = startStack.first()) {
        firstRunDestinations(graph, navController)
        contentsDestinations(graph, navController, links)
        themeDestinations(graph, navController)
        settingsDestinations(graph, navController)
        webDestinations(graph, navController, links)
    }
    if (startStack.first() != Route.Onboarding) {
        UpdateNoticeHost(
            viewModel = viewModel { graph.updateNoticeViewModel() },
            onOpenUpdate = { navController.navigate(Route.Update) { launchSingleTop = true } },
        )
    }
    LaunchedEffect(Unit) {
        if (stackOpened) return@LaunchedEffect
        stackOpened = true
        startStack.drop(1).forEach { navController.navigate(it) }
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
            viewModel = viewModel { graph.addThemeViewModel.create(createSavedStateHandle()) },
            onboarding = onboarding,
            onBack = if (onboarding) null else ({ entry.whenResumed { navController.popBackStack() } }),
            onInstalled = { themeId ->
                if (onboarding) {
                    navController.resetTo(Route.Contents())
                } else {
                    navController.navigate(Route.ThemeDetails(themeId)) { popUpTo<Route.AddTheme> { inclusive = true } }
                }
            },
        )
    }
}

private fun NavGraphBuilder.contentsDestinations(graph: AppGraph, navController: NavHostController, links: LinksViewModel) {
    composable<Route.Contents> { entry ->
        ContentsScreen(
            viewModel = viewModel { graph.contentsViewModel.create(null) },
            onOpenSection = { id -> entry.whenResumed { navController.navigate(Route.Section(id)) } },
            onOpenPage = { id -> entry.whenResumed { navController.navigate(Route.Page(id)) } },
            onOpenSettings = { entry.whenResumed { navController.navigate(Route.Settings) } },
            onOpenBookmarks = { entry.whenResumed { navController.navigate(Route.Bookmarks) } },
            onAddTheme = { entry.whenResumed { navController.resetTo(Route.AddTheme(onboarding = true)) } },
            onBack = null,
            anchor = entry.toRoute<Route.Contents>().anchor,
        )
    }
    composable<Route.Section> { entry ->
        val section = entry.toRoute<Route.Section>()
        ContentsScreen(
            viewModel = viewModel(key = section.sectionId) { graph.contentsViewModel.create(section.sectionId) },
            onOpenSection = { id -> entry.whenResumed { navController.navigate(Route.Section(id)) } },
            onOpenPage = { id -> entry.whenResumed { navController.navigate(Route.Page(id)) } },
            onOpenSettings = null,
            onOpenBookmarks = null,
            onAddTheme = { entry.whenResumed { navController.resetTo(Route.AddTheme(onboarding = true)) } },
            onBack = { entry.whenResumed { navController.popBackStack() } },
            anchor = section.anchor,
        )
    }
    composable<Route.Page> { entry ->
        val page = entry.toRoute<Route.Page>()
        PageScreen(
            viewModel = viewModel(key = page.pageId) { graph.pageViewModel.create(page.pageId) },
            anchor = page.anchor,
            links = rememberLinkActions(entry, navController, links),
            onNext = { id ->
                entry.whenResumed { navController.navigate(Route.Page(id)) { popUpTo(page) { inclusive = true } } }
            },
            onBack = { entry.whenResumed { navController.popBackStack() } },
        )
    }
    composable<Route.Bookmarks> { entry ->
        BookmarksScreen(
            viewModel = viewModel { graph.bookmarksViewModel() },
            onBack = { entry.whenResumed { navController.popBackStack() } },
            onOpen = { stack -> entry.whenResumed { navController.openStack(stack) } },
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
            onApplied = { navController.resetTo(Route.Contents()) },
            onManage = { entry.whenResumed { navController.navigate(Route.ManageTheme(themeId)) } },
        )
    }
    composable<Route.ManageTheme> { entry ->
        val themeId = entry.toRoute<Route.ManageTheme>().themeId
        ManageThemeScreen(
            viewModel = viewModel(key = themeId) { graph.manageThemeViewModel.create(themeId, createSavedStateHandle()) },
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
            onOpenDownloads = { entry.whenResumed { navController.navigate(Route.Downloads) } },
            onOpenAbout = { entry.whenResumed { navController.navigate(Route.About) } },
            onOpenComponentCatalog = { entry.whenResumed { navController.navigate(Route.ComponentCatalog) } },
        )
    }
    composable<Route.Downloads> { entry ->
        DownloadsScreen(
            viewModel = viewModel { graph.downloadsViewModel() },
            onBack = { entry.whenResumed { navController.popBackStack() } },
        )
    }
    composable<Route.About> { entry ->
        AboutScreen(
            viewModel = viewModel { graph.aboutViewModel() },
            onBack = { entry.whenResumed { navController.popBackStack() } },
            onCheckUpdate = { entry.whenResumed { navController.navigate(Route.Update) { launchSingleTop = true } } },
            onOpenPolicy = { entry.whenResumed { navController.navigate(Route.Web(Consent.POLICY_URL)) } },
        )
    }
    composable<Route.Update> { entry ->
        UpdateScreen(
            viewModel = viewModel { graph.updateViewModel() },
            onBack = { entry.whenResumed { navController.popBackStack() } },
        )
    }
    composable<Route.ComponentCatalog> { entry ->
        ComponentCatalogScreen(onBack = { entry.whenResumed { navController.popBackStack() } })
    }
}

private fun NavGraphBuilder.webDestinations(graph: AppGraph, navController: NavHostController, links: LinksViewModel) {
    composable<Route.Web> { entry ->
        val url = entry.toRoute<Route.Web>().url
        WebScreen(
            url = url,
            startUrl = graph.endpoints.webUrl(url),
            links = rememberLinkActions(entry, navController, links),
            onBack = { entry.whenResumed { navController.popBackStack() } },
            onNothingShown = { if (navController.currentBackStackEntry == entry) navController.popBackStack() },
        )
    }
}

@Composable
private fun rememberLinkActions(entry: NavBackStackEntry, navController: NavHostController, links: LinksViewModel): LinkActions =
    remember(entry, navController, links) {
        LinkActions(
            openPage = { id -> entry.whenResumed { navController.navigate(Route.Page(id)) } },
            openWeb = { url ->
                entry.whenResumed {
                    links.openedWeb()
                    navController.navigate(Route.Web(url))
                }
            },
            openPdf = links::openPdf,
            redownloadPdf = links::redownloadPdf,
            openExternal = links::openExternal,
            copyLink = links::copyLink,
        )
    }

private fun NavHostController.resetTo(route: Route) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}

private fun NavHostController.openStack(stack: List<Route>) {
    resetTo(stack.first())
    stack.drop(1).forEach { navigate(it) }
}

private fun NavHostController.handleRemoval(removal: ThemeRemoval) {
    when (removal) {
        ThemeRemoval.ThemeDeleted -> popBackStack<Route.Themes>(inclusive = false)
        is ThemeRemoval.SwitchedTo -> resetTo(Route.Contents())
        ThemeRemoval.NoThemesLeft -> resetTo(Route.AddTheme(onboarding = true))
        ThemeRemoval.ProgressReset, is ThemeRemoval.Failed -> Unit
    }
}

private fun NavBackStackEntry.whenResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
