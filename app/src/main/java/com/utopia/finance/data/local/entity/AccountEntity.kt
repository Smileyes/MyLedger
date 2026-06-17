package com.utopia.finance.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.utopia.finance.domain.model.AccountRole
import com.utopia.finance.domain.model.AccountType
import com.utopia.finance.domain.model.CurrencyCode
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "accounts", indices = [androidx.room.Index("parentAccountId")])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val parentAccountId: Long? = null,
    @ColumnInfo(defaultValue = "'AVAILABLE_CASH'")
    val role: AccountRole = AccountRole.AVAILABLE_CASH,
    val currency: CurrencyCode = CurrencyCode.CNY,
    val openingBalanceMinor: Long = 0,
    val investmentQuantity: String = "",
    val billDay: Int? = null,
    val repaymentDay: Int? = null,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
