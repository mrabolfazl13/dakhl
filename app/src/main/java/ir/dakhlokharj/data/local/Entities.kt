package ir.dakhlokharj.data.local

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.dakhlokharj.domain.model.AccountType
import ir.dakhlokharj.domain.model.BudgetPeriod
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.PlannedEventKind
import ir.dakhlokharj.domain.model.RecurringKind
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.domain.model.TransactionType

@Entity(tableName = "accounts")
@Serializable
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val openingBalanceRial: Long,
    val balanceRial: Long,
    val colorArgb: Int,
    val iconKey: String,
    val archived: Boolean = false,
    val createdAt: Long = 0,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "categories",
    indices = [Index("kind"), Index("sortOrder")],
)
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val iconKey: String,
    val colorArgb: Int,
    val essential: Boolean = true,
    val system: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "transactions",
    indices = [Index("dateEpochDay"), Index("accountId"), Index("categoryId"), Index("type")],
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amountRial: Long,
    val title: String,
    val categoryId: Long? = null,
    val accountId: Long,
    val targetAccountId: Long? = null,
    val dateEpochDay: Long,
    val timeMinutes: Int = 0,
    val note: String = "",
    val receiptUri: String? = null,
    val tag: String? = null,
    val recurringId: Long? = null,
    val debtId: Long? = null,
    val receivableId: Long? = null,
    val installmentId: Long? = null,
    val savingsGoalId: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Entity(tableName = "budgets", indices = [Index("categoryId"), Index("startEpochDay"), Index("endEpochDay")])
@Serializable
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val scope: BudgetScope,
    val categoryId: Long? = null,
    val period: BudgetPeriod,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val amountRial: Long,
    val alertAt50: Boolean = true,
    val alertAt75: Boolean = true,
    val alertAt90: Boolean = true,
    val alertAt100: Boolean = true,
    val repeatNextMonth: Boolean = false,
    val createdAt: Long = 0,
)

@Entity(tableName = "envelopes", indices = [Index("categoryId")])
@Serializable
data class EnvelopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val allocatedRial: Long,
    val sortOrder: Int = 0,
)

@Entity(tableName = "debts", indices = [Index("dueEpochDay")])
@Serializable
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditor: String,
    val principalRial: Long,
    val paidRial: Long = 0,
    val dueEpochDay: Long,
    val installmentCount: Int = 1,
    val note: String = "",
    val createdAt: Long = 0,
)

@Entity(
    tableName = "debt_payments",
    indices = [Index(value = ["debtId"]), Index(value = ["transactionId"], unique = true)],
)
@Serializable
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amountRial: Long,
    val dateEpochDay: Long,
    val transactionId: Long? = null,
    val createdAt: Long = 0,
)

@Entity(tableName = "receivables", indices = [Index("expectedEpochDay")])
@Serializable
data class ReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtor: String,
    val amountRial: Long,
    val receivedRial: Long = 0,
    val expectedEpochDay: Long,
    val note: String = "",
    val createdAt: Long = 0,
)

@Entity(
    tableName = "receivable_payments",
    indices = [Index(value = ["receivableId"]), Index(value = ["transactionId"], unique = true)],
)
@Serializable
data class ReceivablePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receivableId: Long,
    val amountRial: Long,
    val dateEpochDay: Long,
    val transactionId: Long? = null,
    val createdAt: Long = 0,
)

@Entity(tableName = "installment_plans")
@Serializable
data class InstallmentPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalRial: Long,
    val downPaymentRial: Long,
    val installmentCount: Int,
    val installmentRial: Long,
    val firstDueEpochDay: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val note: String = "",
    val createdAt: Long = 0,
)

@Entity(tableName = "installments", indices = [Index("planId"), Index("dueEpochDay")])
@Serializable
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val index: Int,
    val amountRial: Long,
    val dueEpochDay: Long,
    val paidEpochDay: Long? = null,
    val transactionId: Long? = null,
)

@Entity(tableName = "recurring_expenses", indices = [Index("nextDueEpochDay"), Index("categoryId")])
@Serializable
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountRial: Long,
    val categoryId: Long? = null,
    val accountId: Long,
    val period: RecurringPeriod,
    val nextDueEpochDay: Long,
    val kind: RecurringKind = RecurringKind.BILL,
    val active: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val note: String = "",
    val lastPaidEpochDay: Long? = null,
    val createdAt: Long = 0,
)

@Entity(tableName = "savings_goals")
@Serializable
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetRial: Long,
    val savedRial: Long = 0,
    val targetEpochDay: Long? = null,
    val colorArgb: Int,
    val iconKey: String,
    val note: String = "",
    val achieved: Boolean = false,
    val createdAt: Long = 0,
)

@Entity(tableName = "savings_contributions", indices = [Index("goalId"), Index("dateEpochDay")])
@Serializable
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amountRial: Long,
    val dateEpochDay: Long,
    val transactionId: Long? = null,
    val createdAt: Long = 0,
)

@Entity(tableName = "financial_events", indices = [Index("dateEpochDay")])
@Serializable
data class FinancialEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: PlannedEventKind,
    val title: String,
    val amountRial: Long,
    val dateEpochDay: Long,
    val definite: Boolean = true,
    val note: String = "",
    val createdAt: Long = 0,
)

@Entity(tableName = "backup_metadata")
@Serializable
data class BackupMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val locationUri: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val schemaVersion: Int,
    val encrypted: Boolean,
)