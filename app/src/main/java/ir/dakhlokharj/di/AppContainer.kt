package ir.dakhlokharj.di

import android.content.Context
import ir.dakhlokharj.backup.BackupRepository
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.data.repository.AccountRepository
import ir.dakhlokharj.data.repository.BudgetRepository
import ir.dakhlokharj.data.repository.CategoryRepository
import ir.dakhlokharj.data.repository.DebtRepository
import ir.dakhlokharj.data.repository.EventRepository
import ir.dakhlokharj.data.repository.InstallmentRepository
import ir.dakhlokharj.data.repository.RecurringRepository
import ir.dakhlokharj.data.repository.SavingsRepository
import ir.dakhlokharj.data.repository.SeedData
import ir.dakhlokharj.data.repository.SettingsRepository
import ir.dakhlokharj.data.repository.TransactionRepository
import ir.dakhlokharj.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    val appContext = context.applicationContext
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: DkDatabase = DkDatabase.build(appContext)

    val settingsRepository = SettingsRepository(appContext)
    val categoryRepository = CategoryRepository(database.categoryDao())
    val accountRepository = AccountRepository(database, database.accountDao())
    val transactionRepository = TransactionRepository(database, database.transactionDao(), database.accountDao())
    val budgetRepository = BudgetRepository(database.budgetDao(), database.envelopeDao())
    val debtRepository = DebtRepository(
        database,
        database.debtDao(),
        database.debtPaymentDao(),
        database.receivableDao(),
        database.receivablePaymentDao(),
        transactionRepository,
    )
    val installmentRepository = InstallmentRepository(
        database,
        database.installmentPlanDao(),
        database.installmentDao(),
        transactionRepository,
    )
    val recurringRepository = RecurringRepository(database, database.recurringDao(), transactionRepository)
    val savingsRepository = SavingsRepository(
        database,
        database.savingsGoalDao(),
        database.savingsContributionDao(),
        transactionRepository,
    )
    val eventRepository = EventRepository(database.financialEventDao())
    val backupRepository = BackupRepository(appContext, database)

    init {
        applicationScope.launch {
            SeedData.seedIfEmpty(database)
            val settings = settingsRepository.current()
            ReminderScheduler.schedule(appContext, settings.dailyReminderHour, settings.dailyReminderMinute)
        }
    }
}