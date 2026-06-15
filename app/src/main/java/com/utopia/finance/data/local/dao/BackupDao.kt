package com.utopia.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.utopia.finance.data.local.entity.AccountEntity
import com.utopia.finance.data.local.entity.CounterpartyEntity
import com.utopia.finance.data.local.entity.CreditBillEntity
import com.utopia.finance.data.local.entity.DebtEntity
import com.utopia.finance.data.local.entity.EconomicNoteEntity
import com.utopia.finance.data.local.entity.ExchangeRateEntity
import com.utopia.finance.data.local.entity.ExpenseCategoryEntity
import com.utopia.finance.data.local.entity.IncomeSourceEntity
import com.utopia.finance.data.local.entity.InvestmentEntity
import com.utopia.finance.data.local.entity.LendingEntity
import com.utopia.finance.data.local.entity.SubscriptionEntity
import com.utopia.finance.data.local.entity.TransactionEntity

@Dao
interface BackupDao {
    @Query("SELECT * FROM accounts")
    suspend fun accounts(): List<AccountEntity>

    @Query("SELECT * FROM income_sources")
    suspend fun incomeSources(): List<IncomeSourceEntity>

    @Query("SELECT * FROM expense_categories")
    suspend fun expenseCategories(): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM counterparties")
    suspend fun counterparties(): List<CounterpartyEntity>

    @Query("SELECT * FROM transactions")
    suspend fun transactions(): List<TransactionEntity>

    @Query("SELECT * FROM investments")
    suspend fun investments(): List<InvestmentEntity>

    @Query("SELECT * FROM lending")
    suspend fun lending(): List<LendingEntity>

    @Query("SELECT * FROM debts")
    suspend fun debts(): List<DebtEntity>

    @Query("SELECT * FROM credit_bills")
    suspend fun creditBills(): List<CreditBillEntity>

    @Query("SELECT * FROM subscriptions")
    suspend fun subscriptions(): List<SubscriptionEntity>

    @Query("SELECT * FROM exchange_rates")
    suspend fun exchangeRates(): List<ExchangeRateEntity>

    @Query("SELECT * FROM economic_notes")
    suspend fun economicNotes(): List<EconomicNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(items: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSources(items: List<IncomeSourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategories(items: List<ExpenseCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounterparties(items: List<CounterpartyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(items: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestments(items: List<InvestmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLending(items: List<LendingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(items: List<DebtEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditBills(items: List<CreditBillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(items: List<SubscriptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRates(items: List<ExchangeRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEconomicNotes(items: List<EconomicNoteEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM subscriptions")
    suspend fun clearSubscriptions()

    @Query("DELETE FROM lending")
    suspend fun clearLending()

    @Query("DELETE FROM debts")
    suspend fun clearDebts()

    @Query("DELETE FROM credit_bills")
    suspend fun clearCreditBills()

    @Query("DELETE FROM investments")
    suspend fun clearInvestments()

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM income_sources")
    suspend fun clearIncomeSources()

    @Query("DELETE FROM expense_categories")
    suspend fun clearExpenseCategories()

    @Query("DELETE FROM counterparties")
    suspend fun clearCounterparties()

    @Query("DELETE FROM exchange_rates")
    suspend fun clearExchangeRates()

    @Query("DELETE FROM economic_notes")
    suspend fun clearEconomicNotes()
}
