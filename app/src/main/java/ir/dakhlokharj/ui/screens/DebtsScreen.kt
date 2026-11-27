package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.Debt
import ir.dakhlokharj.domain.model.Receivable
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.debtStatusLabel
import ir.dakhlokharj.ui.debtStatusOf
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

@Composable
fun DebtsScreen(
    viewModel: PlanningViewModel,
    onBack: () -> Unit,
    onOpenDebt: (Long) -> Unit,
    onOpenReceivable: (Long) -> Unit,
) {
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val receivables by viewModel.receivables.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var amountToman by remember { mutableStateOf(0L) }
    var dueDate by remember { mutableStateOf(JalaliDate.today().plusDays(30)) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val remainingDebt = Money.sum(debts.map { it.remaining })
    val remainingReceivable = Money.sum(receivables.map { it.remaining })

    DkScreen(
        title = "بدهی و طلب",
        subtitle = "تعهدات و مطالبات خود را دقیق پیگیری کنید",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAdd = true }) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DkCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp)) {
                        Text("بدهی من", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        Text(moneyShort(remainingDebt), style = MaterialTheme.typography.titleMedium)
                    }
                    DkCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp)) {
                        Text("طلب من", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text(moneyShort(remainingReceivable), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            item {
                SegmentedControl(listOf("بدهی‌ها", "طلب‌ها"), tab) { tab = it }
            }

            if (tab == 0) {
                if (debts.isEmpty()) {
                    item {
                        EmptyState(
                            title = "بدهی ثبت نشده",
                            description = "بدهی‌ها را ثبت کنید تا سررسید و مانده هرکدام را دقیق بدانید.",
                            icon = Icons.Outlined.ArrowUpward,
                            actionLabel = "ثبت بدهی",
                            onAction = { showAdd = true },
                        )
                    }
                } else {
                    items(debts, key = { it.id }) { debt ->
                        DebtCard(debt) { onOpenDebt(debt.id) }
                    }
                }
            } else {
                if (receivables.isEmpty()) {
                    item {
                        EmptyState(
                            title = "طلبی ثبت نشده",
                            description = "پولی که به دیگران قرض داده‌اید را ثبت کنید تا فراموش نشود.",
                            icon = Icons.Outlined.ArrowDownward,
                            actionLabel = "ثبت طلب",
                            onAction = { showAdd = true },
                        )
                    }
                } else {
                    items(receivables, key = { it.id }) { receivable ->
                        ReceivableCard(receivable) { onOpenReceivable(receivable.id) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        DkBottomSheet(onDismiss = { showAdd = false }) {
            SheetTitle(if (tab == 0) "بدهی جدید" else "طلب جدید", "جزئیات را وارد کنید؛ همه‌چیز آفلاین می‌ماند")
            DkTextField(
                name,
                { name = it },
                label = if (tab == 0) "طلبکار" else "بدهکار",
                placeholder = "نام شخص یا مؤسسه",
            )
            Spacer(Modifier.height(10.dp))
            PickerRow("مبلغ", if (amountToman > 0) moneyText(Money.toman(amountToman)) else "تعیین مبلغ") { showAmount = true }
            Spacer(Modifier.height(10.dp))
            PickerRow(
                if (tab == 0) "سررسید" else "تاریخ بازپرداخت",
                faDate(dueDate, DateStyle.FULL),
            ) { showDate = true }
            Spacer(Modifier.height(10.dp))
            DkTextField(note, { note = it }, label = "یادداشت (اختیاری)", singleLine = false, maxLines = 3)
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(if (tab == 0) "ثبت بدهی" else "ثبت طلب") {
                if (name.isBlank()) error = "نام را وارد کنید"
                else if (amountToman <= 0) error = "مبلغ را وارد کنید"
                else {
                    scope.launch {
                        if (tab == 0) {
                            viewModel.saveDebt(
                                Debt(
                                    creditor = name,
                                    principal = Money.toman(amountToman),
                                    paid = Money.ZERO,
                                    dueDate = dueDate,
                                    note = note,
                                ),
                                editing = false,
                            )
                        } else {
                            viewModel.saveReceivable(
                                Receivable(
                                    debtor = name,
                                    amount = Money.toman(amountToman),
                                    received = Money.ZERO,
                                    expectedDate = dueDate,
                                    note = note,
                                ),
                                editing = false,
                            )
                        }.onSuccess {
                            showAdd = false
                            name = ""
                            amountToman = 0
                            note = ""
                            error = null
                        }.onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showAmount) {
        AmountInputSheet("مبلغ", amountToman, { showAmount = false }) { amountToman = it }
    }
    if (showDate) {
        DatePickerSheet(initial = dueDate, onDismiss = { showDate = false }) { dueDate = it }
    }
}

@Composable
private fun ReceivableCard(receivable: Receivable, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val progress = if (receivable.amount.rial > 0) receivable.received.rial.toFloat() / receivable.amount.rial else 0f
    DkCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.ArrowDownward, contentDescription = null, tint = colors.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(receivable.debtor, style = MaterialTheme.typography.titleMedium)
                Text(
                    "دریافت‌شده ${moneyShort(receivable.received)} • سررسید ${faDate(receivable.expectedDate, DateStyle.SHORT)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(moneyText(receivable.remaining), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        DkProgressBar(progress = progress, color = colors.primary)
    }
}
