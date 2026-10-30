package ru.aokazantsev.devatlas.ui.addtheme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.di.PlatformBindings
import ru.aokazantsev.devatlas.themes.domain.InstalledThemeRepository
import ru.aokazantsev.devatlas.themes.domain.ThemeInstaller
import ru.aokazantsev.devatlas.themes.domain.ThemeLibrary
import ru.aokazantsev.devatlas.themes.domain.model.InstallResult
import ru.aokazantsev.devatlas.themes.domain.model.PrepareResult
import ru.aokazantsev.devatlas.themes.domain.model.PreparedTheme
import ru.aokazantsev.devatlas.themes.domain.model.SampleTheme

@Inject
class AddThemeViewModel(
    private val installer: ThemeInstaller,
    private val library: ThemeLibrary,
    private val repository: InstalledThemeRepository,
    private val platform: PlatformBindings,
    private val endpoints: NetworkEndpoints,
) : ViewModel() {

    private val mutableState = MutableStateFlow<AddThemeState>(AddThemeState.Editing(address = "", hint = null))
    val state: StateFlow<AddThemeState> = mutableState.asStateFlow()

    val sampleAvailable: Flow<Boolean> = library.observeInstalled().map { themes -> themes.none { it.id == SampleTheme.ID } }

    private val installedChannel = Channel<String>(Channel.BUFFERED)
    val installed: Flow<String> = installedChannel.receiveAsFlow()

    private var work: Job? = null

    fun setAddress(address: String) {
        mutableState.value = AddThemeState.Editing(address = address, hint = null)
    }

    fun paste() {
        val text = platform.clipboardText()?.trim().orEmpty()
        mutableState.value = if (text.isEmpty()) {
            AddThemeState.Editing(address = "", hint = "В буфере обмена пусто")
        } else {
            AddThemeState.Editing(address = text, hint = null)
        }
    }

    fun startWithSample() {
        load(endpoints.sampleThemeUrl)
    }

    fun load(address: String) {
        work?.cancel()
        mutableState.value = AddThemeState.Loading(message = "Скачиваю и проверяю тему…")
        work = viewModelScope.launch {
            mutableState.value = when (val result = installer.prepare(address)) {
                is PrepareResult.Failed -> AddThemeState.Failed(address = address, failure = result.failure)
                is PrepareResult.Ready -> AddThemeState.Preview(
                    prepared = result.prepared,
                    existing = repository.find(result.prepared.manifest.id),
                    confirmReplace = false,
                )
            }
        }
    }

    fun install() {
        val preview = mutableState.value as? AddThemeState.Preview ?: return
        if (preview.existing != null && !preview.confirmReplace) {
            mutableState.value = preview.copy(confirmReplace = true)
            return
        }
        commit(preview.prepared)
    }

    fun dismissReplace() {
        val preview = mutableState.value as? AddThemeState.Preview ?: return
        mutableState.value = preview.copy(confirmReplace = false)
    }

    fun cancelPreview() {
        val preview = mutableState.value as? AddThemeState.Preview ?: return
        installer.discard(preview.prepared)
        mutableState.value = AddThemeState.Editing(address = preview.prepared.source, hint = null)
    }

    fun backToEditing() {
        val failed = mutableState.value as? AddThemeState.Failed ?: return
        mutableState.value = AddThemeState.Editing(address = failed.address, hint = null)
    }

    private fun commit(prepared: PreparedTheme) {
        mutableState.value = AddThemeState.Loading(message = "Устанавливаю тему…")
        work = viewModelScope.launch {
            when (val result = installer.install(prepared)) {
                is InstallResult.Failed -> mutableState.value = AddThemeState.Failed(address = prepared.source, failure = result.failure)
                is InstallResult.Installed -> {
                    mutableState.value = AddThemeState.Editing(address = "", hint = null)
                    installedChannel.send(result.theme.id)
                }
            }
        }
    }

    override fun onCleared() {
        val preview = mutableState.value as? AddThemeState.Preview
        if (preview != null) installer.discard(preview.prepared)
        super.onCleared()
    }
}
