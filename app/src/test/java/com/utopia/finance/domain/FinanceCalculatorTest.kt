package com.utopia.finance.domain

import com.utopia.finance.domain.model.AccountRecord
import com.utopia.finance.domain.model.CreditBillRecord
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.DebtRecord
import com.utopia.finance.domain.model.InvestmentRecord
import com.utopia.finance.domain.model.LendingRecord
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionRecord
import com.utopia.finance.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceCalculatorTest {
    @Test
    fun accountFundsUseOnlyConfirmedTransactions() {
        val accounts = listOf(AccountRecord(id = 1, openingBalanceMinor = 10_000, currency = CurrencyCode.CNY))
        val transactions = listOf(
            TransactionRecord(1, 1, TransactionType.INCOME, 5_000, CurrencyCode.CNY, PendingStatus.CONFIRMED),
            TransactionRecord(2, 1, TransactionType.EXPENSE, 1_200, CurrencyCode.CNY, PendingStatus.CONFIRMED),
            TransactionRecord(3, 1, TransactionType.SUBSCRIPTION, 600, CurrencyCode.CNY, PendingStatus.PENDING),
        )

        val funds = FinanceCalculator.accountFunds(accounts, transactions)

        assertEquals(13_800L, funds[CurrencyCode.CNY])
    }

    @Test
    fun investmentCostDoesNotChangeAccountFundsButCountsInNetWorth() {
        val accounts = listOf(AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY))
        val investments = listOf(InvestmentRecord(id = 1, totalCostMinor = 20_000, currency = CurrencyCode.CNY))

        val summary = FinanceCalculator.summarize(
            accounts = accounts,
            transactions = emptyList(),
            investments = investments,
            lending = emptyList(),
            debts = emptyList(),
            asOfEpochDay = 100,
        )

        assertEquals(100_000L, summary.accountFundsByCurrency[CurrencyCode.CNY])
        assertEquals(20_000L, summary.investmentCostByCurrency[CurrencyCode.CNY])
        assertEquals(120_000L, summary.netWorthByCurrency[CurrencyCode.CNY])
    }

    @Test
    fun investmentAccountAssetsAreSeparatedFromAccountFunds() {
        val summary = FinanceCalculator.summarize(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 30_000, currency = CurrencyCode.CNY, includeInFunds = false, includeInInvestmentAssets = true),
            ),
            transactions = emptyList(),
            investments = emptyList(),
            lending = emptyList(),
            debts = emptyList(),
            asOfEpochDay = 1,
        )

        assertEquals(100_000L, summary.accountFundsByCurrency[CurrencyCode.CNY])
        assertEquals(30_000L, summary.investmentCostByCurrency[CurrencyCode.CNY])
        assertEquals(130_000L, summary.netWorthByCurrency[CurrencyCode.CNY])
    }

    @Test
    fun lendingAndDebtUseOppositeNetWorthDirections() {
        val summary = FinanceCalculator.summarize(
            accounts = listOf(AccountRecord(id = 1, openingBalanceMinor = 0, currency = CurrencyCode.CNY)),
            transactions = emptyList(),
            investments = emptyList(),
            lending = listOf(LendingRecord(id = 1, outstandingMinor = 30_000, currency = CurrencyCode.CNY)),
            debts = listOf(DebtRecord(id = 1, outstandingPrincipalMinor = 10_000, annualRateBps = 0, startedAtEpochDay = 1, currency = CurrencyCode.CNY)),
            asOfEpochDay = 1,
        )

        assertEquals(30_000L, summary.lendingReceivableByCurrency[CurrencyCode.CNY])
        assertEquals(10_000L, summary.debtLiabilityByCurrency[CurrencyCode.CNY])
        assertEquals(20_000L, summary.netWorthByCurrency[CurrencyCode.CNY])
    }

    @Test
    fun cashPositionAddsLendingReceivableAndSubtractsLiabilities() {
        val summary = FinanceCalculator.summarize(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = -198_348, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 4_037, currency = CurrencyCode.USD),
            ),
            transactions = emptyList(),
            investments = listOf(InvestmentRecord(id = 1, totalCostMinor = 75_000, currency = CurrencyCode.USD)),
            lending = listOf(LendingRecord(id = 1, outstandingMinor = 200_000, currency = CurrencyCode.CNY)),
            debts = listOf(DebtRecord(id = 1, outstandingPrincipalMinor = 1_300, annualRateBps = 0, startedAtEpochDay = 1, currency = CurrencyCode.CNY)),
            asOfEpochDay = 1,
        )

        assertEquals(1_652L, summary.totalAssetByCurrency[CurrencyCode.CNY])
        assertEquals(79_037L, summary.totalAssetByCurrency[CurrencyCode.USD])
        assertEquals(352L, summary.cashPositionByCurrency[CurrencyCode.CNY])
        assertEquals(4_037L, summary.cashPositionByCurrency[CurrencyCode.USD])
        assertEquals(75_000L, summary.investmentCostByCurrency[CurrencyCode.USD])
        assertEquals(352L, summary.netWorthByCurrency[CurrencyCode.CNY])
        assertEquals(79_037L, summary.netWorthByCurrency[CurrencyCode.USD])
    }

    @Test
    fun debtLiabilityIncludesSimpleAnnualInterest() {
        val debt = DebtRecord(
            id = 1,
            outstandingPrincipalMinor = 100_000,
            annualRateBps = 1_200,
            startedAtEpochDay = 0,
            currency = CurrencyCode.CNY,
        )

        assertEquals(112_000L, FinanceCalculator.debtLiabilityMinor(debt, asOfEpochDay = 365))
    }

    @Test
    fun lendingOutMovesFundsToReceivableWithoutReducingNetWorth() {
        val summary = FinanceCalculator.summarize(
            accounts = listOf(AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY)),
            transactions = listOf(
                TransactionRecord(1, 1, TransactionType.LENDING_OUT, 20_000, CurrencyCode.CNY, PendingStatus.CONFIRMED, lendingId = 1),
            ),
            investments = emptyList(),
            lending = listOf(LendingRecord(id = 1, outstandingMinor = 20_000, currency = CurrencyCode.CNY)),
            debts = emptyList(),
            asOfEpochDay = 1,
        )

        assertEquals(80_000L, summary.accountFundsByCurrency[CurrencyCode.CNY])
        assertEquals(20_000L, summary.lendingReceivableByCurrency[CurrencyCode.CNY])
        assertEquals(100_000L, summary.netWorthByCurrency[CurrencyCode.CNY])
    }

    @Test
    fun legacyLendingOutWithoutLinkedReceivableDoesNotReduceNetWorth() {
        val summary = FinanceCalculator.summarize(
            accounts = listOf(AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY)),
            transactions = listOf(
                TransactionRecord(1, 1, TransactionType.LENDING_OUT, 120_000, CurrencyCode.CNY, PendingStatus.CONFIRMED),
            ),
            investments = emptyList(),
            lending = emptyList(),
            debts = emptyList(),
            asOfEpochDay = 1,
        )

        assertEquals(-20_000L, summary.accountFundsByCurrency[CurrencyCode.CNY])
        assertEquals(120_000L, summary.lendingReceivableByCurrency[CurrencyCode.CNY])
        assertEquals(100_000L, summary.netWorthByCurrency[CurrencyCode.CNY])
    }

    @Test
    fun lendingRepaymentMovesReceivableBackToAccountWithoutIncreasingNetWorth() {
        val summary = FinanceCalculator.summarize(
            accounts = listOf(AccountRecord(id = 1, openingBalanceMinor = 80_000, currency = CurrencyCode.CNY)),
            transactions = listOf(
                TransactionRecord(1, 1, TransactionType.LENDING_REPAYMENT, 20_000, CurrencyCode.CNY, PendingStatus.CONFIRMED),
            ),
            investments = emptyList(),
            lending = emptyList(),
            debts = emptyList(),
            asOfEpochDay = 1,
        )

        assertEquals(100_000L, summary.accountFundsByCurrency[CurrencyCode.CNY])
        assertEquals(100_000L, summary.netWorthByCurrency[CurrencyCode.CNY])
    }

    @Test
    fun creditPaymentAccountsAreNotCountedAsAccountFunds() {
        val funds = FinanceCalculator.accountFunds(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 0, currency = CurrencyCode.CNY, includeInFunds = false),
            ),
            transactions = listOf(
                TransactionRecord(1, 2, TransactionType.EXPENSE, 8_800, CurrencyCode.CNY, PendingStatus.CONFIRMED),
                TransactionRecord(2, 2, TransactionType.DEBT_IN, 8_800, CurrencyCode.CNY, PendingStatus.CONFIRMED),
            ),
        )

        assertEquals(100_000L, funds[CurrencyCode.CNY])
    }

    @Test
    fun transferMovesFundsBetweenAccountsWithoutChangingTotal() {
        val funds = FinanceCalculator.accountFunds(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 0, currency = CurrencyCode.CNY),
            ),
            transactions = listOf(
                TransactionRecord(1, 1, TransactionType.TRANSFER_OUT, 25_000, CurrencyCode.CNY, PendingStatus.CONFIRMED),
                TransactionRecord(2, 2, TransactionType.TRANSFER_IN, 25_000, CurrencyCode.CNY, PendingStatus.CONFIRMED),
            ),
        )

        assertEquals(100_000L, funds[CurrencyCode.CNY])
    }

    @Test
    fun exchangeKeepsAccountFundsSeparatedByCurrency() {
        val funds = FinanceCalculator.accountFunds(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 0, currency = CurrencyCode.USD),
            ),
            transactions = listOf(
                TransactionRecord(1, 1, TransactionType.TRANSFER_OUT, 70_000, CurrencyCode.CNY, PendingStatus.CONFIRMED),
                TransactionRecord(2, 2, TransactionType.TRANSFER_IN, 10_000, CurrencyCode.USD, PendingStatus.CONFIRMED),
            ),
        )

        assertEquals(30_000L, funds[CurrencyCode.CNY])
        assertEquals(10_000L, funds[CurrencyCode.USD])
    }

    @Test
    fun btcAccountFundsAreTrackedSeparatelyFromCashCurrencies() {
        val funds = FinanceCalculator.accountFunds(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 12_345_678, currency = CurrencyCode.BTC),
            ),
            transactions = emptyList(),
        )

        assertEquals(100_000L, funds[CurrencyCode.CNY])
        assertEquals(12_345_678L, funds[CurrencyCode.BTC])
    }

    @Test
    fun creditExpensesCountAsLiabilityBeforeAndAfterBillGeneration() {
        val beforeBill = FinanceCalculator.summarize(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 0, currency = CurrencyCode.CNY, includeInFunds = false, includeInCreditLiability = true),
            ),
            transactions = listOf(
                TransactionRecord(1, 2, TransactionType.EXPENSE, 8_800, CurrencyCode.CNY, PendingStatus.CONFIRMED),
                TransactionRecord(2, 2, TransactionType.EXPENSE, 1_200, CurrencyCode.CNY, PendingStatus.CONFIRMED),
            ),
            investments = emptyList(),
            lending = emptyList(),
            debts = emptyList(),
            creditBills = emptyList(),
            asOfEpochDay = 1,
        )

        val afterBill = FinanceCalculator.summarize(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 0, currency = CurrencyCode.CNY, includeInFunds = false, includeInCreditLiability = true),
            ),
            transactions = listOf(
                TransactionRecord(1, 2, TransactionType.EXPENSE, 8_800, CurrencyCode.CNY, PendingStatus.CONFIRMED, creditBillId = 1),
                TransactionRecord(2, 2, TransactionType.EXPENSE, 1_200, CurrencyCode.CNY, PendingStatus.CONFIRMED, creditBillId = 1),
            ),
            investments = emptyList(),
            lending = emptyList(),
            debts = emptyList(),
            creditBills = listOf(CreditBillRecord(id = 1, amountMinor = 10_000, repaidMinor = 0, currency = CurrencyCode.CNY)),
            asOfEpochDay = 1,
        )

        assertEquals(10_000L, beforeBill.debtLiabilityByCurrency[CurrencyCode.CNY])
        assertEquals(10_000L, afterBill.debtLiabilityByCurrency[CurrencyCode.CNY])
        assertEquals(90_000L, beforeBill.netWorthByCurrency[CurrencyCode.CNY])
        assertEquals(90_000L, afterBill.netWorthByCurrency[CurrencyCode.CNY])
    }

    @Test
    fun creditBillRepaymentReducesLiabilityAndAccountFunds() {
        val summary = FinanceCalculator.summarize(
            accounts = listOf(
                AccountRecord(id = 1, openingBalanceMinor = 100_000, currency = CurrencyCode.CNY),
                AccountRecord(id = 2, openingBalanceMinor = 0, currency = CurrencyCode.CNY, includeInFunds = false, includeInCreditLiability = true),
            ),
            transactions = listOf(
                TransactionRecord(1, 1, TransactionType.DEBT_REPAYMENT, 6_000, CurrencyCode.CNY, PendingStatus.CONFIRMED),
            ),
            investments = emptyList(),
            lending = emptyList(),
            debts = emptyList(),
            creditBills = listOf(CreditBillRecord(id = 1, amountMinor = 10_000, repaidMinor = 6_000, currency = CurrencyCode.CNY)),
            asOfEpochDay = 1,
        )

        assertEquals(94_000L, summary.accountFundsByCurrency[CurrencyCode.CNY])
        assertEquals(4_000L, summary.debtLiabilityByCurrency[CurrencyCode.CNY])
        assertEquals(90_000L, summary.netWorthByCurrency[CurrencyCode.CNY])
    }

    @Test
    fun correctedOpeningBalancePreservesHistoryAndTargetsCurrentBalance() {
        val confirmedDelta = -12_300L

        assertEquals(62_300L, FinanceCalculator.correctedOpeningBalance(50_000L, confirmedDelta))
    }
}
