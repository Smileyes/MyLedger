package com.utopia.finance.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.utopia.finance.FinanceApplication
import java.time.LocalDate

class SubscriptionDueWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        runCatching {
            val app = applicationContext as FinanceApplication
            val today = LocalDate.now().toEpochDay()
            app.container.subscriptionRepository.generateDuePending(today)
            app.container.transactionRepository.generateCreditBills(today)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}
