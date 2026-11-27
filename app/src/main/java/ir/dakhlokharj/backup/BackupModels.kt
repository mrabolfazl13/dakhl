package ir.dakhlokharj.backup

import ir.dakhlokharj.data.local.AccountEntity
import ir.dakhlokharj.data.local.BackupMetadataEntity
import ir.dakhlokharj.data.local.BudgetEntity
import ir.dakhlokharj.data.local.CategoryEntity
import ir.dakhlokharj.data.local.DebtEntity
import ir.dakhlokharj.data.local.DebtPaymentEntity
import ir.dakhlokharj.data.local.EnvelopeEntity
import ir.dakhlokharj.data.local.FinancialEventEntity
import ir.dakhlokharj.data.local.InstallmentEntity
import ir.dakhlokharj.data.local.InstallmentPlanEntity
import ir.dakhlokharj.data.local.ReceivableEntity
import ir.dakhlokharj.data.local.ReceivablePaymentEntity
import ir.dakhlokharj.data.local.RecurringExpenseEntity
import ir.dakhlokharj.data.local.SavingsContributionEntity
import ir.dakhlokharj.data.local.SavingsGoalEntity
import ir.dakhlokharj.data.local.TransactionEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val format: String = "dakhlo-kharj-data",
    val schemaVersion: Int,
    val createdAt: Long,
    val appVersion: String,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val envelopes: List<EnvelopeEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val debtPayments: List<DebtPaymentEntity> = emptyList(),
    val receivables: List<ReceivableEntity> = emptyList(),
    val receivablePayments: List<ReceivablePaymentEntity> = emptyList(),
    val installmentPlans: List<InstallmentPlanEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val recurringExpenses: List<RecurringExpenseEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val savingsContributions: List<SavingsContributionEntity> = emptyList(),
    val financialEvents: List<FinancialEventEntity> = emptyList(),
    val backupMetadata: List<BackupMetadataEntity> = emptyList(),
)

@Serializable
data class BackupEnvelope(
    val format: String = "dakhlo-kharj-backup",
    val schemaVersion: Int,
    val createdAt: Long,
    val appVersion: String,
    val encrypted: Boolean,
    /** Base64 ciphertext when encrypted, otherwise the raw UTF-8 JSON payload. */
    val payload: String,
    val salt: String? = null,
    val iv: String? = null,
    val checksum: String,
)

data class RestoreSummary(
    val accounts: Int,
    val transactions: Int,
    val categories: Int,
    val budgets: Int,
    val debts: Int,
    val savingsGoals: Int,
)
