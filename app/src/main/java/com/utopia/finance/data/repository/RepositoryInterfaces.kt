package com.utopia.finance.data.repository

import com.utopia.finance.data.local.entity.AccountEntity
import com.utopia.finance.data.local.entity.CounterpartyEntity
import com.utopia.finance.data.local.entity.CreditBillEntity
import com.utopia.finance.data.local.entity.DebtEntity
import com.utopia.finance.data.local.entity.EconomicNoteEntity
import com.utopia.finance.data.local.entity.ExpenseCategoryEntity
import com.utopia.finance.data.local.entity.IncomeSourceEntity
import com.utopia.finance.data.local.entity.InvestmentEntity
import com.utopia.finance.data.local.entity.LendingEntity
import com.utopia.finance.data.local.entity.SubscriptionEntity
import com.utopia.finance.data.local.entity.TransactionEntity
import com.utopia.finance.domain.model.AssetSummary
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccounts(): List<AccountEntity>
    suspend fun accountBalances(): Map<Long, Long>
    suspend fun correctAccountBalance(accountId: Long, currentBalanceMinor: Long)
    suspend fun saveAccount(account: AccountEntity): Long
    suspend fun archiveAccount(id: Long)
    suspend fun assetSummary(asOfEpochDay: Long): AssetSummary
}

interface TransactionRepository {
    fun observeRecent(limit: Int = 100): Flow<List<TransactionEntity>>
    fun observePending(): Flow<List<TransactionEntity>>
    fun observeCreditBills(): Flow<List<CreditBillEntity>>
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<TransactionEntity>
    suspend fun getAll(): List<TransactionEntity>
    suspend fun getCreditBills(): List<CreditBillEntity>
    suspend fun saveTransaction(transaction: TransactionEntity): Long
    suspend fun recordIncome(
        accountId: Long,
        incomeSourceId: Long?,
        currency: CurrencyCode,
        amountMinor: Long,
        description: String,
        occurredAtMillis: Long,
    ): Long
    suspend fun recordExpense(
        paymentAccountId: Long,
        repaymentAccountId: Long?,
        expenseCategoryId: Long?,
        currency: CurrencyCode,
        amountMinor: Long,
        description: String,
        occurredAtMillis: Long,
        repaymentAtMillis: Long,
    ): Long
    suspend fun recordTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        fromAmountMinor: Long,
        toAmountMinor: Long,
        description: String,
        occurredAtMillis: Long,
    )
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun setStatus(id: Long, status: PendingStatus)
    suspend fun generateCreditBills(asOfEpochDay: Long): Int
    suspend fun confirmPending(id: Long, repaymentAccountId: Long? = null)
    suspend fun skipPending(id: Long)
    suspend fun deleteTransaction(id: Long)
}

interface CatalogRepository {
    fun observeIncomeSources(): Flow<List<IncomeSourceEntity>>
    fun observeExpenseCategories(): Flow<List<ExpenseCategoryEntity>>
    fun observeCounterparties(): Flow<List<CounterpartyEntity>>
    suspend fun getIncomeSources(): List<IncomeSourceEntity>
    suspend fun getExpenseCategories(): List<ExpenseCategoryEntity>
    suspend fun getCounterparties(): List<CounterpartyEntity>
    suspend fun addIncomeSource(name: String, defaultAccountId: Long? = null): Long
    suspend fun updateIncomeSource(source: IncomeSourceEntity)
    suspend fun addExpenseCategory(name: String, defaultAccountId: Long? = null): Long
    suspend fun updateExpenseCategory(category: ExpenseCategoryEntity)
    suspend fun addCounterparty(name: String): Long
    suspend fun deleteIncomeSource(id: Long)
    suspend fun deleteExpenseCategory(id: Long)
    suspend fun seedDefaults()
}

interface AssetRepository {
    fun observeInvestments(): Flow<List<InvestmentEntity>>
    fun observeLending(): Flow<List<LendingEntity>>
    fun observeDebts(): Flow<List<DebtEntity>>
    suspend fun getInvestments(): List<InvestmentEntity>
    suspend fun getLending(): List<LendingEntity>
    suspend fun getDebts(): List<DebtEntity>
    suspend fun addInvestment(investment: InvestmentEntity): Long
    suspend fun addLending(lending: LendingEntity): Long
    suspend fun addDebt(debt: DebtEntity): Long
    suspend fun updateLending(lending: LendingEntity)
    suspend fun updateDebt(debt: DebtEntity)
}

interface SubscriptionRepository {
    fun observeActive(): Flow<List<SubscriptionEntity>>
    suspend fun getAll(): List<SubscriptionEntity>
    suspend fun saveSubscription(subscription: SubscriptionEntity): Long
    suspend fun generateDuePending(asOfEpochDay: Long): Int
    suspend fun confirmPending(transactionId: Long)
    suspend fun skipPending(transactionId: Long)
}

interface EconomicNoteRepository {
    fun observeRecent(limit: Int = 100): Flow<List<EconomicNoteEntity>>
    suspend fun getBetween(startEpochDay: Long, endEpochDay: Long): List<EconomicNoteEntity>
    suspend fun getAll(): List<EconomicNoteEntity>
    suspend fun addNote(note: EconomicNoteEntity): Long
    suspend fun deleteNote(id: Long)
}
