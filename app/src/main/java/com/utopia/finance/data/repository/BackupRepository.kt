package com.utopia.finance.data.repository

import androidx.room.withTransaction
import com.utopia.finance.data.local.FinanceDatabase
import com.utopia.finance.data.local.entity.AccountEntity
import com.utopia.finance.data.local.entity.CounterpartyEntity
import com.utopia.finance.data.local.entity.CreditBillEntity
import com.utopia.finance.data.local.entity.DebtEntity
import com.utopia.finance.data.local.entity.EconomicNoteEntity
import com.utopia.finance.data.local.entity.ExchangeRateEntity
import com.utopia.finance.data.local.entity.ExpenseCategoryEntity
import com.utopia.finance.data.local.entity.IncomeSourceEntity
import com.utopia.finance.data.local.entity.InvestmentEntity
import com.utopia.finance.data.local.entity.LendingEntity
import com.utopia.finance.data.local.entity.SubscriptionEntity
import com.utopia.finance.data.local.entity.TransactionEntity
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface BackupRepository {
    suspend fun exportEncrypted(password: CharArray): ByteArray
    suspend fun importEncrypted(bytes: ByteArray, password: CharArray)
}

class EncryptedBackupRepository(
    private val database: FinanceDatabase,
) : BackupRepository {
    private val backupDao = database.backupDao()
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportEncrypted(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        require(password.size >= 8) { "Backup password must be at least 8 characters." }
        val backup = FinanceBackup(
            exportedAtMillis = System.currentTimeMillis(),
            accounts = backupDao.accounts(),
            incomeSources = backupDao.incomeSources(),
            expenseCategories = backupDao.expenseCategories(),
            counterparties = backupDao.counterparties(),
            transactions = backupDao.transactions(),
            investments = backupDao.investments(),
            lending = backupDao.lending(),
            debts = backupDao.debts(),
            creditBills = backupDao.creditBills(),
            subscriptions = backupDao.subscriptions(),
            exchangeRates = backupDao.exchangeRates(),
            economicNotes = backupDao.economicNotes(),
        )
        encrypt(json.encodeToString(FinanceBackup.serializer(), backup).toByteArray(Charsets.UTF_8), password)
    }

    override suspend fun importEncrypted(bytes: ByteArray, password: CharArray) = withContext(Dispatchers.IO) {
        require(password.size >= 8) { "Backup password must be at least 8 characters." }
        val decoded = decrypt(bytes, password)
        val backup = json.decodeFromString(FinanceBackup.serializer(), decoded.toString(Charsets.UTF_8))
        require(backup.schemaVersion == 1) { "Unsupported backup schema: ${backup.schemaVersion}" }
        database.withTransaction {
            backupDao.clearTransactions()
            backupDao.clearSubscriptions()
            backupDao.clearLending()
            backupDao.clearDebts()
            backupDao.clearCreditBills()
            backupDao.clearInvestments()
            backupDao.clearAccounts()
            backupDao.clearIncomeSources()
            backupDao.clearExpenseCategories()
            backupDao.clearCounterparties()
            backupDao.clearExchangeRates()
            backupDao.clearEconomicNotes()

            backupDao.insertAccounts(backup.accounts)
            backupDao.insertIncomeSources(backup.incomeSources)
            backupDao.insertExpenseCategories(backup.expenseCategories)
            backupDao.insertCounterparties(backup.counterparties)
            backupDao.insertInvestments(backup.investments)
            backupDao.insertLending(backup.lending)
            backupDao.insertDebts(backup.debts)
            backupDao.insertCreditBills(backup.creditBills)
            backupDao.insertSubscriptions(backup.subscriptions)
            backupDao.insertExchangeRates(backup.exchangeRates)
            backupDao.insertEconomicNotes(backup.economicNotes)
            backupDao.insertTransactions(backup.transactions)
        }
    }
}

@Serializable
data class FinanceBackup(
    val schemaVersion: Int = 1,
    val exportedAtMillis: Long,
    val accounts: List<AccountEntity>,
    val incomeSources: List<IncomeSourceEntity>,
    val expenseCategories: List<ExpenseCategoryEntity>,
    val counterparties: List<CounterpartyEntity>,
    val transactions: List<TransactionEntity>,
    val investments: List<InvestmentEntity>,
    val lending: List<LendingEntity>,
    val debts: List<DebtEntity>,
    val subscriptions: List<SubscriptionEntity>,
    val exchangeRates: List<ExchangeRateEntity>,
    val economicNotes: List<EconomicNoteEntity> = emptyList(),
    val creditBills: List<CreditBillEntity> = emptyList(),
)

private fun encrypt(plainBytes: ByteArray, password: CharArray): ByteArray {
    val random = SecureRandom()
    val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
    val iv = ByteArray(IV_BYTES).also(random::nextBytes)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
    val cipherText = cipher.doFinal(plainBytes)
    return MAGIC + salt + iv + cipherText
}

private fun decrypt(bytes: ByteArray, password: CharArray): ByteArray {
    require(bytes.size > MAGIC.size + SALT_BYTES + IV_BYTES) { "Invalid backup file." }
    require(bytes.take(MAGIC.size).toByteArray().contentEquals(MAGIC)) { "Invalid backup file." }
    val saltStart = MAGIC.size
    val ivStart = saltStart + SALT_BYTES
    val cipherStart = ivStart + IV_BYTES
    val salt = bytes.copyOfRange(saltStart, ivStart)
    val iv = bytes.copyOfRange(ivStart, cipherStart)
    val cipherText = bytes.copyOfRange(cipherStart, bytes.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
    return cipher.doFinal(cipherText)
}

private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
    val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    return SecretKeySpec(bytes, "AES")
}

private val MAGIC = byteArrayOf('P'.code.toByte(), 'F'.code.toByte(), 'B'.code.toByte(), '1'.code.toByte())
private const val SALT_BYTES = 16
private const val IV_BYTES = 12
private const val GCM_TAG_BITS = 128
private const val KEY_BITS = 256
private const val PBKDF2_ITERATIONS = 120_000
