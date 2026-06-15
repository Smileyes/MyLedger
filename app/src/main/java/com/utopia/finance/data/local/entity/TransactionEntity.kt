package com.utopia.finance.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("accountId"),
        Index("occurredAtMillis"),
        Index("type"),
        Index("status"),
        Index("subscriptionId", "periodKey", unique = true),
        Index("creditBillId"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long?,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: CurrencyCode = CurrencyCode.CNY,
    val status: PendingStatus = PendingStatus.CONFIRMED,
    val occurredAtMillis: Long = System.currentTimeMillis(),
    val description: String = "",
    val incomeSourceId: Long? = null,
    val expenseCategoryId: Long? = null,
    val counterpartyId: Long? = null,
    val investmentId: Long? = null,
    val lendingId: Long? = null,
    val debtId: Long? = null,
    val subscriptionId: Long? = null,
    val creditBillId: Long? = null,
    val periodKey: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)
