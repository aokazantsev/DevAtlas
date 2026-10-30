package ru.aokazantsev.devatlas.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel

class ViewModelScopes {

    private val viewModels = mutableListOf<ViewModel>()

    fun <T : ViewModel> track(viewModel: T): T = viewModel.also { viewModels += it }

    fun cancelAll() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
    }
}
