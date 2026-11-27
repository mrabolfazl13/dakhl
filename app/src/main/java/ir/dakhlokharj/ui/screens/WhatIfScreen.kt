package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.ui.budgetStatusLabel
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel

@Composable
fun WhatIfScreen(viewModel: OverviewViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var title by remember { mutableStateOf("") }
    var priceToman by remember { mutableStateOf(0L) }
    var payCash by remember { mutableStateOf(true) }
    var downToman by remember { mutableStateOf(0L) }
    var countText by remember { mutableStateOf("12") }
    var showPrice by remember { mutableStateOf(false) }
    var showDown by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val result = state.whatIf

    DkScreen(
        title = "مشاور خرید",
        subtitle = "قبل از خرید، اثرش را روی آینده مالی ببینید",
        onBack = onBack,
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
                DkCard {
                    Text("سناریوی خرید", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    DkTextField(title, { title = it }, label = "چه چیزی می‌خواهید بخرید؟", placeholder = "مثلاً لپ‌تاپ جدید")
                    Spacer(Modifier.height(10.dp))
                    PickerRow("قیمت", if (priceToman > 0) moneyText(Money.toman(priceToman)) else "تعیین قیمت") { showPrice = true }
                    Spacer(Modifier.height(10.dp))
                    SegmentedControl(listOf("نقدی", "اقساطی"), if (payCash) 0 else 1) { payCash = it == 0 }
                    if (!payCash) {
                        Spacer(Modifier.height(10.dp))
                        PickerRow("پیش‌پرداخت", moneyText(Money.toman(downToman))) { showDown = true }
                        Spacer(Modifier.height(10.dp))
                        DkTextField(
                            countText,
                            { countText = it.filter(Char::isDigit).take(3) },
                            label = "تعداد اقساط",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("شبیه‌سازی", icon = Icons.Outlined.Calculate) {
                        when {
                            priceToman <= 0 -> error = "قیمت را وارد کنید"
                            !payCash && downToman > priceToman -> error = "پیش‌پرداخت بیشتر از قیمت است"
                            !payCash && (countText.toIntOrNull() ?: 0) <= 0 -> error = "تعداد اقساط را وارد کنید"
                            else -> {
                                error = null
                                viewModel.simulate(
                                    title = title,
                                    priceToman = priceToman,
                                    payCash = payCash,
                                    downPaymentToman = downToman,
                                    installmentCount = countText.toIntOrNull() ?: 0,
                                )
                            }
                        }
                    }
                }
            }

            if (result != null) {
                item {
                    DkCard(accent = if (result.isAffordable) colors.primary else colors.error) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (result.isAffordable) "این خرید با برنامه شما سازگار است" else "این خرید برنامه مالی را به هم می‌زند",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (result.isAffordable) colors.primary else colors.error,
                                )
                                Text(
                                    "موجودی بعد از خرید: ${moneyText(result.balanceAfterPurchase)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Pill(
                                if (result.isAffordable) "قابل انجام" else "پرخطر",
                                if (result.isAffordable) ir.dakhlokharj.domain.model.InsightTone.POSITIVE
                                else ir.dakhlokharj.domain.model.InsightTone.NEGATIVE,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        InfoRow("پرداخت اولیه", moneyText(result.upfrontCost))
                        if (!result.scenario.cash) {
                            InfoRow("قسط ماهانه", moneyText(result.monthlyInstallment))
                            InfoRow("تعداد اقساط", fa(result.scenario.installmentCount))
                        }
                        InfoRow("تعهد کل", moneyText(result.totalCommitment))
                        InfoRow("موجودی پایان ماه پس از خرید", moneyText(result.projectedEndAfterPurchase))
                        InfoRow("تغییر قابل خرج", moneyText(result.freeToSpendChange), valueColor = colors.error)
                        if (result.savingsMonthsDelayed > 0) {
                            InfoRow("تأخیر در هدف پس‌انداز", fa(result.savingsMonthsDelayed) + " ماه")
                        }
                    }
                }
                if (result.projections.isNotEmpty()) {
                    item {
                        DkCard {
                            Text("اثر خرید بر موجودی", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            ProjectionChart(result.projections, lineColor = if (result.isAffordable) colors.primary else colors.error)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "روند موجودی تا ${fa(result.scenario.installmentCount.coerceAtLeast(6))} ماه آینده",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (result.budgetImpacts.isNotEmpty()) {
                    item { SectionHeader("اثر روی بودجه‌ها") }
                    items(result.budgetImpacts, key = { it.budgetName }) { impact ->
                        DkCard(contentPadding = PaddingValues(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(impact.budgetName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "از ${faPercent(impact.beforePercent)} به ${faPercent(impact.afterPercent)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                                Pill(
                                    budgetStatusLabel(impact.afterStatus),
                                    when (impact.afterStatus) {
                                        ir.dakhlokharj.domain.model.BudgetStatus.HEALTHY -> ir.dakhlokharj.domain.model.InsightTone.POSITIVE
                                        ir.dakhlokharj.domain.model.BudgetStatus.WARNING -> ir.dakhlokharj.domain.model.InsightTone.NEUTRAL
                                        else -> ir.dakhlokharj.domain.model.InsightTone.NEGATIVE
                                    },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            DkProgressBar((impact.afterPercent / 100f).coerceIn(0f, 1f), color = budgetStatusColor(impact.afterStatus))
                        }
                    }
                }
                item {
                    SecondaryButton("پاک‌کردن شبیه‌سازی", icon = Icons.Outlined.Refresh) {
                        viewModel.clearSimulation()
                    }
                }
            }
        }
    }

    if (showPrice) {
        AmountInputSheet("قیمت خرید", priceToman, { showPrice = false }) { priceToman = it }
    }
    if (showDown) {
        AmountInputSheet("پیش‌پرداخت", downToman, { showDown = false }) { downToman = it }
    }
}