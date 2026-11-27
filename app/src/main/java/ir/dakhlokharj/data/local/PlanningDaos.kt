package ir.dakhlokharj.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY dueEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Query("SELECT * FROM debts")
    suspend fun getAll(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(debts: List<DebtEntity>)

    @Update
    suspend fun update(debt: DebtEntity)

    @Delete
    suspend fun delete(debt: DebtEntity)

    @Query("DELETE FROM debts")
    suspend fun clear()
}

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY dateEpochDay DESC, id DESC")
    fun observeForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments")
    suspend fun getAll(): List<DebtPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: DebtPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<DebtPaymentEntity>)

    @Query("DELETE FROM debt_payments")
    suspend fun clear()
}

@Dao
interface ReceivableDao {
    @Query("SELECT * FROM receivables ORDER BY expectedEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables WHERE id = :id")
    suspend fun getById(id: Long): ReceivableEntity?

    @Query("SELECT * FROM receivables")
    suspend fun getAll(): List<ReceivableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receivable: ReceivableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(receivables: List<ReceivableEntity>)

    @Update
    suspend fun update(receivable: ReceivableEntity)

    @Delete
    suspend fun delete(receivable: ReceivableEntity)

    @Query("DELETE FROM receivables")
    suspend fun clear()
}

@Dao
interface ReceivablePaymentDao {
    @Query("SELECT * FROM receivable_payments ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<ReceivablePaymentEntity>>

    @Query("SELECT * FROM receivable_payments")
    suspend fun getAll(): List<ReceivablePaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: ReceivablePaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<ReceivablePaymentEntity>)

    @Query("DELETE FROM receivable_payments")
    suspend fun clear()
}

@Dao
interface InstallmentPlanDao {
    @Query("SELECT * FROM installment_plans ORDER BY firstDueEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<InstallmentPlanEntity>>

    @Query("SELECT * FROM installment_plans WHERE id = :id")
    suspend fun getById(id: Long): InstallmentPlanEntity?

    @Query("SELECT * FROM installment_plans")
    suspend fun getAll(): List<InstallmentPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: InstallmentPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<InstallmentPlanEntity>)

    @Update
    suspend fun update(plan: InstallmentPlanEntity)

    @Delete
    suspend fun delete(plan: InstallmentPlanEntity)

    @Query("DELETE FROM installment_plans")
    suspend fun clear()
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY dueEpochDay ASC, \"index\" ASC")
    fun observeAll(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE planId = :planId ORDER BY \"index\" ASC")
    fun observeForPlan(planId: Long): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getById(id: Long): InstallmentEntity?

    @Query("SELECT * FROM installments")
    suspend fun getAll(): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE paidEpochDay IS NULL AND dueEpochDay <= :until")
    suspend fun getUnpaidUntil(until: Long): List<InstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(installment: InstallmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(installments: List<InstallmentEntity>)

    @Update
    suspend fun update(installment: InstallmentEntity)

    @Query("UPDATE installments SET paidEpochDay = :paidEpochDay, transactionId = :transactionId WHERE id = :id")
    suspend fun markPaid(id: Long, paidEpochDay: Long, transactionId: Long)

    @Query("DELETE FROM installments WHERE planId = :planId")
    suspend fun deleteForPlan(planId: Long)

    @Query("DELETE FROM installments")
    suspend fun clear()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_expenses ORDER BY nextDueEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getById(id: Long): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses")
    suspend fun getAll(): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecurringExpenseEntity>)

    @Update
    suspend fun update(recurring: RecurringExpenseEntity)

    @Delete
    suspend fun delete(recurring: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses")
    suspend fun clear()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY achieved ASC, createdAt ASC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals")
    suspend fun getAll(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<SavingsGoalEntity>)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals")
    suspend fun clear()
}

@Dao
interface SavingsContributionDao {
    @Query("SELECT * FROM savings_contributions ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<SavingsContributionEntity>>

    @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY dateEpochDay DESC, id DESC")
    fun observeForGoal(goalId: Long): Flow<List<SavingsContributionEntity>>

    @Query("SELECT * FROM savings_contributions")
    suspend fun getAll(): List<SavingsContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contribution: SavingsContributionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contributions: List<SavingsContributionEntity>)

    @Query("DELETE FROM savings_contributions")
    suspend fun clear()
}

@Dao
interface FinancialEventDao {
    @Query("SELECT * FROM financial_events ORDER BY dateEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events")
    suspend fun getAll(): List<FinancialEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: FinancialEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<FinancialEventEntity>)

    @Update
    suspend fun update(event: FinancialEventEntity)

    @Delete
    suspend fun delete(event: FinancialEventEntity)

    @Query("DELETE FROM financial_events")
    suspend fun clear()
}