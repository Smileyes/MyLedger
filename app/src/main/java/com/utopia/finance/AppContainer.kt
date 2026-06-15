package com.utopia.finance

import android.content.Context
import com.utopia.finance.data.local.FinanceDatabase
import com.utopia.finance.data.repository.AccountRepository
import com.utopia.finance.data.repository.AssetRepository
import com.utopia.finance.data.repository.BackupRepository
import com.utopia.finance.data.repository.CatalogRepository
import com.utopia.finance.data.repository.DataStoreSettingsRepository
import com.utopia.finance.data.repository.DefaultAccountRepository
import com.utopia.finance.data.repository.DefaultAssetRepository
import com.utopia.finance.data.repository.DefaultCatalogRepository
import com.utopia.finance.data.repository.DefaultEconomicNoteRepository
import com.utopia.finance.data.repository.DefaultReportRepository
import com.utopia.finance.data.repository.DefaultSubscriptionRepository
import com.utopia.finance.data.repository.DefaultTransactionRepository
import com.utopia.finance.data.repository.EcbExchangeRateRepository
import com.utopia.finance.data.repository.EncryptedBackupRepository
import com.utopia.finance.data.repository.ExchangeRateRepository
import com.utopia.finance.data.repository.EconomicNoteRepository
import com.utopia.finance.data.repository.ReportRepository
import com.utopia.finance.data.repository.SettingsRepository
import com.utopia.finance.data.repository.SubscriptionRepository
import com.utopia.finance.data.repository.TransactionRepository

class AppContainer(context: Context) {
    val database: FinanceDatabase = FinanceDatabase.create(context)
    val accountRepository: AccountRepository = DefaultAccountRepository(
        accountDao = database.accountDao(),
        transactionDao = database.transactionDao(),
        investmentDao = database.investmentDao(),
        lendingDao = database.lendingDao(),
        debtDao = database.debtDao(),
        creditBillDao = database.creditBillDao(),
    )
    val transactionRepository: TransactionRepository = DefaultTransactionRepository(database)
    val catalogRepository: CatalogRepository = DefaultCatalogRepository(database.accountDao(), database.catalogDao())
    val assetRepository: AssetRepository = DefaultAssetRepository(
        investmentDao = database.investmentDao(),
        lendingDao = database.lendingDao(),
        debtDao = database.debtDao(),
    )
    val subscriptionRepository: SubscriptionRepository =
        DefaultSubscriptionRepository(database.subscriptionDao(), database.transactionDao())
    val exchangeRateRepository: ExchangeRateRepository = EcbExchangeRateRepository(database.exchangeRateDao())
    val economicNoteRepository: EconomicNoteRepository = DefaultEconomicNoteRepository(database.economicNoteDao())
    val reportRepository: ReportRepository = DefaultReportRepository(
        accountRepository = accountRepository,
        transactionRepository = transactionRepository,
        catalogRepository = catalogRepository,
        exchangeRateRepository = exchangeRateRepository,
        economicNoteRepository = economicNoteRepository,
    )
    val backupRepository: BackupRepository = EncryptedBackupRepository(database)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context.applicationContext)
}
