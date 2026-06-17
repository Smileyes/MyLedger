package com.utopia.finance.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.utopia.finance.data.local.FinanceDatabase
import com.utopia.finance.data.local.entity.AccountEntity
import com.utopia.finance.data.local.entity.CounterpartyEntity
import com.utopia.finance.data.local.entity.CreditBillEntity
import com.utopia.finance.data.local.entity.DebtEntity
import com.utopia.finance.data.local.entity.LendingEntity
import com.utopia.finance.data.local.entity.TransactionEntity
import com.utopia.finance.data.repository.DefaultTransactionRepository
import com.utopia.finance.domain.model.AccountRole
import com.utopia.finance.domain.model.AccountType
import com.utopia.finance.domain.model.CreditBillStatus
import com.utopia.finance.domain.model.CurrencyCode
import com.utopia.finance.domain.model.PendingStatus
import com.utopia.finance.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class DefaultTransactionRepositoryDeleteTest {
    private lateinit var database: FinanceDatabase
    private lateinit var repository: DefaultTransactionRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FinanceDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = DefaultTransactionRepository(database)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) database.close()
    }

    @Test
    fun deletingBilledCreditExpenseSyncsPendingRepaymentTodo() = runTest {
        val creditAccountId = database.accountDao().insert(
            AccountEntity(
                name = "花呗",
                type = AccountType.HUABEI,
                role = AccountRole.CREDIT_LIABILITY,
                billDay = 1,
                repaymentDay = 10,
            ),
        )
        val billId = database.creditBillDao().insert(
            CreditBillEntity(
                creditAccountId = creditAccountId,
                periodKey = "2026-06",
                billEpochDay = 1,
                dueEpochDay = 10,
                amountMinor = 10_000,
            ),
        )
        val expenseId = database.transactionDao().insert(
            TransactionEntity(
                accountId = creditAccountId,
                type = TransactionType.EXPENSE,
                amountMinor = 4_000,
                creditBillId = billId,
            ),
        )
        database.transactionDao().insert(
            TransactionEntity(
                accountId = null,
                type = TransactionType.DEBT_REPAYMENT,
                amountMinor = 10_000,
                status = PendingStatus.PENDING,
                creditBillId = billId,
            ),
        )

        repository.deleteTransaction(expenseId)

        val bill = database.creditBillDao().getById(billId)
        val todo = database.transactionDao().findPendingCreditBillRepayment(billId)
        assertEquals(6_000L, bill?.amountMinor)
        assertEquals(CreditBillStatus.UNPAID, bill?.status)
        assertEquals(6_000L, todo?.amountMinor)
    }

    @Test
    fun deletingConfirmedLendingRepaymentRestoresReceivableTodo() = runTest {
        val accountId = database.accountDao().insert(AccountEntity(name = "现金", type = AccountType.CASH))
        val counterpartyId = database.catalogDao().insertCounterparty(CounterpartyEntity(name = "朋友"))
        val lendingId = database.lendingDao().insert(
            LendingEntity(
                counterpartyId = counterpartyId,
                principalMinor = 10_000,
                repaidMinor = 4_000,
            ),
        )
        val repaymentId = database.transactionDao().insert(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.LENDING_REPAYMENT,
                amountMinor = 4_000,
                status = PendingStatus.CONFIRMED,
                counterpartyId = counterpartyId,
                lendingId = lendingId,
            ),
        )

        repository.deleteTransaction(repaymentId)

        val lending = database.lendingDao().getById(lendingId)
        val todo = database.transactionDao().findPendingLendingRepayment(lendingId)
        assertEquals(0L, lending?.repaidMinor)
        assertNull(database.transactionDao().getById(repaymentId))
        assertNotNull(todo)
        assertEquals(10_000L, todo?.amountMinor)
    }

    @Test
    fun deletingConfirmedDebtRepaymentRestoresDebtTodo() = runTest {
        val accountId = database.accountDao().insert(AccountEntity(name = "现金", type = AccountType.CASH))
        val counterpartyId = database.catalogDao().insertCounterparty(CounterpartyEntity(name = "银行"))
        val debtId = database.debtDao().insert(
            DebtEntity(
                counterpartyId = counterpartyId,
                principalMinor = 10_000,
                repaidMinor = 4_000,
                annualRateBps = 0,
                startedAtEpochDay = 1,
            ),
        )
        val repaymentId = database.transactionDao().insert(
            TransactionEntity(
                accountId = accountId,
                type = TransactionType.DEBT_REPAYMENT,
                amountMinor = 4_000,
                status = PendingStatus.CONFIRMED,
                counterpartyId = counterpartyId,
                debtId = debtId,
            ),
        )

        repository.deleteTransaction(repaymentId)

        val debt = database.debtDao().getById(debtId)
        val todo = database.transactionDao().findPendingDebtRepayment(debtId)
        assertEquals(0L, debt?.repaidMinor)
        assertNull(database.transactionDao().getById(repaymentId))
        assertNotNull(todo)
        assertEquals(10_000L, todo?.amountMinor)
    }
}
