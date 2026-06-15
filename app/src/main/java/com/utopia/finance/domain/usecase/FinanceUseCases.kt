package com.utopia.finance.domain.usecase

import com.utopia.finance.domain.model.CurrencyCode
import java.time.LocalDate

data class AddIncomeInput(
    val accountId: Long,
    val incomeSourceId: Long?,
    val currency: CurrencyCode,
    val amountMinor: Long,
    val description: String,
    val date: LocalDate,
)

data class AddExpenseInput(
    val accountId: Long,
    val expenseCategoryId: Long?,
    val currency: CurrencyCode,
    val amountMinor: Long,
    val description: String,
    val date: LocalDate,
)

data class AddLendingInput(
    val accountId: Long?,
    val counterpartyName: String,
    val currency: CurrencyCode,
    val amountMinor: Long,
    val description: String,
    val date: LocalDate,
)

data class AddDebtInput(
    val accountId: Long?,
    val counterpartyName: String,
    val currency: CurrencyCode,
    val amountMinor: Long,
    val annualRateBps: Int,
    val description: String,
    val date: LocalDate,
    val repaymentDate: LocalDate,
)

data class TransferOrExchangeInput(
    val fromAccountId: Long,
    val toAccountId: Long,
    val fromAmountMinor: Long,
    val toAmountMinor: Long,
    val description: String,
    val date: LocalDate,
)

interface AddIncomeUseCase {
    suspend operator fun invoke(input: AddIncomeInput): Long
}

interface AddExpenseUseCase {
    suspend operator fun invoke(input: AddExpenseInput): Long
}

interface AddLendingUseCase {
    suspend operator fun invoke(input: AddLendingInput): Long
}

interface AddDebtUseCase {
    suspend operator fun invoke(input: AddDebtInput): Long
}

interface GenerateCreditBillUseCase {
    suspend operator fun invoke(asOf: LocalDate): Int
}

interface ConfirmTodoUseCase {
    suspend operator fun invoke(todoTransactionId: Long, repaymentAccountId: Long? = null)
}

interface TransferOrExchangeUseCase {
    suspend operator fun invoke(input: TransferOrExchangeInput)
}

interface CorrectAccountBalanceUseCase {
    suspend operator fun invoke(accountId: Long, currentBalanceMinor: Long)
}

interface GenerateFinanceReportUseCase<T> {
    suspend operator fun invoke(periodAnchor: LocalDate): T
}
