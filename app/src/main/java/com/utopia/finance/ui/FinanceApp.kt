package com.utopia.finance.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.utopia.finance.FinanceApplication
import com.utopia.finance.data.local.entity.AccountEntity
import com.utopia.finance.data.local.entity.CounterpartyEntity
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
import com.utopia.finance.domain.model.AccountRole
import com.utopia.finance.domain.model.AccountType
import com.utopia.finance.domain.model.BillingPeriod
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionType
import com.utopia.finance.domain.model.defaultRole
import com.utopia.finance.domain.model.displayName
import com.utopia.finance.domain.model.fractionDigits
import com.utopia.finance.domain.model.includeInAccountFunds
import com.utopia.finance.domain.model.includeInCreditLiability
import com.utopia.finance.domain.model.includeInInvestmentAssets
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.launch

private enum class MainTab { OVERVIEW, DETAILS, ASSETS, EXPENSES, NOTES, SETTINGS }

private enum class SettingsSection { MAIN, INCOME, EXPENSE, ACCOUNT, EXPORT, SECURITY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp() {
    val app = LocalContext.current.applicationContext as FinanceApplication
    val viewModel: FinanceViewModel = viewModel(factory = FinanceViewModelFactory(app.container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(MainTab.OVERVIEW) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showInvestmentDialog by remember { mutableStateOf(false) }
    var showLendingDialog by remember { mutableStateOf(false) }
    var showDebtDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var confirmingRepayment by remember { mutableStateOf<TransactionEntity?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("个人财务") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (selectedTab) {
                MainTab.DETAILS -> FloatingActionButton(onClick = { showTransactionDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加明细")
                }
                MainTab.OVERVIEW -> FloatingActionButton(onClick = { showTransactionDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加明细")
                }
                MainTab.ASSETS -> FloatingActionButton(onClick = { showAccountDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加账户")
                }
                MainTab.EXPENSES -> FloatingActionButton(onClick = { showTransactionDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加支出")
                }
                MainTab.NOTES -> FloatingActionButton(onClick = { showNoteDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加笔记")
                }
                MainTab.SETTINGS -> Unit
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == MainTab.OVERVIEW,
                    onClick = { selectedTab = MainTab.OVERVIEW },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("总览") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.DETAILS,
                    onClick = { selectedTab = MainTab.DETAILS },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("明细") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.ASSETS,
                    onClick = { selectedTab = MainTab.ASSETS },
                    icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("资产") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.EXPENSES,
                    onClick = { selectedTab = MainTab.EXPENSES },
                    icon = { Icon(Icons.Filled.Payments, contentDescription = null) },
                    label = { Text("支出") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.NOTES,
                    onClick = { selectedTab = MainTab.NOTES },
                    icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                    label = { Text("笔记") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.SETTINGS,
                    onClick = { selectedTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("设置") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                MainTab.OVERVIEW -> OverviewScreen(
                    uiState = uiState,
                    onAddTransaction = { showTransactionDialog = true },
                    onAddInvestment = { showInvestmentDialog = true },
                )
                MainTab.DETAILS -> DetailsScreen(
                    uiState = uiState,
                    onAddTransaction = { showTransactionDialog = true },
                    onAddTransfer = { showTransferDialog = true },
                    onConfirm = { tx ->
                        if (tx.creditBillId != null) confirmingRepayment = tx else viewModel.confirmPending(tx.id)
                    },
                    onSkip = viewModel::skipPending,
                    onEdit = { editingTransaction = it },
                )
                MainTab.ASSETS -> AssetsScreen(
                    uiState = uiState,
                    onAddAccount = { showAccountDialog = true },
                    onEditAccount = { editingAccount = it },
                )
                MainTab.EXPENSES -> ExpensesScreen(
                    uiState = uiState,
                    onAddExpense = { showTransactionDialog = true },
                    onEdit = { editingTransaction = it },
                    onConfirm = { tx ->
                        if (tx.creditBillId != null) confirmingRepayment = tx else viewModel.confirmPending(tx.id)
                    },
                    onSkip = viewModel::skipPending,
                )
                MainTab.NOTES -> NotesScreen(
                    uiState = uiState,
                    onAddNote = { showNoteDialog = true },
                    onDeleteNote = viewModel::deleteEconomicNote,
                )
                MainTab.SETTINGS -> SettingsScreen(viewModel, uiState)
            }
        }
    }

    if (showAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAccountDialog = false },
            accounts = uiState.accounts,
            onSave = { name, type, parentAccountId, role, currency, opening, billDay, repaymentDay ->
                viewModel.addAccount(name, type, parentAccountId, role, currency, opening, billDay, repaymentDay)
                showAccountDialog = false
            },
        )
    }
    if (showTransactionDialog) {
        AddTransactionDialog(
            uiState = uiState,
            onDismiss = { showTransactionDialog = false },
            onSave = { type, accountId, incomeSourceId, expenseCategoryId, currency, amount, counterpartyName, annualRate, description, date, repaymentDate ->
                when (type) {
                    TransactionType.INCOME -> viewModel.addIncome(accountId, incomeSourceId, currency, amount, description, date)
                    TransactionType.EXPENSE -> viewModel.addExpense(accountId, null, expenseCategoryId, currency, amount, description, date, repaymentDate)
                    TransactionType.LENDING_OUT -> viewModel.addLending(accountId, counterpartyName, currency, amount, description, date)
                    else -> viewModel.addDebt(accountId, counterpartyName, currency, amount, annualRate, description, date, repaymentDate)
                }
                showTransactionDialog = false
            },
        )
    }
    if (showTransferDialog) {
        AddTransferDialog(
            accounts = uiState.accounts,
            onDismiss = { showTransferDialog = false },
            onSave = { fromAccountId, toAccountId, fromAmount, toAmount, description, date ->
                viewModel.addTransfer(fromAccountId, toAccountId, fromAmount, toAmount, description, date)
                showTransferDialog = false
            },
        )
    }
    confirmingRepayment?.let { transaction ->
        ConfirmRepaymentDialog(
            transaction = transaction,
            accounts = uiState.accounts,
            onDismiss = { confirmingRepayment = null },
            onConfirm = { accountId ->
                viewModel.confirmPending(transaction.id, accountId)
                confirmingRepayment = null
            },
        )
    }
    editingTransaction?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            uiState = uiState,
            onDismiss = { editingTransaction = null },
            onSave = { accountId, currency, amount, counterpartyName, annualRate, description, date ->
                viewModel.updateDetailTransaction(transaction, accountId, currency, amount, counterpartyName, annualRate, description, date)
                editingTransaction = null
            },
        )
    }
    editingAccount?.let { account ->
        CorrectAccountBalanceDialog(
            account = account,
            balanceMinor = uiState.accountBalances[account.id] ?: account.openingBalanceMinor,
            onDismiss = { editingAccount = null },
            onSave = { value ->
                viewModel.correctAccountBalance(account.id, value)
                editingAccount = null
            },
        )
    }
    if (showInvestmentDialog) {
        AddInvestmentDialog(
            onDismiss = { showInvestmentDialog = false },
            onSave = { name, direction, quantity, currency, totalCost, description ->
                viewModel.addInvestment(name, direction, quantity, currency, totalCost, description)
                showInvestmentDialog = false
            },
        )
    }
    if (showLendingDialog) {
        AddLendingDialog(
            accounts = uiState.accounts,
            onDismiss = { showLendingDialog = false },
            onSave = { accountId, name, currency, amount, description ->
                viewModel.addLending(accountId, name, currency, amount, description)
                showLendingDialog = false
            },
        )
    }
    if (showDebtDialog) {
        AddDebtDialog(
            accounts = uiState.accounts,
            onDismiss = { showDebtDialog = false },
            onSave = { accountId, name, currency, amount, rate, description ->
                viewModel.addDebt(accountId, name, currency, amount, rate, description)
                showDebtDialog = false
            },
        )
    }
    if (showNoteDialog) {
        AddEconomicNoteDialog(
            onDismiss = { showNoteDialog = false },
            onSave = { date, operationType, content ->
                viewModel.addEconomicNote(date, operationType, content)
                showNoteDialog = false
            },
        )
    }
}

@Composable
private fun OverviewScreen(
    uiState: FinanceUiState,
    onAddTransaction: () -> Unit,
    onAddInvestment: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("资产概览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        item {
            Button(onClick = onAddTransaction, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("新增明细")
            }
        }
        item {
            val summary = uiState.summary
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val totalAssets = mergeAmountMaps(
                    summary?.accountFundsByCurrency.orEmpty(),
                    summary?.investmentCostByCurrency.orEmpty(),
                    summary?.lendingReceivableByCurrency.orEmpty(),
                )
                SummaryCard("总资产", totalAssets, prominent = true)
                SummaryCard("账户", summary?.accountFundsByCurrency.orEmpty())
                SummaryCard("投资", summary?.investmentCostByCurrency.orEmpty(), onClick = onAddInvestment)
                SummaryCard("借出", summary?.lendingReceivableByCurrency.orEmpty(), onClick = onAddTransaction)
                SummaryCard("负债", summary?.debtLiabilityByCurrency.orEmpty(), onClick = onAddTransaction)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amounts: Map<CurrencyCode, Long>,
    prominent: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val modifier = if (onClick == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    }
    Card(
        colors = if (prominent) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
        modifier = modifier,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (amounts.isEmpty()) formatAmount(CurrencyCode.CNY, 0) else amounts.entries.joinToString("  ") { formatAmount(it.key, it.value) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DetailsScreen(
    uiState: FinanceUiState,
    onAddTransaction: () -> Unit,
    onAddTransfer: () -> Unit,
    onConfirm: (TransactionEntity) -> Unit,
    onSkip: (Long) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(DetailFilter.ALL) }
    val todos = uiState.pendingTransactions.filter { it.type.isFinancialTodoType() }
        .filter { selectedFilter.matches(it.type) }
    val transactions = uiState.recentTransactions.filter { selectedFilter.matches(it.type) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("最近明细", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAddTransaction) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("新增")
                    }
                    OutlinedButton(onClick = onAddTransfer) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("转账")
                    }
                }
            }
        }
        item {
            FilterChipRow(
                selected = selectedFilter,
                options = DetailFilter.entries,
                text = { it.label },
                onSelected = { selectedFilter = it },
            )
        }
        if (todos.isNotEmpty()) {
            item {
                Text("待办事项", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(todos, key = { it.id }) { tx ->
                TransactionCard(
                    tx = tx,
                    accounts = uiState.accounts,
                    sources = uiState.incomeSources,
                    categories = uiState.expenseCategories,
                    counterparties = uiState.counterparties,
                    onConfirm = onConfirm,
                    onSkip = onSkip,
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text("最近明细", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (transactions.isEmpty()) {
            item {
                SectionCard("暂无明细") {
                    Text("点击新增开始记录支出、借出或欠款。")
                }
            }
        }
        items(transactions, key = { it.id }) { tx ->
            TransactionCard(tx, uiState.accounts, uiState.incomeSources, uiState.expenseCategories, uiState.counterparties, onConfirm, onSkip, onClick = { onEdit(tx) })
        }
    }
}

@Composable
private fun TransactionCard(
    tx: TransactionEntity,
    accounts: List<AccountEntity>,
    sources: List<IncomeSourceEntity>,
    categories: List<ExpenseCategoryEntity>,
    counterparties: List<CounterpartyEntity>,
    onConfirm: (TransactionEntity) -> Unit,
    onSkip: (Long) -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val account = accounts.firstOrNull { it.id == tx.accountId }?.name.orEmpty()
    val label = tx.incomeSourceId?.let { id -> sources.firstOrNull { it.id == id }?.name }
        ?: tx.expenseCategoryId?.let { id -> categories.firstOrNull { it.id == id }?.name }
        ?: tx.counterpartyId?.let { id -> counterparties.firstOrNull { it.id == id }?.name }
        ?: tx.type.displayName()
    SectionCard(title = "${tx.type.displayName()} · $label", onClick = onClick) {
        val amount = formatTransactionAmount(tx, accounts)
        Text("${formatDate(tx.occurredAtMillis)} · $account · $amount")
        if (tx.description.isNotBlank()) Text(tx.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (tx.status == PendingStatus.PENDING) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onConfirm(tx) }) { Text(tx.confirmLabel()) }
                OutlinedButton(onClick = { onSkip(tx.id) }) { Text("跳过") }
            }
        }
    }
}

@Composable
private fun AssetsScreen(
    uiState: FinanceUiState,
    onAddAccount: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("账户资产", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Button(onClick = onAddAccount) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增账户")
                }
            }
        }
        item {
            SectionCard("账户合计") {
                val funds = uiState.summary?.accountFundsByCurrency.orEmpty()
                Text(
                    listOf(
                        formatAmount(CurrencyCode.CNY, funds[CurrencyCode.CNY] ?: 0),
                        formatAmount(CurrencyCode.USD, funds[CurrencyCode.USD] ?: 0),
                        formatAmount(CurrencyCode.BTC, funds[CurrencyCode.BTC] ?: 0),
                    ).joinToString("  "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        val assetAccounts = uiState.accounts.filterNot { it.isDefaultBankPlaceholder() }
        if (assetAccounts.isEmpty()) {
            item {
                SectionCard("暂无账户") {
                    Text("添加微信、U卡、现金或其他子账号后会显示在这里。")
                }
            }
        }
        items(assetAccounts, key = { it.id }) { account ->
            AccountRow(account, uiState.accounts, uiState.accountBalances, uiState, onEdit = { onEditAccount(account) })
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountEntity,
    accounts: List<AccountEntity>,
    balances: Map<Long, Long>,
    uiState: FinanceUiState,
    onEdit: () -> Unit,
) {
    val parentName = account.parentAccountId?.let { parentId -> accounts.firstOrNull { it.id == parentId }?.name }
    val prefix = if (parentName == null) "" else "$parentName / "
    val creditLiability = remember(account.id, uiState.creditBills, uiState.recentTransactions) {
        val billed = uiState.creditBills
            .filter { it.creditAccountId == account.id }
            .sumOf { (it.amountMinor - it.repaidMinor).coerceAtLeast(0) }
        val unbilled = uiState.recentTransactions
            .filter {
                it.accountId == account.id &&
                    it.type == TransactionType.EXPENSE &&
                    it.status == PendingStatus.CONFIRMED &&
                    it.creditBillId == null
            }
            .sumOf { it.amountMinor }
        billed + unbilled
    }
    val balanceLabel = if (account.role.includeInAccountFunds() || account.role.includeInInvestmentAssets()) {
        formatAccountAmount(account, balances[account.id] ?: account.openingBalanceMinor)
    } else if (account.role.includeInCreditLiability()) {
        "待还 ${formatAccountAmount(account, creditLiability)}"
    } else {
        account.role.displayName()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("$prefix${account.name}")
            Text("${account.type.displayName()} · ${account.role.displayName()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(balanceLabel, style = MaterialTheme.typography.bodyMedium)
            if (account.role.includeInAccountFunds() || account.role.includeInInvestmentAssets()) {
                TextButton(onClick = onEdit) {
                    Text("修正")
                }
            } else if (account.role.includeInCreditLiability()) {
                Text(
                    "账单日 ${account.billDay ?: "-"} · 还款日 ${account.repaymentDay ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExpensesScreen(
    uiState: FinanceUiState,
    onAddExpense: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onConfirm: (TransactionEntity) -> Unit,
    onSkip: (Long) -> Unit,
) {
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    val paymentAccounts = uiState.accounts.filter {
        it.currency != CurrencyCode.BTC && (it.role.includeInAccountFunds() || it.role.includeInCreditLiability())
    }
    val expenseTransactions = uiState.recentTransactions
        .filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.SUBSCRIPTION }
        .filter { selectedAccountId == null || it.accountId == selectedAccountId }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("支出记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Button(onClick = onAddExpense) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增支出")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedAccountId == null,
                    onClick = { selectedAccountId = null },
                    label = { Text("全部") },
                )
                paymentAccounts.forEach { account ->
                    FilterChip(
                        selected = selectedAccountId == account.id,
                        onClick = { selectedAccountId = account.id },
                        label = { Text(account.name) },
                    )
                }
            }
        }
        if (expenseTransactions.isEmpty()) {
            item {
                SectionCard("暂无支出") {
                    Text("可以按支付方式查看银行卡、微信、U卡、花呗或信用卡支出。")
                }
            }
        }
        items(expenseTransactions, key = { it.id }) { tx ->
            TransactionCard(tx, uiState.accounts, uiState.incomeSources, uiState.expenseCategories, uiState.counterparties, onConfirm, onSkip, onClick = { onEdit(tx) })
        }
    }
}

@Composable
private fun SubscriptionScreen(
    uiState: FinanceUiState,
    onConfirm: (TransactionEntity) -> Unit,
    onSkip: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("待确认订阅支出", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(uiState.pendingTransactions, key = { it.id }) { tx ->
            TransactionCard(tx, uiState.accounts, uiState.incomeSources, uiState.expenseCategories, uiState.counterparties, onConfirm, onSkip)
        }
        item { Text("订阅计划", style = MaterialTheme.typography.titleMedium) }
        items(uiState.subscriptions, key = { it.id }) { item ->
            SectionCard(item.name) {
                val account = uiState.accounts.firstOrNull { it.id == item.accountId }?.name.orEmpty()
                Text("$account · ${item.period.displayName()} · ${formatAmount(item.currency, item.amountMinor)}")
                Text("下次应扣：${formatLocalDate(LocalDate.ofEpochDay(item.nextDueEpochDay))}")
                if (item.description.isNotBlank()) Text(item.description)
            }
        }
    }
}

@Composable
private fun NotesScreen(
    uiState: FinanceUiState,
    onAddNote: () -> Unit,
    onDeleteNote: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("经济操作笔记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Button(onClick = onAddNote) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新增笔记")
                }
            }
        }
        if (uiState.economicNotes.isEmpty()) {
            item {
                SectionCard("暂无笔记") {
                    Text("可以记录买入、卖出、调仓、还款、订阅调整等经济操作。")
                }
            }
        }
        items(uiState.economicNotes, key = { it.id }) { note ->
            EconomicNoteCard(note, onDeleteNote)
        }
    }
}

@Composable
private fun EconomicNoteCard(note: EconomicNoteEntity, onDeleteNote: (Long) -> Unit) {
    SectionCard("${formatLocalDate(LocalDate.ofEpochDay(note.occurredEpochDay))} · ${note.operationType}") {
        Text(note.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onDeleteNote(note.id) }) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: FinanceViewModel, uiState: FinanceUiState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var pendingReport by remember { mutableStateOf<ReportPeriodType?>(null) }
    var section by remember { mutableStateOf(SettingsSection.MAIN) }
    val exportTreeUri = uiState.exportTreeUri

    val reportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val type = pendingReport
        if (uri != null && type != null) {
            scope.launch {
                runCatching {
                    val bundle = viewModel.generateReport(type, LocalDate.now())
                    writeToUri(context, uri) { output -> writeReportZip(bundle, output) }
                }.onSuccess {
                    viewModel.showMessage("报表已导出")
                }.onFailure { error ->
                    viewModel.showMessage(error.message ?: "报表导出失败")
                }
            }
        }
    }
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val bytes = viewModel.exportBackup(password.toCharArray())
                    writeToUri(context, uri) { output -> output.write(bytes) }
                }.onSuccess {
                    viewModel.showMessage("备份已导出")
                }.onFailure { error ->
                    viewModel.showMessage(error.message ?: "备份导出失败")
                }
            }
        }
    }
    val exportLocationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            if (persistTreePermission(context, uri)) {
                viewModel.setExportTreeUri(uri.toString())
            } else {
                viewModel.showMessage("无法保存该导出位置权限")
            }
        }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                viewModel.importBackup(bytes, password.toCharArray())
            }
        }
    }

    fun exportReport(type: ReportPeriodType) {
        val selectedTreeUri = exportTreeUri
        if (selectedTreeUri == null) {
            pendingReport = type
            reportLauncher.launch(if (type == ReportPeriodType.MONTH) "财务月报.zip" else "财务年报.zip")
            return
        }
        scope.launch {
            runCatching {
                val bundle = viewModel.generateReport(type, LocalDate.now())
                writeDocumentToTree(context, selectedTreeUri, "${bundle.fileBaseName}.zip", "application/zip") { output ->
                    writeReportZip(bundle, output)
                }
            }.onSuccess {
                viewModel.showMessage("报表已导出到所选位置")
            }.onFailure { error ->
                viewModel.showMessage(error.message ?: "报表导出失败")
            }
        }
    }

    fun exportBackup() {
        val selectedTreeUri = exportTreeUri
        if (selectedTreeUri == null) {
            backupExportLauncher.launch("个人财务备份.pfb")
            return
        }
        scope.launch {
            runCatching {
                val bytes = viewModel.exportBackup(password.toCharArray())
                writeDocumentToTree(context, selectedTreeUri, "个人财务备份.pfb", "application/octet-stream") { output ->
                    output.write(bytes)
                }
            }.onSuccess {
                viewModel.showMessage("备份已导出到所选位置")
            }.onFailure { error ->
                viewModel.showMessage(error.message ?: "备份导出失败")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (section) {
            SettingsSection.MAIN -> {
                Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                SettingsEntry("收入设置", "自定义收入源头和默认到账账户") { section = SettingsSection.INCOME }
                SettingsEntry("支出设置", "自定义支出渠道和默认支出账号") { section = SettingsSection.EXPENSE }
                SettingsEntry("账户设置", "查看账户角色、账单日和还款日") { section = SettingsSection.ACCOUNT }
                SettingsEntry("报表与备份", "导出给 GPT 分析，或导入导出加密备份") { section = SettingsSection.EXPORT }
                SettingsEntry("安全设置", if (uiState.biometricEnabled) "指纹认证已开启" else "指纹认证已关闭") { section = SettingsSection.SECURITY }
            }
            SettingsSection.INCOME -> IncomeSettingsScreen(
                uiState = uiState,
                onBack = { section = SettingsSection.MAIN },
                onAdd = viewModel::addIncomeSource,
                onUpdateAccount = viewModel::updateIncomeSourceDefaultAccount,
                onDelete = viewModel::deleteIncomeSource,
            )
            SettingsSection.EXPENSE -> ExpenseSettingsScreen(
                uiState = uiState,
                onBack = { section = SettingsSection.MAIN },
                onAdd = viewModel::addExpenseCategory,
                onUpdateAccount = viewModel::updateExpenseCategoryDefaultAccount,
                onDelete = viewModel::deleteExpenseCategory,
            )
            SettingsSection.ACCOUNT -> AccountSettingsScreen(
                uiState = uiState,
                onBack = { section = SettingsSection.MAIN },
                onGenerateCreditBills = viewModel::generateCreditBills,
                onUpdateRepaymentDay = viewModel::updateCreditRepaymentDay,
            )
            SettingsSection.EXPORT -> {
                SettingsBackHeader("报表与备份") { section = SettingsSection.MAIN }
                SectionCard("导出位置") {
                    Text(exportLocationLabel(exportTreeUri), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportLocationLauncher.launch(null) }) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("选择位置")
                        }
                        if (exportTreeUri != null) {
                            TextButton(onClick = {
                                releaseTreePermission(context, exportTreeUri)
                                viewModel.setExportTreeUri(null)
                            }) {
                                Text("清除")
                            }
                        }
                    }
                }
                SectionCard("给 GPT 分析") {
                    Text("导出 Zip，内含 Markdown 汇总和 CSV 明细。")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportReport(ReportPeriodType.MONTH) }) {
                            Icon(Icons.Filled.Assessment, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("月报")
                        }
                        Button(onClick = { exportReport(ReportPeriodType.YEAR) }) {
                            Icon(Icons.Filled.Assessment, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("年报")
                        }
                    }
                }
                SectionCard("加密备份") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("备份密码，至少 8 位") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportBackup() }) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("导出")
                        }
                        OutlinedButton(onClick = { backupImportLauncher.launch(arrayOf("application/octet-stream", "*/*")) }) {
                            Text("导入")
                        }
                    }
                }
            }
            SettingsSection.SECURITY -> {
                SettingsBackHeader("安全设置") { section = SettingsSection.MAIN }
                SectionCard("指纹认证") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("打开 App 时验证指纹", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (uiState.biometricEnabled) "已开启" else "已关闭",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.biometricEnabled,
                            onCheckedChange = viewModel::setBiometricEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsEntry(title: String, subtitle: String, onClick: () -> Unit) {
    SectionCard(title = title, onClick = onClick) {
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsBackHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onBack) {
            Text("返回")
        }
    }
}

@Composable
private fun IncomeSettingsScreen(
    uiState: FinanceUiState,
    onBack: () -> Unit,
    onAdd: (String, Long?) -> Unit,
    onUpdateAccount: (IncomeSourceEntity, Long?) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val accounts = uiState.accounts.filter { it.role.includeInAccountFunds() && it.currency != CurrencyCode.BTC }
    var name by remember { mutableStateOf("") }
    var account by remember(uiState.accounts) { mutableStateOf(accounts.firstOrNull()) }

    SettingsBackHeader("收入设置", onBack)
    SectionCard("新增收入源头") {
        OutlinedTextField(name, { name = it }, label = { Text("收入源头") }, modifier = Modifier.fillMaxWidth())
        EntityMenu("默认到账账户", account, accounts, { it.name }) { account = it }
        Button(
            onClick = {
                onAdd(name, account?.id)
                name = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("添加")
        }
    }
    SectionCard("已设置收入源头") {
        if (uiState.incomeSources.isEmpty()) {
            Text("暂无")
        } else {
            uiState.incomeSources.forEach { source ->
                val selectedAccount = accounts.firstOrNull { it.id == source.defaultAccountId }
                Text(source.name, style = MaterialTheme.typography.titleSmall)
                EntityMenu("到账账户", selectedAccount, accounts, { it.name }) { selected ->
                    onUpdateAccount(source, selected?.id)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onDelete(source.id) }) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseSettingsScreen(
    uiState: FinanceUiState,
    onBack: () -> Unit,
    onAdd: (String, Long?) -> Unit,
    onUpdateAccount: (ExpenseCategoryEntity, Long?) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val accounts = uiState.accounts.filter {
        it.currency != CurrencyCode.BTC && (it.role.includeInAccountFunds() || it.role.includeInCreditLiability())
    }
    var name by remember { mutableStateOf("") }
    var account by remember(uiState.accounts) { mutableStateOf(accounts.firstOrNull()) }

    SettingsBackHeader("支出设置", onBack)
    SectionCard("新增支出渠道") {
        OutlinedTextField(name, { name = it }, label = { Text("支出渠道") }, modifier = Modifier.fillMaxWidth())
        EntityMenu("默认支出账号", account, accounts, { it.name }) { account = it }
        Button(
            onClick = {
                onAdd(name, account?.id)
                name = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("添加")
        }
    }
    SectionCard("已设置支出渠道") {
        if (uiState.expenseCategories.isEmpty()) {
            Text("暂无")
        } else {
            uiState.expenseCategories.forEach { category ->
                val selectedAccount = accounts.firstOrNull { it.id == category.defaultAccountId }
                Text(category.name, style = MaterialTheme.typography.titleSmall)
                EntityMenu("支出账号", selectedAccount, accounts, { it.name }) { selected ->
                    onUpdateAccount(category, selected?.id)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onDelete(category.id) }) {
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSettingsScreen(
    uiState: FinanceUiState,
    onBack: () -> Unit,
    onGenerateCreditBills: () -> Unit,
    onUpdateRepaymentDay: (AccountEntity, String) -> Unit,
) {
    SettingsBackHeader("账户设置", onBack)
    SectionCard("信用账单") {
        Button(onClick = onGenerateCreditBills, modifier = Modifier.fillMaxWidth()) {
            Text("生成到期账单")
        }
        Spacer(Modifier.height(8.dp))
        val creditAccounts = uiState.accounts.filter { it.role.includeInCreditLiability() }
        if (creditAccounts.isEmpty()) {
            Text("暂无信用账户")
        } else {
            creditAccounts.forEach { account ->
                CreditAccountSettingsRow(
                    account = account,
                    onUpdateRepaymentDay = onUpdateRepaymentDay,
                )
            }
        }
    }
    SectionCard("账户角色") {
        uiState.accounts.forEach { account ->
            Text("${account.name} · ${account.role.displayName()}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CreditAccountSettingsRow(
    account: AccountEntity,
    onUpdateRepaymentDay: (AccountEntity, String) -> Unit,
) {
    var repaymentDay by remember(account.id, account.repaymentDay) {
        mutableStateOf((account.repaymentDay ?: 10).toString())
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(account.name, style = MaterialTheme.typography.titleSmall)
        Text(
            "账单日 ${account.billDay ?: "-"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = repaymentDay,
                onValueChange = { repaymentDay = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("还款日") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onUpdateRepaymentDay(account, repaymentDay) }) {
                Text("保存")
            }
        }
    }
}

@Composable
private fun AddAccountDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (String, AccountType, Long?, AccountRole, CurrencyCode, String, Int?, Int?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("0") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var role by remember { mutableStateOf(type.defaultRole()) }
    var parentAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var currency by remember { mutableStateOf(CurrencyCode.CNY) }
    var billDay by remember { mutableStateOf("1") }
    var repaymentDay by remember { mutableStateOf("10") }
    val parentOptions = accounts.filter { it.parentAccountId == null && !it.role.includeInCreditLiability() }
    LaunchedEffect(type) {
        role = type.defaultRole()
        if (type == AccountType.U_CARD) {
            currency = CurrencyCode.USD
        }
    }
    EditDialog(
        "添加账户",
        onDismiss,
        onConfirm = {
            onSave(
                name,
                type,
                parentAccount?.id,
                role,
                currency,
                opening,
                billDay.toIntOrNull(),
                repaymentDay.toIntOrNull(),
            )
        },
    ) {
        OutlinedTextField(name, { name = it }, label = { Text("账户名") }, modifier = Modifier.fillMaxWidth())
        EnumMenu("账户类型", type, AccountType.entries, { it.displayName() }) { type = it }
        EnumMenu("账户角色", role, AccountRole.entries, { it.displayName() }) { selected ->
            role = if (type.defaultRole() == AccountRole.CREDIT_LIABILITY) AccountRole.CREDIT_LIABILITY else selected
        }
        EntityMenu("上级账户", parentAccount, parentOptions, { it.name }) { parentAccount = it }
        EnumMenu("币种", currency, CurrencyCode.entries, { it.displayName() }) { currency = it }
        MoneyField(opening, { opening = it }, "初始余额")
        if (role.includeInCreditLiability()) {
            OutlinedTextField(
                value = billDay,
                onValueChange = { billDay = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("账单日") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = repaymentDay,
                onValueChange = { repaymentDay = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text("还款日") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CorrectAccountBalanceDialog(
    account: AccountEntity,
    balanceMinor: Long,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var balance by remember(account.id, balanceMinor) { mutableStateOf(minorAsDecimal(balanceMinor, account.currency)) }
    EditDialog("修正账户资产", onDismiss, onConfirm = { onSave(balance) }) {
        Text(account.name, style = MaterialTheme.typography.titleMedium)
        MoneyField(balance, { balance = it }, "当前余额 ${account.currency.displayName()}")
    }
}

@Composable
private fun ConfirmRepaymentDialog(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val repaymentAccounts = accounts.filter {
        it.role.includeInAccountFunds() && it.currency == transaction.currency && it.currency != CurrencyCode.BTC
    }
    var account by remember(transaction.id, repaymentAccounts) { mutableStateOf(repaymentAccounts.firstOrNull()) }
    EditDialog("确认还款", onDismiss, onConfirm = {
        account?.let { onConfirm(it.id) }
    }) {
        Text(formatAmount(transaction.currency, transaction.amountMinor), style = MaterialTheme.typography.titleMedium)
        EntityMenu("还款账户", account, repaymentAccounts, { it.name }) { account = it }
        if (transaction.description.isNotBlank()) {
            Text(transaction.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddTransactionDialog(
    uiState: FinanceUiState,
    onDismiss: () -> Unit,
    onSave: (TransactionType, Long?, Long?, Long?, CurrencyCode, String, String, String, String, LocalDate, LocalDate) -> Unit,
) {
    val assetAccounts = uiState.accounts.filter { it.role.includeInAccountFunds() && it.currency != CurrencyCode.BTC }
    val paymentAccounts = uiState.accounts.filter {
        it.currency != CurrencyCode.BTC && (it.role.includeInAccountFunds() || it.role.includeInCreditLiability())
    }
    var account by remember(uiState.accounts) { mutableStateOf(assetAccounts.firstOrNull() ?: paymentAccounts.firstOrNull()) }
    var incomeSource by remember(uiState.incomeSources) { mutableStateOf(uiState.incomeSources.firstOrNull()) }
    var expenseCategory by remember(uiState.expenseCategories) { mutableStateOf(uiState.expenseCategories.firstOrNull()) }
    var type by remember { mutableStateOf(TransactionType.INCOME) }
    var currency by remember { mutableStateOf(CurrencyCode.CNY) }
    var amount by remember { mutableStateOf("") }
    var counterpartyName by remember { mutableStateOf("") }
    var annualRate by remember { mutableStateOf("0") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var repaymentDate by remember { mutableStateOf(LocalDate.now()) }
    val accountOptions = when (type) {
        TransactionType.EXPENSE -> paymentAccounts
        else -> assetAccounts
    }
    LaunchedEffect(type, accountOptions) {
        if (account != null && account !in accountOptions) {
            account = accountOptions.firstOrNull()
        }
    }
    LaunchedEffect(account?.id) {
        account?.let { currency = it.currency }
    }
    LaunchedEffect(type, incomeSource?.id) {
        if (type == TransactionType.INCOME) {
            incomeSource?.defaultAccountId
                ?.let { id -> assetAccounts.firstOrNull { it.id == id } }
                ?.let { account = it }
        }
    }
    LaunchedEffect(type, expenseCategory?.id) {
        if (type == TransactionType.EXPENSE) {
            expenseCategory?.defaultAccountId
                ?.let { id -> paymentAccounts.firstOrNull { it.id == id } }
                ?.let { account = it }
        }
    }
    EditDialog("添加明细", onDismiss, onConfirm = {
        onSave(
            type,
            account?.id,
            incomeSource?.id,
            expenseCategory?.id,
            currency,
            amount,
            counterpartyName,
            annualRate,
            description,
            selectedDate,
            repaymentDate,
        )
    }) {
        EnumMenu("类型", type, detailEntryTypes, { it.displayName() }) { type = it }
        EntityMenu(
            when (type) {
                TransactionType.INCOME -> "到账账户"
                TransactionType.EXPENSE -> "支付方式"
                TransactionType.LENDING_OUT -> "出款账户"
                else -> "入账账户"
            },
            account,
            accountOptions,
            { it.name },
        ) { account = it }
        EnumMenu("币种", currency, recordCurrencies, { it.displayName() }) { currency = it }
        MoneyField(amount, { amount = it }, "金额")
        when (type) {
            TransactionType.INCOME -> EntityMenu("收入来源", incomeSource, uiState.incomeSources, { it.name }) { incomeSource = it }
            TransactionType.EXPENSE -> EntityMenu("支出分类", expenseCategory, uiState.expenseCategories, { it.name }) { expenseCategory = it }
            else -> {
                OutlinedTextField(
                    value = counterpartyName,
                    onValueChange = { counterpartyName = it },
                    label = { Text(if (type == TransactionType.LENDING_OUT) "借款人" else "欠款主体") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (type == TransactionType.DEBT_IN) {
            OutlinedTextField(
                value = annualRate,
                onValueChange = { annualRate = it },
                label = { Text("年化利率 %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            DatePickerField("还款时间", repaymentDate) { repaymentDate = it }
        }
        DatePickerField("日期", selectedDate) { selectedDate = it }
        OutlinedTextField(description, { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    }
}

@Composable
private fun AddTransferDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (Long?, Long?, String, String, String, LocalDate) -> Unit,
) {
    val assetAccounts = accounts.filter { it.role.includeInAccountFunds() && it.currency != CurrencyCode.BTC }
    var fromAccount by remember(accounts) { mutableStateOf(assetAccounts.firstOrNull()) }
    var toAccount by remember(accounts) { mutableStateOf(assetAccounts.drop(1).firstOrNull() ?: assetAccounts.firstOrNull()) }
    var fromAmount by remember { mutableStateOf("") }
    var toAmount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    EditDialog("转账/换汇", onDismiss, onConfirm = {
        onSave(fromAccount?.id, toAccount?.id, fromAmount, toAmount, description, selectedDate)
    }) {
        EntityMenu("转出账户", fromAccount, assetAccounts, { it.name }) { fromAccount = it }
        MoneyField(fromAmount, { fromAmount = it }, "转出金额 ${fromAccount?.currency?.displayName().orEmpty()}")
        EntityMenu("转入账户", toAccount, assetAccounts, { it.name }) { toAccount = it }
        MoneyField(toAmount, { toAmount = it }, "转入金额 ${toAccount?.currency?.displayName().orEmpty()}")
        DatePickerField("日期", selectedDate) { selectedDate = it }
        OutlinedTextField(description, { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    }
}

@Composable
private fun EditTransactionDialog(
    transaction: TransactionEntity,
    uiState: FinanceUiState,
    onDismiss: () -> Unit,
    onSave: (Long?, CurrencyCode, String, String, String, String, LocalDate) -> Unit,
) {
    var account by remember(transaction.id) { mutableStateOf(uiState.accounts.firstOrNull { it.id == transaction.accountId }) }
    var currency by remember(transaction.id) { mutableStateOf(transaction.currency) }
    var amount by remember(transaction.id) { mutableStateOf(minorAsDecimal(transaction.amountMinor, transaction.currency)) }
    var counterpartyName by remember(transaction.id) {
        mutableStateOf(uiState.counterparties.firstOrNull { it.id == transaction.counterpartyId }?.name.orEmpty())
    }
    val debt = remember(transaction.id, uiState.debts) {
        transaction.debtId?.let { id -> uiState.debts.firstOrNull { it.id == id } }
    }
    var annualRate by remember(transaction.id, debt?.annualRateBps) {
        mutableStateOf(debt?.let { minorAsDecimal(it.annualRateBps.toLong()) } ?: "0.00")
    }
    var description by remember(transaction.id) { mutableStateOf(transaction.description) }
    var selectedDate by remember(transaction.id) {
        mutableStateOf(Instant.ofEpochMilli(transaction.occurredAtMillis).atZone(ZoneId.systemDefault()).toLocalDate())
    }
    EditDialog("修改明细", onDismiss, onConfirm = {
        onSave(account?.id, currency, amount, counterpartyName, annualRate, description, selectedDate)
    }) {
        Text("类型：${transaction.type.displayName()}", style = MaterialTheme.typography.bodyMedium)
        EntityMenu("账户", account, uiState.accounts, { it.name }) { account = it }
        EnumMenu("币种", currency, CurrencyCode.entries, { it.displayName() }) { currency = it }
        MoneyField(amount, { amount = it }, "金额")
        if (transaction.type == TransactionType.LENDING_OUT || transaction.type == TransactionType.DEBT_IN) {
            OutlinedTextField(
                value = counterpartyName,
                onValueChange = { counterpartyName = it },
                label = { Text(if (transaction.type == TransactionType.LENDING_OUT) "借出主体" else "欠款主体") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (transaction.type == TransactionType.DEBT_IN) {
            OutlinedTextField(
                value = annualRate,
                onValueChange = { annualRate = it },
                label = { Text("年化利率 %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DatePickerField("日期", selectedDate) { selectedDate = it }
        OutlinedTextField(description, { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    }
}

@Composable
private fun AddInvestmentDialog(onDismiss: () -> Unit, onSave: (String, String, String, CurrencyCode, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CurrencyCode.CNY) }
    var cost by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    EditDialog("添加投资成本", onDismiss, onConfirm = { onSave(name, direction, quantity, currency, cost, description) }) {
        OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(direction, { direction = it }, label = { Text("投资方向") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(quantity, { quantity = it }, label = { Text("数量") }, modifier = Modifier.fillMaxWidth())
        EnumMenu("币种", currency, CurrencyCode.entries, { it.displayName() }) { currency = it }
        MoneyField(cost, { cost = it }, "总成本")
        OutlinedTextField(description, { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddLendingDialog(accounts: List<AccountEntity>, onDismiss: () -> Unit, onSave: (Long?, String, CurrencyCode, String, String) -> Unit) {
    val assetAccounts = accounts.filter { it.role.includeInAccountFunds() && it.currency != CurrencyCode.BTC }
    var account by remember { mutableStateOf(assetAccounts.firstOrNull()) }
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CurrencyCode.CNY) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    EditDialog("添加借出", onDismiss, onConfirm = { onSave(account?.id, name, currency, amount, description) }) {
        EntityMenu("出款账户", account, assetAccounts, { it.name }) { account = it }
        OutlinedTextField(name, { name = it }, label = { Text("借出主体") }, modifier = Modifier.fillMaxWidth())
        EnumMenu("币种", currency, CurrencyCode.entries, { it.displayName() }) { currency = it }
        MoneyField(amount, { amount = it }, "金额")
        OutlinedTextField(description, { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddDebtDialog(accounts: List<AccountEntity>, onDismiss: () -> Unit, onSave: (Long?, String, CurrencyCode, String, String, String) -> Unit) {
    val assetAccounts = accounts.filter { it.role.includeInAccountFunds() && it.currency != CurrencyCode.BTC }
    var account by remember { mutableStateOf(assetAccounts.firstOrNull()) }
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CurrencyCode.CNY) }
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("0") }
    var description by remember { mutableStateOf("") }
    EditDialog("添加欠款", onDismiss, onConfirm = { onSave(account?.id, name, currency, amount, rate, description) }) {
        EntityMenu("入账账户", account, assetAccounts, { it.name }) { account = it }
        OutlinedTextField(name, { name = it }, label = { Text("欠款主体") }, modifier = Modifier.fillMaxWidth())
        EnumMenu("币种", currency, CurrencyCode.entries, { it.displayName() }) { currency = it }
        MoneyField(amount, { amount = it }, "本金")
        OutlinedTextField(rate, { rate = it }, label = { Text("年化利率 %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddSubscriptionDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Long?, CurrencyCode, String, BillingPeriod, LocalDate, String) -> Unit,
) {
    var account by remember { mutableStateOf(accounts.firstOrNull()) }
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CurrencyCode.CNY) }
    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(BillingPeriod.MONTHLY) }
    var selectedDueDate by remember { mutableStateOf(LocalDate.now()) }
    var description by remember { mutableStateOf("") }
    EditDialog("添加订阅", onDismiss, onConfirm = { onSave(name, account?.id, currency, amount, period, selectedDueDate, description) }) {
        EntityMenu("支付账户", account, accounts, { it.name }) { account = it }
        OutlinedTextField(name, { name = it }, label = { Text("订阅名称") }, modifier = Modifier.fillMaxWidth())
        EnumMenu("币种", currency, CurrencyCode.entries, { it.displayName() }) { currency = it }
        MoneyField(amount, { amount = it }, "金额")
        EnumMenu("周期", period, BillingPeriod.entries, { it.displayName() }) { period = it }
        DatePickerField("下次应扣", selectedDueDate) { selectedDueDate = it }
        OutlinedTextField(description, { description = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddEconomicNoteDialog(onDismiss: () -> Unit, onSave: (LocalDate, String, String) -> Unit) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var operationType by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    EditDialog("添加经济操作笔记", onDismiss, onConfirm = { onSave(selectedDate, operationType, content) }) {
        DatePickerField("日期", selectedDate) { selectedDate = it }
        OutlinedTextField(
            value = operationType,
            onValueChange = { operationType = it },
            label = { Text("经济操作") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("记录") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
    }
}

@Composable
private fun EditDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(label: String, date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(formatLocalDate(date))
        }
    }
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onDateSelected(millis.toLocalDateFromUtcMillis())
                        }
                        showPicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun <T> EnumMenu(label: String, selected: T, options: List<T>, text: (T) -> String, onSelected: (T) -> Unit) {
    MenuField(label, selected, options, text, onSelected)
}

@Composable
private fun <T> EntityMenu(label: String, selected: T?, options: List<T>, text: (T) -> String, onSelected: (T?) -> Unit) {
    MenuField(label, selected, listOf<T?>(null) + options, { it?.let(text) ?: "无" }, onSelected)
}

@Composable
private fun <T> MenuField(label: String, selected: T, options: List<T>, text: (T) -> String, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(text(selected))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> FilterChipRow(selected: T, options: List<T>, text: (T) -> String, onSelected: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(text(option)) },
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val modifier = if (onClick == null) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    }
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

private fun writeReportZip(bundle: ReportBundle, output: java.io.OutputStream) {
    ZipOutputStream(output).use { zip ->
        zip.putNextEntry(ZipEntry("${bundle.fileBaseName}.md"))
        zip.write(bundle.markdown.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.putNextEntry(ZipEntry("${bundle.fileBaseName}.csv"))
        zip.write(bundle.csv.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.putNextEntry(ZipEntry("${bundle.fileBaseName}-notes.csv"))
        zip.write(bundle.notesCsv.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.putNextEntry(ZipEntry("${bundle.fileBaseName}-credit-bills.csv"))
        zip.write(bundle.creditBillsCsv.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}

private fun writeToUri(context: Context, uri: Uri, writer: (OutputStream) -> Unit) {
    val output = context.contentResolver.openOutputStream(uri) ?: error("无法打开导出文件")
    output.use(writer)
}

private fun writeDocumentToTree(
    context: Context,
    treeUriString: String,
    fileName: String,
    mimeType: String,
    writer: (OutputStream) -> Unit,
) {
    val treeUri = Uri.parse(treeUriString)
    val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
    val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
    val documentUri = DocumentsContract.createDocument(context.contentResolver, parentUri, mimeType, fileName)
        ?: error("无法在所选位置创建文件")
    writeToUri(context, documentUri, writer)
}

private fun persistTreePermission(context: Context, uri: Uri): Boolean =
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }.isSuccess

private fun releaseTreePermission(context: Context, uriString: String) {
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            Uri.parse(uriString),
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }
}

private fun exportLocationLabel(uriString: String?): String {
    if (uriString == null) {
        return "未固定位置，导出时可在系统保存窗口选择"
    }
    val documentId = runCatching {
        DocumentsContract.getTreeDocumentId(Uri.parse(uriString))
    }.getOrNull()
    val path = documentId
        ?.substringAfter(':', missingDelimiterValue = documentId)
        ?.ifBlank { "内部存储" }
        ?: "已选择位置"
    return "已选择：$path"
}

private fun minorAsDecimal(value: Long): String {
    return minorAsDecimal(value, CurrencyCode.CNY)
}

private fun minorAsDecimal(value: Long, currency: CurrencyCode): String {
    val sign = if (value < 0) "-" else ""
    val absolute = kotlin.math.abs(value)
    val scale = currency.fractionDigits()
    val divisor = (1..scale).fold(1L) { acc, _ -> acc * 10L }
    return "$sign${absolute / divisor}.${(absolute % divisor).toString().padStart(scale, '0')}"
}

private fun formatAmount(currency: CurrencyCode, minor: Long): String =
    "${minorAsDecimal(minor, currency)} ${currency.displayName()}"

private fun formatAccountAmount(account: AccountEntity, minor: Long): String =
    formatAmount(account.currency, minor)

private fun formatTransactionAmount(transaction: TransactionEntity, accounts: List<AccountEntity>): String {
    val account = transaction.accountId?.let { id -> accounts.firstOrNull { it.id == id } }
    val currency = if (account?.type == AccountType.U_CARD) CurrencyCode.USD else transaction.currency
    return formatAmount(currency, transaction.amountMinor)
}

private fun AccountEntity.isDefaultBankPlaceholder(): Boolean =
    name == "银行卡" && type == AccountType.BANK && parentAccountId == null

private val detailEntryTypes = listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.LENDING_OUT, TransactionType.DEBT_IN)
private val recordCurrencies = listOf(CurrencyCode.CNY, CurrencyCode.USD)

private enum class DetailFilter(val label: String) {
    ALL("全部"),
    INCOME("收入"),
    EXPENSE("支出"),
    LENDING("借款"),
    DEBT("欠款");

    fun matches(type: TransactionType): Boolean =
        when (this) {
            ALL -> true
            INCOME -> type == TransactionType.INCOME
            EXPENSE -> type == TransactionType.EXPENSE || type == TransactionType.SUBSCRIPTION
            LENDING -> type == TransactionType.LENDING_OUT || type == TransactionType.LENDING_REPAYMENT
            DEBT -> type == TransactionType.DEBT_IN || type == TransactionType.DEBT_REPAYMENT
        }
}

private fun mergeAmountMaps(vararg maps: Map<CurrencyCode, Long>): Map<CurrencyCode, Long> =
    CurrencyCode.entries.associateWith { currency ->
        maps.sumOf { it[currency] ?: 0L }
    }.filterValues { it != 0L }

private fun TransactionType.isFinancialTodoType(): Boolean =
    this == TransactionType.LENDING_REPAYMENT || this == TransactionType.DEBT_REPAYMENT

private fun TransactionEntity.confirmLabel(): String =
    when (type) {
        TransactionType.LENDING_REPAYMENT -> "确认收回"
        TransactionType.DEBT_REPAYMENT -> "确认还款"
        else -> "确认扣款"
    }

private fun formatLocalDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))

private fun formatDate(millis: Long): String =
    formatLocalDate(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateFromUtcMillis(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
