package com.utopia.finance.data.local

import androidx.room.TypeConverter
import com.utopia.finance.domain.model.AccountRole
import com.utopia.finance.domain.model.AccountType
import com.utopia.finance.domain.model.BillingPeriod
import com.utopia.finance.domain.model.CreditBillStatus
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionType

class Converters {
    @TypeConverter fun toCurrencyCode(value: String): CurrencyCode = CurrencyCode.valueOf(value)
    @TypeConverter fun fromCurrencyCode(value: CurrencyCode): String = value.name

    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter fun toAccountType(value: String): AccountType = AccountType.valueOf(value)
    @TypeConverter fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter fun toAccountRole(value: String): AccountRole = AccountRole.valueOf(value)
    @TypeConverter fun fromAccountRole(value: AccountRole): String = value.name

    @TypeConverter fun toPendingStatus(value: String): PendingStatus = PendingStatus.valueOf(value)
    @TypeConverter fun fromPendingStatus(value: PendingStatus): String = value.name

    @TypeConverter fun toCreditBillStatus(value: String): CreditBillStatus = CreditBillStatus.valueOf(value)
    @TypeConverter fun fromCreditBillStatus(value: CreditBillStatus): String = value.name

    @TypeConverter fun toBillingPeriod(value: String): BillingPeriod = BillingPeriod.valueOf(value)
    @TypeConverter fun fromBillingPeriod(value: BillingPeriod): String = value.name
}
