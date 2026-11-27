package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.BudgetEngine
import ir.dakhlokharj.domain.model.Budget
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.ui.budgetPeriodLabel
import ir.dakhlokharj.ui.budgetScopeLabel
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

@Composable
fun BudgetDetailScreen(
    viewModel: PlanningViewModel,
    budgetId: Long,
    onBack: () -> Unit,
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val budget = budgets.firstOrNull { it.id == budgetId }
    val today = JalaliDate.today()
    val scope = rememberCoroutineScope()
    var showDelete by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DkScreen(
        title = budget?.name ?: "بودجه",
        subtitle = budget?.let { "${it.startDate.formatShort()} تا ${it.endDate.formatShort()}" },
        onBack = onBack,
    ) { padding ->
        if (budget == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("بودجه یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@DkScreen
        }
        val progress = BudgetEngine.progress(budget, transactions, today)
        val statusColor = budgetStatusColor(progress.status)
        val categoryMap = categories.associateBy { it.id }
        val scoped = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .filter { it.date >= budget.startDate && it.date <= budget.endDate }
            .filter { budget.scope == ir.dakhlokharj.domain.model.BudgetScope.OVERALL || it.categoryId == budget.categoryId }
            .sortedByDescending { it.date }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            DkCard(accent = statusColor) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(moneyText(progress.spent), style = MaterialTheme.typography.displaySmall)
                        Text(
                            "از ${moneyText(budget.amount)} • باقی‌مانده ${moneyText(progress.remaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Pill(
                        "${faPercent(progress.percent)} • ${ir.dakhlokharj.ui.budgetStatusLabel(progress.status)}",
                        tone = when (progress.status) {
                            ir.dakhlokharj.domain.model.BudgetStatus.SAFE -> ir.dakhlokharj.domain.model.InsightTone.POSITIVE
                            ir.dakhlokharj.domain.model.BudgetStatus.WARNING -> ir.dakhlokharj.domain.model.InsightTone.NEUTRAL
                            else -> ir.dakhlokharj.domain.model.InsightTone.NEGATIVE
                        },
                    )
                }
                Spacer(Modifier.height(14.dp))
                DkProgressBar(progress.percent / 100f, color = statusColor)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("پیش‌بینی پایان", faPercent(progress.projectedPercent), Modifier.weight(1f), statusColor)
                    StatTile("روز گذشته", "${fa(progress.daysElapsed)} از ${fa(progress.daysTotal)}", Modifier.weight(1f))
                    StatTile("پیش‌بینی هزینه", moneyShort(progress.projectedSpend), Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))
            DkCard {
                InfoRow("دوره", budgetPeriodLabel(budget.period))
                InfoRow("دامنه", budgetScopeLabel(budget.scope))
                InfoRow("دسته‌بندی", categoryMap[budget.categoryId]?.name ?: "کل هزینه‌ها")
                InfoRow("تکرار خودکار", if (budget.repeatNextMonth) "فعال" else "غیرفعال")
                InfoRow("هزینه‌های ثبت‌شده", "${fa(scoped.size)} تراکنش")
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("ویرایش مبلغ", Modifier.weight(1f), icon = Icons.Outlined.Edit) { showAmount = true }
                SecondaryButton(
                    "تکرار دوره بعد",
                    Modifier.weight(1f),
                    icon = Icons.Outlined.Repeat,
                ) {
                    scope.launch {
                        viewModel.repeatBudgetNextMonth(budget)
                            .onSuccess { error = "بودجه برای دوره بعد ساخته شد" }
                            .onFailure { error = it.message }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SecondaryButton("حذف بودجه", icon = Icons.Outlined.Delete) { showDelete = true }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("هزینه‌های این بودجه")
            if (scoped.isEmpty()) {
                EmptyState(
                    title = "هنوز هزینه‌ای در این دامنه نیست",
                    description = "با ثبت هزینه، مصرف بودجه به‌صورت خودکار محاسبه می‌شود.",
                    icon = Icons.Outlined.Delete,
                )
            } else {
                Column {
                    scoped.take(30).forEach { transaction ->
                        TransactionTile(
                            transaction = transaction,
                            categoryName = categoryMap[transaction.categoryId]?.name,
                            accountName = faDate(transaction.date, DateStyle.SHORT),
                        )
                    }
                }
            }
        }
    }

    if (showDelete && budget != null) {
        ConfirmDialog(
            title = "حذف بودجه؟",
            message = "این بودجه حذف می‌شود اما تراکنش‌های شما دست‌نخورده باقی می‌مانند.",
            confirmLabel = "حذف",
            danger = true,
            onConfirm = {
                scope.launch {
                    viewModel.deleteBudget(budget).onSuccess { onBack() }
                        .onFailure { error = it.message }
                }
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showAmount && budget != null) {
        AmountInputSheet(
            title = "مبلغ بودجه",
            initialToman = budget.amount.toTomanRounded(),
            onDismiss = { showAmount = false },
            onConfirm = { toman ->
                scope.launch {
                    viewModel.saveBudget(budget.copy(amount = Money.toman(toman)), editing = true)
                        .onFailure { error = it.message }
                }
            },
        )
    }
}