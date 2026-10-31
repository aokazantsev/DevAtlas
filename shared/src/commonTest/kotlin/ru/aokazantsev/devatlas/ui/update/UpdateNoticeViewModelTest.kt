package ru.aokazantsev.devatlas.ui.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ru.aokazantsev.devatlas.testing.ViewModelScopes
import ru.aokazantsev.devatlas.update.domain.AppUpdateChecker
import ru.aokazantsev.devatlas.update.domain.model.AppRelease
import ru.aokazantsev.devatlas.update.domain.model.AppVersion
import ru.aokazantsev.devatlas.update.domain.model.UpdateCheck
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateNoticeViewModelTest {

    private val scopes = ViewModelScopes()
    private val release = AppRelease(AppVersion(listOf(0, 2)), bodyHtml = "", apk = null)
    private val found = UpdateCheck.Available(release, listOf(release), hasOlderChanges = false)
    private val announced = mutableListOf<UpdateCheck.Available>()
    private var due: UpdateCheck.Available? = found

    private val checker = object : AppUpdateChecker {
        override suspend fun check(): UpdateCheck = error("не используется")

        override suspend fun checkIfDue(): UpdateCheck.Available? = due

        override suspend fun markAnnounced(check: UpdateCheck.Available) {
            announced += check
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        scopes.cancelAll()
        Dispatchers.resetMain()
    }

    @Test
    fun foundVersionIsShownAndAnnouncedOnceWhenDismissed() = runTest {
        val viewModel = scopes.track(UpdateNoticeViewModel(checker))
        advanceUntilIdle()
        assertEquals(found, viewModel.notice.value)

        viewModel.dismiss()
        viewModel.dismiss()
        advanceUntilIdle()

        assertNull(viewModel.notice.value)
        assertEquals(listOf(found), announced)
    }

    @Test
    fun nothingDueShowsNothing() = runTest {
        due = null
        val viewModel = scopes.track(UpdateNoticeViewModel(checker))
        advanceUntilIdle()

        assertNull(viewModel.notice.value)
    }
}
