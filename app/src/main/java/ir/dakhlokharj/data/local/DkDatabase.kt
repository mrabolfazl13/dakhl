package ir.dakhlokharj.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        EnvelopeEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        ReceivableEntity::class,
        ReceivablePaymentEntity::class,
        InstallmentPlanEntity::class,
        InstallmentEntity::class,
        RecurringExpenseEntity::class,
        SavingsGoalEntity::class,
        SavingsContributionEntity::class,
        FinancialEventEntity::class,
        BackupMetadataEntity::class,
    ],
    version = DkDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DkDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun receivablePaymentDao(): ReceivablePaymentDao
    abstract fun installmentPlanDao(): InstallmentPlanDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun recurringDao(): RecurringDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun savingsContributionDao(): SavingsContributionDao
    abstract fun financialEventDao(): FinancialEventDao
    abstract fun backupMetadataDao(): BackupMetadataDao

    companion object {
        const val VERSION = 1
        const val NAME = "dakhlo_kharj.db"

        fun build(context: Context): DkDatabase = Room.databaseBuilder(
            context.applicationContext,
            DkDatabase::class.java,
            NAME,
        ).addMigrations(*Migrations.ALL)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
}
