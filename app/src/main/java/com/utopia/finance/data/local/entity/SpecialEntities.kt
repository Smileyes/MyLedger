package com.utopia.finance.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.utopia.finance.domain.model.BillingPeriod
import com.utopia.finance.domain.model.CreditBillStatus
import com.utopia.finance.domain.model.CurrencyCode
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val direction: String,
    val quantity: String,
    val totalCostMinor: Long,
    val currency: CurrencyCode = CurrencyCode.CNY,
    val description: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "lending",
    foreignKeys = [
        ForeignKey(
            entity = CounterpartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["counterpartyId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("counterpartyId")],
)
data class LendingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterpartyId: Long,
    val principalMinor: Long,
    val repaidMinor: Long = 0,
    val currency: CurrencyCode = CurrencyCode.CNY,
    val description: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(
            entity = CounterpartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["counterpartyId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("counterpartyId")],
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterpartyId: Long,
    val principalMinor: Long,
    val repaidMinor: Long = 0,
    val annualRateBps: Int,
    val startedAtEpochDay: Long,
    val currency: CurrencyCode = CurrencyCode.CNY,
    val description: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "credit_bills",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("creditAccountId"),
        Index("status"),
        Index("creditAccountId", "currency", "periodKey", unique = true),
    ],
)
data class CreditBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditAccountId: Long,
    val currency: CurrencyCode = CurrencyCode.CNY,
    val periodKey: String,
    val billEpochDay: Long,
    val dueEpochDay: Long,
    val amountMinor: Long,
    val repaidMinor: Long = 0,
    val status: CreditBillStatus = CreditBillStatus.UNPAID,
    val description: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("accountId")],
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountId: Long,
    val amountMinor: Long,
    val currency: CurrencyCode = CurrencyCode.CNY,
    val period: BillingPeriod = BillingPeriod.MONTHLY,
    val nextDueEpochDay: Long,
    val isActive: Boolean = true,
    val description: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val pair: String,
    val rateScaled: Long,
    val scale: Int,
    val asOfEpochDay: Long,
    val fetchedAtMillis: Long,
)

@Serializable
@Entity(
    tableName = "economic_notes",
    indices = [
        Index("occurredEpochDay"),
        Index("createdAtMillis"),
    ],
)
data class EconomicNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredEpochDay: Long,
    val operationType: String,
    val content: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)
