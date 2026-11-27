package ir.dakhlokharj.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.dakhlokharj.DakhloKharjApp
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.format.MoneyFormatter
import ir.dakhlokharj.domain.engine.BudgetEngine
import ir.dakhlokharj.domain.model.TransactionType
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as? DakhloKharjApp ?: return Result.success()
        val container = app.container
        val settings = container.settingsRepository.current()
        if (!settings.reminderEnabled) return Result.success()
        val today = JalaliDate.today()
        val horizon = today.plusDays(3)
        val db = container.database

        val installments = db.installmentDao().getUnpaidUntil(horizon.toEpochDay())
            .filter { it.paidEpochDay == null }
        if (installments.isNotEmpty()) {
            val total = installments.sumOf { it.amountRial }
            NotificationHelper.notify(
                applicationContext,
                1001,
                "اقساط نزدیک سررسید",
                "${installments.size} قسط تا سه روز آینده سررسید می‌شود؛ مجموع ${MoneyFormatter.format(ir.dakhlokharj.core.money.Money(total))}.",
            )
        }

        val debts = db.debtDao().getAll().filter {
            it.principalRial - it.paidRial > 0 && it.dueEpochDay <= horizon.toEpochDay()
        }
        if (debts.isNotEmpty()) {
            NotificationHelper.notify(
                applicationContext,
                1002,
                "سررسید بدهی",
                "${debts.size} بدهی سررسید شده یا نزدیک است. وضعیت پرداخت را بررسی کنید.",
            )
        }

        val recurring = db.recurringDao().getAll().filter {
            it.active && it.nextDueEpochDay <= horizon.toEpochDay()
        }
        if (recurring.isNotEmpty()) {
            NotificationHelper.notify(
                applicationContext,
                1003,
                "تمدید اشتراک و هزینه ثابت",
                "${recurring.size} هزینه تکرارشونده در روزهای آینده سررسید می‌شود.",
            )
        }

        val budgets = db.budgetDao().getAll()
        if (budgets.isNotEmpty()) {
            val transactions = db.transactionDao().getAll().map { it.toDomain() }
            val critical = budgets.filter {
                BudgetEngine.progress(it.toDomain(), transactions, today).percent >= 90
            }
            if (critical.isNotEmpty()) {
                NotificationHelper.notify(
                    applicationContext,
                    1004,
                    "نزدیک شدن به سقف بودجه",
                    "${critical.size} بودجه به ۹۰٪ سقف یا بیشتر رسیده است.",
                )
            }
        }

        val todayTransactions = db.transactionDao()
            .getRange(today.toEpochDay(), today.toEpochDay())
            .filter { it.type == TransactionType.EXPENSE }
        if (todayTransactions.isEmpty()) {
            NotificationHelper.notify(
                applicationContext,
                1005,
                "ثبت هزینه امروز",
                "امروز هنوز هزینه‌ای ثبت نکرده‌اید. با یک ثبت کوتاه، تصویر مالی خود را کامل نگه دارید.",
            )
        }
        Result.success()
    }.getOrElse { Result.success() }
}

object ReminderScheduler {

    private const val WORK_NAME = "dakhlo_kharj_daily_reminders"

    fun schedule(context: Context, hour: Int = 21, minute: Int = 0) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
