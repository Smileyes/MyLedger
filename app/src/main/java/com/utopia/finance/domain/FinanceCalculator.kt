package com.utopia.finance.domain

import com.utopia.finance.domain.model.AccountRecord
import com.utopia.finance.domain.model.AssetSummary
import com.utopia.finance.domain.model.CreditBillRecord
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.DebtRecord
import com.utopia.finance.domain.model.InvestmentRecord
import com.utopia.finance.domain.model.LendingRecord
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionRecord
import com.utopia.finance.domain.model.TransactionType
import kotlin.math.roundToLong

object FinanceCalculator {
    fun transactionDeltaMinor(type: TransactionType, amountMinor: Long): Long {
        require(amountMinor >= 0) { "Amount cannot be negative." }
        return when (type) {
            TransactionType.INCOME,
            TransactionType.TRANSFER_IN,
            TransactionType.LENDING_REPAYMENT,
            TransactionType.DEBT_IN -> amountMinor

            TransactionType.EXPENSE,
            TransactionType.SUBSCRIPTION,
            TransactionType.TRANSFER_OUT,
            TransactionType.LENDING_OUT,
            TransactionType.DEBT_REPAYMENT -> -amountMinor
        }
    }

    fun accountFunds(
        accounts: List<AccountRecord>,
        transactions: List<TransactionRecord>,
    ): Map<CurrencyCode, Long> {
        return accountBalances(accounts.filter { it.includeInFunds }, transactions)
    }

    fun debtLiabilityMinor(debt: DebtRecord, asOfEpochDay: Long): Long {
        require(debt.annualRateBps >= 0) { "Annual rate cannot be negative." }
        val elapsedDays = (asOfEpochDay - debt.startedAtEpochDay).coerceAtLeast(0)
        val interest = debt.outstandingPrincipalMinor *
            (debt.annualRateBps / 10_000.0) *
            (elapsedDays / 365.0)
        return debt.outstandingPrincipalMinor + interest.roundToLong()
    }

    fun correctedOpeningBalance(currentBalanceMinor: Long, confirmedTransactionDeltaMinor: Long): Long =
        currentBalanceMinor - confirmedTransactionDeltaMinor

    fun summarize(
        accounts: List<AccountRecord>,
        transactions: List<TransactionRecord>,
        investments: List<InvestmentRecord>,
        lending: List<LendingRecord>,
        debts: List<DebtRecord>,
        asOfEpochDay: Long,
        creditBills: List<CreditBillRecord> = emptyList(),
    ): AssetSummary {
        val accountFunds = accountFunds(accounts, transactions)
        val investmentAccountAssets = accountBalances(accounts.filter { it.includeInInvestmentAssets }, transactions)
        val investmentCosts = mergeAmountMaps(
            investments.sumByCurrency { it.currency to it.totalCostMinor },
            investmentAccountAssets,
        )
        val lendingReceivable = mergeAmountMaps(
            lending.sumByCurrency { it.currency to it.outstandingMinor },
            legacyLendingReceivable(transactions),
        )
        val debtLiabilities = debts.sumByCurrency { debt ->
            debt.currency to debtLiabilityMinor(debt, asOfEpochDay)
        }
        val creditAccountIds = accounts
            .asSequence()
            .filter { it.includeInCreditLiability }
            .map { it.id }
            .toSet()
        val unbilledCreditLiabilities = transactions
            .asSequence()
            .filter {
                it.status == PendingStatus.CONFIRMED &&
                    it.type == TransactionType.EXPENSE &&
                    it.accountId in creditAccountIds &&
                    it.creditBillId == null
            }
            .toList()
            .sumByCurrency { it.currency to it.amountMinor }
        val billedCreditLiabilities = creditBills.sumByCurrency { it.currency to it.outstandingMinor }
        val creditLiabilities = mergeAmountMaps(unbilledCreditLiabilities, billedCreditLiabilities)
        val totalDebtLiabilities = mergeAmountMaps(debtLiabilities, creditLiabilities)
        val totalAssets = mergeAmountMaps(
            accountFunds,
            investmentCosts,
            lendingReceivable,
        )
        val cashPosition = mergeAmountMaps(
            accountFunds,
            lendingReceivable,
            totalDebtLiabilities.mapValues { -it.value },
        )

        val allCurrencies = CurrencyCode.entries.toSet()
        val netWorth = allCurrencies.associateWith { currency ->
            (accountFunds[currency] ?: 0L) +
                (investmentCosts[currency] ?: 0L) +
                (lendingReceivable[currency] ?: 0L) -
                (totalDebtLiabilities[currency] ?: 0L)
        }.filterValues { it != 0L }

        return AssetSummary(
            accountFundsByCurrency = accountFunds.filterValues { it != 0L },
            investmentCostByCurrency = investmentCosts.filterValues { it != 0L },
            lendingReceivableByCurrency = lendingReceivable.filterValues { it != 0L },
            creditBillLiabilityByCurrency = creditLiabilities.filterValues { it != 0L },
            debtLiabilityByCurrency = totalDebtLiabilities.filterValues { it != 0L },
            netWorthByCurrency = netWorth,
            totalAssetByCurrency = totalAssets,
            cashPositionByCurrency = cashPosition,
        )
    }
}

private fun <T> Iterable<T>.sumByCurrency(selector: (T) -> Pair<CurrencyCode, Long>): Map<CurrencyCode, Long> =
    groupBy({ selector(it).first }, { selector(it).second })
        .mapValues { entry -> entry.value.sum() }

private fun accountBalances(
    accounts: List<AccountRecord>,
    transactions: List<TransactionRecord>,
): Map<CurrencyCode, Long> {
    val balancesByAccount = accounts.associate { it.id to it.openingBalanceMinor }.toMutableMap()
    val currenciesByAccount = accounts.associate { it.id to it.currency }

    transactions
        .asSequence()
        .filter { it.status == PendingStatus.CONFIRMED && it.accountId != null }
        .forEach { transaction ->
            val accountId = transaction.accountId ?: return@forEach
            if (accountId !in balancesByAccount) return@forEach
            balancesByAccount[accountId] =
                (balancesByAccount[accountId] ?: 0L) +
                FinanceCalculator.transactionDeltaMinor(transaction.type, transaction.amountMinor)
        }

    return balancesByAccount.entries
        .groupBy({ currenciesByAccount[it.key] ?: CurrencyCode.CNY }, { it.value })
        .mapValues { entry -> entry.value.sum() }
}

private fun legacyLendingReceivable(transactions: List<TransactionRecord>): Map<CurrencyCode, Long> =
    transactions
        .asSequence()
        .filter {
            it.status == PendingStatus.CONFIRMED &&
                it.lendingId == null &&
                (it.type == TransactionType.LENDING_OUT || it.type == TransactionType.LENDING_REPAYMENT)
        }
        .groupBy { it.currency }
        .mapValues { entry ->
            entry.value.sumOf { transaction ->
                when (transaction.type) {
                    TransactionType.LENDING_OUT -> transaction.amountMinor
                    TransactionType.LENDING_REPAYMENT -> -transaction.amountMinor
                    else -> 0L
                }
            }.coerceAtLeast(0)
        }
        .filterValues { it != 0L }

private fun mergeAmountMaps(vararg maps: Map<CurrencyCode, Long>): Map<CurrencyCode, Long> =
    CurrencyCode.entries.associateWith { currency ->
        maps.sumOf { it[currency] ?: 0L }
    }.filterValues { it != 0L }
