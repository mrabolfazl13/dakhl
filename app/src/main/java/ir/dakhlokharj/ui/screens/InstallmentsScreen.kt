package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.InstallmentScheduler
import ir.dakhlokharj.domain.model.Installment
import ir.dakhlokharj.domain.model.InstallmentPlan
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

@Composable
fun InstallmentsScreen(
    viewModel: PlanningViewModel,
    onBack: () -> Unit,
) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val installments by viewModel.installments.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = JalaliDate.today()
    val colors = MaterialTheme.colorScheme

    var showAdd by remember { mutableStateOf(false) }
    var openPlanId by remember { mutableStateOf<Long?>(null) }
    var showAmount by remember { mutableStateOf(false) }
    var amountTarget by remember { mutableStateOf(0) }
    var showDate by remember { mutableStateOf(false) }
    var showPaySheet by remember { mutableStateOf<Installment?>(null) }
    var showPayDate by remember { mutableStateOf(false) }
    var showPlanAccountPicker by remember { mutableStateOf(false) }
    var payAccountId by remember { mutableStateOf<Long?>(null) }
    var payDate by remember { mutableStateOf(today) }
    var error by remember { mutableStateOf<String?>(null) }

    var title by remember { mutableStateOf("") }
    var totalToman by remember { mutableStateOf(0L) }
    var downToman by remember { mutableStateOf(0L) }
    var countText by remember { mutableStateOf("12") }
    var firstDue by remember { mutableStateOf(today.plusMonths(1)) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }

    val unpaid = installments.filter { !it.isPaid }.sortedBy { it.dueDate }
    val totalRemaining = Money.sum(unpaid.map { it.amount })
    val overdue = unpaid.filter { it.dueDate < today }
    val thisMonth = unpaid.filter { it.dueDate >= today.startOfMonth && it.dueDate <= today.endOfMonth }
    val planMap = plans.associateBy { it.id }

    DkScreen(
        title = "اقساط",
        subtitle = "برنامه پرداخت و اقساط پیش‌رو",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "طرح اقساط جدید")
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
                DkCard(accent = colors.tertiary) {
                    Text("مانده اقساط", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(moneyText(totalRemaining), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("این ماه", moneyShort(Money.sum(thisMonth.map { it.amount })), Modifier.weight(1f))
                        StatTile("معوق", fa(overdue.size) + " قسط", Modifier.weight(1f), colors.error)
                        StatTile("طرح فعال", fa(plans.size), Modifier.weight(1f))
                    }
                }
            }

            if (unpaid.isNotEmpty()) {
                item { SectionHeader("اقساط پیش‌رو") }
                items(unpaid.take(12), key = { it.id }) { installment ->
                    InstallmentRow(
                        installment = installment,
                        planTitle = planMap[installment.planId]?.title ?: "طرح اقساط",
                    ) {
                        showPaySheet = installment
                        payDate = today
                        if (payAccountId == null) payAccountId = accounts.firstOrNull()?.id
                    }
                }
            }

            item { SectionHeader("طرح‌های اقساط") }
            if (plans.isEmpty()) {
                item {
                    EmptyState(
                        title = "طرح اقساطی ثبت نشده",
                        description = "خرید اقساطی یا وام را ثبت کنید تا سررسید هر قسط را دقیق پیگیری کنید.",
                        icon = Icons.Outlined.CreditCard,
                        actionLabel = "ساخت طرح",
                        onAction = { showAdd = true },
                    )
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    val planInstallments = installments.filter { it.planId == plan.id }
                    val paidCount = planInstallments.count { it.isPaid }
                    DkCard(onClick = { openPlanId = plan.id }, contentPadding = PaddingValues(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${fa(paidCount)} از ${fa(plan.installmentCount)} قسط • هر قسط ${moneyShort(plan.installmentAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Text(
                                faPercent(if (plan.installmentCount > 0) paidCount * 100 / plan.installmentCount else 0),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.primary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        DkProgressBar(
                            progress = if (plan.installmentCount > 0) paidCount.toFloat() / plan.installmentCount else 0f,
                            color = colors.primary,
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        DkBottomSheet(onDismiss = { showAdd = false }) {
            SheetTitle("طرح اقساط جدید", "مبلغ کل، پیش‌پرداخت و تعداد اقساط را وارد کنید")
            DkTextField(title, { title = it }, label = "عنوان", placeholder = "مثلاً خرید لپ‌تاپ")
            Spacer(Modifier.height(10.dp))
            PickerRow("مبلغ کل", if (totalToman > 0) moneyText(Money.toman(totalToman)) else "تعیین مبلغ") {
                amountTarget = 0
                showAmount = true
            }
            Spacer(Modifier.height(10.dp))
            PickerRow("پیش‌پرداخت", moneyText(Money.toman(downToman))) {
                amountTarget = 1
                showAmount = true
            }
            Spacer(Modifier.height(10.dp))
            DkTextField(
                value = countText,
                onValueChange = { countText = it.filter(Char::isDigit).take(3) },
                label = "تعداد اقساط",
                keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(10.dp))
            PickerRow("اولین سررسید", faDate(firstDue, DateStyle.FULL)) { showDate = true }
            Spacer(Modifier.height(10.dp))
            PickerRow(
                "حساب پرداخت",
                accounts.firstOrNull { it.id == accountId }?.name ?: "انتخاب حساب",
            ) { showPlanAccountPicker = true }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { DkChoiceChip("بدون دسته", categoryId == null) { categoryId = null } }
                items(categories.filter { it.kind == ir.dakhlokharj.domain.model.CategoryKind.EXPENSE }) { category ->
                    DkChoiceChip(category.name, categoryId == category.id, color = Color(category.colorArgb)) { categoryId = category.id }
                }
            }
            Spacer(Modifier.height(6.dp))
            val count = countText.toIntOrNull() ?: 0
            if (count > 0 && totalToman > 0) {
                Text(
                    "هر قسط تقریباً ${moneyText(InstallmentScheduler.monthlyAmount(Money.toman(totalToman), Money.toman(downToman), count))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            DkTextField(note, { note = it }, label = "یادداشت (اختیاری)", singleLine = false, maxLines = 2)
            if (error != null) {
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton("ساخت طرح", enabled = accounts.isNotEmpty()) {
                val chosenAccount = accountId ?: accounts.firstOrNull()?.id
                val count = countText.toIntOrNull() ?: 0
                when {
                    title.isBlank() -> error = "عنوان را وارد کنید"
                    totalToman <= 0 -> error = "مبلغ کل را وارد کنید"
                    downToman > totalToman -> error = "پیش‌پرداخت بیشتر از مبلغ کل است"
                    count <= 0 -> error = "تعداد اقساط را وارد کنید"
                    chosenAccount == null -> error = "حساب پرداخت را انتخاب کنید"
                    else -> scope.launch {
                        viewModel.createInstallmentPlan(
                            title = title,
                            totalToman = totalToman,
                            downPaymentToman = downToman,
                            count = count,
                            firstDueDate = firstDue,
                            accountId = chosenAccount,
                            categoryId = categoryId,
                            note = note,
                        ).onSuccess {
                            showAdd = false
                            title = ""
                            totalToman = 0
                            downToman = 0
                            categoryId = null
                            note = ""
                            error = null
                        }.onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showAmount) {
        AmountInputSheet(
            title = if (amountTarget == 0) "مبلغ کل" else "پیش‌پرداخت",
            initialToman = if (amountTarget == 0) totalToman else downToman,
            onDismiss = { showAmount = false },
        ) { value ->
            if (amountTarget == 0) totalToman = value else downToman = value
        }
    }
    if (showDate) {
        DatePickerSheet(initial = firstDue, onDismiss = { showDate = false }) { firstDue = it }
    }
    if (showPayDate) {
        DatePickerSheet(initial = payDate, onDismiss = { showPayDate = false }) { payDate = it }
    }
    if (showPlanAccountPicker) {
        OptionPickerSheet(
            title = "حساب پرداخت",
            options = accounts.map { SheetOption(it.id, it.name, moneyShort(it.balance)) },
            selectedId = accountId,
            onDismiss = { showPlanAccountPicker = false },
            onSelect = { accountId = it },
        )
    }

    val payTarget = showPaySheet
    if (payTarget != null) {
        DkBottomSheet(onDismiss = { showPaySheet = null }) {
            SheetTitle("پرداخت قسط", "قسط ${fa(payTarget.index)} • سررسید ${faDate(payTarget.dueDate, DateStyle.FULL)}")
            Text(
                moneyText(payTarget.amount),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.primary,
            )
            Spacer(Modifier.height(12.dp))
            PickerRow("تاریخ پرداخت", faDate(payDate, DateStyle.FULL)) { showPayDate = true }
            Spacer(Modifier.height(10.dp))
            Text("حساب پرداخت", style = MaterialTheme.typography.labelMedium)
            accounts.forEach { account ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = payAccountId == account.id, onClick = { payAccountId = account.id })
                    Text(account.name, modifier = Modifier.weight(1f))
                }
            }
            if (error != null) {
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ثبت پرداخت", enabled = payAccountId != null) {
                val chosen = payAccountId ?: return@PrimaryButton
                scope.launch {
                    viewModel.payInstallment(payTarget.id, chosen, payDate)
                        .onSuccess {
                            showPaySheet = null
                            error = null
                        }
                        .onFailure { error = it.message }
                }
            }
        }
    }

    val openPlan = planMap[openPlanId]
    if (openPlan != null) {
        val planInstallments = installments.filter { it.planId == openPlan.id }.sortedBy { it.index }
        DkBottomSheet(onDismiss = { openPlanId = null }) {
            SheetTitle(openPlan.title, "مبلغ کل ${moneyText(openPlan.total)} • پیش‌پرداخت ${moneyShort(openPlan.downPayment)}")
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(planInstallments, key = { it.id }) { installment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!installment.isPaid) {
                                    showPaySheet = installment
                                    openPlanId = null
                                    payDate = today
                                    if (payAccountId == null) payAccountId = accounts.firstOrNull()?.id
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(fa(installment.index), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(moneyText(installment.amount), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                faDate(installment.dueDate, DateStyle.SHORT),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        if (installment.isPaid) {
                            Pill("پرداخت‌شده", ir.dakhlokharj.domain.model.InsightTone.POSITIVE)
                        } else {
                            Text("پرداخت", style = MaterialTheme.typography.labelMedium, color = colors.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("حذف طرح", icon = Icons.Outlined.Delete) {
                scope.launch {
                    viewModel.deletePlan(openPlan)
                        .onSuccess { openPlanId = null }
                        .onFailure { error = it.message }
                }
            }
        }
    }
}