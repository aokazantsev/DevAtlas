package ru.aokazantsev.devatlas.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.aokazantsev.devatlas.update.domain.AppUpdateChecker
import ru.aokazantsev.devatlas.update.domain.model.UpdateCheck

@Inject
class UpdateNoticeViewModel(
    private val checker: AppUpdateChecker,
) : ViewModel() {

    private val mutableNotice = MutableStateFlow<UpdateCheck.Available?>(null)
    val notice: StateFlow<UpdateCheck.Available?> = mutableNotice.asStateFlow()

    init {
        viewModelScope.launch { mutableNotice.value = checker.checkIfDue() }
    }

    fun dismiss() {
        val shown = mutableNotice.value ?: return
        mutableNotice.value = null
        viewModelScope.launch { checker.markAnnounced(shown) }
    }
}
