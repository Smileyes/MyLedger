package com.utopia.finance

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.utopia.finance.work.SubscriptionDueWorker
import java.util.concurrent.TimeUnit

class FinanceApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        scheduleSubscriptionChecks()
    }

    private fun scheduleSubscriptionChecks() {
        val request = PeriodicWorkRequestBuilder<SubscriptionDueWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "subscription-due-check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
