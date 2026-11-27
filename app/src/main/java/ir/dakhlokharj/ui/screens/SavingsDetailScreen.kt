package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

private val goalPalette = listOf(
    0xFF34D399.toInt(), 0xFF60A5FA.toInt(), 0xFFE9C46A.toInt(),
    0xFFF472B6.toInt(), 0xFF818CF8.toInt(), 0xFFF97316.toInt(),
)

@Composable
fun SavingsDetailScreen(
    viewModel: PlanningViewModel,
    goalId: Long,
    onBack: () -> Unit,
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val contributions by viewModel.contributions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val goal = goals.firstOrNull { it.id == goalId }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var showDeposit by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showEditAmount by remember { mutableStateOf(false) }
    var showEditDate by remember { mutableStateOf(false) }
    var amountToman by remember { mutableStateOf(0L) }
    var date by remember { mutableStateOf(JalaliDate.today()) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    var editTitle by remember { mutableStateOf("") }
    var editTargetToman by remember { mutableStateOf(0L) }
    var editTargetDate by remember { mutableStateOf(JalaliDate.today().plusMonths(6)) }
    var editColor by remember { mutableStateOf(goalPalette.first()) }

    LaunchedEffect(goal) {
        if (goal != null) {
            editTitle = goal.title
            editTargetToman = goal.target.toTomanRounded()
            editTargetDate = goal.targetDate ?: JalaliDate.today().plusMonths(6)
            editColor = goal.colorArgb
        }
    }

    val history = contributions.filter { it.goalId == goalId }.sortedByDescending { it.date }

    DkScreen(
        title = goal?.title ?: "هدف پس‌انداز",
        subtitle = "پیشرفت هدف و تاریخچه واریز",
        onBack = onBack,
        actions = {
            if (goal != null) {
                IconButton(onClick = { showEdit = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "ویرایش")
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "حذف")
                }
            }
        },
    ) { padding ->
        if (goal == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هدف یافت نشد", color = colors.onSurfaceVariant)
            }
            return@DkScreen
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            DkCard(accent = Color(goal.colorArgb)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("پس‌انداز شده", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(moneyText(goal.saved), style = MaterialTheme.typography.displaySmall)
                    }
                    Text(faPercent((goal.progress * 100).toInt()), style = MaterialTheme.typography.headlineMedium, color = Color(goal.colorArgb))
                }
                Spacer(Modifier.height(12.dp))
                DkProgressBar(goal.progress, color = Color(goal.colorArgb))
                Spacer(Modifier.height(8.dp))
                Text(
                    "هدف: ${moneyText(goal.target)} • باقی‌مانده: ${moneyText(goal.remaining)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                if (goal.targetDate != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "مهلت: ${faDate(goal.targetDate, DateStyle.FULL)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("واریز", Modifier.weight(1f), icon = Icons.Outlined.Add) {
                    amountToman = 0
                    accountId = accounts.firstOrNull()?.id
                    showDeposit = true
                }
                SecondaryButton("برداشت", Modifier.weight(1f), icon = Icons.Outlined.Remove) {
                    amountToman = 0
                    accountId = accounts.firstOrNull()?.id
                    showWithdraw = true
                }
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader("تاریخچه")
            if (history.isEmpty()) {
                Text(
                    "هنوز واریزی ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                history.forEach { contribution ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (contribution.amount.isPositive) colors.primary.copy(alpha = 0.12f)
                                    else colors.error.copy(alpha = 0.12f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (contribution.amount.isPositive) Icons.Outlined.Add else Icons.Outlined.Remove,
                                contentDescription = null,
                                tint = if (contribution.amount.isPositive) colors.primary else colors.error,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (contribution.amount.isPositive) "واریز" else "برداشت",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                faDate(contribution.date, DateStyle.LONG),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Text(
                            moneyText(Money(kotlin.math.abs(contribution.amount.rial))),
                            color = if (contribution.amount.isPositive) colors.primary else colors.error,
                        )
                    }
                }
            }
        }
    }

    val isDeposit = showDeposit
    if (showDeposit || showWithdraw) {
        DkBottomSheet(onDismiss = { showDeposit = false; showWithdraw = false }) {
            SheetTitle(if (isDeposit) "واریز به هدف" else "برداشت از هدف", "در صورت انتخاب حساب، تراکنش واقعی ثبت می‌شود")
            PickerRow("مبلغ", if (amountToman > 0) moneyText(Money.toman(amountToman)) else "تعیین مبلغ") { showAmount = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("تاریخ", faDate(date, DateStyle.FULL)) { showDate = true }
            Spacer(Modifier.height(10.dp))
            PickerRow(
                "حساب (اختیاری)",
                if (accountId == null) "بدون اثر روی حساب" else accounts.firstOrNull { it.id == accountId }?.name ?: "بدون اثر روی حساب",
            ) { showAccountPicker = true }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(if (isDeposit) "ثبت واریز" else "ثبت برداشت", enabled = amountToman > 0) {
                scope.launch {
                    val action = if (isDeposit) {
                        viewModel.contribute(goal.id, amountToman, date, accountId)
                    } else {
                        viewModel.withdraw(goal.id, amountToman, date, accountId)
                    }
                    action.onSuccess {
                        showDeposit = false
                        showWithdraw = false
                        error = null
                    }.onFailure { error = it.message }
                }
            }
        }
    }

    if (showAmount) {
        AmountInputSheet("مبلغ", amountToman, { showAmount = false }) { amountToman = it }
    }
    if (showEditAmount) {
        AmountInputSheet("مبلغ هدف", editTargetToman, { showEditAmount = false }) { editTargetToman = it }
    }
    if (showEditDate) {
        DatePickerSheet(initial = editTargetDate, onDismiss = { showEditDate = false }) { editTargetDate = it }
    }
    if (showDate) {
        DatePickerSheet(initial = date, onDismiss = { showDate = false }) { date = it }
    }
    if (showAccountPicker) {
        OptionPickerSheet(
            title = "حساب",
            options = listOf(SheetOption(0L, "بدون اثر روی حساب", "فقط موجودی هدف تغییر می‌کند")) +
                accounts.map { SheetOption(it.id, it.name, moneyShort(it.balance)) },
            selectedId = accountId,
            onDismiss = { showAccountPicker = false },
            onSelect = { accountId = if (it == 0L) null else it },
        )
    }

    if (showEdit && goal != null) {
        DkBottomSheet(onDismiss = { showEdit = false }) {
            SheetTitle("ویرایش هدف", "عنوان، مبلغ و مهلت را تغییر دهید")
            DkTextField(editTitle, { editTitle = it }, label = "عنوان هدف")
            Spacer(Modifier.height(10.dp))
            PickerRow("مبلغ هدف", moneyText(Money.toman(editTargetToman))) { showEditAmount = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("مهلت", faDate(editTargetDate, DateStyle.FULL)) { showEditDate = true }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                goalPalette.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .clickable { editColor = argb },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (editColor == argb) Box(Modifier.size(9.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton("ذخیره") {
                scope.launch {
                    viewModel.saveGoal(
                        goal.copy(
                            title = editTitle,
                            target = Money.toman(editTargetToman),
                            targetDate = editTargetDate,
                            colorArgb = editColor,
                        ),
                        editing = true,
                    ).onSuccess { showEdit = false }
                        .onFailure { error = it.message }
                }
            }
        }
    }

    if (showDelete && goal != null) {
        ConfirmDialog(
            title = "حذف هدف؟",
            message = "این هدف و تاریخچه آن حذف می‌شود؛ تراکنش‌های واریز در حساب باقی می‌مانند.",
            confirmLabel = "حذف",
            danger = true,
            onConfirm = {
                scope.launch {
                    viewModel.deleteGoal(goal).onSuccess { onBack() }
                        .onFailure { error = it.message }
                }
            },
            onDismiss = { showDelete = false },
        )
    }
}