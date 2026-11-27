package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.debtStatusLabel
import ir.dakhlokharj.ui.debtStatusOf
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

@Composable
fun DebtDetailScreen(
    viewModel: PlanningViewModel,
    debtId: Long,
    onBack: () -> Unit,
) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val payments by viewModel.debtPayments.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val debt = debts.firstOrNull { it.id == debtId }
    val scope = rememberCoroutineScope()
    var showPay by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var amountToman by remember { mutableStateOf(0L) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf(JalaliDate.today()) }
    var showAmount by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(debt) {
        if (debt != null) amountToman = debt.remaining.toTomanRounded()
        if (accountId == null) accountId = accounts.firstOrNull()?.id
    }
    val debtPayments = payments.filter { it.debtId == debtId }.sortedByDescending { it.date }

    DkScreen(
        title = debt?.creditor ?: "بدهی",
        subtitle = debt?.let { debtStatusLabel(debtStatusOf(it)) },
        onBack = onBack,
        actions = {
            if (debt != null) {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "حذف")
                }
            }
        },
    ) { padding ->
        if (debt == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("بدهی یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@DkScreen
        }
        val progress = if (debt.principal.rial > 0) debt.paid.rial.toFloat() / debt.principal.rial else 0f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            DkCard(accent = MaterialTheme.colorScheme.error) {
                Text("مانده بدهی", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(moneyText(debt.remaining), style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(12.dp))
                DkProgressBar(progress, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${moneyShort(debt.paid)} از ${moneyShort(debt.principal)} پرداخت شده",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            DkCard {
                InfoRow("طلبکار", debt.creditor)
                InfoRow("سررسید", faDate(debt.dueDate, DateStyle.FULL))
                InfoRow("تعداد اقساط", fa(debt.installmentCount))
                InfoRow("وضعیت", debtStatusLabel(debtStatusOf(debt)))
                if (debt.note.isNotBlank()) {
                    DkTopDivider()
                    Text(debt.note, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = "پرداخت بدهی",
                icon = Icons.Outlined.Payments,
                enabled = debt.remaining.isPositive,
            ) { showPay = true }

            Spacer(Modifier.height(16.dp))
            SectionHeader("تاریخچه پرداخت‌ها")
            if (debtPayments.isEmpty()) {
                Text(
                    "هنوز پرداختی ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                debtPayments.forEach { payment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(moneyText(payment.amount), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                faDate(payment.date, DateStyle.LONG),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showPay && debt != null) {
        DkBottomSheet(onDismiss = { showPay = false }) {
            SheetTitle("پرداخت بدهی", "پرداخت به‌صورت هزینه واقعی در حساب ثبت می‌شود")
            PickerRow("مبلغ پرداخت", moneyText(Money.toman(amountToman))) { showAmount = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("تاریخ", faDate(date, DateStyle.FULL)) { showDate = true }
            Spacer(Modifier.height(10.dp))
            Text("حساب پرداخت", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                items(accounts, key = { it.id }) { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = accountId == account.id, onClick = { accountId = account.id })
                        Text(account.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(moneyShort(account.balance), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ثبت پرداخت", enabled = amountToman > 0 && accountId != null) {
                val chosenAccount = accountId ?: return@PrimaryButton
                scope.launch {
                    viewModel.payDebt(debt.id, amountToman, date, chosenAccount)
                        .onSuccess {
                            showPay = false
                            error = null
                        }
                        .onFailure { error = it.message }
                }
            }
        }
    }

    if (showAmount) {
        AmountInputSheet("مبلغ پرداخت", amountToman, { showAmount = false }) { amountToman = it }
    }
    if (showDate) {
        DatePickerSheet(initial = date, onDismiss = { showDate = false }) { date = it }
    }
    if (showDelete && debt != null) {
        ConfirmDialog(
            title = "حذف بدهی؟",
            message = "سابقه پرداخت‌ها حذف می‌شود اما تراکنش‌های هزینه دست‌نخورده می‌مانند.",
            confirmLabel = "حذف",
            danger = true,
            onConfirm = {
                scope.launch {
                    viewModel.deleteDebt(debt).onSuccess { onBack() }
                        .onFailure { error = it.message }
                }
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
fun ReceivableDetailScreen(
    viewModel: PlanningViewModel,
    receivableId: Long,
    onBack: () -> Unit,
) {
    val receivables by viewModel.receivables.collectAsStateWithLifecycle()
    val payments by viewModel.receivablePayments.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val receivable = receivables.firstOrNull { it.id == receivableId }
    val scope = rememberCoroutineScope()
    var showPay by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var amountToman by remember { mutableStateOf(0L) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf(JalaliDate.today()) }
    var showAmount by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(receivable) {
        if (receivable != null) amountToman = receivable.remaining.toTomanRounded()
        if (accountId == null) accountId = accounts.firstOrNull()?.id
    }
    val history = payments.filter { it.receivableId == receivableId }.sortedByDescending { it.date }

    DkScreen(
        title = receivable?.debtor ?: "طلب",
        subtitle = "پیگیری بازپرداخت",
        onBack = onBack,
        actions = {
            if (receivable != null) {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "حذف")
                }
            }
        },
    ) { padding ->
        if (receivable == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("طلب یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@DkScreen
        }
        val progress = if (receivable.amount.rial > 0) receivable.received.rial.toFloat() / receivable.amount.rial else 0f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            DkCard(accent = MaterialTheme.colorScheme.primary) {
                Text("مانده طلب", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(moneyText(receivable.remaining), style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(12.dp))
                DkProgressBar(progress, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${moneyShort(receivable.received)} دریافت شده از ${moneyShort(receivable.amount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            DkCard {
                InfoRow("بدهکار", receivable.debtor)
                InfoRow("تاریخ بازپرداخت", faDate(receivable.expectedDate, DateStyle.FULL))
                if (receivable.note.isNotBlank()) {
                    DkTopDivider()
                    Text(receivable.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ثبت دریافت", enabled = receivable.remaining.isPositive) { showPay = true }
            Spacer(Modifier.height(16.dp))
            SectionHeader("تاریخچه دریافت‌ها")
            if (history.isEmpty()) {
                Text(
                    "هنوز دریافتی ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                history.forEach { payment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(moneyText(payment.amount), modifier = Modifier.weight(1f))
                        Text(
                            faDate(payment.date, DateStyle.SHORT),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showPay && receivable != null) {
        DkBottomSheet(onDismiss = { showPay = false }) {
            SheetTitle("ثبت دریافت", "دریافت به‌صورت درآمد واقعی در حساب ثبت می‌شود")
            PickerRow("مبلغ دریافت", moneyText(Money.toman(amountToman))) { showAmount = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("تاریخ", faDate(date, DateStyle.FULL)) { showDate = true }
            Spacer(Modifier.height(10.dp))
            Text("حساب دریافت", style = MaterialTheme.typography.labelMedium)
            accounts.forEach { account ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = accountId == account.id, onClick = { accountId = account.id })
                    Text(account.name, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ثبت دریافت", enabled = amountToman > 0 && accountId != null) {
                val chosenAccount = accountId ?: return@PrimaryButton
                scope.launch {
                    viewModel.receiveReceivable(receivable.id, amountToman, date, chosenAccount)
                        .onSuccess {
                            showPay = false
                            error = null
                        }
                        .onFailure { error = it.message }
                }
            }
        }
    }
    if (showAmount) {
        AmountInputSheet("مبلغ دریافت", amountToman, { showAmount = false }) { amountToman = it }
    }
    if (showDate) {
        DatePickerSheet(initial = date, onDismiss = { showDate = false }) { date = it }
    }
    if (showDelete && receivable != null) {
        ConfirmDialog(
            title = "حذف طلب؟",
            message = "این طلب از فهرست حذف می‌شود؛ تراکنش‌های دریافتی حفظ می‌شوند.",
            confirmLabel = "حذف",
            danger = true,
            onConfirm = {
                scope.launch {
                    viewModel.deleteReceivable(receivable).onSuccess { onBack() }
                }
            },
            onDismiss = { showDelete = false },
        )
    }
}
