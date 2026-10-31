package ru.aokazantsev.devatlas.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.di.AppInfo
import ru.aokazantsev.devatlas.di.AppLinks
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.files.domain.ExternalApps

@Inject
class AboutViewModel(
    appInfo: AppInfo,
    private val externalApps: ExternalApps,
    private val platform: PlatformBindings,
) : ViewModel() {

    val versionName: String = appInfo.versionName

    private val mutableNotice = MutableStateFlow<AboutNotice?>(null)
    val notice: StateFlow<AboutNotice?> = mutableNotice.asStateFlow()

    private var noticeJob: Job? = null

    fun open(url: String) {
        if (!externalApps.openUri(url)) show(AboutNotice(url, "Нет браузера, чтобы открыть ссылку"))
    }

    fun copy(url: String) {
        if (!platform.copyToClipboard(url)) {
            show(AboutNotice(url, "Не удалось скопировать ссылку"))
        } else if (!platform.clipboardConfirmsCopy) {
            show(AboutNotice(url, "Ссылка скопирована"))
        }
    }

    fun share() {
        if (!externalApps.shareText(SHARE_TEXT)) show(AboutNotice(SHARE_KEY, "Нет приложения, чтобы поделиться ссылкой"))
    }

    private fun show(notice: AboutNotice) {
        noticeJob?.cancel()
        mutableNotice.value = notice
        noticeJob = viewModelScope.launch {
            delay(NOTICE_MILLIS)
            mutableNotice.value = null
        }
    }

    companion object {
        const val SHARE_KEY = "share"
        private const val NOTICE_MILLIS = 2_000L
        private const val SHARE_TEXT = "DevAtlas — приложение для изучения тем по разработке: ${AppLinks.SITE_URL}"
    }
}
