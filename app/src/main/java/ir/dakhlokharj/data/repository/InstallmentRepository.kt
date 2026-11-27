package ir.dakhlokharj.data.repository

import androidx.room.withTransaction
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.data.local.DkDatabase
import ir.dakhlokharj.data.local.InstallmentDao
import ir.dakhlokharj.data.local.InstallmentPlanDao
import ir.dakhlokharj.data.local.toDomain
import ir.dakhlokharj.data.local.toEntity
import ir.dakhlokharj.domain.engine.InstallmentScheduler
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.InstallmentPlan
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InstallmentRepository(
    private val db: DkDatabase,
    private val planDao: InstallmentPlanDao,
    private val installmentDao: InstallmentDao,
    private val transactionRepository: TransactionRepository,
) {

    fun observePlans(): Flow<List<InstallmentPlan>> = planDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<Installment>> = installmentDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observePlanInstallments(planId: Long): Flow<List<Installment>> =
        installmentDao.observeForPlan(planId).map { list -> list.map { it.toDomain() } }

    suspend fun createPlan(plan: InstallmentPlan): Long = db.withTransaction {
        val schedule = InstallmentScheduler.buildSchedule(
            total = plan.total,
            downPayment = plan.downPayment,
            count = plan.installmentCount,
            firstDueDate = plan.firstDueDate,
        )
        val first = schedule.first()
        val planId = planDao.insert(
            plan.copy(
                installmentAmount = first.amount,
                createdAt = System.currentTimeMillis(),
            ).toEntity(),
        )
        installmentDao.insertAll(
            schedule.map { scheduled ->
                Installment(
                    planId = planId,
                    index = scheduled.index,
                    amount = scheduled.amount,
                    dueDate = scheduled.dueDate,
                ).toEntity()
            },
        )
        if (plan.downPayment.isPositive) {
            transactionRepository.add(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = plan.downPayment,
                    title = "پیش‌پرداخت ${plan.title}",
                    accountId = plan.accountId,
                    categoryId = plan.categoryId,
                    date = plan.firstDueDate,
                ),
            )
        }
        planId
    }

    /** Pays one instalment. Paid instalments are immutable, which prevents duplicate payments. */
    suspend fun payInstallment(installmentId: Long, accountId: Long, date: JalaliDate): Long = db.withTransaction {
        val installment = installmentDao.getById(installmentId) ?: error("قسط یافت نشد")
        require(installment.paidEpochDay == null) { "این قسط قبلاً پرداخت شده است" }
        val plan = planDao.getById(installment.planId) ?: error("طرح اقساط یافت نشد")
        val transactionId = transactionRepository.add(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = Money(installment.amountRial),
                title = "قسط ${installment.index} - ${plan.title}",
                accountId = accountId,
                categoryId = plan.categoryId,
                date = date,
                installmentId = installmentId,
            ),
        )
        installmentDao.markPaid(installmentId, date.toEpochDay(), transactionId)
        transactionId
    }

    suspend fun deletePlan(plan: InstallmentPlan) = db.withTransaction {
        installmentDao.deleteForPlan(plan.id)
        planDao.delete(plan.toEntity())
    }
}
