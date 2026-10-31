package ru.aokazantsev.devatlas.themes

import androidx.room.RoomDatabase
import io.ktor.client.HttpClient
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import ru.aokazantsev.devatlas.database.data.DevAtlasDatabase
import ru.aokazantsev.devatlas.di.DeviceInfo
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.files.domain.ExternalApps
import ru.aokazantsev.devatlas.files.domain.PublicFiles
import ru.aokazantsev.devatlas.themes.domain.ThemeInstaller
import ru.aokazantsev.devatlas.themes.domain.ThemeUpdater
import ru.aokazantsev.devatlas.themes.domain.model.InstallResult
import ru.aokazantsev.devatlas.themes.domain.model.PrepareResult
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.update.domain.AppInstaller
import ru.aokazantsev.devatlas.usage.domain.UsageScheduler

class ScriptedInstaller(
    var prepareResult: PrepareResult = PrepareResult.Failed(ThemeFailure("не настроено")),
) : ThemeInstaller {
    val prepared = mutableListOf<String>()
    val installed = mutableListOf<PreparedTheme>()
    val discarded = mutableListOf<PreparedTheme>()

    override suspend fun prepare(address: String): PrepareResult {
        prepared += address
        return prepareResult
    }

    var installFailure: ThemeFailure? = null

    override suspend fun install(prepared: PreparedTheme): InstallResult {
        installed += prepared
        return installFailure?.let { InstallResult.Failed(it) } ?: InstallResult.Installed(installedOf(prepared.manifest.id))
    }

    override fun discard(prepared: PreparedTheme) {
        discarded += prepared
    }

    override suspend fun redownload(themeId: String): ThemeFailure? = null
}

class ScriptedUpdater(var result: UpdateCheck = UpdateCheck.NoChanges) : ThemeUpdater {
    val requests = mutableListOf<Pair<String, String?>>()

    override suspend fun check(themeId: String, newAddress: String?): UpdateCheck {
        requests += themeId to newAddress
        return result
    }
}

class FakePlatformBindings(var clipboard: String? = null, var copySucceeds: Boolean = true) : PlatformBindings {
    override val dataDirectory: Path = "/data".toPath()
    override val fileSystem: FileSystem get() = error("не используется")

    override fun databaseBuilder(): RoomDatabase.Builder<DevAtlasDatabase> = error("не используется")

    override fun httpClient(): HttpClient = error("не используется")

    override fun clipboardText(): String? = clipboard

    override val publicFiles: PublicFiles get() = error("не используется")
    override val externalApps: ExternalApps get() = error("не используется")
    override val appInstaller: AppInstaller get() = error("не используется")
    override val usageScheduler: UsageScheduler get() = error("не используется")
    override val device: DeviceInfo = DeviceInfo(model = "Pixel 8", androidVersion = "14")

    override fun copyToClipboard(text: String): Boolean {
        if (copySucceeds) clipboard = text
        return copySucceeds
    }

    override fun timeZoneOffsetMillis(epochMillis: Long): Long = timeZoneOffset

    var timeZoneOffset: Long = 0L
}
