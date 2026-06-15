package com.utopia.finance.domain.model

data class AccountRecord(
    val id: Long,
    val openingBalanceMinor: Long,
    val currency: CurrencyCode,
    val includeInFunds: Boolean = true,
    val includeInInvestmentAssets: Boolean = false,
    val includeInCreditLiability: Boolean = false,
)

data class TransactionRecord(
    val id: Long,
    val accountId: Long?,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: CurrencyCode,
    val status: PendingStatus,
    val creditBillId: Long? = null,
)

data class InvestmentRecord(
    val id: Long,
    val totalCostMinor: Long,
    val currency: CurrencyCode,
)

data class LendingRecord(
    val id: Long,
    val outstandingMinor: Long,
    val currency: CurrencyCode,
)

data class DebtRecord(
    val id: Long,
    val outstandingPrincipalMinor: Long,
    val annualRateBps: Int,
    val startedAtEpochDay: Long,
    val currency: CurrencyCode,
)

data class CreditBillRecord(
    val id: Long,
    val amountMinor: Long,
    val repaidMinor: Long,
    val currency: CurrencyCode,
) {
    val outstandingMinor: Long
        get() = (amountMinor - repaidMinor).coerceAtLeast(0)
}

data class AssetSummary(
    val accountFundsByCurrency: Map<CurrencyCode, Long>,
    val investmentCostByCurrency: Map<CurrencyCode, Long>,
    val lendingReceivableByCurrency: Map<CurrencyCode, Long>,
    val debtLiabilityByCurrency: Map<CurrencyCode, Long>,
    val netWorthByCurrency: Map<CurrencyCode, Long>,
    val creditBillLiabilityByCurrency: Map<CurrencyCode, Long> = emptyMap(),
)
