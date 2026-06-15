package com.utopia.finance.domain

import com.utopia.finance.domain.model.AccountType
import com.utopia.finance.domain.model.BillingPeriod
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionType
import com.utopia.finance.domain.model.displayName
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceDisplayNamesTest {
    @Test
    fun `finance enums expose Chinese display names`() {
        assertEquals("收入", TransactionType.INCOME.displayName())
        assertEquals("会员订阅", TransactionType.SUBSCRIPTION.displayName())
        assertEquals("转出", TransactionType.TRANSFER_OUT.displayName())
        assertEquals("转入", TransactionType.TRANSFER_IN.displayName())
        assertEquals("欠款", TransactionType.DEBT_IN.displayName())
        assertEquals("借出还款", TransactionType.LENDING_REPAYMENT.displayName())
        assertEquals("RMB", CurrencyCode.CNY.displayName())
        assertEquals("USD", CurrencyCode.USD.displayName())
        assertEquals("BTC", CurrencyCode.BTC.displayName())
        assertEquals("支付账户", AccountType.E_WALLET.displayName())
        assertEquals("微信", AccountType.WECHAT.displayName())
        assertEquals("U卡", AccountType.U_CARD.displayName())
        assertEquals("花呗", AccountType.HUABEI.displayName())
        assertEquals("信用卡", AccountType.CREDIT_CARD.displayName())
        assertEquals("待确认", PendingStatus.PENDING.displayName())
        assertEquals("每月", BillingPeriod.MONTHLY.displayName())
    }
}
