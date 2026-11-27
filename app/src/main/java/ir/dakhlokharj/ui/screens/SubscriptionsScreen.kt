package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Subscriptions
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
import ir.dakhlokharj.domain.model.RecurringExpense
import ir.dakhlokharj.domain.model.RecurringKind
import ir.dakhlokharj.domain.model.RecurringPeriod
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.recurringKindLabel
import ir.dakhlokharj.ui.recurringPeriodLabel
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

@Composable
fun SubscriptionsScreen(
    viewModel: PlanningViewModel,
    onBack: () -> Unit,
) {
    val recurring by viewModel.recurring.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val today = JalaliDate.today()
    val colors = MaterialTheme.colorScheme

    var editing by remember { mutableStateOf<RecurringExpense?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showPaySheet by remember { mutableStateOf<RecurringExpense?>(null) }
    var payAccountId by remember { mutableStateOf<Long?>(null) }
    var payDate by remember { mutableStateOf(today) }
    var showPayDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var title by remember { mutableStateOf("") }
    var amountToman by remember { mutableStateOf(0L) }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var period by remember { mutableStateOf(RecurringPeriod.MONTHLY) }
    var nextDue by remember { mutableStateOf(today.plusDays(7)) }
    var kind by remember { mutableStateOf(RecurringKind.SUBSCRIPTION) }
    var reminderDays by remember { mutableStateOf("۲") }
    var note by remember { mutableStateOf("") }

    val active = recurring.filter { it.active }
    val monthlyBurden = Money.sum(
        active.map {
            when (it.period) {
                RecurringPeriod.WEEKLY -> Money(it.amount.rial * 4)
                RecurringPeriod.MONTHLY -> it.amount
                RecurringPeriod.YEARLY -> Money(it.amount.rial / 12)
            }
        },
    )
    val upcoming = recurring.filter { it.active && it.nextDueDate <= today.plusDays(7) }.sortedBy { it.nextDueDate }
    val categoryMap = categories.associateBy { it.id }

    fun openForm(item: RecurringExpense?) {
        editing = item
        title = item?.title ?: ""
        amountToman = item?.amount?.toTomanRounded() ?: 0L
        categoryId = item?.categoryId
        accountId = item?.accountId ?: accounts.firstOrNull()?.id
        period = item?.period ?: RecurringPeriod.MONTHLY
        nextDue = item?.nextDueDate ?: today.plusDays(7)
        kind = item?.kind ?: RecurringKind.SUBSCRIPTION
        reminderDays = (item?.reminderDaysBefore ?: 2).toString()
        note = item?.note ?: ""
        error = null
        showForm = true
    }

    DkScreen(
        title = "اشتراک و قبض",
        subtitle = "هزینه‌های تکرارشونده و یادآوری سررسید",
        onBack = onBack,
        actions = {
            IconButton(onClick = { openForm(null) }) {
                Icon(Icons.Outlined.Add, contentDescription = "افزودن")
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
                DkCard(accent = colors.secondary) {
                    Text("بار ماهانه تقریبی", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(moneyText(monthlyBurden), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${fa(active.size)} مورد فعال • ${fa(upcoming.size)} سررسید در هفت روز آینده",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                item { SectionHeader("یادآوری‌های نزدیک") }
                items(upcoming, key = { "up-${it.id}" }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openForm(item) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = colors.secondary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "سررسید ${faDate(item.nextDueDate, DateStyle.SHORT)} • ${recurringKindLabel(item.kind)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Text(moneyShort(item.amount), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item { SectionHeader("همه موارد", actionLabel = "مورد جدید") { openForm(null) } }
            if (recurring.isEmpty()) {
                item {
                    EmptyState(
                        title = "هزینه تکرارشونده ندارید",
                        description = "قبض‌ها و اشتراک‌ها را ثبت کنید تا سررسیدشان را فراموش نکنید.",
                        icon = Icons.Outlined.Subscriptions,
                        actionLabel = "افزودن مورد",
                        onAction = { openForm(null) },
                    )
                }
            } else {
                items(recurring, key = { it.id }) { item ->
                    DkCard(contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${recurringPeriodLabel(item.period)} • سررسید ${faDate(item.nextDueDate, DateStyle.SHORT)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(moneyText(item.amount), style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = item.active,
                                    onCheckedChange = { checked ->
                                        scope.launch { viewModel.toggleRecurring(item, checked) }
                                    },
                                )
                            }
                        }
                        if (item.active) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    showPaySheet = item
                                    payDate = today
                                    if (payAccountId == null) payAccountId = accounts.firstOrNull()?.id
                                }) {
                                    Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("پرداخت")
                                }
                                TextButton(onClick = { openForm(item) }) {
                                    Text("ویرایش")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        val options = listOf(RecurringKind.SUBSCRIPTION, RecurringKind.BILL)
        DkBottomSheet(onDismiss = { showForm = false }) {
            SheetTitle(if (editing == null) "مورد جدید" else "ویرایش مورد", "قبض، اشتراک یا هزینه تکرارشونده")
            DkTextField(title, { title = it }, label = "عنوان", placeholder = "مثلاً اشتراک اینترنت")
            Spacer(Modifier.height(10.dp))
            PickerRow("مبلغ", if (amountToman > 0) moneyText(Money.toman(amountToman)) else "تعیین مبلغ") { showAmount = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("دسته‌بندی", categoryMap[categoryId]?.name ?: "بدون دسته") { showCategoryPicker = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("حساب", accounts.firstOrNull { it.id == accountId }?.name ?: "انتخاب حساب") { showAccountPicker = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("سررسید بعدی", faDate(nextDue, DateStyle.FULL)) { showDate = true }
            Spacer(Modifier.height(10.dp))
            Text("دوره تکرار", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RecurringPeriod.entries.toList()) { entry ->
                    DkChoiceChip(recurringPeriodLabel(entry), period == entry) { period = entry }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("نوع", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { entry ->
                    DkChoiceChip(recurringKindLabel(entry), kind == entry) { kind = entry }
                }
            }
            Spacer(Modifier.height(10.dp))
            DkTextField(
                reminderDays,
                { reminderDays = it.filter(Char::isDigit).take(2) },
                label = "یادآوری چند روز قبل",
                keyboardType = KeyboardType.Number,
            )
            Spacer(Modifier.height(6.dp))
            DkTextField(note, { note = it }, label = "یادداشت (اختیاری)", singleLine = false, maxLines = 2)
            if (error != null) {
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(if (editing == null) "افزودن" else "ذخیره تغییرات") {
                val chosenAccount = accountId ?: accounts.firstOrNull()?.id
                when {
                    title.isBlank() -> error = "عنوان را وارد کنید"
                    amountToman <= 0 -> error = "مبلغ را وارد کنید"
                    chosenAccount == null -> error = "حساب را انتخاب کنید"
                    else -> {
                        val item = RecurringExpense(
                            id = editing?.id ?: 0,
                            title = title,
                            amount = Money.toman(amountToman),
                            categoryId = categoryId,
                            accountId = chosenAccount,
                            period = period,
                            nextDueDate = nextDue,
                            kind = kind,
                            active = editing?.active ?: true,
                            reminderEnabled = true,
                            reminderDaysBefore = reminderDays.toIntOrNull() ?: 2,
                            note = note,
                            lastPaidDate = editing?.lastPaidDate,
                            createdAt = editing?.createdAt ?: 0,
                        )
                        scope.launch {
                            viewModel.saveRecurring(item, editing != null)
                                .onSuccess { showForm = false }
                                .onFailure { error = it.message }
                        }
                    }
                }
            }
            if (editing != null) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton("حذف", icon = Icons.Outlined.Delete) {
                    scope.launch {
                        viewModel.deleteRecurring(editing!!)
                            .onSuccess { showForm = false }
                            .onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showAmount) {
        AmountInputSheet("مبلغ", amountToman, { showAmount = false }) { amountToman = it }
    }
    if (showDate) {
        DatePickerSheet(initial = nextDue, onDismiss = { showDate = false }) { nextDue = it }
    }
    if (showPayDate) {
        DatePickerSheet(initial = payDate, onDismiss = { showPayDate = false }) { payDate = it }
    }
    if (showAccountPicker) {
        OptionPickerSheet(
            title = "حساب",
            options = accounts.map { SheetOption(it.id, it.name, moneyShort(it.balance)) },
            selectedId = accountId,
            onDismiss = { showAccountPicker = false },
            onSelect = { accountId = it },
        )
    }
    if (showCategoryPicker) {
        OptionPickerSheet(
            title = "دسته‌بندی",
            options = listOf(SheetOption(0L, "بدون دسته")) + categories.filter {
                it.kind == ir.dakhlokharj.domain.model.CategoryKind.EXPENSE
            }.map { SheetOption(it.id, it.name, color = Color(it.colorArgb)) },
            selectedId = categoryId,
            onDismiss = { showCategoryPicker = false },
            onSelect = { categoryId = if (it == 0L) null else it },
        )
    }

    val payItem = showPaySheet
    if (payItem != null) {
        DkBottomSheet(onDismiss = { showPaySheet = null }) {
            SheetTitle("پرداخت ${payItem.title}", "هزینه واقعی در حساب ثبت می‌شود و سررسید جلو می‌رود")
            Text(moneyText(payItem.amount), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            PickerRow("تاریخ", faDate(payDate, DateStyle.FULL)) { showPayDate = true }
            Spacer(Modifier.height(10.dp))
            Text("حساب پرداخت", style = MaterialTheme.typography.labelMedium)
            accounts.forEach { account ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = payAccountId == account.id, onClick = { payAccountId = account.id })
                    Text(account.name, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ثبت پرداخت", enabled = payAccountId != null) {
                val chosen = payAccountId ?: return@PrimaryButton
                scope.launch {
                    viewModel.payRecurring(payItem, chosen, payDate)
                        .onSuccess { showPaySheet = null }
                        .onFailure { error = it.message }
                }
            }
        }
    }
}