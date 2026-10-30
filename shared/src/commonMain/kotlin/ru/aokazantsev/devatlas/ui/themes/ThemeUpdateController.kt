package ru.aokazantsev.devatlas.ui.themes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.themes.domain.ThemeInstaller
import ru.aokazantsev.devatlas.themes.domain.ThemeUpdater
import ru.aokazantsev.devatlas.themes.domain.model.InstallResult
import ru.aokazantsev.devatlas.themes.domain.model.UpdateCheck

class ThemeUpdateController(
    private val themeId: String,
    private val updater: ThemeUpdater,
    private val installer: ThemeInstaller,
    private val scope: CoroutineScope,
) {

    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = mutableState.asStateFlow()

    fun check(newAddress: String? = null) {
        if (mutableState.value is UpdateState.Checking || mutableState.value is UpdateState.Applying) return
        mutableState.value = UpdateState.Checking
        scope.launch {
            mutableState.value = when (val result = updater.check(themeId, newAddress)) {
                UpdateCheck.NoChanges -> UpdateState.Message("Изменений нет")
                is UpdateCheck.Failed -> UpdateState.Failed(result.failure)
                is UpdateCheck.Available -> UpdateState.Available(
                    prepared = result.prepared,
                    summary = result.summary,
                    sourceChanged = newAddress != null,
                    contentChanged = result.contentChanged,
                )
            }
        }
    }

    fun confirm() {
        val available = mutableState.value as? UpdateState.Available ?: return
        mutableState.value = UpdateState.Applying
        scope.launch {
            mutableState.value = when (val result = installer.install(available.prepared)) {
                is InstallResult.Failed -> UpdateState.Failed(result.failure)
                is InstallResult.Installed -> UpdateState.Message(appliedText(available))
            }
        }
    }

    private fun appliedText(available: UpdateState.Available): String = when {
        !available.sourceChanged -> "Тема обновлена"
        available.contentChanged -> "Источник и содержимое обновлены"
        else -> "Источник обновлён"
    }

    fun cancel() {
        val available = mutableState.value as? UpdateState.Available
        if (available != null) installer.discard(available.prepared)
        mutableState.value = UpdateState.Idle
    }

    fun dismiss() {
        if (mutableState.value is UpdateState.Message || mutableState.value is UpdateState.Failed) mutableState.value = UpdateState.Idle
    }

    fun release() {
        val available = mutableState.value as? UpdateState.Available
        if (available != null) installer.discard(available.prepared)
    }
}
