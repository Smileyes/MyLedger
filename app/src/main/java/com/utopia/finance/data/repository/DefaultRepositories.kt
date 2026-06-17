package com.utopia.finance.data.repository

import androidx.room.withTransaction
import com.utopia.finance.data.local.FinanceDatabase
import com.utopia.finance.data.local.dao.AccountDao
import com.utopia.finance.data.local.dao.CatalogDao
import com.utopia.finance.data.local.dao.CreditBillDao
import com.utopia.finance.data.local.dao.DebtDao
import com.utopia.finance.data.local.dao.EconomicNoteDao
import com.utopia.finance.data.local.dao.InvestmentDao
import com.utopia.finance.data.local.dao.LendingDao
import com.utopia.finance.data.local.dao.SubscriptionDao
import com.utopia.finance.data.local.dao.TransactionDao
import com.utopia.finance.data.local.entity.AccountEntity
import com.utopia.finance.data.local.entity.CounterpartyEntity
import com.utopia.finance.data.local.entity.CreditBillEntity
import com.utopia.finance.data.local.entity.DebtEntity
import com.utopia.finance.data.local.entity.EconomicNoteEntity
import com.utopia.finance.data.local.entity.ExpenseCategoryEntity
import com.utopia.finance.data.local.entity.IncomeSourceEntity
import com.utopia.finance.data.local.entity.InvestmentEntity
import com.utopia.finance.data.local.entity.LendingEntity
import com.utopia.finance.data.local.entity.SubscriptionEntity
import com.utopia.finance.data.local.entity.TransactionEntity
import com.utopia.finance.domain.FinanceCalculator
import com.utopia.finance.domain.model.AccountRecord
import com.utopia.finance.domain.model.AccountRole
import com.utopia.finance.domain.model.AccountType
import com.utopia.finance.domain.model.AssetSummary
import com.utopia.finance.domain.model.CreditBillRecord
import com.utopia.finance.domain.model.CreditBillStatus
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.DebtRecord
import com.utopia.finance.domain.model.InvestmentRecord
import com.utopia.finance.domain.model.LendingRecord
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionRecord
import com.utopia.finance.domain.model.TransactionType
import com.utopia.finance.domain.model.defaultRole
import com.utopia.finance.domain.model.includeInAccountFunds
import com.utopia.finance.domain.model.includeInCreditLiability
import com.utopia.finance.domain.model.includeInInvestmentAssets
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow

class DefaultAccountRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val investmentDao: InvestmentDao,
    private val lendingDao: LendingDao,
    private val debtDao: DebtDao,
    private val creditBillDao: CreditBillDao,
) : AccountRepository {
    override fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.observeActive()

    override fun observeAllAccounts(): Flow<List<AccountEntity>> = accountDao.observeAll()

    override suspend fun getAccounts(): List<AccountEntity> = accountDao.getAll()

    override suspend fun accountBalances(): Map<Long, Long> {
        val accounts = accountDao.getAll().filter {
            !it.isArchived && (it.role.includeInAccountFunds() || it.role.includeInInvestmentAssets())
        }
        val balancesByAccount = accounts.associate { it.id to it.openingBalanceMinor }.toMutableMap()
        transactionDao.getAll()
            .asSequence()
            .filter { it.status == PendingStatus.CONFIRMED && it.accountId in balancesByAccount }
            .forEach { transaction ->
                val accountId = transaction.accountId ?: return@forEach
                balancesByAccount[accountId] =
                    (balancesByAccount[accountId] ?: 0L) +
                    FinanceCalculator.transactionDeltaMinor(transaction.type, transaction.amountMinor)
            }
        return balancesByAccount
    }

    override suspend fun correctAccountBalance(accountId: Long, currentBalanceMinor: Long) {
        val account = accountDao.getById(accountId) ?: error("账户不存在")
        require(account.role.includeInAccountFunds() || account.role.includeInInvestmentAssets()) { "该账户不能作为资产修正" }
        val confirmedDelta = transactionDao.getAll()
            .asSequence()
            .filter { it.status == PendingStatus.CONFIRMED && it.accountId == accountId }
            .sumOf { FinanceCalculator.transactionDeltaMinor(it.type, it.amountMinor) }
        accountDao.update(account.copy(openingBalanceMinor = FinanceCalculator.correctedOpeningBalance(currentBalanceMinor, confirmedDelta)))
    }

    override suspend fun saveAccount(account: AccountEntity): Long =
        if (account.id == 0L) accountDao.insert(account) else {
            accountDao.update(account)
            account.id
        }

    override suspend fun archiveAccount(id: Long) = accountDao.archive(id)

    override suspend fun assetSummary(asOfEpochDay: Long): AssetSummary =
        FinanceCalculator.summarize(
            accounts = accountDao.getAll().filterNot { it.isArchived }.map {
                AccountRecord(
                    id = it.id,
                    openingBalanceMinor = it.openingBalanceMinor,
                    currency = it.currency,
                    includeInFunds = it.role.includeInAccountFunds(),
                    includeInInvestmentAssets = it.role.includeInInvestmentAssets(),
                    includeInCreditLiability = it.role.includeInCreditLiability(),
                )
            },
            transactions = transactionDao.getAll().map {
                TransactionRecord(
                    id = it.id,
                    accountId = it.accountId,
                    type = it.type,
                    amountMinor = it.amountMinor,
                    currency = it.currency,
                    status = it.status,
                    creditBillId = it.creditBillId,
                    lendingId = it.lendingId,
                )
            },
            investments = investmentDao.getAll().map {
                InvestmentRecord(it.id, it.totalCostMinor, it.currency)
            },
            lending = lendingDao.getAll().map {
                LendingRecord(it.id, (it.principalMinor - it.repaidMinor).coerceAtLeast(0), it.currency)
            },
            debts = debtDao.getAll().map {
                DebtRecord(
                    id = it.id,
                    outstandingPrincipalMinor = (it.principalMinor - it.repaidMinor).coerceAtLeast(0),
                    annualRateBps = it.annualRateBps,
                    startedAtEpochDay = it.startedAtEpochDay,
                    currency = it.currency,
                )
            },
            creditBills = creditBillDao.getAll().map {
                CreditBillRecord(it.id, it.amountMinor, it.repaidMinor, it.currency)
            },
            asOfEpochDay = asOfEpochDay,
        )
}

class DefaultTransactionRepository(
    private val database: FinanceDatabase,
) : TransactionRepository {
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val lendingDao = database.lendingDao()
    private val debtDao = database.debtDao()
    private val creditBillDao = database.creditBillDao()

    override fun observeRecent(limit: Int): Flow<List<TransactionEntity>> = transactionDao.observeRecent(limit)

    override fun observePending(): Flow<List<TransactionEntity>> =
        transactionDao.observeByStatus(PendingStatus.PENDING)

    override fun observeCreditBills(): Flow<List<CreditBillEntity>> = creditBillDao.observeAll()

    override suspend fun getBetween(startMillis: Long, endMillis: Long): List<TransactionEntity> =
        transactionDao.getBetween(startMillis, endMillis)

    override suspend fun getAll(): List<TransactionEntity> = transactionDao.getAll()

    override suspend fun getCreditBills(): List<CreditBillEntity> = creditBillDao.getAll()

    override suspend fun saveTransaction(transaction: TransactionEntity): Long {
        require(transaction.amountMinor > 0) { "Amount must be greater than zero." }
        return transactionDao.insert(transaction)
    }

    override suspend fun recordIncome(
        accountId: Long,
        incomeSourceId: Long?,
        currency: CurrencyCode,
        amountMinor: Long,
        description: String,
        occurredAtMillis: Long,
    ): Long {
        require(amountMinor > 0) { "金额必须大于 0" }
        require(currency != CurrencyCode.BTC) { "BTC 暂只用于资产展示，不能记录收入明细" }
        val account = accountDao.getById(accountId) ?: error("到账账户不存在")
        require(account.role.includeInAccountFunds()) { "收入只能选择可用资金账户" }
        require(account.currency == currency) { "明细币种必须和账户币种一致" }
        return transactionDao.insert(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.INCOME,
                amountMinor = amountMinor,
                currency = currency,
                incomeSourceId = incomeSourceId,
                description = description.trim(),
                occurredAtMillis = occurredAtMillis,
            ),
        )
    }

    override suspend fun recordExpense(
        paymentAccountId: Long,
        repaymentAccountId: Long?,
        expenseCategoryId: Long?,
        currency: CurrencyCode,
        amountMinor: Long,
        description: String,
        occurredAtMillis: Long,
        repaymentAtMillis: Long,
    ): Long {
        require(amountMinor > 0) { "金额必须大于 0" }
        require(currency != CurrencyCode.BTC) { "BTC 暂只用于资产展示，不能记录支出明细" }
        return database.withTransaction {
            val paymentAccount = accountDao.getById(paymentAccountId) ?: error("支付方式不存在")
            require(paymentAccount.currency == currency) { "明细币种必须和账户币种一致" }
            val trimmedDescription = description.trim()
            if (!paymentAccount.role.includeInCreditLiability()) {
                require(paymentAccount.role.includeInAccountFunds()) { "支出账户必须是资产账户或信用账户" }
                return@withTransaction transactionDao.insert(
                    TransactionEntity(
                        accountId = paymentAccountId,
                        type = TransactionType.EXPENSE,
                        amountMinor = amountMinor,
                        currency = currency,
                        expenseCategoryId = expenseCategoryId,
                        description = trimmedDescription,
                        occurredAtMillis = occurredAtMillis,
                    ),
                )
            }

            require(paymentAccount.billDay != null && paymentAccount.repaymentDay != null) {
                "请先在账户设置中设置账单日和还款日"
            }
            transactionDao.insert(
                TransactionEntity(
                    accountId = paymentAccountId,
                    type = TransactionType.EXPENSE,
                    amountMinor = amountMinor,
                    currency = currency,
                    expenseCategoryId = expenseCategoryId,
                    description = trimmedDescription,
                    occurredAtMillis = occurredAtMillis,
                ),
            )
        }
    }

    override suspend fun recordTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        fromAmountMinor: Long,
        toAmountMinor: Long,
        description: String,
        occurredAtMillis: Long,
    ) {
        require(fromAccountId != toAccountId) { "转出和转入账户不能相同" }
        require(fromAmountMinor > 0 && toAmountMinor > 0) { "金额必须大于 0" }
        database.withTransaction {
            val fromAccount = accountDao.getById(fromAccountId) ?: error("转出账户不存在")
            val toAccount = accountDao.getById(toAccountId) ?: error("转入账户不存在")
            require(fromAccount.role.includeInAccountFunds() && toAccount.role.includeInAccountFunds()) { "转账/换汇只能选择资金账户" }
            require(fromAccount.currency != CurrencyCode.BTC && toAccount.currency != CurrencyCode.BTC) { "BTC 暂只用于资产展示，不能记录转账/换汇" }
            val trimmedDescription = description.trim()
            transactionDao.insert(
                TransactionEntity(
                    accountId = fromAccountId,
                    type = TransactionType.TRANSFER_OUT,
                    amountMinor = fromAmountMinor,
                    currency = fromAccount.currency,
                    description = trimmedDescription,
                    occurredAtMillis = occurredAtMillis,
                ),
            )
            transactionDao.insert(
                TransactionEntity(
                    accountId = toAccountId,
                    type = TransactionType.TRANSFER_IN,
                    amountMinor = toAmountMinor,
                    currency = toAccount.currency,
                    description = trimmedDescription,
                    occurredAtMillis = occurredAtMillis,
                ),
            )
        }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        require(transaction.amountMinor > 0) { "Amount must be greater than zero." }
        transactionDao.update(transaction.copy(updatedAtMillis = System.currentTimeMillis()))
    }

    override suspend fun setStatus(id: Long, status: PendingStatus) {
        transactionDao.updateStatus(id, status)
    }

    override suspend fun generateCreditBills(asOfEpochDay: Long): Int {
        val asOf = LocalDate.ofEpochDay(asOfEpochDay)
        return database.withTransaction {
            var generated = 0
            val creditAccounts = accountDao.getAll()
                .filter { !it.isArchived && it.role.includeInCreditLiability() }
            creditAccounts.forEach { account ->
                val billDay = account.billDay?.coerceIn(1, 28) ?: return@forEach
                val repaymentDay = account.repaymentDay?.coerceIn(1, 28) ?: return@forEach
                val billDate = asOf.withDayOfMonth(billDay.coerceAtMost(asOf.lengthOfMonth()))
                if (asOf.isBefore(billDate)) return@forEach
                val periodKey = DateTimeFormatter.ofPattern("yyyy-MM").format(billDate)
                if (creditBillDao.findPeriod(account.id, account.currency, periodKey) != null) return@forEach
                val unbilled = transactionDao.findUnbilledCreditExpenses(account.id, account.currency)
                val amountMinor = unbilled.sumOf { it.amountMinor }
                if (amountMinor <= 0) return@forEach
                val dueBase = if (repaymentDay >= billDay) billDate else billDate.plusMonths(1)
                val dueDate = dueBase.withDayOfMonth(repaymentDay.coerceAtMost(dueBase.lengthOfMonth()))
                val billId = creditBillDao.insert(
                    CreditBillEntity(
                        creditAccountId = account.id,
                        currency = account.currency,
                        periodKey = periodKey,
                        billEpochDay = billDate.toEpochDay(),
                        dueEpochDay = dueDate.toEpochDay(),
                        amountMinor = amountMinor,
                        description = "${account.name} $periodKey 账单",
                    ),
                )
                transactionDao.attachUnbilledCreditExpenses(account.id, account.currency, billId)
                transactionDao.insert(
                    TransactionEntity(
                        accountId = null,
                        type = TransactionType.DEBT_REPAYMENT,
                        amountMinor = amountMinor,
                        currency = account.currency,
                        status = PendingStatus.PENDING,
                        creditBillId = billId,
                        description = "${account.name}账单待还款",
                        occurredAtMillis = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    ),
                )
                generated += 1
            }
            generated
        }
    }

    override suspend fun confirmPending(id: Long, repaymentAccountId: Long?) {
        database.withTransaction {
            val transaction = transactionDao.getById(id) ?: error("待办不存在")
            require(transaction.status == PendingStatus.PENDING) { "只能确认待办事项" }
            when (transaction.type) {
                TransactionType.LENDING_REPAYMENT -> {
                    val lendingId = requireNotNull(transaction.lendingId) { "借出待办缺少关联记录" }
                    val lending = lendingDao.getById(lendingId) ?: error("借出记录不存在")
                    lendingDao.update(
                        lending.copy(repaidMinor = (lending.repaidMinor + transaction.amountMinor).coerceAtMost(lending.principalMinor)),
                    )
                }
                TransactionType.DEBT_REPAYMENT -> {
                    val creditBillId = transaction.creditBillId
                    if (creditBillId != null) {
                        val accountId = requireNotNull(repaymentAccountId ?: transaction.accountId) { "请选择还款账户" }
                        val repaymentAccount = accountDao.getById(accountId) ?: error("还款账户不存在")
                        require(repaymentAccount.role.includeInAccountFunds()) { "还款账户必须是资金账户" }
                        val bill = creditBillDao.getById(creditBillId) ?: error("信用账单不存在")
                        val repaidMinor = (bill.repaidMinor + transaction.amountMinor).coerceAtMost(bill.amountMinor)
                        val updatedStatus = when {
                            repaidMinor >= bill.amountMinor -> CreditBillStatus.PAID
                            repaidMinor > 0 -> CreditBillStatus.PARTIAL_PAID
                            else -> CreditBillStatus.UNPAID
                        }
                        creditBillDao.update(
                            bill.copy(
                                repaidMinor = repaidMinor,
                                status = updatedStatus,
                                updatedAtMillis = System.currentTimeMillis(),
                            ),
                        )
                        transactionDao.update(transaction.copy(accountId = accountId, updatedAtMillis = System.currentTimeMillis()))
                    } else {
                        val debtId = requireNotNull(transaction.debtId) { "欠款待办缺少关联记录" }
                        val debt = debtDao.getById(debtId) ?: error("欠款记录不存在")
                        debtDao.update(
                            debt.copy(repaidMinor = (debt.repaidMinor + transaction.amountMinor).coerceAtMost(debt.principalMinor)),
                        )
                    }
                }
                else -> Unit
            }
            transactionDao.updateStatus(id, PendingStatus.CONFIRMED)
        }
    }

    override suspend fun skipPending(id: Long) {
        transactionDao.updateStatus(id, PendingStatus.SKIPPED)
    }

    override suspend fun deleteTransaction(id: Long) {
        database.withTransaction {
            val transaction = transactionDao.getById(id) ?: return@withTransaction
            when (transaction.type) {
                TransactionType.LENDING_OUT -> {
                    val lendingId = transaction.lendingId
                    if (lendingId == null) {
                        transactionDao.delete(id)
                    } else {
                        transactionDao.deleteByLendingId(lendingId)
                        lendingDao.delete(lendingId)
                    }
                }
                TransactionType.LENDING_REPAYMENT -> {
                    if (transaction.status == PendingStatus.CONFIRMED) {
                        transaction.lendingId
                            ?.let { lendingDao.getById(it) }
                            ?.let { lending ->
                                val updated = lending.copy(
                                    repaidMinor = (lending.repaidMinor - transaction.amountMinor).coerceAtLeast(0),
                                )
                                lendingDao.update(updated)
                                syncPendingLendingRepayment(
                                    lending = updated,
                                    accountId = transaction.accountId,
                                    counterpartyId = transaction.counterpartyId,
                                    occurredAtMillis = transaction.occurredAtMillis,
                                )
                            }
                    }
                    transactionDao.delete(id)
                }
                TransactionType.DEBT_IN -> {
                    val debtId = transaction.debtId
                    if (debtId == null) {
                        transactionDao.delete(id)
                    } else {
                        transactionDao.deleteByDebtId(debtId)
                        debtDao.delete(debtId)
                    }
                }
                TransactionType.DEBT_REPAYMENT -> {
                    if (transaction.status == PendingStatus.CONFIRMED) {
                        transaction.creditBillId?.let { creditBillId ->
                            creditBillDao.getById(creditBillId)?.let { bill ->
                                val repaidMinor = (bill.repaidMinor - transaction.amountMinor).coerceAtLeast(0)
                                val updated = bill.copy(
                                    repaidMinor = repaidMinor,
                                    status = when {
                                        repaidMinor >= bill.amountMinor -> CreditBillStatus.PAID
                                        repaidMinor > 0 -> CreditBillStatus.PARTIAL_PAID
                                        else -> CreditBillStatus.UNPAID
                                    },
                                    updatedAtMillis = System.currentTimeMillis(),
                                )
                                creditBillDao.update(updated)
                                syncPendingCreditBillRepayment(updated)
                            }
                        } ?: transaction.debtId
                            ?.let { debtDao.getById(it) }
                            ?.let { debt ->
                                val updated = debt.copy(
                                    repaidMinor = (debt.repaidMinor - transaction.amountMinor).coerceAtLeast(0),
                                )
                                debtDao.update(updated)
                                syncPendingDebtRepayment(
                                    debt = updated,
                                    accountId = transaction.accountId,
                                    counterpartyId = transaction.counterpartyId,
                                    occurredAtMillis = transaction.occurredAtMillis,
                                )
                            }
                    }
                    transactionDao.delete(id)
                }
                TransactionType.EXPENSE -> {
                    transaction.creditBillId?.let { creditBillId ->
                        creditBillDao.getById(creditBillId)?.let { bill ->
                            val amountMinor = (bill.amountMinor - transaction.amountMinor).coerceAtLeast(0)
                            val repaidMinor = bill.repaidMinor.coerceAtMost(amountMinor)
                            val updated = bill.copy(
                                amountMinor = amountMinor,
                                repaidMinor = repaidMinor,
                                status = when {
                                    amountMinor <= 0L || repaidMinor >= amountMinor -> CreditBillStatus.PAID
                                    repaidMinor > 0 -> CreditBillStatus.PARTIAL_PAID
                                    else -> CreditBillStatus.UNPAID
                                },
                                updatedAtMillis = System.currentTimeMillis(),
                            )
                            creditBillDao.update(updated)
                            syncPendingCreditBillRepayment(updated)
                        }
                    }
                    transactionDao.delete(id)
                }
                TransactionType.INCOME,
                TransactionType.SUBSCRIPTION,
                TransactionType.TRANSFER_OUT,
                TransactionType.TRANSFER_IN -> transactionDao.delete(id)
            }
        }
    }

    private suspend fun syncPendingLendingRepayment(
        lending: LendingEntity,
        accountId: Long?,
        counterpartyId: Long?,
        occurredAtMillis: Long,
    ) {
        val pending = transactionDao.findPendingLendingRepayment(lending.id)
        val outstandingMinor = (lending.principalMinor - lending.repaidMinor).coerceAtLeast(0)
        if (outstandingMinor <= 0) {
            pending?.let { transactionDao.delete(it.id) }
            return
        }
        val updatedAtMillis = System.currentTimeMillis()
        val base = pending ?: TransactionEntity(
            accountId = accountId,
            type = TransactionType.LENDING_REPAYMENT,
            amountMinor = outstandingMinor,
            currency = lending.currency,
            status = PendingStatus.PENDING,
            occurredAtMillis = occurredAtMillis,
            description = lending.description.ifBlank { "待收回借出款" },
            counterpartyId = counterpartyId ?: lending.counterpartyId,
            lendingId = lending.id,
        )
        val next = base.copy(
            accountId = base.accountId ?: accountId,
            amountMinor = outstandingMinor,
            currency = lending.currency,
            status = PendingStatus.PENDING,
            description = base.description.ifBlank { lending.description.ifBlank { "待收回借出款" } },
            counterpartyId = base.counterpartyId ?: counterpartyId ?: lending.counterpartyId,
            lendingId = lending.id,
            updatedAtMillis = updatedAtMillis,
        )
        if (pending == null) transactionDao.insert(next) else transactionDao.update(next)
    }

    private suspend fun syncPendingDebtRepayment(
        debt: DebtEntity,
        accountId: Long?,
        counterpartyId: Long?,
        occurredAtMillis: Long,
    ) {
        val pending = transactionDao.findPendingDebtRepayment(debt.id)
        val outstandingMinor = (debt.principalMinor - debt.repaidMinor).coerceAtLeast(0)
        if (outstandingMinor <= 0) {
            pending?.let { transactionDao.delete(it.id) }
            return
        }
        val updatedAtMillis = System.currentTimeMillis()
        val base = pending ?: TransactionEntity(
            accountId = accountId,
            type = TransactionType.DEBT_REPAYMENT,
            amountMinor = outstandingMinor,
            currency = debt.currency,
            status = PendingStatus.PENDING,
            occurredAtMillis = occurredAtMillis,
            description = debt.description.ifBlank { "待偿还欠款" },
            counterpartyId = counterpartyId ?: debt.counterpartyId,
            debtId = debt.id,
        )
        val next = base.copy(
            accountId = base.accountId ?: accountId,
            amountMinor = outstandingMinor,
            currency = debt.currency,
            status = PendingStatus.PENDING,
            description = base.description.ifBlank { debt.description.ifBlank { "待偿还欠款" } },
            counterpartyId = base.counterpartyId ?: counterpartyId ?: debt.counterpartyId,
            debtId = debt.id,
            updatedAtMillis = updatedAtMillis,
        )
        if (pending == null) transactionDao.insert(next) else transactionDao.update(next)
    }

    private suspend fun syncPendingCreditBillRepayment(bill: CreditBillEntity) {
        val pending = transactionDao.findPendingCreditBillRepayment(bill.id)
        val outstandingMinor = (bill.amountMinor - bill.repaidMinor).coerceAtLeast(0)
        if (outstandingMinor <= 0) {
            pending?.let { transactionDao.delete(it.id) }
            return
        }
        val updatedAtMillis = System.currentTimeMillis()
        val dueMillis = LocalDate.ofEpochDay(bill.dueEpochDay)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val base = pending ?: TransactionEntity(
            accountId = null,
            type = TransactionType.DEBT_REPAYMENT,
            amountMinor = outstandingMinor,
            currency = bill.currency,
            status = PendingStatus.PENDING,
            occurredAtMillis = dueMillis,
            description = bill.description.ifBlank { "信用账单待还款" },
            creditBillId = bill.id,
        )
        val next = base.copy(
            amountMinor = outstandingMinor,
            currency = bill.currency,
            status = PendingStatus.PENDING,
            occurredAtMillis = base.occurredAtMillis.takeIf { it > 0 } ?: dueMillis,
            description = base.description.ifBlank { bill.description.ifBlank { "信用账单待还款" } },
            creditBillId = bill.id,
            updatedAtMillis = updatedAtMillis,
        )
        if (pending == null) transactionDao.insert(next) else transactionDao.update(next)
    }

}

class DefaultCatalogRepository(
    private val accountDao: AccountDao,
    private val catalogDao: CatalogDao,
) : CatalogRepository {
    override fun observeIncomeSources(): Flow<List<IncomeSourceEntity>> = catalogDao.observeIncomeSources()
    override fun observeExpenseCategories(): Flow<List<ExpenseCategoryEntity>> = catalogDao.observeExpenseCategories()
    override fun observeCounterparties(): Flow<List<CounterpartyEntity>> = catalogDao.observeCounterparties()
    override suspend fun getIncomeSources(): List<IncomeSourceEntity> = catalogDao.getIncomeSources()
    override suspend fun getExpenseCategories(): List<ExpenseCategoryEntity> = catalogDao.getExpenseCategories()
    override suspend fun getCounterparties(): List<CounterpartyEntity> = catalogDao.getCounterparties()

    override suspend fun addIncomeSource(name: String, defaultAccountId: Long?): Long =
        catalogDao.insertIncomeSource(IncomeSourceEntity(name = name.trim(), defaultAccountId = defaultAccountId))

    override suspend fun updateIncomeSource(source: IncomeSourceEntity) =
        catalogDao.updateIncomeSource(source.copy(name = source.name.trim()))

    override suspend fun addExpenseCategory(name: String, defaultAccountId: Long?): Long =
        catalogDao.insertExpenseCategory(ExpenseCategoryEntity(name = name.trim(), defaultAccountId = defaultAccountId))

    override suspend fun updateExpenseCategory(category: ExpenseCategoryEntity) =
        catalogDao.updateExpenseCategory(category.copy(name = category.name.trim()))

    override suspend fun addCounterparty(name: String): Long =
        catalogDao.insertCounterparty(CounterpartyEntity(name = name.trim()))

    override suspend fun deleteIncomeSource(id: Long) {
        catalogDao.archiveIncomeSource(id)
    }

    override suspend fun deleteExpenseCategory(id: Long) {
        catalogDao.archiveExpenseCategory(id)
    }

    override suspend fun seedDefaults() {
        val accounts = accountDao.getAll()
        seedAccountIfMissing(accounts, "现金", AccountType.CASH, 0)
        seedAccountIfMissing(accounts, "微信", AccountType.WECHAT, 2)
        seedAccountIfMissing(accounts, "U卡", AccountType.U_CARD, 3)
        seedAccountIfMissing(accounts, "花呗", AccountType.HUABEI, 4)
        seedAccountIfMissing(accounts, "信用卡", AccountType.CREDIT_CARD, 5)
        if (catalogDao.getIncomeSources().isEmpty()) {
            listOf("工资", "兼职", "奖金", "投资回款", "其他").forEach { addIncomeSource(it) }
        }
        if (catalogDao.getExpenseCategories().isEmpty()) {
            listOf("餐饮", "购物", "出行", "维修", "赠礼", "游戏充值", "会员订阅", "其他").forEach { addExpenseCategory(it) }
        }
    }

    private suspend fun seedAccountIfMissing(existing: List<AccountEntity>, name: String, type: AccountType, sortOrder: Int) {
        if (existing.none { it.name == name }) {
            accountDao.insert(
                AccountEntity(
                    name = name,
                    type = type,
                    role = type.defaultRole(),
                    currency = if (type == AccountType.U_CARD) CurrencyCode.USD else CurrencyCode.CNY,
                    billDay = if (type.defaultRole() == AccountRole.CREDIT_LIABILITY) 1 else null,
                    repaymentDay = if (type.defaultRole() == AccountRole.CREDIT_LIABILITY) 10 else null,
                    sortOrder = sortOrder,
                ),
            )
        }
    }
}

class DefaultAssetRepository(
    private val investmentDao: InvestmentDao,
    private val lendingDao: LendingDao,
    private val debtDao: DebtDao,
) : AssetRepository {
    override fun observeInvestments(): Flow<List<InvestmentEntity>> = investmentDao.observeAll()
    override fun observeLending(): Flow<List<LendingEntity>> = lendingDao.observeAll()
    override fun observeDebts(): Flow<List<DebtEntity>> = debtDao.observeAll()
    override suspend fun getInvestments(): List<InvestmentEntity> = investmentDao.getAll()
    override suspend fun getLending(): List<LendingEntity> = lendingDao.getAll()
    override suspend fun getDebts(): List<DebtEntity> = debtDao.getAll()
    override suspend fun addInvestment(investment: InvestmentEntity): Long = investmentDao.insert(investment)
    override suspend fun addLending(lending: LendingEntity): Long = lendingDao.insert(lending)
    override suspend fun addDebt(debt: DebtEntity): Long = debtDao.insert(debt)
    override suspend fun updateLending(lending: LendingEntity) = lendingDao.update(lending)
    override suspend fun updateDebt(debt: DebtEntity) = debtDao.update(debt)
}

class DefaultSubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val transactionDao: TransactionDao,
) : SubscriptionRepository {
    override fun observeActive(): Flow<List<SubscriptionEntity>> = subscriptionDao.observeActive()
    override suspend fun getAll(): List<SubscriptionEntity> = subscriptionDao.getAll()

    override suspend fun saveSubscription(subscription: SubscriptionEntity): Long =
        if (subscription.id == 0L) subscriptionDao.insert(subscription) else {
            subscriptionDao.update(subscription)
            subscription.id
        }

    override suspend fun generateDuePending(asOfEpochDay: Long): Int {
        val due = subscriptionDao.getDue(asOfEpochDay)
        var generated = 0
        due.forEach { subscription ->
            val dueDate = LocalDate.ofEpochDay(subscription.nextDueEpochDay)
            val periodKey = periodKey(dueDate, subscription.period.name)
            if (transactionDao.findSubscriptionPeriod(subscription.id, periodKey) == null) {
                transactionDao.insert(
                    TransactionEntity(
                        accountId = subscription.accountId,
                        type = TransactionType.SUBSCRIPTION,
                        amountMinor = subscription.amountMinor,
                        currency = subscription.currency,
                        status = PendingStatus.PENDING,
                        occurredAtMillis = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        description = subscription.description.ifBlank { subscription.name },
                        subscriptionId = subscription.id,
                        periodKey = periodKey,
                    ),
                )
                generated += 1
            }
            subscriptionDao.update(subscription.copy(nextDueEpochDay = nextDue(dueDate, subscription.period.name).toEpochDay()))
        }
        return generated
    }

    override suspend fun confirmPending(transactionId: Long) {
        transactionDao.updateStatus(transactionId, PendingStatus.CONFIRMED)
    }

    override suspend fun skipPending(transactionId: Long) {
        transactionDao.updateStatus(transactionId, PendingStatus.SKIPPED)
    }
}

class DefaultEconomicNoteRepository(
    private val economicNoteDao: EconomicNoteDao,
) : EconomicNoteRepository {
    override fun observeRecent(limit: Int): Flow<List<EconomicNoteEntity>> =
        economicNoteDao.observeRecent(limit)

    override suspend fun getBetween(startEpochDay: Long, endEpochDay: Long): List<EconomicNoteEntity> =
        economicNoteDao.getBetween(startEpochDay, endEpochDay)

    override suspend fun getAll(): List<EconomicNoteEntity> =
        economicNoteDao.getAll()

    override suspend fun addNote(note: EconomicNoteEntity): Long {
        require(note.operationType.isNotBlank()) { "操作类型不能为空" }
        require(note.content.isNotBlank()) { "笔记内容不能为空" }
        return economicNoteDao.insert(note)
    }

    override suspend fun deleteNote(id: Long) {
        economicNoteDao.delete(id)
    }
}

private fun periodKey(date: LocalDate, periodName: String): String =
    if (periodName == "YEARLY") {
        DateTimeFormatter.ofPattern("yyyy").format(date)
    } else {
        DateTimeFormatter.ofPattern("yyyy-MM").format(date)
    }

private fun nextDue(date: LocalDate, periodName: String): LocalDate =
    if (periodName == "YEARLY") date.plusYears(1) else date.plusMonths(1)
