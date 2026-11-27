package ir.dakhlokharj.data.local

import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.DebtPayment
import ir.dakhlokharj.domain.model.Envelope
import ir.dakhlokharj.domain.model.FinancialEvent
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.InstallmentPlan
import ir.dakhlokharj.domain.model.Receivable
import ir.dakhlokharj.domain.model.ReceivablePayment
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.SavingsContribution
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.domain.model.Transaction

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    openingBalance = Money(openingBalanceRial),
    balance = Money(balanceRial),
    colorArgb = colorArgb,
    iconKey = iconKey,
    archived = archived,
    createdAt = createdAt,
    sortOrder = sortOrder,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type,
    openingBalanceRial = openingBalance.rial,
    balanceRial = balance.rial,
    colorArgb = colorArgb,
    iconKey = iconKey,
    archived = archived,
    createdAt = createdAt,
    sortOrder = sortOrder,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    kind = kind,
    iconKey = iconKey,
    colorArgb = colorArgb,
    essential = essential,
    system = system,
    sortOrder = sortOrder,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    kind = kind,
    iconKey = iconKey,
    colorArgb = colorArgb,
    essential = essential,
    system = system,
    sortOrder = sortOrder,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    type = type,
    amount = Money(amountRial),
    title = title,
    categoryId = categoryId,
    accountId = accountId,
    targetAccountId = targetAccountId,
    date = JalaliDate.fromEpochDay(dateEpochDay),
    timeMinutes = timeMinutes,
    note = note,
    receiptUri = receiptUri,
    tag = tag,
    recurringId = recurringId,
    debtId = debtId,
    receivableId = receivableId,
    installmentId = installmentId,
    savingsGoalId = savingsGoalId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type,
    amountRial = amount.rial,
    title = title,
    categoryId = categoryId,
    accountId = accountId,
    targetAccountId = targetAccountId,
    dateEpochDay = date.toEpochDay(),
    timeMinutes = timeMinutes,
    note = note,
    receiptUri = receiptUri,
    tag = tag,
    recurringId = recurringId,
    debtId = debtId,
    receivableId = receivableId,
    installmentId = installmentId,
    savingsGoalId = savingsGoalId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    name = name,
    scope = scope,
    categoryId = categoryId,
    period = period,
    startDate = JalaliDate.fromEpochDay(startEpochDay),
    endDate = JalaliDate.fromEpochDay(endEpochDay),
    amount = Money(amountRial),
    alertAt50 = alertAt50,
    alertAt75 = alertAt75,
    alertAt90 = alertAt90,
    alertAt100 = alertAt100,
    repeatNextMonth = repeatNextMonth,
    createdAt = createdAt,
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    scope = scope,
    categoryId = categoryId,
    period = period,
    startEpochDay = startDate.toEpochDay(),
    endEpochDay = endDate.toEpochDay(),
    amountRial = amount.rial,
    alertAt50 = alertAt50,
    alertAt75 = alertAt75,
    alertAt90 = alertAt90,
    alertAt100 = alertAt100,
    repeatNextMonth = repeatNextMonth,
    createdAt = createdAt,
)

fun EnvelopeEntity.toDomain(): Envelope = Envelope(id, name, categoryId, Money(allocatedRial), sortOrder)

fun Envelope.toEntity(): EnvelopeEntity = EnvelopeEntity(id, name, categoryId, allocated.rial, sortOrder)

fun DebtEntity.toDomain(): Debt = Debt(
    id = id,
    creditor = creditor,
    principal = Money(principalRial),
    paid = Money(paidRial),
    dueDate = JalaliDate.fromEpochDay(dueEpochDay),
    installmentCount = installmentCount,
    note = note,
    createdAt = createdAt,
)

fun Debt.toEntity(): DebtEntity = DebtEntity(
    id = id,
    creditor = creditor,
    principalRial = principal.rial,
    paidRial = paid.rial,
    dueEpochDay = dueDate.toEpochDay(),
    installmentCount = installmentCount,
    note = note,
    createdAt = createdAt,
)

fun DebtPaymentEntity.toDomain(): DebtPayment = DebtPayment(
    id = id,
    debtId = debtId,
    amount = Money(amountRial),
    date = JalaliDate.fromEpochDay(dateEpochDay),
    transactionId = transactionId,
    createdAt = createdAt,
)

fun DebtPayment.toEntity(): DebtPaymentEntity = DebtPaymentEntity(
    id = id,
    debtId = debtId,
    amountRial = amount.rial,
    dateEpochDay = date.toEpochDay(),
    transactionId = transactionId,
    createdAt = createdAt,
)

fun ReceivableEntity.toDomain(): Receivable = Receivable(
    id = id,
    debtor = debtor,
    amount = Money(amountRial),
    received = Money(receivedRial),
    expectedDate = JalaliDate.fromEpochDay(expectedEpochDay),
    note = note,
    createdAt = createdAt,
)

fun Receivable.toEntity(): ReceivableEntity = ReceivableEntity(
    id = id,
    debtor = debtor,
    amountRial = amount.rial,
    receivedRial = received.rial,
    expectedEpochDay = expectedDate.toEpochDay(),
    note = note,
    createdAt = createdAt,
)

fun ReceivablePaymentEntity.toDomain(): ReceivablePayment = ReceivablePayment(
    id = id,
    receivableId = receivableId,
    amount = Money(amountRial),
    date = JalaliDate.fromEpochDay(dateEpochDay),
    transactionId = transactionId,
    createdAt = createdAt,
)

fun ReceivablePayment.toEntity(): ReceivablePaymentEntity = ReceivablePaymentEntity(
    id = id,
    receivableId = receivableId,
    amountRial = amount.rial,
    dateEpochDay = date.toEpochDay(),
    transactionId = transactionId,
    createdAt = createdAt,
)

fun InstallmentPlanEntity.toDomain(): InstallmentPlan = InstallmentPlan(
    id = id,
    title = title,
    total = Money(totalRial),
    downPayment = Money(downPaymentRial),
    installmentCount = installmentCount,
    installmentAmount = Money(installmentRial),
    firstDueDate = JalaliDate.fromEpochDay(firstDueEpochDay),
    accountId = accountId,
    categoryId = categoryId,
    note = note,
    createdAt = createdAt,
)

fun InstallmentPlan.toEntity(): InstallmentPlanEntity = InstallmentPlanEntity(
    id = id,
    title = title,
    totalRial = total.rial,
    downPaymentRial = downPayment.rial,
    installmentCount = installmentCount,
    installmentRial = installmentAmount.rial,
    firstDueEpochDay = firstDueDate.toEpochDay(),
    accountId = accountId,
    categoryId = categoryId,
    note = note,
    createdAt = createdAt,
)

fun InstallmentEntity.toDomain(): Installment = Installment(
    id = id,
    planId = planId,
    index = index,
    amount = Money(amountRial),
    dueDate = JalaliDate.fromEpochDay(dueEpochDay),
    paidDate = paidEpochDay?.let { JalaliDate.fromEpochDay(it) },
    transactionId = transactionId,
)

fun Installment.toEntity(): InstallmentEntity = InstallmentEntity(
    id = id,
    planId = planId,
    index = index,
    amountRial = amount.rial,
    dueEpochDay = dueDate.toEpochDay(),
    paidEpochDay = paidDate?.toEpochDay(),
    transactionId = transactionId,
)

fun RecurringExpenseEntity.toDomain(): RecurringExpense = RecurringExpense(
    id = id,
    title = title,
    amount = Money(amountRial),
    categoryId = categoryId,
    accountId = accountId,
    period = period,
    nextDueDate = JalaliDate.fromEpochDay(nextDueEpochDay),
    kind = kind,
    active = active,
    reminderEnabled = reminderEnabled,
    reminderDaysBefore = reminderDaysBefore,
    note = note,
    lastPaidDate = lastPaidEpochDay?.let { JalaliDate.fromEpochDay(it) },
    createdAt = createdAt,
)

fun RecurringExpense.toEntity(): RecurringExpenseEntity = RecurringExpenseEntity(
    id = id,
    title = title,
    amountRial = amount.rial,
    categoryId = categoryId,
    accountId = accountId,
    period = period,
    nextDueEpochDay = nextDueDate.toEpochDay(),
    kind = kind,
    active = active,
    reminderEnabled = reminderEnabled,
    reminderDaysBefore = reminderDaysBefore,
    note = note,
    lastPaidEpochDay = lastPaidDate?.toEpochDay(),
    createdAt = createdAt,
)

fun SavingsGoalEntity.toDomain(): SavingsGoal = SavingsGoal(
    id = id,
    title = title,
    target = Money(targetRial),
    saved = Money(savedRial),
    targetDate = targetEpochDay?.let { JalaliDate.fromEpochDay(it) },
    colorArgb = colorArgb,
    iconKey = iconKey,
    note = note,
    achieved = achieved,
    createdAt = createdAt,
)

fun SavingsGoal.toEntity(): SavingsGoalEntity = SavingsGoalEntity(
    id = id,
    title = title,
    targetRial = target.rial,
    savedRial = saved.rial,
    targetEpochDay = targetDate?.toEpochDay(),
    colorArgb = colorArgb,
    iconKey = iconKey,
    note = note,
    achieved = achieved,
    createdAt = createdAt,
)

fun SavingsContributionEntity.toDomain(): SavingsContribution = SavingsContribution(
    id = id,
    goalId = goalId,
    amount = Money(amountRial),
    date = JalaliDate.fromEpochDay(dateEpochDay),
    transactionId = transactionId,
    createdAt = createdAt,
)

fun SavingsContribution.toEntity(): SavingsContributionEntity = SavingsContributionEntity(
    id = id,
    goalId = goalId,
    amountRial = amount.rial,
    dateEpochDay = date.toEpochDay(),
    transactionId = transactionId,
    createdAt = createdAt,
)

fun FinancialEventEntity.toDomain(): FinancialEvent = FinancialEvent(
    id = id,
    kind = kind,
    title = title,
    amount = Money(amountRial),
    date = JalaliDate.fromEpochDay(dateEpochDay),
    definite = definite,
    note = note,
    createdAt = createdAt,
)

fun FinancialEvent.toEntity(): FinancialEventEntity = FinancialEventEntity(
    id = id,
    kind = kind,
    title = title,
    amountRial = amount.rial,
    dateEpochDay = date.toEpochDay(),
    definite = definite,
    note = note,
    createdAt = createdAt,
)

fun BackupMetadataEntity.toDomain(): ir.dakhlokharj.domain.model.BackupMetadata = ir.dakhlokharj.domain.model.BackupMetadata(
    id = id,
    fileName = fileName,
    locationUri = locationUri,
    createdAt = createdAt,
    sizeBytes = sizeBytes,
    schemaVersion = schemaVersion,
    encrypted = encrypted,
)

fun ir.dakhlokharj.domain.model.BackupMetadata.toEntity(): BackupMetadataEntity = BackupMetadataEntity(
    id = id,
    fileName = fileName,
    locationUri = locationUri,
    createdAt = createdAt,
    sizeBytes = sizeBytes,
    schemaVersion = schemaVersion,
    encrypted = encrypted,
)
