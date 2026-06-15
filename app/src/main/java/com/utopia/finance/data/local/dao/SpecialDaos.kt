package com.utopia.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.utopia.finance.data.local.entity.CreditBillEntity
import com.utopia.finance.data.local.entity.DebtEntity
import com.utopia.finance.data.local.entity.EconomicNoteEntity
import com.utopia.finance.data.local.entity.ExchangeRateEntity
import com.utopia.finance.data.local.entity.InvestmentEntity
import com.utopia.finance.data.local.entity.LendingEntity
import com.utopia.finance.data.local.entity.SubscriptionEntity
import com.utopia.finance.domain.model.CurrencyCode
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<InvestmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(investment: InvestmentEntity): Long
}

@Dao
interface LendingDao {
    @Query("SELECT * FROM lending ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<LendingEntity>>

    @Query("SELECT * FROM lending ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<LendingEntity>

    @Query("SELECT * FROM lending WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LendingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lending: LendingEntity): Long

    @Update
    suspend fun update(lending: LendingEntity)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DebtEntity?

    @Query("SELECT * FROM debts WHERE counterpartyId = :counterpartyId AND currency = :currency ORDER BY createdAtMillis DESC")
    suspend fun getByCounterpartyAndCurrency(counterpartyId: Long, currency: CurrencyCode): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)
}

@Dao
interface CreditBillDao {
    @Query("SELECT * FROM credit_bills ORDER BY billEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<CreditBillEntity>>

    @Query("SELECT * FROM credit_bills ORDER BY billEpochDay DESC, id DESC")
    suspend fun getAll(): List<CreditBillEntity>

    @Query("SELECT * FROM credit_bills WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CreditBillEntity?

    @Query("SELECT * FROM credit_bills WHERE status != 'PAID' ORDER BY dueEpochDay, id")
    suspend fun getUnpaid(): List<CreditBillEntity>

    @Query("SELECT * FROM credit_bills WHERE creditAccountId = :creditAccountId AND currency = :currency AND periodKey = :periodKey LIMIT 1")
    suspend fun findPeriod(creditAccountId: Long, currency: CurrencyCode, periodKey: String): CreditBillEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bill: CreditBillEntity): Long

    @Update
    suspend fun update(bill: CreditBillEntity)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextDueEpochDay, name")
    fun observeActive(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions ORDER BY nextDueEpochDay, name")
    suspend fun getAll(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE isActive = 1 AND nextDueEpochDay <= :epochDay ORDER BY nextDueEpochDay")
    suspend fun getDue(epochDay: Long): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity): Long

    @Update
    suspend fun update(subscription: SubscriptionEntity)
}

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE pair = :pair LIMIT 1")
    suspend fun get(pair: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rate: ExchangeRateEntity)
}

@Dao
interface EconomicNoteDao {
    @Query("SELECT * FROM economic_notes ORDER BY occurredEpochDay DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<EconomicNoteEntity>>

    @Query("SELECT * FROM economic_notes WHERE occurredEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY occurredEpochDay DESC, id DESC")
    suspend fun getBetween(startEpochDay: Long, endEpochDay: Long): List<EconomicNoteEntity>

    @Query("SELECT * FROM economic_notes ORDER BY occurredEpochDay DESC, id DESC")
    suspend fun getAll(): List<EconomicNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: EconomicNoteEntity): Long

    @Query("DELETE FROM economic_notes WHERE id = :id")
    suspend fun delete(id: Long)
}
