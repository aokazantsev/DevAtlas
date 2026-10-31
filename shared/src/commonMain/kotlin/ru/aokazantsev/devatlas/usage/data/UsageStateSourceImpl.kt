package ru.aokazantsev.devatlas.usage.data

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import ru.aokazantsev.devatlas.onboarding.domain.OnboardingRepository
import ru.aokazantsev.devatlas.settings.domain.ThemeSettingsRepository
import ru.aokazantsev.devatlas.themes.domain.CurrentThemeRepository
import ru.aokazantsev.devatlas.themes.domain.InstalledThemeRepository
import ru.aokazantsev.devatlas.themes.domain.model.SampleTheme
import ru.aokazantsev.devatlas.update.domain.UpdateSettingsRepository
import ru.aokazantsev.devatlas.usage.domain.UsageStateSource

@ContributesBinding(AppScope::class)
@Inject
internal class UsageStateSourceImpl(
    private val onboarding: OnboardingRepository,
    private val updateSettings: UpdateSettingsRepository,
    private val themeSettings: ThemeSettingsRepository,
    private val current: CurrentThemeRepository,
    private val installed: InstalledThemeRepository,
) : UsageStateSource {

    override suspend fun state(): Map<String, String> = buildMap {
        put("crash_reports", onboarding.consent.first().crashReports.toString())
        put("auto_update", updateSettings.autoCheck.first().toString())
        val appearance = themeSettings.appearance.first()
        put("theme_mode", appearance.themeMode.name.lowercase())
        put("wallpaper_colors", appearance.wallpaperColors.toString())
        try {
            put("themes_count", installed.all().size.toString())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: SQLiteException) {
        } catch (_: IllegalStateException) {
        }
        current.currentThemeId.first()?.let { put("theme", if (it == SampleTheme.ID) "sample" else "custom") }
    }
}
