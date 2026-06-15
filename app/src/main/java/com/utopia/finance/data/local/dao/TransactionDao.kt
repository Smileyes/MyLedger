package com.utopia.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.utopia.finance.data.local.entity.TransactionEntity
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE status != 'PENDING' ORDER BY occurredAtMillis DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE occurredAtMillis BETWEEN :startMillis AND :endMillis ORDER BY occurredAtMillis DESC, id DESC")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY occurredAtMillis DESC, id DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY occurredAtMillis")
    fun observeByStatus(status: PendingStatus): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE subscriptionId = :subscriptionId AND periodKey = :periodKey LIMIT 1")
    suspend fun findSubscriptionPeriod(subscriptionId: Long, periodKey: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' AND type = 'DEBT_REPAYMENT' AND debtId = :debtId LIMIT 1")
    suspend fun findPendingDebtRepayment(debtId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' AND type = 'DEBT_REPAYMENT' AND creditBillId = :creditBillId LIMIT 1")
    suspend fun findPendingCreditBillRepayment(creditBillId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = 'CONFIRMED' AND type = 'EXPENSE' AND accountId = :accountId AND currency = :currency AND creditBillId IS NULL ORDER BY occurredAtMillis, id")
    suspend fun findUnbilledCreditExpenses(accountId: Long, currency: CurrencyCode): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET status = :status, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateStatus(id: Long, status: PendingStatus, updatedAtMillis: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET creditBillId = :creditBillId, updatedAtMillis = :updatedAtMillis WHERE status = 'CONFIRMED' AND type = 'EXPENSE' AND accountId = :accountId AND currency = :currency AND creditBillId IS NULL")
    suspend fun attachUnbilledCreditExpenses(accountId: Long, currency: CurrencyCode, creditBillId: Long, updatedAtMillis: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
