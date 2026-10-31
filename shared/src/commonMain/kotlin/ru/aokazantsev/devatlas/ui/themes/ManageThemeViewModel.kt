package ru.aokazantsev.devatlas.ui.themes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.themes.domain.ThemeInstaller
import ru.aokazantsev.devatlas.themes.domain.ThemeLibrary
import ru.aokazantsev.devatlas.themes.domain.ThemeUpdater
import ru.aokazantsev.devatlas.themes.domain.model.ThemeRemoval
import ru.aokazantsev.devatlas.usage.domain.UsageStatistics
import ru.aokazantsev.devatlas.usage.domain.model.UsageEvent

@AssistedInject
class ManageThemeViewModel(
    @Assisted private val themeId: String,
    @Assisted private val savedState: SavedStateHandle,
    private val library: ThemeLibrary,
    updater: ThemeUpdater,
    installer: ThemeInstaller,
    private val usage: UsageStatistics,
) : ViewModel() {

    val update = ThemeUpdateController(themeId, updater, installer, viewModelScope)

    private val mutableState = MutableStateFlow(
        ManageState(deleteTheme = savedState[DELETE_THEME_KEY] ?: false, resetProgress = savedState[RESET_PROGRESS_KEY] ?: false),
    )
    val state: StateFlow<ManageState> = mutableState.asStateFlow()

    private val removalChannel = Channel<ThemeRemoval>(Channel.BUFFERED)
    val removals: Flow<ThemeRemoval> = removalChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            library.observeInstalled()
                .catch { error -> if (error is SQLiteException) showDatabaseFailure(error) else throw error }
                .collect { themes ->
                    val theme = themes.firstOrNull { it.id == themeId }
                    mutableState.update { state ->
                        val untouched = !state.loaded || state.sourceAddress == state.theme?.sourceUrl
                        state.copy(
                            loaded = true,
                            theme = theme,
                            sourceAddress = if (untouched) theme?.sourceUrl.orEmpty() else state.sourceAddress,
                        )
                    }
                }
        }
    }

    fun setSourceAddress(address: String) {
        mutableState.update { it.copy(sourceAddress = address) }
    }

    fun saveSource() {
        val current = mutableState.value
        val address = current.sourceAddress.trim()
        if (address.isEmpty() || address.substringBefore('#') == current.theme?.sourceUrl) return
        update.check(address)
    }

    fun setDeleteTheme(enabled: Boolean) {
        savedState[DELETE_THEME_KEY] = enabled
        mutableState.update { it.copy(deleteTheme = enabled, notice = null) }
    }

    fun setResetProgress(enabled: Boolean) {
        savedState[RESET_PROGRESS_KEY] = enabled
        mutableState.update { it.copy(resetProgress = enabled, notice = null) }
    }

    private fun showDatabaseFailure(error: SQLiteException) {
        mutableState.update { it.copy(notice ="Не удалось прочитать базу приложения: ${error.message ?: "ошибка базы"}") }
    }

    fun requestRemoval() {
        val current = mutableState.value
        val theme = current.theme ?: return
        if (!current.canContinue) return
        viewModelScope.launch {
            val viewed: Int
            val bookmarked: Int
            try {
                viewed = library.viewedCount(themeId)
                bookmarked = library.bookmarkCount(themeId)
            } catch (error: SQLiteException) {
                showDatabaseFailure(error)
                return@launch
            }
            val isCurrent = library.observeCurrent().first()?.id == themeId
            mutableState.update {
                it.copy(
                    confirm = RemovalConfirm(
                        themeTitle = theme.title,
                        deleteTheme = current.deleteTheme,
                        resetProgress = current.resetProgress,
                        viewedCount = viewed,
                        bookmarkCount = bookmarked,
                        isCurrent = isCurrent,
                    ),
                )
            }
        }
    }

    fun cancelRemoval() {
        mutableState.update { it.copy(confirm = null) }
    }

    fun confirmRemoval() {
        val confirm = mutableState.value.confirm ?: return
        mutableState.update { it.copy(busy = true, confirm = null) }
        viewModelScope.launch {
            val outcome = library.remove(themeId, deleteTheme = confirm.deleteTheme, resetProgress = confirm.resetProgress)
            if (outcome !is ThemeRemoval.Failed) {
                if (confirm.resetProgress) usage.hit(UsageEvent.PROGRESS_RESET)
                if (confirm.deleteTheme) usage.hit(UsageEvent.THEME_DELETE)
            }
            when (outcome) {
                ThemeRemoval.ProgressReset -> {
                    savedState[DELETE_THEME_KEY] = false
                    savedState[RESET_PROGRESS_KEY] = false
                    mutableState.update { it.copy(busy = false, deleteTheme = false, resetProgress = false, notice = "Прогресс сброшен") }
                }
                is ThemeRemoval.Failed -> mutableState.update { it.copy(busy = false, notice = outcome.failure.message) }
                else -> removalChannel.send(outcome)
            }
        }
    }

    override fun onCleared() {
        update.release()
        super.onCleared()
    }

    @AssistedFactory
    fun interface Factory {
        fun create(themeId: String, savedState: SavedStateHandle): ManageThemeViewModel
    }

    private companion object {
        const val DELETE_THEME_KEY = "deleteTheme"
        const val RESET_PROGRESS_KEY = "resetProgress"
    }
}
