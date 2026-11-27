package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.domain.engine.ForecastLine
import ir.dakhlokharj.domain.engine.ForecastLineKind
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel

@Composable
fun ForecastScreen(viewModel: OverviewViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val forecast = state.forecast
    val colors = MaterialTheme.colorScheme

    DkScreen(
        title = "پیش‌بینی مالی",
        subtitle = "پایان ماه را قبل از رسیدن ببینید",
        onBack = onBack,
    ) { padding ->
        if (forecast == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@DkScreen
        }
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
                    Text("موجودی پیش‌بینی‌شده پایان ماه", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        moneyText(forecast.projectedEndBalance),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (forecast.projectedEndBalance.isNegative) colors.error else colors.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "با فرض دریافت درآمدهای قطعی و پرداخت تعهدات تا ${faDate(state.today.endOfMonth, DateStyle.SHORT)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("موجودی فعلی", moneyShort(forecast.currentBalance), Modifier.weight(1f))
                        StatTile("قابل خرج", moneyShort(forecast.freeToSpend), Modifier.weight(1f), colors.primary)
                        StatTile("خوش‌بینانه", moneyShort(forecast.optimisticEndBalance), Modifier.weight(1f))
                    }
                }
            }

            item {
                DkCard {
                    Text("ورودی و خروجی این ماه", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("درآمد دریافت‌شده", moneyText(forecast.receivedIncome), valueColor = colors.primary)
                    InfoRow("هزینه پرداخت‌شده", moneyText(forecast.paidExpenses), valueColor = colors.secondary)
                    InfoRow("درآمد قطعی آینده", moneyText(forecast.definiteFutureIncome), valueColor = colors.primary)
                    InfoRow("درآمد احتمالی", moneyText(forecast.probableFutureIncome), valueColor = colors.onSurfaceVariant)
                    InfoRow("تعهدات باقی‌مانده", moneyText(forecast.remainingCommitments), valueColor = colors.error)
                    DkTopDivider()
                    InfoRow("هزینه تکرارشونده", moneyText(forecast.upcomingRecurring))
                    InfoRow("اقساط پیش‌رو", moneyText(forecast.upcomingInstallments))
                    InfoRow("بدهی سررسیدشده", moneyText(forecast.upcomingDebts))
                    InfoRow("هزینه برنامه‌ریزی‌شده", moneyText(forecast.upcomingPlannedExpenses))
                    InfoRow("باقی‌مانده بودجه", moneyText(forecast.budgetRemaining))
                    InfoRow("روزهای باقی‌مانده", fa(forecast.daysRemaining) + " روز")
                }
            }

            if (forecast.lines.isNotEmpty()) {
                item { SectionHeader("خط زمان تعهدات") }
                items(forecast.lines.sortedBy { it.date ?: state.today }, key = { "${it.kind.name}-${it.title}-${it.amount.rial}" }) { line ->
                    ForecastLineRow(line)
                }
            } else {
                item {
                    Text(
                        "تعهد یا رویداد مالی دیگری برای این ماه ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            item {
                Text(
                    "این پیش‌بینی فقط بر پایه داده‌های ثبت‌شده شماست و هیچ داده‌ای به بیرون ارسال نمی‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun ForecastLineRow(line: ForecastLine) {
    val colors = MaterialTheme.colorScheme
    val isIncome = line.kind == ForecastLineKind.INCOME
    val tint = when (line.kind) {
        ForecastLineKind.INCOME -> colors.primary
        ForecastLineKind.RECEIVABLE -> colors.tertiary
        ForecastLineKind.INSTALLMENT -> colors.secondary
        else -> colors.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            when (line.kind) {
                ForecastLineKind.INCOME -> Icons.Outlined.TrendingUp
                ForecastLineKind.INSTALLMENT -> Icons.Outlined.CreditCard
                ForecastLineKind.DEBT -> Icons.Outlined.Payments
                else -> Icons.Outlined.CalendarToday
            },
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(line.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                line.date?.let { faDate(it, DateStyle.SHORT) } ?: "بدون تاریخ",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Text(
            (if (isIncome) "+" else "−") + moneyShort(line.amount),
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
        )
    }
}
