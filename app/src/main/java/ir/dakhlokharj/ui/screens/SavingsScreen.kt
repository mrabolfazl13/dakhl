package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Savings
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
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.DateStyle
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.DateStyle
import ir.dakhlokharj.ui.theme.faDate
import ir.dakhlokharj.ui.theme.moneyShort
import ir.dakhlokharj.ui.theme.moneyText
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

private val goalColors = listOf(
    0xFF34D399.toInt(), 0xFF60A5FA.toInt(), 0xFFE9C46A.toInt(),
    0xFFF472B6.toInt(), 0xFF818CF8.toInt(), 0xFFF97316.toInt(),
)

@Composable
fun SavingsScreen(
    viewModel: PlanningViewModel,
    onBack: () -> Unit,
    onOpenGoal: (Long) -> Unit,
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var showAdd by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var targetToman by remember { mutableStateOf(0L) }
    var targetDate by remember { mutableStateOf(JalaliDate.today().plusMonths(6)) }
    var color by remember { mutableStateOf(goalColors.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    val totalSaved = Money.sum(goals.map { it.saved })
    val totalTarget = Money.sum(goals.map { it.target })

    DkScreen(
        title = "پس‌انداز",
        subtitle = "اهداف مالی و مسیر رسیدن به آن‌ها",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "هدف جدید")
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
                DkCard(accent = colors.primary) {
                    Text("جمع پس‌انداز", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(moneyText(totalSaved), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(10.dp))
                    DkProgressBar(
                        progress = if (totalTarget.rial > 0) totalSaved.rial.toFloat() / totalTarget.rial else 0f,
                        color = colors.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "از هدف کل ${moneyShort(totalTarget)} • ${fa(goals.count { it.achieved })} هدف محقق‌شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            item { SectionHeader("اهداف من", actionLabel = "هدف جدید") { showAdd = true } }
            if (goals.isEmpty()) {
                item {
                    EmptyState(
                        title = "هنوز هدفی ندارید",
                        description = "برای سفر، خانه یا صندوق اضطراری هدف بسازید و پیشرفت را ببینید.",
                        icon = Icons.Outlined.Savings,
                        actionLabel = "ساخت هدف",
                        onAction = { showAdd = true },
                    )
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    SavingsGoalCard(goal) { onOpenGoal(goal.id) }
                }
            }
        }
    }

    if (showAdd) {
        DkBottomSheet(onDismiss = { showAdd = false }) {
            SheetTitle("هدف پس‌انداز جدید", "مبلغ هدف و افق زمانی را مشخص کنید")
            DkTextField(title, { title = it }, label = "عنوان هدف", placeholder = "مثلاً سفر به شمال")
            Spacer(Modifier.height(10.dp))
            PickerRow("مبلغ هدف", if (targetToman > 0) moneyText(Money.toman(targetToman)) else "تعیین مبلغ") { showAmount = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("تاریخ هدف (اختیاری)", faDate(targetDate, DateStyle.FULL)) { showDate = true }
            Spacer(Modifier.height(12.dp))
            Text("رنگ هدف", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                goalColors.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .clickable { color = argb },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (color == argb) Box(Modifier.size(9.dp).clip(CircleShape).background(Color.White))
                    }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton("ساخت هدف") {
                if (title.isBlank()) error = "عنوان هدف را وارد کنید"
                else if (targetToman <= 0) error = "مبلغ هدف را وارد کنید"
                else {
                    scope.launch {
                        viewModel.saveGoal(
                            SavingsGoal(
                                title = title,
                                target = Money.toman(targetToman),
                                saved = Money.ZERO,
                                targetDate = targetDate,
                                colorArgb = color,
                                iconKey = "goal",
                            ),
                            editing = false,
                        ).onSuccess {
                            showAdd = false
                            title = ""
                            targetToman = 0
                            error = null
                        }.onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showAmount) {
        AmountInputSheet("مبلغ هدف", targetToman, { showAmount = false }) { targetToman = it }
    }
    if (showDate) {
        DatePickerSheet(initial = targetDate, onDismiss = { showDate = false }) { targetDate = it }
    }
}