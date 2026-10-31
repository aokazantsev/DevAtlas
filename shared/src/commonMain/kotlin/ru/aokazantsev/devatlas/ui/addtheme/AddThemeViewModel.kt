package ru.aokazantsev.devatlas.ui.addtheme

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure

@AssistedInject
class AddThemeViewModel(
    @Assisted private val savedState: SavedStateHandle,
    private val installer: ThemeInstaller,
    private val library: ThemeLibrary,
    private val repository: InstalledThemeRepository,
    private val platform: PlatformBindings,
    private val endpoints: NetworkEndpoints,
) : ViewModel() {

    private val mutableState = MutableStateFlow<AddThemeState>(
        AddThemeState.Editing(address = savedState[ADDRESS_KEY] ?: "", hint = null),
    )
    val state: StateFlow<AddThemeState> = mutableState.asStateFlow()

    val sampleAvailable: Flow<Boolean> = library.observeInstalled().map { themes -> themes.none { it.id == SampleTheme.ID } }

    private val installedChannel = Channel<String>(Channel.BUFFERED)
    val installed: Flow<String> = installedChannel.receiveAsFlow()

    private var work: Job? = null

    fun setAddress(address: String) {
        edit(address)
    }

    fun paste() {
        val text = platform.clipboardText()?.trim().orEmpty()
        if (text.isEmpty()) edit("", hint = "В буфере обмена пусто") else edit(text)
    }

    private fun edit(address: String, hint: String? = null) {
        savedState[ADDRESS_KEY] = address
        mutableState.value = AddThemeState.Editing(address = address, hint = hint)
    }

    fun startWithSample() {
        load(endpoints.sampleThemeUrl)
    }

    fun load(address: String) {
        work?.cancel()
        savedState[ADDRESS_KEY] = address
        mutableState.value = AddThemeState.Loading(message = "Скачиваю и проверяю тему…")
        work = viewModelScope.launch {
            mutableState.value = when (val result = installer.prepare(address)) {
                is PrepareResult.Failed -> AddThemeState.Failed(address = address, failure = result.failure)
                is PrepareResult.Ready -> preview(result.prepared)
            }
        }
    }

    private suspend fun preview(prepared: PreparedTheme): AddThemeState = try {
        AddThemeState.Preview(prepared = prepared, existing = repository.find(prepared.manifest.id), confirmReplace = false)
    } catch (cancellation: CancellationException) {
        installer.discard(prepared)
        throw cancellation
    } catch (error: SQLiteException) {
        installer.discard(prepared)
        AddThemeState.Failed(
            address = prepared.source,
            failure = ThemeFailure("Не удалось прочитать базу приложения: ${error.message ?: "ошибка базы"}"),
        )
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
        edit(preview.prepared.source)
    }

    fun backToEditing() {
        val failed = mutableState.value as? AddThemeState.Failed ?: return
        edit(failed.address)
    }

    private fun commit(prepared: PreparedTheme) {
        mutableState.value = AddThemeState.Loading(message = "Устанавливаю тему…")
        work = viewModelScope.launch {
            when (val result = installer.install(prepared)) {
                is InstallResult.Failed -> mutableState.value = AddThemeState.Failed(address = prepared.source, failure = result.failure)
                is InstallResult.Installed -> {
                    edit("")
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

    @AssistedFactory
    fun interface Factory {
        fun create(savedState: SavedStateHandle): AddThemeViewModel
    }

    private companion object {
        const val ADDRESS_KEY = "address"
    }
}
