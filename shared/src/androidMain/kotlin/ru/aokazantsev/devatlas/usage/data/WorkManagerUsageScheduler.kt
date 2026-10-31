package ru.aokazantsev.devatlas.usage.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.aokazantsev.devatlas.usage.domain.UsageScheduler
import java.util.concurrent.TimeUnit.HOURS

internal class WorkManagerUsageScheduler(private val context: Context) : UsageScheduler {

    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<UsageWorker>(INTERVAL_HOURS, HOURS)
            .setInitialDelay(INTERVAL_HOURS, HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "usage-statistics"
        const val INTERVAL_HOURS = 12L
    }
}
