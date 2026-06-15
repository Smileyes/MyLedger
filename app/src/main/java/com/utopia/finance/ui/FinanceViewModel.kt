package com.utopia.finance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utopia.finance.AppContainer
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
import com.utopia.finance.data.repository.ReportBundle
import com.utopia.finance.data.repository.ReportPeriodType
import com.utopia.finance.domain.model.AccountType
import com.utopia.finance.domain.model.AccountRole
import com.utopia.finance.domain.model.AssetSummary
import com.utopia.finance.domain.model.BillingPeriod
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionType
import com.utopia.finance.domain.model.defaultRole
import com.utopia.finance.domain.model.fractionDigits
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinanceUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val pendingTransactions: List<TransactionEntity> = emptyList(),
    val incomeSources: List<IncomeSourceEntity> = emptyList(),
    val expenseCategories: List<ExpenseCategoryEntity> = emptyList(),
    val counterparties: List<CounterpartyEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val lending: List<LendingEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val creditBills: List<CreditBillEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val economicNotes: List<EconomicNoteEntity> = emptyList(),
    val summary: AssetSummary? = null,
    val accountBalances: Map<Long, Long> = emptyMap(),
    val exportTreeUri: String? = null,
    val biometricEnabled: Boolean = true,
    val isBusy: Boolean = false,
    val message: String? = null,
)

class FinanceViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            container.catalogRepository.seedDefaults()
            container.subscriptionRepository.generateDuePending(LocalDate.now().toEpochDay())
            container.transactionRepository.generateCreditBills(LocalDate.now().toEpochDay())
            refreshSummary()
        }
        collectState()
    }

    private fun collectState() {
        viewModelScope.launch {
            container.accountRepository.observeAccounts().collect { items ->
                _uiState.update { it.copy(accounts = items) }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            container.transactionRepository.observeRecent().collect { items ->
                _uiState.update { it.copy(recentTransactions = items) }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            container.transactionRepository.observePending().collect { items ->
                _uiState.update { it.copy(pendingTransactions = items) }
            }
        }
        viewModelScope.launch {
            container.transactionRepository.observeCreditBills().collect { items ->
                _uiState.update { it.copy(creditBills = items) }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            container.catalogRepository.observeIncomeSources().collect { items ->
                _uiState.update { it.copy(incomeSources = items) }
            }
        }
        viewModelScope.launch {
            container.catalogRepository.observeExpenseCategories().collect { items ->
                _uiState.update { it.copy(expenseCategories = items) }
            }
        }
        viewModelScope.launch {
            container.catalogRepository.observeCounterparties().collect { items ->
                _uiState.update { it.copy(counterparties = items) }
            }
        }
        viewModelScope.launch {
            container.assetRepository.observeInvestments().collect { items ->
                _uiState.update { it.copy(investments = items) }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            container.assetRepository.observeLending().collect { items ->
                _uiState.update { it.copy(lending = items) }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            container.assetRepository.observeDebts().collect { items ->
                _uiState.update { it.copy(debts = items) }
                refreshSummary()
            }
        }
        viewModelScope.launch {
            container.subscriptionRepository.observeActive().collect { items ->
                _uiState.update { it.copy(subscriptions = items) }
            }
        }
        viewModelScope.launch {
            container.settingsRepository.exportTreeUri.collect { uri ->
                _uiState.update { it.copy(exportTreeUri = uri) }
            }
        }
        viewModelScope.launch {
            container.settingsRepository.biometricEnabled.collect { enabled ->
                _uiState.update { it.copy(biometricEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            container.economicNoteRepository.observeRecent().collect { items ->
                _uiState.update { it.copy(economicNotes = items) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun setExportTreeUri(uri: String?) {
        viewModelScope.launch {
            container.settingsRepository.setExportTreeUri(uri)
            _uiState.update {
                it.copy(message = if (uri == null) "已清除导出位置" else "导出位置已保存")
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setBiometricEnabled(enabled)
            _uiState.update {
                it.copy(message = if (enabled) "指纹认证已开启" else "指纹认证已关闭")
            }
        }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        parentAccountId: Long?,
        role: AccountRole,
        currency: CurrencyCode,
        openingAmount: String,
        billDay: Int?,
        repaymentDay: Int?,
    ) = launchBusy {
        val finalRole = if (type.defaultRole() == AccountRole.CREDIT_LIABILITY) AccountRole.CREDIT_LIABILITY else role
        val finalCurrency = if (type == AccountType.U_CARD) CurrencyCode.USD else currency
        container.accountRepository.saveAccount(
            AccountEntity(
                name = requireName(name),
                type = type,
                parentAccountId = parentAccountId,
                role = finalRole,
                currency = finalCurrency,
                openingBalanceMinor = parseNonNegativeMinor(openingAmount, finalCurrency),
                billDay = if (finalRole == AccountRole.CREDIT_LIABILITY) requireBillDay(billDay, "账单日") else null,
                repaymentDay = if (finalRole == AccountRole.CREDIT_LIABILITY) requireBillDay(repaymentDay, "还款日") else null,
            ),
        )
        "账户已添加"
    }

    fun correctAccountBalance(accountId: Long, currentBalance: String) = launchBusy {
        val account = _uiState.value.accounts.firstOrNull { it.id == accountId } ?: error("账户不存在")
        container.accountRepository.correctAccountBalance(accountId, parseSignedMinor(currentBalance, account.currency))
        refreshSummary()
        "账户资产已修正"
    }

    fun updateCreditRepaymentDay(account: AccountEntity, repaymentDay: String) = launchBusy {
        require(account.role == AccountRole.CREDIT_LIABILITY) { "只有花呗或信用卡可以设置还款日" }
        container.accountRepository.saveAccount(
            account.copy(
                repaymentDay = requireBillDay(repaymentDay.toIntOrNull(), "还款日"),
            ),
        )
        "还款日已更新"
    }

    fun addIncomeSource(name: String, defaultAccountId: Long?) = launchBusy {
        container.catalogRepository.addIncomeSource(requireName(name), defaultAccountId)
        "收入来源已添加"
    }

    fun updateIncomeSourceDefaultAccount(source: IncomeSourceEntity, defaultAccountId: Long?) = launchBusy {
        container.catalogRepository.updateIncomeSource(source.copy(defaultAccountId = defaultAccountId))
        "收入到账账户已更新"
    }

    fun addExpenseCategory(name: String, defaultAccountId: Long?) = launchBusy {
        container.catalogRepository.addExpenseCategory(requireName(name), defaultAccountId)
        "支出分类已添加"
    }

    fun updateExpenseCategoryDefaultAccount(category: ExpenseCategoryEntity, defaultAccountId: Long?) = launchBusy {
        container.catalogRepository.updateExpenseCategory(category.copy(defaultAccountId = defaultAccountId))
        "支出账号已更新"
    }

    fun deleteIncomeSource(id: Long) = launchBusy {
        container.catalogRepository.deleteIncomeSource(id)
        "收入来源已删除"
    }

    fun deleteExpenseCategory(id: Long) = launchBusy {
        container.catalogRepository.deleteExpenseCategory(id)
        "支出类型已删除"
    }

    fun addIncome(
        accountId: Long?,
        incomeSourceId: Long?,
        currency: CurrencyCode,
        amount: String,
        description: String,
        date: LocalDate,
    ) = launchBusy {
        require(accountId != null) { "请选择到账账户" }
        container.transactionRepository.recordIncome(
            accountId = accountId,
            incomeSourceId = incomeSourceId,
            currency = currency,
            amountMinor = parseMinor(amount, currency),
            description = description,
            occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        "收入已记录"
    }

    fun addExpense(
        paymentAccountId: Long?,
        repaymentAccountId: Long?,
        expenseCategoryId: Long?,
        currency: CurrencyCode,
        amount: String,
        description: String,
        date: LocalDate,
        repaymentDate: LocalDate,
    ) = launchBusy {
        require(paymentAccountId != null) { "请选择支付方式" }
        container.transactionRepository.recordExpense(
            paymentAccountId = paymentAccountId,
            repaymentAccountId = repaymentAccountId,
            expenseCategoryId = expenseCategoryId,
            currency = currency,
            amountMinor = parseMinor(amount, currency),
            description = description,
            occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            repaymentAtMillis = repaymentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        "支出已记录"
    }

    fun addTransfer(
        fromAccountId: Long?,
        toAccountId: Long?,
        fromAmount: String,
        toAmount: String,
        description: String,
        date: LocalDate,
    ) = launchBusy {
        require(fromAccountId != null) { "请选择转出账户" }
        require(toAccountId != null) { "请选择转入账户" }
        container.transactionRepository.recordTransfer(
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            fromAmountMinor = parseMinor(fromAmount, _uiState.value.accounts.firstOrNull { it.id == fromAccountId }?.currency ?: CurrencyCode.CNY),
            toAmountMinor = parseMinor(toAmount, _uiState.value.accounts.firstOrNull { it.id == toAccountId }?.currency ?: CurrencyCode.CNY),
            description = description,
            occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        "转账/换汇已记录"
    }

    fun addTransaction(
        accountId: Long?,
        type: TransactionType,
        currency: CurrencyCode,
        amount: String,
        description: String,
        incomeSourceId: Long?,
        expenseCategoryId: Long?,
        counterpartyId: Long?,
        date: LocalDate,
    ) = launchBusy {
        require(type == TransactionType.LENDING_OUT || type == TransactionType.DEBT_IN) { "明细类型只能选择借出或欠款" }
        accountId?.let { id ->
            val account = _uiState.value.accounts.firstOrNull { it.id == id } ?: error("账户不存在")
            require(account.currency == currency) { "明细币种必须和账户币种一致" }
        }
        container.transactionRepository.saveTransaction(
            TransactionEntity(
                accountId = accountId,
                type = type,
                amountMinor = parseMinor(amount, currency),
                currency = currency,
                description = description.trim(),
                incomeSourceId = incomeSourceId,
                expenseCategoryId = expenseCategoryId,
                counterpartyId = counterpartyId,
                occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ),
        )
        "明细已记录"
    }

    fun updateTransaction(
        transaction: TransactionEntity,
        accountId: Long?,
        currency: CurrencyCode,
        amount: String,
        description: String,
        date: LocalDate,
    ) = launchBusy {
        accountId?.let { id ->
            val account = _uiState.value.accounts.firstOrNull { it.id == id } ?: error("账户不存在")
            require(account.currency == currency) { "明细币种必须和账户币种一致" }
        }
        container.transactionRepository.updateTransaction(
            transaction.copy(
                accountId = accountId,
                currency = currency,
                amountMinor = parseMinor(amount, currency),
                description = description.trim(),
                occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ),
        )
        "明细已修改"
    }

    fun updateDetailTransaction(
        transaction: TransactionEntity,
        accountId: Long?,
        currency: CurrencyCode,
        amount: String,
        counterpartyName: String,
        annualRate: String,
        description: String,
        date: LocalDate,
    ) = launchBusy {
        val amountMinor = parseMinor(amount, currency)
        val trimmedDescription = description.trim()
        val occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        accountId?.let { id ->
            val account = _uiState.value.accounts.firstOrNull { it.id == id } ?: error("账户不存在")
            require(account.currency == currency) { "明细币种必须和账户币种一致" }
        }
        val counterpartyId = if (transaction.type == TransactionType.LENDING_OUT || transaction.type == TransactionType.DEBT_IN) {
            findOrCreateCounterparty(counterpartyName)
        } else {
            transaction.counterpartyId
        }
        container.transactionRepository.updateTransaction(
            transaction.copy(
                accountId = accountId,
                currency = currency,
                amountMinor = amountMinor,
                description = trimmedDescription,
                counterpartyId = counterpartyId,
                occurredAtMillis = occurredAtMillis,
            ),
        )
        when (transaction.type) {
            TransactionType.LENDING_OUT -> {
                val lendingId = transaction.lendingId
                val lending = _uiState.value.lending.firstOrNull { it.id == lendingId }
                if (lending != null && counterpartyId != null) {
                    val principalMinor = amountMinor.coerceAtLeast(lending.repaidMinor)
                    container.assetRepository.updateLending(
                        lending.copy(
                            counterpartyId = counterpartyId,
                            principalMinor = principalMinor,
                            currency = currency,
                            description = trimmedDescription,
                        ),
                    )
                    updateLinkedPendingTodo(
                        predicate = { it.type == TransactionType.LENDING_REPAYMENT && it.lendingId == lending.id },
                        accountId = accountId,
                        currency = currency,
                        amountMinor = (principalMinor - lending.repaidMinor).coerceAtLeast(0),
                        counterpartyId = counterpartyId,
                        description = trimmedDescription.ifBlank { "待收回借出款" },
                        occurredAtMillis = occurredAtMillis,
                    )
                }
            }
            TransactionType.DEBT_IN -> {
                val debtId = transaction.debtId
                val debt = _uiState.value.debts.firstOrNull { it.id == debtId }
                if (debt != null && counterpartyId != null) {
                    val principalMinor = amountMinor.coerceAtLeast(debt.repaidMinor)
                    container.assetRepository.updateDebt(
                        debt.copy(
                            counterpartyId = counterpartyId,
                            principalMinor = principalMinor,
                            currency = currency,
                            annualRateBps = parseAnnualRateBps(annualRate),
                            description = trimmedDescription,
                        ),
                    )
                    updateLinkedPendingTodo(
                        predicate = { it.type == TransactionType.DEBT_REPAYMENT && it.debtId == debt.id },
                        accountId = accountId,
                        currency = currency,
                        amountMinor = (principalMinor - debt.repaidMinor).coerceAtLeast(0),
                        counterpartyId = counterpartyId,
                        description = trimmedDescription.ifBlank { "待偿还欠款" },
                        occurredAtMillis = occurredAtMillis,
                    )
                }
            }
            else -> Unit
        }
        "明细已修改"
    }

    fun addInvestment(name: String, direction: String, quantity: String, currency: CurrencyCode, totalCost: String, description: String) = launchBusy {
        container.assetRepository.addInvestment(
            InvestmentEntity(
                name = requireName(name),
                direction = direction.trim(),
                quantity = quantity.trim(),
                currency = currency,
                totalCostMinor = parseMinor(totalCost, currency),
                description = description.trim(),
            ),
        )
        "投资成本已记录"
    }

    fun addLending(
        accountId: Long?,
        counterpartyName: String,
        currency: CurrencyCode,
        amount: String,
        description: String,
        date: LocalDate = LocalDate.now(),
    ) = launchBusy {
        val counterpartyId = findOrCreateCounterparty(counterpartyName)
        require(currency != CurrencyCode.BTC) { "BTC 暂只用于资产展示，不能记录借出明细" }
        accountId?.let { id ->
            val account = _uiState.value.accounts.firstOrNull { it.id == id } ?: error("账户不存在")
            require(account.currency == currency) { "明细币种必须和账户币种一致" }
        }
        val amountMinor = parseMinor(amount, currency)
        val lendingId = container.assetRepository.addLending(
            LendingEntity(
                counterpartyId = counterpartyId,
                principalMinor = amountMinor,
                currency = currency,
                description = description.trim(),
            ),
        )
        container.transactionRepository.saveTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.LENDING_OUT,
                amountMinor = amountMinor,
                currency = currency,
                counterpartyId = counterpartyId,
                lendingId = lendingId,
                description = description.trim(),
                occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ),
        )
        container.transactionRepository.saveTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.LENDING_REPAYMENT,
                amountMinor = amountMinor,
                currency = currency,
                status = PendingStatus.PENDING,
                counterpartyId = counterpartyId,
                lendingId = lendingId,
                description = description.trim().ifBlank { "待收回借出款" },
                occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ),
        )
        "借出已记录，并生成待收回事项"
    }

    fun addDebt(
        accountId: Long?,
        counterpartyName: String,
        currency: CurrencyCode,
        amount: String,
        annualRate: String,
        description: String,
        date: LocalDate = LocalDate.now(),
        repaymentDate: LocalDate = date,
    ) = launchBusy {
        val counterpartyId = findOrCreateCounterparty(counterpartyName)
        require(currency != CurrencyCode.BTC) { "BTC 暂只用于资产展示，不能记录欠款明细" }
        accountId?.let { id ->
            val account = _uiState.value.accounts.firstOrNull { it.id == id } ?: error("账户不存在")
            require(account.currency == currency) { "明细币种必须和账户币种一致" }
        }
        val amountMinor = parseMinor(amount, currency)
        val debtId = container.assetRepository.addDebt(
            DebtEntity(
                counterpartyId = counterpartyId,
                principalMinor = amountMinor,
                annualRateBps = parseAnnualRateBps(annualRate),
                startedAtEpochDay = date.toEpochDay(),
                currency = currency,
                description = description.trim(),
            ),
        )
        container.transactionRepository.saveTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.DEBT_IN,
                amountMinor = amountMinor,
                currency = currency,
                counterpartyId = counterpartyId,
                debtId = debtId,
                description = description.trim(),
                occurredAtMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ),
        )
        container.transactionRepository.saveTransaction(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.DEBT_REPAYMENT,
                amountMinor = amountMinor,
                currency = currency,
                status = PendingStatus.PENDING,
                counterpartyId = counterpartyId,
                debtId = debtId,
                description = description.trim().ifBlank { "待偿还欠款" },
                occurredAtMillis = repaymentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            ),
        )
        "欠款已记录，并生成待还款事项"
    }

    fun addSubscription(name: String, accountId: Long?, currency: CurrencyCode, amount: String, period: BillingPeriod, dueDate: LocalDate, description: String) = launchBusy {
        require(accountId != null) { "请选择支付账户" }
        container.subscriptionRepository.saveSubscription(
            SubscriptionEntity(
                name = requireName(name),
                accountId = accountId,
                amountMinor = parseMinor(amount, currency),
                currency = currency,
                period = period,
                nextDueEpochDay = dueDate.toEpochDay(),
                description = description.trim(),
            ),
        )
        "订阅已添加"
    }

    fun addEconomicNote(date: LocalDate, operationType: String, content: String) = launchBusy {
        container.economicNoteRepository.addNote(
            EconomicNoteEntity(
                occurredEpochDay = date.toEpochDay(),
                operationType = requireName(operationType),
                content = content.trim().also { require(it.isNotBlank()) { "笔记内容不能为空" } },
            ),
        )
        "笔记已记录"
    }

    fun deleteEconomicNote(id: Long) = launchBusy {
        container.economicNoteRepository.deleteNote(id)
        "笔记已删除"
    }

    fun generateDueSubscriptions() = launchBusy {
        val count = container.subscriptionRepository.generateDuePending(LocalDate.now().toEpochDay())
        "已生成 $count 条待确认订阅支出"
    }

    fun generateCreditBills() = launchBusy {
        val count = container.transactionRepository.generateCreditBills(LocalDate.now().toEpochDay())
        refreshSummary()
        "已生成 $count 条信用账单"
    }

    fun confirmPending(transactionId: Long, repaymentAccountId: Long? = null) = launchBusy {
        container.transactionRepository.confirmPending(transactionId, repaymentAccountId)
        "待办已确认"
    }

    fun skipPending(transactionId: Long) = launchBusy {
        container.transactionRepository.skipPending(transactionId)
        "待办已跳过"
    }

    suspend fun generateReport(periodType: ReportPeriodType, anchorDate: LocalDate): ReportBundle =
        container.reportRepository.generate(periodType, anchorDate)

    suspend fun exportBackup(password: CharArray): ByteArray =
        container.backupRepository.exportEncrypted(password)

    suspend fun importBackup(bytes: ByteArray, password: CharArray) {
        container.backupRepository.importEncrypted(bytes, password)
        refreshSummary()
    }

    private suspend fun refreshSummary() {
        val summary = container.accountRepository.assetSummary(LocalDate.now().toEpochDay())
        val accountBalances = container.accountRepository.accountBalances()
        _uiState.update { it.copy(summary = summary, accountBalances = accountBalances) }
    }

    private suspend fun findOrCreateCounterparty(name: String): Long {
        val normalized = requireName(name)
        return container.catalogRepository.getCounterparties().firstOrNull { it.name == normalized }?.id
            ?: container.catalogRepository.addCounterparty(normalized)
    }

    private suspend fun updateLinkedPendingTodo(
        predicate: (TransactionEntity) -> Boolean,
        accountId: Long?,
        currency: CurrencyCode,
        amountMinor: Long,
        counterpartyId: Long,
        description: String,
        occurredAtMillis: Long,
    ) {
        if (amountMinor <= 0) return
        _uiState.value.pendingTransactions
            .firstOrNull { it.status == PendingStatus.PENDING && predicate(it) }
            ?.let { todo ->
                container.transactionRepository.updateTransaction(
                    todo.copy(
                        accountId = accountId,
                        currency = currency,
                        amountMinor = amountMinor,
                        counterpartyId = counterpartyId,
                        description = description,
                        occurredAtMillis = occurredAtMillis,
                    ),
                )
            }
    }

    private fun launchBusy(block: suspend () -> String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching { block() }
                .onSuccess { message -> _uiState.update { it.copy(isBusy = false, message = message) } }
                .onFailure { error -> _uiState.update { it.copy(isBusy = false, message = error.message ?: "操作失败") } }
        }
    }
}

class FinanceViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        FinanceViewModel(container) as T
}

private fun parseMinor(value: String, currency: CurrencyCode): Long =
    BigDecimal(value.trim().ifBlank { "0" })
        .setScale(currency.fractionDigits(), RoundingMode.HALF_UP)
        .movePointRight(currency.fractionDigits())
        .longValueExact()
        .also { require(it > 0) { "金额必须大于 0" } }

private fun parseNonNegativeMinor(value: String, currency: CurrencyCode): Long =
    BigDecimal(value.trim().ifBlank { "0" })
        .setScale(currency.fractionDigits(), RoundingMode.HALF_UP)
        .movePointRight(currency.fractionDigits())
        .longValueExact()
        .also { require(it >= 0) { "金额不能为负" } }

private fun parseSignedMinor(value: String, currency: CurrencyCode): Long =
    BigDecimal(value.trim().ifBlank { "0" })
        .setScale(currency.fractionDigits(), RoundingMode.HALF_UP)
        .movePointRight(currency.fractionDigits())
        .longValueExact()

private fun parseAnnualRateBps(value: String): Int =
    BigDecimal(value.trim().ifBlank { "0" })
        .multiply(BigDecimal(100))
        .setScale(0, RoundingMode.HALF_UP)
        .intValueExact()
        .also { require(it >= 0) { "年化利率不能为负" } }

private fun requireName(name: String): String =
    name.trim().also { require(it.isNotBlank()) { "名称不能为空" } }

private fun requireBillDay(value: Int?, label: String): Int {
    val day = value ?: error("请填写$label")
    require(day in 1..28) { "$label 必须在 1 到 28 日之间" }
    return day
}
