package com.utopia.finance.domain.model

fun TransactionType.displayName(): String =
    when (this) {
        TransactionType.INCOME -> "收入"
        TransactionType.EXPENSE -> "支出"
        TransactionType.SUBSCRIPTION -> "会员订阅"
        TransactionType.TRANSFER_OUT -> "转出"
        TransactionType.TRANSFER_IN -> "转入"
        TransactionType.LENDING_OUT -> "借出"
        TransactionType.LENDING_REPAYMENT -> "借出还款"
        TransactionType.DEBT_IN -> "欠款"
        TransactionType.DEBT_REPAYMENT -> "欠款还款"
    }

fun CurrencyCode.displayName(): String =
    when (this) {
        CurrencyCode.CNY -> "RMB"
        CurrencyCode.USD -> "USD"
        CurrencyCode.BTC -> "BTC"
    }

fun CurrencyCode.fractionDigits(): Int =
    when (this) {
        CurrencyCode.BTC -> 8
        CurrencyCode.CNY,
        CurrencyCode.USD -> 2
    }

fun AccountType.displayName(): String =
    when (this) {
        AccountType.CASH -> "现金"
        AccountType.BANK -> "银行卡"
        AccountType.E_WALLET -> "支付宝"
        AccountType.WECHAT -> "微信"
        AccountType.U_CARD -> "U卡"
        AccountType.CREDIT -> "信用账户"
        AccountType.HUABEI -> "花呗"
        AccountType.CREDIT_CARD -> "信用卡"
        AccountType.INVESTMENT -> "投资账户"
        AccountType.OTHER -> "其他"
    }

fun AccountType.defaultRole(): AccountRole =
    when (this) {
        AccountType.CREDIT,
        AccountType.HUABEI,
        AccountType.CREDIT_CARD -> AccountRole.CREDIT_LIABILITY
        AccountType.INVESTMENT -> AccountRole.INVESTMENT_ASSET
        else -> AccountRole.AVAILABLE_CASH
    }

fun AccountType.toKind(): AccountKind =
    when (this) {
        AccountType.CASH -> AccountKind.CASH
        AccountType.BANK -> AccountKind.BANK_CARD
        AccountType.E_WALLET -> AccountKind.ALIPAY
        AccountType.WECHAT -> AccountKind.WECHAT
        AccountType.U_CARD -> AccountKind.U_CARD
        AccountType.CREDIT,
        AccountType.HUABEI -> AccountKind.HUABEI
        AccountType.CREDIT_CARD -> AccountKind.CREDIT_CARD
        AccountType.INVESTMENT -> AccountKind.INVESTMENT
        AccountType.OTHER -> AccountKind.OTHER
    }

fun AccountRole.displayName(): String =
    when (this) {
        AccountRole.AVAILABLE_CASH -> "可用现金"
        AccountRole.RESTRICTED_CASH -> "受限现金"
        AccountRole.INVESTMENT_ASSET -> "投资资产"
        AccountRole.CREDIT_LIABILITY -> "信用负债"
        AccountRole.GROUP -> "分组账户"
    }

fun AccountRole.includeInAccountFunds(): Boolean =
    this == AccountRole.AVAILABLE_CASH || this == AccountRole.RESTRICTED_CASH

fun AccountRole.includeInInvestmentAssets(): Boolean =
    this == AccountRole.INVESTMENT_ASSET

fun AccountRole.includeInCreditLiability(): Boolean =
    this == AccountRole.CREDIT_LIABILITY

fun AccountType.isCreditPayment(): Boolean =
    this == AccountType.CREDIT || this == AccountType.HUABEI || this == AccountType.CREDIT_CARD

fun AccountType.isAccountFund(): Boolean = !isCreditPayment()

fun PendingStatus.displayName(): String =
    when (this) {
        PendingStatus.PENDING -> "待确认"
        PendingStatus.CONFIRMED -> "已确认"
        PendingStatus.SKIPPED -> "已跳过"
    }

fun BillingPeriod.displayName(): String =
    when (this) {
        BillingPeriod.MONTHLY -> "每月"
        BillingPeriod.YEARLY -> "每年"
    }

fun CreditBillStatus.displayName(): String =
    when (this) {
        CreditBillStatus.UNPAID -> "未还"
        CreditBillStatus.PARTIAL_PAID -> "部分已还"
        CreditBillStatus.PAID -> "已还清"
    }
