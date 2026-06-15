package com.utopia.finance.data.repository

import com.utopia.finance.data.local.entity.EconomicNoteEntity
import com.utopia.finance.data.local.entity.TransactionEntity
import com.utopia.finance.data.local.entity.CreditBillEntity
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.TransactionType
import com.utopia.finance.domain.model.displayName
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ReportPeriodType {
    MONTH,
    YEAR,
}

data class ReportBundle(
    val fileBaseName: String,
    val markdown: String,
    val csv: String,
    val notesCsv: String,
    val creditBillsCsv: String,
)

interface ReportRepository {
    suspend fun generate(periodType: ReportPeriodType, anchorDate: LocalDate): ReportBundle
}

class DefaultReportRepository(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val catalogRepository: CatalogRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val economicNoteRepository: EconomicNoteRepository,
) : ReportRepository {
    override suspend fun generate(periodType: ReportPeriodType, anchorDate: LocalDate): ReportBundle =
        withContext(Dispatchers.Default) {
            val zone = ZoneId.systemDefault()
            val range = when (periodType) {
                ReportPeriodType.MONTH -> {
                    val month = YearMonth.from(anchorDate)
                    month.atDay(1) to month.atEndOfMonth()
                }
                ReportPeriodType.YEAR -> {
                    val year = Year.from(anchorDate)
                    year.atDay(1) to year.atMonth(12).atEndOfMonth()
                }
            }
            val startMillis = range.first.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = range.second.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val transactions = transactionRepository.getBetween(startMillis, endMillis)
            val creditBills = transactionRepository.getCreditBills()
                .filter { it.billEpochDay in range.first.toEpochDay()..range.second.toEpochDay() }
            val notes = economicNoteRepository.getBetween(range.first.toEpochDay(), range.second.toEpochDay())
            val accounts = accountRepository.getAccounts().associateBy { it.id }
            val sources = catalogRepository.getIncomeSources().associateBy { it.id }
            val categories = catalogRepository.getExpenseCategories().associateBy { it.id }
            val counterparties = catalogRepository.getCounterparties().associateBy { it.id }
            val cnyConverted = transactions.associate { it.id to exchangeRateRepository.convertToCny(it.amountMinor, it.currency) }
            val totalsByType = transactions.groupBy { it.type }.mapValues { entry ->
                entry.value.sumOf { cnyConverted[it.id] ?: 0L }
            }
            val title = when (periodType) {
                ReportPeriodType.MONTH -> "${YearMonth.from(anchorDate)} 财务月报"
                ReportPeriodType.YEAR -> "${Year.from(anchorDate)} 财务年报"
            }
            val csv = buildCsv(transactions, accounts, sources, categories, counterparties, cnyConverted)
            val notesCsv = buildNotesCsv(notes)
            val creditBillsCsv = buildCreditBillsCsv(creditBills, accounts)
            val markdown = buildMarkdown(title, range.first, range.second, transactions, notes, creditBills, totalsByType)
            val baseName = when (periodType) {
                ReportPeriodType.MONTH -> "财务月报-${YearMonth.from(anchorDate)}"
                ReportPeriodType.YEAR -> "财务年报-${Year.from(anchorDate)}"
            }
            ReportBundle(baseName, markdown, csv, notesCsv, creditBillsCsv)
        }

    private fun buildMarkdown(
        title: String,
        start: LocalDate,
        end: LocalDate,
        transactions: List<TransactionEntity>,
        notes: List<EconomicNoteEntity>,
        creditBills: List<CreditBillEntity>,
        totalsByType: Map<TransactionType, Long>,
    ): String = buildString {
        appendLine("# $title")
        appendLine()
        appendLine("- 周期：${formatDate(start)} 至 ${formatDate(end)}")
        appendLine("- 明细数量：${transactions.size}")
        appendLine("- 信用账单数量：${creditBills.size}")
        appendLine("- 经济操作笔记数量：${notes.size}")
        appendLine("- 说明：金额按原币种保存；USD 金额在汇总中按自动汇率折算为 RMB。")
        appendLine()
        appendLine("## 按类型汇总（折算 RMB）")
        appendLine()
        appendLine("| 类型 | 金额 |")
        appendLine("| --- | ---: |")
        TransactionType.entries.forEach { type ->
            appendLine("| ${type.displayName()} | ${formatMinor(totalsByType[type] ?: 0L, CurrencyCode.CNY)} |")
        }
        appendLine()
        appendLine("## 经济操作笔记")
        appendLine()
        if (notes.isEmpty()) {
            appendLine("本周期暂无经济操作笔记。")
        } else {
            appendLine("| 日期 | 操作 | 记录 |")
            appendLine("| --- | --- | --- |")
            notes.forEach { note ->
                appendLine("| ${formatDate(LocalDate.ofEpochDay(note.occurredEpochDay))} | ${escapeMarkdownCell(note.operationType)} | ${escapeMarkdownCell(note.content)} |")
            }
        }
        appendLine()
        appendLine("## 信用账单历史")
        appendLine()
        if (creditBills.isEmpty()) {
            appendLine("本周期暂无信用账单。")
        } else {
            appendLine("| 账单日 | 还款日 | 金额 | 已还 | 状态 |")
            appendLine("| --- | --- | ---: | ---: | --- |")
            creditBills.forEach { bill ->
                appendLine(
                    "| ${formatDate(LocalDate.ofEpochDay(bill.billEpochDay))} | ${formatDate(LocalDate.ofEpochDay(bill.dueEpochDay))} | ${formatMinor(bill.amountMinor, bill.currency)} | ${formatMinor(bill.repaidMinor, bill.currency)} | ${bill.status.displayName()} |",
                )
            }
        }
        appendLine()
        appendLine("## 给 GPT 的分析提示")
        appendLine()
        appendLine("请结合 Markdown 汇总、同包 CSV 明细和经济操作笔记，分析我的收入结构、支出高频类别、订阅扣款风险、借出/欠款压力、投资习惯，以及下个周期可以优化的三件事。")
    }

    private fun buildCsv(
        transactions: List<TransactionEntity>,
        accounts: Map<Long, com.utopia.finance.data.local.entity.AccountEntity>,
        sources: Map<Long, com.utopia.finance.data.local.entity.IncomeSourceEntity>,
        categories: Map<Long, com.utopia.finance.data.local.entity.ExpenseCategoryEntity>,
        counterparties: Map<Long, com.utopia.finance.data.local.entity.CounterpartyEntity>,
        cnyConverted: Map<Long, Long>,
    ): String = buildString {
        appendLine("日期,类型,状态,账户,分类或来源,主体,币种,原币种金额,折算RMB金额,描述")
        transactions.forEach { tx ->
            val date = Instant.ofEpochMilli(tx.occurredAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val label = tx.incomeSourceId?.let { sources[it]?.name }
                ?: tx.expenseCategoryId?.let { categories[it]?.name }
                ?: ""
            val counterparty = tx.counterpartyId?.let { counterparties[it]?.name } ?: ""
            appendLine(
                listOf(
                    formatDate(date),
                    tx.type.displayName(),
                    tx.status.displayName(),
                    accounts[tx.accountId]?.name.orEmpty(),
                    label,
                    counterparty,
                    tx.currency.displayName(),
                    minorAsDecimal(tx.amountMinor),
                    minorAsDecimal(cnyConverted[tx.id] ?: tx.amountMinor),
                    tx.description,
                ).joinToString(",") { csvEscape(it) },
            )
        }
    }

    private fun buildNotesCsv(notes: List<EconomicNoteEntity>): String = buildString {
        appendLine("日期,操作,记录")
        notes.forEach { note ->
            appendLine(
                listOf(
                    formatDate(LocalDate.ofEpochDay(note.occurredEpochDay)),
                    note.operationType,
                    note.content,
                ).joinToString(",") { csvEscape(it) },
            )
        }
    }

    private fun buildCreditBillsCsv(
        creditBills: List<CreditBillEntity>,
        accounts: Map<Long, com.utopia.finance.data.local.entity.AccountEntity>,
    ): String = buildString {
        appendLine("账单日,还款日,账户,币种,账单金额,已还金额,剩余金额,状态,描述")
        creditBills.forEach { bill ->
            appendLine(
                listOf(
                    formatDate(LocalDate.ofEpochDay(bill.billEpochDay)),
                    formatDate(LocalDate.ofEpochDay(bill.dueEpochDay)),
                    accounts[bill.creditAccountId]?.name.orEmpty(),
                    bill.currency.displayName(),
                    minorAsDecimal(bill.amountMinor),
                    minorAsDecimal(bill.repaidMinor),
                    minorAsDecimal((bill.amountMinor - bill.repaidMinor).coerceAtLeast(0)),
                    bill.status.displayName(),
                    bill.description,
                ).joinToString(",") { csvEscape(it) },
            )
        }
    }
}

private fun formatMinor(value: Long, currency: CurrencyCode): String =
    "${minorAsDecimal(value)} ${currency.displayName()}"

private fun minorAsDecimal(value: Long): String {
    val sign = if (value < 0) "-" else ""
    val absolute = kotlin.math.abs(value)
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

private fun csvEscape(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"$escaped\""
    } else {
        escaped
    }
}

private fun escapeMarkdownCell(value: String): String =
    value.replace("|", "\\|").replace("\n", "<br>")

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
