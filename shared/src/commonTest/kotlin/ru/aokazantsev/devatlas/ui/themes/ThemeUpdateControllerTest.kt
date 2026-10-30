package ru.aokazantsev.devatlas.ui.themes

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import ru.aokazantsev.devatlas.themes.ScriptedInstaller
import ru.aokazantsev.devatlas.themes.ScriptedUpdater
import ru.aokazantsev.devatlas.themes.domain.model.ThemeFailure
import ru.aokazantsev.devatlas.themes.domain.model.UpdateCheck
import ru.aokazantsev.devatlas.themes.domain.model.UpdateSummary
import ru.aokazantsev.devatlas.themes.preparedOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeUpdateControllerTest {

    private val updater = ScriptedUpdater()
    private val installer = ScriptedInstaller()

    private fun TestScope.controller() = ThemeUpdateController("a", updater, installer, this)

    @Test
    fun noChangesGivesMessage() = runTest {
        val controller = controller()

        controller.check()
        assertEquals(UpdateState.Checking, controller.state.value)
        advanceUntilIdle()

        assertEquals(UpdateState.Message("Изменений нет"), controller.state.value)
        assertEquals(listOf("a" to null), updater.requests.toList())
    }

    @Test
    fun failureIsExposed() = runTest {
        val failure = ThemeFailure("нет сети")
        updater.result = UpdateCheck.Failed(failure)
        val controller = controller()

        controller.check()
        advanceUntilIdle()

        assertEquals(UpdateState.Failed(failure), controller.state.value)
        controller.dismiss()
        assertEquals(UpdateState.Idle, controller.state.value)
    }

    @Test
    fun availableUpdateWaitsForConfirmationThenInstalls() = runTest {
        val prepared = preparedOf("a", "new", "p1")
        updater.result = UpdateCheck.Available(prepared, UpdateSummary(added = 1, removed = 0, changed = 2), contentChanged = true)
        val controller = controller()
        controller.check()
        advanceUntilIdle()

        val available = assertIs<UpdateState.Available>(controller.state.value)
        assertEquals(UpdateSummary(1, 0, 2), available.summary)
        assertEquals(emptyList(), installer.installed)

        controller.confirm()
        advanceUntilIdle()

        assertEquals(listOf(prepared), installer.installed)
        assertEquals(UpdateState.Message("Тема обновлена"), controller.state.value)
    }

    @Test
    fun installFailureIsShownInsteadOfHanging() = runTest {
        updater.result = UpdateCheck.Available(preparedOf("a", "new", "p1"), UpdateSummary(1, 0, 0), contentChanged = true)
        installer.installFailure = ThemeFailure("нет места")
        val controller = controller()
        controller.check()
        advanceUntilIdle()

        controller.confirm()
        advanceUntilIdle()

        assertEquals(UpdateState.Failed(ThemeFailure("нет места")), controller.state.value)
        controller.dismiss()
        controller.check()
        advanceUntilIdle()
        assertEquals(2, updater.requests.size)
    }

    @Test
    fun cancelDiscardsDownloadedTheme() = runTest {
        val prepared = preparedOf("a", "new", "p1")
        updater.result = UpdateCheck.Available(prepared, UpdateSummary(0, 0, 1), contentChanged = true)
        val controller = controller()
        controller.check()
        advanceUntilIdle()

        controller.cancel()

        assertEquals(listOf(prepared), installer.discarded)
        assertEquals(UpdateState.Idle, controller.state.value)
        assertEquals(emptyList(), installer.installed)
    }

    @Test
    fun newAddressIsForwardedAndConfirmationSaysSourceChanged() = runTest {
        updater.result = UpdateCheck.Available(preparedOf("a", "same", "p1"), UpdateSummary(0, 0, 0), contentChanged = true)
        val controller = controller()

        controller.check("https://example.com/new.zip")
        advanceUntilIdle()
        controller.confirm()
        advanceUntilIdle()

        assertEquals(listOf("a" to "https://example.com/new.zip"), updater.requests.toList())
        assertEquals(UpdateState.Message("Источник и содержимое обновлены"), controller.state.value)
    }

    @Test
    fun newAddressWithSameContentSaysOnlySourceChanged() = runTest {
        updater.result = UpdateCheck.Available(preparedOf("a", "same", "p1"), UpdateSummary(0, 0, 0), contentChanged = false)
        val controller = controller()

        controller.check("https://example.com/new.zip")
        advanceUntilIdle()
        controller.confirm()
        advanceUntilIdle()

        assertEquals(UpdateState.Message("Источник обновлён"), controller.state.value)
    }

    @Test
    fun releaseDiscardsPendingDownload() = runTest {
        val prepared = preparedOf("a", "new", "p1")
        updater.result = UpdateCheck.Available(prepared, UpdateSummary(1, 0, 0), contentChanged = true)
        val controller = controller()
        controller.check()
        advanceUntilIdle()

        controller.release()

        assertEquals(listOf(prepared), installer.discarded)
    }

    @Test
    fun secondCheckWhileBusyIsIgnored() = runTest {
        val controller = controller()

        controller.check()
        controller.check()
        advanceUntilIdle()

        assertEquals(1, updater.requests.size)
    }
}
