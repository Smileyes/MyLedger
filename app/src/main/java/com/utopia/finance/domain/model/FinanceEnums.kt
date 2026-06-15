package com.utopia.finance.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE,
    SUBSCRIPTION,
    TRANSFER_OUT,
    TRANSFER_IN,
    LENDING_OUT,
    LENDING_REPAYMENT,
    DEBT_IN,
    DEBT_REPAYMENT,
}

enum class FinanceRecordType {
    INCOME,
    EXPENSE,
    LENDING,
    DEBT,
}

enum class CurrencyCode {
    CNY,
    USD,
    BTC,
}

enum class AccountKind {
    CASH,
    ALIPAY,
    WECHAT,
    BANK_CARD,
    U_CARD,
    HUABEI,
    CREDIT_CARD,
    INVESTMENT,
    OTHER,
}

enum class AccountType {
    CASH,
    BANK,
    E_WALLET,
    WECHAT,
    U_CARD,
    CREDIT,
    HUABEI,
    CREDIT_CARD,
    INVESTMENT,
    OTHER,
}

enum class AccountRole {
    AVAILABLE_CASH,
    RESTRICTED_CASH,
    INVESTMENT_ASSET,
    CREDIT_LIABILITY,
    GROUP,
}

enum class PendingStatus {
    PENDING,
    CONFIRMED,
    SKIPPED,
}

enum class CreditBillStatus {
    UNPAID,
    PARTIAL_PAID,
    PAID,
}

enum class TodoType {
    LENDING_RECEIVABLE,
    DEBT_REPAYMENT,
    CREDIT_BILL_REPAYMENT,
    SUBSCRIPTION_CONFIRMATION,
}

enum class BillingPeriod {
    MONTHLY,
    YEARLY,
}
