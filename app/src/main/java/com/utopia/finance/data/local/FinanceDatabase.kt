package com.utopia.finance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.utopia.finance.data.local.dao.AccountDao
import com.utopia.finance.data.local.dao.BackupDao
import com.utopia.finance.data.local.dao.CatalogDao
import com.utopia.finance.data.local.dao.CreditBillDao
import com.utopia.finance.data.local.dao.DebtDao
import com.utopia.finance.data.local.dao.EconomicNoteDao
import com.utopia.finance.data.local.dao.ExchangeRateDao
import com.utopia.finance.data.local.dao.InvestmentDao
import com.utopia.finance.data.local.dao.LendingDao
import com.utopia.finance.data.local.dao.SubscriptionDao
import com.utopia.finance.data.local.dao.TransactionDao
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

@Database(
    entities = [
        AccountEntity::class,
        IncomeSourceEntity::class,
        ExpenseCategoryEntity::class,
        CounterpartyEntity::class,
        TransactionEntity::class,
        InvestmentEntity::class,
        LendingEntity::class,
        DebtEntity::class,
        CreditBillEntity::class,
        SubscriptionEntity::class,
        ExchangeRateEntity::class,
        EconomicNoteEntity::class,
    ],
    version = 7,
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun catalogDao(): CatalogDao
    abstract fun transactionDao(): TransactionDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun lendingDao(): LendingDao
    abstract fun debtDao(): DebtDao
    abstract fun creditBillDao(): CreditBillDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun economicNoteDao(): EconomicNoteDao
    abstract fun backupDao(): BackupDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `economic_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `occurredEpochDay` INTEGER NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_economic_notes_occurredEpochDay` ON `economic_notes` (`occurredEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_economic_notes_createdAtMillis` ON `economic_notes` (`createdAtMillis`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `parentAccountId` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_parentAccountId` ON `accounts` (`parentAccountId`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `income_sources` ADD COLUMN `defaultAccountId` INTEGER")
                db.execSQL("ALTER TABLE `expense_categories` ADD COLUMN `defaultAccountId` INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `role` TEXT NOT NULL DEFAULT 'AVAILABLE_CASH'")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `billDay` INTEGER")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `repaymentDay` INTEGER")
                db.execSQL(
                    """
                    UPDATE `accounts`
                    SET `role` = 'CREDIT_LIABILITY',
                        `billDay` = COALESCE(`billDay`, 1),
                        `repaymentDay` = COALESCE(`repaymentDay`, 10)
                    WHERE `type` IN ('CREDIT', 'HUABEI', 'CREDIT_CARD')
                    """.trimIndent(),
                )
                db.execSQL("UPDATE `accounts` SET `role` = 'INVESTMENT_ASSET' WHERE `type` = 'INVESTMENT'")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `creditBillId` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_creditBillId` ON `transactions` (`creditBillId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_bills` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `creditAccountId` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `periodKey` TEXT NOT NULL,
                        `billEpochDay` INTEGER NOT NULL,
                        `dueEpochDay` INTEGER NOT NULL,
                        `amountMinor` INTEGER NOT NULL,
                        `repaidMinor` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`creditAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_credit_bills_creditAccountId` ON `credit_bills` (`creditAccountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_credit_bills_status` ON `credit_bills` (`status`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_credit_bills_creditAccountId_currency_periodKey` ON `credit_bills` (`creditAccountId`, `currency`, `periodKey`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE `accounts` SET `currency` = 'USD' WHERE `type` = 'U_CARD'")
                db.execSQL(
                    """
                    UPDATE `transactions`
                    SET `currency` = 'USD'
                    WHERE `accountId` IN (SELECT `id` FROM `accounts` WHERE `type` = 'U_CARD')
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE `subscriptions`
                    SET `currency` = 'USD'
                    WHERE `accountId` IN (SELECT `id` FROM `accounts` WHERE `type` = 'U_CARD')
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `investmentQuantity` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun create(context: Context): FinanceDatabase =
            Room.databaseBuilder(context, FinanceDatabase::class.java, "personal-finance.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration(false)
                .build()
    }
}
