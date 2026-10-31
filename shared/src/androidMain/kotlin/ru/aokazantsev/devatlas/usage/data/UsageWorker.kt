package ru.aokazantsev.devatlas.usage.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.aokazantsev.devatlas.di.AppGraphHolder

class UsageWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val holder = applicationContext as? AppGraphHolder ?: return Result.success()
        holder.graph.usageStatistics.send()
        return Result.success()
    }
}
