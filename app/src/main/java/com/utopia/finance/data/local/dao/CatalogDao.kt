package com.utopia.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.utopia.finance.data.local.entity.CounterpartyEntity
import com.utopia.finance.data.local.entity.ExpenseCategoryEntity
import com.utopia.finance.data.local.entity.IncomeSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM income_sources WHERE isArchived = 0 ORDER BY name")
    fun observeIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM expense_categories WHERE isArchived = 0 ORDER BY name")
    fun observeExpenseCategories(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM counterparties WHERE isArchived = 0 ORDER BY name")
    fun observeCounterparties(): Flow<List<CounterpartyEntity>>

    @Query("SELECT * FROM income_sources ORDER BY name")
    suspend fun getIncomeSources(): List<IncomeSourceEntity>

    @Query("SELECT * FROM expense_categories ORDER BY name")
    suspend fun getExpenseCategories(): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM counterparties ORDER BY name")
    suspend fun getCounterparties(): List<CounterpartyEntity>

    @Query("SELECT * FROM counterparties WHERE name = :name AND isArchived = 0 LIMIT 1")
    suspend fun findCounterpartyByName(name: String): CounterpartyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(source: IncomeSourceEntity): Long

    @Update
    suspend fun updateIncomeSource(source: IncomeSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategory(category: ExpenseCategoryEntity): Long

    @Update
    suspend fun updateExpenseCategory(category: ExpenseCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounterparty(counterparty: CounterpartyEntity): Long

    @Query("UPDATE income_sources SET isArchived = 1 WHERE id = :id")
    suspend fun archiveIncomeSource(id: Long)

    @Query("UPDATE expense_categories SET isArchived = 1 WHERE id = :id")
    suspend fun archiveExpenseCategory(id: Long)
}
