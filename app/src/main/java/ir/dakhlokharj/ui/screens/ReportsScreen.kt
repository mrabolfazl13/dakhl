package ir.dakhlokharj.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.BudgetEngine
import ir.dakhlokharj.domain.engine.ReportEngine
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.domain.model.TransactionType
import ir.dakhlokharj.ui.budgetStatusLabel
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel
import ir.dakhlokharj.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private enum class ReportPeriod(val label: String) {
    THIS_MONTH("این ماه"),
    LAST_MONTH("ماه قبل"),
    LAST_90("۹۰ روز اخیر"),
    THIS_YEAR("سال جاری"),
}

@Composable
fun ReportsScreen(
    viewModel: OverviewViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by settingsViewModel.busy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var period by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    var exportMessage by remember { mutableStateOf<String?>(null) }

    val today = state.today
    val (from, to) = when (period) {
        ReportPeriod.THIS_MONTH -> today.startOfMonth to today.endOfMonth
        ReportPeriod.LAST_MONTH -> today.startOfMonth.plusMonths(-1) to today.startOfMonth.plusMonths(-1).endOfMonth
        ReportPeriod.LAST_90 -> today.plusDays(-89) to today
        ReportPeriod.THIS_YEAR -> today.startOfYear to today.endOfYear
    }
    val days = (to.toEpochDay() - from.toEpochDay() + 1).coerceAtLeast(1)
    val previousTo = from.plusDays(-1)
    val previousFrom = previousTo.plusDays(-(days - 1))

    fun totals(rangeFrom: JalaliDate, rangeTo: JalaliDate): Pair<Money, Money> {
        val inRange = state.transactions.filter { it.date >= rangeFrom && it.date <= rangeTo }
        return Money.sum(inRange.filter { it.type == TransactionType.INCOME }.map { it.amount }) to
            Money.sum(inRange.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
    }

    val (income, expense) = totals(from, to)
    val (prevIncome, prevExpense) = totals(previousFrom, previousTo)
    val net = income - expense
    val prevNet = prevIncome - prevExpense
    val savingsRate = if (income.rial > 0) net.rial.toDouble() / income.rial else 0.0
    val expenseSlices = ReportEngine.categoryTotals(state.transactions, state.categories, CategoryKind.EXPENSE, from, to)
    val incomeSlices = ReportEngine.categoryTotals(state.transactions, state.categories, CategoryKind.INCOME, from, to)
    val accountMap = state.accountById
    val periodTransactions = state.transactions.filter { it.date >= from && it.date <= to }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                settingsViewModel.exportCsv(uri)
                    .onSuccess { exportMessage = "فایل CSV با موفقیت ذخیره شد." }
                    .onFailure { exportMessage = it.message ?: "ذخیره فایل ناموفق بود" }
            }
        }
    }

    DkScreen(
        title = "گزارش‌ها",
        subtitle = "تحلیل دوره‌ای و خروجی داده",
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                exportLauncher.launch("dakhlo-kharj-${today.year}-${today.month}-${today.day}.csv")
            }) {
                Icon(Icons.Outlined.FileDownload, contentDescription = "خروجی CSV")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 6.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ReportPeriod.entries.toList()) { entry ->
                        DkChoiceChip(entry.label, period == entry) { period = entry }
                    }
                }
            }
            item {
                Text(
                    "${faDate(from, DateStyle.FULL)} تا ${faDate(to, DateStyle.FULL)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            item {
                DkCard(accent = colors.primary) {
                    Text("خلاصه دوره", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        (if (net.isNegative) "−" else "") + moneyText(Money(kotlin.math.abs(net.rial))),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (net.isNegative) colors.error else colors.onSurface,
                    )
                    Text("خالص جریان نقدی", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("درآمد", moneyShort(income), Modifier.weight(1f), colors.primary)
                        StatTile("هزینه", moneyShort(expense), Modifier.weight(1f), colors.secondary)
                        StatTile("نرخ پس‌انداز", faPercent((savingsRate * 100).toInt()), Modifier.weight(1f))
                    }
                }
            }

            item {
                DkCard {
                    Text("مقایسه با دوره قبل", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    InfoRow(
                        "درآمد",
                        moneyShort(income) + " • " + changeLabel(income, prevIncome),
                        valueColor = if (income >= prevIncome) colors.primary else colors.error,
                    )
                    InfoRow(
                        "هزینه",
                        moneyShort(expense) + " • " + changeLabel(expense, prevExpense),
                        valueColor = if (expense <= prevExpense) colors.primary else colors.error,
                    )
                    InfoRow(
                        "خالص",
                        moneyShort(net) + " • " + changeLabel(net, prevNet),
                        valueColor = if (net >= prevNet) colors.primary else colors.error,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "دوره مقایسه: ${faDate(previousFrom, DateStyle.SHORT)} تا ${faDate(previousTo, DateStyle.SHORT)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            item { SectionHeader("جریان نقدی شش ماه") }
            item { DkCard { CashFlowChart(state.monthlyTotals) } }

            item { SectionHeader("هزینه به تفکیک دسته") }
            if (expenseSlices.isEmpty()) {
                item { Text("در این دوره هزینه‌ای ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            } else {
                item {
                    DkCard {
                        DonutChart(
                            slices = expenseSlices,
                            centerLabel = "هزینه دوره",
                            centerValue = moneyShort(expense),
                            modifier = Modifier.size(180.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        expenseSlices.take(8).forEach { slice ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LegendDot(androidx.compose.ui.graphics.Color(slice.colorArgb), slice.name)
                                Spacer(Modifier.weight(1f))
                                Text(faPercent((slice.share * 100).toInt()), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(moneyShort(slice.amount), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item { SectionHeader("درآمد به تفکیک دسته") }
            if (incomeSlices.isEmpty()) {
                item { Text("در این دوره درآمدی ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            } else {
                item {
                    DkCard {
                        incomeSlices.take(8).forEach { slice ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LegendDot(androidx.compose.ui.graphics.Color(slice.colorArgb), slice.name)
                                Spacer(Modifier.weight(1f))
                                Text(moneyShort(slice.amount), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item { SectionHeader("حساب‌ها") }
            items(state.accounts, key = { "acc-${it.id}" }) { account ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(account.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(moneyText(account.balance), style = MaterialTheme.typography.bodyMedium)
                }
            }

            item { SectionHeader("تعهدات و اقساط") }
            item {
                DkCard {
                    InfoRow("بدهی باقی‌مانده", moneyText(Money.sum(state.debts.map { it.remaining })), valueColor = colors.error)
                    InfoRow("طلب باقی‌مانده", moneyText(Money.sum(state.receivables.map { it.remaining })), valueColor = colors.primary)
                    InfoRow("اقساط پرداخت‌نشده", moneyText(Money.sum(state.installments.filter { !it.isPaid }.map { it.amount })))
                    InfoRow("تعداد طرح اقساط", fa(state.plans.size))
                }
            }

            item { SectionHeader("پس‌انداز") }
            if (state.goals.isEmpty()) {
                item { Text("هدف پس‌اندازی ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            } else {
                item {
                    DkCard {
                        state.goals.forEach { goal ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(goal.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${moneyShort(goal.saved)} از ${moneyShort(goal.target)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("بودجه‌ها") }
            if (state.budgets.isEmpty()) {
                item { Text("بودجه‌ای تعریف نشده است.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            } else {
                items(state.budgets, key = { "budget-${it.id}" }) { budget ->
                    val progress = BudgetEngine.progress(budget, state.transactions, today)
                    DkCard(contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(budget.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${moneyShort(progress.spent)} از ${moneyShort(budget.amount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Pill(
                                "${faPercent(progress.percent)} • ${budgetStatusLabel(progress.status)}",
                                when (progress.status) {
                                    ir.dakhlokharj.domain.model.BudgetStatus.HEALTHY -> ir.dakhlokharj.domain.model.InsightTone.POSITIVE
                                    ir.dakhlokharj.domain.model.BudgetStatus.WARNING -> ir.dakhlokharj.domain.model.InsightTone.NEUTRAL
                                    else -> ir.dakhlokharj.domain.model.InsightTone.NEGATIVE
                                },
                            )
                        }
                    }
                }
            }

            item { SectionHeader("تراکنش‌های دوره") }
            item {
                Text(
                    "${fa(periodTransactions.size)} تراکنش • خروجی CSV شامل همه تراکنش‌های شماست",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            item {
                SecondaryButton(
                    if (busy) "در حال آماده‌سازی..." else "خروجی CSV",
                    icon = Icons.Outlined.FileDownload,
                ) {
                    exportLauncher.launch("dakhlo-kharj-${today.year}-${today.month}-${today.day}.csv")
                }
            }
            if (exportMessage != null) {
                item {
                    Text(exportMessage!!, style = MaterialTheme.typography.bodySmall, color = colors.primary)
                }
            }
        }
    }
}

private fun changeLabel(current: Money, previous: Money): String {
    if (previous.rial == 0L) {
        return if (current.rial == 0L) "بدون تغییر" else "جدید"
    }
    val delta = (current.rial - previous.rial) * 100.0 / kotlin.math.abs(previous.rial)
    val sign = if (delta >= 0) "+" else "−"
    return sign + faPercent(kotlin.math.abs(delta).toInt())
}