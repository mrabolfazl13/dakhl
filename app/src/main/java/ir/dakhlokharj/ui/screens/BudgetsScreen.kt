package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Savings
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
import ir.dakhlokharj.domain.model.BudgetPeriod
import ir.dakhlokharj.domain.model.BudgetScope
import ir.dakhlokharj.domain.model.Envelope
import ir.dakhlokharj.ui.budgetPeriodLabel
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

@Composable
fun BudgetsScreen(
    viewModel: PlanningViewModel,
    onBack: () -> Unit,
    onOpenBudget: (Long) -> Unit,
    onAddTransaction: (String) -> Unit,
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val envelopes by viewModel.envelopes.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = JalaliDate.today()
    val colors = MaterialTheme.colorScheme

    var showAddBudget by remember { mutableStateOf(false) }
    var showAddEnvelope by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var showEnvelopeAmount by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var amountToman by remember { mutableStateOf(0L) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var period by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var repeatNextMonth by remember { mutableStateOf(true) }
    var envelopeName by remember { mutableStateOf("") }
    var envelopeAmount by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    val active = budgets.filter { today >= it.startDate && today <= it.endDate }
    val progresses = active.map { BudgetEngine.progress(it, transactions, today) }
    val totalAllocated = Money.sum(active.map { it.amount })
    val totalSpent = Money.sum(progresses.map { it.spent })
    val overallPercent = if (totalAllocated.rial > 0) (totalSpent.rial * 100 / totalAllocated.rial).toInt() else 0
    val expenseCategories = categories.filter { it.kind == ir.dakhlokharj.domain.model.CategoryKind.EXPENSE }
    val categoryMap = categories.associateBy { it.id }

    DkScreen(
        title = "بودجه‌ها",
        subtitle = "سقف هزینه‌ها، پاکت‌ها و کنترل ماهانه",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAddBudget = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "افزودن بودجه")
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTransaction("EXPENSE") },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ) { Icon(Icons.Outlined.Add, contentDescription = "هزینه جدید") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 6.dp,
                bottom = padding.calculateBottomPadding() + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DkCard(accent = colors.primary) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("بودجه این دوره", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(moneyText(totalSpent), style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "از ${moneyText(totalAllocated)} • ${fa(active.size)} بودجه فعال",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Text(faPercent(overallPercent), style = MaterialTheme.typography.headlineMedium, color = if (overallPercent > 100) colors.error else colors.primary)
                    }
                    Spacer(Modifier.height(14.dp))
                    DkProgressBar(progress = overallPercent / 100f, color = if (overallPercent > 100) colors.error else colors.primary)
                }
            }

            item {
                SectionHeader("بودجه‌های فعال", actionLabel = "افزودن") { showAddBudget = true }
            }
            if (progresses.isEmpty()) {
                item {
                    EmptyState(
                        title = "هنوز بودجه‌ای تعریف نکرده‌اید",
                        description = "برای دسته‌های مهم مثل خوراک و حمل‌ونقل سقف ماهانه تعیین کنید تا هشدارهای پیشگیرانه بگیرید.",
                        icon = Icons.Outlined.Savings,
                        actionLabel = "ساخت بودجه",
                        onAction = { showAddBudget = true },
                    )
                }
            } else {
                items(progresses, key = { it.budget.id }) { progress ->
                    BudgetProgressCard(progress) { onOpenBudget(progress.budget.id) }
                }
            }

            item {
                SectionHeader("بودجه‌های دوره‌های دیگر")
            }
            val others = budgets.filterNot { today >= it.startDate && today <= it.endDate }
            if (others.isEmpty()) {
                item {
                    Text(
                        "بودجه غیرفعالی وجود ندارد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                items(others, key = { it.id }) { budget ->
                    DkCard(onClick = { onOpenBudget(budget.id) }, contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(budget.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${budgetPeriodLabel(budget.period)} • ${budget.startDate.formatShort()} تا ${budget.endDate.formatShort()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Text(moneyText(budget.amount), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                SectionHeader("پاکت‌های بودجه", actionLabel = "پاکت جدید") { showAddEnvelope = true }
            }
            if (envelopes.isEmpty()) {
                item {
                    Text(
                        "پاکت‌ها به شما کمک می‌کنند پول هر هدف را از قبل کنار بگذارید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                items(envelopes, key = { it.id }) { envelope ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(iconFor(categoryMap[envelope.categoryId]?.iconKey ?: "category"), contentDescription = null, tint = colors.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(envelope.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                categoryMap[envelope.categoryId]?.name ?: "بدون دسته",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Text(moneyText(envelope.allocated), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showAddBudget) {
        DkBottomSheet(onDismiss = { showAddBudget = false }) {
            SheetTitle("بودجه جدید", "برای یک دسته یا کل هزینه‌ها سقف تعیین کنید")
            DkTextField(name, { name = it }, label = "نام بودجه", placeholder = "مثلاً بودجه خوراک")
            Spacer(Modifier.height(10.dp))
            PickerRow(
                "دسته‌بندی",
                categoryMap[categoryId]?.name ?: "کل هزینه‌ها (بدون دسته)",
            ) { showCategoryPicker = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("مبلغ بودجه", if (amountToman > 0) moneyText(Money.toman(amountToman)) else "تعیین مبلغ") { showAmount = true }
            Spacer(Modifier.height(12.dp))
            Text("دوره", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(BudgetPeriod.MONTHLY, BudgetPeriod.WEEKLY, BudgetPeriod.YEARLY)) { entry ->
                    DkChoiceChip(budgetPeriodLabel(entry), period == entry) { period = entry }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = repeatNextMonth, onCheckedChange = { repeatNextMonth = it })
                Text("تکرار خودکار در دوره بعد", style = MaterialTheme.typography.bodySmall)
            }
            if (error != null) {
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ساخت بودجه") {
                if (name.isBlank()) error = "نام بودجه را وارد کنید"
                else if (amountToman <= 0) error = "مبلغ بودجه را وارد کنید"
                else {
                    val start = when (period) {
                        BudgetPeriod.WEEKLY -> today
                        BudgetPeriod.YEARLY -> today.startOfYear
                        else -> today.startOfMonth
                    }
                    val end = when (period) {
                        BudgetPeriod.WEEKLY -> start.plusDays(6)
                        BudgetPeriod.YEARLY -> today.endOfYear
                        else -> today.endOfMonth
                    }
                    scope.launch {
                        viewModel.saveBudget(
                            Budget(
                                name = name,
                                scope = if (categoryId == null) BudgetScope.OVERALL else BudgetScope.CATEGORY,
                                categoryId = categoryId,
                                period = period,
                                startDate = start,
                                endDate = end,
                                amount = Money.toman(amountToman),
                                repeatNextMonth = repeatNextMonth,
                                createdAt = System.currentTimeMillis(),
                            ),
                            editing = false,
                        ).onSuccess {
                            showAddBudget = false
                            name = ""
                            amountToman = 0
                            categoryId = null
                            error = null
                        }.onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showAddEnvelope) {
        DkBottomSheet(onDismiss = { showAddEnvelope = false }) {
            SheetTitle("پاکت بودجه", "مبلغی را از قبل برای یک هدف کنار بگذارید")
            DkTextField(envelopeName, { envelopeName = it }, label = "نام پاکت", placeholder = "مثلاً پاکت سفر")
            Spacer(Modifier.height(10.dp))
            PickerRow("مبلغ", if (envelopeAmount > 0) moneyText(Money.toman(envelopeAmount)) else "تعیین مبلغ") { showEnvelopeAmount = true }
            Spacer(Modifier.height(14.dp))
            PrimaryButton("ساخت پاکت") {
                if (envelopeName.isBlank() || envelopeAmount <= 0) {
                    error = "نام و مبلغ پاکت را کامل کنید"
                } else {
                    scope.launch {
                        viewModel.saveEnvelope(
                            Envelope(name = envelopeName, allocated = Money.toman(envelopeAmount)),
                            editing = false,
                        ).onSuccess {
                            showAddEnvelope = false
                            envelopeName = ""
                            envelopeAmount = 0
                        }.onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showAmount) {
        AmountInputSheet("مبلغ بودجه", amountToman, { showAmount = false }) { amountToman = it }
    }
    if (showEnvelopeAmount) {
        AmountInputSheet("مبلغ پاکت", envelopeAmount, { showEnvelopeAmount = false }) { envelopeAmount = it }
    }
    if (showCategoryPicker) {
        OptionPickerSheet(
            title = "دسته‌بندی بودجه",
            options = listOf(SheetOption(0L, "کل هزینه‌ها")) + expenseCategories.map {
                SheetOption(it.id, it.name, color = androidx.compose.ui.graphics.Color(it.colorArgb))
            },
            selectedId = categoryId,
            onDismiss = { showCategoryPicker = false },
            onSelect = { categoryId = if (it == 0L) null else it },
        )
    }
}