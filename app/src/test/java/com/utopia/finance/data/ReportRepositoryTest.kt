package com.utopia.finance.data

import com.utopia.finance.data.local.entity.AccountEntity
import com.utopia.finance.data.local.entity.CounterpartyEntity
import com.utopia.finance.data.local.entity.CreditBillEntity
import com.utopia.finance.data.local.entity.EconomicNoteEntity
import com.utopia.finance.data.local.entity.ExpenseCategoryEntity
import com.utopia.finance.data.local.entity.IncomeSourceEntity
import com.utopia.finance.data.local.entity.TransactionEntity
import com.utopia.finance.data.repository.AccountRepository
import com.utopia.finance.data.repository.CatalogRepository
import com.utopia.finance.data.repository.DefaultReportRepository
import com.utopia.finance.data.repository.EconomicNoteRepository
import com.utopia.finance.data.repository.ExchangeRateRepository
import com.utopia.finance.data.repository.ExchangeRateSnapshot
import com.utopia.finance.data.repository.ReportPeriodType
import com.utopia.finance.data.repository.TransactionRepository
import com.utopia.finance.domain.model.AssetSummary
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRepositoryTest {
    @Test
    fun `monthly report includes economic notes for investment habit analysis`() = runTest {
        val note = EconomicNoteEntity(
            id = 1,
            occurredEpochDay = LocalDate.of(2026, 6, 13).toEpochDay(),
            operationType = "基金定投",
            content = "看到下跌后没有追涨杀跌，按计划小额买入。",
        )
        val repository = DefaultReportRepository(
            accountRepository = FakeAccountRepository,
            transactionRepository = FakeTransactionRepository,
            catalogRepository = FakeCatalogRepository,
            exchangeRateRepository = FakeExchangeRateRepository,
            economicNoteRepository = FakeEconomicNoteRepository(listOf(note)),
        )

        val report = repository.generate(ReportPeriodType.MONTH, LocalDate.of(2026, 6, 13))

        assertTrue(report.markdown.contains("经济操作笔记"))
        assertTrue(report.markdown.contains("基金定投"))
        assertTrue(report.markdown.contains("投资习惯"))
        assertTrue(report.notesCsv.contains("基金定投"))
        assertTrue(report.notesCsv.contains("按计划小额买入"))
    }
}

private object FakeAccountRepository : AccountRepository {
    override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(emptyList())
    override suspend fun getAccounts(): List<AccountEntity> = emptyList()
    override suspend fun accountBalances(): Map<Long, Long> = emptyMap()
    override suspend fun correctAccountBalance(accountId: Long, currentBalanceMinor: Long) = error("unused")
    override suspend fun saveAccount(account: AccountEntity): Long = error("unused")
    override suspend fun archiveAccount(id: Long) = error("unused")
    override suspend fun assetSummary(asOfEpochDay: Long): AssetSummary = error("unused")
}

private object FakeTransactionRepository : TransactionRepository {
    override fun observeRecent(limit: Int): Flow<List<TransactionEntity>> = flowOf(emptyList())
    override fun observePending(): Flow<List<TransactionEntity>> = flowOf(emptyList())
    override fun observeCreditBills(): Flow<List<CreditBillEntity>> = flowOf(emptyList())
    override suspend fun getBetween(startMillis: Long, endMillis: Long): List<TransactionEntity> = emptyList()
    override suspend fun getAll(): List<TransactionEntity> = emptyList()
    override suspend fun getCreditBills(): List<CreditBillEntity> = emptyList()
    override suspend fun saveTransaction(transaction: TransactionEntity): Long = error("unused")
    override suspend fun recordIncome(
        accountId: Long,
        incomeSourceId: Long?,
        currency: CurrencyCode,
        amountMinor: Long,
        description: String,
        occurredAtMillis: Long,
    ): Long = error("unused")
    override suspend fun recordExpense(
        paymentAccountId: Long,
        repaymentAccountId: Long?,
        expenseCategoryId: Long?,
        currency: CurrencyCode,
        amountMinor: Long,
        description: String,
        occurredAtMillis: Long,
        repaymentAtMillis: Long,
    ): Long = error("unused")
    override suspend fun recordTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        fromAmountMinor: Long,
        toAmountMinor: Long,
        description: String,
        occurredAtMillis: Long,
    ) = error("unused")
    override suspend fun updateTransaction(transaction: TransactionEntity) = error("unused")
    override suspend fun setStatus(id: Long, status: PendingStatus) = error("unused")
    override suspend fun generateCreditBills(asOfEpochDay: Long): Int = error("unused")
    override suspend fun confirmPending(id: Long, repaymentAccountId: Long?) = error("unused")
    override suspend fun skipPending(id: Long) = error("unused")
    override suspend fun deleteTransaction(id: Long) = error("unused")
}

private object FakeCatalogRepository : CatalogRepository {
    override fun observeIncomeSources(): Flow<List<IncomeSourceEntity>> = flowOf(emptyList())
    override fun observeExpenseCategories(): Flow<List<ExpenseCategoryEntity>> = flowOf(emptyList())
    override fun observeCounterparties(): Flow<List<CounterpartyEntity>> = flowOf(emptyList())
    override suspend fun getIncomeSources(): List<IncomeSourceEntity> = emptyList()
    override suspend fun getExpenseCategories(): List<ExpenseCategoryEntity> = emptyList()
    override suspend fun getCounterparties(): List<CounterpartyEntity> = emptyList()
    override suspend fun addIncomeSource(name: String, defaultAccountId: Long?): Long = error("unused")
    override suspend fun updateIncomeSource(source: IncomeSourceEntity) = error("unused")
    override suspend fun addExpenseCategory(name: String, defaultAccountId: Long?): Long = error("unused")
    override suspend fun updateExpenseCategory(category: ExpenseCategoryEntity) = error("unused")
    override suspend fun addCounterparty(name: String): Long = error("unused")
    override suspend fun deleteIncomeSource(id: Long) = error("unused")
    override suspend fun deleteExpenseCategory(id: Long) = error("unused")
    override suspend fun seedDefaults() = error("unused")
}

private object FakeExchangeRateRepository : ExchangeRateRepository {
    override suspend fun usdCny(): ExchangeRateSnapshot? = null
    override suspend fun refreshUsdCny(): ExchangeRateSnapshot =
        ExchangeRateSnapshot("USD_CNY", BigDecimal.ONE, 0, isCached = true)

    override suspend fun convertToCny(amountMinor: Long, currency: CurrencyCode): Long = amountMinor
}

private class FakeEconomicNoteRepository(
    private val notes: List<EconomicNoteEntity>,
) : EconomicNoteRepository {
    override fun observeRecent(limit: Int): Flow<List<EconomicNoteEntity>> = flowOf(notes)
    override suspend fun getBetween(startEpochDay: Long, endEpochDay: Long): List<EconomicNoteEntity> =
        notes.filter { it.occurredEpochDay in startEpochDay..endEpochDay }

    override suspend fun getAll(): List<EconomicNoteEntity> = notes
    override suspend fun addNote(note: EconomicNoteEntity): Long = error("unused")
    override suspend fun deleteNote(id: Long) = error("unused")
}
