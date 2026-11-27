package ir.dakhlokharj.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.BudgetProgress
import ir.dakhlokharj.domain.engine.FinancialAlert
import ir.dakhlokharj.domain.model.Account
import ir.dakhlokharj.domain.model.BudgetStatus
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.domain.model.Transaction
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.ui.accountTypeLabel
import ir.dakhlokharj.ui.budgetStatusLabel
import ir.dakhlokharj.ui.debtStatusLabel
import ir.dakhlokharj.ui.debtStatusOf
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.faDate
import ir.dakhlokharj.ui.theme.faPercent
import ir.dakhlokharj.ui.theme.iconFor
import ir.dakhlokharj.ui.theme.moneyShort
import ir.dakhlokharj.ui.theme.moneyText

@Composable
fun TransactionTile(
    transaction: Transaction,
    categoryName: String?,
    accountName: String?,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val isIncome = transaction.type == TransactionType.INCOME ||
        transaction.type == TransactionType.SAVINGS_WITHDRAW
    val amountColor = if (isIncome) colors.primary else colors.onSurface
    val iconColor = when (transaction.type) {
        TransactionType.INCOME -> colors.primary
        TransactionType.EXPENSE -> colors.secondary
        TransactionType.TRANSFER -> colors.tertiary
        TransactionType.ADJUSTMENT -> colors.onSurfaceVariant
        TransactionType.SAVINGS_DEPOSIT -> colors.tertiary
        TransactionType.SAVINGS_WITHDRAW -> colors.primary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconColor.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                iconFor(if (transaction.type == TransactionType.TRANSFER) "card" else "category"),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title.ifBlank { categoryName ?: "تراکنش" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    categoryName,
                    if (showDate) faDate(transaction.date) else null,
                    accountName,
                ).joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (isIncome) "+" else "−") + moneyText(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = amountColor,
                fontWeight = FontWeight.Medium,
            )
            if (transaction.type == TransactionType.SAVINGS_DEPOSIT) {
                Text(
                    "پس‌انداز",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.tertiary,
                )
            }
        }
    }
}

@Composable
fun AccountCard(account: Account, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val accent = Color(account.colorArgb)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.3f), MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                iconFor(account.iconKey),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                account.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                accountTypeLabel(account.type),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            moneyText(account.balance),
            style = MaterialTheme.typography.titleMedium,
            color = if (account.balance.isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (onClick != null) {
            Icon(
                Icons.Outlined.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun BudgetProgressCard(progress: BudgetProgress, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val color = budgetStatusColor(progress.status)
    DkCard(
        modifier = modifier,
        onClick = onClick,
        accent = color.copy(alpha = 0.6f),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(progress.budget.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${moneyText(progress.spent)} از ${moneyText(progress.budget.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill(
                text = "${faPercent(progress.percent)} • ${budgetStatusLabel(progress.status)}",
                tone = when (progress.status) {
                    BudgetStatus.SAFE -> ir.dakhlokharj.domain.model.InsightTone.POSITIVE
                    BudgetStatus.WARNING -> ir.dakhlokharj.domain.model.InsightTone.NEUTRAL
                    BudgetStatus.DANGER,
                    BudgetStatus.EXCEEDED,
                    -> ir.dakhlokharj.domain.model.InsightTone.NEGATIVE
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        DkProgressBar(progress = progress.percent / 100f, color = color)
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                "باقی‌مانده: ${moneyText(progress.remaining)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "پیش‌بینی: ${faPercent(progress.projectedPercent)}",
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

@Composable
fun budgetStatusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.SAFE -> MaterialTheme.colorScheme.primary
    BudgetStatus.WARNING -> MaterialTheme.colorScheme.secondary
    BudgetStatus.DANGER,
    BudgetStatus.EXCEEDED,
    -> MaterialTheme.colorScheme.error
}

@Composable
fun DebtCard(debt: Debt, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val progress = if (debt.principal.rial > 0) debt.paid.rial.toFloat() / debt.principal.rial else 0f
    DkCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = null, tint = colors.error)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(debt.creditor, style = MaterialTheme.typography.titleMedium)
                Text(
                    debtStatusLabel(debtStatusOf(debt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (debtStatusOf(debt) == ir.dakhlokharj.domain.model.DebtStatus.OVERDUE) colors.error else colors.onSurfaceVariant,
                )
            }
            Text(moneyText(debt.remaining), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        DkProgressBar(progress = progress, color = colors.primary)
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                "پرداخت‌شده ${moneyShort(debt.paid)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            debt.dueDate?.let {
                Text(
                    "سررسید ${faDate(it, ir.dakhlokharj.ui.theme.DateStyle.SHORT)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun InstallmentRow(installment: Installment, planTitle: String, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val paid = installment.paidDate != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (paid) colors.primary.copy(alpha = 0.16f)
                    else colors.secondary.copy(alpha = 0.14f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fa(installment.index),
                style = MaterialTheme.typography.labelLarge,
                color = if (paid) colors.primary else colors.secondary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(planTitle, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                if (paid) "پرداخت‌شده • ${faDate(installment.paidDate!!)}"
                else "سررسید ${faDate(installment.dueDate)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Text(moneyText(installment.amount), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SavingsGoalCard(goal: SavingsGoal, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val progress = if (goal.target.rial > 0) (goal.saved.rial.toFloat() / goal.target.rial).coerceIn(0f, 1f) else 0f
    DkCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorsForGoal(goal.colorArgb).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    iconFor(goal.iconKey),
                    contentDescription = null,
                    tint = colorsForGoal(goal.colorArgb),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (goal.achieved) "به هدف رسیدید 🎉" else "${faPercent((progress * 100).toInt())} تکمیل شده",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (goal.achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(moneyText(goal.target), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        DkProgressBar(progress = progress, color = colorsForGoal(goal.colorArgb))
        Spacer(Modifier.height(6.dp))
        Text(
            "${moneyText(goal.saved)} پس‌انداز شده",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun colorsForGoal(argb: Int): Color = Color(argb)

@Composable
fun FinancialAlertCard(alert: FinancialAlert, onClick: (() -> Unit)? = null) {
    val color = ToneColor(alert.tone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = 0.09f))
            .border(1.dp, color.copy(alpha = 0.28f), MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            if (alert.tone == ir.dakhlokharj.domain.model.InsightTone.NEGATIVE) Icons.Outlined.Warning else iconFor(alert.iconKey),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                alert.title,
                style = MaterialTheme.typography.titleSmall,
                color = color,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}